package com.UngDung.ThucPhamXanh.Helper;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Dòng chi tiết đơn (ChitietHoaDon/{uid}/ALL: id_hoadon, id_product, soluong, giaBan) + sản phẩm hiện tại
 * (SanPham/{id}: tensp, hinhanh, giatien) → {@link OrderMath.Line}. Chịu được dòng thiếu field và sản phẩm
 * đã bị xóa hẳn (product == null).
 */
public final class OrderLineMapper {

    private OrderLineMapper() { }

    public static OrderMath.Line toLine(DocumentSnapshot line, DocumentSnapshot product) {
        String productId = FirestoreMapper.str(line, "id_product");
        long qty = FirestoreMapper.num(line, "soluong");
        Long atOrder = FirestoreMapper.numOrNull(line, "giaBan");
        boolean hasProduct = product != null && product.exists();
        long current = hasProduct ? FirestoreMapper.num(product, "giatien") : 0;
        String name = hasProduct ? FirestoreMapper.str(product, "tensp") : "";
        if (name.trim().isEmpty()) name = "Sản phẩm không còn bán";
        String image = hasProduct ? FirestoreMapper.str(product, "hinhanh") : "";
        boolean currentPrice = atOrder == null || atOrder <= 0;
        return new OrderMath.Line(productId, name, image, qty, OrderMath.unitPrice(atOrder, current), currentPrice);
    }
}
