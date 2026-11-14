package com.UngDung.ThucPhamXanh.Helper;

import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lọc danh sách đơn trang admin (thuần Java → unit test được).
 *
 * Tab trạng thái: "Chờ xác nhận" gộp Đã đặt + Đang xử lý (cùng nghĩa với dashboard), các tab còn lại
 * ứng với một mã {@link OrderStatus}. Tìm kiếm không dấu theo tên, SĐT, mã đơn, document id, địa chỉ.
 * Sắp xếp: đơn mới nhất (ngaydat) lên đầu; cùng ngày thì giữ ổn định theo id.
 */
public final class AdminOrderFilter {

    public static final int TAB_ALL = 0;
    public static final int TAB_PENDING = 1;     // 1 + 2
    public static final int TAB_CONFIRMED = 2;   // 5
    public static final int TAB_SHIPPING = 3;    // 4
    public static final int TAB_DELIVERED = 4;   // 3
    public static final int TAB_CANCELLED = 5;   // 6
    public static final int TAB_COUNT = 6;

    private static final String[] TAB_LABELS = {
            "Tất cả", "Chờ xác nhận", "Đã xác nhận", "Đang giao", "Đã giao", "Đã hủy"};

    private int tab = TAB_ALL;
    private String query = "";

    public void setTab(int t) { tab = (t >= 0 && t < TAB_COUNT) ? t : TAB_ALL; }
    public void setQuery(String q) { query = q == null ? "" : q; }
    public int getTab() { return tab; }
    public String getQuery() { return query; }

    public boolean isFiltering() {
        return tab != TAB_ALL || !TextNormalizer.normalize(query).isEmpty();
    }

    public static String tabLabel(int tab) {
        return (tab >= 0 && tab < TAB_COUNT) ? TAB_LABELS[tab] : TAB_LABELS[0];
    }

    /** Đơn có thuộc tab không. */
    public static boolean inTab(long status, int tab) {
        switch (tab) {
            case TAB_ALL: return true;
            case TAB_PENDING: return AdminOrderFlow.isPending(status);
            case TAB_CONFIRMED: return status == OrderStatus.DA_XAC_NHAN;
            case TAB_SHIPPING: return status == OrderStatus.DANG_GIAO;
            case TAB_DELIVERED: return status == OrderStatus.DA_GIAO;
            case TAB_CANCELLED: return status == OrderStatus.DA_HUY;
            default: return false;
        }
    }

    /** Số đơn của từng tab (chỉ áp từ khóa, không áp tab) — để ghi lên chip. */
    public int[] countByTab(List<AdminOrder> all) {
        int[] counts = new int[TAB_COUNT];
        if (all == null) return counts;
        String q = TextNormalizer.normalize(query);
        for (AdminOrder o : all) {
            if (o == null || (!q.isEmpty() && !matches(o, q))) continue;
            for (int t = 0; t < TAB_COUNT; t++) {
                if (inTab(o.status, t)) counts[t]++;
            }
        }
        return counts;
    }

    public List<AdminOrder> apply(List<AdminOrder> all) {
        List<AdminOrder> result = new ArrayList<>();
        if (all == null) return result;
        String q = TextNormalizer.normalize(query);
        for (AdminOrder o : all) {
            if (o == null || !inTab(o.status, tab)) continue;
            if (!q.isEmpty() && !matches(o, q)) continue;
            result.add(o);
        }
        Collections.sort(result, (a, b) -> {
            int c = Long.compare(DashboardStats.sortKey(b.date), DashboardStats.sortKey(a.date));
            return c != 0 ? c : String.valueOf(a.id).compareTo(String.valueOf(b.id));
        });
        return result;
    }

    private static boolean matches(AdminOrder o, String q) {
        String digits = q.replace(" ", "");
        return TextNormalizer.normalize(o.name).contains(q)
                || TextNormalizer.normalize(o.code).contains(q)
                || TextNormalizer.normalize(o.id).contains(q)
                || TextNormalizer.normalize(o.address).contains(q)
                || (!digits.isEmpty() && o.phone != null && o.phone.replace(" ", "").contains(digits));
    }
}
