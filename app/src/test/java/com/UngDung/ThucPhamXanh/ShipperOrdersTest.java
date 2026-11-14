package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.DashboardStats;
import com.UngDung.ThucPhamXanh.Helper.ShipperOrders;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ShipperOrdersTest {

    private static AdminOrder o(String id, long status, String shipper, String date) {
        AdminOrder x = new AdminOrder();
        x.id = id;
        x.status = status;
        x.shipperId = shipper;
        x.date = date;
        x.total = 370000;
        x.payment = "Thanh toán khi nhận hàng";
        return x;
    }

    @Test
    public void tabsClassifyByStatusAndOwner() {
        AdminOrder fresh = o("a", 5, "", "10/11/2026");
        AdminOrder mineActive = o("b", 4, "shipper1", "10/11/2026");
        AdminOrder otherActive = o("c", 4, "shipper2", "10/11/2026");
        AdminOrder orphanActive = o("d", 4, "", "10/11/2026");   // admin đã chuyển Đang giao nhưng chưa gán shipper
        AdminOrder mineDone = o("e", 3, "shipper1", "10/11/2026");
        AdminOrder claimedConfirmed = o("f", 5, "shipper2", "10/11/2026");

        assertTrue(ShipperOrders.belongsTo(fresh, ShipperOrders.TAB_NEW, "shipper1"));
        assertFalse(ShipperOrders.belongsTo(claimedConfirmed, ShipperOrders.TAB_NEW, "shipper1"));
        assertTrue(ShipperOrders.belongsTo(mineActive, ShipperOrders.TAB_ACTIVE, "shipper1"));
        assertFalse(ShipperOrders.belongsTo(otherActive, ShipperOrders.TAB_ACTIVE, "shipper1"));
        assertFalse(ShipperOrders.belongsTo(orphanActive, ShipperOrders.TAB_ACTIVE, "shipper1")); // chưa của ai
        assertTrue(ShipperOrders.belongsTo(orphanActive, ShipperOrders.TAB_NEW, "shipper1"));      // nhưng hiện ở "Đơn mới" để nhận
        assertTrue(ShipperOrders.isWaitingForShipper(orphanActive));
        assertFalse(ShipperOrders.isWaitingForShipper(fresh));
        assertFalse(ShipperOrders.isWaitingForShipper(otherActive));
        assertTrue(ShipperOrders.belongsTo(mineDone, ShipperOrders.TAB_DONE, "shipper1"));
        assertFalse(ShipperOrders.belongsTo(mineDone, ShipperOrders.TAB_ACTIVE, "shipper1"));
    }

    @Test
    public void forTabSortsAndCaps() {
        List<AdminOrder> all = new ArrayList<>();
        all.add(o("new2", 5, "", "12/11/2026"));
        all.add(o("new1", 5, "", "10/11/2026"));
        List<AdminOrder> news = ShipperOrders.forTab(all, ShipperOrders.TAB_NEW, "s");
        assertEquals("new1", news.get(0).id);   // đơn cũ trước

        List<AdminOrder> done = new ArrayList<>();
        for (int i = 1; i <= ShipperOrders.DONE_LIMIT + 5; i++) {
            done.add(o("d" + i, 3, "s", String.format("%02d/10/2026", Math.min(i, 28))));
        }
        List<AdminOrder> shown = ShipperOrders.forTab(done, ShipperOrders.TAB_DONE, "s");
        assertEquals(ShipperOrders.DONE_LIMIT, shown.size());
        assertEquals(ShipperOrders.DONE_LIMIT + 5, ShipperOrders.count(done, ShipperOrders.TAB_DONE, "s"));
        assertTrue(DashboardStats.sortKey(shown.get(0).date) >= DashboardStats.sortKey(shown.get(1).date));
    }

    @Test
    public void search_matchesNameCodeAddressAndPhoneIgnoringAccents() {
        AdminOrder x = o("abc123", 5, "", "10/11/2026");
        x.name = "Nguyễn Anh";
        x.code = "TPS0512250013TM";
        x.address = "144 Nguyễn Trọng Hợp, Gò Công, Tiền Giang";
        x.phone = "0984 546.164";

        assertTrue(ShipperOrders.matches(x, ""));              // rỗng → khớp hết
        assertTrue(ShipperOrders.matches(x, "nguyen anh"));    // bỏ dấu
        assertTrue(ShipperOrders.matches(x, "TIEN GIANG"));    // không phân biệt hoa thường
        assertTrue(ShipperOrders.matches(x, "tps0512"));       // mã đơn
        assertTrue(ShipperOrders.matches(x, "abc1"));          // id tài liệu
        assertTrue(ShipperOrders.matches(x, "0984546164"));    // số đầy đủ, khách nhập có dấu cách / chấm
        assertTrue(ShipperOrders.matches(x, "546 164"));       // một phần số
        assertFalse(ShipperOrders.matches(x, "Hà Nội"));
        assertFalse(ShipperOrders.matches(x, "999"));
    }

    @Test
    public void search_codeWithDigitsDoesNotMatchPhonesByItsDigits() {
        // Lỗi thật: gõ "r0HNTr" bị rút thành "0" và khớp gần như mọi số điện thoại
        AdminOrder x = o("jDglsvb3", 5, "", "10/11/2026");
        x.name = "Nguyễn Anh";
        x.phone = "0984546164";
        assertFalse(ShipperOrders.matches(x, "r0HNTr"));
        assertFalse(ShipperOrders.matches(x, "TPS0512"));
        assertFalse(ShipperOrders.matches(x, "abc9845"));
        assertTrue(ShipperOrders.matches(x, "0984546164"));
        assertTrue(ShipperOrders.matches(x, "(098) 454-6164"));
        assertTrue(ShipperOrders.matches(x, "0984"));          // một phần đầu số
        assertFalse(ShipperOrders.matches(x, "+0984"));        // dấu + không có trong số đã lưu
    }

    @Test
    public void search_filtersListAndCountsPerTab() {
        AdminOrder a = o("a", 5, "", "10/11/2026");
        a.name = "Hữu Tình";
        AdminOrder b = o("b", 5, "", "11/11/2026");
        b.name = "Nguyễn Anh";
        AdminOrder c = o("c", 4, "me", "11/11/2026");
        c.name = "Hữu Tình";
        List<AdminOrder> all = new ArrayList<>();
        all.add(a);
        all.add(b);
        all.add(c);

        assertEquals(1, ShipperOrders.forTab(all, ShipperOrders.TAB_NEW, "me", "huu tinh").size());
        assertEquals("a", ShipperOrders.forTab(all, ShipperOrders.TAB_NEW, "me", "huu tinh").get(0).id);
        assertEquals(1, ShipperOrders.count(all, ShipperOrders.TAB_NEW, "me", "huu tinh"));
        assertEquals(1, ShipperOrders.count(all, ShipperOrders.TAB_ACTIVE, "me", "huu tinh"));
        assertEquals(0, ShipperOrders.count(all, ShipperOrders.TAB_DONE, "me", "huu tinh"));
        assertEquals(2, ShipperOrders.count(all, ShipperOrders.TAB_NEW, "me", ""));      // không từ khóa = như cũ
        assertEquals(0, ShipperOrders.forTab(all, ShipperOrders.TAB_NEW, "me", "không có").size());
    }

    @Test
    public void actionsFollowLiveData() {
        assertTrue(ShipperOrders.canClaim(o("a", 5, "", "01/01/2026")));
        assertFalse(ShipperOrders.canClaim(o("a", 5, "shipper2", "01/01/2026")));   // vừa bị người khác nhận
        assertFalse(ShipperOrders.canClaim(o("a", 6, "", "01/01/2026")));           // vừa bị hủy
        assertTrue(ShipperOrders.canClaim(o("a", 4, "", "01/01/2026")));            // admin đã giao nhưng chưa gán shipper
        assertFalse(ShipperOrders.canClaim(o("a", 4, "shipper2", "01/01/2026")));   // đã có shipper
        assertTrue(ShipperOrders.canComplete(o("a", 4, "me", "01/01/2026"), "me"));
        assertFalse(ShipperOrders.canComplete(o("a", 4, "other", "01/01/2026"), "me"));
        assertFalse(ShipperOrders.canComplete(o("a", 3, "me", "01/01/2026"), "me"));
    }

    @Test
    public void lockedReasonExplainsWhy() {
        assertEquals("", ShipperOrders.lockedReason(o("a", 5, "", "x"), "me"));
        assertEquals("Đơn đã bị hủy", ShipperOrders.lockedReason(o("a", 6, "", "x"), "me"));
        assertEquals("Đơn đã giao xong", ShipperOrders.lockedReason(o("a", 3, "me", "x"), "me"));
        AdminOrder taken = o("a", 4, "other", "x");
        taken.shipperName = "Nguyễn Văn A";
        assertEquals("Đơn đã được Nguyễn Văn A nhận", ShipperOrders.lockedReason(taken, "me"));
        assertEquals("Đơn đã được shipper khác nhận", ShipperOrders.lockedReason(o("a", 4, "other", "x"), "me"));
    }

    @Test
    public void paymentLabel_codVersusPrepaid() {
        AdminOrder cod = o("a", 5, "", "x");
        assertEquals("Thu hộ 370.000 đ", ShipperOrders.paymentLabel(cod));
        cod.payment = "Tiền mặt khi nhận hàng (COD)";
        assertTrue(ShipperOrders.isCod(cod.payment));

        AdminOrder vnpay = o("a", 5, "", "x");
        vnpay.payment = "Thanh toán VNPay";
        assertEquals("Đã thanh toán", ShipperOrders.paymentLabel(vnpay));
        vnpay.payment = "Chuyển khoản ngân hàng";
        assertFalse(ShipperOrders.isCod(vnpay.payment));
        assertTrue(ShipperOrders.isCod("")); // không rõ → coi như COD
    }

    @Test
    public void deliverySlotMapsAndDial() {
        AdminOrder x = o("a", 5, "", "x");
        assertEquals("", ShipperOrders.deliverySlot(x));
        x.deliveryDate = "15/11/2026";
        assertEquals("15/11/2026", ShipperOrders.deliverySlot(x));
        x.deliverySlot = "9h-11h";
        assertEquals("15/11/2026 · 9h-11h", ShipperOrders.deliverySlot(x));
        assertEquals("geo:0,0?q=12+L%C3%AA+L%E1%BB%A3i%2C+H%C3%A0+N%E1%BB%99i",
                ShipperOrders.mapsUri(" 12 Lê Lợi, Hà Nội "));
        assertEquals("+84349132622", ShipperOrders.dialNumber("+84 349.132.622"));
        assertEquals("", ShipperOrders.dialNumber(null));
    }
}
