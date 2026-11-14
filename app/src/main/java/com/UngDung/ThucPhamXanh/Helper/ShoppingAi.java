package com.UngDung.ThucPhamXanh.Helper;

import com.UngDung.ThucPhamXanh.Models.Product;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prompt + đọc kết quả cho tính năng AI của khách hàng (gợi ý món từ giỏ hàng, gợi ý sản phẩm
 * trên trang chủ). Tính thuần (không đụng Android/Firebase) nên unit test được.
 *
 * Nguyên tắc: AI chỉ được chọn sản phẩm CÓ TRONG danh mục gửi kèm (đang bán, còn hàng, còn hạn).
 * Kết quả AI luôn được đối chiếu lại với danh mục thật bằng {@link #matchProducts}, tên lạ bị bỏ.
 * Không gửi thông tin cá nhân — chỉ tên sản phẩm.
 */
public final class ShoppingAi {

    private ShoppingAi() { }

    /** Số sản phẩm tối đa đưa vào danh mục gửi cho AI (rẻ token, đủ đa dạng). */
    public static final int MAX_CATALOG = 150;

    /** Một món ăn AI gợi ý. */
    public static final class Dish {
        public final String name;
        public final String description;
        /** Tên sản phẩm (đã đối chiếu với danh mục thật) nên mua thêm để nấu món này. */
        public final List<Product> missing;

        public Dish(String name, String description, List<Product> missing) {
            this.name = name;
            this.description = description;
            this.missing = missing;
        }
    }

    // ------------------------------------------------------------------ gợi ý món ăn từ giỏ hàng

    public static String dishSystemPrompt(List<Product> catalog) {
        return "Bạn là đầu bếp gia đình Việt Nam trong ứng dụng bán thực phẩm sạch \"Thực Phẩm Xanh\".\n"
                + "Từ các nguyên liệu khách đang có trong giỏ hàng, gợi ý 3 món ăn thông dụng nấu được từ chúng.\n"
                + "Với mỗi món, liệt kê các nguyên liệu khách còn THIẾU và nên mua thêm; mỗi nguyên liệu thiếu phải lấy "
                + "NGUYÊN VĂN một tên trong [DANH MỤC] bên dưới (không tự đặt tên khác); không liệt kê gia vị cơ bản nếu danh mục không có. "
                + "Mỗi món tối đa 4 nguyên liệu thiếu; món đã đủ nguyên liệu thì để mảng rỗng.\n"
                + "Chỉ trả về JSON đúng dạng, không thêm chữ nào khác, không markdown:\n"
                + "{\"monan\":[{\"ten\":\"Tên món\",\"mota\":\"Một câu ngắn về cách nấu\",\"thieu\":[\"Tên sản phẩm trong danh mục\"]}]}\n\n"
                + "[DANH MỤC]\n" + catalogText(catalog);
    }

    public static String dishUserMessage(List<String> cartNames) {
        return "Giỏ hàng của tôi có: " + String.join(", ", cartNames) + ".";
    }

    /** Đọc JSON món ăn; lỗi / rỗng → danh sách rỗng. Nguyên liệu thiếu được đối chiếu với danh mục thật. */
    public static List<Dish> parseDishes(String raw, List<Product> catalog) {
        List<Dish> out = new ArrayList<>();
        JsonObject root = parseObject(raw);
        if (root == null || !root.has("monan") || !root.get("monan").isJsonArray()) return out;
        for (JsonElement e : root.getAsJsonArray("monan")) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            String name = str(o, "ten");
            if (name.isEmpty()) continue;
            List<String> missingNames = new ArrayList<>();
            if (o.has("thieu") && o.get("thieu").isJsonArray()) {
                for (JsonElement m : o.getAsJsonArray("thieu")) {
                    if (m.isJsonPrimitive()) missingNames.add(m.getAsString());
                }
            }
            out.add(new Dish(name, str(o, "mota"), matchProducts(missingNames, catalog, 4)));
        }
        return out;
    }

    // ------------------------------------------------------------------ gợi ý cho bạn (trang chủ)

    public static String recommendSystemPrompt(List<Product> catalog) {
        return "Bạn là trợ lý mua sắm của ứng dụng thực phẩm sạch \"Thực Phẩm Xanh\".\n"
                + "Từ lịch sử mua và sản phẩm yêu thích của khách, chọn 8 sản phẩm khách có thể muốn mua: "
                + "khoảng một nửa là món họ hay mua (mua lại), nửa còn lại là sản phẩm mới hợp khẩu vị / nấu kèm được. "
                + "Mỗi tên phải lấy NGUYÊN VĂN từ [DANH MỤC] bên dưới, không trùng nhau.\n"
                + "Chỉ trả về JSON đúng dạng, không thêm chữ nào khác, không markdown:\n"
                + "{\"goiy\":[\"Tên sản phẩm 1\",\"Tên sản phẩm 2\"]}\n\n"
                + "[DANH MỤC]\n" + catalogText(catalog);
    }

    public static String recommendUserMessage(List<String> bought, List<String> favorites) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sản phẩm tôi đã mua: ").append(bought.isEmpty() ? "(chưa có)" : String.join(", ", bought)).append('\n');
        sb.append("Sản phẩm tôi yêu thích: ").append(favorites.isEmpty() ? "(chưa có)" : String.join(", ", favorites));
        return sb.toString();
    }

    /** Đọc JSON gợi ý → các sản phẩm thật trong danh mục (tối đa limit, không trùng). */
    public static List<Product> parseRecommendations(String raw, List<Product> catalog, int limit) {
        JsonObject root = parseObject(raw);
        List<String> names = new ArrayList<>();
        if (root != null && root.has("goiy") && root.get("goiy").isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray("goiy")) {
                if (e.isJsonPrimitive()) names.add(e.getAsString());
            }
        }
        return matchProducts(names, catalog, limit);
    }

    // ------------------------------------------------------------------ dùng chung

    /**
     * Đối chiếu tên do AI trả về với danh mục thật (không phân biệt hoa thường / dấu). Khớp chính xác
     * trước, không có thì khớp khi một tên chứa tên kia. Tên lạ bị bỏ, kết quả không trùng.
     */
    public static List<Product> matchProducts(List<String> names, List<Product> catalog, int limit) {
        Map<String, Product> byName = new LinkedHashMap<>();
        for (Product p : catalog) {
            String key = TextNormalizer.normalize(p.getTensp());
            if (!key.isEmpty() && !byName.containsKey(key)) byName.put(key, p);
        }
        Map<String, Product> picked = new LinkedHashMap<>();
        for (String raw : names) {
            if (picked.size() >= limit) break;
            String n = TextNormalizer.normalize(raw);
            if (n.isEmpty()) continue;
            Product hit = byName.get(n);
            if (hit == null) {
                for (Map.Entry<String, Product> en : byName.entrySet()) {
                    if (en.getKey().contains(n) || n.contains(en.getKey())) { hit = en.getValue(); break; }
                }
            }
            if (hit != null) picked.put(hit.getId(), hit);
        }
        return new ArrayList<>(picked.values());
    }

    /** "Tên (Loại)" mỗi dòng; cắt ở {@link #MAX_CATALOG}. */
    static String catalogText(List<Product> catalog) {
        StringBuilder sb = new StringBuilder();
        int n = Math.min(catalog.size(), MAX_CATALOG);
        for (int i = 0; i < n; i++) {
            Product p = catalog.get(i);
            sb.append("- ").append(p.getTensp());
            if (p.getLoaisp() != null && !p.getLoaisp().isEmpty()) sb.append(" (").append(p.getLoaisp()).append(')');
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Lấy object JSON đầu tiên trong câu trả lời (bỏ rào ```json ... ``` và chữ thừa); lỗi → null. */
    static JsonObject parseObject(String raw) {
        if (raw == null) return null;
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        try {
            JsonElement el = new JsonParser().parse(raw.substring(start, end + 1));
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString().trim() : "";
    }
}
