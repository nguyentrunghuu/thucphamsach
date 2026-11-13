package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.AdminOrderFilter;
import com.UngDung.ThucPhamXanh.Helper.OrderMath;
import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminOrderFilterTest {

    private static AdminOrder o(String id, String name, String phone, String date, long status, String code) {
        AdminOrder x = new AdminOrder();
        x.id = id;
        x.name = name;
        x.phone = phone;
        x.date = date;
        x.status = status;
        x.code = code;
        return x;
    }

    private final List<AdminOrder> all = Arrays.asList(
            o("a1", "Hữu Tình", "0349132622", "05/12/2025", OrderStatus.DA_GIAO, "TPS0512250010TM"),
            o("b2", "Nguyễn Văn An", "0912 345 678", "12/11/2026", OrderStatus.DA_DAT, ""),
            o("c3", "Trần Thị Bình", "0987000111", "13/11/2026", OrderStatus.DANG_XU_LY, ""),
            o("d4", "Lê Cường", "0900000000", "01/11/2026", OrderStatus.DA_XAC_NHAN, ""),
            o("e5", "Phạm Dũng", "0911111111", "10/11/2026", OrderStatus.DANG_GIAO, ""),
            o("f6", "Võ Em", "0922222222", "", OrderStatus.DA_HUY, ""));

    private static List<String> ids(List<AdminOrder> list) {
        List<String> r = new ArrayList<>();
        for (AdminOrder x : list) r.add(x.id);
        return r;
    }

    @Test
    public void tatCa_moiNhatTruoc_ngayLoiXuongCuoi() {
        assertEquals(Arrays.asList("c3", "b2", "e5", "d4", "a1", "f6"), ids(new AdminOrderFilter().apply(all)));
    }

    @Test
    public void choXacNhan_gopDaDatVaDangXuLy() {
        AdminOrderFilter f = new AdminOrderFilter();
        f.setTab(AdminOrderFilter.TAB_PENDING);
        assertEquals(Arrays.asList("c3", "b2"), ids(f.apply(all)));
        assertTrue(f.isFiltering());
    }

    @Test
    public void moiTab_dungMotMa() {
        AdminOrderFilter f = new AdminOrderFilter();
        f.setTab(AdminOrderFilter.TAB_CONFIRMED);
        assertEquals(Arrays.asList("d4"), ids(f.apply(all)));
        f.setTab(AdminOrderFilter.TAB_SHIPPING);
        assertEquals(Arrays.asList("e5"), ids(f.apply(all)));
        f.setTab(AdminOrderFilter.TAB_DELIVERED);
        assertEquals(Arrays.asList("a1"), ids(f.apply(all)));
        f.setTab(AdminOrderFilter.TAB_CANCELLED);
        assertEquals(Arrays.asList("f6"), ids(f.apply(all)));
        f.setTab(99); // sai → tất cả
        assertEquals(6, f.apply(all).size());
    }

    @Test
    public void timKiem_tenKhongDau_sdtBoKhoangTrang_maDon() {
        AdminOrderFilter f = new AdminOrderFilter();
        f.setQuery("binh");
        assertEquals(Arrays.asList("c3"), ids(f.apply(all)));
        f.setQuery("0912345");
        assertEquals(Arrays.asList("b2"), ids(f.apply(all)));
        f.setQuery("tps0512");
        assertEquals(Arrays.asList("a1"), ids(f.apply(all)));
        f.setQuery("e5");
        assertEquals(Arrays.asList("e5"), ids(f.apply(all)));
    }

    @Test
    public void demTheoTab_apTuKhoa() {
        AdminOrderFilter f = new AdminOrderFilter();
        assertArrayEquals(new int[]{6, 2, 1, 1, 1, 1}, f.countByTab(all));
        f.setQuery("nguyen");
        assertArrayEquals(new int[]{1, 1, 0, 0, 0, 0}, f.countByTab(all));
    }

    @Test
    public void maHienThi() {
        assertEquals("TPS0512250010TM", all.get(0).displayCode());
        AdminOrder x = o("AbCdEfGhIjKl", "", "", "", 1, "");
        assertEquals("#AbCdEfGh", x.displayCode());
    }

    @Test
    public void tienDon_giaLucDat_vaGiamGiaSuyRa() {
        assertEquals(17500, OrderMath.unitPrice(17500L, 35000));
        assertEquals(35000, OrderMath.unitPrice(null, 35000));
        assertEquals(35000, OrderMath.unitPrice(0L, 35000));

        List<OrderMath.Line> lines = Arrays.asList(
                new OrderMath.Line("p1", "A", "", 2, 17500, false),
                new OrderMath.Line("p2", "B", "", 1, 180000, false));
        assertEquals(215000, OrderMath.subtotal(lines));
        assertEquals(3, OrderMath.totalQuantity(lines));
        assertEquals(15000, OrderMath.discount(215000, 30000, 230000));
        assertEquals(0, OrderMath.discount(215000, 30000, 245000));
        assertEquals(0, OrderMath.discount(215000, 30000, 300000)); // tổng lớn hơn: không âm
        assertFalse(lines.get(0).priceIsCurrent);
    }
}
