package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;

import com.UngDung.ThucPhamXanh.Helper.SalesTally;

import org.junit.Test;

public class SalesTallyTest {

    @Test
    public void countsOrderedExceptCancelledAndDeliveredOnly() {
        SalesTally t = new SalesTally();
        t.add(3L, "A", 2);   // đã giao
        t.add(4L, "A", 3);   // đang giao
        t.add(6L, "A", 10);  // đã hủy → bỏ
        t.add(1L, "B", 1);
        assertEquals(5, t.ordered("A"));
        assertEquals(2, t.delivered("A"));
        assertEquals(1, t.ordered("B"));
        assertEquals(0, t.delivered("B"));
        assertEquals(0, t.ordered("zzz"));
    }

    @Test
    public void ignoresBrokenLines() {
        SalesTally t = new SalesTally();
        t.add(null, "A", 5);
        t.add(3L, null, 5);
        t.add(3L, "", 5);
        t.add(3L, "A", 0);
        t.add(3L, "A", -2);
        assertEquals(0, t.ordered("A"));
    }
}
