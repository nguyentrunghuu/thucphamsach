package com.UngDung.ThucPhamXanh.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Adapter.HoaDonDaGiaoAdapter;
import com.UngDung.ThucPhamXanh.Models.HoaDon;
import com.UngDung.ThucPhamXanh.Models.OrderStatus;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.Presenter.GioHangPresenter;
import com.UngDung.ThucPhamXanh.Presenter.HoaDonPreSenter;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.GioHangView;
import com.UngDung.ThucPhamXanh.my_interface.HoaDonView;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

public class CTHDActivity extends AppCompatActivity implements GioHangView, HoaDonView {

    private ImageView imgBackCTHD;
    private TextView tvMaCTHD, tvNgaydatCTHD, tvHotenCTHD, tvDiachiCTHD, tvSdtCTHD, tvTongtienCTHD, tvTrangthaiCTHD;
    private RecyclerView rcvCTHD;
    private Button btnCapnhat;
    private HoaDon hoaDon;
    private ArrayList<Product> mlist;
    private HoaDonDaGiaoAdapter hoaDonDaGiaoAdapter;
    private GioHangPresenter gioHangPresenter;
    private HoaDonPreSenter hoaDonPreSenter;
    boolean cm;



    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cthdactivity);

        InitWidget();
        Init();

        imgBackCTHD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btnCapnhat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Kiểm tra trạng thái trước khi hủy
                if (OrderStatus.customerCanCancel(hoaDon.getType())) {
                    Log.d("CTHDActivity", "🔄 User hủy đơn hàng - Trạng thái hiện tại: " + hoaDon.getType());
                    hoaDonPreSenter.CapNhatTrangThai(OrderStatus.DA_HUY, hoaDon.getId());
                    mlist.clear();
                    hoaDonDaGiaoAdapter.notifyDataSetChanged();
                    Toast.makeText(CTHDActivity.this, "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show();
                    Log.d("CTHDActivity", "✅ Đã hủy đơn hàng thành công");
                    finish();
                } else {
                    Log.w("CTHDActivity", "⚠️ Không thể hủy đơn hàng - Trạng thái: " + hoaDon.getType());
                    Toast.makeText(CTHDActivity.this, "Không thể hủy đơn hàng ở trạng thái này", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    private void Init() {
        Log.d("CTHDActivity", "🚀 Init: Khởi tạo CTHDActivity");
        mlist = new ArrayList<>();
        Intent intent = getIntent();
        hoaDon = (HoaDon) intent.getSerializableExtra("HD");
        cm = intent.getBooleanExtra("CM", false);

        Log.d("CTHDActivity", "📋 Thông tin hóa đơn:");
        Log.d("CTHDActivity", "📋 ID: " + hoaDon.getId());
        Log.d("CTHDActivity", "📋 UID: " + hoaDon.getUid());
        Log.d("CTHDActivity", "📋 CM: " + cm);

        tvMaCTHD.setText(hoaDon.getId());
        tvNgaydatCTHD.setText(hoaDon.getNgaydat());
        tvHotenCTHD.setText(hoaDon.getHoten());
        tvDiachiCTHD.setText(hoaDon.getDiachi());
        tvSdtCTHD.setText(hoaDon.getSdt());
        tvTongtienCTHD.setText(hoaDon.getTongtien());
        // Trạng thái theo quy ước chung (Models/OrderStatus)
        long status = hoaDon.getType();
        tvTrangthaiCTHD.setText(OrderStatus.label(status));
        if (OrderStatus.customerCanCancel(status)) {
            btnCapnhat.setVisibility(View.VISIBLE);
            btnCapnhat.setText("Hủy đơn hàng");
        } else {
            btnCapnhat.setVisibility(View.GONE);
        }
        applyStatusStyle(status);
        // Ẩn/hiện cả thanh chứa nút hủy theo nút
        if (btnCapnhat.getParent() instanceof View) {
            ((View) btnCapnhat.getParent()).setVisibility(btnCapnhat.getVisibility());
        }
        hoaDonPreSenter = new HoaDonPreSenter(this);
        gioHangPresenter = new GioHangPresenter(this);

        // Khởi tạo adapter và RecyclerView ngay từ đầu
        rcvCTHD.setLayoutManager(new LinearLayoutManager(this));
        rcvCTHD.setAdapter(hoaDonDaGiaoAdapter);
        
        // Set adapter với danh sách rỗng ban đầu
        if (cm) {
            hoaDonDaGiaoAdapter.setDataHoaDonDaGiao(this, mlist, 1, new IClickCTHD() {
                @Override
                public void onClickCTHD(int pos) {
                    if (cm) {
                        ShowDialog(pos);
                    }
                }
            });
        } else {
            hoaDonDaGiaoAdapter.setDataHoaDonDaGiao(this, mlist, 0, new IClickCTHD() {
                @Override
                public void onClickCTHD(int pos) {
                }
            });
        }

        Log.d("CTHDActivity", "═══════════════════════════════════════");
        Log.d("CTHDActivity", "📦 Bắt đầu load dữ liệu chi tiết hóa đơn...");
        Log.d("CTHDActivity", "📦 ID hóa đơn: " + hoaDon.getId());
        Log.d("CTHDActivity", "📦 UID: " + hoaDon.getUid());
        Log.d("CTHDActivity", "📦 GioHangPresenter: " + (gioHangPresenter != null ? "NOT NULL" : "NULL"));
        Log.d("CTHDActivity", "═══════════════════════════════════════");
        
        if (hoaDon.getUid() == null || hoaDon.getUid().isEmpty()) {
            Log.e("CTHDActivity", "❌ UID là null hoặc rỗng, không thể load dữ liệu");
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
        } else if (gioHangPresenter == null) {
            Log.e("CTHDActivity", "❌ GioHangPresenter là NULL!");
            Toast.makeText(this, "Lỗi: Không thể khởi tạo presenter", Toast.LENGTH_SHORT).show();
        } else {
            Log.d("CTHDActivity", "📦 Gọi gioHangPresenter.HandlegetDataCTHD...");
            gioHangPresenter.HandlegetDataCTHD(hoaDon.getId(), hoaDon.getUid());
            Log.d("CTHDActivity", "📦 Đã gọi gioHangPresenter.HandlegetDataCTHD");
            
            // Debug: Query trực tiếp để kiểm tra
            debugQueryFirestore(hoaDon.getId(), hoaDon.getUid());
        }

    }
    
    private void debugQueryFirestore(String hoaDonId, String uid) {
        Log.d("CTHDActivity", "═══════════════════════════════════════");
        Log.d("CTHDActivity", "🔍 DEBUG: Query Firestore trực tiếp");
        Log.d("CTHDActivity", "🔍 Path: ChitietHoaDon/" + uid + "/ALL");
        Log.d("CTHDActivity", "🔍 Query: where id_hoadon = " + hoaDonId);
        Log.d("CTHDActivity", "═══════════════════════════════════════");
        
        db.collection("ChitietHoaDon").document(uid)
                .collection("ALL").whereEqualTo("id_hoadon", hoaDonId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("CTHDActivity", "🔍 Query thành công! Tìm thấy: " + queryDocumentSnapshots.size() + " chi tiết");
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.w("CTHDActivity", "⚠️ Không tìm thấy chi tiết hóa đơn nào!");
                        Log.w("CTHDActivity", "⚠️ Có thể dữ liệu chưa được lưu hoặc đường dẫn sai");
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Không tìm thấy sản phẩm trong hóa đơn này", Toast.LENGTH_LONG).show();
                        });
                    } else {
                        Log.d("CTHDActivity", "🔍 Danh sách chi tiết hóa đơn:");
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Log.d("CTHDActivity", "  - ID: " + doc.getId() + ", id_product: " + doc.getString("id_product") + ", soluong: " + doc.getLong("soluong"));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CTHDActivity", "❌ Lỗi query Firestore: " + e.getMessage());
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                });
    }

    /** Tô màu nhãn trạng thái theo quy ước chung (Models/OrderStatus). */
    private void applyStatusStyle(long status) {
        tvTrangthaiCTHD.setTextColor(OrderStatus.textColor(status));
        android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
        pill.setColor(OrderStatus.backgroundColor(status));
        pill.setCornerRadius(100 * getResources().getDisplayMetrics().density);
        tvTrangthaiCTHD.setBackground(pill);
    }

    private void InitWidget() {
        imgBackCTHD = findViewById(R.id.img_back_cthd);
        tvMaCTHD = findViewById(R.id.tv_ma_cthd);
        tvNgaydatCTHD = findViewById(R.id.tv_ngaydat_cthd);
        tvHotenCTHD = findViewById(R.id.tv_hoten_cthd);
        tvDiachiCTHD = findViewById(R.id.tv_diachi_cthd);
        tvSdtCTHD = findViewById(R.id.tv_sdt_cthd);
        tvTongtienCTHD = findViewById(R.id.tv_tongtien_cthd);
        tvTrangthaiCTHD = findViewById(R.id.tv_trangthai_cthd);
        rcvCTHD = findViewById(R.id.rcv_cthd);
        btnCapnhat = findViewById(R.id.btn_capnhat_cthd);
        hoaDonDaGiaoAdapter = new HoaDonDaGiaoAdapter();
    }

    @Override
    public void getDataSanPham(String id, String idsp, String tensp, Long giatien, String hinhanh, String loaisp,
            String mota, Long soluong, String hansudung, Long type, String trongluong) {
        Log.d("CTHDActivity", "═══════════════════════════════════════");
        Log.d("CTHDActivity", "📦 getDataSanPham: CALLBACK ĐƯỢC GỌI!");
        Log.d("CTHDActivity", "📦 Thêm sản phẩm vào danh sách");
        Log.d("CTHDActivity", "📦 ID: " + id);
        Log.d("CTHDActivity", "📦 IDSP: " + idsp);
        Log.d("CTHDActivity", "📦 Tên SP: " + tensp);
        Log.d("CTHDActivity", "📦 Số lượng: " + soluong);
        Log.d("CTHDActivity", "📦 Giá: " + giatien);
        Log.d("CTHDActivity", "═══════════════════════════════════════");

        // Kiểm tra null để tránh crash
        if (tensp == null || idsp == null) {
            Log.e("CTHDActivity", "❌ Dữ liệu sản phẩm không hợp lệ");
            return;
        }

        mlist.add(new Product(id, idsp, tensp, giatien, hinhanh, loaisp, mota, soluong, hansudung, type, trongluong));
        Log.d("CTHDActivity", "📦 Danh sách hiện có " + mlist.size() + " sản phẩm");

        // Update adapter mỗi khi có sản phẩm mới - đảm bảo chạy trên UI thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Đã ở UI thread
            updateAdapter();
        } else {
            // Chưa ở UI thread, chuyển sang UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateAdapter();
                }
            });
        }
    }
    
    private void updateAdapter() {
        Log.d("CTHDActivity", "📦 updateAdapter: Danh sách có " + mlist.size() + " sản phẩm");
        Log.d("CTHDActivity", "📦 cm: " + cm);
        
        if (hoaDonDaGiaoAdapter == null) {
            Log.e("CTHDActivity", "❌ Adapter là NULL!");
            return;
        }
        
        if (rcvCTHD == null) {
            Log.e("CTHDActivity", "❌ RecyclerView là NULL!");
            return;
        }
        
        int typeValue = cm ? 1 : 0;
        Log.d("CTHDActivity", "📦 Update adapter với type=" + typeValue);
        
        hoaDonDaGiaoAdapter.setDataHoaDonDaGiao(this, mlist, typeValue, new IClickCTHD() {
            @Override
            public void onClickCTHD(int pos) {
                if (cm && pos >= 0 && pos < mlist.size()) {
                    ShowDialog(pos);
                }
            }
        });
        
        // Đảm bảo RecyclerView có adapter
        if (rcvCTHD.getAdapter() == null) {
            Log.d("CTHDActivity", "📦 Set adapter cho RecyclerView");
            rcvCTHD.setAdapter(hoaDonDaGiaoAdapter);
        }
        
        // Force refresh
        hoaDonDaGiaoAdapter.notifyDataSetChanged();
        Log.d("CTHDActivity", "✅ Adapter đã được update với " + mlist.size() + " sản phẩm");

    }

    private void ShowDialog(int pos) {
        Product product = mlist.get(pos);
        Dialog dialog = new Dialog(CTHDActivity.this);
        dialog.setContentView(R.layout.dialog_danhgia);
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        CustomDialog(dialog, product);
    }

    private void CustomDialog(Dialog dialog, Product product) {
        ImageView imgDanhgia = dialog.findViewById(R.id.img_danhgia);
        TextView tvNameDanhgia = dialog.findViewById(R.id.tv_name_danhgia);
        TextView tvNumberDanhgia = dialog.findViewById(R.id.tv_number_danhgia);
        RadioGroup rdgDanhgia = dialog.findViewById(R.id.rdg_danhgia);
        RadioButton rdoDanhgiaTot = dialog.findViewById(R.id.rdo_danhgia_tot);
        RadioButton rdoDanhgiaKhongTot = dialog.findViewById(R.id.rdo_danhgia_khongtot);
        RadioButton rdoDanhgiaKhac = dialog.findViewById(R.id.rdo_danhgia_khac);
        EditText edtDanhgiaKhac = dialog.findViewById(R.id.edt_danhgia_khac);
        RatingBar ratingDanhgia = dialog.findViewById(R.id.rating_danhgia);
        Button btnGuiDanhgia = dialog.findViewById(R.id.btn_danhgia_gui);
        Button btnHuyDanhgia = dialog.findViewById(R.id.btn_danhgia_huy);

        Picasso.get().load(product.getHinhanh()).into(imgDanhgia);
        tvNameDanhgia.setText(product.getTensp());
        tvNumberDanhgia.setText(product.getSoluong() + "");
        rdgDanhgia.clearCheck();
        edtDanhgiaKhac.setEnabled(false);
        ratingDanhgia.setRating(5f);
        // Đọc trạng thái trực tiếp từ các view khi bấm Gửi, không dùng biến dùng chung giữa các lần mở dialog
        // (trước đây số sao / nội dung của lần đánh giá trước bị gửi lại cho sản phẩm sau).
        rdgDanhgia.setOnCheckedChangeListener((group, checkedId) -> {
            boolean khac = checkedId == R.id.rdo_danhgia_khac;
            edtDanhgiaKhac.setEnabled(khac);
            if (khac) edtDanhgiaKhac.requestFocus();
        });

        btnGuiDanhgia.setOnClickListener(view -> {
            String noidung;
            int checked = rdgDanhgia.getCheckedRadioButtonId();
            if (checked == R.id.rdo_danhgia_tot) {
                noidung = "Sản phẩm rất tốt";
            } else if (checked == R.id.rdo_danhgia_khongtot) {
                noidung = "Không hài lòng";
            } else if (checked == R.id.rdo_danhgia_khac) {
                noidung = edtDanhgiaKhac.getText().toString().trim();
                if (noidung.isEmpty()) {
                    edtDanhgiaKhac.setError("Vui lòng nhập nhận xét");
                    return;
                }
            } else {
                Toast.makeText(CTHDActivity.this, "Vui lòng chọn nhận xét", Toast.LENGTH_SHORT).show();
                return;
            }
            float rating = ratingDanhgia.getRating();
            if (rating <= 0f) {
                Toast.makeText(CTHDActivity.this, "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show();
                return;
            }
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(CTHDActivity.this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                return;
            }

            HashMap<String, String> map = new HashMap<>();
            map.put("idproduct", product.getIdsp());
            map.put("iduser", FirebaseAuth.getInstance().getCurrentUser().getUid());
            map.put("noidung", noidung);
            map.put("rate", String.valueOf(rating));
            btnGuiDanhgia.setEnabled(false); // chống bấm nhiều lần tạo đánh giá trùng
            db.collection("BinhLuan").add(map)
                    .addOnSuccessListener(documentReference -> {
                        hoaDonDaGiaoAdapter.notifyDataSetChanged();
                        Toast.makeText(CTHDActivity.this, "Đánh giá thành công", Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    })
                    .addOnFailureListener(e -> {
                        btnGuiDanhgia.setEnabled(true);
                        Toast.makeText(CTHDActivity.this, "Gửi đánh giá thất bại, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                    });
        });

        btnHuyDanhgia.setOnClickListener(view -> dialog.cancel());
    }

    @Override
    public void getDataHD(String id, String uid, String ghichu, String diachi, String hoten, String ngaydat,
            String phuongthuc, String sdt, String tongtien, Long type) {

    }

    @Override
    public void OnSucess() {

    }

    @Override
    public void OnFail() {

    }
}