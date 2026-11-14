package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.CustomerOrders;
import com.UngDung.ThucPhamXanh.Helper.CustomerOrders.Entry;
import com.UngDung.ThucPhamXanh.Helper.OrderMath;
import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomerOrdersTest {

    private static OrderMath.Line line(String name, long qty) {
        return new OrderMath.Line("p-" + name, name, "", qty, 10000, false);
    }

    private static Entry e(String id, String code, String date, long status, OrderMath.Line... lines) {
        AdminOrder o = new AdminOrder();
        o.id = id;
        o.code = code;
        o.date = date;
        o.status = status;
        o.name = "Hữu Tình";
        return new Entry(o, Arrays.asList(lines));
    }

    private final List<Entry> all = Arrays.asList(
            e("a", "TPS01", "05/12/2025", OrderStatus.DA_GIAO, line("Củ Sắn", 1)),
            e("b", "TPS02", "12/11/2026", OrderStatus.DA_DAT, line("Thịt ba chỉ", 2), line("Hành lá", 1)),
            e("c", "TPS03", "13/11/2026", OrderStatus.DANG_XU_LY, line("Cua biển", 1)),
            e("d", "TPS04", "01/11/2026", OrderStatus.DA_XAC_NHAN, line("Hàu", 6)),
            e("e", "TPS05", "10/11/2026", OrderStatus.DANG_GIAO, line("Bò cuộn nấm", 1)),
            e("f", "TPS06", "", OrderStatus.DA_HUY, line("Rau muống", 3)));

    private static List<String> ids(List<Entry> list) {
        List<String> r = new ArrayList<>();
        for (Entry x : list) r.add(x.order.id);
        return r;
    }

    @Test
    public void tabsGroupStatusesTheWayCustomersThinkAboutThem() {
        assertTrue(CustomerOrders.inTab(OrderStatus.DA_DAT, CustomerOrders.TAB_PROCESSING));
        assertTrue(CustomerOrders.inTab(OrderStatus.DANG_XU_LY, CustomerOrders.TAB_PROCESSING));
        assertTrue(CustomerOrders.inTab(OrderStatus.DA_XAC_NHAN, CustomerOrders.TAB_PROCESSING));
        assertFalse(CustomerOrders.inTab(OrderStatus.DANG_GIAO, CustomerOrders.TAB_PROCESSING));
        assertTrue(CustomerOrders.inTab(OrderStatus.DANG_GIAO, CustomerOrders.TAB_SHIPPING));
        assertTrue(CustomerOrders.inTab(OrderStatus.DA_GIAO, CustomerOrders.TAB_DONE));
        assertTrue(CustomerOrders.inTab(OrderStatus.DA_HUY, CustomerOrders.TAB_CANCELLED));
        assertTrue(CustomerOrders.inTab(99, CustomerOrders.TAB_ALL));
    }

    @Test
    public void everyKnownStatusLandsInExactlyOneSpecificTab() {
        for (int s = 1; s <= 6; s++) {
            int n = 0;
            for (int t = 1; t < CustomerOrders.TAB_COUNT; t++) if (CustomerOrders.inTab(s, t)) n++;
            assertEquals("status " + s, 1, n);
        }
    }

    @Test
    public void applySortsNewestFirstAndUndatedLast() {
        assertEquals(Arrays.asList("c", "b", "e", "d", "a", "f"),
                ids(CustomerOrders.apply(all, CustomerOrders.TAB_ALL, "")));
        assertEquals(Arrays.asList("c", "b", "d"),
                ids(CustomerOrders.apply(all, CustomerOrders.TAB_PROCESSING, "")));
    }

    @Test
    public void searchMatchesOrderCodeAndProductNamesWithoutDiacritics() {
        assertEquals(Arrays.asList("c"), ids(CustomerOrders.apply(all, CustomerOrders.TAB_ALL, "cua bien")));
        assertEquals(Arrays.asList("b"), ids(CustomerOrders.apply(all, CustomerOrders.TAB_ALL, "hanh la")));
        assertEquals(Arrays.asList("e"), ids(CustomerOrders.apply(all, CustomerOrders.TAB_ALL, "tps05")));
        assertEquals(Arrays.asList("b"), ids(CustomerOrders.apply(all, CustomerOrders.TAB_PROCESSING, "ba chi")));
        assertTrue(CustomerOrders.apply(all, CustomerOrders.TAB_DONE, "hau").isEmpty());
    }

    @Test
    public void countsFollowTheQueryNotTheTab() {
        assertArrayEquals(new int[]{6, 3, 1, 1, 1}, CustomerOrders.countByTab(all, ""));
        assertArrayEquals(new int[]{1, 1, 0, 0, 0}, CustomerOrders.countByTab(all, "cua"));
        assertArrayEquals(new int[]{0, 0, 0, 0, 0}, CustomerOrders.countByTab(null, ""));
    }

    @Test
    public void stagesShowWhereTheOrderIs() {
        assertArrayEquals(new int[]{2, 1, 0, 0}, CustomerOrders.stageStates(OrderStatus.DA_DAT));
        assertArrayEquals(new int[]{2, 1, 0, 0}, CustomerOrders.stageStates(OrderStatus.DANG_XU_LY));
        assertArrayEquals(new int[]{2, 2, 1, 0}, CustomerOrders.stageStates(OrderStatus.DA_XAC_NHAN));
        assertArrayEquals(new int[]{2, 2, 1, 0}, CustomerOrders.stageStates(OrderStatus.DANG_GIAO));
        assertArrayEquals(new int[]{2, 2, 2, 2}, CustomerOrders.stageStates(OrderStatus.DA_GIAO));
        assertArrayEquals(new int[]{0, 0, 0, 0}, CustomerOrders.stageStates(OrderStatus.DA_HUY));
    }

    @Test
    public void statusMessageMentionsShipperAndCancelReason() {
        AdminOrder o = new AdminOrder();
        o.status = OrderStatus.DANG_GIAO;
        assertEquals("Đơn đang trên đường giao đến bạn", CustomerOrders.statusMessage(o));
        o.shipperName = "Dang Khoa";
        assertEquals("Shipper Dang Khoa đang giao đơn đến bạn", CustomerOrders.statusMessage(o));
        o.status = OrderStatus.DA_HUY;
        o.cancelledBy = "customer";
        assertEquals("Bạn đã hủy đơn này", CustomerOrders.statusMessage(o));
        o.cancelledBy = "admin";
        assertEquals("Đơn đã được cửa hàng hủy", CustomerOrders.statusMessage(o));
        o.cancelReason = "Hết hàng";
        assertEquals("Đã hủy · Hết hàng", CustomerOrders.statusMessage(o));
        assertEquals("", CustomerOrders.statusMessage(null));
    }

    @Test
    public void scheduleAndSummary() {
        AdminOrder o = new AdminOrder();
        assertEquals("", CustomerOrders.deliverySchedule(o));
        o.deliveryDate = "17/12/2025";
        assertEquals("17/12/2025", CustomerOrders.deliverySchedule(o));
        o.deliverySlot = "10:00 - 12:00";
        assertEquals("17/12/2025 · 10:00 - 12:00", CustomerOrders.deliverySchedule(o));

        assertEquals("", CustomerOrders.itemsSummary(null));
        assertEquals("Củ Sắn", CustomerOrders.itemsSummary(Arrays.asList(line("Củ Sắn", 1))));
        assertEquals("A, B", CustomerOrders.itemsSummary(Arrays.asList(line("A", 1), line("B", 2))));
        assertEquals("A, B và 2 sản phẩm khác",
                CustomerOrders.itemsSummary(Arrays.asList(line("A", 1), line("B", 1), line("C", 1), line("D", 1))));
    }

    @Test
    public void reorderReviewAndPaymentRules() {
        assertTrue(CustomerOrders.canReorder(OrderStatus.DA_GIAO));
        assertTrue(CustomerOrders.canReorder(OrderStatus.DA_HUY));
        assertFalse(CustomerOrders.canReorder(OrderStatus.DANG_GIAO));
        assertTrue(CustomerOrders.canReview(OrderStatus.DA_GIAO));
        assertFalse(CustomerOrders.canReview(OrderStatus.DA_HUY));
        assertTrue(CustomerOrders.isPrepaid("Thanh toán VNPay"));
        assertTrue(CustomerOrders.isPrepaid("Chuyển khoản ngân hàng"));
        assertFalse(CustomerOrders.isPrepaid("Thanh toán khi nhận hàng"));
        assertFalse(CustomerOrders.isPrepaid(null));
    }
}
