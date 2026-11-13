package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.UngDung.ThucPhamXanh.Helper.AdminProductFilter;
import com.UngDung.ThucPhamXanh.Models.Product;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AdminProductFilterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    private static Product p(String id, String name, String cat, long stock, String hsd) {
        return new Product(id, name, 10000, "", cat, "mô tả " + name, stock, hsd, 0, "");
    }

    private final List<Product> all = Arrays.asList(
            p("1", "Tiêu đen", "Gia vị", 50, "01/01/2028"),
            p("2", "Cải bó xôi", "Rau", 3, "20/11/2026"),       // sắp hết hàng
            p("3", "Bò thăn", "Thịt", 20, "14/11/2026"),        // còn 1 ngày
            p("4", "Ốc hương", "Hải sản", 0, "10/11/2026"),     // hết hàng + hết hạn
            p("5", "Đậu phộng", "Đồ khô", 100, ""));            // không rõ hạn

    private static List<String> ids(List<Product> list) {
        List<String> r = new ArrayList<>();
        for (Product x : list) r.add(x.getId());
        return r;
    }

    @Test
    public void macDinh_sapTheoTenKhongDau() {
        AdminProductFilter f = new AdminProductFilter(TODAY);
        assertFalse(f.isFiltering());
        // bo, cai, dau, oc, tieu
        assertEquals(Arrays.asList("3", "2", "5", "4", "1"), ids(f.apply(all)));
    }

    @Test
    public void timKiem_khongDau_theoTenLoaiMoTa() {
        AdminProductFilter f = new AdminProductFilter(TODAY);
        f.setQuery("tieu");
        assertEquals(Arrays.asList("1"), ids(f.apply(all)));
        f.setQuery("  HAI SAN ");
        assertEquals(Arrays.asList("4"), ids(f.apply(all)));
        f.setQuery("xyz");
        assertTrue(f.apply(all).isEmpty());
        assertTrue(f.isFiltering());
    }

    @Test
    public void locTheoLoai() {
        AdminProductFilter f = new AdminProductFilter(TODAY);
        f.setCategory("Rau");
        assertEquals(Arrays.asList("2"), ids(f.apply(all)));
        f.setCategory("");
        assertEquals(5, f.apply(all).size());
    }

    @Test
    public void sapHetHang_gomHetHang_gapNhatTruoc() {
        AdminProductFilter f = new AdminProductFilter(TODAY);
        f.setStatus(AdminProductFilter.STATUS_LOW_STOCK);
        assertEquals(Arrays.asList("4", "2"), ids(f.apply(all)));
    }

    @Test
    public void hanDung_gomDaHetHanVaSapHetHan() {
        AdminProductFilter f = new AdminProductFilter(TODAY);
        f.setStatus(AdminProductFilter.STATUS_EXPIRY);
        assertEquals(Arrays.asList("4", "3"), ids(f.apply(all)));
    }

    @Test
    public void ketHopLoaiVaTrangThai() {
        AdminProductFilter f = new AdminProductFilter(TODAY);
        f.setStatus(AdminProductFilter.STATUS_LOW_STOCK);
        f.setCategory("Thịt");
        assertTrue(f.apply(all).isEmpty());
    }

    @Test
    public void nullAnToan() {
        AdminProductFilter f = new AdminProductFilter(TODAY);
        f.setQuery(null);
        assertTrue(f.apply(null).isEmpty());
        assertEquals(1, f.apply(Arrays.asList(null, p("9", null, null, 1, null))).size());
    }

    @Test
    public void demTheoLoai_giuThuTu_themLoaiLa() {
        Map<String, Integer> m = AdminProductFilter.countByCategory(
                Arrays.asList("Rau", "Thịt", "Trái cây"), all);
        assertEquals(Arrays.asList("Rau", "Thịt", "Trái cây", "Gia vị", "Hải sản", "Đồ khô"),
                new ArrayList<>(m.keySet()));
        assertEquals(Integer.valueOf(1), m.get("Rau"));
        assertEquals(Integer.valueOf(0), m.get("Trái cây"));
        assertEquals(Integer.valueOf(1), m.get("Gia vị"));
    }
}
