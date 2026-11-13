package com.UngDung.ThucPhamXanh.Helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.TextUtils;
import android.text.TextPaint;

import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hóa đơn PDF khổ A4 cho một đơn hàng. Ghi vào cache (cache/invoices/) — không cần quyền bộ nhớ —
 * rồi màn hình chia sẻ qua FileProvider (Zalo, Gmail, Drive, máy in...). Tự sang trang khi nhiều dòng.
 */
public final class InvoicePdf {

    private static final int W = 595, H = 842, M = 40;   // A4 tính bằng point
    private static final String STORE = "THỰC PHẨM XANH";

    private InvoicePdf() { }

    public static File create(Context ctx, AdminOrder o, List<OrderMath.Line> lines,
                              long subtotal, long discount) throws IOException {
        PdfDocument pdf = new PdfDocument();
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.BLACK);
        Typeface regular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        Typeface bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        int green = Color.rgb(0x2E, 0x7D, 0x32);

        int pageNo = 1;
        PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(W, H, pageNo).create());
        Canvas c = page.getCanvas();

        // ----- Đầu trang
        p.setTypeface(bold);
        p.setTextSize(20);
        p.setColor(green);
        c.drawText(STORE, M, 60, p);
        p.setColor(Color.BLACK);
        p.setTextSize(16);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("HÓA ĐƠN BÁN HÀNG", W - M, 60, p);
        p.setTypeface(regular);
        p.setTextSize(10);
        c.drawText("Mã đơn: " + o.displayCode(), W - M, 78, p);
        c.drawText("Ngày đặt: " + o.date, W - M, 92, p);
        c.drawText("Trạng thái: " + OrderStatus.label(o.status), W - M, 106, p);
        p.setTextAlign(Paint.Align.LEFT);
        p.setColor(green);
        c.drawRect(M, 118, W - M, 120, p);
        p.setColor(Color.BLACK);

        // ----- Khách hàng
        int y = 142;
        p.setTextSize(11);
        y = row(c, p, "Khách hàng:", o.name, y);
        y = row(c, p, "Điện thoại:", o.phone, y);
        y = row(c, p, "Địa chỉ:", o.address, y);
        y = row(c, p, "Thanh toán:", o.payment, y);
        if (!o.note.trim().isEmpty()) y = row(c, p, "Ghi chú:", o.note, y);
        y += 10;

        // ----- Bảng sản phẩm
        int colName = M + 26, colQty = 330, colPrice = 440, colTotal = W - M;
        y = tableHeader(c, p, bold, regular, y, colName, colQty, colPrice, colTotal);
        int i = 0;
        for (OrderMath.Line l : lines) {
            if (y > H - 140) { // hết chỗ → trang mới
                pdf.finishPage(page);
                page = pdf.startPage(new PdfDocument.PageInfo.Builder(W, H, ++pageNo).create());
                c = page.getCanvas();
                y = tableHeader(c, p, bold, regular, M + 10, colName, colQty, colPrice, colTotal);
            }
            i++;
            p.setTextSize(10);
            c.drawText(String.valueOf(i), M + 4, y, p);
            String name = TextUtils.ellipsize(l.name, p, colQty - colName - 30, TextUtils.TruncateAt.END).toString();
            c.drawText(name + (l.priceIsCurrent ? " *" : ""), colName, y, p);
            p.setTextAlign(Paint.Align.RIGHT);
            c.drawText(String.valueOf(l.quantity), colQty + 20, y, p);
            c.drawText(MoneyUtil.formatNumber(l.unitPrice), colPrice + 60, y, p);
            c.drawText(MoneyUtil.formatNumber(l.lineTotal()), colTotal, y, p);
            p.setTextAlign(Paint.Align.LEFT);
            p.setColor(Color.rgb(0xE0, 0xDC, 0xD3));
            c.drawLine(M, y + 7, W - M, y + 7, p);
            p.setColor(Color.BLACK);
            y += 22;
        }

        // ----- Tổng (không đủ chỗ thì sang trang mới)
        if (y > H - 150) {
            pdf.finishPage(page);
            page = pdf.startPage(new PdfDocument.PageInfo.Builder(W, H, ++pageNo).create());
            c = page.getCanvas();
            y = M + 20;
        }
        y += 10;
        y = total(c, p, "Tạm tính", MoneyUtil.formatVnd(subtotal), y, false);
        y = total(c, p, "Phí vận chuyển", MoneyUtil.formatVnd(o.shippingFee), y, false);
        if (discount > 0) y = total(c, p, "Khuyến mãi", "−" + MoneyUtil.formatVnd(discount), y, false);
        p.setTypeface(bold);
        y = total(c, p, "TỔNG CỘNG", MoneyUtil.formatVnd(o.total), y + 4, true);
        p.setTypeface(regular);

        // ----- Chân trang
        p.setTextSize(9);
        p.setColor(Color.GRAY);
        boolean anyCurrent = false;
        for (OrderMath.Line l : lines) anyCurrent |= l.priceIsCurrent;
        if (anyCurrent) c.drawText("* Đơn cũ không lưu giá lúc đặt, dòng này dùng giá hiện tại.", M, H - 52, p);
        String printed = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT).format(new Date());
        c.drawText("In lúc " + printed + " · Cảm ơn quý khách!", M, H - 38, p);
        pdf.finishPage(page);

        File dir = new File(ctx.getCacheDir(), "invoices");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Không tạo được thư mục hóa đơn");
        String safe = o.displayCode().replaceAll("[^A-Za-z0-9_-]", "");
        File file = new File(dir, "HoaDon-" + (safe.isEmpty() ? "don" : safe) + ".pdf");
        try (FileOutputStream out = new FileOutputStream(file)) {
            pdf.writeTo(out);
        } finally {
            pdf.close();
        }
        return file;
    }

    private static int row(Canvas c, TextPaint p, String label, String value, int y) {
        p.setColor(Color.DKGRAY);
        c.drawText(label, M, y, p);
        p.setColor(Color.BLACK);
        String v = TextUtils.ellipsize(value == null ? "" : value, p, W - 2 * M - 80, TextUtils.TruncateAt.END).toString();
        c.drawText(v, M + 80, y, p);
        return y + 17;
    }

    private static int tableHeader(Canvas c, TextPaint p, Typeface bold, Typeface regular, int y,
                                   int colName, int colQty, int colPrice, int colTotal) {
        p.setColor(Color.rgb(0xE8, 0xF5, 0xE9));
        c.drawRect(M, y - 14, W - M, y + 8, p);
        p.setColor(Color.BLACK);
        p.setTypeface(bold);
        p.setTextSize(10);
        c.drawText("#", M + 4, y, p);
        c.drawText("Sản phẩm", colName, y, p);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("SL", colQty + 20, y, p);
        c.drawText("Đơn giá", colPrice + 60, y, p);
        c.drawText("Thành tiền", colTotal, y, p);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTypeface(regular);
        return y + 26;
    }

    private static int total(Canvas c, TextPaint p, String label, String value, int y, boolean big) {
        p.setTextSize(big ? 13 : 11);
        c.drawText(label, 330, y, p);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText(value, W - M, y, p);
        p.setTextAlign(Paint.Align.LEFT);
        return y + (big ? 22 : 18);
    }
}
