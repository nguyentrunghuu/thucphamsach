package com.UngDung.ThucPhamXanh.Helper;

import com.google.firebase.firestore.DocumentSnapshot;

import java.time.LocalDate;

/**
 * Sản phẩm nào khách được thấy / mua.
 *
 *  - Xóa mềm: admin "xóa" chỉ đặt daxoa = true để giữ hóa đơn, thống kê, lịch sử mua hàng.
 *    → {@link #isHidden}: dùng ở MỌI nơi (cả admin).
 *  - Hết hạn: quá ngày hansudung ("dd/MM/yyyy") thì ngừng bán nhưng admin vẫn thấy để gia hạn / xử lý.
 *    → {@link #isHiddenFromShop}: dùng ở mọi danh sách phía khách (trang chủ, danh mục, tìm kiếm...).
 *  Sản phẩm không có / sai hansudung vẫn bán (không đủ thông tin để chặn).
 */
public final class ProductVisibility {
    public static final String FIELD_DELETED = "daxoa";
    public static final String FIELD_EXPIRY = "hansudung";

    private ProductVisibility() { }

    /** Đã xóa mềm (hoặc document không tồn tại). */
    public static boolean isHidden(DocumentSnapshot doc) {
        return doc == null || !doc.exists() || Boolean.TRUE.equals(doc.getBoolean(FIELD_DELETED));
    }

    /** Khách không được thấy: đã xóa mềm hoặc đã quá hạn sử dụng. */
    public static boolean isHiddenFromShop(DocumentSnapshot doc) {
        if (isHidden(doc)) return true;
        Object hsd = doc.get(FIELD_EXPIRY);
        return isExpired(hsd == null ? null : hsd.toString(), LocalDate.now());
    }

    /** Quá hạn: ngày hansudung đã qua (hết hạn hôm nay vẫn bán đến hết ngày). */
    public static boolean isExpired(String hansudung, LocalDate today) {
        return DashboardStats.isExpired(hansudung, today);
    }

    /** Lý do không bán được (để hiện cho khách); bán được → null. */
    public static String unavailableReason(String hansudung, long soluong, LocalDate today) {
        if (isExpired(hansudung, today)) return "Sản phẩm đã hết hạn sử dụng, tạm ngừng bán";
        if (soluong <= 0) return "Sản phẩm đã hết hàng";
        return null;
    }
}
