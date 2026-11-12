package com.UngDung.ThucPhamXanh.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.UngDung.ThucPhamXanh.Adapter.PromotionAdapter;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class PromotionActivity extends AppCompatActivity {

    private ImageView imgBackPromotion;
    private TextView tvTitlePromotion;
    private RecyclerView rcvPromotionList;
    private ProgressBar progressBarPromotion;
    private View layoutEmptyState;
    
    private PromotionAdapter promotionAdapter;
    private ArrayList<String> promotionList;
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private ListenerRegistration promotionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotion);

        InitWidget();
        Event();
        LoadPromotions();
    }

    private void InitWidget() {
        imgBackPromotion = findViewById(R.id.img_back_promotion);
        tvTitlePromotion = findViewById(R.id.tv_title_promotion);
        rcvPromotionList = findViewById(R.id.rcv_promotion_list);
        progressBarPromotion = findViewById(R.id.progress_bar_promotion);
        layoutEmptyState = findViewById(R.id.layout_empty_state);

        promotionList = new ArrayList<>();
        tvTitlePromotion.setText("Chương trình khuyến mãi");
    }

    private void Event() {
        imgBackPromotion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void LoadPromotions() {
        progressBarPromotion.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);

        // Setup realtime listener cho khuyến mãi
        promotionListener = firestore.collection("KhuyenMai")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e("PromotionActivity", "Error listening to promotions: " + e.getMessage());
                        progressBarPromotion.setVisibility(View.GONE);
                        showEmptyState();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        promotionList.clear();
                        Log.d("PromotionActivity", "Loaded " + queryDocumentSnapshots.size() + " promotions");

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            // Lấy hình ảnh khuyến mãi
                            String imageUrl = doc.getString("hinhanh");
                            if (imageUrl == null || imageUrl.isEmpty()) {
                                imageUrl = doc.getString("image");
                            }
                            if (imageUrl == null || imageUrl.isEmpty()) {
                                imageUrl = doc.getString("url");
                            }

                            // Kiểm tra trạng thái nếu có
                            Boolean trangthai = doc.getBoolean("trangthai");
                            if (trangthai == null || trangthai) {
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    promotionList.add(imageUrl);
                                }
                            }
                        }

                        progressBarPromotion.setVisibility(View.GONE);

                        if (promotionList.isEmpty()) {
                            showEmptyState();
                        } else {
                            layoutEmptyState.setVisibility(View.GONE);
                            setupRecyclerView();
                        }
                    }
                });
    }

    private void setupRecyclerView() {
        if (promotionList.isEmpty()) {
            showEmptyState();
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        rcvPromotionList.setVisibility(View.VISIBLE);

        // Sử dụng LinearLayoutManager vertical để hiển thị danh sách dọc
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rcvPromotionList.setLayoutManager(linearLayoutManager);

        promotionAdapter = new PromotionAdapter(this, promotionList, new IClickCTHD() {
            @Override
            public void onClickCTHD(int position) {
                // Mở danh sách sản phẩm khuyến mãi
                Intent intent = new Intent(PromotionActivity.this, ProductListActivity.class);
                intent.putExtra(ProductListActivity.EXTRA_TYPE, "all");
                intent.putExtra(ProductListActivity.EXTRA_TITLE, "Sản phẩm khuyến mãi");
                startActivity(intent);
            }
        });

        rcvPromotionList.setAdapter(promotionAdapter);
    }

    private void showEmptyState() {
        rcvPromotionList.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (promotionListener != null) {
            promotionListener.remove();
            promotionListener = null;
        }
    }
}

