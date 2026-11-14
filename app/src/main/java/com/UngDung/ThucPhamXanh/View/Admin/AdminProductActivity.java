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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.AdminProductFilter;
import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.ProductVisibility;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Danh sách sản phẩm trang admin.
 *
 * Đọc: 1 listener toàn bộ SanPham + 1 listener LoaiProduct (gắn ở onStart, tự gỡ ở onStop).
 * Tìm kiếm / lọc loại / lọc trạng thái làm trên máy bằng {@link AdminProductFilter}
 * nên đổi bộ lọc không tốn thêm lượt đọc. Sản phẩm đã xóa mềm (daxoa) không hiện.
 *
 * Mở kèm {@link #EXTRA_STATUS} (vd. từ thẻ "Sắp hết hàng" trên dashboard) để chọn sẵn bộ lọc.
 */
public class AdminProductActivity extends BaseAdminActivity {

    private static final String TAG = "AdminProductActivity";
    /** int: AdminProductFilter.STATUS_* chọn sẵn khi mở màn hình. */
    public static final String EXTRA_STATUS = "admin_product_status";
    private static final String STATE_CATEGORY = "category";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final AdminProductFilter filter = new AdminProductFilter(LocalDate.now());

    private final List<Product> allProducts = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private boolean productsLoaded;

    private AdminProductAdapter adapter;
    private RecyclerView rcv;
    private ChipGroup statusGroup, categoryGroup;
    private TextView tvResult;
    private View emptyView, progress;
    private ExtendedFloatingActionButton fab;
    /** Đang dựng lại chip loại bằng code → bỏ qua sự kiện chọn chip. */
    private boolean rebuildingChips;

    public static Intent intent(Context context, int status) {
        return new Intent(context, AdminProductActivity.class).putExtra(EXTRA_STATUS, status);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_product);
        setupAdminToolbar("Sản phẩm", null);

        rcv = findViewById(R.id.rcv_admin_product);
        statusGroup = findViewById(R.id.chip_group_status);
        categoryGroup = findViewById(R.id.chip_group_category);
        tvResult = findViewById(R.id.tv_product_result);
        emptyView = findViewById(R.id.admin_empty);
        progress = findViewById(R.id.progress_admin_product);
        fab = findViewById(R.id.fab_add_product);

        applySystemBarInsets(null, rcv);
        applySystemBarInsets(null, findViewById(R.id.fab_container_product));

        if (savedInstanceState != null) {
            filter.setCategory(savedInstanceState.getString(STATE_CATEGORY));
        } else {
            int status = getIntent().getIntExtra(EXTRA_STATUS, AdminProductFilter.STATUS_ALL);
            statusGroup.check(statusChipId(status));
        }
        filter.setStatus(statusOf(statusGroup.getCheckedChipId()));

        setupList();
        setupSearch();
        statusGroup.setOnCheckedChangeListener((group, checkedId) -> {
            filter.setStatus(statusOf(checkedId));
            onFilterChanged();
        });
        categoryGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (rebuildingChips) return;
            Chip chip = group.findViewById(checkedId);
            filter.setCategory(chip == null ? null : (String) chip.getTag());
            onFilterChanged();
        });
        fab.setOnClickListener(v -> startActivity(new Intent(this, AdminAddSPActivity.class)));

        rebuildCategoryChips();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CATEGORY, filter.getCategory());
    }

    @Override
    protected void onStartListening() {
        LocalDate today = LocalDate.now(); // qua ngày khi màn hình để mở
        filter.setToday(today);
        adapter.setToday(today);

        listen(db.collection("SanPham").addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.e(TAG, "Lỗi đọc SanPham", e);
                progress.setVisibility(View.GONE);
                Toast.makeText(this, "Không tải được sản phẩm", Toast.LENGTH_SHORT).show();
                return;
            }
            if (snap != null) onProducts(snap);
        }));
        listen(db.collection("LoaiProduct").orderBy("tenloai", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { Log.e(TAG, "Lỗi đọc LoaiProduct", e); return; }
                    if (snap == null) return;
                    categories.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String name = doc.getString("tenloai");
                        if (name != null && !name.trim().isEmpty()) categories.add(name);
                    }
                    rebuildCategoryChips();
                    render();
                }));
    }

    // ------------------------------------------------------------------ dữ liệu

    private void onProducts(QuerySnapshot snap) {
        allProducts.clear();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            if (ProductVisibility.isHidden(doc)) continue;
            allProducts.add(FirestoreMapper.toProduct(doc));
        }
        productsLoaded = true;
        progress.setVisibility(View.GONE);
        setToolbarSubtitle(allProducts.size() + " sản phẩm");
        rebuildCategoryChips();
        render();
    }

    /** Bộ lọc vừa đổi: vẽ lại và cuộn lên đầu (ListAdapter giữ vị trí cuộn theo dòng cũ). */
    private void onFilterChanged() {
        render(true);
    }

    private void render() {
        render(false);
    }

    private void render(boolean scrollToTop) {
        if (!productsLoaded) {
            tvResult.setText("Đang tải…");
            showEmpty(emptyView, false, null);
            return;
        }
        List<Product> shown = filter.apply(allProducts);
        if (scrollToTop) adapter.submitList(shown, () -> rcv.scrollToPosition(0));
        else adapter.submitList(shown);

        tvResult.setText(filter.isFiltering()
                ? "Hiển thị " + shown.size() + " / " + allProducts.size() + " sản phẩm"
                : allProducts.size() + " sản phẩm, sắp theo tên");

        String emptyMsg;
        if (allProducts.isEmpty()) {
            emptyMsg = "Cửa hàng chưa có sản phẩm nào.\nBấm \"Thêm sản phẩm\" để bắt đầu.";
        } else if (filter.getStatus() == AdminProductFilter.STATUS_LOW_STOCK && filter.getCategory() == null
                && filter.getQuery().trim().isEmpty()) {
            emptyMsg = "Không có sản phẩm nào sắp hết hàng";
        } else if (filter.getStatus() == AdminProductFilter.STATUS_EXPIRY && filter.getCategory() == null
                && filter.getQuery().trim().isEmpty()) {
            emptyMsg = "Không có sản phẩm nào hết hạn hoặc sắp hết hạn";
        } else {
            emptyMsg = "Không tìm thấy sản phẩm phù hợp";
        }
        showEmpty(emptyView, shown.isEmpty(), emptyMsg);
    }

    // ------------------------------------------------------------------ giao diện

    private void setupList() {
        adapter = new AdminProductAdapter(product -> {
            Intent i = new Intent(this, AdminAddSPActivity.class);
            i.putExtra("SP", product);
            startActivity(i);
        });
        rcv.setLayoutManager(new LinearLayoutManager(this));
        rcv.setAdapter(adapter);
        // Thu gọn nút thêm khi cuộn xuống để không che dòng cuối
        rcv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView r, int dx, int dy) {
                if (dy > 4 && fab.isExtended()) fab.shrink();
                else if (dy < -4 && !fab.isExtended()) fab.extend();
            }
        });
    }

    private void setupSearch() {
        TextView edt = findViewById(R.id.edt_search_product);
        edt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                filter.setQuery(s.toString());
                onFilterChanged();
            }
        });
    }

    /** Dựng lại chip loại: "Tất cả (n)" + mỗi loại kèm số sản phẩm; giữ loại đang chọn nếu còn. */
    private void rebuildCategoryChips() {
        rebuildingChips = true;
        categoryGroup.removeAllViews();

        String selected = filter.getCategory();
        Map<String, Integer> counts = AdminProductFilter.countByCategory(categories, allProducts);
        if (selected != null && !counts.containsKey(selected)) {
            selected = null; // loại vừa bị xóa / đổi tên
            filter.setCategory(null);
        }

        Chip all = addCategoryChip(productsLoaded ? "Tất cả (" + allProducts.size() + ")" : "Tất cả", null);
        Chip checked = all;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            Chip c = addCategoryChip(e.getKey() + " (" + e.getValue() + ")", e.getKey());
            if (e.getKey().equals(selected)) checked = c;
        }
        categoryGroup.check(checked.getId());
        rebuildingChips = false;
    }

    private Chip addCategoryChip(String label, String category) {
        Chip chip = new Chip(this); // style mặc định lấy từ theme admin (chipStyle = Admin.Chip.Filter)
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(category);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setSaveEnabled(false); // id sinh lúc chạy, không khôi phục theo id cũ
        categoryGroup.addView(chip);
        return chip;
    }

    private void setToolbarSubtitle(String subtitle) {
        com.google.android.material.appbar.MaterialToolbar tb = findViewById(R.id.admin_toolbar);
        tb.setSubtitle(subtitle);
    }

    private static int statusOf(int chipId) {
        if (chipId == R.id.chip_status_low_stock) return AdminProductFilter.STATUS_LOW_STOCK;
        if (chipId == R.id.chip_status_expiry) return AdminProductFilter.STATUS_EXPIRY;
        return AdminProductFilter.STATUS_ALL;
    }

    private static int statusChipId(int status) {
        if (status == AdminProductFilter.STATUS_LOW_STOCK) return R.id.chip_status_low_stock;
        if (status == AdminProductFilter.STATUS_EXPIRY) return R.id.chip_status_expiry;
        return R.id.chip_status_all;
    }
}
