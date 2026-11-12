package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import org.junit.Test;

/** Giữ cố định quy ước mã trạng thái đơn hàng dùng chung cho khách, admin, shipper. */
public class OrderStatusTest {

    @Test
    public void maTrangThai_khopVoiAdminVaShipper() {
        assertEquals(1, OrderStatus.DA_DAT);
        assertEquals(2, OrderStatus.DANG_XU_LY);
        assertEquals(3, OrderStatus.DA_GIAO);
        assertEquals(4, OrderStatus.DANG_GIAO);   // shipper ghi 4 khi nhận đi giao
        assertEquals(5, OrderStatus.DA_XAC_NHAN);
        assertEquals(6, OrderStatus.DA_HUY);      // KHÔNG được trùng với "Đang giao" (4)
    }

    @Test
    public void nhanHienThi() {
        assertEquals("Đang giao hàng", OrderStatus.label(4));
        assertEquals("Đã hủy", OrderStatus.label(6));
        assertEquals("Đã giao", OrderStatus.label(3));
        assertEquals("Không xác định", OrderStatus.label(99));
    }

    @Test
    public void khachChiHuyDuocKhiChuaXacNhan() {
        assertTrue(OrderStatus.customerCanCancel(OrderStatus.DA_DAT));
        assertTrue(OrderStatus.customerCanCancel(OrderStatus.DANG_XU_LY));
        assertFalse(OrderStatus.customerCanCancel(OrderStatus.DA_XAC_NHAN));
        assertFalse(OrderStatus.customerCanCancel(OrderStatus.DANG_GIAO));
        assertFalse(OrderStatus.customerCanCancel(OrderStatus.DA_GIAO));
        assertFalse(OrderStatus.customerCanCancel(OrderStatus.DA_HUY));
    }

    @Test
    public void adminKhongSuaDonDaKetThuc_nhungSuaDuocDonDangGiao() {
        assertTrue(OrderStatus.isFinal(OrderStatus.DA_GIAO));
        assertTrue(OrderStatus.isFinal(OrderStatus.DA_HUY));
        assertFalse(OrderStatus.isFinal(OrderStatus.DANG_GIAO)); // trước đây bị chặn nhầm
    }
}
