package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.UngDung.ThucPhamXanh.Helper.AdminOrderFlow;
import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import org.junit.Test;

/** Admin chỉ được đổi trạng thái đơn theo đúng luồng, không lùi, không nhảy cóc. */
public class AdminOrderFlowTest {

    @Test
    public void daDat_duocXuLy_xacNhan_hoacHuy() {
        assertArrayEquals(new int[]{OrderStatus.DANG_XU_LY, OrderStatus.DA_XAC_NHAN, OrderStatus.DA_HUY},
                AdminOrderFlow.nextStatuses(OrderStatus.DA_DAT));
    }

    @Test
    public void luongDayDu_diToiTungBuoc() {
        assertTrue(AdminOrderFlow.canTransition(OrderStatus.DA_DAT, OrderStatus.DANG_XU_LY));
        assertTrue(AdminOrderFlow.canTransition(OrderStatus.DANG_XU_LY, OrderStatus.DA_XAC_NHAN));
        assertTrue(AdminOrderFlow.canTransition(OrderStatus.DA_XAC_NHAN, OrderStatus.DANG_GIAO));
        assertTrue(AdminOrderFlow.canTransition(OrderStatus.DANG_GIAO, OrderStatus.DA_GIAO));
    }

    @Test
    public void khongNhayCoc_sangDaGiao() {
        assertFalse(AdminOrderFlow.canTransition(OrderStatus.DA_DAT, OrderStatus.DA_GIAO));
        assertFalse(AdminOrderFlow.canTransition(OrderStatus.DANG_XU_LY, OrderStatus.DA_GIAO));
        assertFalse(AdminOrderFlow.canTransition(OrderStatus.DA_XAC_NHAN, OrderStatus.DA_GIAO));
        assertFalse(AdminOrderFlow.canTransition(OrderStatus.DA_DAT, OrderStatus.DANG_GIAO));
    }

    @Test
    public void khongDiLui() {
        assertFalse(AdminOrderFlow.canTransition(OrderStatus.DANG_GIAO, OrderStatus.DA_XAC_NHAN));
        assertFalse(AdminOrderFlow.canTransition(OrderStatus.DA_XAC_NHAN, OrderStatus.DANG_XU_LY));
        assertFalse(AdminOrderFlow.canTransition(OrderStatus.DANG_XU_LY, OrderStatus.DA_DAT));
        assertFalse(AdminOrderFlow.canTransition(OrderStatus.DANG_XU_LY, OrderStatus.DANG_XU_LY));
    }

    @Test
    public void huy_chiKhiChuaGiaoChoShipper() {
        assertTrue(AdminOrderFlow.adminCanCancel(OrderStatus.DA_DAT));
        assertTrue(AdminOrderFlow.adminCanCancel(OrderStatus.DANG_XU_LY));
        assertTrue(AdminOrderFlow.adminCanCancel(OrderStatus.DA_XAC_NHAN));
        assertFalse(AdminOrderFlow.adminCanCancel(OrderStatus.DANG_GIAO));
        assertFalse(AdminOrderFlow.adminCanCancel(OrderStatus.DA_GIAO));
        assertFalse(AdminOrderFlow.adminCanCancel(OrderStatus.DA_HUY));
    }

    @Test
    public void trangThaiCuoi_vaMaLa_khongDoiDuoc() {
        assertEquals(0, AdminOrderFlow.nextStatuses(OrderStatus.DA_GIAO).length);
        assertEquals(0, AdminOrderFlow.nextStatuses(OrderStatus.DA_HUY).length);
        assertEquals(0, AdminOrderFlow.nextStatuses(0).length);
        assertEquals(0, AdminOrderFlow.nextStatuses(99).length);
        assertFalse(AdminOrderFlow.canTransition(99, OrderStatus.DA_HUY));
        // long vượt phạm vi int không được "quay vòng" thành mã hợp lệ (2^32 + 1 -> 1)
        assertFalse(AdminOrderFlow.canTransition(4294967297L, OrderStatus.DANG_XU_LY));
    }

    @Test
    public void trangThaiCuoi_khongCoBuocTiep_vaKhongTuChuyenSangChinhNo() {
        for (int s = 1; s <= 6; s++) {
            int[] next = AdminOrderFlow.nextStatuses(s);
            if (OrderStatus.isFinal(s)) assertEquals(0, next.length);
            for (int n : next) assertTrue(n != s);
        }
    }

    @Test
    public void choXacNhan_laDaDatVaDangXuLy() {
        assertTrue(AdminOrderFlow.isPending(OrderStatus.DA_DAT));
        assertTrue(AdminOrderFlow.isPending(OrderStatus.DANG_XU_LY));
        assertFalse(AdminOrderFlow.isPending(OrderStatus.DA_XAC_NHAN));
        assertFalse(AdminOrderFlow.isPending(OrderStatus.DA_HUY));
    }
}
