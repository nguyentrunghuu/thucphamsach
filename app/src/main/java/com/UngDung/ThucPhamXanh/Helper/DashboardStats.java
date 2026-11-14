package com.UngDung.ThucPhamXanh.Helper;

import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Số liệu dashboard admin, tính thuần (không đụng Firebase) để unit test được.
 *
 * Cách dùng: tạo với ngày hôm nay, gọi {@link #addOrder} cho từng hóa đơn rồi đọc getter.
 * Các hàm static về tồn kho / hạn sử dụng dùng cho danh sách "Cần chú ý" và badge sản phẩm.
 *
 * Quy ước dữ liệu:
 *   - HoaDon.ngaydat và SanPham.hansudung là chuỗi "dd/MM/yyyy" (chấp nhận "1/2/2026").
 *   - HoaDon.tongtien đọc qua {@link MoneyUtil#parseVnd} (chuỗi định dạng / chuỗi số / số).
 *   - Doanh thu chỉ tính đơn ĐÃ GIAO, theo ngày đặt (hóa đơn không lưu ngày giao).
 */
public final class DashboardStats {

    /** Còn từ chừng này sản phẩm trở xuống là "sắp hết hàng". */
    public static final long LOW_STOCK_THRESHOLD = 5;
    /** Còn từ chừng này ngày trở xuống (tính cả hôm nay) là "sắp hết hạn". */
    public static final int EXPIRY_WARNING_DAYS = 2;

    private final LocalDate today;

    private int totalOrders;
    private int ordersToday;
    private int pendingOrders;
    private int deliveredOrders;
    private int cancelledOrders;
    private long revenueToday;
    private long revenueMonth;
    private long revenueTotal;

    public DashboardStats(LocalDate today) {
        if (today == null) throw new IllegalArgumentException("today == null");
        this.today = today;
    }

    /**
     * Cộng một hóa đơn vào thống kê.
     * @param status   trường "trangthai" (null nếu thiếu)
     * @param ngaydat  trường "ngaydat"
     * @param tongtien trường "tongtien" (String hoặc Number)
     */
    public void addOrder(Long status, String ngaydat, Object tongtien) {
        totalOrders++;
        LocalDate date = parseDate(ngaydat);
        boolean isToday = today.equals(date);
        boolean isThisMonth = date != null
                && date.getYear() == today.getYear()
                && date.getMonthValue() == today.getMonthValue();

        if (isToday) ordersToday++;
        if (status == null) return;

        if (AdminOrderFlow.isPending(status)) pendingOrders++;
        if (status == OrderStatus.DA_HUY) cancelledOrders++;
        if (status == OrderStatus.DA_GIAO) {
            deliveredOrders++;
            long money = MoneyUtil.parseVnd(tongtien);
            revenueTotal += money;
            if (isThisMonth) revenueMonth += money;
            if (isToday) revenueToday += money;
        }
    }

    public int getTotalOrders() { return totalOrders; }
    public int getOrdersToday() { return ordersToday; }
    public int getPendingOrders() { return pendingOrders; }
    public int getDeliveredOrders() { return deliveredOrders; }
    public int getCancelledOrders() { return cancelledOrders; }
    public long getRevenueToday() { return revenueToday; }
    public long getRevenueMonth() { return revenueMonth; }
    public long getRevenueTotal() { return revenueTotal; }

    // ------------------------------------------------------------------ ngày tháng

    /** "dd/MM/yyyy" → LocalDate; rỗng / sai định dạng / ngày không tồn tại → null. */
    public static LocalDate parseDate(String ddMMyyyy) {
        if (ddMMyyyy == null) return null;
        String[] p = ddMMyyyy.trim().split("/");
        if (p.length != 3) return null;
        try {
            int d = Integer.parseInt(p[0].trim());
            int m = Integer.parseInt(p[1].trim());
            int y = Integer.parseInt(p[2].trim());
            if (y < 1000 || y > 9999) return null;
            return LocalDate.of(y, m, d);
        } catch (NumberFormatException | DateTimeException e) {
            return null;
        }
    }

    /** Khóa sắp xếp yyyyMMdd (mới nhất = lớn nhất); ngày không đọc được → 0 (xuống cuối). */
    public static long sortKey(String ddMMyyyy) {
        LocalDate d = parseDate(ddMMyyyy);
        return d == null ? 0 : d.getYear() * 10000L + d.getMonthValue() * 100L + d.getDayOfMonth();
    }

    // ------------------------------------------------------------------ tồn kho

    public static boolean isOutOfStock(long soluong) {
        return soluong <= 0;
    }

    /** Sắp hết hàng (gồm cả hết hàng). */
    public static boolean isLowStock(long soluong) {
        return soluong <= LOW_STOCK_THRESHOLD;
    }

    // ------------------------------------------------------------------ hạn sử dụng

    /**
     * Số ngày còn lại tới hạn sử dụng: 0 = hết hạn cuối ngày hôm nay, âm = đã quá hạn.
     * Không có / sai định dạng → null (không cảnh báo, vì không biết).
     */
    public static Long daysUntilExpiry(String hansudung, LocalDate today) {
        LocalDate exp = parseDate(hansudung);
        if (exp == null || today == null) return null;
        return ChronoUnit.DAYS.between(today, exp);
    }

    public static boolean isExpired(String hansudung, LocalDate today) {
        Long days = daysUntilExpiry(hansudung, today);
        return days != null && days < 0;
    }

    /** Còn hạn nhưng chỉ còn ≤ {@link #EXPIRY_WARNING_DAYS} ngày. */
    public static boolean isExpiringSoon(String hansudung, LocalDate today) {
        Long days = daysUntilExpiry(hansudung, today);
        return days != null && days >= 0 && days <= EXPIRY_WARNING_DAYS;
    }

    // ------------------------------------------------------------------ cảnh báo sản phẩm

    /**
     * Cảnh báo của một sản phẩm (danh sách "Cần chú ý" trên dashboard).
     * Mỗi sản phẩm có tối đa một nhãn tồn kho và một nhãn hạn dùng.
     */
    public static final class ProductAlert {
        /** Mức nghiêm trọng, lớn hơn = xếp trên. */
        public static final int EXPIRED = 4, OUT_OF_STOCK = 3, EXPIRING = 2, LOW_STOCK = 1;

        public final int stockLevel;   // 0, LOW_STOCK hoặc OUT_OF_STOCK
        public final int expiryLevel;  // 0, EXPIRING hoặc EXPIRED
        public final String stockLabel;   // "Hết hàng" / "Còn 3" / null
        public final String expiryLabel;  // "Hết hạn" / "Hết hạn hôm nay" / "Còn 2 ngày" / null
        public final long stock;          // số lượng tồn
        public final Long daysLeft;       // số ngày còn hạn (âm = quá hạn), null = không rõ

        ProductAlert(int stockLevel, String stockLabel, int expiryLevel, String expiryLabel,
                     long stock, Long daysLeft) {
            this.stockLevel = stockLevel;
            this.stockLabel = stockLabel;
            this.expiryLevel = expiryLevel;
            this.expiryLabel = expiryLabel;
            this.stock = stock;
            this.daysLeft = daysLeft;
        }

        public int severity() {
            return Math.max(stockLevel, expiryLevel);
        }

        /**
         * Gấp hơn đứng trước: mức nghiêm trọng cao hơn → ít ngày còn hạn hơn → ít hàng tồn hơn.
         * (Không xét tên; nơi gọi tự so tên nếu cần thứ tự ổn định.)
         */
        public static final java.util.Comparator<ProductAlert> URGENCY = (a, b) -> {
            int c = Integer.compare(b.severity(), a.severity());
            if (c != 0) return c;
            long da = a.daysLeft == null ? Long.MAX_VALUE : a.daysLeft;
            long db = b.daysLeft == null ? Long.MAX_VALUE : b.daysLeft;
            c = Long.compare(da, db);
            if (c != 0) return c;
            return Long.compare(a.stock, b.stock);
        };
    }

    /** Sản phẩm có cần admin chú ý không; ổn → null. */
    public static ProductAlert checkProduct(long soluong, String hansudung, LocalDate today) {
        int stockLevel = 0;
        String stockLabel = null;
        if (isOutOfStock(soluong)) {
            stockLevel = ProductAlert.OUT_OF_STOCK;
            stockLabel = "Hết hàng";
        } else if (isLowStock(soluong)) {
            stockLevel = ProductAlert.LOW_STOCK;
            stockLabel = "Còn " + soluong;
        }

        int expiryLevel = 0;
        String expiryLabel = null;
        Long days = daysUntilExpiry(hansudung, today);
        if (days != null) {
            if (days < 0) {
                expiryLevel = ProductAlert.EXPIRED;
                expiryLabel = "Hết hạn";
            } else if (days <= EXPIRY_WARNING_DAYS) {
                expiryLevel = ProductAlert.EXPIRING;
                expiryLabel = days == 0 ? "Hết hạn hôm nay" : "Còn " + days + " ngày";
            }
        }

        if (stockLevel == 0 && expiryLevel == 0) return null;
        return new ProductAlert(stockLevel, stockLabel, expiryLevel, expiryLabel, soluong, days);
    }

    // ------------------------------------------------------------------ hiển thị ngày

    private static final String[] WEEKDAYS = {
            "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};

    /** 2026-11-13 → "Thứ Sáu, 13/11/2026". */
    public static String formatLongDate(LocalDate d) {
        return WEEKDAYS[d.getDayOfWeek().getValue() - 1] + ", "
                + String.format(java.util.Locale.ROOT, "%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }
}
