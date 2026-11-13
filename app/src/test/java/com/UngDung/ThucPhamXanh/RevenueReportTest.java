package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.UngDung.ThucPhamXanh.Helper.MoneyUtil;
import com.UngDung.ThucPhamXanh.Helper.RevenueReport;
import com.UngDung.ThucPhamXanh.Helper.RevenueReport.Range;

import org.junit.Test;

import java.time.LocalDate;

public class RevenueReportTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    @Test
    public void range_lastDaysAndPrevious() {
        Range r = Range.lastDays(TODAY, 7);
        assertEquals(LocalDate.of(2026, 11, 8), r.from);
        assertEquals(7, r.days());
        Range p = r.previous();
        assertEquals(LocalDate.of(2026, 11, 1), p.from);
        assertEquals(LocalDate.of(2026, 11, 7), p.to);
        assertTrue(r.contains(TODAY));
        assertFalse(r.contains(LocalDate.of(2026, 11, 7)));
    }

    @Test
    public void range_swapsReversedDates() {
        Range r = new Range(TODAY, TODAY.minusDays(3));
        assertEquals(TODAY.minusDays(3), r.from);
        assertEquals(4, r.days());
    }

    @Test
    public void onlyDeliveredCountsAsRevenue() {
        RevenueReport rep = new RevenueReport(Range.thisMonth(TODAY));
        rep.addOrder(3L, "10/11/2026", "200.000");
        rep.addOrder(3L, "14/11/2026", 100000L);
        rep.addOrder(4L, "12/11/2026", "50.000");   // đang giao
        rep.addOrder(6L, "12/11/2026", "70.000");   // hủy
        assertEquals(300000, rep.getRevenue());
        assertEquals(2, rep.getDeliveredOrders());
        assertEquals(150000, rep.getAverageOrder());
        assertEquals(1, rep.getCancelledOrders());
        assertEquals(1, rep.getUnfinishedOrders());
        assertEquals(50000, rep.getUnfinishedValue());
        assertEquals(4, rep.getTotalOrders());
    }

    @Test
    public void ordersOutsideRangeOrBrokenDataAreIgnored() {
        RevenueReport rep = new RevenueReport(Range.lastDays(TODAY, 7));
        rep.addOrder(3L, "01/10/2026", "100.000");  // quá xa (cả kỳ trước cũng không tới)
        rep.addOrder(3L, "", "100.000");
        rep.addOrder(3L, "abc", "100.000");
        rep.addOrder(null, "10/11/2026", "100.000");
        rep.addOrder(3L, "10/11/2026", null);        // tiền thiếu → 0 nhưng vẫn là 1 đơn
        assertEquals(0, rep.getRevenue());
        assertEquals(1, rep.getDeliveredOrders());
        assertEquals(0, rep.getAverageOrder());
    }

    @Test
    public void dailyBuckets() {
        RevenueReport rep = new RevenueReport(Range.lastDays(TODAY, 7));
        assertFalse(rep.isMonthly());
        assertEquals(7, rep.getBucketCount());
        rep.addOrder(3L, "08/11/2026", "10.000");
        rep.addOrder(3L, "08/11/2026", "5.000");
        rep.addOrder(3L, "14/11/2026", "40.000");
        assertEquals(15000, rep.getBucketRevenue(0));
        assertEquals(40000, rep.getBucketRevenue(6));
        assertEquals("8/11", rep.getBucketLabel(0));
        assertEquals("14/11", rep.getBucketLabel(6));
        assertEquals(6, rep.getBestBucket());
    }

    @Test
    public void yearIsGroupedByMonth() {
        RevenueReport rep = new RevenueReport(Range.thisYear(TODAY));
        assertTrue(rep.isMonthly());
        assertEquals(11, rep.getBucketCount()); // tới tháng 11
        rep.addOrder(3L, "05/01/2026", "100.000");
        rep.addOrder(3L, "30/11/2026", "300.000"); // ngoài kỳ (sau hôm nay) nhưng cùng tháng 11 → vẫn ngoài range
        rep.addOrder(3L, "14/11/2026", "200.000");
        assertEquals(100000, rep.getBucketRevenue(0));
        assertEquals(200000, rep.getBucketRevenue(10));
        assertEquals("T1", rep.getBucketLabel(0));
        assertEquals("Tháng 11/2026", rep.getBucketTitle(10));
        assertEquals(300000, rep.getRevenue());
    }

    @Test
    public void monthlyAcrossYearsLabelsJanuaryWithYear() {
        RevenueReport rep = new RevenueReport(new Range(LocalDate.of(2025, 11, 1), LocalDate.of(2026, 2, 28)));
        assertEquals(4, rep.getBucketCount());
        assertEquals("T11", rep.getBucketLabel(0));
        assertEquals("T1/26", rep.getBucketLabel(2));
        rep.addOrder(3L, "10/02/2026", "10.000");
        assertEquals(10000, rep.getBucketRevenue(3));
    }

    @Test
    public void deltaVersusPreviousPeriod() {
        RevenueReport rep = new RevenueReport(Range.lastDays(TODAY, 7));
        assertNull(rep.getDeltaPercent()); // chưa có kỳ trước
        rep.addOrder(3L, "03/11/2026", "100.000");  // kỳ trước (1/11–7/11)
        rep.addOrder(3L, "10/11/2026", "150.000");
        assertEquals(100000, rep.getPreviousRevenue());
        assertEquals(150000, rep.getRevenue());
        assertEquals(Integer.valueOf(50), rep.getDeltaPercent());

        RevenueReport down = new RevenueReport(Range.lastDays(TODAY, 7));
        down.addOrder(3L, "03/11/2026", "200.000");
        down.addOrder(3L, "10/11/2026", "50.000");
        assertEquals(Integer.valueOf(-75), down.getDeltaPercent());
    }

    @Test
    public void previousPeriodOrdersDoNotLeakIntoCounts() {
        RevenueReport rep = new RevenueReport(Range.lastDays(TODAY, 7));
        rep.addOrder(6L, "03/11/2026", "100.000");
        rep.addOrder(4L, "03/11/2026", "100.000");
        assertEquals(0, rep.getTotalOrders());
        assertEquals(0, rep.getUnfinishedValue());
    }

    @Test
    public void bestBucketIsMinusOneWhenNoRevenue() {
        assertEquals(-1, new RevenueReport(Range.lastDays(TODAY, 7)).getBestBucket());
    }

    @Test
    public void formatCompact() {
        assertEquals("0", MoneyUtil.formatCompact(0));
        assertEquals("500", MoneyUtil.formatCompact(500));
        assertEquals("12k", MoneyUtil.formatCompact(12000));
        assertEquals("1,5k", MoneyUtil.formatCompact(1500));
        assertEquals("1,5tr", MoneyUtil.formatCompact(1500000));
        assertEquals("2tr", MoneyUtil.formatCompact(2000000));
        assertEquals("2,3 tỷ", MoneyUtil.formatCompact(2300000000L));
        assertEquals("-12k", MoneyUtil.formatCompact(-12000));
    }
}
