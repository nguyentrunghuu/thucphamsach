package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;

import com.UngDung.ThucPhamXanh.Helper.ShippingFeeHelper;

import org.junit.Test;

/** Kiểm tra nhận dạng tỉnh/thành và phí ship (cửa hàng đặt tại ĐH Điện lực, Hà Nội). */
public class ShippingFeeHelperTest {

    @Test
    public void nhanDangTinh_coDauKhongDauVietTat() {
        assertEquals("Hà Nội", ShippingFeeHelper.extractProvince("235 Hoàng Quốc Việt, Bắc Từ Liêm, Hà Nội"));
        assertEquals("Hà Nội", ShippingFeeHelper.extractProvince("235 Hoang Quoc Viet, Ha Noi"));
        assertEquals("Hà Nội", ShippingFeeHelper.extractProvince("12 Kim Ma, Ba Dinh, Hanoi"));
        assertEquals("Hà Nội", ShippingFeeHelper.extractProvince("Cầu Giấy, HN"));
        assertEquals("Hồ Chí Minh", ShippingFeeHelper.extractProvince("Quận 1, TP.HCM"));
        assertEquals("Hồ Chí Minh", ShippingFeeHelper.extractProvince("Quan 3, Sai Gon"));
        assertEquals("Đà Nẵng", ShippingFeeHelper.extractProvince("Hải Châu, Da Nang"));
        assertEquals("Hà Nam", ShippingFeeHelper.extractProvince("Phủ Lý, Hà Nam"));
    }

    @Test
    public void tenPhoTrungTenTinh_layTinhOCuoi() {
        // "Thái Bình" là tên phố, tỉnh thật là Hà Nội ở cuối
        assertEquals("Hà Nội", ShippingFeeHelper.extractProvince("5 phố Thái Bình, Đống Đa, Hà Nội"));
    }

    @Test
    public void khongNhanRaTinh_khongDuocCoiLaCungTinh() {
        assertEquals("", ShippingFeeHelper.extractProvince("Số 10 đường ABC"));
        assertEquals(false, ShippingFeeHelper.isSameProvinceAsStore("Số 10 đường ABC"));
    }

    @Test
    public void phiShip_theoKhoangCachVaTinh() {
        // Không có tọa độ, không có context -> dựa vào tỉnh
        assertEquals(20000, ShippingFeeHelper.calculateShippingFee("Hoàn Kiếm, Ha Noi"));
        assertEquals(30000, ShippingFeeHelper.calculateShippingFee("Quận 1, TP.HCM"));
        assertEquals(30000, ShippingFeeHelper.calculateShippingFee("Số 10 đường ABC"));

        // Có tọa độ: ngay cạnh trường (< 5km) -> miễn phí
        assertEquals(0, ShippingFeeHelper.calculateShippingFee(null, "Cầu Giấy, Hà Nội", 21.036, 105.790));
        // Có tọa độ: Hà Đông (> 5km, cùng Hà Nội) -> 20k
        assertEquals(20000, ShippingFeeHelper.calculateShippingFee(null, "Hà Đông, Hà Nội", 20.955, 105.756));
        // Có tọa độ: TP.HCM -> 30k
        assertEquals(30000, ShippingFeeHelper.calculateShippingFee(null, "Quận 1, TP.HCM", 10.776, 106.700));
    }
}
