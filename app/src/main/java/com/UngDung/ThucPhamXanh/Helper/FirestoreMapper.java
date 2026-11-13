package com.UngDung.ThucPhamXanh.Helper;

import com.UngDung.ThucPhamXanh.Models.HoaDon;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Chuyển document Firestore → model, gom một chỗ thay vì mỗi màn admin tự viết lại.
 *
 * An toàn với dữ liệu thiếu field: số thiếu / sai kiểu → 0, chuỗi thiếu → "" (trước đây
 * getLong(...) trả null rồi bị unbox vào tham số long → app crash).
 */
public final class FirestoreMapper {

    private FirestoreMapper() { }

    /** Document collection "SanPham" → Product (id = document id). */
    public static Product toProduct(DocumentSnapshot doc) {
        return new Product(
                doc.getId(),
                str(doc, "tensp"),
                num(doc, "giatien"),
                str(doc, "hinhanh"),
                str(doc, "loaisp"),
                str(doc, "mota"),
                num(doc, "soluong"),
                str(doc, "hansudung"),
                num(doc, "type"),
                str(doc, "trongluong"));
    }

    /** Document collection "HoaDon" → HoaDon (id = document id, type = "trangthai"). */
    public static HoaDon toHoaDon(DocumentSnapshot doc) {
        Object tongtien = doc.get("tongtien"); // có đơn lưu chuỗi, có đơn lưu số
        return new HoaDon(
                doc.getId(),
                str(doc, "UID"),
                str(doc, "ghichu"),
                str(doc, "diachi"),
                str(doc, "hoten"),
                str(doc, "ngaydat"),
                str(doc, "phuongthuc"),
                str(doc, "sdt"),
                tongtien == null ? "" : tongtien.toString(),
                num(doc, "trangthai"));
    }

    /** Chuỗi của field; thiếu → "", kiểu khác (vd. số) → toString(). */
    public static String str(DocumentSnapshot doc, String field) {
        Object v = doc.get(field);
        return v == null ? "" : v.toString();
    }

    /** Số nguyên của field; thiếu / không phải số → 0. Chấp nhận cả chuỗi số ("12"). */
    public static long num(DocumentSnapshot doc, String field) {
        Long v = numOrNull(doc, field);
        return v == null ? 0 : v;
    }

    /** Như {@link #num} nhưng thiếu / không đọc được → null (để phân biệt với giá trị 0 thật). */
    public static Long numOrNull(DocumentSnapshot doc, String field) {
        Object v = doc.get(field);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try {
                return Long.parseLong(((String) v).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
