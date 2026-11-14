package com.UngDung.ThucPhamXanh.View.Shipper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Adapter.ShipperOrderAdapter;
import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.ShipperOrders;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.Admin.BaseAdminActivity;
import com.UngDung.ThucPhamXanh.View.SignInActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn chính của shipper: 3 tab Đơn mới / Đang giao / Đã giao (kèm số đơn), cập nhật realtime.
 *
 * Dùng chung khung của BaseAdminActivity (toolbar, chừa chỗ cho thanh hệ thống, tự gỡ listener ở onStop).
 * Đọc Firestore bằng đúng 2 listener, đều có limit — thay cho việc tải mọi đơn 4/5 của mọi shipper:
 *   1) HoaDon where trangthai == Đã xác nhận (limit 50)  → tab "Đơn mới" (chỉ giữ đơn chưa có shipper)
 *   2) HoaDon where shipperId == tôi (limit 100)          → tab "Đang giao" và "Đã giao"
 * Việc nhận / hoàn thành đơn nằm ở {@link ShipperUpdateOrderActivity}.
 */
public class ShipperHomeActivity extends BaseAdminActivity implements ShipperOrderAdapter.Listener {

    private static final String TAG = "ShipperHome";
    private static final int LIMIT_NEW = 50;
    private static final int LIMIT_MINE = 100;
    private static final int LIMIT_WAITING = 50;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String shipperUsername;
    private String shipperName = "";

    private RecyclerView rcv;
    private ProgressBar progress;
    private View emptyView;
    private MaterialToolbar toolbar;
    private ChipGroup tabs;
    private Chip chipNew, chipActive, chipDone;
    private ShipperOrderAdapter adapter;

    // Hai nguồn dữ liệu, gộp theo id khi vẽ
    private final Map<String, AdminOrder> newOrders = new HashMap<>();
    private final Map<String, AdminOrder> myOrders = new HashMap<>();
    // Đơn admin đã chuyển "Đang giao" nhưng chưa gán shipper — cũng hiện ở tab "Đơn mới" để shipper nhận
    private final Map<String, AdminOrder> waitingOrders = new HashMap<>();
    private boolean newLoaded, mineLoaded, waitingLoaded;
    private String error;     // lỗi đọc dữ liệu gần nhất (null = ổn)
    private int tab = ShipperOrders.TAB_NEW;
    private String query = "";  // từ khóa tìm kiếm (rỗng = không lọc)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shipperUsername = getIntent().getStringExtra("shipper_username");
        if (shipperUsername == null || shipperUsername.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không có thông tin shipper", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_shipper_home);

        toolbar = setupAdminToolbar("Xin chào, " + shipperUsername, null);
        setupLogoutMenu();

        rcv = findViewById(R.id.rcv_shipper_orders);
        progress = findViewById(R.id.progress_shipper);
        emptyView = findViewById(R.id.admin_empty);
        tabs = findViewById(R.id.chip_group_shipper_tabs);
        chipNew = findViewById(R.id.chip_tab_new);
        chipActive = findViewById(R.id.chip_tab_active);
        chipDone = findViewById(R.id.chip_tab_done);

        adapter = new ShipperOrderAdapter(shipperUsername, this);
        rcv.setLayoutManager(new LinearLayoutManager(this));
        rcv.setAdapter(adapter);
        applySystemBarInsets(null, rcv);

        tabs.setOnCheckedChangeListener((group, id) -> {
            if (id == R.id.chip_tab_new) tab = ShipperOrders.TAB_NEW;
            else if (id == R.id.chip_tab_active) tab = ShipperOrders.TAB_ACTIVE;
            else if (id == R.id.chip_tab_done) tab = ShipperOrders.TAB_DONE;
            else return;
            render(true);
        });

        setupSearch();
        loadShipperName();
        render(false);
    }

