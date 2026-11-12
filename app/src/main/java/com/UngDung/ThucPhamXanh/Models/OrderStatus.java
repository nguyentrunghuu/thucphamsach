package com.UngDung.ThucPhamXanh.Models;

/**
 * Mã trạng thái đơn hàng (trường "trangthai" trong collection HoaDon).
 * MỌI màn hình (khách hàng, admin, shipper, thông báo, chatbot) phải dùng chung lớp này.
 *
 * Luồng đơn:  1 Đã đặt → 2 Đang xử lý → 5 Đã xác nhận → 4 Đang giao → 3 Đã giao
 *             (1 hoặc 2) → 6 Đã hủy  (khách tự hủy khi đơn chưa được xác nhận)
 *
 * Lưu ý: trước đây khách hủy đơn bị ghi nhầm thành 4 (trùng "Đang giao").
 * Các đơn hủy cũ đó cần admin sửa tay "trangthai" = 6 trên Firebase Console.
 */
public final class OrderStatus {

    public static final int DA_DAT = 1;
    public static final int DANG_XU_LY = 2;
    public static final int DA_GIAO = 3;
    public static final int DANG_GIAO = 4;
    public static final int DA_XAC_NHAN = 5;
    public static final int DA_HUY = 6;

    private OrderStatus() { }

    /** Tên trạng thái để hiển thị. */
    public static String label(long status) {
        switch ((int) status) {
            case DA_DAT:      return "Đã đặt hàng";
            case DANG_XU_LY:  return "Đang xử lý";
            case DA_XAC_NHAN: return "Đã xác nhận";
            case DANG_GIAO:   return "Đang giao hàng";
            case DA_GIAO:     return "Đã giao";
            case DA_HUY:      return "Đã hủy";
            default:          return "Không xác định";
        }
    }

    /** Cụm từ đứng sau chữ "đã" trong thông báo: "Đơn hàng ... đã <cụm từ>". */
    public static String notifyPhrase(long status) {
        switch ((int) status) {
            case DA_DAT:      return "được đặt thành công";
            case DANG_XU_LY:  return "được tiếp nhận xử lý";
            case DA_XAC_NHAN: return "được xác nhận";
            case DANG_GIAO:   return "được giao cho shipper";
            case DA_GIAO:     return "giao hàng thành công";
            case DA_HUY:      return "bị hủy";
            default:          return "cập nhật trạng thái";
        }
    }

    /** Màu chữ (ARGB) cho nhãn trạng thái. */
    public static int textColor(long status) {
        switch ((int) status) {
            case DA_DAT:
            case DANG_XU_LY:  return 0xFFE65100; // cam
            case DA_XAC_NHAN: return 0xFF6A1B9A; // tím
            case DANG_GIAO:   return 0xFF1565C0; // xanh dương
            case DA_GIAO:     return 0xFF2E7D32; // xanh lá
            case DA_HUY:      return 0xFFC62828; // đỏ
            default:          return 0xFF616161;
        }
    }

    /** Màu nền nhạt (ARGB) cho nhãn trạng thái. */
    public static int backgroundColor(long status) {
        switch ((int) status) {
            case DA_DAT:
            case DANG_XU_LY:  return 0xFFFFF3E0;
            case DA_XAC_NHAN: return 0xFFF3E5F5;
            case DANG_GIAO:   return 0xFFE3F2FD;
            case DA_GIAO:     return 0xFFE8F5E9;
            case DA_HUY:      return 0xFFFFEBEE;
            default:          return 0xFFF5F5F5;
        }
    }

    /** Khách được tự hủy khi đơn chưa được cửa hàng xác nhận. */
    public static boolean customerCanCancel(long status) {
        return status == DA_DAT || status == DANG_XU_LY;
    }

    /** Đơn đã kết thúc (đã giao / đã hủy) thì admin, shipper không được đổi trạng thái nữa. */
    public static boolean isFinal(long status) {
        return status == DA_GIAO || status == DA_HUY;
    }
}
