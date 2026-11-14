package com.UngDung.ThucPhamXanh.Helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.UngDung.ThucPhamXanh.BotAI.GeminiClient;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Gợi ý cho bạn" trên trang chủ: dựa vào các sản phẩm khách đã mua (ChitietHoaDon/{uid}/ALL) và
 * yêu thích (Favorite), nhờ Gemini chọn sản phẩm trong danh mục đang bán (xem {@link ShoppingAi}).
 *
 * Tiết kiệm lượt gọi AI: kết quả được lưu theo từng tài khoản trong SharedPreferences và dùng lại cả
 * ngày; khách chưa có lịch sử mua / yêu thích thì không gọi AI và không hiện mục này. Không đủ dữ liệu
 * hoặc AI lỗi → im lặng không hiện gì (trang chủ vẫn như cũ).
 */
public final class HomeAiPicks {

    private static final String TAG = "HomeAiPicks";
    private static final String PREFS = "ai_picks";
    private static final int MAX_HISTORY = 40;
    private static final int MAX_PICKS = 8;

    public interface Callback {
        void onPicks(List<Product> picks);
    }

    private HomeAiPicks() { }

    /** catalog: các sản phẩm đang bán (đã lọc xóa/hết hạn). Gọi lại nhiều lần cũng an toàn. */
    public static void load(Context context, List<Product> catalog, Callback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || catalog.isEmpty() || !GeminiClient.hasKey()) return;
        final String uid = user.getUid();
        final SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final String today = LocalDate.now().toString();

        // 1) Dùng lại kết quả trong ngày
        String cached = prefs.getString(uid, "");
        int bar = cached.indexOf('|');
        if (bar > 0 && cached.substring(0, bar).equals(today)) {
            List<Product> picks = byIds(catalog, cached.substring(bar + 1));
            if (!picks.isEmpty()) callback.onPicks(picks);
            return; // cache rỗng = hôm nay đã xác định khách chưa có lịch sử
        }

        // 2) Đọc lịch sử mua + yêu thích
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Product> byId = new java.util.HashMap<>();
        for (Product p : catalog) byId.put(p.getId(), p);

        db.collection("ChitietHoaDon").document(uid).collection("ALL").get().addOnCompleteListener(boughtTask -> {
            Set<String> boughtNames = new LinkedHashSet<>();
            if (boughtTask.isSuccessful() && boughtTask.getResult() != null) {
                for (DocumentSnapshot line : boughtTask.getResult()) {
                    addName(boughtNames, byId.get(line.getString("id_product")));
                    if (boughtNames.size() >= MAX_HISTORY) break;
                }
            }
            db.collection("Favorite").whereEqualTo("iduser", uid).get().addOnCompleteListener(favTask -> {
                Set<String> favNames = new LinkedHashSet<>();
                if (favTask.isSuccessful() && favTask.getResult() != null) {
                    for (DocumentSnapshot f : favTask.getResult()) {
                        addName(favNames, byId.get(f.getString("idproduct")));
                        if (favNames.size() >= MAX_HISTORY) break;
                    }
                }
                if (boughtNames.isEmpty() && favNames.isEmpty()) {
                    prefs.edit().putString(uid, today + "|").apply();
                    return;
                }
                ask(prefs, uid, today, catalog, new ArrayList<>(boughtNames), new ArrayList<>(favNames), callback);
            });
        });
    }

    private static void ask(SharedPreferences prefs, String uid, String today, List<Product> catalog,
                            List<String> bought, List<String> favorites, Callback callback) {
        GeminiClient.ask(ShoppingAi.recommendSystemPrompt(catalog), null,
                ShoppingAi.recommendUserMessage(bought, favorites), new GeminiClient.Result() {
                    @Override
                    public void onSuccess(String text) {
                        List<Product> picks = ShoppingAi.parseRecommendations(text, catalog, MAX_PICKS);
                        if (picks.isEmpty()) return;
                        StringBuilder ids = new StringBuilder();
                        for (Product p : picks) ids.append(ids.length() == 0 ? "" : ",").append(p.getId());
                        prefs.edit().putString(uid, today + "|" + ids).apply();
                        callback.onPicks(picks);
                    }

                    @Override
                    public void onError(String message) {
                        Log.w(TAG, "Không lấy được gợi ý: " + message); // im lặng, lần mở sau thử lại
                    }
                });
    }

    private static void addName(Set<String> names, Product p) {
        if (p != null && p.getTensp() != null && !p.getTensp().trim().isEmpty()) names.add(p.getTensp().trim());
    }

    private static List<Product> byIds(List<Product> catalog, String commaIds) {
        List<Product> out = new ArrayList<>();
        for (String id : commaIds.split(",")) {
            if (id.isEmpty()) continue;
            for (Product p : catalog) {
                if (id.equals(p.getId())) { out.add(p); break; }
            }
        }
        return out;
    }
}
