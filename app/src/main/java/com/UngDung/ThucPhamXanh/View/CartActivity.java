package com.UngDung.ThucPhamXanh.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Adapter.GiohangAdapter;
import com.UngDung.ThucPhamXanh.Models.CategoryDiscount;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.Presenter.GioHangPresenter;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.GioHangView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.UngDung.ThucPhamXanh.Helper.VNPayHelper;

public class CartActivity extends AppCompatActivity implements GioHangView {

    private NestedScrollView scrollViewCart;
    private TextView tvNullCart;
    private View view;
    private android.view.View emptyStateCart;
    private android.view.View paymentButtonCard;

    private RecyclerView rcvGioHang;
    private GiohangAdapter giohangAdapter;
    private GioHangPresenter gioHangPresenter;
    public ArrayList<Product> listGiohang;
    private TextView tvDongia, tvPhiVanChuyen, tvTongTien;
    private TextView btnThanhToan;
    private ImageView imgBackCart;

    private Intent intent;
    private Product product;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ActivityResultLauncher for MapLocationActivity
    private ActivityResultLauncher<Intent> mapLocationLauncher;

    // Hiển thị dialog thanh toán
    private Spinner spinnerPhuongthuc;
    private String s[] = { "Thanh toán khi nhận hàng", "Thanh toán VNPay" };
    private String tienthanhtoan = "";
    private String hoten, diachi, sdt, ghichu;
    private String ngaydat, phuongthuc;

    // Dialog UI components for address selection
    private TextView tvSelectedAddress;
    private Button btnSelectAddress;
    private TextView tvDongiaDialog;
    private TextView tvPhiVanChuyenDialog;
    private TextView tvTongTienDialog;
    private TextView tvPromotionMessage;
    private androidx.cardview.widget.CardView cardPromotionMessage;
    private LinearLayout layoutDiscount;
    private TextView tvDiscountDialog;
    private TextView tvDistanceDialog;
    private LinearLayout layoutDistance;
    private Dialog currentDialog;
    
    // Address coordinates
    private Double deliveryLatitude = null;
    private Double deliveryLongitude = null;
    
    // Promotion data - chỉ khuyến mãi theo loại sản phẩm
    private HashMap<String, CategoryDiscount> categoryDiscounts = new HashMap<>(); // categoryName -> CategoryDiscount

    // Momo
    int total;
    Number number;

    String sanpham = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        InitWidget();
        DeleteDataGioHang();
        TongTienGioHang();

