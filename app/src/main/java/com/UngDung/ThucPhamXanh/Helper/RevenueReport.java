package com.UngDung.ThucPhamXanh.Helper;

import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Báo cáo doanh thu cho màn "Thống kê" của admin, tính thuần (không đụng Firebase) để unit test được.
 *
 * Cách dùng: tạo với một {@link Range}, gọi {@link #addOrder} cho từng hóa đơn rồi đọc getter.
 *
 * Quy ước (giống {@link DashboardStats}): doanh thu chỉ tính đơn ĐÃ GIAO, theo ngày đặt.
 * Báo cáo còn gom thêm: số đơn theo từng trạng thái trong kỳ, giá trị đơn chưa giao xong ("dự kiến")
 * và doanh thu của kỳ liền trước có cùng độ dài để so sánh.
 */
public final class RevenueReport {

    /** Khoảng ngày, gồm cả hai đầu. */
    public static final class Range {
        public final LocalDate from;
        public final LocalDate to;

        public Range(LocalDate from, LocalDate to) {
            if (from == null || to == null) throw new IllegalArgumentException("from/to == null");
            if (to.isBefore(from)) { LocalDate t = from; from = to; to = t; }
            this.from = from;
            this.to = to;
        }

        public static Range lastDays(LocalDate today, int days) {
            return new Range(today.minusDays(Math.max(days, 1) - 1L), today);
        }

        public static Range thisMonth(LocalDate today) {
            return new Range(today.withDayOfMonth(1), today);
        }

        public static Range thisYear(LocalDate today) {
            return new Range(today.withDayOfYear(1), today);
        }

        public int days() {
            return (int) ChronoUnit.DAYS.between(from, to) + 1;
        }

        public boolean contains(LocalDate d) {
            return d != null && !d.isBefore(from) && !d.isAfter(to);
        }

        /** Kỳ liền trước có cùng số ngày (7 ngày qua → 7 ngày trước đó). */
        public Range previous() {
            return new Range(from.minusDays(days()), from.minusDays(1));
        }
    }

    /** Kỳ dài hơn số ngày này thì gom theo tháng, ngược lại theo ngày. */
    public static final int MAX_DAILY_DAYS = 62;

    private final Range range;
    private final Range previousRange;
    private final boolean monthly;
    private final long[] bucketRevenue;
    private final String[] bucketLabels;
    private final String[] bucketTitles;

    private final int[] statusCount = new int[7]; // chỉ số = mã trạng thái 1..6
    private int deliveredOrders;
    private long revenue;
    private long previousRevenue;
    private long unfinishedValue;   // đơn đã đặt/xử lý/xác nhận/đang giao: chưa tính vào doanh thu
    private int unfinishedOrders;

    public RevenueReport(Range range) {
        if (range == null) throw new IllegalArgumentException("range == null");
        this.range = range;
        this.previousRange = range.previous();
        this.monthly = range.days() > MAX_DAILY_DAYS;

        if (monthly) {
            int n = monthIndex(range.to) + 1;
            bucketRevenue = new long[n];
            bucketLabels = new String[n];
            bucketTitles = new String[n];
            boolean multiYear = range.from.getYear() != range.to.getYear();
            for (int i = 0; i < n; i++) {
                LocalDate m = range.from.withDayOfMonth(1).plusMonths(i);
                bucketLabels[i] = "T" + m.getMonthValue();
                bucketTitles[i] = "Tháng " + m.getMonthValue() + "/" + m.getYear();
                if (multiYear && m.getMonthValue() == 1) bucketLabels[i] += "/" + (m.getYear() % 100);
            }
        } else {
            int n = range.days();
            bucketRevenue = new long[n];
            bucketLabels = new String[n];
            bucketTitles = new String[n];
            for (int i = 0; i < n; i++) {
                LocalDate d = range.from.plusDays(i);
                bucketLabels[i] = d.getDayOfMonth() + "/" + d.getMonthValue();
                bucketTitles[i] = DashboardStats.formatLongDate(d);
            }
        }
    }

    private int monthIndex(LocalDate d) {
        return (d.getYear() - range.from.getYear()) * 12 + d.getMonthValue() - range.from.getMonthValue();
    }

    /**
     * Cộng một hóa đơn.
     * @param status   trường "trangthai" (null → bỏ qua)
     * @param ngaydat  trường "ngaydat" dạng dd/MM/yyyy (sai định dạng → bỏ qua)
     * @param tongtien trường "tongtien" (String hoặc Number)
     */
    public void addOrder(Long status, String ngaydat, Object tongtien) {
        LocalDate date = DashboardStats.parseDate(ngaydat);
        if (date == null || status == null) return;
        long money = MoneyUtil.parseVnd(tongtien);

        if (previousRange.contains(date)) {
            if (status == OrderStatus.DA_GIAO) previousRevenue += money;
            return;
        }
        if (!range.contains(date)) return;

        int s = status.intValue();
        if (s >= 1 && s <= 6) statusCount[s]++;

        if (status == OrderStatus.DA_GIAO) {
            deliveredOrders++;
            revenue += money;
            int idx = monthly ? monthIndex(date) : (int) ChronoUnit.DAYS.between(range.from, date);
            if (idx >= 0 && idx < bucketRevenue.length) bucketRevenue[idx] += money;
        } else if (status != OrderStatus.DA_HUY) {
            unfinishedOrders++;
            unfinishedValue += money;
        }
    }

    public Range getRange() { return range; }
    public boolean isMonthly() { return monthly; }
    public long getRevenue() { return revenue; }
    public long getPreviousRevenue() { return previousRevenue; }
    public int getDeliveredOrders() { return deliveredOrders; }
    public int getCancelledOrders() { return statusCount[OrderStatus.DA_HUY]; }
    public long getUnfinishedValue() { return unfinishedValue; }
    public int getUnfinishedOrders() { return unfinishedOrders; }
    public int getOrderCount(int status) {
        return status >= 1 && status <= 6 ? statusCount[status] : 0;
    }
    /** Tổng số đơn (mọi trạng thái) đặt trong kỳ. */
    public int getTotalOrders() {
        int t = 0;
        for (int c : statusCount) t += c;
        return t;
    }

    /** Doanh thu trung bình mỗi đơn đã giao (0 nếu chưa có đơn). */
    public long getAverageOrder() {
        return deliveredOrders == 0 ? 0 : Math.round((double) revenue / deliveredOrders);
    }

    /**
     * Tăng/giảm so với kỳ trước, tính theo %. Null khi kỳ trước không có doanh thu
     * (không chia được cho 0 — màn hình nên ghi "chưa có dữ liệu kỳ trước").
     */
    public Integer getDeltaPercent() {
        if (previousRevenue <= 0) return null;
        return (int) Math.round((revenue - previousRevenue) * 100.0 / previousRevenue);
    }

    // ------------------------------------------------------------------ cột biểu đồ

    public int getBucketCount() { return bucketRevenue.length; }
    public long getBucketRevenue(int i) { return bucketRevenue[i]; }
    /** Nhãn ngắn dưới cột: "12/11" hoặc "T3". */
    public String getBucketLabel(int i) { return bucketLabels[i]; }
    /** Tên đầy đủ khi bấm vào cột: "Thứ Bảy, 14/11/2026" hoặc "Tháng 3/2026". */
    public String getBucketTitle(int i) { return bucketTitles[i]; }

    /** Cột có doanh thu cao nhất (-1 nếu cả kỳ chưa có doanh thu). */
    public int getBestBucket() {
        int best = -1;
        long max = 0;
        for (int i = 0; i < bucketRevenue.length; i++) {
            if (bucketRevenue[i] > max) { max = bucketRevenue[i]; best = i; }
        }
        return best;
    }
}
