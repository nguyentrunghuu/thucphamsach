package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;

import com.UngDung.ThucPhamXanh.Helper.MoneyUtil;

import org.junit.Test;

/** Đọc tổng tiền hóa đơn ở mọi định dạng đang có trong Firestore. */
public class MoneyUtilTest {

    @Test
    public void docCacDinhDangTien() {
        assertEquals(255000L, MoneyUtil.parseVnd("255.000"));   // định dạng tiếng Việt
        assertEquals(255000L, MoneyUtil.parseVnd("255,000"));   // định dạng tiếng Anh
        assertEquals(255000L, MoneyUtil.parseVnd("255000"));    // chatbot lưu số trơn
        assertEquals(255000L, MoneyUtil.parseVnd("255.000 VNĐ"));
        assertEquals(255000L, MoneyUtil.parseVnd(255000L));     // kiểu số
        assertEquals(0L, MoneyUtil.parseVnd(null));
        assertEquals(0L, MoneyUtil.parseVnd(""));
    }

    @Test
    public void khongTranSoKhiDoanhThuLon() {
        // > 2,1 tỷ: trước đây dùng Integer.parseInt nên app bị crash
        assertEquals(3_500_000_000L, MoneyUtil.parseVnd("3.500.000.000"));
    }

    @Test
    public void dinhDangTien() {
        assertEquals("0 đ", MoneyUtil.formatVnd(0));
        assertEquals("999 đ", MoneyUtil.formatVnd(999));
        assertEquals("1.000 đ", MoneyUtil.formatVnd(1000));
        assertEquals("2.058.250 đ", MoneyUtil.formatVnd(2_058_250));
        assertEquals("120.000", MoneyUtil.formatNumber(120_000));
        assertEquals("3.500.000.000", MoneyUtil.formatNumber(3_500_000_000L));
        assertEquals("-12.500", MoneyUtil.formatNumber(-12_500));
    }
}
