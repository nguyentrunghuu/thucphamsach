package com.UngDung.ThucPhamXanh.View.Admin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.AdminOrderFilter;
import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Danh sách đơn hàng trang admin.
 *
 * Đọc: 1 listener toàn bộ HoaDon (gắn ở onStart, tự gỡ ở onStop); lọc tab / tìm kiếm trên máy
 * bằng {@link AdminOrderFilter}. Đổi trạng thái qua {@link AdminOrderStatusSheet}; listener tự vẽ lại.
 * Mở kèm {@link #EXTRA_TAB} (vd. từ thẻ "Đơn chờ xác nhận" trên dashboard) để chọn sẵn tab.
 */
public class AdminBillMainActivity extends BaseAdminActivity {

    private static final String TAG = "AdminBillMainActivity";
    /** int: AdminOrderFilter.TAB_* chọn sẵn khi mở màn hình. */
    public static final String EXTRA_TAB = "admin_order_tab";

    private static final int[] TAB_CHIPS = {
            R.id.chip_order_all, R.id.chip_order_pending, R.id.chip_order_confirmed,
            R.id.chip_order_shipping, R.id.chip_order_delivered, R.id.chip_order_cancelled};

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final AdminOrderFilter filter = new AdminOrderFilter();
    private final List<AdminOrder> allOrders = new ArrayList<>();
    private boolean loaded;

    private AdminOrderAdapter adapter;
    private RecyclerView rcv;
    private ChipGroup chips;
    private TextView tvResult;
    private View emptyView, progress;

    public static Intent intent(Context context, int tab) {
        return new Intent(context, AdminBillMainActivity.class).putExtra(EXTRA_TAB, tab);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_bill_main);
        setupAdminToolbar("Đơn hàng", null);

        rcv = findViewById(R.id.rcv_bill_admin);
        chips = findViewById(R.id.chip_group_order_status);
        tvResult = findViewById(R.id.tv_order_result);
        emptyView = findViewById(R.id.admin_empty);
        progress = findViewById(R.id.progress_bill_admin);
        applySystemBarInsets(null, rcv);

        adapter = new AdminOrderAdapter(new AdminOrderAdapter.Listener() {
            @Override
            public void onOpen(AdminOrder order) {
                startActivity(AdminCTHDActivity.intent(AdminBillMainActivity.this, order));
            }

            @Override
            public void onUpdateStatus(AdminOrder order) {
                AdminOrderStatusSheet.show(AdminBillMainActivity.this, order, null); // listener tự cập nhật
            }
        });
        rcv.setLayoutManager(new LinearLayoutManager(this));
        rcv.setAdapter(adapter);

        if (savedInstanceState == null) {
            int tab = getIntent().getIntExtra(EXTRA_TAB, AdminOrderFilter.TAB_ALL);
            chips.check(TAB_CHIPS[tab >= 0 && tab < TAB_CHIPS.length ? tab : 0]);
        }
        filter.setTab(tabOf(chips.getCheckedChipId()));
        chips.setOnCheckedChangeListener((group, id) -> {
            filter.setTab(tabOf(id));
            render(true);
        });

        TextView edt = findViewById(R.id.edt_search_order);
        edt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                filter.setQuery(s.toString());
                render(true);
            }
        });
        render(false);
    }

    @Override
    protected void onStartListening() {
        listen(db.collection("HoaDon").addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.e(TAG, "Lỗi đọc HoaDon", e);
                progress.setVisibility(View.GONE);
                Toast.makeText(this, "Không tải được đơn hàng", Toast.LENGTH_SHORT).show();
                return;
            }
            if (snap == null) return;
            allOrders.clear();
            for (DocumentSnapshot d : snap.getDocuments()) allOrders.add(FirestoreMapper.toAdminOrder(d));
            loaded = true;
            progress.setVisibility(View.GONE);
            com.google.android.material.appbar.MaterialToolbar tb = findViewById(R.id.admin_toolbar);
            tb.setSubtitle(allOrders.size() + " đơn");
            render(false);
        }));
    }

    private void render(boolean scrollToTop) {
        if (!loaded) {
            tvResult.setText("Đang tải…");
            showEmpty(emptyView, false, null);
            return;
        }
        int[] counts = filter.countByTab(allOrders);
        for (int t = 0; t < TAB_CHIPS.length; t++) {
            Chip chip = findViewById(TAB_CHIPS[t]);
            chip.setText(AdminOrderFilter.tabLabel(t) + " (" + counts[t] + ")");
        }

        List<AdminOrder> shown = filter.apply(allOrders);
        if (scrollToTop) adapter.submitList(shown, () -> rcv.scrollToPosition(0));
        else adapter.submitList(shown);

        tvResult.setText(filter.isFiltering()
                ? "Hiển thị " + shown.size() + " / " + allOrders.size() + " đơn, mới nhất trước"
                : allOrders.size() + " đơn, mới nhất trước");
        String msg = allOrders.isEmpty() ? "Chưa có đơn hàng nào"
                : filter.getQuery().trim().isEmpty()
                ? "Không có đơn \"" + AdminOrderFilter.tabLabel(filter.getTab()).toLowerCase() + "\""
                : "Không tìm thấy đơn phù hợp";
        showEmpty(emptyView, shown.isEmpty(), msg);
    }

    private static int tabOf(int chipId) {
        for (int t = 0; t < TAB_CHIPS.length; t++) if (TAB_CHIPS[t] == chipId) return t;
        return AdminOrderFilter.TAB_ALL;
    }
}
