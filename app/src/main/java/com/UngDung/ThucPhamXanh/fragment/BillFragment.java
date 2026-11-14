package com.UngDung.ThucPhamXanh.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.UngDung.ThucPhamXanh.Adapter.CustomerOrderAdapter;
import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.CustomerOrders;
import com.UngDung.ThucPhamXanh.Helper.CustomerOrders.Entry;
import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.OrderLineMapper;
import com.UngDung.ThucPhamXanh.Helper.OrderMath;
import com.UngDung.ThucPhamXanh.MainActivity;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.CTHDActivity;
import com.UngDung.ThucPhamXanh.View.OrderUi;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tab "Đơn hàng" của khách: MỘT màn thay cho 4 fragment con cũ. Dữ liệu cập nhật trực tiếp
 * (listener trên HoaDon của người dùng + chi tiết các đơn), nên khi cửa hàng xác nhận / shipper giao,
 * thẻ đơn tự đổi mà không cần kéo làm mới.
 */
public class BillFragment extends Fragment {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private SwipeRefreshLayout swipe;
    private RecyclerView rcv;
    private ProgressBar progress;
    private View boxEmpty;
    private TextView tvEmptyTitle, tvEmptyDesc, tvSubtitle;
    private ImageView imgEmpty, btnClear;
    private MaterialButton btnEmpty;
    private EditText edtSearch;
    private ChipGroup chipGroup;
    private final Chip[] chips = new Chip[CustomerOrders.TAB_COUNT];

    private CustomerOrderAdapter adapter;
    private ListenerRegistration ordersReg, linesReg;

    // dữ liệu thô
    private List<AdminOrder> orders = new ArrayList<>();
    private final Map<String, List<DocumentSnapshot>> linesByOrder = new HashMap<>();
    private final Map<String, DocumentSnapshot> products = new HashMap<>();
    private final Set<String> missingProducts = new HashSet<>();
    private List<Entry> entries = new ArrayList<>();
    private boolean ordersLoaded = false;
    private boolean loadFailed = false;
    private int generation = 0; // bỏ kết quả của lần tải sản phẩm đã cũ

