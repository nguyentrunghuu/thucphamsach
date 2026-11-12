package com.UngDung.ThucPhamXanh.Helper;

import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.UngDung.ThucPhamXanh.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;

import java.util.Locale;

/**
 * Tiện ích dùng chung cho phần đánh giá / nhận xét (collection "BinhLuan").
 * Trường "rate" được lưu dạng chuỗi ("5", "4.0", "2.5"...), có thể thiếu ở dữ liệu cũ.
 */
public final class ReviewUtil {

    public static final String DEFAULT_NAME = "Người dùng";

    private ReviewUtil() { }

    /** Đọc số sao an toàn: null / rỗng / sai định dạng -> 0, kẹp trong khoảng 0..5. */
    public static float parseRate(String rate) {
        if (rate == null) return 0f;
        try {
            float v = Float.parseFloat(rate.trim().replace(',', '.'));
            if (Float.isNaN(v)) return 0f;
            return Math.max(0f, Math.min(5f, v));
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    /** Hiển thị số sao với 1 chữ số thập phân: 4.5, 5.0. */
    public static String formatRate(float rate) {
        return String.format(Locale.US, "%.1f", rate);
    }

    /** Nhóm sao 1..5 dùng cho thanh tỉ lệ (2.5 sao -> nhóm 3). */
    public static int starBucket(float rate) {
        int b = (int) Math.ceil(rate);
        return Math.max(1, Math.min(5, b));
    }

    /**
     * Hiển thị tên + ảnh người đánh giá.
     * Ưu tiên hồ sơ Firestore User/{uid}/Profile; tài khoản đăng nhập Google/Facebook có thể
     * chưa có hồ sơ nên lấy dự phòng từ Realtime Database "Users/{uid}".
     * Gắn tag uid vào view để bỏ qua kết quả trả về muộn khi RecyclerView đã tái sử dụng dòng.
     */
    public static void bindReviewer(String uid, TextView tvName, ImageView imgAvatar) {
        tvName.setText(DEFAULT_NAME);
        imgAvatar.setImageResource(R.drawable.ic_user);
        tvName.setTag(uid);
        if (TextUtils.isEmpty(uid)) return;

        FirebaseFirestore.getInstance().collection("User").document(uid).collection("Profile").get()
                .addOnSuccessListener(snap -> {
                    if (!uid.equals(tvName.getTag())) return;
                    String name = null, avatar = null;
                    for (QueryDocumentSnapshot q : snap) {
                        name = q.getString("hoten");
                        avatar = q.getString("avatar");
                    }
                    if (!TextUtils.isEmpty(name)) {
                        apply(tvName, imgAvatar, name, avatar);
                    } else {
                        loadFromRealtime(uid, tvName, imgAvatar, avatar);
                    }
                })
                .addOnFailureListener(e -> loadFromRealtime(uid, tvName, imgAvatar, null));
    }

    private static void loadFromRealtime(String uid, TextView tvName, ImageView imgAvatar, String fallbackAvatar) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot ds) {
                        if (!uid.equals(tvName.getTag())) return;
                        String name = ds.child("name").getValue(String.class);
                        String avatar = ds.child("avatar").getValue(String.class);
                        apply(tvName, imgAvatar, name, TextUtils.isEmpty(fallbackAvatar) ? avatar : fallbackAvatar);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private static void apply(TextView tvName, ImageView imgAvatar, String name, String avatar) {
        boolean validName = !TextUtils.isEmpty(name) && !"any name".equalsIgnoreCase(name);
        tvName.setText(validName ? name : DEFAULT_NAME);
        if (avatar != null && (avatar.startsWith("http://") || avatar.startsWith("https://"))) {
            Picasso.get().load(avatar).placeholder(R.drawable.ic_user).error(R.drawable.ic_user).into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.ic_user);
        }
    }
}
