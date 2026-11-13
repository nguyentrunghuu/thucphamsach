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
}
