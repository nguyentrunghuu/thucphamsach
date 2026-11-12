package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;

import com.UngDung.ThucPhamXanh.Helper.ProductSearch;
import com.UngDung.ThucPhamXanh.Helper.TextNormalizer;
import com.UngDung.ThucPhamXanh.Models.Product;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Kiểm tra tìm kiếm không dấu + lọc + sắp xếp của màn tìm kiếm. */
public class ProductSearchTest {

    private ProductSearch search;
    private List<Product> products;

    private static Product p(String id, String name, long price, String cat, long stock) {
        return new Product(id, name, price, "", cat, "", stock, "", 2, "");
    }

    private List<String> ids() {
        List<String> r = new ArrayList<>();
        for (Product pr : search.apply(products)) r.add(pr.getId());
        return r;
    }

    @Before
    public void setUp() {
        products = Arrays.asList(
                p("1", "Rau muống", 15_000, "Rau Củ", 10),
                p("2", "Cải bó xôi", 30_000, "Rau Củ", 0),      // hết hàng
                p("3", "Thịt ba chỉ", 155_000, "Thịt", 5),
                p("4", "Tôm sú", 250_000, "Hải sản", 3),
                p("5", "Rau thơm", 8_000, "Rau Củ", 20));
        Map<String, Long> sold = new HashMap<>();
        sold.put("3", 50L);
        sold.put("5", 120L);
        sold.put("1", 10L);
        search = new ProductSearch();
        search.setSoldCounts(sold);
    }

    @Test
    public void boDau() {
        assertEquals("rau cu", TextNormalizer.normalize("Rau Củ"));
        assertEquals("thit ba chi", TextNormalizer.normalize("  Thịt  ba chỉ "));
        assertEquals("dau phong", TextNormalizer.normalize("Đậu phộng"));
    }

    @Test
    public void timKhongDau_bangDiemThiMonBanChayLenTruoc() {
        search.setQuery("rau");                       // gõ không dấu vẫn ra "Rau ..."
        // "Rau thơm", "Rau muống" khớp tên (bán chạy hơn đứng trước); "Cải bó xôi" chỉ khớp danh mục -> cuối
        assertEquals(Arrays.asList("5", "1", "2"), ids());
        search.setQuery("thit");
        assertEquals(Arrays.asList("3"), ids());
        search.setQuery("rau cu");                    // khớp theo danh mục
        assertEquals(3, ids().size());
    }

    @Test
    public void sapXepBanChay() {
        search.setSort(ProductSearch.SORT_BESTSELLER);
        assertEquals(Arrays.asList("5", "3", "1", "2", "4"), ids());
    }

    @Test
    public void sapXepTheoGia() {
        search.setSort(ProductSearch.SORT_PRICE_ASC);
        assertEquals(Arrays.asList("5", "1", "2", "3", "4"), ids());
        search.setSort(ProductSearch.SORT_PRICE_DESC);
        assertEquals(Arrays.asList("4", "3", "2", "1", "5"), ids());
    }

    @Test
    public void locDanhMucGiaVaConHang() {
        search.setSort(ProductSearch.SORT_PRICE_ASC);
        search.setFilters("Rau Củ", 0, Long.MAX_VALUE, false);
        assertEquals(Arrays.asList("5", "1", "2"), ids());
        search.setFilters("Rau Củ", 0, Long.MAX_VALUE, true);     // bỏ món hết hàng
        assertEquals(Arrays.asList("5", "1"), ids());
        search.setFilters(null, 20_000, 200_000, false);           // khoảng giá
        assertEquals(Arrays.asList("2", "3"), ids());
        assertEquals(1, search.activeFilterCount());
    }
}
