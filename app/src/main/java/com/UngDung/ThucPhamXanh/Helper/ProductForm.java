package com.UngDung.ThucPhamXanh.Helper;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kiểm tra / chuẩn hóa form thêm-sửa sản phẩm trang admin (thuần Java → unit test được).
 *
 * Field Firestore "SanPham": tensp, giatien (số), loaisp, type (số, nhóm hiển thị trên trang chủ),
 * soluong (số), trongluong, hansudung ("dd/MM/yyyy"), mota, xuatxu (tùy chọn, mới).
 */
public final class ProductForm {

    private ProductForm() { }

    // Khóa lỗi (trùng tên field Firestore)
    public static final String F_IMAGE = "hinhanh";
    public static final String F_NAME = "tensp";
    public static final String F_PRICE = "giatien";
    public static final String F_CATEGORY = "loaisp";
    public static final String F_QUANTITY = "soluong";
    public static final String F_WEIGHT = "trongluong";
    public static final String F_EXPIRY = "hansudung";
    public static final String F_DESCRIPTION = "mota";
    public static final String F_ORIGIN = "xuatxu";
    public static final String F_TYPE = "type";

    /** Giá tối đa chấp nhận (chặn gõ nhầm thừa số 0). */
    public static final long MAX_PRICE = 100_000_000L;
    public static final long MAX_QUANTITY = 1_000_000L;

    /**
     * Nhóm hiển thị (field "type") — HomeFragment chia sản phẩm theo các giá trị này.
     * Thứ tự = thứ tự trong dropdown.
     */
    public static final long[] TYPE_VALUES = {1, 2, 3, 4, 5, 6, 7, 8};
    private static final String[] TYPE_LABELS = {
            "Thường", "Nổi bật", "Đồ uống", "Hàn Quốc", "Mì cay", "Yêu thích", "Lẩu", "Gợi ý"};

    public static String typeLabel(long type) {
        for (int i = 0; i < TYPE_VALUES.length; i++) {
            if (TYPE_VALUES[i] == type) return TYPE_LABELS[i];
        }
        return "Nhóm " + type;
    }

    /** Ngược lại của {@link #typeLabel}; không nhận ra → null. */
    public static Long typeOf(String label) {
        if (label == null) return null;
        String l = label.trim();
        for (int i = 0; i < TYPE_VALUES.length; i++) {
            if (TYPE_LABELS[i].equals(l)) return TYPE_VALUES[i];
        }
        if (l.startsWith("Nhóm ")) {
            try {
                return Long.parseLong(l.substring(5).trim());
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    /**
     * Số nguyên không âm từ chuỗi người dùng gõ: bỏ dấu chấm/phẩy/khoảng trắng phân cách nghìn
     * ("35.000", "35,000", "35 000" → 35000). Rỗng / có ký tự khác / quá lớn → null.
     */
    public static Long parseWholeNumber(String s) {
        if (s == null) return null;
        String digits = s.trim().replace(".", "").replace(",", "").replace(" ", "").replace(" ", "");
        if (digits.isEmpty() || digits.length() > 15) return null;
        for (int i = 0; i < digits.length(); i++) {
            if (!Character.isDigit(digits.charAt(i))) return null;
        }
        return Long.parseLong(digits);
    }

    /** Dữ liệu form dạng chuỗi như người dùng nhập. */
    public static final class Input {
        public String image, name, price, category, quantity, weight, expiry, description, origin;
    }

    /**
     * Lỗi theo field (giữ thứ tự từ trên xuống để focus vào lỗi đầu tiên). Rỗng = hợp lệ.
     * @param isNew  thêm mới thì hạn sử dụng phải từ hôm nay trở đi; khi sửa cho phép giữ ngày cũ
     *               (vd. chỉ sửa giá một sản phẩm đã hết hạn) — màn hình tự cảnh báo.
     */
    public static Map<String, String> validate(Input in, LocalDate today, boolean isNew) {
        Map<String, String> e = new LinkedHashMap<>();
        if (blank(in.image)) e.put(F_IMAGE, "Chọn ảnh sản phẩm");

        if (blank(in.name)) e.put(F_NAME, "Nhập tên sản phẩm");
        else if (in.name.trim().length() > 100) e.put(F_NAME, "Tên tối đa 100 ký tự");

        Long price = parseWholeNumber(in.price);
        if (blank(in.price)) e.put(F_PRICE, "Nhập giá bán");
        else if (price == null) e.put(F_PRICE, "Giá chỉ gồm chữ số");
        else if (price <= 0) e.put(F_PRICE, "Giá phải lớn hơn 0");
        else if (price > MAX_PRICE) e.put(F_PRICE, "Giá quá lớn, kiểm tra lại");

        if (blank(in.category)) e.put(F_CATEGORY, "Chọn danh mục");

        Long qty = parseWholeNumber(in.quantity);
        if (blank(in.quantity)) e.put(F_QUANTITY, "Nhập số lượng tồn kho");
        else if (qty == null) e.put(F_QUANTITY, "Số lượng là số nguyên ≥ 0");
        else if (qty > MAX_QUANTITY) e.put(F_QUANTITY, "Số lượng quá lớn, kiểm tra lại");

        if (blank(in.weight)) e.put(F_WEIGHT, "Nhập trọng lượng / quy cách");

        if (blank(in.expiry)) {
            e.put(F_EXPIRY, "Chọn hạn sử dụng");
        } else {
            LocalDate d = DashboardStats.parseDate(in.expiry);
            if (d == null) e.put(F_EXPIRY, "Ngày không hợp lệ (dd/MM/yyyy)");
            else if (isNew && today != null && d.isBefore(today)) e.put(F_EXPIRY, "Hạn sử dụng đã qua");
        }

        if (blank(in.description)) e.put(F_DESCRIPTION, "Nhập mô tả sản phẩm");
        return e;
    }

    /** Dòng gợi ý dưới ô hạn sử dụng: "Còn 3 ngày", "Hết hạn hôm nay", "Đã hết hạn 2 ngày"; không rõ → "". */
    public static String expiryHint(String hansudung, LocalDate today) {
        Long days = DashboardStats.daysUntilExpiry(hansudung, today);
        if (days == null) return "";
        if (days < 0) return "Đã hết hạn " + (-days) + " ngày";
        if (days == 0) return "Hết hạn hôm nay";
        return "Còn " + days + " ngày";
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
