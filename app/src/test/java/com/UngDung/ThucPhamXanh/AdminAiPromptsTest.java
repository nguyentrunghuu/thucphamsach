package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.UngDung.ThucPhamXanh.Helper.AdminAiPrompts;
import com.UngDung.ThucPhamXanh.Helper.AdminAiPrompts.OrderLine;
import com.UngDung.ThucPhamXanh.Helper.AdminAiPrompts.ProductLine;
import com.UngDung.ThucPhamXanh.Helper.RevenueReport;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AdminAiPromptsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    @Test
    public void assistantContextHasOrdersAndProductsButNoPersonalData() {
        List<OrderLine> orders = new ArrayList<>();
        orders.add(new OrderLine("TPS001", "10/11/2026", 3L, 250000));
        orders.add(new OrderLine("TPS002", "11/11/2026", null, 0));
        List<ProductLine> products = new ArrayList<>();
        products.add(new ProductLine("Tiêu đen", "Gia vị", 45000, 3, "", 12, 5));

        String p = AdminAiPrompts.assistantSystemPrompt(TODAY, orders, products);
        assertTrue(p.contains("14/11/2026"));
        assertTrue(p.contains("TPS001 | 10/11/2026 | Đã giao | 250.000"));
        assertTrue(p.contains("TPS002 | 11/11/2026 | ? | 0"));
        assertTrue(p.contains("Tiêu đen | Gia vị | 45.000 | 3 | - | 12 | 5"));
    }

    @Test
    public void assistantContextIsCapped() {
        List<OrderLine> orders = new ArrayList<>();
        for (int i = 0; i < AdminAiPrompts.MAX_ORDERS + 5; i++) orders.add(new OrderLine("D" + i, "01/01/2026", 3L, 1));
        String p = AdminAiPrompts.assistantSystemPrompt(TODAY, orders, new ArrayList<>());
        assertTrue(p.contains("D" + (AdminAiPrompts.MAX_ORDERS - 1) + " |"));
        assertFalse(p.contains("D" + AdminAiPrompts.MAX_ORDERS + " |"));
        assertTrue(p.contains("còn 5 đơn cũ hơn"));
    }

    @Test
    public void insightMessageContainsPeriodTotalsAndSeries() {
        RevenueReport r = new RevenueReport(RevenueReport.Range.lastDays(TODAY, 7));
        r.addOrder(3L, "10/11/2026", "150.000");
        r.addOrder(6L, "11/11/2026", "70.000");
        r.addOrder(4L, "12/11/2026", "30.000");
        String m = AdminAiPrompts.insightUserMessage(r);
        assertTrue(m.contains("08/11/2026 đến 14/11/2026 (7 ngày)"));
        assertTrue(m.contains("Doanh thu (đơn đã giao): 150.000 đ"));
        assertTrue(m.contains("đã hủy 1"));
        assertTrue(m.contains("1 đơn, trị giá 30.000 đ"));
        assertTrue(m.contains("10/11: 150.000"));
    }

    @Test
    public void descriptionMessageSkipsEmptyFields() {
        assertEquals("Viết mô tả cho sản phẩm: Cà rốt\nLoại: Rau Củ",
                AdminAiPrompts.descriptionUserMessage(" Cà rốt ", "Rau Củ", "", null));
        assertTrue(AdminAiPrompts.descriptionUserMessage("Cà rốt", "", "500g", "Đà Lạt")
                .endsWith("Quy cách: 500g\nXuất xứ: Đà Lạt"));
    }

    @Test
    public void cleanDescriptionStripsMarkdownAndQuotes() {
        assertEquals("Cà rốt giòn ngọt.", AdminAiPrompts.cleanDescription("\"**Cà rốt** giòn ngọt.\""));
        assertEquals("Cà rốt.", AdminAiPrompts.cleanDescription("## Cà rốt."));
        assertEquals("", AdminAiPrompts.cleanDescription(null));
    }

    @Test
    public void stripMarkdownTurnsStarBulletsIntoDashes() {
        assertEquals("- một\n- hai", AdminAiPrompts.stripMarkdown("* **một**\n* hai"));
    }
}
