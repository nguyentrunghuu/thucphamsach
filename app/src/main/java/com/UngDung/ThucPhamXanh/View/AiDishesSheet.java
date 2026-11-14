package com.UngDung.ThucPhamXanh.View;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.UngDung.ThucPhamXanh.BotAI.GeminiClient;
import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.ProductVisibility;
import com.UngDung.ThucPhamXanh.Helper.ShoppingAi;
import com.UngDung.ThucPhamXanh.Helper.TextNormalizer;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Hộp "Gợi ý món ăn" ở giỏ hàng: gửi tên các sản phẩm trong giỏ + danh mục đang bán cho Gemini,
 * nhận về vài món ăn kèm nguyên liệu còn thiếu. Nguyên liệu thiếu luôn là sản phẩm thật đang bán
 * (còn hàng, còn hạn, chưa có trong giỏ) và bấm vào mở trang chi tiết sản phẩm. Chỉ đọc, không đổi giỏ hàng.
 */
final class AiDishesSheet {

    private static final String TAG = "AiDishesSheet";

    private AiDishesSheet() { }

    static void show(Activity activity, List<Product> cart) {
        Set<String> cartNames = new LinkedHashSet<>();
        for (Product p : cart) {
            if (p.getTensp() != null && !p.getTensp().trim().isEmpty()) cartNames.add(p.getTensp().trim());
        }
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View content = LayoutInflater.from(activity).inflate(R.layout.sheet_ai_dishes, null);
        dialog.setContentView(content);
        TextView status = content.findViewById(R.id.tv_ai_status);
        LinearLayout container = content.findViewById(R.id.ai_dish_container);
        dialog.show();

        if (cartNames.isEmpty()) {
            status.setText("Giỏ hàng đang trống, hãy thêm vài nguyên liệu rồi thử lại.");
            return;
        }
        if (!GeminiClient.hasKey()) {
            status.setText("Tính năng AI chưa được cấu hình.");
            return;
        }

        FirebaseFirestore.getInstance().collection("SanPham").get().addOnSuccessListener(snap -> {
            Set<String> inCart = new LinkedHashSet<>();
            for (String n : cartNames) inCart.add(TextNormalizer.normalize(n));
            List<Product> catalog = new ArrayList<>();
            for (DocumentSnapshot d : snap) {
                if (ProductVisibility.isHiddenFromShop(d)) continue; // đã xóa / hết hạn
                Product p = FirestoreMapper.toProduct(d);
                if (p.getSoluong() <= 0) continue;                    // hết hàng
                if (inCart.contains(TextNormalizer.normalize(p.getTensp()))) continue; // đã có trong giỏ
                catalog.add(p);
            }
            GeminiClient.ask(ShoppingAi.dishSystemPrompt(catalog), null,
                    ShoppingAi.dishUserMessage(new ArrayList<>(cartNames)), new GeminiClient.Result() {
                        @Override
                        public void onSuccess(String text) {
                            if (!dialog.isShowing()) return;
                            List<ShoppingAi.Dish> dishes = ShoppingAi.parseDishes(text, catalog);
                            if (dishes.isEmpty()) {
                                status.setText("AI chưa nghĩ ra món phù hợp, bạn thử lại sau nhé.");
                                return;
                            }
                            status.setText("Từ " + cartNames.size() + " sản phẩm trong giỏ, bạn có thể nấu:");
                            render(activity, dialog, container, dishes);
                        }

                        @Override
                        public void onError(String message) {
                            if (dialog.isShowing()) status.setText(message);
                        }
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Không đọc được danh mục", e);
            if (dialog.isShowing()) status.setText("Không tải được dữ liệu sản phẩm. Kiểm tra Internet rồi thử lại.");
        });
    }

    private static void render(Activity activity, BottomSheetDialog dialog, LinearLayout container,
                               List<ShoppingAi.Dish> dishes) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        container.removeAllViews();
        for (ShoppingAi.Dish dish : dishes) {
            View card = inflater.inflate(R.layout.item_ai_dish, container, false);
            ((TextView) card.findViewById(R.id.tv_dish_name)).setText(dish.name);
            TextView desc = card.findViewById(R.id.tv_dish_desc);
            desc.setText(dish.description);
            desc.setVisibility(dish.description.isEmpty() ? View.GONE : View.VISIBLE);

            ChipGroup chips = card.findViewById(R.id.chips_dish_missing);
            View label = card.findViewById(R.id.tv_dish_missing_label);
            if (dish.missing.isEmpty()) {
                label.setVisibility(View.GONE);
                chips.setVisibility(View.GONE);
            }
            for (Product p : dish.missing) {
                Chip chip = new Chip(activity);
                chip.setText(p.getTensp());
                chip.setCheckable(false);
                chip.setOnClickListener(v -> {
                    Intent i = new Intent(activity, DetailSPActivity.class);
                    i.putExtra("search", p);
                    activity.startActivity(i);
                    dialog.dismiss();
                });
                chips.addView(chip);
            }
            container.addView(card);
        }
    }
}