    // bộ lọc
    private int tab = CustomerOrders.TAB_ALL;
    private String query = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bill, container, false);
        bindViews(view);
        setupList();
        setupChips();
        setupSearch();
        swipe.setColorSchemeColors(0xFF2E7D32);
        swipe.setOnRefreshListener(() -> {
            stopListening();
            startListening();
            swipe.postDelayed(() -> {
                if (swipe != null) swipe.setRefreshing(false);
            }, 700);
        });
        btnEmpty.setOnClickListener(v -> onEmptyButton());
        render();
        return view;
    }

    private void bindViews(View view) {
        swipe = view.findViewById(R.id.swipe_bill);
        rcv = view.findViewById(R.id.rcv_bill);
        progress = view.findViewById(R.id.progress_bill);
        boxEmpty = view.findViewById(R.id.box_bill_empty);
        tvEmptyTitle = view.findViewById(R.id.tv_bill_empty_title);
        tvEmptyDesc = view.findViewById(R.id.tv_bill_empty_desc);
        tvSubtitle = view.findViewById(R.id.tv_bill_subtitle);
        imgEmpty = view.findViewById(R.id.img_bill_empty);
        btnEmpty = view.findViewById(R.id.btn_bill_empty);
        edtSearch = view.findViewById(R.id.edt_search_bill);
        btnClear = view.findViewById(R.id.btn_clear_search);
        chipGroup = view.findViewById(R.id.chip_group_bill);
        chips[CustomerOrders.TAB_ALL] = view.findViewById(R.id.chip_bill_all);
        chips[CustomerOrders.TAB_PROCESSING] = view.findViewById(R.id.chip_bill_processing);
        chips[CustomerOrders.TAB_SHIPPING] = view.findViewById(R.id.chip_bill_shipping);
        chips[CustomerOrders.TAB_DONE] = view.findViewById(R.id.chip_bill_done);
        chips[CustomerOrders.TAB_CANCELLED] = view.findViewById(R.id.chip_bill_cancelled);
    }

    private void setupList() {
        adapter = new CustomerOrderAdapter(new CustomerOrderAdapter.Listener() {
            @Override
            public void onOpen(Entry entry) {
                Intent intent = new Intent(requireContext(), CTHDActivity.class);
                intent.putExtra(CTHDActivity.EXTRA_ORDER_ID, entry.order.id);
                startActivity(intent);
            }

            @Override
            public void onCancel(Entry entry) {
                OrderUi.confirmCancel(requireActivity(), entry.order.id, null); // danh sách tự cập nhật (live)
            }

            @Override
            public void onReorder(Entry entry) {
                OrderUi.reorder(requireActivity(), entry.lines);
            }
        });
        rcv.setLayoutManager(new LinearLayoutManager(getContext()));
        rcv.setAdapter(adapter);
        rcv.setHasFixedSize(false);
    }

    private void setupChips() {
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            for (int t = 0; t < chips.length; t++) {
                if (chips[t].getId() == checkedId) {
                    if (tab != t) {
                        tab = t;
                        render();
                        rcv.scrollToPosition(0);
                    }
                    return;
                }
            }
        });
    }

    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                query = s == null ? "" : s.toString();
                btnClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                render();
                rcv.scrollToPosition(0);
            }
        });
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        btnClear.setOnClickListener(v -> edtSearch.setText(""));
    }

    private void hideKeyboard() {
        Context c = getContext();
        if (c == null) return;
        InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(edtSearch.getWindowToken(), 0);
        edtSearch.clearFocus();
    }

    // ---------------------------------------------------------------- dữ liệu

    @Override
    public void onStart() {
        super.onStart();
        startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopListening();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopListening();
        swipe = null;
    }

    private void stopListening() {
        if (ordersReg != null) { ordersReg.remove(); ordersReg = null; }
        if (linesReg != null) { linesReg.remove(); linesReg = null; }
    }

    private void startListening() {
        stopListening();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            ordersLoaded = true;
            orders = new ArrayList<>();
            rebuild();
            return;
        }
        final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        loadFailed = false;

        ordersReg = db.collection("HoaDon").whereEqualTo("UID", uid).addSnapshotListener((snap, e) -> {
            if (!isAdded()) return;
            if (e != null || snap == null) {
                loadFailed = true;
                render();
                return;
            }
            loadFailed = false;
            List<AdminOrder> list = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) list.add(FirestoreMapper.toAdminOrder(d));
            orders = list;
            ordersLoaded = true;
            rebuild();
        });

        linesReg = db.collection("ChitietHoaDon").document(uid).collection("ALL")
                .addSnapshotListener((snap, e) -> {
                    if (!isAdded() || e != null || snap == null) return;
                    linesByOrder.clear();
                    Set<String> need = new HashSet<>();
                    for (QueryDocumentSnapshot d : snap) {
                        String orderId = FirestoreMapper.str(d, "id_hoadon");
                        if (orderId.isEmpty()) continue;
                        List<DocumentSnapshot> l = linesByOrder.get(orderId);
                        if (l == null) {
                            l = new ArrayList<>();
                            linesByOrder.put(orderId, l);
                        }
                        l.add(d);
                        String pid = FirestoreMapper.str(d, "id_product");
                        if (!pid.isEmpty() && !products.containsKey(pid) && !missingProducts.contains(pid)) {
                            need.add(pid);
                        }
                    }
                    loadProducts(need);
                });
    }

    /** Tải thông tin các sản phẩm chưa có trong bộ nhớ (tên + ảnh), rồi dựng lại danh sách. */
    private void loadProducts(Set<String> ids) {
        if (ids.isEmpty()) {
            rebuild();
            return;
        }
        final int gen = ++generation;
        List<String> list = new ArrayList<>(ids);
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : list) tasks.add(db.collection("SanPham").document(id).get());
        Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
            if (!isAdded() || gen != generation) return;
            for (int i = 0; i < list.size(); i++) {
                Task<DocumentSnapshot> task = tasks.get(i);
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    products.put(list.get(i), task.getResult());
                } else if (task.isSuccessful()) {
                    missingProducts.add(list.get(i)); // sản phẩm đã bị xóa hẳn
                }
            }
            rebuild();
        });
    }

    private void rebuild() {
        List<Entry> built = new ArrayList<>();
        for (AdminOrder o : orders) {
            List<OrderMath.Line> lines = new ArrayList<>();
            List<DocumentSnapshot> docs = linesByOrder.get(o.id);
            if (docs != null) {
                for (DocumentSnapshot d : docs) {
                    lines.add(OrderLineMapper.toLine(d, products.get(FirestoreMapper.str(d, "id_product"))));
                }
            }
            built.add(new Entry(o, lines));
        }
        entries = built;
        render();
    }

    // ---------------------------------------------------------------- hiển thị

    private void render() {
        if (swipe == null) return; // view đã hủy

        int[] counts = CustomerOrders.countByTab(entries, query);
        for (int t = 0; t < chips.length; t++) {
            chips[t].setText(CustomerOrders.tabLabel(t) + (counts[t] > 0 ? " (" + counts[t] + ")" : ""));
        }
        List<Entry> shown = CustomerOrders.apply(entries, tab, query);
        adapter.submitList(shown);

        tvSubtitle.setText(entries.isEmpty()
                ? "Theo dõi và quản lý đơn hàng của bạn"
                : entries.size() + " đơn hàng");

        boolean showProgress = !ordersLoaded && !loadFailed;
        progress.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        boolean showEmpty = !showProgress && (loadFailed && entries.isEmpty() || shown.isEmpty());
        boxEmpty.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        rcv.setVisibility(showEmpty || showProgress ? View.INVISIBLE : View.VISIBLE);
        if (showEmpty) bindEmpty();
    }

    private int emptyMode = 0; // 0 mua sắm, 1 thử lại, 2 xóa tìm kiếm, 3 không nút

    private void bindEmpty() {
        boolean searching = !query.trim().isEmpty();
        if (loadFailed && entries.isEmpty()) {
            imgEmpty.setImageResource(R.drawable.ic_admin_warning);
            tvEmptyTitle.setText("Không tải được đơn hàng");
            tvEmptyDesc.setText("Kiểm tra kết nối mạng rồi thử lại nhé");
            btnEmpty.setText("Thử lại");
            btnEmpty.setVisibility(View.VISIBLE);
            emptyMode = 1;
        } else if (searching) {
            imgEmpty.setImageResource(R.drawable.ic_search);
            tvEmptyTitle.setText("Không tìm thấy đơn phù hợp");
            tvEmptyDesc.setText("Không có đơn nào khớp với \"" + query.trim() + "\"");
            btnEmpty.setText("Xóa tìm kiếm");
            btnEmpty.setVisibility(View.VISIBLE);
            emptyMode = 2;
        } else if (entries.isEmpty()) {
            imgEmpty.setImageResource(R.drawable.ic_admin_order);
            tvEmptyTitle.setText("Bạn chưa có đơn hàng nào");
            tvEmptyDesc.setText("Chọn vài món tươi ngon để bắt đầu nhé!");
            btnEmpty.setText("Mua sắm ngay");
            btnEmpty.setVisibility(View.VISIBLE);
            emptyMode = 0;
        } else {
            imgEmpty.setImageResource(R.drawable.ic_admin_order);
            tvEmptyTitle.setText("Chưa có đơn \"" + CustomerOrders.tabLabel(tab).toLowerCase() + "\"");
            tvEmptyDesc.setText(emptyHintFor(tab));
            btnEmpty.setVisibility(View.GONE);
            emptyMode = 3;
        }
    }

    private static String emptyHintFor(int tab) {
        switch (tab) {
            case CustomerOrders.TAB_PROCESSING: return "Đơn mới đặt sẽ xuất hiện ở đây cho đến khi giao đi";
            case CustomerOrders.TAB_SHIPPING: return "Khi shipper nhận đơn, bạn sẽ theo dõi được ở đây";
            case CustomerOrders.TAB_DONE: return "Những đơn đã giao thành công sẽ nằm ở đây";
            case CustomerOrders.TAB_CANCELLED: return "Bạn chưa hủy đơn nào";
            default: return "";
        }
    }

    private void onEmptyButton() {
        switch (emptyMode) {
            case 1:
                ordersLoaded = false;
                startListening();
                render();
                break;
            case 2:
                edtSearch.setText("");
                break;
            default:
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).getBottomNavigation().setSelectedItemId(R.id.menu_home);
                }
                break;
        }
    }
}
