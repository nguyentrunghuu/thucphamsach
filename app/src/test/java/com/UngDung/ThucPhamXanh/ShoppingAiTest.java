package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.UngDung.ThucPhamXanh.Helper.ShoppingAi;
import com.UngDung.ThucPhamXanh.Models.Product;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShoppingAiTest {

    private static Product p(String id, String name, String cat) {
        return new Product(id, name, 10000, "", cat, "", 10, "", 1, "");
    }

    private static List<Product> catalog() {
        return Arrays.asList(
                p("1", "Cà chua", "Rau Củ"),
                p("2", "Thịt heo ba chỉ", "Thịt heo"),
                p("3", "Dứa (Thơm)", "Trái cây"),
                p("4", "Hành lá", "Rau Lá"));
    }

    @Test
    public void matchProducts_ignoresCaseAccentsAndUnknownNames() {
        List<Product> r = ShoppingAi.matchProducts(Arrays.asList("CA CHUA", "Sầu riêng", "hành lá", "Cà chua"), catalog(), 10);
        assertEquals(2, r.size());               // không trùng, tên lạ bị bỏ
        assertEquals("1", r.get(0).getId());
        assertEquals("4", r.get(1).getId());
    }

    @Test
    public void matchProducts_partialNameAndLimit() {
        List<Product> r = ShoppingAi.matchProducts(Arrays.asList("thịt heo", "dứa"), catalog(), 1);
        assertEquals(1, r.size());
        assertEquals("2", r.get(0).getId());
        assertEquals("3", ShoppingAi.matchProducts(Arrays.asList("dứa"), catalog(), 5).get(0).getId());
    }

    @Test
    public void parseDishes_readsJsonInsideMarkdownFenceAndMatchesIngredients() {
        String raw = "Đây là gợi ý:\n```json\n{\"monan\":[{\"ten\":\"Canh chua\",\"mota\":\"Nấu với dứa.\","
                + "\"thieu\":[\"Dứa (Thơm)\",\"Món không có\"]},{\"ten\":\"Thịt kho\",\"mota\":\"\",\"thieu\":[]}]}\n```";
        List<ShoppingAi.Dish> dishes = ShoppingAi.parseDishes(raw, catalog());
        assertEquals(2, dishes.size());
        assertEquals("Canh chua", dishes.get(0).name);
        assertEquals(1, dishes.get(0).missing.size());
        assertEquals("3", dishes.get(0).missing.get(0).getId());
        assertTrue(dishes.get(1).missing.isEmpty());
    }

    @Test
    public void parseDishes_badInputGivesEmptyList() {
        assertTrue(ShoppingAi.parseDishes(null, catalog()).isEmpty());
        assertTrue(ShoppingAi.parseDishes("không phải json", catalog()).isEmpty());
        assertTrue(ShoppingAi.parseDishes("{\"khac\":1}", catalog()).isEmpty());
        assertTrue(ShoppingAi.parseDishes("{\"monan\":[{\"mota\":\"thiếu tên\"}]}", catalog()).isEmpty());
    }

    @Test
    public void parseRecommendations_matchesCatalogOnly() {
        List<Product> r = ShoppingAi.parseRecommendations("{\"goiy\":[\"Hành lá\",\"Xoài\",\"Cà chua\"]}", catalog(), 8);
        assertEquals(2, r.size());
        assertEquals("4", r.get(0).getId());
    }

    @Test
    public void prompts_containCatalogAndInputs() {
        String sys = ShoppingAi.dishSystemPrompt(catalog());
        assertTrue(sys.contains("- Cà chua (Rau Củ)"));
        assertEquals("Giỏ hàng của tôi có: Cà chua, Hành lá.", ShoppingAi.dishUserMessage(Arrays.asList("Cà chua", "Hành lá")));
        String u = ShoppingAi.recommendUserMessage(new ArrayList<>(), Arrays.asList("Cà chua"));
        assertTrue(u.contains("(chưa có)"));
        assertTrue(u.contains("yêu thích: Cà chua"));
    }
}
