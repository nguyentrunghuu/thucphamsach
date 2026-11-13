package com.UngDung.ThucPhamXanh.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.UngDung.ThucPhamXanh.Adapter.ProductAdapter;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IClickOpenBottomSheet;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Map;

public class ProductListActivity extends AppCompatActivity {

    private ImageView imgBackProductList;
    private TextView tvTitleProductList;
    private RecyclerView rcvProductList;
    private ProgressBar progressBarProductList;
    private View layoutEmptyState;
    private Spinner spinnerCategoryFilter;

    private ProductAdapter productAdapter;
    private ArrayList<Product> productList;
    private ArrayList<Product> allProductList; // Lưu tất cả sản phẩm để lọc
    private ArrayList<String> categoryList;
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private String selectedCategory = "Tất cả";

    // Intent extras
    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_TYPE = "type"; // "all" or "category"
    public static final String EXTRA_TITLE = "title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        InitWidget();
        Event();
        LoadProducts();
    }

    private void InitWidget() {
        imgBackProductList = findViewById(R.id.img_back_product_list);
        tvTitleProductList = findViewById(R.id.tv_title_product_list);
        rcvProductList = findViewById(R.id.rcv_product_list);
        progressBarProductList = findViewById(R.id.progress_bar_product_list);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        spinnerCategoryFilter = findViewById(R.id.spinner_category_filter);

        productList = new ArrayList<>();
        allProductList = new ArrayList<>();
        categoryList = new ArrayList<>();

        // Get intent data
        Intent intent = getIntent();
        String type = intent.getStringExtra(EXTRA_TYPE);
        String category = intent.getStringExtra(EXTRA_CATEGORY);
        String title = intent.getStringExtra(EXTRA_TITLE);

        // Set title
        if (title != null && !title.isEmpty()) {
            tvTitleProductList.setText(title);
        } else if (category != null && !category.isEmpty()) {
            tvTitleProductList.setText(category);
        } else {
            tvTitleProductList.setText("Danh sách sản phẩm");
        }
    }

    private void Event() {
        imgBackProductList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        
        // Setup category filter spinner
        setupCategoryFilter();
    }
    
    private void setupCategoryFilter() {
        // Load danh sách loại sản phẩm
        firestore.collection("LoaiProduct")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        categoryList.clear();
                        categoryList.add("Tất cả");
                        
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String categoryName = doc.getString("tenloai");
                            if (categoryName != null && !categoryName.trim().isEmpty()) {
                                categoryList.add(categoryName);
                            }
                        }
                        
                        // Setup spinner adapter
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                ProductListActivity.this,
                                android.R.layout.simple_spinner_item,
                                categoryList
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerCategoryFilter.setAdapter(adapter);
                        
                        // Set listener
                        spinnerCategoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                String selected = categoryList.get(position);
                                if (!selected.equals(selectedCategory)) {
                                    selectedCategory = selected;
                                    filterProductsByCategory(selected);
                                }
                            }
                            
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("ProductListActivity", "Error loading categories: " + e.getMessage());
                    }
                });
    }
    
    private void filterProductsByCategory(String category) {
        productList.clear();
        
        if (category.equals("Tất cả")) {
            // Hiển thị tất cả sản phẩm
            productList.addAll(allProductList);
        } else {
            // Lọc theo loại
            for (Product product : allProductList) {
                if (product.getLoaisp() != null && product.getLoaisp().equals(category)) {
                    productList.add(product);
                }
            }
        }
        
        // Cập nhật RecyclerView
        if (productAdapter != null) {
            productAdapter.notifyDataSetChanged();
        } else {
            setupRecyclerView();
        }
        
        // Hiển thị empty state nếu không có sản phẩm
        if (productList.isEmpty()) {
            showEmptyState();
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rcvProductList.setVisibility(View.VISIBLE);
        }
        
        Log.d("ProductListActivity", "Filtered products: " + productList.size() + " products for category: " + category);
    }

    private void LoadProducts() {
        progressBarProductList.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);

        Intent intent = getIntent();
        String type = intent.getStringExtra(EXTRA_TYPE);
        String category = intent.getStringExtra(EXTRA_CATEGORY);

        if (type != null && type.equals("promotion")) {
            // Load products with promotions
            LoadProductsWithPromotions();
        } else if (type != null && type.equals("category") && category != null && !category.isEmpty()) {
            // Load products by category
            LoadProductsByCategory(category);
        } else {
            // Load all products
            LoadAllProducts();
        }
    }

    private void LoadAllProducts() {
        Log.d("ProductListActivity", "Loading all products...");
        firestore.collection("SanPham")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        allProductList.clear();
                        productList.clear();
                        if (queryDocumentSnapshots.size() > 0) {
                            for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                                if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHiddenFromShop(d)) continue; // đã xóa mềm hoặc hết hạn
                                try {
                                    Product product = new Product(
                                            d.getId(),
                                            d.getString("tensp"),
                                            d.getLong("giatien"),
                                            d.getString("hinhanh"),
                                            d.getString("loaisp"),
                                            d.getString("mota"),
                                            d.getLong("soluong"),
                                            d.getString("hansudung"),
                                            d.getLong("type"),
                                            d.getString("trongluong")
                                    );
                                    allProductList.add(product);
                                } catch (Exception e) {
                                    Log.e("ProductListActivity", "Error parsing product: " + e.getMessage());
                                }
                            }
                            // Hiển thị tất cả sản phẩm ban đầu
                            productList.addAll(allProductList);
                            Log.d("ProductListActivity", "Loaded " + allProductList.size() + " products");
                            setupRecyclerView();
                        } else {
                            showEmptyState();
                        }
                        progressBarProductList.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("ProductListActivity", "Error loading products: " + e.getMessage());
                        progressBarProductList.setVisibility(View.GONE);
                        showEmptyState();
                    }
                });
    }

    private void LoadProductsByCategory(String category) {
        Log.d("ProductListActivity", "Loading products for category: " + category);
        firestore.collection("SanPham")
                .whereEqualTo("loaisp", category)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        allProductList.clear();
                        productList.clear();
                        if (queryDocumentSnapshots.size() > 0) {
                            for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                                if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHiddenFromShop(d)) continue; // đã xóa mềm hoặc hết hạn
                                try {
                                    Product product = new Product(
                                            d.getId(),
                                            d.getString("tensp"),
                                            d.getLong("giatien"),
                                            d.getString("hinhanh"),
                                            d.getString("loaisp"),
                                            d.getString("mota"),
                                            d.getLong("soluong"),
                                            d.getString("hansudung"),
                                            d.getLong("type"),
                                            d.getString("trongluong")
                                    );
                                    allProductList.add(product);
                                } catch (Exception e) {
                                    Log.e("ProductListActivity", "Error parsing product: " + e.getMessage());
                                }
                            }
                            // Hiển thị sản phẩm của category được chọn
                            productList.addAll(allProductList);
                            // Set spinner selection
                            if (spinnerCategoryFilter != null && categoryList.contains(category)) {
                                int position = categoryList.indexOf(category);
                                if (position >= 0) {
                                    spinnerCategoryFilter.setSelection(position);
                                    selectedCategory = category;
                                }
                            }
                            Log.d("ProductListActivity", "Loaded " + productList.size() + " products for category: " + category);
                            setupRecyclerView();
                        } else {
                            showEmptyState();
                        }
                        progressBarProductList.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("ProductListActivity", "Error loading products by category: " + e.getMessage());
                        progressBarProductList.setVisibility(View.GONE);
                        showEmptyState();
                    }
                });
    }
    
    private void LoadProductsWithPromotions() {
        Log.d("ProductListActivity", "Loading products with promotions...");
        
        // Bước 1: Load thông tin khuyến mãi từ Promotions/current_promotion
        firestore.collection("Promotions")
                .document("current_promotion")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Lấy danh sách các loại sản phẩm có khuyến mãi
                        Map<String, Object> categoryDiscounts = (Map<String, Object>) documentSnapshot.get("categoryDiscounts");
                        if (categoryDiscounts != null && !categoryDiscounts.isEmpty()) {
                            // Tạo danh sách các loại có khuyến mãi
                            ArrayList<String> promotionCategories = new ArrayList<>();
                            for (Map.Entry<String, Object> entry : categoryDiscounts.entrySet()) {
                                String categoryName = entry.getKey();
                                Object discountObj = entry.getValue();
                                
                                // Kiểm tra xem có discount hợp lệ không
                                if (discountObj instanceof Map) {
                                    Map<String, Object> discountMap = (Map<String, Object>) discountObj;
                                    Integer discountPercent = discountMap.get("discountPercent") != null 
                                            ? ((Number) discountMap.get("discountPercent")).intValue() : 0;
                                    if (discountPercent > 0) {
                                        promotionCategories.add(categoryName);
                                    }
                                } else if (discountObj instanceof Number) {
                                    int discountPercent = ((Number) discountObj).intValue();
                                    if (discountPercent > 0) {
                                        promotionCategories.add(categoryName);
                                    }
                                }
                            }
                            
                            Log.d("ProductListActivity", "Found " + promotionCategories.size() + " categories with promotions: " + promotionCategories);
                            
                            // Bước 2: Load sản phẩm từ các loại có khuyến mãi
                            if (!promotionCategories.isEmpty()) {
                                LoadProductsFromPromotionCategories(promotionCategories);
                            } else {
                                Log.w("ProductListActivity", "No valid promotion categories found");
                                progressBarProductList.setVisibility(View.GONE);
                                showEmptyState();
                            }
                        } else {
                            Log.w("ProductListActivity", "No categoryDiscounts found in promotion document");
                            progressBarProductList.setVisibility(View.GONE);
                            showEmptyState();
                        }
                    } else {
                        Log.w("ProductListActivity", "Promotion document does not exist");
                        progressBarProductList.setVisibility(View.GONE);
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProductListActivity", "Error loading promotions: " + e.getMessage());
                    progressBarProductList.setVisibility(View.GONE);
                    showEmptyState();
                });
    }
    
    private void LoadProductsFromPromotionCategories(ArrayList<String> promotionCategories) {
        allProductList.clear();
        productList.clear();
        
        // Load tất cả sản phẩm, sau đó filter theo các loại có khuyến mãi
        // (Firestore whereIn chỉ hỗ trợ tối đa 10 items, nên load tất cả rồi filter)
        firestore.collection("SanPham")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                                if (com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isHiddenFromShop(d)) continue; // đã xóa mềm hoặc hết hạn
                                try {
                                    String loaisp = d.getString("loaisp");
                                    // Chỉ thêm sản phẩm thuộc các loại có khuyến mãi
                                    if (loaisp != null && promotionCategories.contains(loaisp)) {
                                        Product product = new Product(
                                                d.getId(),
                                                d.getString("tensp"),
                                                d.getLong("giatien"),
                                                d.getString("hinhanh"),
                                                loaisp,
                                                d.getString("mota"),
                                                d.getLong("soluong"),
                                                d.getString("hansudung"),
                                                d.getLong("type"),
                                                d.getString("trongluong")
                                        );
                                        allProductList.add(product);
                                    }
                                } catch (Exception e) {
                                    Log.e("ProductListActivity", "Error parsing product: " + e.getMessage());
                                }
                            }
                            // Hiển thị tất cả sản phẩm có khuyến mãi
                            productList.addAll(allProductList);
                            Log.d("ProductListActivity", "Loaded " + productList.size() + " products with promotions from " + promotionCategories.size() + " categories");
                            setupRecyclerView();
                        } else {
                            showEmptyState();
                        }
                        progressBarProductList.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("ProductListActivity", "Error loading products with promotions: " + e.getMessage());
                        progressBarProductList.setVisibility(View.GONE);
                        showEmptyState();
                    }
                });
    }

    private void setupRecyclerView() {
        if (productList.isEmpty()) {
            showEmptyState();
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        rcvProductList.setVisibility(View.VISIBLE);

        // Use GridLayoutManager for better display (2 columns)
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        rcvProductList.setLayoutManager(gridLayoutManager);

        productAdapter = new ProductAdapter(this, productList, 1, new IClickOpenBottomSheet() {
            @Override
            public void onClickOpenBottomSheet(int position) {
                Product selectedProduct = productList.get(position);
                Intent intent = new Intent(ProductListActivity.this, DetailSPActivity.class);
                intent.putExtra("search", selectedProduct);
                startActivity(intent);
            }
        });

        rcvProductList.setAdapter(productAdapter);
    }

    private void showEmptyState() {
        rcvProductList.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }
}

