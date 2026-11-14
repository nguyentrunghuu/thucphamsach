package com.UngDung.ThucPhamXanh.Helper;

import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Logic thuần Java (unit test được) cho màn "Đơn hàng của tôi" phía khách hàng:
 * gom trạng thái thành 4 tab, tìm kiếm không dấu (mã đơn, tên sản phẩm, người nhận),
 * sắp xếp mới nhất trước, tiến trình đơn, câu mô tả trạng thái, tóm tắt sản phẩm.
 */
public final class CustomerOrders {

    public static final int TAB_ALL = 0;
    public static final int TAB_PROCESSING = 1;  // Đã đặt (1) + Đang xử lý (2) + Đã xác nhận (5)
    public static final int TAB_SHIPPING = 2;    // Đang giao (4)
    public static final int TAB_DONE = 3;        // Đã giao (3)
    public static final int TAB_CANCELLED = 4;   // Đã hủy (6)
    public static final int TAB_COUNT = 5;

    private static final String[] TAB_LABELS = {"Tất cả", "Đang xử lý", "Đang giao", "Đã giao", "Đã hủy"};

    /** Các chặng của thanh tiến trình. */
    public static final String[] STAGE_LABELS = {"Đặt hàng", "Xác nhận", "Vận chuyển", "Hoàn tất"};
    public static final int STAGE_DONE = 2;
    public static final int STAGE_CURRENT = 1;
    public static final int STAGE_TODO = 0;

    private CustomerOrders() { }

    /** Một đơn + các dòng hàng của nó (để hiển thị ảnh / tên sản phẩm ngay trên thẻ). */
    public static final class Entry {
        public final AdminOrder order;
        public final List<OrderMath.Line> lines;

        public Entry(AdminOrder order, List<OrderMath.Line> lines) {
            this.order = order;
            this.lines = lines == null ? new ArrayList<>() : lines;
        }

        public int itemCount() {
            return (int) OrderMath.totalQuantity(lines);
        }
    }

    public static String tabLabel(int tab) {
        return (tab >= 0 && tab < TAB_COUNT) ? TAB_LABELS[tab] : TAB_LABELS[0];
    }

    public static boolean inTab(long status, int tab) {
        switch (tab) {
            case TAB_ALL: return true;
            case TAB_PROCESSING:
                return status == OrderStatus.DA_DAT || status == OrderStatus.DANG_XU_LY
                        || status == OrderStatus.DA_XAC_NHAN;
            case TAB_SHIPPING: return status == OrderStatus.DANG_GIAO;
            case TAB_DONE: return status == OrderStatus.DA_GIAO;
            case TAB_CANCELLED: return status == OrderStatus.DA_HUY;
            default: return false;
        }
    }

    /** Số đơn của từng tab (chỉ áp từ khóa) — ghi lên chip. */
    public static int[] countByTab(List<Entry> all, String query) {
        int[] counts = new int[TAB_COUNT];
        if (all == null) return counts;
        String q = TextNormalizer.normalize(query);
        for (Entry e : all) {
            if (e == null || e.order == null || (!q.isEmpty() && !matches(e, q))) continue;
            for (int t = 0; t < TAB_COUNT; t++) {
                if (inTab(e.order.status, t)) counts[t]++;
            }
        }
        return counts;
    }

    /** Lọc theo tab + từ khóa, đơn mới nhất lên đầu. */
    public static List<Entry> apply(List<Entry> all, int tab, String query) {
        List<Entry> result = new ArrayList<>();
        if (all == null) return result;
        String q = TextNormalizer.normalize(query);
        for (Entry e : all) {
            if (e == null || e.order == null || !inTab(e.order.status, tab)) continue;
            if (!q.isEmpty() && !matches(e, q)) continue;
            result.add(e);
        }
        Collections.sort(result, (a, b) -> {
            int c = Long.compare(DashboardStats.sortKey(b.order.date), DashboardStats.sortKey(a.order.date));
            return c != 0 ? c : String.valueOf(a.order.id).compareTo(String.valueOf(b.order.id));
        });
        return result;
    }

