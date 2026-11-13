package com.UngDung.ThucPhamXanh.Helper;

/**
 * Đọc số tiền VNĐ từ dữ liệu Firestore một cách an toàn.
 * Trường "tongtien" của hóa đơn lúc là chuỗi đã định dạng ("255.000" / "255,000"),
 * lúc là chuỗi số ("255000"), lúc là kiểu số -> chỉ giữ lại chữ số, KHÔNG phụ thuộc
 * ngôn ngữ của điện thoại, dùng long nên không tràn số khi doanh thu vượt 2,1 tỷ.
 */
public final class MoneyUtil {

    private MoneyUtil() { }

    /** Trả về số tiền (đồng); dữ liệu rỗng / không đọc được -> 0. */
    public static long parseVnd(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).longValue();
        String digits = value.toString().replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return 0; // quá dài, dữ liệu lỗi
        }
    }

    /** 2058250 → "2.058.250 đ" (dấu chấm ngăn cách nghìn, không phụ thuộc ngôn ngữ máy). */
    public static String formatVnd(long amount) {
        return formatNumber(amount) + " đ";
    }

    /** 2058250 → "2.058.250". */
    public static String formatNumber(long amount) {
        String digits = Long.toString(Math.abs(amount));
        StringBuilder sb = new StringBuilder();
        int lead = digits.length() % 3;
        if (lead == 0) lead = 3;
        sb.append(digits, 0, lead);
        for (int i = lead; i < digits.length(); i += 3) {
            sb.append('.').append(digits, i, i + 3);
        }
        return amount < 0 ? "-" + sb : sb.toString();
    }

    /**
     * Rút gọn cho trục biểu đồ / chỗ hẹp: 500 → "500", 12.000 → "12k", 1.500.000 → "1,5tr", 2.300.000.000 → "2,3 tỷ".
     */
    public static String formatCompact(long amount) {
        long abs = Math.abs(amount);
        String s;
        if (abs >= 1_000_000_000L) s = trim1(abs / 1_000_000_000.0) + " tỷ";
        else if (abs >= 1_000_000L) s = trim1(abs / 1_000_000.0) + "tr";
        else if (abs >= 1_000L) s = trim1(abs / 1_000.0) + "k";
        else s = Long.toString(abs);
        return amount < 0 ? "-" + s : s;
    }

    /** 1.5 → "1,5"; 2.0 → "2"; làm tròn 1 chữ số thập phân, dấu phẩy kiểu Việt Nam. */
    private static String trim1(double v) {
        long tenths = Math.round(v * 10);
        return tenths % 10 == 0 ? Long.toString(tenths / 10) : (tenths / 10) + "," + (tenths % 10);
    }
}
