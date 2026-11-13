package com.UngDung.ThucPhamXanh.View.Admin;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.MoneyUtil;
import com.UngDung.ThucPhamXanh.Helper.RevenueReport;
import com.UngDung.ThucPhamXanh.Helper.RevenueReport.Range;
import com.UngDung.ThucPhamXanh.Models.OrderStatus;
import com.UngDung.ThucPhamXanh.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Thống kê doanh thu cho admin.
 *
 * Chọn kỳ bằng chip (7 ngày / 30 ngày / tháng này / năm nay / tự chọn) → thẻ doanh thu kèm so sánh
 * với kỳ trước → biểu đồ cột (theo ngày, hoặc theo tháng khi kỳ dài) → số đơn theo trạng thái
 * → giá trị đơn chưa giao xong → xuất Excel các đơn đã giao trong kỳ.
 *
 * Mọi phép tính nằm ở {@link RevenueReport} (có unit test). Màn hình chỉ đọc 1 listener HoaDon
 * (chỉ khi đang hiện — xem BaseAdminActivity) và vẽ lại khi dữ liệu hoặc kỳ thay đổi.
 */
public class AdminChartBillActivity extends BaseAdminActivity {

    private static final String TAG = "AdminChartBill";

    /** Một hóa đơn rút gọn, đủ cho thống kê và xuất Excel. */
    private static final class OrderRow {
        String id, uid, hoten, diachi, sdt, ngaydat;
        Object tongtien;
        Long status;
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<OrderRow> rows = new ArrayList<>();
    private boolean loaded;

    private Range range;
    private RevenueReport report;
    private int lastPresetChip = R.id.chip_period_month;

    private ChipGroup chipGroup;
    private BarChart barChart;
    private TextView tvRange, tvRevenue, tvDelta, tvChartTitle, tvChartHint, tvChartEmpty, tvUnfinished;
    private View miniDelivered, miniAverage, miniCancelled;
    private final List<StatusRow> statusRows = new ArrayList<>();

    /** Dòng trạng thái: các status gộp (vd. 1+2 = "Chờ xác nhận"), nhãn và màu chấm. */
    private static final class StatusRow {
        final int[] statuses;
        final String label;
        final int color;
        View view;

        StatusRow(String label, int color, int... statuses) {
            this.label = label;
            this.color = color;
            this.statuses = statuses;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chart_bill);

        setupAdminToolbar("Thống kê doanh thu", null);
        applySystemBarInsets(null, findViewById(R.id.chart_scroll));

        chipGroup = findViewById(R.id.chip_group_period);
        barChart = findViewById(R.id.barchart);
        tvRange = findViewById(R.id.tv_range);
        tvRevenue = findViewById(R.id.tv_revenue);
        tvDelta = findViewById(R.id.tv_delta);
        tvChartTitle = findViewById(R.id.tv_chart_title);
        tvChartHint = findViewById(R.id.tv_chart_hint);
        tvChartEmpty = findViewById(R.id.tv_chart_empty);
        tvUnfinished = findViewById(R.id.tv_unfinished);
        miniDelivered = findViewById(R.id.mini_delivered);
        miniAverage = findViewById(R.id.mini_average);
        miniCancelled = findViewById(R.id.mini_cancelled);

        ((TextView) miniDelivered.findViewById(R.id.mini_label)).setText("Đơn đã giao");
        ((TextView) miniAverage.findViewById(R.id.mini_label)).setText("TB mỗi đơn");
        ((TextView) miniCancelled.findViewById(R.id.mini_label)).setText("Đơn hủy");

        range = rangeFor(R.id.chip_period_month);
        setupChart();
        setupStatusRows();
        setupPeriodChips();
        findViewById(R.id.btn_export).setOnClickListener(v -> exportExcel());
        render();
    }

    @Override
    protected void onStartListening() {
        listen(db.collection("HoaDon").addSnapshotListener((snap, e) -> {
            if (e != null) { Log.e(TAG, "Lỗi đọc HoaDon", e); return; }
            if (snap != null) onOrders(snap);
        }));
    }

    private void onOrders(QuerySnapshot snap) {
        rows.clear();
        for (QueryDocumentSnapshot doc : snap) {
            OrderRow r = new OrderRow();
            r.id = doc.getId();
            r.uid = FirestoreMapper.str(doc, "UID");
            r.hoten = FirestoreMapper.str(doc, "hoten");
            r.diachi = FirestoreMapper.str(doc, "diachi");
            r.sdt = FirestoreMapper.str(doc, "sdt");
            r.ngaydat = FirestoreMapper.str(doc, "ngaydat");
            r.tongtien = doc.get("tongtien");
            r.status = FirestoreMapper.numOrNull(doc, "trangthai");
            rows.add(r);
        }
        loaded = true;
        render();
    }

