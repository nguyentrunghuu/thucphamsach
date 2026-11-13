package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.UngDung.ThucPhamXanh.Helper.DashboardStats;
import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import org.junit.Test;

import java.time.LocalDate;

public class DashboardStatsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    @Test
    public void doanhThu_chiTinhDonDaGiao_theoNgayVaThang() {
        DashboardStats s = new DashboardStats(TODAY);
        s.addOrder((long) OrderStatus.DA_GIAO, "13/11/2026", "255.000");   // hôm nay
        s.addOrder((long) OrderStatus.DA_GIAO, "01/11/2026", 100000L);     // cùng tháng
        s.addOrder((long) OrderStatus.DA_GIAO, "30/10/2026", "50000");     // tháng trước
        s.addOrder((long) OrderStatus.DA_GIAO, "13/11/2025", "70,000");    // cùng ngày-tháng, khác năm
        s.addOrder((long) OrderStatus.DA_HUY, "13/11/2026", "999.000");    // hủy: không tính
        s.addOrder((long) OrderStatus.DANG_GIAO, "13/11/2026", "888.000"); // chưa giao: không tính

        assertEquals(255_000, s.getRevenueToday());
        assertEquals(355_000, s.getRevenueMonth());
        assertEquals(475_000, s.getRevenueTotal());
        assertEquals(4, s.getDeliveredOrders());
        assertEquals(1, s.getCancelledOrders());
        assertEquals(6, s.getTotalOrders());
        assertEquals(3, s.getOrdersToday());
    }

    @Test
    public void donCho_laDaDatVaDangXuLy() {
        DashboardStats s = new DashboardStats(TODAY);
        s.addOrder((long) OrderStatus.DA_DAT, "13/11/2026", "1");
        s.addOrder((long) OrderStatus.DANG_XU_LY, "12/11/2026", "1");
        s.addOrder((long) OrderStatus.DA_XAC_NHAN, "12/11/2026", "1");
        assertEquals(2, s.getPendingOrders());
        assertEquals(0, s.getRevenueTotal());
    }

    @Test
    public void duLieuThieu_khongCrash() {
        DashboardStats s = new DashboardStats(TODAY);
        s.addOrder(null, null, null);
        s.addOrder((long) OrderStatus.DA_GIAO, "", null);
        s.addOrder((long) OrderStatus.DA_GIAO, "abc", "xyz");
        assertEquals(3, s.getTotalOrders());
        assertEquals(0, s.getOrdersToday());
        assertEquals(0, s.getRevenueTotal());
        assertEquals(0, s.getRevenueMonth());
    }

    @Test
    public void docNgay() {
        assertEquals(LocalDate.of(2026, 2, 1), DashboardStats.parseDate("1/2/2026"));
        assertEquals(LocalDate.of(2026, 11, 13), DashboardStats.parseDate(" 13/11/2026 "));
        assertNull(DashboardStats.parseDate("31/02/2026"));  // ngày không tồn tại
        assertNull(DashboardStats.parseDate("2026-11-13"));
        assertNull(DashboardStats.parseDate("13/11/26"));
        assertNull(DashboardStats.parseDate(null));
    }

    @Test
    public void khoaSapXep_moiNhatLonNhat() {
        assertTrue(DashboardStats.sortKey("13/11/2026") > DashboardStats.sortKey("31/10/2026"));
        assertTrue(DashboardStats.sortKey("01/01/2027") > DashboardStats.sortKey("31/12/2026"));
        assertEquals(0, DashboardStats.sortKey("sai"));
    }

    @Test
    public void tonKho() {
        assertTrue(DashboardStats.isOutOfStock(0));
        assertTrue(DashboardStats.isOutOfStock(-1));
        assertFalse(DashboardStats.isOutOfStock(1));
        assertTrue(DashboardStats.isLowStock(0));
        assertTrue(DashboardStats.isLowStock(DashboardStats.LOW_STOCK_THRESHOLD));
        assertFalse(DashboardStats.isLowStock(DashboardStats.LOW_STOCK_THRESHOLD + 1));
    }

    @Test
    public void hanSuDung() {
        assertEquals(Long.valueOf(0), DashboardStats.daysUntilExpiry("13/11/2026", TODAY));
        assertEquals(Long.valueOf(-1), DashboardStats.daysUntilExpiry("12/11/2026", TODAY));
        assertEquals(Long.valueOf(49), DashboardStats.daysUntilExpiry("01/01/2027", TODAY));
        assertNull(DashboardStats.daysUntilExpiry("", TODAY));

        assertTrue(DashboardStats.isExpired("12/11/2026", TODAY));
        assertFalse(DashboardStats.isExpired("13/11/2026", TODAY));   // hết hạn cuối ngày hôm nay
        assertFalse(DashboardStats.isExpired(null, TODAY));

        assertTrue(DashboardStats.isExpiringSoon("13/11/2026", TODAY));
        assertTrue(DashboardStats.isExpiringSoon("15/11/2026", TODAY));
        assertFalse(DashboardStats.isExpiringSoon("16/11/2026", TODAY));
        assertFalse(DashboardStats.isExpiringSoon("12/11/2026", TODAY)); // đã quá hạn: không phải "sắp"
        assertFalse(DashboardStats.isExpiringSoon("không rõ", TODAY));
    }
}
