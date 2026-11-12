package com.UngDung.ThucPhamXanh.View.Admin;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Models.LoaiProduct;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class AdminLoaiSPActivity extends AppCompatActivity {

    private RecyclerView rcvLoaiSP;
    private ImageView imgBackLoaiSP, imgAddLoaiSP;
    private TextView tvEmptyLoaiSP;
    private ArrayList<LoaiProduct> mlistLoaiSP;
    private AdminLoaiSPAdapter adapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration loaiSPListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_loai_sp);

        InitWidget();
        Init();
        Event();
        setupRealtimeListener();
    }

    private void InitWidget() {
        rcvLoaiSP = findViewById(R.id.rcv_loai_sp);
        imgBackLoaiSP = findViewById(R.id.img_back_loai_sp);
        imgAddLoaiSP = findViewById(R.id.img_add_loai_sp);
        tvEmptyLoaiSP = findViewById(R.id.tv_empty_loai_sp);
    }

    private void Init() {
        mlistLoaiSP = new ArrayList<>();
        adapter = new AdminLoaiSPAdapter(this, mlistLoaiSP, new IClickCTHD() {
            @Override
            public void onClickCTHD(int pos) {
                LoaiProduct loaiSP = mlistLoaiSP.get(pos);
                showEditDeleteDialog(loaiSP, pos);
            }
        });
        rcvLoaiSP.setLayoutManager(new GridLayoutManager(this, 2));
        rcvLoaiSP.setAdapter(adapter);
    }

    private void Event() {
        imgBackLoaiSP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        imgAddLoaiSP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminLoaiSPActivity.this, AdminAddLoaiSPActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupRealtimeListener() {
        // Remove existing listener if any
        if (loaiSPListener != null) {
            loaiSPListener.remove();
        }

        loaiSPListener = db.collection("LoaiProduct")
                .orderBy("tenloai", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e("AdminLoaiSPActivity", "Error listening to categories", e);
                            Toast.makeText(AdminLoaiSPActivity.this, "Lỗi tải danh sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (snapshots != null) {
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                LoaiProduct loaiSP = documentToLoaiProduct(dc.getDocument());
                                int index = -1;
                                
                                // Tìm index của item trong list
                                for (int i = 0; i < mlistLoaiSP.size(); i++) {
                                    if (mlistLoaiSP.get(i).getTenloai().equals(loaiSP.getTenloai())) {
                                        index = i;
                                        break;
                                    }
                                }

                                switch (dc.getType()) {
                                    case ADDED:
                                        if (index == -1) {
                                            mlistLoaiSP.add(loaiSP);
                                            adapter.notifyItemInserted(mlistLoaiSP.size() - 1);
                                        }
                                        break;
                                    case MODIFIED:
                                        if (index != -1) {
                                            mlistLoaiSP.set(index, loaiSP);
                                            adapter.notifyItemChanged(index);
                                        }
                                        break;
                                    case REMOVED:
                                        if (index != -1) {
                                            mlistLoaiSP.remove(index);
                                            adapter.notifyItemRemoved(index);
                                        }
                                        break;
                                }
                            }
                            
                            updateEmptyState();
                        }
                    }
                });
    }

    private LoaiProduct documentToLoaiProduct(DocumentSnapshot document) {
        LoaiProduct loaiSP = new LoaiProduct();
        loaiSP.setTenloai(document.getString("tenloai"));
        // Firestore lưu field "hinhanh" khi add object trực tiếp
        // Nhưng có thể có dữ liệu cũ dùng "hinhanhloai" - thử cả hai
        String hinhanh = document.getString("hinhanh");
        if (hinhanh == null || hinhanh.isEmpty()) {
            hinhanh = document.getString("hinhanhloai");
        }
        loaiSP.setHinhanh(hinhanh);
        return loaiSP;
    }

    private void showEditDeleteDialog(LoaiProduct loaiSP, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn thao tác");
        builder.setItems(new String[]{"Chỉnh sửa", "Xóa"}, (dialog, which) -> {
            if (which == 0) {
                // Edit
                Intent intent = new Intent(AdminLoaiSPActivity.this, AdminAddLoaiSPActivity.class);
                intent.putExtra("loaisp", loaiSP.getTenloai());
                startActivity(intent);
            } else if (which == 1) {
                // Delete
                showDeleteConfirmDialog(loaiSP, position);
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showDeleteConfirmDialog(LoaiProduct loaiSP, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác nhận xóa");
        builder.setMessage("Bạn có chắc muốn xóa danh mục \"" + loaiSP.getTenloai() + "\"?");
        builder.setPositiveButton("Xóa", (dialog, which) -> {
            deleteLoaiSP(loaiSP, position);
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void deleteLoaiSP(LoaiProduct loaiSP, int position) {
        // Không cho xóa danh mục khi vẫn còn sản phẩm (đang bán) thuộc danh mục đó
        db.collection("SanPham").whereEqualTo("loaisp", loaiSP.getTenloai()).get()
                .addOnSuccessListener(products -> {
                    int count = 0;
                    for (QueryDocumentSnapshot p : products) {
                        if (!com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHidden(p)) count++;
                    }
                    if (count > 0) {
                        new AlertDialog.Builder(this)
                                .setTitle("Không thể xóa danh mục")
                                .setMessage("Danh mục \"" + loaiSP.getTenloai() + "\" vẫn còn " + count
                                        + " sản phẩm. Hãy chuyển các sản phẩm sang danh mục khác hoặc xóa chúng trước.")
                                .setPositiveButton("Đã hiểu", null)
                                .show();
                    } else {
                        doDeleteLoaiSP(loaiSP);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi kiểm tra sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void doDeleteLoaiSP(LoaiProduct loaiSP) {
        db.collection("LoaiProduct")
                .whereEqualTo("tenloai", loaiSP.getTenloai())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Xóa danh mục thành công", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Lỗi xóa danh mục: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tìm danh mục: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEmptyState() {
        if (mlistLoaiSP.isEmpty()) {
            tvEmptyLoaiSP.setVisibility(View.VISIBLE);
            rcvLoaiSP.setVisibility(View.GONE);
        } else {
            tvEmptyLoaiSP.setVisibility(View.GONE);
            rcvLoaiSP.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loaiSPListener != null) {
            loaiSPListener.remove();
        }
    }
}