    /** Tìm tới đâu lọc tới đó (dữ liệu đã nằm trong máy nên không tốn lượt đọc Firestore). */
    private void setupSearch() {
        TextView edt = findViewById(R.id.edt_search_shipper);
        edt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                if (q.equals(query)) return;
                query = q;
                render(true);
            }
        });
        edt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0); // giữ nguyên kết quả, chỉ hạ bàn phím
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onStartListening() {
        if (shipperUsername == null) return; // đã finish() ở onCreate
        newLoaded = false;
        mineLoaded = false;
        waitingLoaded = false;
        render(false);

        listen(db.collection("HoaDon")
                .whereEqualTo("trangthai", com.UngDung.ThucPhamXanh.Models.OrderStatus.DA_XAC_NHAN)
                .limit(LIMIT_NEW)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { onError("đơn mới", e); return; }
                    if (snap != null) { fill(newOrders, snap); newLoaded = true; render(false); }
                }));

        listen(db.collection("HoaDon")
                .whereEqualTo("shipperId", shipperUsername)
                .limit(LIMIT_MINE)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { onError("đơn của bạn", e); return; }
                    if (snap != null) { fill(myOrders, snap); mineLoaded = true; render(false); }
                }));

        // Đơn đang giao mà chưa có shipper: không truy vấn được "thiếu shipperId" nên lấy các đơn đang giao
        // (có limit) rồi chỉ giữ đơn chưa gán. Khi admin luôn gán shipper lúc giao thì danh sách này sẽ trống dần.
        listen(db.collection("HoaDon")
                .whereEqualTo("trangthai", com.UngDung.ThucPhamXanh.Models.OrderStatus.DANG_GIAO)
                .limit(LIMIT_WAITING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { onError("đơn chờ nhận", e); return; }
                    if (snap == null) return;
                    waitingOrders.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        AdminOrder o = FirestoreMapper.toAdminOrder(doc);
                        if (ShipperOrders.isWaitingForShipper(o)) waitingOrders.put(o.id, o);
                    }
                    waitingLoaded = true;
                    render(false);
                }));
    }

    private static void fill(Map<String, AdminOrder> target, QuerySnapshot snap) {
        target.clear();
        for (QueryDocumentSnapshot doc : snap) target.put(doc.getId(), FirestoreMapper.toAdminOrder(doc));
    }

    private void onError(String what, Exception e) {
        Log.e(TAG, "Lỗi đọc " + what, e);
        error = "Không tải được " + what + ". Kiểm tra kết nối mạng rồi thử lại.";
        newLoaded = true;
        mineLoaded = true;
        waitingLoaded = true;
        render(false);
    }

    private void loadShipperName() {
        db.collection("Shipper").whereEqualTo("username", shipperUsername).limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;
                    String name = FirestoreMapper.str(snap.getDocuments().get(0), "name");
                    if (!name.isEmpty()) {
                        shipperName = name;
                        toolbar.setTitle("Xin chào, " + name);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi đọc thông tin shipper", e));
    }

    // ------------------------------------------------------------------ vẽ màn hình

    private void render(boolean scrollToTop) {
        if (adapter == null) return;
        List<AdminOrder> all = new ArrayList<>(myOrders.values());
        for (AdminOrder o : newOrders.values()) {
            if (!myOrders.containsKey(o.id)) all.add(o);
        }
        for (AdminOrder o : waitingOrders.values()) {
            if (!myOrders.containsKey(o.id)) all.add(o);
        }

        // Đang tìm kiếm thì số trên chip là số đơn khớp từ khóa, để biết kết quả nằm ở tab nào
        int nNew = ShipperOrders.count(all, ShipperOrders.TAB_NEW, shipperUsername, query);
        int nActive = ShipperOrders.count(all, ShipperOrders.TAB_ACTIVE, shipperUsername, query);
        int nDone = ShipperOrders.count(all, ShipperOrders.TAB_DONE, shipperUsername, query);
        // Truy vấn đơn mới có limit: đủ LIMIT_NEW đơn nghĩa là còn nữa, hiện "50+" cho khỏi hiểu nhầm là hết
        boolean searching = !query.isEmpty();
        String newText = !searching && newOrders.size() >= LIMIT_NEW ? nNew + "+" : String.valueOf(nNew);
        chipNew.setText("Đơn mới (" + newText + ")");
        chipActive.setText("Đang giao (" + nActive + ")");
        chipDone.setText("Đã giao (" + nDone + ")");
        if (searching) {
            toolbar.setSubtitle("Tìm \"" + query + "\": " + (nNew + nActive + nDone) + " đơn");
        } else {
            toolbar.setSubtitle(nActive > 0 ? nActive + " đang giao · " + newText + " mới" : newText + " đơn mới chờ nhận");
        }

        boolean loading = !(newLoaded && mineLoaded && waitingLoaded);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);

        List<AdminOrder> shown = ShipperOrders.forTab(all, tab, shipperUsername, query);
        adapter.submitList(shown, scrollToTop ? () -> rcv.scrollToPosition(0) : null);

        if (loading) {
            showEmpty(emptyView, false, null);
        } else if (error != null && shown.isEmpty()) {
            showEmpty(emptyView, true, error);
        } else {
            showEmpty(emptyView, shown.isEmpty(), searching ? searchEmptyText(nNew, nActive, nDone) : emptyText());
        }
    }

    /** Không có kết quả ở tab hiện tại: nói rõ và chỉ sang tab khác nếu ở đó có đơn khớp. */
    private String searchEmptyText(int nNew, int nActive, int nDone) {
        StringBuilder sb = new StringBuilder("Không có đơn nào khớp \"" + query + "\" ở tab này.");
        if (tab != ShipperOrders.TAB_NEW && nNew > 0) {
            sb.append("\nCó ").append(nNew).append(" đơn ở tab \"Đơn mới\".");
        }
        if (tab != ShipperOrders.TAB_ACTIVE && nActive > 0) {
            sb.append("\nCó ").append(nActive).append(" đơn ở tab \"Đang giao\".");
        }
        if (tab != ShipperOrders.TAB_DONE && nDone > 0) {
            sb.append("\nCó ").append(nDone).append(" đơn ở tab \"Đã giao\".");
        }
        return sb.toString();
    }

    private String emptyText() {
        switch (tab) {
            case ShipperOrders.TAB_ACTIVE: return "Bạn chưa nhận đơn nào.\nVào tab \"Đơn mới\" để nhận đơn.";
            case ShipperOrders.TAB_DONE:   return "Chưa có đơn nào bạn đã giao.";
            default:                       return "Hiện chưa có đơn mới cần giao.";
        }
    }

    // ------------------------------------------------------------------ thao tác trên đơn

    @Override
    public void onOpen(AdminOrder order) {
        Intent i = new Intent(this, ShipperUpdateOrderActivity.class);
        i.putExtra("order_id", order.id);
        i.putExtra("shipper_username", shipperUsername);
        i.putExtra("shipper_name", shipperName);
        startActivity(i);
    }

    @Override
    public void onCall(AdminOrder order) {
        String number = ShipperOrders.dialNumber(order.phone);
        if (number.isEmpty()) return;
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number))); // mở trình quay số, không cần quyền
    }

    // ------------------------------------------------------------------ đăng xuất

    private void setupLogoutMenu() {
        toolbar.setNavigationIcon(null); // màn chính: không có nút quay lại, đăng xuất bằng biểu tượng bên phải
        Menu menu = toolbar.getMenu();
        MenuItem logout = menu.add(Menu.NONE, 1, Menu.NONE, "Đăng xuất");
        logout.setIcon(R.drawable.ic_sign_out);
        if (logout.getIcon() != null) {
            logout.getIcon().setTint(ContextCompat.getColor(this, R.color.white));
        }
        logout.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                confirm("Đăng xuất", "Bạn muốn thoát khỏi tài khoản shipper?", "Đăng xuất", () -> {
                    Intent i = new Intent(this, SignInActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                });
                return true;
            }
            return false;
        });
    }
}