    private static boolean matches(Entry e, String q) {
        AdminOrder o = e.order;
        if (TextNormalizer.normalize(o.code).contains(q)
                || TextNormalizer.normalize(o.id).contains(q)
                || TextNormalizer.normalize(o.name).contains(q)) {
            return true;
        }
        for (OrderMath.Line l : e.lines) {
            if (TextNormalizer.normalize(l.name).contains(q)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- tiến trình & mô tả

    /**
     * Chặng hiện tại: 1 = chờ xác nhận (Đã đặt / Đang xử lý), 2 = đã xác nhận / đang giao,
     * 4 = hoàn tất (mọi chặng xong), -1 = đã hủy (không có tiến trình).
     */
    public static int currentStage(long status) {
        switch ((int) status) {
            case OrderStatus.DA_DAT:
            case OrderStatus.DANG_XU_LY: return 1;
            case OrderStatus.DA_XAC_NHAN:
            case OrderStatus.DANG_GIAO: return 2;
            case OrderStatus.DA_GIAO: return 4;
            default: return -1;
        }
    }

    /** Trạng thái từng chặng (STAGE_DONE / STAGE_CURRENT / STAGE_TODO); đơn hủy → toàn bộ TODO. */
    public static int[] stageStates(long status) {
        int[] states = new int[STAGE_LABELS.length];
        int cur = currentStage(status);
        for (int i = 0; i < states.length; i++) {
            if (cur < 0) states[i] = STAGE_TODO;
            else if (i < cur) states[i] = STAGE_DONE;
            else if (i == cur) states[i] = STAGE_CURRENT;
            else states[i] = STAGE_TODO;
        }
        return states;
    }

    /** Câu nói ngắn gọn về tình trạng đơn, đọc là hiểu ngay đơn đang ở đâu. */
    public static String statusMessage(AdminOrder o) {
        if (o == null) return "";
        switch ((int) o.status) {
            case OrderStatus.DA_DAT:
                return "Đơn đã được gửi, đang chờ cửa hàng xác nhận";
            case OrderStatus.DANG_XU_LY:
                return "Cửa hàng đang xử lý đơn của bạn";
            case OrderStatus.DA_XAC_NHAN:
                return "Cửa hàng đã xác nhận, đang chuẩn bị giao hàng";
            case OrderStatus.DANG_GIAO:
                return hasText(o.shipperName)
                        ? "Shipper " + o.shipperName.trim() + " đang giao đơn đến bạn"
                        : "Đơn đang trên đường giao đến bạn";
            case OrderStatus.DA_GIAO:
                return "Giao hàng thành công. Cảm ơn bạn đã mua sắm!";
            case OrderStatus.DA_HUY:
                return hasText(o.cancelReason)
                        ? "Đã hủy · " + o.cancelReason.trim()
                        : ("admin".equals(o.cancelledBy) ? "Đơn đã được cửa hàng hủy" : "Bạn đã hủy đơn này");
            default:
                return "";
        }
    }

    /** "Giao dự kiến: 17/12/2025 · 10:00 - 12:00" — rỗng nếu đơn không có lịch giao. */
    public static String deliverySchedule(AdminOrder o) {
        if (o == null) return "";
        boolean d = hasText(o.deliveryDate), s = hasText(o.deliverySlot);
        if (!d && !s) return "";
        if (d && s) return o.deliveryDate.trim() + " · " + o.deliverySlot.trim();
        return (d ? o.deliveryDate : o.deliverySlot).trim();
    }

    /** "Củ Sắn" / "Củ Sắn, Hành lá" / "Củ Sắn, Hành lá và 2 sản phẩm khác". */
    public static String itemsSummary(List<OrderMath.Line> lines) {
        if (lines == null || lines.isEmpty()) return "";
        List<String> names = new ArrayList<>();
        for (OrderMath.Line l : lines) {
            if (l != null && hasText(l.name)) names.add(l.name.trim());
        }
        if (names.isEmpty()) return "";
        if (names.size() <= 2) return join(names);
        return names.get(0) + ", " + names.get(1) + " và " + (names.size() - 2) + " sản phẩm khác";
    }

    private static String join(List<String> names) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    /** Đơn đã kết thúc → cho "Mua lại". */
    public static boolean canReorder(long status) {
        return status == OrderStatus.DA_GIAO || status == OrderStatus.DA_HUY;
    }

    /** Chỉ đơn đã giao mới đánh giá được sản phẩm. */
    public static boolean canReview(long status) {
        return status == OrderStatus.DA_GIAO;
    }

    /** Thanh toán online (VNPay / chuyển khoản / MoMo) → "Đã thanh toán"; còn lại là thu tiền khi nhận hàng. */
    public static boolean isPrepaid(String payment) {
        if (!hasText(payment)) return false;
        String p = TextNormalizer.normalize(payment);
        return p.contains("vnpay") || p.contains("momo") || p.contains("chuyen khoan");
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
