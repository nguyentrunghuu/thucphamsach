package com.UngDung.ThucPhamXanh.Helper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Khuyến mãi theo loại sản phẩm: document Promotions/current_promotion có
 * categoryDiscounts = { "<tên loại>": { minProducts, discountPercent } }.
 * Giỏ hàng áp dụng khi SỐ LƯỢNG sản phẩm thuộc loại đó trong giỏ ≥ minProducts → giảm discountPercent%
 * trên tiền của loại đó (CartActivity). Lớp này đọc/ghi/kiểm tra dữ liệu đó — thuần Java, test được.
 */
public final class PromotionRules {

    public static final int MAX_MIN_PRODUCTS = 99;
    public static final int MAX_PERCENT = 100;

    private PromotionRules() { }

    public static final class Rule {
        public final int minProducts;
        public final int percent;

        public Rule(int minProducts, int percent) {
            this.minProducts = minProducts;
            this.percent = percent;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Rule && ((Rule) o).minProducts == minProducts && ((Rule) o).percent == percent;
        }

        @Override
        public int hashCode() {
            return minProducts * 1000 + percent;
        }
    }

    /**
     * Đọc categoryDiscounts từ Firestore. Chấp nhận Map {minProducts, discountPercent} (mới) hoặc số
     * (dữ liệu cũ chỉ có phần trăm → coi như mua từ 1 sản phẩm). Bỏ qua mục không hợp lệ.
     */
    public static Map<String, Rule> parse(Object raw) {
        Map<String, Rule> out = new LinkedHashMap<>();
        if (!(raw instanceof Map)) return out;
        for (Map.Entry<?, ?> en : ((Map<?, ?>) raw).entrySet()) {
            if (!(en.getKey() instanceof String)) continue;
            String name = (String) en.getKey();
            Object v = en.getValue();
            int min = 0, pct = 0;
            if (v instanceof Map) {
                Map<?, ?> m = (Map<?, ?>) v;
                min = toInt(m.get("minProducts"));
                pct = toInt(m.get("discountPercent"));
            } else if (v instanceof Number) {
                min = 1;
                pct = ((Number) v).intValue();
            }
            if (min > 0 && pct > 0 && pct <= MAX_PERCENT) out.put(name, new Rule(min, pct));
        }
        return out;
    }

    private static int toInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o instanceof String) {
            try {
                return Integer.parseInt(((String) o).trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /** Dạng ghi lên Firestore. */
    public static Map<String, Object> toFirestore(Map<String, Rule> rules) {
        Map<String, Object> out = new HashMap<>();
        if (rules == null) return out;
        for (Map.Entry<String, Rule> en : rules.entrySet()) {
            Rule r = en.getValue();
            if (r == null || r.minProducts <= 0 || r.percent <= 0) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("minProducts", r.minProducts);
            m.put("discountPercent", r.percent);
            out.put(en.getKey(), m);
        }
        return out;
    }

    /** Kiểm tra hai ô nhập; trả về lỗi theo ô (phần tử [0] = số sản phẩm, [1] = phần trăm), null = ổn. */
    public static String[] validate(String minText, String percentText) {
        String[] err = new String[2];
        Integer min = parseIntOrNull(minText);
        Integer pct = parseIntOrNull(percentText);
        if (min == null) err[0] = "Nhập số sản phẩm tối thiểu";
        else if (min < 1 || min > MAX_MIN_PRODUCTS) err[0] = "Từ 1 đến " + MAX_MIN_PRODUCTS;
        if (pct == null) err[1] = "Nhập % giảm giá";
        else if (pct < 1 || pct > MAX_PERCENT) err[1] = "Từ 1 đến " + MAX_PERCENT;
        return err;
    }

    public static boolean hasError(String[] err) {
        return err[0] != null || err[1] != null;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** "Mua từ 3 sản phẩm → giảm 10%". */
    public static String describe(Rule r) {
        if (r == null) return "Chưa có khuyến mãi";
        return "Mua từ " + r.minProducts + " sản phẩm → giảm " + r.percent + "%";
    }

    /** Hai bộ quy tắc giống nhau (để biết đã sửa gì chưa mà bật nút Lưu). */
    public static boolean same(Map<String, Rule> a, Map<String, Rule> b) {
        Map<String, Rule> x = a == null ? new HashMap<>() : a;
        Map<String, Rule> y = b == null ? new HashMap<>() : b;
        return x.equals(y);
    }
}
