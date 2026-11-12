package com.UngDung.ThucPhamXanh.Helper;

import com.UngDung.ThucPhamXanh.Models.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Logic tìm kiếm sản phẩm (thuần Java, không phụ thuộc Android -> test được):
 * tìm theo từ khóa không phân biệt dấu + lọc danh mục / khoảng giá / còn hàng + sắp xếp.
 */
public class ProductSearch {

    public static final int SORT_RELEVANT = 0;
    public static final int SORT_BESTSELLER = 1;
    public static final int SORT_PRICE_ASC = 2;
    public static final int SORT_PRICE_DESC = 3;

    private final Map<String, Long> soldById = new HashMap<>();

    private String query = "";
    private String category = null;          // null = tất cả danh mục
    private long minPrice = 0;
    private long maxPrice = Long.MAX_VALUE;
    private boolean inStockOnly = false;
    private int sort = SORT_RELEVANT;

    // ---------- Cấu hình ----------

    public void setSoldCounts(Map<String, Long> sold) {
        soldById.clear();
        if (sold != null) soldById.putAll(sold);
    }

    public void setQuery(String q) { query = q == null ? "" : q; }
    public void setSort(int s) { sort = s; }
    public void setFilters(String category, long minPrice, long maxPrice, boolean inStockOnly) {
        this.category = category;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.inStockOnly = inStockOnly;
    }

    public int getSort() { return sort; }
    public String getCategory() { return category; }
    public long getMinPrice() { return minPrice; }
    public long getMaxPrice() { return maxPrice; }
    public boolean isInStockOnly() { return inStockOnly; }

    public long soldOf(Product p) {
        Long v = soldById.get(p.getId());
        return v == null ? 0 : v;
    }

    /** Số bộ lọc đang bật (để hiện trên nút "Bộ lọc"). */
    public int activeFilterCount() {
        int n = 0;
        if (category != null) n++;
        if (minPrice > 0 || maxPrice != Long.MAX_VALUE) n++;
        if (inStockOnly) n++;
        return n;
    }

    public boolean hasQueryOrFilter() {
        return !TextNormalizer.normalize(query).isEmpty() || activeFilterCount() > 0 || sort != SORT_RELEVANT;
    }

    // ---------- Tính kết quả ----------

    public List<Product> apply(List<Product> all) {
        String q = TextNormalizer.normalize(query);
        final Map<Product, Integer> score = new HashMap<>();
        List<Product> result = new ArrayList<>();
        for (Product p : all) {
            if (category != null && !category.equals(p.getLoaisp())) continue;
            if (p.getGiatien() < minPrice || p.getGiatien() > maxPrice) continue;
            if (inStockOnly && p.getSoluong() <= 0) continue;
            int s = relevance(p, q);
            if (!q.isEmpty() && s == 0) continue;
            score.put(p, s);
            result.add(p);
        }

        Comparator<Product> bySold = (a, b) -> Long.compare(soldOf(b), soldOf(a));
        switch (sort) {
            case SORT_BESTSELLER:
                Collections.sort(result, bySold);
                break;
            case SORT_PRICE_ASC:
                Collections.sort(result, (a, b) -> Long.compare(a.getGiatien(), b.getGiatien()));
                break;
            case SORT_PRICE_DESC:
                Collections.sort(result, (a, b) -> Long.compare(b.getGiatien(), a.getGiatien()));
                break;
            default:
                // Phù hợp nhất: điểm khớp cao trước, bằng điểm thì món bán chạy trước
                Collections.sort(result, (a, b) -> {
                    int c = Integer.compare(score.get(b), score.get(a));
                    return c != 0 ? c : bySold.compare(a, b);
                });
        }
        return result;
    }

    /** Điểm khớp: tên bắt đầu bằng từ khóa > một từ trong tên bắt đầu bằng từ khóa > tên chứa > danh mục/mô tả chứa. */
    static int relevance(Product p, String normalizedQuery) {
        String q = normalizedQuery;
        if (q.isEmpty()) return 1;
        String name = TextNormalizer.normalize(p.getTensp());
        if (name.startsWith(q)) return 4;
        if ((" " + name).contains(" " + q)) return 3;
        if (name.contains(q)) return 2;
        if (TextNormalizer.normalize(p.getLoaisp()).contains(q)
                || TextNormalizer.normalize(p.getMota()).contains(q)) return 1;
        return 0;
    }
}
