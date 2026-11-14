package com.UngDung.ThucPhamXanh.Helper;

import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Cộng số lượng bán theo sản phẩm từ chi tiết các đơn (ChitietHoaDon/{uid}/ALL), tính thuần để test được.
 *
 * Trường SanPham.daban KHÔNG đáng tin (không sản phẩm nào có trong dữ liệu thật), nên số bán được
 * tính lại từ chi tiết đơn:
 *   - "đã đặt"  = mọi đơn trừ đơn đã hủy (đơn đã hủy đã trả hàng về kho nên không phải hàng bán được);
 *   - "đã giao" = chỉ đơn đã giao.
 */
public final class SalesTally {

    private final Map<String, Long> ordered = new HashMap<>();
    private final Map<String, Long> delivered = new HashMap<>();

    /**
     * @param orderStatus trạng thái của đơn chứa dòng này (null → bỏ qua vì không biết)
     * @param productId   SanPham doc id (id_product)
     * @param qty         số lượng của dòng
     */
    public void add(Long orderStatus, String productId, long qty) {
        if (orderStatus == null || productId == null || productId.isEmpty() || qty <= 0) return;
        if (orderStatus == OrderStatus.DA_HUY) return;
        ordered.merge(productId, qty, Long::sum);
        if (orderStatus == OrderStatus.DA_GIAO) delivered.merge(productId, qty, Long::sum);
    }

    public long ordered(String productId) {
        Long v = ordered.get(productId);
        return v == null ? 0 : v;
    }

    public long delivered(String productId) {
        Long v = delivered.get(productId);
        return v == null ? 0 : v;
    }
}
