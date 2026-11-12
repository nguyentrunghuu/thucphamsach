package com.UngDung.ThucPhamXanh.View.Admin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Adapter.CategoryDiscountAdapter;
import com.UngDung.ThucPhamXanh.Models.CategoryDiscount;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AdminPromotionActivity extends AppCompatActivity {

    private static final String TAG = "AdminPromotionActivity";
    private Button btnSavePromotion;
    private ImageView imgBackPromotion;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String PROMOTION_DOC_ID = "current_promotion";
    
    // Category discounts
    private RecyclerView rcvCategoryDiscounts;
    private CategoryDiscountAdapter categoryDiscountAdapter;
    private ArrayList<String> categoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_promotion);

        InitWidget();
        // LoadCurrentPromotion() sẽ được gọi sau khi loadCategories() hoàn thành
        Event();
    }

    private void InitWidget() {
        btnSavePromotion = findViewById(R.id.btn_save_promotion);
        imgBackPromotion = findViewById(R.id.img_back_promotion);
        rcvCategoryDiscounts = findViewById(R.id.rcv_category_discounts);
        
        // Initialize category discounts
        categoryList = new ArrayList<>();
        categoryDiscountAdapter = new CategoryDiscountAdapter(this, categoryList);
        rcvCategoryDiscounts.setLayoutManager(new LinearLayoutManager(this));
        rcvCategoryDiscounts.setAdapter(categoryDiscountAdapter);
        
        // Load categories trước, sau đó load promotions
        loadCategories();
    }

    private void LoadCurrentPromotion() {
        if (categoryDiscountAdapter == null) {
            Log.e(TAG, "❌ Adapter chưa được khởi tạo");
            return;
        }
        
        db.collection("Promotions")
                .document(PROMOTION_DOC_ID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            // Load category discounts
                            Map<String, Object> categoryDiscountsMap = (Map<String, Object>) documentSnapshot.get("categoryDiscounts");
                            if (categoryDiscountsMap != null && !categoryDiscountsMap.isEmpty()) {
                                HashMap<String, CategoryDiscount> categoryDiscounts = new HashMap<>();
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
                                            Log.d(TAG, "✅ Loaded discount for " + categoryName + ": " + minProducts + " SP -> " + discountPercent + "%");
                                        }
                                    } else if (discountObj instanceof Number) {
                                        // Tương thích với dữ liệu cũ (chỉ có discountPercent)
                                        int discountPercent = ((Number) discountObj).intValue();
                                        if (discountPercent > 0) {
                                            categoryDiscounts.put(categoryName, new CategoryDiscount(1, discountPercent));
                                            Log.d(TAG, "✅ Loaded discount for " + categoryName + ": 1 SP -> " + discountPercent + "%");
                                        }
                                    }
                                }
                                
                                // Set discounts vào adapter và cập nhật UI
                                if (categoryDiscountAdapter != null) {
                                    categoryDiscountAdapter.setCategoryDiscounts(categoryDiscounts);
                                    Log.d(TAG, "✅ Đã load khuyến mãi theo loại: " + categoryDiscounts.size() + " loại");
                                } else {
                                    Log.e(TAG, "❌ Adapter is null when trying to set discounts");
                                }
                            } else {
                                Log.d(TAG, "ℹ️ Chưa có khuyến mãi theo loại (categoryDiscountsMap is null or empty)");
                            }
                        } else {
                            Log.d(TAG, "ℹ️ Chưa có chương trình khuyến mãi (document không tồn tại)");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "❌ Lỗi load khuyến mãi: " + e.getMessage(), e);
                        Toast.makeText(AdminPromotionActivity.this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadCategories() {
        db.collection("LoaiProduct")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        categoryList.clear();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String categoryName = doc.getString("tenloai");
                            if (categoryName != null && !categoryName.trim().isEmpty()) {
                                categoryList.add(categoryName);
                            }
                        }
                        // Cập nhật adapter với danh sách categories mới
                        if (categoryDiscountAdapter != null) {
                            // Cập nhật categoryList trong adapter
                            categoryDiscountAdapter.updateCategoryList(categoryList);
                        } else {
                            categoryDiscountAdapter = new CategoryDiscountAdapter(AdminPromotionActivity.this, categoryList);
                            rcvCategoryDiscounts.setAdapter(categoryDiscountAdapter);
                        }
                        categoryDiscountAdapter.notifyDataSetChanged();
                        Log.d(TAG, "✅ Đã load " + categoryList.size() + " loại sản phẩm");
                        
                        // Sau khi load xong categories, mới load promotions
                        LoadCurrentPromotion();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "❌ Lỗi load danh sách loại sản phẩm: " + e.getMessage());
                        Toast.makeText(AdminPromotionActivity.this, "Lỗi tải danh sách loại sản phẩm", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void Event() {
        imgBackPromotion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btnSavePromotion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SavePromotion();
            }
        });
    }

    private void SavePromotion() {
        // Lưu khuyến mãi theo loại sản phẩm
        HashMap<String, CategoryDiscount> categoryDiscountsToSave = categoryDiscountAdapter.getCategoryDiscounts();
        Map<String, Object> categoryDiscountsMap = new HashMap<>();
        int activeDiscounts = 0;
        
        for (Map.Entry<String, CategoryDiscount> entry : categoryDiscountsToSave.entrySet()) {
            CategoryDiscount discount = entry.getValue();
            if (discount != null && discount.getMinProducts() > 0 && discount.getDiscountPercent() > 0) {
                Map<String, Object> discountMap = new HashMap<>();
                discountMap.put("minProducts", discount.getMinProducts());
                discountMap.put("discountPercent", discount.getDiscountPercent());
                categoryDiscountsMap.put(entry.getKey(), discountMap);
                activeDiscounts++;
            }
        }
        
        if (activeDiscounts == 0) {
            Toast.makeText(this, "Vui lòng thiết lập khuyến mãi cho ít nhất 1 loại sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Lưu vào Firestore
        Map<String, Object> promotionData = new HashMap<>();
        promotionData.put("categoryDiscounts", categoryDiscountsMap);
        promotionData.put("updatedAt", System.currentTimeMillis());

        int finalActiveDiscounts = activeDiscounts;
        db.collection("Promotions")
                .document(PROMOTION_DOC_ID)
                .set(promotionData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "✅ Đã lưu khuyến mãi theo loại thành công: " + finalActiveDiscounts + " loại");
                        Toast.makeText(AdminPromotionActivity.this, 
                                "Đã lưu khuyến mãi cho " + finalActiveDiscounts + " loại sản phẩm", 
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "❌ Lỗi lưu khuyến mãi: " + e.getMessage());
                        Toast.makeText(AdminPromotionActivity.this, 
                                "Lỗi lưu khuyến mãi", 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