        initMapLocationLauncher();
    }

    private void initMapLocationLauncher() {
        mapLocationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String selectedAddress = result.getData().getStringExtra("selected_address");
                        // Kiểm tra xem có tọa độ không (có thể là 0.0 nếu không có)
                        boolean hasLatitude = result.getData().hasExtra("latitude");
                        boolean hasLongitude = result.getData().hasExtra("longitude");
                        double latitude = hasLatitude ? result.getData().getDoubleExtra("latitude", 0.0) : 0.0;
                        double longitude = hasLongitude ? result.getData().getDoubleExtra("longitude", 0.0) : 0.0;

                        Log.d("CartActivity", "Nhận kết quả từ map - Địa chỉ: " + selectedAddress);
                        Log.d("CartActivity", "Tọa độ nhận được - lat: " + latitude + ", lon: " + longitude);
                        Log.d("CartActivity", "Has latitude extra: " + hasLatitude + ", Has longitude extra: " + hasLongitude);

                        // Update the selected address in the dialog
                        if (selectedAddress != null && !selectedAddress.isEmpty()) {
                            // Store the selected address for later use
                            this.diachi = selectedAddress;
                            
                            // Lưu tọa độ để tính khoảng cách chính xác
                            // Kiểm tra cả hasExtra và giá trị != 0.0 để đảm bảo có tọa độ hợp lệ
                            if (hasLatitude && hasLongitude && latitude != 0.0 && longitude != 0.0) {
                                deliveryLatitude = latitude;
                                deliveryLongitude = longitude;
                                Log.d("CartActivity", "✅ Đã lưu tọa độ từ map: lat=" + latitude + ", lon=" + longitude);
                            } else {
                                // Nếu không có tọa độ hợp lệ, reset về null để dùng geocoding
                                deliveryLatitude = null;
                                deliveryLongitude = null;
                                Log.w("CartActivity", "⚠️ Không có tọa độ hợp lệ từ map, sẽ dùng geocoding. lat=" + latitude + ", lon=" + longitude);
                            }

                            // Update UI in the dialog
                            if (tvSelectedAddress != null && btnSelectAddress != null) {
                                tvSelectedAddress.setText(selectedAddress);
                                tvSelectedAddress.setVisibility(View.VISIBLE);
                                btnSelectAddress.setText("Thay đổi địa chỉ");
                            }

                            // Cập nhật lại phí ship và tổng tiền trong dialog khi địa chỉ thay đổi
                            updateDialogPrices();
                            
                            // Cập nhật lại phí ship và tổng tiền ở activity chính
                            TongTienGioHang();

                            Toast.makeText(this, "Đã chọn địa chỉ: " + selectedAddress, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateProductQuantity() {
        Log.d("CartActivity",
                "updateProductQuantity: Bắt đầu cập nhật số lượng cho " + listGiohang.size() + " sản phẩm");

        // Trừ số lượng sản phẩm trong database khi thanh toán thành công
        for (Product product : listGiohang) {
            Log.d("CartActivity", "updateProductQuantity: Đang cập nhật sản phẩm " + product.getTensp() +
                    " (Document ID: " + product.getIdsp() + ", Số lượng: " + product.getSoluong() + ")");

            // Sử dụng document ID trực tiếp thay vì tìm kiếm bằng field
            if (product.getIdsp() != null && !product.getIdsp().isEmpty()) {
                db.collection("SanPham")
                        .document(product.getIdsp())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                Log.d("CartActivity", "updateProductQuantity: Tìm thấy sản phẩm với Document ID "
                                        + product.getIdsp());

                                int currentQuantity = documentSnapshot.getLong("soluong").intValue();
                                int newQuantity = (int) (currentQuantity - product.getSoluong());

                                Log.d("CartActivity", "updateProductQuantity: Sản phẩm " + product.getTensp() +
                                        " - Số lượng hiện tại: " + currentQuantity + ", Số lượng đặt: "
                                        + product.getSoluong() +
                                        ", Số lượng mới: " + newQuantity);

                                // Đảm bảo số lượng không âm
                                if (newQuantity < 0) {
                                    Log.w("CartActivity", "updateProductQuantity: Số lượng mới âm, đặt về 0");
                                    newQuantity = 0;
                                }

                                // Cập nhật số lượng mới
                                int finalNewQuantity = newQuantity;
                                // Trừ tồn kho + cộng số đã bán (dùng cho sắp xếp "Bán chạy")
                                documentSnapshot.getReference().update("soluong", newQuantity,
                                        "daban", com.google.firebase.firestore.FieldValue.increment(product.getSoluong()))
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("CartActivity",
                                                    "✅ Đã cập nhật số lượng sản phẩm " + product.getTensp() +
                                                            " từ " + currentQuantity + " xuống " + finalNewQuantity);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("CartActivity", "❌ Lỗi cập nhật số lượng sản phẩm "
                                                    + product.getTensp() + ": " + e.getMessage());
                                        });
                            } else {
                                Log.e("CartActivity", "updateProductQuantity: Không tìm thấy sản phẩm với Document ID "
                                        + product.getIdsp());
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("CartActivity", "❌ Lỗi tìm sản phẩm " + product.getTensp() + " (Document ID: "
                                    + product.getIdsp() + "): " + e.getMessage());
                        });
            } else {
                Log.e("CartActivity",
                        "updateProductQuantity: Document ID của sản phẩm " + product.getTensp() + " là null hoặc rỗng");
            }
        }
    }

    private void InitWidget() {

        scrollViewCart = findViewById(R.id.scrollView_cart);
        tvNullCart = findViewById(R.id.tv_null_cart);
        emptyStateCart = findViewById(R.id.empty_state_cart);
        paymentButtonCard = findViewById(R.id.payment_button_card);
        rcvGioHang = findViewById(R.id.rcv_giohang);
        tvDongia = findViewById(R.id.tv_dongia);
        tvPhiVanChuyen = findViewById(R.id.tv_phivanchuyen);
        tvTongTien = findViewById(R.id.tv_tongtien);
        btnThanhToan = findViewById(R.id.btn_thanhtoan);
        imgBackCart = findViewById(R.id.img_back_cart);

        listGiohang = new ArrayList<>();
        giohangAdapter = new GiohangAdapter(this, listGiohang, this);
        gioHangPresenter = new GioHangPresenter(this);
        gioHangPresenter.HandlegetDataGioHang();

        btnThanhToan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("CartActivity", "Nút thanh toán được bấm! Số lượng sản phẩm: " + listGiohang.size());
                if (isCartValid()) {
                    DiaLogThanhToan();
                }
            }
        });

        // Giỏ trống: nút "Mua sắm ngay" quay về trang trước
        View btnShopNow = findViewById(R.id.btn_shop_now);
        if (btnShopNow != null) btnShopNow.setOnClickListener(v -> finish());

        // AI gợi ý món ăn từ các sản phẩm đang có trong giỏ
        View btnAiDishes = findViewById(R.id.btn_ai_dishes);
        if (btnAiDishes != null) btnAiDishes.setOnClickListener(v -> AiDishesSheet.show(this, listGiohang));

        imgBackCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void DiaLogThanhToan() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_thanhtoan);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        currentDialog = dialog; // Lưu reference để cập nhật sau
        CustomInit(dialog);
        dialog.show();
        // Rộng 92% màn hình, cao tối đa 88% (phần giữa tự cuộn)
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        dialog.getWindow().setLayout((int) (dm.widthPixels * 0.92), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        android.view.View content = dialog.findViewById(android.R.id.content);
        if (content != null) {
            content.post(() -> {
                int maxH = (int) (dm.heightPixels * 0.88);
                if (content.getHeight() > maxH) {
                    dialog.getWindow().setLayout((int) (dm.widthPixels * 0.92), maxH);
                }
            });
        }
    }

    private void CustomInit(Dialog dialog) {
        spinnerPhuongthuc = dialog.findViewById(R.id.spinner_phuongthuc);
        EditText edthoten = dialog.findViewById(R.id.edt_hoten);
        btnSelectAddress = dialog.findViewById(R.id.btn_select_address);
        tvSelectedAddress = dialog.findViewById(R.id.tv_selected_address);
        EditText edtsdt = dialog.findViewById(R.id.edt_sdt);
        EditText edtghichu = dialog.findViewById(R.id.edt_ghichu);
        tvTongTienDialog = dialog.findViewById(R.id.tv_tongtien);
        tvDongiaDialog = dialog.findViewById(R.id.tv_dongia_dialog);
        tvPhiVanChuyenDialog = dialog.findViewById(R.id.tv_phivanchuyen_dialog);
        tvPromotionMessage = dialog.findViewById(R.id.tv_promotion_message);
        cardPromotionMessage = dialog.findViewById(R.id.card_promotion_message);
        layoutDiscount = dialog.findViewById(R.id.layout_discount);
        tvDiscountDialog = dialog.findViewById(R.id.tv_discount_dialog);
        tvDistanceDialog = dialog.findViewById(R.id.tv_distance_dialog);
        layoutDistance = dialog.findViewById(R.id.layout_distance);
        Button btnxacnhan = dialog.findViewById(R.id.btn_xacnhan);
        ImageView btnCancel = dialog.findViewById(R.id.btn_cancel);
        dialog.setCanceledOnTouchOutside(false);
        
        // Load chương trình khuyến mãi
        loadPromotion();

        ArrayAdapter arrayAdapter = new ArrayAdapter(CartActivity.this,
                android.R.layout.simple_list_item_1, s);
        spinnerPhuongthuc.setAdapter(arrayAdapter);

        // Cập nhật giá trị ban đầu cho dialog (trước khi load địa chỉ từ server)
        updateDialogPrices();
        
        // Load thông tin user từ server
        db.collection("User").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("Profile").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                            if (documentSnapshot != null) {
                                edthoten.setText(documentSnapshot.getString("hoten"));
                                String savedAddress = documentSnapshot.getString("diachi");
                                if (savedAddress != null && !savedAddress.isEmpty()) {
                                    // Đã có địa chỉ từ server, set vào biến diachi để không bắt chọn
                                    diachi = savedAddress;
                                    tvSelectedAddress.setText(savedAddress);
                                    tvSelectedAddress.setVisibility(View.VISIBLE);
                                    btnSelectAddress.setText("Thay đổi địa chỉ");
                                    Log.d("CartActivity", "Đã load địa chỉ từ server: " + savedAddress);
                                    
                                    // Reset tọa độ vì địa chỉ từ server không có tọa độ
                                    // Sẽ dùng geocoding để tính khoảng cách
                                    deliveryLatitude = null;
                                    deliveryLongitude = null;
                                    
                                    // Cập nhật lại phí ship và tổng tiền khi có địa chỉ
                                    updateDialogPrices();
                                } else {
                                    // Chưa có địa chỉ, bắt người dùng phải chọn
                                    diachi = null;
                                    tvSelectedAddress.setVisibility(View.GONE);
                                    btnSelectAddress.setText("Chọn địa chỉ");
                                    Log.d("CartActivity", "Chưa có địa chỉ từ server, bắt người dùng chọn");
                                    
                                    // Reset tọa độ
                                    deliveryLatitude = null;
                                    deliveryLongitude = null;
                                    
                                    // Vẫn tính phí ship mặc định (đã được cập nhật ở trên)
                                }
                                edtsdt.setText(documentSnapshot.getString("sdt"));
                            }
                        } else {
                            // Không có thông tin user, vẫn cập nhật giá trị dialog
                            updateDialogPrices();
                        }
                    }
                });

        // Set click listener for address selection button
        btnSelectAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(CartActivity.this, MapLocationOSMActivity.class);
                // Truyền địa chỉ hiện tại nếu có
                if (diachi != null && !diachi.isEmpty()) {
                    mapIntent.putExtra("existing_address", diachi);
                    Log.d("CartActivity", "Truyền địa chỉ hiện tại vào map: " + diachi);
                }
                mapLocationLauncher.launch(mapIntent);
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });

        btnxacnhan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Kiểm tra lại giỏ hàng ngay trước khi tạo đơn
                if (!isCartValid()) {
                    dialog.cancel();
                    return;
                }
                sanpham = "";
                for (Product product : listGiohang) {
                    sanpham += product.getTensp() + " x " + product.getSoluong() + "\n";
                }
                hoten = edthoten.getText().toString().trim();
                sdt = edtsdt.getText().toString().trim();
                ghichu = edtghichu.getText().toString().trim();

                if (hoten.length() > 0) {
                    if (diachi != null && diachi.length() > 0) {
                        if (sdt.length() > 0) {
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
                            Calendar calendar = Calendar.getInstance();
                            ngaydat = simpleDateFormat.format(calendar.getTime());
                            phuongthuc = spinnerPhuongthuc.getSelectedItem().toString();

                            if (spinnerPhuongthuc.getSelectedItemPosition() == 0) {
                                // Thanh toán khi nhận hàng
                                processCashOnDelivery(dialog);
                            } else {
                                // Thanh toán VNPay
                                dialog.cancel();
                                requestVNPayPayment();
                                DatHangGuiTinNhan();
                            }
                        } else {
                            Toast.makeText(CartActivity.this, "Số điện thoại không để trống", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    } else {
                        Toast.makeText(CartActivity.this, "Địa chỉ không để trống", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CartActivity.this, "Họ tên không để trống", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void processCashOnDelivery(Dialog dialog) {
        // Tính phí ship dựa trên địa chỉ và tọa độ
        long phivanchuyen = 0;
        if (diachi != null && !diachi.trim().isEmpty()) {
            phivanchuyen = com.UngDung.ThucPhamXanh.Helper.ShippingFeeHelper.calculateShippingFee(
                CartActivity.this, 
                diachi, 
                deliveryLatitude, 
                deliveryLongitude
            );
        } else {
            phivanchuyen = com.UngDung.ThucPhamXanh.Helper.ShippingFeeHelper.calculateShippingFeeByProvince("");
        }
        
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("ghichu", ghichu);
        hashMap.put("ngaydat", ngaydat);
        hashMap.put("diachi", diachi);
        hashMap.put("sdt", sdt);
        hashMap.put("hoten", hoten);
        hashMap.put("phuongthuc", phuongthuc);
        hashMap.put("tongtien", tienthanhtoan);
        hashMap.put("phivanchuyen", phivanchuyen); // Lưu phí ship vào hóa đơn
        hashMap.put("trangthai", 1);
        hashMap.put("UID", FirebaseAuth.getInstance().getCurrentUser().getUid());

        Log.d("CartActivity", "🔄 Bắt đầu tạo hóa đơn thanh toán khi nhận hàng...");
        db.collection("HoaDon")
                .add(hashMap)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            String idhoadon = task.getResult().getId();
                            Log.d("CartActivity", "✅ Tạo hóa đơn thanh toán khi nhận hàng thành công: " + idhoadon);

                            // Add chi tiết hóa đơn - Sửa cấu trúc để khớp với CTHDActivity
                            Log.d("CartActivity", "🔄 Bắt đầu tạo chi tiết hóa đơn...");
                            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            final int[] completedCount = {0};
                            final int totalProducts = listGiohang.size();
                            
                            for (Product sanPhamModels : listGiohang) {
                                // Lưu reference để dùng trong callback
                                final Product product = sanPhamModels;
                                
                                HashMap<String, Object> map_chitiet = new HashMap<>();
                                map_chitiet.put("id_hoadon", idhoadon); // Sửa từ idhoadon thành id_hoadon
                                map_chitiet.put("id_product", product.getIdsp()); // Sửa từ idsp thành id_product
                                map_chitiet.put("soluong", product.getSoluong());
                                map_chitiet.put("giaBan", product.getGiatien()); // giá lúc đặt (admin / hóa đơn dùng, không đổi theo giá mới)
                                
                                // Lưu vào đúng cấu trúc: ChitietHoaDon/{uid}/ALL
                                db.collection("ChitietHoaDon") // Sửa từ ChiTietHoaDon thành ChitietHoaDon
                                        .document(uid)
                                        .collection("ALL")
                                        .add(map_chitiet)
                                        .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                                if (task.isSuccessful()) {
                                                    completedCount[0]++;
                                                    Log.d("CartActivity", "✅ Đã thêm chi tiết hóa đơn: " + completedCount[0] + "/" + totalProducts + " - " + product.getTensp());
                                                } else {
                                                    Log.e("CartActivity", "❌ Lỗi thêm chi tiết hóa đơn: " + task.getException().getMessage());
                                                }
                                            }
                                        });
                            }

                            // Clear giỏ hàng
                            db.collection("GioHang")
                                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .collection("ALL").get()
                                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                            for (QueryDocumentSnapshot q : queryDocumentSnapshots) {
                                                db.collection("GioHang")
                                                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                        .collection("ALL")
                                                        .document(q.getId())
                                                        .delete();
                                            }
                                            giohangAdapter.notifyDataSetChanged();
                                            TongTienGioHang();
                                            scrollViewCart.setVisibility(View.GONE);
                                            emptyStateCart.setVisibility(View.VISIBLE);
                                            paymentButtonCard.setVisibility(View.GONE);
                                        }
                                    });

                            // Trừ số lượng sản phẩm
                            Log.d("CartActivity", "🔄 Bắt đầu trừ số lượng sản phẩm...");
                            updateProductQuantity();

                            // Chuyển đến OrderSuccessActivity
                            Intent intent = new Intent(CartActivity.this, OrderSuccessActivity.class);
                            intent.putExtra("idhoadon", idhoadon);
                            intent.putExtra("hoten", hoten);
                            intent.putExtra("diachi", diachi);
                            intent.putExtra("sdt", sdt);
                            intent.putExtra("ghichu", ghichu);
                            intent.putExtra("ngaydat", ngaydat);
                            intent.putExtra("phuongthuc", phuongthuc);
                            intent.putExtra("tienthanhtoan", tienthanhtoan);
                            intent.putExtra("sanpham", sanpham);
                            intent.putExtra("serialzable", listGiohang);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(CartActivity.this, "Lỗi tạo hóa đơn", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void TongTienGioHang() {
        // Tính đơn giá (tổng giá của tất cả sản phẩm)
        long dongia = 0;
        for (Product product : listGiohang) {
            dongia += product.getGiatien() * product.getSoluong();
        }
        
        // Hiển thị đơn giá
        tvDongia.setText(NumberFormat.getInstance().format(dongia));
        
        // Tính phí vận chuyển dựa trên địa chỉ và tọa độ
        long phivanchuyen = 0;
        if (diachi != null && !diachi.trim().isEmpty()) {
            phivanchuyen = com.UngDung.ThucPhamXanh.Helper.ShippingFeeHelper.calculateShippingFee(
                CartActivity.this, 
                diachi, 
                deliveryLatitude, 
                deliveryLongitude
            );
        } else {
            // Nếu chưa có địa chỉ, hiển thị phí ship mặc định (khác tỉnh)
            phivanchuyen = com.UngDung.ThucPhamXanh.Helper.ShippingFeeHelper.calculateShippingFeeByProvince("");
        }
        
        tvPhiVanChuyen.setText(NumberFormat.getInstance().format(phivanchuyen));
        
        // Tổng tiền = Đơn giá + Phí vận chuyển
        long tongtien = dongia + phivanchuyen;
        tvTongTien.setText(NumberFormat.getInstance().format(tongtien));
        
        // Cập nhật biến tienthanhtoan để dùng khi thanh toán
        tienthanhtoan = NumberFormat.getInstance().format(tongtien);
        
        Log.d("CartActivity", "📦 Đơn giá: " + dongia + ", Phí vận chuyển: " + phivanchuyen + ", Tổng: " + tongtien);
        if (diachi != null) {
            Log.d("CartActivity", "📍 Địa chỉ giao hàng: " + diachi);
        }
    }

    /**
     * Load khuyến mãi theo loại sản phẩm từ Firestore
     */
    private void loadPromotion() {
        db.collection("Promotions")
                .document("current_promotion")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Load category discounts
                        categoryDiscounts.clear();
                        Map<String, Object> categoryDiscountsMap = (Map<String, Object>) documentSnapshot.get("categoryDiscounts");
                        if (categoryDiscountsMap != null && !categoryDiscountsMap.isEmpty()) {
                            for (Map.Entry<String, Object> entry : categoryDiscountsMap.entrySet()) {
                                String categoryName = entry.getKey();
                                Object discountObj = entry.getValue();
                                
                                // Có thể là Map (mới) hoặc Number (cũ - để tương thích)
                                if (discountObj instanceof Map) {
                                    Map<String, Object> discountMap = (Map<String, Object>) discountObj;
                                    Integer minProducts = discountMap.get("minProducts") != null 
                                            ? ((Number) discountMap.get("minProducts")).intValue() : 0;
                                    Integer discountPercent = discountMap.get("discountPercent") != null 
                                            ? ((Number) discountMap.get("discountPercent")).intValue() : 0;
                                    if (minProducts > 0 && discountPercent > 0) {
                                        categoryDiscounts.put(categoryName, new CategoryDiscount(minProducts, discountPercent));
                                    }
                                } else if (discountObj instanceof Number) {
                                    // Tương thích với dữ liệu cũ (chỉ có discountPercent)
                                    int discountPercent = ((Number) discountObj).intValue();
                                    if (discountPercent > 0) {
                                        categoryDiscounts.put(categoryName, new CategoryDiscount(1, discountPercent));
                                    }
                                }
                            }
                            Log.d("CartActivity", "✅ Đã load khuyến mãi theo loại: " + categoryDiscounts.size() + " loại");
                        } else {
                            Log.d("CartActivity", "ℹ️ Chưa có khuyến mãi theo loại");
                        }
                        
                        // Cập nhật lại giá sau khi load khuyến mãi
                        updateDialogPrices();
                    } else {
                        categoryDiscounts.clear();
                        Log.d("CartActivity", "ℹ️ Chưa có chương trình khuyến mãi");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CartActivity", "❌ Lỗi load khuyến mãi: " + e.getMessage());
                    categoryDiscounts.clear();
                });
    }

    /**
     * Cập nhật giá trị trong dialog thanh toán (đơn giá, phí vận chuyển, tổng tiền)
     */
    private void updateDialogPrices() {
        if (tvDongiaDialog == null || tvPhiVanChuyenDialog == null || tvTongTienDialog == null) {
            return;
        }
        
        // Tính tổng số sản phẩm trong giỏ hàng
        int totalProducts = 0;
        for (Product product : listGiohang) {
            totalProducts += product.getSoluong();
        }
        
        // Tính đơn giá (tổng giá của tất cả sản phẩm)
        long dongia = 0;
        for (Product product : listGiohang) {
            dongia += product.getGiatien() * product.getSoluong();
        }
        
        // Hiển thị đơn giá trong dialog
        tvDongiaDialog.setText(NumberFormat.getInstance().format(dongia));
        
        // Tính khoảng cách và phí vận chuyển dựa trên địa chỉ
        long phivanchuyen = 0;
        double distance = -1;
        
        if (diachi != null && !diachi.trim().isEmpty()) {
            // Log để debug
            double[] storeCoords = com.UngDung.ThucPhamXanh.Helper.ShippingFeeHelper.getStoreCoordinates();
            Log.d("CartActivity", "═══════════════════════════════════════════════════════");
            Log.d("CartActivity", "📍 CỬA HÀNG:");
            Log.d("CartActivity", "   Địa chỉ: " + com.UngDung.ThucPhamXanh.Helper.ShippingFeeHelper.getStoreAddress());
            Log.d("CartActivity", "   Tọa độ: (" + storeCoords[0] + ", " + storeCoords[1] + ")");
            Log.d("CartActivity", "📍 ĐỊA CHỈ GIAO HÀNG:");
            Log.d("CartActivity", "   Địa chỉ: " + diachi);
            Log.d("CartActivity", "   Tọa độ từ map: lat=" + deliveryLatitude + ", lon=" + deliveryLongitude);
            Log.d("CartActivity", "═══════════════════════════════════════════════════════");
            
            // Lấy khoảng cách
            distance = com.UngDung.ThucPhamXanh.Helper.ShippingFeeHelper.getDistanceToStore(
                CartActivity.this, 
                diachi, 
                deliveryLatitude, 
                deliveryLongitude
            );
            
            Log.d("CartActivity", "📏 KHOẢNG CÁCH TÍNH ĐƯỢC: " + String.format("%.2f", distance) + " km");
            Log.d("CartActivity", "═══════════════════════════════════════════════════════");
            
            // Tính phí ship
            phivanchuyen = com.UngDung.ThucPhamXanh.Helper.ShippingFeeHelper.calculateShippingFee(
                CartActivity.this, 
                diachi,
                deliveryLatitude, 
                deliveryLongitude
            );
        } else {
            // Nếu chưa có địa chỉ, hiển thị phí ship mặc định (khác tỉnh)
            phivanchuyen = com.UngDung.ThucPhamXanh.Helper.ShippingFeeHelper.calculateShippingFeeByProvince("");
        }
        
        // Hiển thị khoảng cách
        if (distance >= 0 && layoutDistance != null && tvDistanceDialog != null) {
            String distanceText = String.format("%.1f km", distance);
            tvDistanceDialog.setText(distanceText);
            layoutDistance.setVisibility(View.VISIBLE);
            Log.d("CartActivity", "updateDialogPrices: Hiển thị khoảng cách: " + distanceText);
        } else if (layoutDistance != null) {
            layoutDistance.setVisibility(View.GONE);
            Log.d("CartActivity", "updateDialogPrices: Ẩn khoảng cách (distance = " + distance + ")");
        }
        
        // Hiển thị phí vận chuyển trong dialog
        tvPhiVanChuyenDialog.setText(NumberFormat.getInstance().format(phivanchuyen));
        
        // Tính giảm giá khuyến mãi theo loại sản phẩm
        long discountAmount = 0;
        StringBuilder promotionMessage = new StringBuilder();
        ArrayList<String> discountDetails = new ArrayList<>(); // Chi tiết giảm giá từng loại
        ArrayList<String> needMoreMessages = new ArrayList<>(); // Thông báo cần mua thêm
        
        if (!categoryDiscounts.isEmpty()) {
            HashMap<String, Long> categoryTotals = new HashMap<>(); // categoryName -> totalPrice
            HashMap<String, Integer> categoryCounts = new HashMap<>(); // categoryName -> count
            
            // Tính tổng giá và số lượng theo từng loại
            for (Product product : listGiohang) {
                String loaisp = product.getLoaisp();
                if (loaisp != null && categoryDiscounts.containsKey(loaisp)) {
                    long price = product.getGiatien() * product.getSoluong();
                    categoryTotals.put(loaisp, categoryTotals.getOrDefault(loaisp, 0L) + price);
                    categoryCounts.put(loaisp, categoryCounts.getOrDefault(loaisp, 0) + (int)product.getSoluong());
                }
            }
            
            // Tính giảm giá cho từng loại
            int categoryDiscountCount = 0;
            for (Map.Entry<String, CategoryDiscount> entry : categoryDiscounts.entrySet()) {
                String categoryName = entry.getKey();
                CategoryDiscount discount = entry.getValue();
                
                if (discount != null && categoryTotals.containsKey(categoryName)) {
                    int currentQuantity = categoryCounts.get(categoryName);
                    int minProducts = discount.getMinProducts();
                    int discountPercent = discount.getDiscountPercent();
                    
                    // Kiểm tra điều kiện số lượng tối thiểu
                    if (currentQuantity >= minProducts) {
                        // Đủ điều kiện, áp dụng giảm giá
                        long categoryTotal = categoryTotals.get(categoryName);
                        long categoryDiscount = (categoryTotal * discountPercent) / 100;
                        discountAmount += categoryDiscount;
                        
                        if (categoryDiscount > 0) {
                            categoryDiscountCount++;
                            String detail = "• " + categoryName + ": Giảm " + discountPercent + "% (" 
                                    + NumberFormat.getInstance().format(categoryDiscount) + " VNĐ)";
                            discountDetails.add(detail);
                        }
                    } else {
                        // Chưa đủ điều kiện, hiển thị thông báo cần mua thêm
                        int needMore = minProducts - currentQuantity;
                        needMoreMessages.add("💡 " + categoryName + ": Mua thêm " + needMore + " sản phẩm để được giảm " + discountPercent + "%");
                    }
                }
            }
            
            // Tạo thông báo khuyến mãi
            if (categoryDiscountCount > 0) {
                if (categoryDiscountCount == 1) {
                    // Chỉ có 1 loại được giảm giá
                    String categoryName = discountDetails.get(0).split(":")[0].substring(2); // Bỏ "• "
                    promotionMessage.append("🎉 Bạn được giảm giá cho loại ").append(categoryName);
                } else {
                    // Nhiều loại được giảm giá
                    promotionMessage.append("🎉 Bạn được giảm giá cho ").append(categoryDiscountCount).append(" loại sản phẩm!");
                }
            }
            
            // Thêm thông báo cần mua thêm nếu có
            if (!needMoreMessages.isEmpty()) {
                if (promotionMessage.length() > 0) {
                    promotionMessage.append("\n\n");
                }
                for (String msg : needMoreMessages) {
                    promotionMessage.append(msg).append("\n");
                }
                // Xóa dòng cuối cùng (dòng trống)
                if (promotionMessage.length() > 0 && promotionMessage.charAt(promotionMessage.length() - 1) == '\n') {
                    promotionMessage.setLength(promotionMessage.length() - 1);
                }
            }
        }
        
        // Hiển thị thông báo khuyến mãi
        if (tvPromotionMessage != null && cardPromotionMessage != null) {
            if (promotionMessage.length() > 0) {
                // Nếu có chi tiết giảm giá từng loại, thêm vào thông báo
                if (!discountDetails.isEmpty() && discountDetails.size() > 1) {
                    promotionMessage.append("\n\n");
                    for (String detail : discountDetails) {
                        promotionMessage.append(detail).append("\n");
                    }
                    // Xóa dòng cuối cùng (dòng trống)
                    if (promotionMessage.length() > 0 && promotionMessage.charAt(promotionMessage.length() - 1) == '\n') {
                        promotionMessage.setLength(promotionMessage.length() - 1);
                    }
                } else if (!discountDetails.isEmpty() && discountDetails.size() == 1) {
                    // Chỉ có 1 loại, thêm chi tiết vào cùng dòng
                    String detail = discountDetails.get(0);
                    String discountInfo = detail.substring(detail.indexOf(":") + 1).trim();
                    promotionMessage.append(" - ").append(discountInfo);
                }
                
                tvPromotionMessage.setText(promotionMessage.toString());
                cardPromotionMessage.setVisibility(View.VISIBLE);
            } else {
                cardPromotionMessage.setVisibility(View.GONE);
            }
        }
        
        // Hiển thị số tiền giảm
        if (layoutDiscount != null && tvDiscountDialog != null) {
            if (discountAmount > 0) {
                layoutDiscount.setVisibility(View.VISIBLE);
                tvDiscountDialog.setText("-" + NumberFormat.getInstance().format(discountAmount));
            } else {
                layoutDiscount.setVisibility(View.GONE);
            }
        }
        
        // Tổng tiền = Đơn giá + Phí vận chuyển - Giảm giá
        long tongtien = dongia + phivanchuyen - discountAmount;
        if (tongtien < 0) {
            tongtien = 0; // Đảm bảo tổng tiền không âm
        }
        tvTongTienDialog.setText(NumberFormat.getInstance().format(tongtien));
        
        // Cập nhật biến tienthanhtoan để dùng khi thanh toán
        tienthanhtoan = NumberFormat.getInstance().format(tongtien);
        
        Log.d("CartActivity", "💰 Dialog - Đơn giá: " + dongia + ", Phí vận chuyển: " + phivanchuyen 
                + ", Giảm giá: " + discountAmount + ", Tổng: " + tongtien);
        if (diachi != null) {
            Log.d("CartActivity", "📍 Dialog - Địa chỉ giao hàng: " + diachi);
        }
    }

    public void DeleteDataGioHang() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                confirmRemoveItem(viewHolder.getAdapterPosition());
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(rcvGioHang);
    }

    /** Hỏi xác nhận rồi xóa sản phẩm ở vị trí pos khỏi giỏ hàng (dùng cho vuốt xóa và nút trừ khi số lượng = 1). */
    public void confirmRemoveItem(int pos) {
        if (pos < 0 || pos >= listGiohang.size()) {
            giohangAdapter.notifyDataSetChanged();
            return;
        }
        Product item = listGiohang.get(pos);
        new AlertDialog.Builder(CartActivity.this)
                .setMessage("Bạn có muốn xóa sản phẩm " + item.getTensp() + " khỏi giỏ hàng không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    gioHangPresenter.HandleDeleteDataGioHang(item.getId());
                    listGiohang.remove(item);
                    giohangAdapter.notifyDataSetChanged();
                    TongTienGioHang();
                    updateCartVisibility();
                })
                .setNegativeButton("Hủy", (dialog, which) -> giohangAdapter.notifyDataSetChanged())
                .setOnCancelListener(dialog -> giohangAdapter.notifyDataSetChanged())
                .show();
    }

    /** Hiện danh sách + nút thanh toán khi giỏ có hàng, ngược lại hiện màn hình giỏ trống. */
    private void updateCartVisibility() {
        boolean hasItems = !listGiohang.isEmpty();
        scrollViewCart.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        emptyStateCart.setVisibility(hasItems ? View.GONE : View.VISIBLE);
        paymentButtonCard.setVisibility(hasItems ? View.VISIBLE : View.GONE);
    }

    /** Giỏ hàng hợp lệ để thanh toán: có ít nhất 1 sản phẩm và mọi sản phẩm có số lượng >= 1. */
    private boolean isCartValid() {
        if (listGiohang.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng của bạn đang trống !", Toast.LENGTH_SHORT).show();
            return false;
        }
        for (Product p : listGiohang) {
            if (p.getSoluong() < 1) {
                Toast.makeText(this, "Số lượng sản phẩm " + p.getTensp() + " không hợp lệ", Toast.LENGTH_SHORT).show();
                return false;
            }
            // Giỏ mở từ hôm trước: kiểm tra lại hạn dùng ngay lúc thanh toán
            if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isExpired(p.getHansudung(), java.time.LocalDate.now())) {
                Toast.makeText(this, "\"" + p.getTensp() + "\" đã hết hạn sử dụng, vui lòng xóa khỏi giỏ",
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    public void DatHangGuiTinNhan() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        HashMap<String, Object> map = new HashMap<>();
        map.put("sender", com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID); // Người gửi đang fix cứng là id của admin
        map.put("receiver", FirebaseAuth.getInstance().getCurrentUser().getUid());
        String donhang = "";
        for (Product product : listGiohang) {
            donhang += product.getTensp() + " x " + product.getSoluong() + "\n";
        }
        String mess = "Đơn hàng của bạn: " + "\n" + donhang + "\n" + "Ngày đặt: " + ngaydat + "\n" + "Địa chỉ: "
                + diachi + "\n" + "SĐT: " + sdt
                + "\n" + "Người nhận: " + hoten + "\n" + "Phương thức thanh toán: " + phuongthuc
                + "\n" + "Tổng tiền: " + tienthanhtoan;
        map.put("message", mess);
        map.put("isseen", false);

        // Gửi qua Firestore "Chats" (màn chat đọc từ Firestore, không phải Realtime Database)
        new com.UngDung.ThucPhamXanh.Helper.ChatFirestoreHelper().sendMessage(
                com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID, (String) map.get("receiver"), (String) map.get("message"), null);

        // Add user to chat fragment
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    chatRef.child("id").setValue(com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void OnSucess() {

    }

    @Override
    public void OnFail() {

    }

    @Override
    public void getDataSanPham(String id, String idsp, String tensp, Long giatien, String hinhanh, String loaisp,
            String mota, Long soluong, String hansudung, Long type, String trongluong) {
        try {
            // Bỏ qua (và xóa khỏi giỏ) sản phẩm không còn tồn tại hoặc có số lượng <= 0 (dữ liệu lỗi cũ)
            if (tensp == null || giatien == null || soluong == null || soluong < 1) {
                Log.w("CartActivity", "Bỏ sản phẩm lỗi khỏi giỏ: id=" + id + ", soluong=" + soluong);
                gioHangPresenter.HandleDeleteDataGioHang(id);
                return;
            }
            // Hết hạn sử dụng: ngừng bán → gỡ khỏi giỏ và báo cho khách biết
            if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isExpired(hansudung, java.time.LocalDate.now())) {
                Log.w("CartActivity", "Gỡ sản phẩm hết hạn khỏi giỏ: " + tensp + " (HSD " + hansudung + ")");
                gioHangPresenter.HandleDeleteDataGioHang(id);
                Toast.makeText(this, "Đã gỡ \"" + tensp + "\" khỏi giỏ vì sản phẩm đã hết hạn sử dụng",
                        Toast.LENGTH_LONG).show();
                return;
            }
            listGiohang.add(
                    new Product(id, idsp, tensp, giatien, hinhanh, loaisp, mota, soluong, hansudung, type, trongluong));
            product = new Product(id, idsp, tensp, giatien, hinhanh, loaisp, mota, soluong, hansudung, type,
                    trongluong);
            Log.d("product",
                    "Sản phẩm: " + product.getId() + product.getTensp() + product.getSoluong() + product.getGiatien());

            updateCartVisibility();

            // Chỉ set layout manager và adapter một lần
            if (rcvGioHang.getLayoutManager() == null) {
                rcvGioHang.setLayoutManager(new LinearLayoutManager(CartActivity.this));
                rcvGioHang.setAdapter(giohangAdapter);
            }

            // Cập nhật adapter
            giohangAdapter.notifyDataSetChanged();
            TongTienGioHang();
        } catch (Exception e) {
            Log.d("ERROR", e.getMessage());
        }
    }

    public void requestVNPayPayment() {
        try {
            // Parse số tiền
            long amount = 0;
            try {
                number = NumberFormat.getInstance().parse(tienthanhtoan.replace(",", ""));
                amount = number.longValue();
            } catch (ParseException e) {
                Log.e("CartActivity", "Error parsing amount: " + e.getMessage());
                Toast.makeText(this, "Lỗi tính toán số tiền", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Tạo order ID
            String orderId = "ORDER_" + System.currentTimeMillis();
            String orderInfo = "Thanh toan don hang " + orderId;
            
            // Tạo payment URL (không chọn ngân hàng cụ thể, để VNPay hiển thị tất cả options)
            String paymentUrl = VNPayHelper.createPaymentUrl(amount, orderId, orderInfo, null);
            
            if (paymentUrl == null || paymentUrl.isEmpty()) {
                Toast.makeText(this, "Lỗi tạo URL thanh toán", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Mở VNPayPaymentActivity
            Intent intent = new Intent(CartActivity.this, VNPayPaymentActivity.class);
            intent.putExtra("payment_url", paymentUrl);
            intent.putExtra("order_id", orderId);
            startActivityForResult(intent, 1001); // Request code cho VNPay
            
        } catch (Exception e) {
            Log.e("CartActivity", "Error creating VNPay payment: " + e.getMessage());
            Toast.makeText(this, "Lỗi tạo thanh toán VNPay", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) { // VNPay payment request code
            if (resultCode == RESULT_OK && data != null) {
                String status = data.getStringExtra("status");
                String message = data.getStringExtra("message");
                String orderId = data.getStringExtra("order_id");
                
                if ("success".equals(status)) {
                    // Thanh toán thành công
                    Log.d("CartActivity", "✅ Thanh toán VNPay thành công!");
                    Toast.makeText(this, "Thanh toán thành công", Toast.LENGTH_SHORT).show();
                    // Xử lý logic sau khi thanh toán thành công
                    processOrderSuccess(orderId != null ? orderId : "VNPAY_" + System.currentTimeMillis());
                } else {
                    // Thanh toán thất bại
                    Log.d("CartActivity", "❌ Thanh toán VNPay thất bại: " + message);
                    Toast.makeText(this, message != null ? message : "Thanh toán thất bại", Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Thanh toán bị hủy", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processOrderSuccess(String idhoadon) {
        // Logic xử lý sau khi thanh toán thành công
        Log.d("CartActivity", "🔄 Gọi updateProductQuantity() - Case 2 (Thanh toán VNPay)");
        updateProductQuantity();

        // Tạo hóa đơn cho thanh toán VNPay
        createInvoiceForVNPay(idhoadon);
    }

    private void createInvoiceForVNPay(String idhoadon) {
        // Tính phí ship dựa trên địa chỉ và tọa độ
        long phivanchuyen = 0;
        if (diachi != null && !diachi.trim().isEmpty()) {
            phivanchuyen = com.UngDung.ThucPhamXanh.Helper.ShippingFeeHelper.calculateShippingFee(
                CartActivity.this, 
                diachi, 
                deliveryLatitude, 
                deliveryLongitude
            );
        } else {
            phivanchuyen = com.UngDung.ThucPhamXanh.Helper.ShippingFeeHelper.calculateShippingFeeByProvince("");
        }
        
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("ghichu", ghichu);
        hashMap.put("ngaydat", ngaydat);
        hashMap.put("diachi", diachi);
        hashMap.put("sdt", sdt);
        hashMap.put("hoten", hoten);
        hashMap.put("phuongthuc", phuongthuc);
        hashMap.put("tongtien", tienthanhtoan);
        hashMap.put("phivanchuyen", phivanchuyen); // Lưu phí ship vào hóa đơn
        hashMap.put("trangthai", 1);
        hashMap.put("UID", FirebaseAuth.getInstance().getCurrentUser().getUid());

        db.collection("HoaDon")
                .add(hashMap)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            String realIdhoadon = task.getResult().getId();
                            Log.d("CartActivity", "✅ Tạo hóa đơn VNPay thành công: " + realIdhoadon);

                            // Add chi tiết hóa đơn - Sửa cấu trúc để khớp với CTHDActivity
                            Log.d("CartActivity", "🔄 Bắt đầu tạo chi tiết hóa đơn cho VNPay...");
                            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            final int[] completedCount = {0};
                            final int totalProducts = listGiohang.size();
                            
                            for (Product product : listGiohang) {
                                HashMap<String, Object> chiTietHoaDon = new HashMap<>();
                                chiTietHoaDon.put("id_hoadon", realIdhoadon); // Sửa từ idhoadon thành id_hoadon
                                chiTietHoaDon.put("id_product", product.getIdsp()); // Sửa từ idsp thành id_product
                                chiTietHoaDon.put("soluong", product.getSoluong());
                                chiTietHoaDon.put("giaBan", product.getGiatien()); // giá lúc đặt

                                // Lưu vào đúng cấu trúc: ChitietHoaDon/{uid}/ALL
                                db.collection("ChitietHoaDon") // Sửa từ ChiTietHoaDon thành ChitietHoaDon
                                        .document(uid)
                                        .collection("ALL")
                                        .add(chiTietHoaDon)
                                        .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                                if (task.isSuccessful()) {
                                                    completedCount[0]++;
                                                    Log.d("CartActivity", "✅ Đã thêm chi tiết hóa đơn VNPay: " + completedCount[0] + "/" + totalProducts + " - " + product.getTensp());
                                                } else {
                                                    Log.e("CartActivity", "❌ Lỗi thêm chi tiết hóa đơn VNPay: " + task.getException().getMessage());
                                                }
                                            }
                                        });
                            }

                            // Xóa giỏ hàng
                            db.collection("GioHang").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .collection("ALL").get()
                                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                            for (QueryDocumentSnapshot q : queryDocumentSnapshots) {
                                                db.collection("GioHang")
                                                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                        .collection("ALL").document(q.getId())
                                                        .delete();
                                            }
                                            giohangAdapter.notifyDataSetChanged();
                                            TongTienGioHang();
                                            scrollViewCart.setVisibility(View.GONE);
                                            emptyStateCart.setVisibility(View.VISIBLE);
                                            paymentButtonCard.setVisibility(View.GONE);
                                        }
                                    });

                            // Chuyển đến OrderSuccessActivity
                            Intent intent = new Intent(CartActivity.this, OrderSuccessActivity.class);
                            intent.putExtra("idhoadon", realIdhoadon);
                            intent.putExtra("hoten", hoten);
                            intent.putExtra("diachi", diachi);
                            intent.putExtra("sdt", sdt);
                            intent.putExtra("ghichu", ghichu);
                            intent.putExtra("ngaydat", ngaydat);
                            intent.putExtra("phuongthuc", phuongthuc);
                            intent.putExtra("tienthanhtoan", tienthanhtoan);
                            intent.putExtra("sanpham", sanpham);
                            intent.putExtra("serialzable", listGiohang);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.e("CartActivity", "❌ Lỗi tạo hóa đơn VNPay: " + task.getException());
                            Toast.makeText(CartActivity.this, "Lỗi tạo hóa đơn", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
