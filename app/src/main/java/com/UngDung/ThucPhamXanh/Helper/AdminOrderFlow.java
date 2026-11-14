package com.UngDung.ThucPhamXanh.Helper;

import com.UngDung.ThucPhamXanh.Models.OrderStatus;

/**
 * Quy tắc admin được đổi trạng thái đơn hàng sang đâu (chỉ đi tới, không lùi, không nhảy cóc qua "Đang giao").
 *
 * Luồng: 1 Đã đặt → 2 Đang xử lý → 5 Đã xác nhận → 4 Đang giao → 3 Đã giao
 *   - Từ "Đã đặt" admin được xác nhận luôn (bỏ qua bước "Đang xử lý").
 *   - Admin được hủy (→ 6) khi đơn CHƯA giao cho shipper (1, 2, 5).
 *   - 3 Đã giao và 6 Đã hủy là trạng thái cuối, không đổi được nữa.
 *   - Mã lạ (dữ liệu cũ/lỗi) → không cho đổi, tránh ghi đè sai.
 *
 * Dùng chung cho danh sách đơn và chi tiết đơn của admin. Mọi mã lấy từ {@link OrderStatus}.
 */
public final class AdminOrderFlow {

    private static final int[] NONE = new int[0];

    private AdminOrderFlow() { }

    /** Các trạng thái admin được chọn tiếp theo, theo thứ tự hiển thị (hủy luôn ở cuối). */
    public static int[] nextStatuses(long current) {
        switch ((int) current) {
            case OrderStatus.DA_DAT:
                return new int[]{OrderStatus.DANG_XU_LY, OrderStatus.DA_XAC_NHAN, OrderStatus.DA_HUY};
            case OrderStatus.DANG_XU_LY:
                return new int[]{OrderStatus.DA_XAC_NHAN, OrderStatus.DA_HUY};
            case OrderStatus.DA_XAC_NHAN:
                return new int[]{OrderStatus.DANG_GIAO, OrderStatus.DA_HUY};
            case OrderStatus.DANG_GIAO:
                return new int[]{OrderStatus.DA_GIAO};
            default:
                return NONE; // DA_GIAO, DA_HUY, hoặc mã không hợp lệ
        }
    }

    /** Admin có được chuyển đơn từ {@code from} sang {@code to} không. */
    public static boolean canTransition(long from, long to) {
        if (from != (int) from) return false; // giá trị long ngoài phạm vi int
        for (int s : nextStatuses(from)) {
            if (s == to) return true;
        }
        return false;
    }

    /** Admin giao đơn cho shipper (kèm chọn shipper) được khi đơn "Đã xác nhận". */
    public static boolean canShip(long current) {
        return current == OrderStatus.DA_XAC_NHAN;
    }

    /** Admin gán / đổi shipper được khi đơn đang "Đang giao" (đơn cũ có thể chưa có shipper). */
    public static boolean canAssignShipper(long current) {
        return current == OrderStatus.DANG_GIAO;
    }

    /** Admin được hủy đơn khi đơn chưa giao cho shipper. */
    public static boolean adminCanCancel(long current) {
        return canTransition(current, OrderStatus.DA_HUY);
    }

    /** Đơn đang chờ cửa hàng xử lý (hiện ở mục "Chờ xác nhận" và đếm "đơn mới" trên dashboard). */
    public static boolean isPending(long status) {
        return status == OrderStatus.DA_DAT || status == OrderStatus.DANG_XU_LY;
    }
}
