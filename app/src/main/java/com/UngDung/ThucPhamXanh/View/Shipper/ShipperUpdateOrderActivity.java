package com.UngDung.ThucPhamXanh.View.Shipper;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Models.HoaDon;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IHoaDon;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.NotNull;

public class ShipperUpdateOrderActivity extends AppCompatActivity implements IHoaDon {

    private static final String TAG = "ShipperUpdateOrder";
    private TextView tvOrderId, tvCustomerName, tvAddress, tvPhone, tvTotal, tvDate, tvPaymentMethod, tvNote;
    private Button btnStartDelivery, btnCompleteDelivery;
    private ImageView imgBackUpdate;
    private String orderId, shipperUsername;
    private long currentStatus;
    private HoaDon hoaDon;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipper_update_order);

        Intent intent = getIntent();
        orderId = intent.getStringExtra("order_id");
        shipperUsername = intent.getStringExtra("shipper_username");
        if (shipperUsername == null || shipperUsername.isEmpty()) {
            // Fallback: try to get from bundle
            Bundle extras = intent.getExtras();
            if (extras != null) {
                shipperUsername = extras.getString("shipper_username");
            }
            // If still null, show error
            if (shipperUsername == null || shipperUsername.isEmpty()) {
                Toast.makeText(this, "Lỗi: Không có thông tin shipper", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        currentStatus = intent.getLongExtra("status", 5);

        InitWidget();
        LoadOrderData();
        Event();
    }

    private void InitWidget() {
        tvOrderId = findViewById(R.id.tv_order_id_detail);
        tvCustomerName = findViewById(R.id.tv_customer_name_detail);
        tvAddress = findViewById(R.id.tv_address_detail);
        tvPhone = findViewById(R.id.tv_phone_detail);
        tvTotal = findViewById(R.id.tv_total_detail);
        tvDate = findViewById(R.id.tv_date_detail);
        tvPaymentMethod = findViewById(R.id.tv_payment_method_detail);
        tvNote = findViewById(R.id.tv_note_detail);
        btnStartDelivery = findViewById(R.id.btn_start_delivery);
        btnCompleteDelivery = findViewById(R.id.btn_complete_delivery);
        imgBackUpdate = findViewById(R.id.img_back_update);

        // Set button visibility based on status
        if (currentStatus == 5) {
            // Đã xác nhận -> có thể bắt đầu giao hàng
            btnStartDelivery.setVisibility(View.VISIBLE);
            btnCompleteDelivery.setVisibility(View.GONE);
        } else if (currentStatus == 4) {
            // Đang giao hàng -> có thể hoàn thành
            btnStartDelivery.setVisibility(View.GONE);
            btnCompleteDelivery.setVisibility(View.VISIBLE);
        }
    }

    private void LoadOrderData() {
        tvOrderId.setText("Mã đơn: " + orderId);
        tvCustomerName.setText("Người nhận: " + getIntent().getStringExtra("customer_name"));
        tvAddress.setText("Địa chỉ: " + getIntent().getStringExtra("address"));
        tvPhone.setText("SĐT: " + getIntent().getStringExtra("phone"));
        tvTotal.setText("Tổng tiền: " + getIntent().getStringExtra("total") + " VNĐ");
        tvDate.setText("Ngày đặt: " + getIntent().getStringExtra("date"));
        tvPaymentMethod.setText("Phương thức: " + getIntent().getStringExtra("payment_method"));
        String note = getIntent().getStringExtra("note");
        if (note != null && !note.isEmpty()) {
            tvNote.setText("Ghi chú: " + note);
            tvNote.setVisibility(View.VISIBLE);
        } else {
            tvNote.setVisibility(View.GONE);
        }
    }

    private void Event() {
        imgBackUpdate.setOnClickListener(view -> finish());

        btnStartDelivery.setOnClickListener(view -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có chắc chắn muốn nhận và bắt đầu giao đơn hàng này?")
                    .setPositiveButton("Có", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Chuyển từ "Đã xác nhận" (5) -> "Đang giao hàng" (4)
                            updateOrderStatus(4, "Đang giao hàng");
                        }
                    })
                    .setNegativeButton("Không", null)
                    .show();
        });

        btnCompleteDelivery.setOnClickListener(view -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có chắc chắn đã giao hàng thành công?")
                    .setPositiveButton("Có", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            updateOrderStatus(3, "Giao thành công"); // 3 = Đã giao
                        }
                    })
                    .setNegativeButton("Không", null)
                    .show();
        });
    }

    private void updateOrderStatus(int newStatus, String statusText) {
        hoaDon = new HoaDon(this);
        hoaDon.HandleUpdateStatusBill(newStatus, orderId);
        
        // Cập nhật shipperId khi bắt đầu giao hàng
        if (newStatus == 4) {
            db.collection("HoaDon")
                    .document(orderId)
                    .update("shipperId", shipperUsername)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NotNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "✅ Đã cập nhật shipperId: " + shipperUsername);
                            }
                        }
                    });
        }
    }

    @Override
    public void getDataHD(String id, String uid, String ghichu, String diachi, String hoten, String ngaydat, String phuongthuc, String sdt, String tongtien, Long trangthai) {
        // Not used here
    }

    @Override
    public void OnSucess() {
        Toast.makeText(this, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void OnFail() {
        Toast.makeText(this, "Lỗi cập nhật trạng thái", Toast.LENGTH_SHORT).show();
    }
}

