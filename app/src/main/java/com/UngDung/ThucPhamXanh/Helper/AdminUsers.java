package com.UngDung.ThucPhamXanh.Helper;

import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Danh sách khách hàng trang admin: ghép IDUser (uid + email) với User/{uid}/Profile, thống kê đơn
 * theo UID, tìm kiếm không dấu và sắp xếp — thuần Java, test được.
 */
public final class AdminUsers {

    private AdminUsers() { }

    public static final int SORT_NAME = 0;
    public static final int SORT_SPENT = 1;   // chi tiêu cao → thấp
    public static final int SORT_ORDERS = 2;  // nhiều đơn → ít đơn

    public static final class Row {
        public String uid = "";
        public String email = "";
        public String name = "";
        public String phone = "";
        public String address = "";
        public String birth = "";
        public String gender = "";
        public String avatar = "";
        public int orders;          // tổng số đơn (trừ đơn đã hủy)
        public int delivered;       // số đơn đã giao
        public long spent;          // tổng tiền các đơn đã giao

        public String displayName() {
            if (name != null && !name.trim().isEmpty()) return name.trim();
            if (email != null && email.contains("@")) return email.substring(0, email.indexOf('@'));
            return "Khách hàng";
        }
    }

    public static final class Stat {
        public int orders;
        public int delivered;
        public long spent;
    }

    /** Đơn theo UID: đơn đã hủy không tính vào "số đơn"; chi tiêu chỉ tính đơn đã giao. */
    public static Map<String, Stat> statsByUser(List<AdminOrder> orders) {
        Map<String, Stat> map = new HashMap<>();
        if (orders == null) return map;
        for (AdminOrder o : orders) {
            if (o == null || o.uid == null || o.uid.isEmpty()) continue;
            Stat s = map.get(o.uid);
            if (s == null) {
                s = new Stat();
                map.put(o.uid, s);
            }
            if (o.status != OrderStatus.DA_HUY) s.orders++;
            if (o.status == OrderStatus.DA_GIAO) {
                s.delivered++;
                s.spent += o.total;
            }
        }
        return map;
    }

    /** Gắn thống kê vào từng dòng. */
    public static void applyStats(List<Row> rows, Map<String, Stat> stats) {
        if (rows == null) return;
        for (Row r : rows) {
            Stat s = stats == null ? null : stats.get(r.uid);
            r.orders = s == null ? 0 : s.orders;
            r.delivered = s == null ? 0 : s.delivered;
            r.spent = s == null ? 0 : s.spent;
        }
    }

    /** Lọc theo tên / email / số điện thoại (số: so theo chữ số) rồi sắp xếp. */
    public static List<Row> filterAndSort(List<Row> all, String query, int sort) {
        List<Row> out = new ArrayList<>();
        if (all == null) return out;
        String q = TextNormalizer.normalize(query);
        String digits = q.replaceAll("[^0-9]", "");
        boolean phoneLike = !digits.isEmpty() && q.replaceAll("[0-9\\s.+-]", "").isEmpty();
        for (Row r : all) {
            if (r == null) continue;
            if (!q.isEmpty()) {
                boolean hit = TextNormalizer.normalize(r.displayName()).contains(q)
                        || TextNormalizer.normalize(r.name).contains(q)
                        || TextNormalizer.normalize(r.email).contains(q)
                        || (phoneLike && r.phone != null && r.phone.replaceAll("[^0-9]", "").contains(digits));
                if (!hit) continue;
            }
            out.add(r);
        }
        Collections.sort(out, (a, b) -> {
            int c;
            switch (sort) {
                case SORT_SPENT: c = Long.compare(b.spent, a.spent); break;
                case SORT_ORDERS: c = Integer.compare(b.orders, a.orders); break;
                default: c = 0;
            }
            if (c != 0) return c;
            return TextNormalizer.normalize(a.displayName()).compareTo(TextNormalizer.normalize(b.displayName()));
        });
        return out;
    }
}
