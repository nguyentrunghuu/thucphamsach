package com.UngDung.ThucPhamXanh.Helper;

import java.io.Serializable;

/**
 * Một hóa đơn (collection "HoaDon") như admin cần xem — đầy đủ hơn Models/HoaDon (vốn còn làm DAO
 * và chỉ có 10 field). Tạo bằng {@link FirestoreMapper#toAdminOrder}; thuần Java nên test được.
 *
 * Field Firestore: UID, hoten, sdt, diachi, ghichu, ngaydat ("dd/MM/yyyy"), phuongthuc,
 * tongtien (chuỗi "370.000 VNĐ" hoặc số), phivanchuyen, trangthai (OrderStatus), madonhang,
 * shipperName, khung_gio_giao, ngay_giao, cancelReason, cancelledBy. Đơn cũ có thể thiếu field.
 */
public class AdminOrder implements Serializable {

    public String id = "";
    public String uid = "";
    public String code = "";          // madonhang; đơn cũ không có
    public String name = "";
    public String phone = "";
    public String address = "";
    public String note = "";
    public String date = "";          // ngaydat dd/MM/yyyy
    public String payment = "";
    public long total;                // tongtien đã đọc thành số
    public long shippingFee;          // phivanchuyen
    public long status;               // trangthai
    public String shipperId = "";     // username shipper đã nhận đơn; rỗng = chưa có
    public String shipperName = "";
    public String deliverySlot = "";  // khung_gio_giao
    public String deliveryDate = "";  // ngay_giao
    public String cancelReason = "";
    public String cancelledBy = "";

    /** Mã hiển thị: madonhang nếu có, không thì 8 ký tự đầu của document id. */
    public String displayCode() {
        if (code != null && !code.trim().isEmpty()) return code.trim();
        if (id == null) return "";
        return "#" + (id.length() > 8 ? id.substring(0, 8) : id);
    }
}
