package com.UngDung.ThucPhamXanh.Helper;

import java.util.List;

/**
 * Kiểm tra form shipper (collection "Shipper": username, password, name, phone, createdAt) — thuần Java.
 * Khi SỬA, để trống mật khẩu nghĩa là giữ mật khẩu cũ (không còn đổ mật khẩu cũ ra ô nhập).
 */
public final class ShipperForm {

    public static final int MIN_PASSWORD = 6;

    /** Lỗi theo từng ô; null = hợp lệ. */
    public static final class Errors {
        public String username, password, name, phone;

        public boolean any() {
            return username != null || password != null || name != null || phone != null;
        }
    }

    private ShipperForm() { }

    public static String cleanUsername(String s) {
        return s == null ? "" : s.trim();
    }

    /** Số điện thoại chỉ giữ chữ số và dấu + đầu; "0984 546.164" → "0984546164". */
    public static String cleanPhone(String s) {
        if (s == null) return "";
        String t = s.trim();
        boolean plus = t.startsWith("+");
        String digits = t.replaceAll("[^0-9]", "");
        return plus ? "+" + digits : digits;
    }

    /** Số di động / cố định VN: 0xxxxxxxxx (10–11 số) hoặc +84xxxxxxxxx. */
    public static boolean isValidPhone(String s) {
        String p = cleanPhone(s);
        if (p.startsWith("+84")) return p.length() >= 11 && p.length() <= 12;
        return p.startsWith("0") && p.length() >= 10 && p.length() <= 11;
    }

    /**
     * @param existingUsernames username đã có (không phân biệt hoa/thường)
     * @param editingUsername   username đang sửa (null = thêm mới)
     */
    public static Errors validate(String username, String password, String name, String phone,
                                  List<String> existingUsernames, String editingUsername) {
        Errors e = new Errors();
        boolean editing = editingUsername != null;
        String u = cleanUsername(username);

        if (!editing) {
            if (u.isEmpty()) {
                e.username = "Vui lòng nhập tên đăng nhập";
            } else if (!u.matches("[A-Za-z0-9._-]{3,30}")) {
                e.username = "3–30 ký tự, chỉ gồm chữ không dấu, số, . _ -";
            } else if (existingUsernames != null) {
                for (String x : existingUsernames) {
                    if (x != null && x.trim().equalsIgnoreCase(u)) {
                        e.username = "Tên đăng nhập đã tồn tại";
                        break;
                    }
                }
            }
        }

        String pw = password == null ? "" : password;
        if (!editing && pw.isEmpty()) {
            e.password = "Vui lòng nhập mật khẩu";
        } else if (!pw.isEmpty() && pw.length() < MIN_PASSWORD) {
            e.password = "Mật khẩu tối thiểu " + MIN_PASSWORD + " ký tự";
        }

        if (name == null || name.trim().isEmpty()) e.name = "Vui lòng nhập họ tên";

        if (phone == null || phone.trim().isEmpty()) {
            e.phone = "Vui lòng nhập số điện thoại";
        } else if (!isValidPhone(phone)) {
            e.phone = "Số điện thoại không hợp lệ";
        }
        return e;
    }
}
