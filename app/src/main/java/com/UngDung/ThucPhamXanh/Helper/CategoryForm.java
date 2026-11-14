package com.UngDung.ThucPhamXanh.Helper;

import java.util.List;

/**
 * Kiểm tra tên danh mục (LoaiProduct.tenloai) — thuần Java, test được.
 * Trùng tên được tính không phân biệt hoa/thường và dấu ("Thịt Heo" = "thit heo"), vì
 * SanPham.loaisp lưu theo TÊN nên hai danh mục gần giống nhau dễ làm sản phẩm bị chia đôi.
 */
public final class CategoryForm {

    public static final int MAX_NAME = 40;

    private CategoryForm() { }

    /** Bỏ khoảng trắng đầu/cuối và gộp nhiều khoảng trắng liền nhau thành một. */
    public static String cleanName(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ");
    }

    /**
     * @param existing   tên các danh mục hiện có
     * @param editingOld tên cũ của danh mục đang sửa (null nếu thêm mới) — không tính là trùng với chính nó
     * @return thông báo lỗi, hoặc null nếu hợp lệ
     */
    public static String validateName(String name, List<String> existing, String editingOld) {
        String clean = cleanName(name);
        if (clean.isEmpty()) return "Vui lòng nhập tên danh mục";
        if (clean.length() > MAX_NAME) return "Tên danh mục tối đa " + MAX_NAME + " ký tự";
        String key = TextNormalizer.normalize(clean);
        String oldKey = editingOld == null ? null : TextNormalizer.normalize(cleanName(editingOld));
        if (existing != null) {
            for (String e : existing) {
                if (e == null) continue;
                String k = TextNormalizer.normalize(cleanName(e));
                if (k.equals(key) && !(oldKey != null && k.equals(oldKey))) {
                    return "Danh mục \"" + cleanName(e) + "\" đã tồn tại";
                }
            }
        }
        return null;
    }

    /** Đếm số tên trùng nhau (theo cách so ở trên) — để cảnh báo dữ liệu cũ bị lặp. */
    public static int countDuplicates(List<String> names) {
        if (names == null) return 0;
        java.util.Set<String> seen = new java.util.HashSet<>();
        int dup = 0;
        for (String n : names) {
            if (n == null) continue;
            if (!seen.add(TextNormalizer.normalize(cleanName(n)))) dup++;
        }
        return dup;
    }
}
