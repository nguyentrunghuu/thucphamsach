package com.UngDung.ThucPhamXanh.View.Admin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.ChatActivity;
import com.UngDung.ThucPhamXanh.View.SignInActivity;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class AdminHomeActivity extends AppCompatActivity {

    private static final String TAG = "AdminHomeActivity";
    private CardView cHoaDon, cThongKe, cAddProduct, cSignOut, cAddUser, cChat, cPromotion, cShipper, cLoaiSP;
    private TextView tvTotalOrders, tvTotalRevenue, tvTotalProducts, tvTotalUsers;
    private FirebaseFirestore db;
    private ListenerRegistration ordersListener, productsListener, usersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_admin_home);
            Log.d(TAG, "✅ Layout loaded successfully");

            db = FirebaseFirestore.getInstance();
            Init();
            Event();
            loadStatistics();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi khởi tạo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void Event() {
        cChat.setOnClickListener(view -> {
            Intent intent = new Intent(AdminHomeActivity.this, ChatActivity.class);
            intent.putExtra("message", 1);
            startActivity(intent);
        });

        cAddUser.setOnClickListener(view -> {
            Intent intent = new Intent(AdminHomeActivity.this, AdminUsersActivity.class);
            startActivity(intent);
        });

        cHoaDon.setOnClickListener(view -> {
            Intent intent = new Intent(AdminHomeActivity.this, AdminBillMainActivity.class);
            startActivity(intent);
        });

        cThongKe.setOnClickListener(view -> {
            Intent intent = new Intent(AdminHomeActivity.this, AdminChartBillActivity.class);
            startActivity(intent);
        });

        cAddProduct.setOnClickListener(view -> {
            Intent intent = new Intent(AdminHomeActivity.this, AdminProductActivity.class);
            startActivity(intent);
        });

        cPromotion.setOnClickListener(view -> {
            Intent intent = new Intent(AdminHomeActivity.this, AdminPromotionActivity.class);
            startActivity(intent);
        });

        cShipper.setOnClickListener(view -> {
            Intent intent = new Intent(AdminHomeActivity.this, AdminShipperActivity.class);
            startActivity(intent);
        });

        cLoaiSP.setOnClickListener(view -> {
            Intent intent = new Intent(AdminHomeActivity.this, AdminLoaiSPActivity.class);
            startActivity(intent);
        });

        cSignOut.setOnClickListener(view -> {
            Intent intent = new Intent(AdminHomeActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void Init() {
        cChat = findViewById(R.id.cChat);
        cAddUser = findViewById(R.id.cAddUser);
        cHoaDon = findViewById(R.id.cHoaDon);
        cThongKe = findViewById(R.id.cThongKe);
        cAddProduct = findViewById(R.id.cAddProduct);
        cPromotion = findViewById(R.id.cPromotion);
        cShipper = findViewById(R.id.cShipper);
        cLoaiSP = findViewById(R.id.cLoaiSP);
        cSignOut = findViewById(R.id.cSignOut);

        tvTotalOrders = findViewById(R.id.tv_total_orders);
        tvTotalRevenue = findViewById(R.id.tv_total_revenue);
        tvTotalProducts = findViewById(R.id.tv_total_products);
        tvTotalUsers = findViewById(R.id.tv_total_users);

        Log.d(TAG, "✅ All views initialized");
    }

    private void loadStatistics() {
        // Setup realtime listeners
        setupRealtimeListeners();
    }

    private void setupRealtimeListeners() {
        // Orders listener
        if (ordersListener != null) {
            ordersListener.remove();
        }
        ordersListener = db.collection("HoaDon")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "❌ Error listening to orders", e);
                            return;
                        }
                        if (snapshots != null) {
                            int totalOrders = snapshots.size();
                            tvTotalOrders.setText(String.valueOf(totalOrders));
                            
                            // Calculate revenue
                            long totalRevenue = 0;
                            for (QueryDocumentSnapshot doc : snapshots) {
                                // Doanh thu chỉ tính đơn ĐÃ GIAO (không tính đơn hủy / chưa giao)
                                Long st = doc.getLong("trangthai");
                                if (st == null || st != com.UngDung.ThucPhamXanh.Models.OrderStatus.DA_GIAO) continue;
                                totalRevenue += com.UngDung.ThucPhamXanh.Helper.MoneyUtil.parseVnd(doc.get("tongtien"));
                            }
                            tvTotalRevenue.setText(NumberFormat.getInstance(new java.util.Locale("vi", "VN")).format(totalRevenue));
                        }
                    }
                });

        // Products listener
        if (productsListener != null) {
            productsListener.remove();
        }
        productsListener = db.collection("SanPham")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "❌ Error listening to products", e);
                            return;
                        }
                        if (snapshots != null) {
                            // Không đếm sản phẩm đã bị xóa (ẩn)
                            int totalProducts = 0;
                            for (QueryDocumentSnapshot doc : snapshots) {
                                if (!com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHidden(doc)) totalProducts++;
                            }
                            tvTotalProducts.setText(String.valueOf(totalProducts));
                        }
                    }
                });

        // Users listener
        if (usersListener != null) {
            usersListener.remove();
        }
        usersListener = db.collection("IDUser")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "❌ Error listening to users", e);
                            return;
                        }
                        if (snapshots != null) {
                            int totalUsers = snapshots.size();
                            tvTotalUsers.setText(String.valueOf(totalUsers));
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) {
            ordersListener.remove();
        }
        if (productsListener != null) {
            productsListener.remove();
        }
        if (usersListener != null) {
            usersListener.remove();
        }
    }
}