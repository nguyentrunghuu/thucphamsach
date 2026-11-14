package com.UngDung.ThucPhamXanh.View.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.CategoryForm;
import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.ProductVisibility;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Danh mục sản phẩm trang admin. Đọc: 1 listener LoaiProduct + 1 listener SanPham (đếm số sản phẩm đang bán
 * theo tên danh mục, vì SanPham.loaisp lưu theo TÊN). Xóa danh mục bị chặn khi còn sản phẩm.
 */
public class AdminLoaiSPActivity extends BaseAdminActivity {

    private static final String TAG = "AdminLoaiSPActivity";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private MaterialToolbar toolbar;
    private RecyclerView rcv;
    private View emptyView, progress;
    private TextView tvWarning;
    private AdminLoaiSPAdapter adapter;

    /** [id, tên, ảnh] của từng danh mục theo thứ tự tên. */
    private final List<String[]> categories = new ArrayList<>();
    private final Map<String, Integer> productCounts = new HashMap<>();
    private boolean categoriesLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_loai_sp);
        toolbar = setupAdminToolbar("Danh mục", null);

        rcv = findViewById(R.id.rcv_loai_sp);
        emptyView = findViewById(R.id.admin_empty);
        progress = findViewById(R.id.progress_loai_sp);
        tvWarning = findViewById(R.id.tv_category_warning);
        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add_loai_sp);

        applySystemBarInsets(null, rcv);
        applySystemBarInsets(null, findViewById(R.id.fab_container_loai_sp));

        adapter = new AdminLoaiSPAdapter(new AdminLoaiSPAdapter.Listener() {
            @Override
            public void onEdit(AdminLoaiSPAdapter.Row row) {
                startActivity(new Intent(AdminLoaiSPActivity.this, AdminAddLoaiSPActivity.class)
                        .putExtra(AdminAddLoaiSPActivity.EXTRA_DOC_ID, row.id)
                        .putExtra(AdminAddLoaiSPActivity.EXTRA_NAME, row.name));
            }

            @Override
            public void onDelete(AdminLoaiSPAdapter.Row row) {
                askDelete(row);
            }
        });
        rcv.setLayoutManager(new LinearLayoutManager(this));
        rcv.setAdapter(adapter);
        fab.setOnClickListener(v -> startActivity(new Intent(this, AdminAddLoaiSPActivity.class)));
    }

    @Override
    protected void onStartListening() {
        listen(db.collection("LoaiProduct").orderBy("tenloai", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Lỗi đọc LoaiProduct", e);
                        progress.setVisibility(View.GONE);
                        Toast.makeText(this, "Không tải được danh mục", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snap == null) return;
                    categories.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String name = FirestoreMapper.str(d, "tenloai").trim();
                        if (name.isEmpty()) continue;
                        String image = FirestoreMapper.str(d, "hinhanh"); // dữ liệu cũ có thể dùng "hinhanhloai"
                        if (image.isEmpty()) image = FirestoreMapper.str(d, "hinhanhloai");
                        categories.add(new String[]{d.getId(), name, image});
                    }
                    categoriesLoaded = true;
                    rebuild();
                }));
        listen(db.collection("SanPham").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            productCounts.clear();
            for (DocumentSnapshot d : snap.getDocuments()) {
                if (ProductVisibility.isHidden(d)) continue;
                String cat = FirestoreMapper.str(d, "loaisp").trim();
                if (cat.isEmpty()) continue;
                Integer n = productCounts.get(cat);
                productCounts.put(cat, n == null ? 1 : n + 1);
            }
            rebuild();
        }));
    }

    private void rebuild() {
        if (!categoriesLoaded) return;
        List<AdminLoaiSPAdapter.Row> rows = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (String[] c : categories) {
            Integer n = productCounts.get(c[1]);
            rows.add(new AdminLoaiSPAdapter.Row(c[0], c[1], c[2], n == null ? 0 : n));
            names.add(c[1]);
        }
        adapter.submitList(rows);
        progress.setVisibility(View.GONE);
        showEmpty(emptyView, rows.isEmpty(), "Chưa có danh mục nào.\nBấm \"Thêm danh mục\" để tạo.");
        toolbar.setSubtitle(rows.isEmpty() ? null : rows.size() + " danh mục");

        int dup = CategoryForm.countDuplicates(names);
        tvWarning.setVisibility(dup > 0 ? View.VISIBLE : View.GONE);
        if (dup > 0) {
            tvWarning.setText("Có " + dup + " danh mục trùng tên (không phân biệt hoa/thường, dấu). "
                    + "Nên gộp lại để sản phẩm không bị chia đôi.");
        }
    }

    private void askDelete(AdminLoaiSPAdapter.Row row) {
        if (row.productCount > 0) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Không thể xóa danh mục")
                    .setMessage("Danh mục \"" + row.name + "\" vẫn còn " + row.productCount
                            + " sản phẩm. Hãy chuyển các sản phẩm sang danh mục khác hoặc xóa chúng trước.")
                    .setPositiveButton("Đã hiểu", null)
                    .show();
            return;
        }
        confirm("Xóa danh mục?", "Bạn có chắc muốn xóa danh mục \"" + row.name + "\"?", "Xóa", () -> delete(row));
    }

    /** Kiểm tra lại trên máy chủ (số đếm trên màn hình có thể chưa kịp cập nhật) rồi mới xóa theo id. */
    private void delete(AdminLoaiSPAdapter.Row row) {
        db.collection("SanPham").whereEqualTo("loaisp", row.name).get()
                .addOnSuccessListener(products -> {
                    int count = 0;
                    for (QueryDocumentSnapshot p : products) if (!ProductVisibility.isHidden(p)) count++;
                    if (count > 0) {
                        Toast.makeText(this, "Danh mục vừa có thêm sản phẩm, không thể xóa", Toast.LENGTH_LONG).show();
                        return;
                    }
                    db.collection("LoaiProduct").document(row.id).delete()
                            .addOnSuccessListener(v -> Toast.makeText(this, "Đã xóa danh mục", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Lỗi xóa danh mục", e);
                                Toast.makeText(this, "Xóa danh mục thất bại, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Không kiểm tra được sản phẩm, thử lại sau", Toast.LENGTH_SHORT).show());
    }
}
