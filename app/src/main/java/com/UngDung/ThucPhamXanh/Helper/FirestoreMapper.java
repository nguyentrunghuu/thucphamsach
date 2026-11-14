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

    /** Document collection "HoaDon" → AdminOrder (đủ field cho màn admin). */
    public static AdminOrder toAdminOrder(DocumentSnapshot doc) {
        AdminOrder o = new AdminOrder();
        o.id = doc.getId();
        o.uid = str(doc, "UID");
        o.code = str(doc, "madonhang");
        o.name = str(doc, "hoten");
        o.phone = str(doc, "sdt");
        o.address = str(doc, "diachi");
        o.note = str(doc, "ghichu");
        o.date = str(doc, "ngaydat");
        o.payment = str(doc, "phuongthuc");
        o.total = MoneyUtil.parseVnd(doc.get("tongtien"));
        o.shippingFee = MoneyUtil.parseVnd(doc.get("phivanchuyen"));
        o.status = num(doc, "trangthai");
        o.shipperId = str(doc, "shipperId");
        o.shipperName = str(doc, "shipperName");
        o.deliverySlot = str(doc, "khung_gio_giao");
        o.deliveryDate = str(doc, "ngay_giao");
        o.cancelReason = str(doc, "cancelReason");
        o.cancelledBy = str(doc, "cancelledBy");
        return o;
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
