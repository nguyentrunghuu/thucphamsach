package com.UngDung.ThucPhamXanh.Helper;

import com.UngDung.ThucPhamXanh.Models.Product;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lọc danh sách sản phẩm trang admin (thuần Java → unit test được).
 *
 *  - Từ khóa: không phân biệt dấu, khớp tên / loại / mô tả (dùng {@link TextNormalizer}).
 *  - Loại: null = tất cả.
 *  - Trạng thái: {@link #STATUS_ALL}, {@link #STATUS_LOW_STOCK} (còn ≤ 5, gồm hết hàng),
 *    {@link #STATUS_EXPIRY} (đã hết hạn hoặc còn ≤ 2 ngày) — ngưỡng lấy từ {@link DashboardStats}.
 *  - Sắp xếp: khi lọc theo trạng thái thì gấp nhất lên đầu, còn lại theo tên (không dấu).
 */
public final class AdminProductFilter {

    public static final int STATUS_ALL = 0;
    public static final int STATUS_LOW_STOCK = 1;
    public static final int STATUS_EXPIRY = 2;

    private LocalDate today;
    private String query = "";
    private String category = null;
    private int status = STATUS_ALL;

    public AdminProductFilter(LocalDate today) {
        if (today == null) throw new IllegalArgumentException("today == null");
        this.today = today;
    }

    public void setQuery(String q) { query = q == null ? "" : q; }
    /** Đổi ngày tính hạn dùng (màn hình mở lại sau nửa đêm). */
    public void setToday(LocalDate d) { if (d != null) today = d; }
    public void setCategory(String c) { category = (c == null || c.isEmpty()) ? null : c; }
    public void setStatus(int s) { status = s; }

    public String getQuery() { return query; }
    public String getCategory() { return category; }
    public int getStatus() { return status; }

    public boolean isFiltering() {
        return !TextNormalizer.normalize(query).isEmpty() || category != null || status != STATUS_ALL;
    }

    public List<Product> apply(List<Product> all) {
        String q = TextNormalizer.normalize(query);
        List<Product> result = new ArrayList<>();
        if (all == null) return result;
        for (Product p : all) {
            if (p == null) continue;
            if (category != null && !category.equals(p.getLoaisp())) continue;
            if (!matchesStatus(p)) continue;
            if (!q.isEmpty() && !matchesQuery(p, q)) continue;
            result.add(p);
        }
        Comparator<Product> byName = (a, b) ->
                TextNormalizer.normalize(a.getTensp()).compareTo(TextNormalizer.normalize(b.getTensp()));
        if (status == STATUS_ALL) {
            Collections.sort(result, byName);
        } else {
            Collections.sort(result, (a, b) -> {
                int c = DashboardStats.ProductAlert.URGENCY.compare(alertOf(a), alertOf(b));
                return c != 0 ? c : byName.compare(a, b);
            });
        }
        return result;
    }

    /** Cảnh báo của sản phẩm (null = ổn). */
    public DashboardStats.ProductAlert alertOf(Product p) {
        return DashboardStats.checkProduct(p.getSoluong(), p.getHansudung(), today);
    }

    private boolean matchesStatus(Product p) {
        switch (status) {
            case STATUS_LOW_STOCK:
                return DashboardStats.isLowStock(p.getSoluong());
            case STATUS_EXPIRY:
                return DashboardStats.isExpired(p.getHansudung(), today)
                        || DashboardStats.isExpiringSoon(p.getHansudung(), today);
            default:
                return true;
        }
    }

    private static boolean matchesQuery(Product p, String normalizedQuery) {
        return TextNormalizer.normalize(p.getTensp()).contains(normalizedQuery)
                || TextNormalizer.normalize(p.getLoaisp()).contains(normalizedQuery)
                || TextNormalizer.normalize(p.getMota()).contains(normalizedQuery);
    }

    /**
     * Số sản phẩm theo loại, giữ thứ tự của {@code categories} (tên loại từ LoaiProduct).
     * Loại có sản phẩm nhưng không nằm trong {@code categories} được thêm vào cuối, để không mất sản phẩm.
     */
    public static Map<String, Integer> countByCategory(List<String> categories, List<Product> all) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (categories != null) {
            for (String c : categories) {
                if (c != null && !c.isEmpty()) counts.put(c, 0);
            }
        }
        if (all != null) {
            for (Product p : all) {
                String c = p == null ? null : p.getLoaisp();
                if (c == null || c.isEmpty()) continue;
                Integer n = counts.get(c);
                counts.put(c, n == null ? 1 : n + 1);
            }
        }
        return counts;
    }
}
