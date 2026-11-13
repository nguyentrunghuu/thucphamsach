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
}