    // ------------------------------------------------------------------ chọn kỳ

    private void setupPeriodChips() {
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) return;
            if (checkedId == R.id.chip_period_custom) {
                pickCustomRange();
                return;
            }
            lastPresetChip = checkedId;
            range = rangeFor(checkedId);
            ((com.google.android.material.chip.Chip) findViewById(R.id.chip_period_custom)).setText("Chọn ngày…");
            render();
        });
    }

    private static Range rangeFor(@IdRes int chipId) {
        LocalDate today = LocalDate.now();
        if (chipId == R.id.chip_period_7) return Range.lastDays(today, 7);
        if (chipId == R.id.chip_period_30) return Range.lastDays(today, 30);
        if (chipId == R.id.chip_period_year) return Range.thisYear(today);
        return Range.thisMonth(today);
    }

    private void pickCustomRange() {
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = MaterialDatePicker.Builder
                .dateRangePicker()
                .setTitleText("Chọn khoảng ngày")
                .build();
        picker.addOnPositiveButtonClickListener(sel -> {
            if (sel == null || sel.first == null || sel.second == null) return;
            LocalDate from = Instant.ofEpochMilli(sel.first).atZone(ZoneOffset.UTC).toLocalDate();
            LocalDate to = Instant.ofEpochMilli(sel.second).atZone(ZoneOffset.UTC).toLocalDate();
            range = new Range(from, to);
            ((com.google.android.material.chip.Chip) findViewById(R.id.chip_period_custom))
                    .setText(shortDate(range.from) + " – " + shortDate(range.to));
            render();
        });
        // Hủy / đóng hộp thoại: quay về chip đang dùng trước đó
        picker.addOnNegativeButtonClickListener(v -> chipGroup.check(lastPresetChip));
        picker.addOnCancelListener(d -> chipGroup.check(lastPresetChip));
        picker.show(getSupportFragmentManager(), "range");
    }

    // ------------------------------------------------------------------ vẽ màn hình

    private void render() {
        report = new RevenueReport(range);
        for (OrderRow r : rows) report.addOrder(r.status, r.ngaydat, r.tongtien);

        tvRange.setText(longDate(range.from) + " – " + longDate(range.to) + " · " + range.days() + " ngày");

        tvRevenue.setText(loaded ? MoneyUtil.formatVnd(report.getRevenue()) : "—");
        bindDelta();
        setMini(miniDelivered, String.valueOf(report.getDeliveredOrders()));
        setMini(miniAverage, report.getDeliveredOrders() == 0 ? "—" : MoneyUtil.formatCompact(report.getAverageOrder()) + " đ");
        setMini(miniCancelled, String.valueOf(report.getCancelledOrders()));

        renderChart();
        renderStatusRows();

        if (loaded && report.getUnfinishedOrders() > 0) {
            tvUnfinished.setVisibility(View.VISIBLE);
            tvUnfinished.setText(report.getUnfinishedOrders() + " đơn trị giá "
                    + MoneyUtil.formatVnd(report.getUnfinishedValue())
                    + " chưa giao xong nên chưa tính vào doanh thu.");
        } else {
            tvUnfinished.setVisibility(View.GONE);
        }
    }

    private void bindDelta() {
        if (!loaded) { tvDelta.setText(""); return; }
        Integer delta = report.getDeltaPercent();
        if (delta == null) {
            tvDelta.setText("Kỳ trước chưa có doanh thu để so sánh");
        } else if (delta == 0) {
            tvDelta.setText("= Bằng kỳ trước (" + MoneyUtil.formatVnd(report.getPreviousRevenue()) + ")");
        } else {
            tvDelta.setText((delta > 0 ? "▲ Tăng " : "▼ Giảm ") + Math.abs(delta) + "% so với kỳ trước ("
                    + MoneyUtil.formatVnd(report.getPreviousRevenue()) + ")");
        }
    }

    private static void setMini(View mini, String value) {
        ((TextView) mini.findViewById(R.id.mini_value)).setText(value);
    }

    // ------------------------------------------------------------------ biểu đồ

    private void setupChart() {
        int text = ContextCompat.getColor(this, R.color.admin_text_muted);
        int grid = ContextCompat.getColor(this, R.color.admin_outline);

        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setScaleEnabled(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(false);
        barChart.setNoDataText("");
        barChart.setExtraBottomOffset(4f);

        XAxis x = barChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setAxisLineColor(grid);
        x.setTextColor(text);
        x.setGranularity(1f);
        x.setGranularityEnabled(true);

        YAxis left = barChart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setTextColor(text);
        left.setGridColor(grid);
        left.setDrawAxisLine(false);
        left.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                return MoneyUtil.formatCompact((long) value);
            }
        });
        barChart.getAxisRight().setEnabled(false);

        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                showBucket((int) e.getX());
            }

            @Override
            public void onNothingSelected() {
                showDefaultHint();
            }
        });
    }

    private void renderChart() {
        tvChartTitle.setText(report.isMonthly() ? "Doanh thu theo tháng" : "Doanh thu theo ngày");

        boolean hasRevenue = loaded && report.getBestBucket() >= 0;
        tvChartEmpty.setVisibility(loaded && !hasRevenue ? View.VISIBLE : View.GONE);
        barChart.setVisibility(hasRevenue ? View.VISIBLE : View.INVISIBLE);
        if (!hasRevenue) {
            barChart.clear();
            tvChartHint.setText(loaded ? "" : "Đang tải…");
            return;
        }

        final int n = report.getBucketCount();
        List<BarEntry> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) entries.add(new BarEntry(i, report.getBucketRevenue(i)));

        BarDataSet set = new BarDataSet(entries, "Doanh thu");
        set.setColor(ContextCompat.getColor(this, R.color.admin_primary));
        set.setHighLightColor(ContextCompat.getColor(this, R.color.admin_secondary));
        set.setHighLightAlpha(255);
        set.setDrawValues(false);
        BarData data = new BarData(set);
        data.setBarWidth(n <= 12 ? 0.6f : 0.75f);

        XAxis x = barChart.getXAxis();
        x.setLabelCount(Math.min(n, report.isMonthly() ? 12 : 6), false);
        x.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                int i = Math.round(value);
                return i >= 0 && i < n ? report.getBucketLabel(i) : "";
            }
        });

        barChart.setData(data);
        barChart.fitScreen();
        barChart.highlightValue(report.getBestBucket(), 0, false);
        barChart.animateY(400);
        barChart.invalidate();
        showBucket(report.getBestBucket());
    }

    private void showBucket(int i) {
        if (i < 0 || i >= report.getBucketCount()) { showDefaultHint(); return; }
        boolean best = i == report.getBestBucket();
        tvChartHint.setText(report.getBucketTitle(i) + ": " + MoneyUtil.formatVnd(report.getBucketRevenue(i))
                + (best ? " (cao nhất)" : ""));
    }

    private void showDefaultHint() {
        tvChartHint.setText("Chạm vào cột để xem chi tiết");
    }

    // ------------------------------------------------------------------ đơn theo trạng thái

    private void setupStatusRows() {
        statusRows.add(new StatusRow("Chờ xác nhận", OrderStatus.textColor(OrderStatus.DA_DAT),
                OrderStatus.DA_DAT, OrderStatus.DANG_XU_LY));
        statusRows.add(new StatusRow("Đã xác nhận", OrderStatus.textColor(OrderStatus.DA_XAC_NHAN), OrderStatus.DA_XAC_NHAN));
        statusRows.add(new StatusRow("Đang giao", OrderStatus.textColor(OrderStatus.DANG_GIAO), OrderStatus.DANG_GIAO));
        statusRows.add(new StatusRow("Đã giao", OrderStatus.textColor(OrderStatus.DA_GIAO), OrderStatus.DA_GIAO));
        statusRows.add(new StatusRow("Đã hủy", OrderStatus.textColor(OrderStatus.DA_HUY), OrderStatus.DA_HUY));

        android.widget.LinearLayout container = findViewById(R.id.status_rows);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (StatusRow s : statusRows) {
            s.view = inflater.inflate(R.layout.item_admin_status_row, container, false);
            View dot = s.view.findViewById(R.id.row_dot);
            dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(s.color));
            ((TextView) s.view.findViewById(R.id.row_label)).setText(s.label);
            ((ProgressBar) s.view.findViewById(R.id.row_bar))
                    .setProgressTintList(android.content.res.ColorStateList.valueOf(s.color));
            ((ProgressBar) s.view.findViewById(R.id.row_bar))
                    .setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.admin_outline)));
            container.addView(s.view);
        }
    }

    private void renderStatusRows() {
        int total = report.getTotalOrders();
        ((TextView) findViewById(R.id.tv_status_title)).setText(
                "Đơn theo trạng thái" + (total > 0 ? " · " + total + " đơn" : ""));
        for (StatusRow s : statusRows) {
            int count = 0;
            for (int st : s.statuses) count += report.getOrderCount(st);
            int percent = total == 0 ? 0 : Math.round(count * 100f / total);
            ((TextView) s.view.findViewById(R.id.row_count)).setText(String.valueOf(count));
            ((TextView) s.view.findViewById(R.id.row_percent)).setText(percent + "%");
            ((ProgressBar) s.view.findViewById(R.id.row_bar)).setProgress(percent);
        }
    }

    // ------------------------------------------------------------------ xuất Excel

    private void exportExcel() {
        if (!loaded) return;
        List<OrderRow> delivered = new ArrayList<>();
        for (OrderRow r : rows) {
            if (r.status == null || r.status != OrderStatus.DA_GIAO) continue;
            if (range.contains(com.UngDung.ThucPhamXanh.Helper.DashboardStats.parseDate(r.ngaydat))) delivered.add(r);
        }
        if (delivered.isEmpty()) {
            Toast.makeText(this, "Không có đơn đã giao trong kỳ này", Toast.LENGTH_SHORT).show();
            return;
        }

        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet("Doanh thu");
        String[] heads = {"STT", "Mã đơn", "Ngày đặt", "Họ tên", "SĐT", "Địa chỉ", "Tổng tiền (đ)"};
        HSSFRow head = sheet.createRow(0);
        for (int c = 0; c < heads.length; c++) head.createCell(c).setCellValue(heads[c]);

        long sum = 0;
        int rowIndex = 1;
        for (OrderRow r : delivered) {
            long money = MoneyUtil.parseVnd(r.tongtien);
            sum += money;
            HSSFRow row = sheet.createRow(rowIndex);
            row.createCell(0).setCellValue(rowIndex);
            row.createCell(1).setCellValue(r.id);
            row.createCell(2).setCellValue(r.ngaydat);
            row.createCell(3).setCellValue(r.hoten);
            row.createCell(4).setCellValue(r.sdt);
            row.createCell(5).setCellValue(r.diachi);
            row.createCell(6).setCellValue(money); // số thật để Excel cộng được
            rowIndex++;
        }
        HSSFRow total = sheet.createRow(rowIndex + 1);
        total.createCell(5).setCellValue("TỔNG DOANH THU");
        total.createCell(6).setCellValue(sum);

        
        String fileName = "DoanhThu-" + compact(range.from) + "-"
                + compact(range.to) + "-"
                + new SimpleDateFormat("HHmmss", Locale.US).format(new Date()) + ".xls";
        saveAndShareWorkbook(wb, fileName);
    }

    /**
     * Lưu file Excel rồi mở hộp "Chia sẻ".
     * - Android 10+: lưu vào thư mục Download qua MediaStore (không cần quyền bộ nhớ).
     * - Android 9: lưu vào thư mục riêng của app (không cần quyền), chia sẻ qua FileProvider.
     */
    private void saveAndShareWorkbook(HSSFWorkbook workbook, String fileName) {
        final String mime = "application/vnd.ms-excel";
        Uri uri;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, mime);
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new java.io.IOException("Không tạo được file trong Download");
                try (java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
                    workbook.write(out);
                }
            } else {
                File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                File file = new File(dir, fileName);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    workbook.write(out);
                }
                uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            }
        } catch (Exception e) {
            Log.e(TAG, "Xuất Excel thất bại", e);
            Toast.makeText(this, "Xuất file thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? "Đã lưu " + fileName + " vào thư mục Download" : "Đã xuất " + fileName, Toast.LENGTH_LONG).show();

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(mime);
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Gửi file doanh thu"));
    }

    // ------------------------------------------------------------------ định dạng ngày

    private static String longDate(LocalDate d) {
        return String.format(Locale.ROOT, "%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    private static String compact(LocalDate d) {
        return String.format(Locale.ROOT, "%02d%02d%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    private static String shortDate(LocalDate d) {
        return String.format(Locale.ROOT, "%02d/%02d", d.getDayOfMonth(), d.getMonthValue());
    }
}
