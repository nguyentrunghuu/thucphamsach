package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.UngDung.ThucPhamXanh.Helper.ProductVisibility;

import org.junit.Test;

import java.time.LocalDate;

public class ProductVisibilityTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    @Test
    public void hetHan_chiKhiNgayDaQua() {
        assertTrue(ProductVisibility.isExpired("12/11/2026", TODAY));
        assertFalse(ProductVisibility.isExpired("13/11/2026", TODAY)); // hết hạn hôm nay: vẫn bán hết ngày
        assertFalse(ProductVisibility.isExpired("14/11/2026", TODAY));
    }

    @Test
    public void khongRoHan_vanBan() {
        assertFalse(ProductVisibility.isExpired(null, TODAY));
        assertFalse(ProductVisibility.isExpired("", TODAY));
        assertFalse(ProductVisibility.isExpired("abc", TODAY));
    }

    @Test
    public void lyDoKhongBan_uuTienHetHan() {
        assertNull(ProductVisibility.unavailableReason("20/11/2026", 5, TODAY));
        assertEquals("Sản phẩm đã hết hàng", ProductVisibility.unavailableReason("20/11/2026", 0, TODAY));
        assertEquals("Sản phẩm đã hết hạn sử dụng, tạm ngừng bán",
                ProductVisibility.unavailableReason("01/11/2026", 0, TODAY));
    }
}
