package com.UngDung.ThucPhamXanh.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import com.UngDung.ThucPhamXanh.Adapter.AllCommentAdapter;
import com.UngDung.ThucPhamXanh.Helper.ReviewUtil;
import com.UngDung.ThucPhamXanh.Models.Binhluan;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/** Màn "Đánh giá và nhận xét": điểm trung bình, tỉ lệ từng mức sao và toàn bộ nhận xét của 1 sản phẩm. */
public class CommentActivity extends AppCompatActivity {

    private ImageView imgBackCMT;
    private TextView tvRatingTotal, tvSoNguoiDanhGia, tvEmptyCmt;
    private RatingBar ratingBarTotal;
    private ProgressBar[] starBars; // index 0 = 1 sao ... index 4 = 5 sao
    private RecyclerView rcvAllCMT;

    private final ArrayList<Binhluan> mListCMT = new ArrayList<>();
    private AllCommentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        InitWidget();
        imgBackCMT.setOnClickListener(v -> finish());

        Product product = (Product) getIntent().getSerializableExtra("allcmt");
        if (product == null || product.getId() == null) {
            showSummary(0, new int[5], 0f);
            return;
        }
        loadReviews(product.getId());
    }

    private void loadReviews(String idProduct) {
        showSummary(0, new int[5], 0f);
        FirebaseFirestore.getInstance().collection("BinhLuan")
                .whereEqualTo("idproduct", idProduct).get()
                .addOnSuccessListener(snap -> {
                    mListCMT.clear();
                    int[] counts = new int[5];
                    float sum = 0f;
                    for (QueryDocumentSnapshot q : snap) {
                        String rate = q.getString("rate");
                        float r = ReviewUtil.parseRate(rate);
                        counts[ReviewUtil.starBucket(r) - 1]++;
                        sum += r;
                        mListCMT.add(new Binhluan(q.getId(), idProduct, q.getString("iduser"),
                                rate, q.getString("noidung")));
                    }
                    adapter.notifyDataSetChanged();
                    int total = mListCMT.size();
                    showSummary(total, counts, total == 0 ? 0f : sum / total);
                })
                .addOnFailureListener(e -> {
                    tvEmptyCmt.setText("Không tải được đánh giá. Vui lòng thử lại.");
                    tvEmptyCmt.setVisibility(View.VISIBLE);
                });
    }

    private void showSummary(int total, int[] counts, float average) {
        tvRatingTotal.setText(ReviewUtil.formatRate(average));
        ratingBarTotal.setRating(average);
        tvSoNguoiDanhGia.setText(String.valueOf(total));
        for (int i = 0; i < 5; i++) {
            starBars[i].setMax(Math.max(total, 1));
            starBars[i].setProgress(counts[i]);
        }
        tvEmptyCmt.setText("Sản phẩm chưa có đánh giá nào.");
        tvEmptyCmt.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
    }

    private void InitWidget() {
        imgBackCMT = findViewById(R.id.img_back_cmt);
        tvRatingTotal = findViewById(R.id.tv_rating_total);
        ratingBarTotal = findViewById(R.id.rating_bar_total);
        tvSoNguoiDanhGia = findViewById(R.id.tv_songuoi_danhgia);
        tvEmptyCmt = findViewById(R.id.tv_empty_cmt);
        starBars = new ProgressBar[]{
                findViewById(R.id.progress_bar_one),
                findViewById(R.id.progress_bar_two),
                findViewById(R.id.progress_bar_three),
                findViewById(R.id.progress_bar_four),
                findViewById(R.id.progress_bar_five)
        };
        rcvAllCMT = findViewById(R.id.rcv_all_cmt);
        ratingBarTotal.setIsIndicator(true);    // Không cho phép thay đổi giá trị ratingbar

        adapter = new AllCommentAdapter(this, mListCMT);
        rcvAllCMT.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        rcvAllCMT.setNestedScrollingEnabled(false);
        rcvAllCMT.setAdapter(adapter);
    }
}
