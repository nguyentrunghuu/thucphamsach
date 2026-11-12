package com.UngDung.ThucPhamXanh.View.Admin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import com.UngDung.ThucPhamXanh.Models.Product;
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

public class AdminProductActivity extends AppCompatActivity {

    private RecyclerView rcvAdminProduct;
    private AppCompatImageView imgAddProduct;
    private ArrayList<Product> mlistProduct;
    private AdminProductAdapter adapter;
    private ImageView imgBackAdminProduct;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Spinner spinnerLoaiSP;
    private ArrayList<String> mlistLoaiSP;
    private String category = "Tất cả";
    private ListenerRegistration productListener;
    private ListenerRegistration categoryListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_admin_product);

        InitWidget();
        Init();
        Event();
        setupRealtimeListeners();
    }

    private void Event() {
        imgAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminProductActivity.this, AdminAddSPActivity.class);
                startActivity(intent);
            }
        });

        imgBackAdminProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }


    private void Init() {
        mlistProduct = new ArrayList<>();
        mlistLoaiSP = new ArrayList<>();
        mlistLoaiSP.add("Tất cả");
        
        // Initialize adapter
        adapter = new AdminProductAdapter(AdminProductActivity.this, mlistProduct, new IClickCTHD() {
            @Override
            public void onClickCTHD(int pos) {
                Product product = mlistProduct.get(pos);
                Intent intent = new Intent(AdminProductActivity.this, AdminAddSPActivity.class);
                intent.putExtra("SP", product);
                startActivity(intent);
            }
        });
        rcvAdminProduct.setLayoutManager(new LinearLayoutManager(AdminProductActivity.this, RecyclerView.VERTICAL, false));
        rcvAdminProduct.setAdapter(adapter);
        
        // Setup category spinner with realtime listener
        setupCategoryRealtimeListener();
        
        // Setup spinner listener
        spinnerLoaiSP.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String s = mlistLoaiSP.get(i);
                category = s;
                setupProductRealtimeListener();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }
    
    private void setupCategoryRealtimeListener() {
        if (categoryListener != null) {
            categoryListener.remove();
        }
        
        categoryListener = db.collection("LoaiProduct")
                .orderBy("tenloai", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }
                        
                        if (snapshots != null) {
                            mlistLoaiSP.clear();
                            mlistLoaiSP.add("Tất cả");
                            
                            for (QueryDocumentSnapshot doc : snapshots) {
                                mlistLoaiSP.add(doc.getString("tenloai"));
                            }
                            
                            ArrayAdapter arrayAdapter = new ArrayAdapter(
                                AdminProductActivity.this, 
                                androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, 
                                mlistLoaiSP
                            );
                            arrayAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                            spinnerLoaiSP.setAdapter(arrayAdapter);
                            
                            // Restore selection
                            if (category != null && !category.equals("Tất cả")) {
                                for (int i = 0; i < mlistLoaiSP.size(); i++) {
                                    if (mlistLoaiSP.get(i).equals(category)) {
                                        spinnerLoaiSP.setSelection(i);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                });
    }
    
    private void setupProductRealtimeListener() {
        // Remove existing listener
        if (productListener != null) {
            productListener.remove();
        }
        
        // Clear the list when filter changes
        mlistProduct.clear();
        adapter.notifyDataSetChanged();
        
        Query query;
        if (category == null || category.equals("Tất cả")) {
            query = db.collection("SanPham");
        } else {
            query = db.collection("SanPham").whereEqualTo("loaisp", category);
        }
        
        productListener = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    return;
                }
                
                if (snapshots != null) {
                    // Dựng lại danh sách từ snapshot (bỏ qua sản phẩm đã bị ẩn/xóa mềm)
                    mlistProduct.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHidden(doc)) continue;
                        mlistProduct.add(documentToProduct(doc));
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }
    
    private void setupRealtimeListeners() {
        setupProductRealtimeListener();
    }
    
    private Product documentToProduct(DocumentSnapshot document) {
        return new Product(
            document.getId(),
            document.getString("tensp"),
            document.getLong("giatien"),
            document.getString("hinhanh"),
            document.getString("loaisp"),
            document.getString("mota"),
            document.getLong("soluong"),
            document.getString("hansudung"),
            document.getLong("type"),
            document.getString("trongluong")
        );
    }
    
    private int findProductIndex(String productId) {
        for (int i = 0; i < mlistProduct.size(); i++) {
            if (mlistProduct.get(i).getId().equals(productId)) {
                return i;
            }
        }
        return -1;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (productListener != null) {
            productListener.remove();
        }
        if (categoryListener != null) {
            categoryListener.remove();
        }
    }

    private void InitWidget() {
        rcvAdminProduct = findViewById(R.id.rcv_admin_product);
        imgAddProduct = findViewById(R.id.image_add_product);
        imgBackAdminProduct = findViewById(R.id.img_back_admin_product);
        spinnerLoaiSP = findViewById(R.id.spinner_loai_sp);

    }
}