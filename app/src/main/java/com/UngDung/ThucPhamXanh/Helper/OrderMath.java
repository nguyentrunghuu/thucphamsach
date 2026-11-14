package com.UngDung.ThucPhamXanh.Helper;

import java.util.List;

/**
 * Tính tiền chi tiết một đơn (thuần Java → unit test được).
 *
 * Dòng chi tiết (ChitietHoaDon/{uid}/ALL) có thể có "giaBan" = giá lúc đặt. Đơn cũ không có thì phải
 * lấy giá hiện tại của sản phẩm — khi đó {@link Line#priceIsCurrent} = true để màn hình ghi chú.
 * HoaDon.tongtien đã gồm phí ship và đã trừ khuyến mãi; khuyến mãi không được lưu riêng nên suy ra:
 * giảm giá = tạm tính + phí ship − tổng (nếu > 0).
 */
public final class OrderMath {

    private OrderMath() { }

    /** Một sản phẩm trong đơn. */
    public static final class Line {
        public final String productId;
        public final String name;
        public final String image;
        public final long quantity;
        public final long unitPrice;
        public final boolean priceIsCurrent;

        public Line(String productId, String name, String image, long quantity, long unitPrice, boolean priceIsCurrent) {
            this.productId = productId;
            this.name = name;
            this.image = image;
            this.quantity = Math.max(0, quantity);
            this.unitPrice = Math.max(0, unitPrice);
            this.priceIsCurrent = priceIsCurrent;
        }

        public long lineTotal() {
            return unitPrice * quantity;
        }
    }

    /** Giá dùng cho dòng: giá lúc đặt nếu có (> 0), không thì giá hiện tại. */
    public static long unitPrice(Long priceAtOrder, long currentPrice) {
        return priceAtOrder != null && priceAtOrder > 0 ? priceAtOrder : Math.max(0, currentPrice);
    }

    public static long subtotal(List<Line> lines) {
        long s = 0;
        if (lines != null) for (Line l : lines) s += l.lineTotal();
        return s;
    }

    /** Khuyến mãi suy ra; không âm. */
    public static long discount(long subtotal, long shippingFee, long total) {
        return Math.max(0, subtotal + Math.max(0, shippingFee) - total);
    }

    public static long totalQuantity(List<Line> lines) {
        long q = 0;
        if (lines != null) for (Line l : lines) q += l.quantity;
        return q;
    }
}
