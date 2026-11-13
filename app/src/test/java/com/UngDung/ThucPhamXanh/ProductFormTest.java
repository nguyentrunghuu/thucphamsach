package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.UngDung.ThucPhamXanh.Helper.ProductForm;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class ProductFormTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    private static ProductForm.Input valid() {
        ProductForm.Input in = new ProductForm.Input();
        in.image = "https://x/y.jpg";
        in.name = "Bắp bò";
        in.price = "250.000";
        in.category = "Thịt bò";
        in.quantity = "25";
        in.weight = "1kg";
        in.expiry = "17/11/2026";
        in.description = "Bắp bò tươi";
        return in;
    }

    @Test
    public void hopLe_khongLoi() {
        assertTrue(ProductForm.validate(valid(), TODAY, true).isEmpty());
    }

    @Test
    public void docSo_boPhanCachNghin() {
        assertEquals(Long.valueOf(35000), ProductForm.parseWholeNumber("35.000"));
        assertEquals(Long.valueOf(35000), ProductForm.parseWholeNumber(" 35,000 "));
        assertEquals(Long.valueOf(35000), ProductForm.parseWholeNumber("35 000"));
        assertEquals(Long.valueOf(0), ProductForm.parseWholeNumber("0"));
        assertNull(ProductForm.parseWholeNumber("-5"));
        assertNull(ProductForm.parseWholeNumber("12a"));
        assertNull(ProductForm.parseWholeNumber(""));
        assertNull(ProductForm.parseWholeNumber(null));
        assertNull(ProductForm.parseWholeNumber("9999999999999999999"));
    }

    @Test
    public void formTrong_baoDuLoi_theoThuTu() {
        Map<String, String> e = ProductForm.validate(new ProductForm.Input(), TODAY, true);
        assertEquals(Arrays.asList(ProductForm.F_IMAGE, ProductForm.F_NAME, ProductForm.F_PRICE,
                        ProductForm.F_CATEGORY, ProductForm.F_QUANTITY, ProductForm.F_WEIGHT,
                        ProductForm.F_EXPIRY, ProductForm.F_DESCRIPTION),
                new ArrayList<>(e.keySet()));
    }

    @Test
    public void gia_phaiDuong_vaKhongQuaLon() {
        ProductForm.Input in = valid();
        in.price = "0";
        assertTrue(ProductForm.validate(in, TODAY, true).containsKey(ProductForm.F_PRICE));
        in.price = "200000000";
        assertTrue(ProductForm.validate(in, TODAY, true).containsKey(ProductForm.F_PRICE));
        in.price = "abc";
        assertEquals("Giá chỉ gồm chữ số", ProductForm.validate(in, TODAY, true).get(ProductForm.F_PRICE));
    }

    @Test
    public void soLuong_choPhep0() {
        ProductForm.Input in = valid();
        in.quantity = "0";
        assertFalse(ProductForm.validate(in, TODAY, true).containsKey(ProductForm.F_QUANTITY));
        in.quantity = "-1";
        assertTrue(ProductForm.validate(in, TODAY, true).containsKey(ProductForm.F_QUANTITY));
    }

    @Test
    public void hanDung_daQua_chiChanKhiThemMoi() {
        ProductForm.Input in = valid();
        in.expiry = "12/11/2026";
        assertTrue(ProductForm.validate(in, TODAY, true).containsKey(ProductForm.F_EXPIRY));
        assertFalse(ProductForm.validate(in, TODAY, false).containsKey(ProductForm.F_EXPIRY));
        in.expiry = "13/11/2026"; // hôm nay vẫn được
        assertFalse(ProductForm.validate(in, TODAY, true).containsKey(ProductForm.F_EXPIRY));
        in.expiry = "31/02/2026";
        assertTrue(ProductForm.validate(in, TODAY, false).containsKey(ProductForm.F_EXPIRY));
    }

    @Test
    public void xuatXu_tuyChon() {
        ProductForm.Input in = valid();
        in.origin = null;
        assertTrue(ProductForm.validate(in, TODAY, true).isEmpty());
    }

    @Test
    public void nhomHienThi_haiChieu() {
        assertEquals("Thường", ProductForm.typeLabel(1));
        assertEquals("Nổi bật", ProductForm.typeLabel(2));
        assertEquals("Nhóm 42", ProductForm.typeLabel(42));
        assertEquals(Long.valueOf(2), ProductForm.typeOf("Nổi bật"));
        assertEquals(Long.valueOf(42), ProductForm.typeOf("Nhóm 42"));
        assertNull(ProductForm.typeOf("???"));
    }

    @Test
    public void goiYHanDung() {
        assertEquals("Còn 4 ngày", ProductForm.expiryHint("17/11/2026", TODAY));
        assertEquals("Hết hạn hôm nay", ProductForm.expiryHint("13/11/2026", TODAY));
        assertEquals("Đã hết hạn 3 ngày", ProductForm.expiryHint("10/11/2026", TODAY));
        assertEquals("", ProductForm.expiryHint("", TODAY));
    }
}
