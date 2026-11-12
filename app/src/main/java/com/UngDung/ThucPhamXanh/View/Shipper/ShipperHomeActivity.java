package com.UngDung.ThucPhamXanh.View.Shipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Models.HoaDon;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.SignInActivity;
import com.UngDung.ThucPhamXanh.Adapter.ShipperOrderAdapter;
import com.UngDung.ThucPhamXanh.my_interface.IHoaDon;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ShipperHomeActivity extends AppCompatActivity implements IHoaDon {

    private static final String TAG = "ShipperHomeActivity";
    private RecyclerView rcvShipperOrders;
    private ImageView imgBackShipper, imgLogoutShipper;
    private TextView tvShipperName, tvEmptyOrders;
    private ShipperOrderAdapter adapter;
    private ArrayList<HoaDon> listOrders;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String shipperUsername;

    public String getShipperUsername() {
        return shipperUsername;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipper_home);

        shipperUsername = getIntent().getStringExtra("shipper_username");
        if (shipperUsername == null || shipperUsername.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không có thông tin shipper", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        InitWidget();
        LoadShipperInfo();
        LoadOrders();
        Event();
    }

    private void InitWidget() {
        rcvShipperOrders = findViewById(R.id.rcv_shipper_orders);
        imgBackShipper = findViewById(R.id.img_back_shipper);
        imgLogoutShipper = findViewById(R.id.img_logout_shipper);
        tvShipperName = findViewById(R.id.tv_shipper_name);
        tvEmptyOrders = findViewById(R.id.tv_empty_orders);

        listOrders = new ArrayList<>();
        adapter = new ShipperOrderAdapter(this, listOrders, this);
        rcvShipperOrders.setLayoutManager(new LinearLayoutManager(this));
        rcvShipperOrders.setAdapter(adapter);
    }

    private void LoadShipperInfo() {
        db.collection("Shipper")
                .whereEqualTo("username", shipperUsername)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.size() > 0) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            tvShipperName.setText("Xin chào, " + name);
                        } else {
                            tvShipperName.setText("Xin chào, " + shipperUsername);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi load thông tin shipper: " + e.getMessage());
                });
    }

    private void LoadOrders() {
        // Load các đơn hàng có trạng thái: 5 (Đã xác nhận), 4 (Đang giao hàng)
        // Shipper chỉ thấy đơn hàng đã được xác nhận hoặc đang được mình giao
        db.collection("HoaDon")
                .whereIn("trangthai", java.util.Arrays.asList(5L, 4L))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listOrders.clear();
                    if (queryDocumentSnapshots.size() > 0) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String shipperId = doc.getString("shipperId");
                            // Chỉ hiển thị đơn hàng chưa có shipper hoặc đơn hàng của shipper này
                            if (shipperId == null || shipperId.isEmpty() || shipperId.equals(shipperUsername)) {
                                HoaDon hoaDon = new HoaDon(this);
                                hoaDon.setId(doc.getId());
                                hoaDon.setUid(doc.getString("UID"));
                                hoaDon.setDiachi(doc.getString("diachi"));
                                hoaDon.setHoten(doc.getString("hoten"));
                                hoaDon.setNgaydat(doc.getString("ngaydat"));
                                hoaDon.setPhuongthuc(doc.getString("phuongthuc"));
                                hoaDon.setSdt(doc.getString("sdt"));
                                hoaDon.setTongtien(doc.getString("tongtien"));
                                hoaDon.setGhichu(doc.getString("ghichu"));
                                Long trangthai = doc.getLong("trangthai");
                                if (trangthai != null) {
                                    hoaDon.setType(trangthai);
                                }
                                listOrders.add(hoaDon);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    } else {
                        updateEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi load đơn hàng: " + e.getMessage());
                    Toast.makeText(this, "Lỗi tải danh sách đơn hàng", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    private void updateEmptyState() {
        if (listOrders.isEmpty()) {
            tvEmptyOrders.setVisibility(View.VISIBLE);
            rcvShipperOrders.setVisibility(View.GONE);
        } else {
            tvEmptyOrders.setVisibility(View.GONE);
            rcvShipperOrders.setVisibility(View.VISIBLE);
        }
    }

    private void Event() {
        imgBackShipper.setOnClickListener(view -> finish());

        imgLogoutShipper.setOnClickListener(view -> {
            Intent intent = new Intent(ShipperHomeActivity.this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload đơn hàng khi quay lại màn hình
        LoadOrders();
    }

    @Override
    public void getDataHD(String id, String uid, String ghichu, String diachi, String hoten, String ngaydat, String phuongthuc, String sdt, String tongtien, Long trangthai) {
        // Không cần implement vì đã load trực tiếp từ Firestore
    }

    @Override
    public void OnSucess() {
        Toast.makeText(this, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
        LoadOrders(); // Reload danh sách
    }

    @Override
    public void OnFail() {
        Toast.makeText(this, "Lỗi cập nhật trạng thái", Toast.LENGTH_SHORT).show();
    }
}

