package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.AdminUsers;
import com.UngDung.ThucPhamXanh.Helper.CategoryForm;
import com.UngDung.ThucPhamXanh.Helper.PromotionRules;
import com.UngDung.ThucPhamXanh.Helper.ShipperForm;
import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Test các helper thuần Java của màn quản lý admin: danh mục, shipper, khuyến mãi, người dùng. */
public class AdminManagementHelpersTest {

    // ------------------------------------------------------------------ CategoryForm

    @Test
    public void categoryNameIsCleanedAndRequired() {
        assertEquals("Rau Củ", CategoryForm.cleanName("  Rau   Củ "));
        assertEquals("", CategoryForm.cleanName(null));
        assertNotNull(CategoryForm.validateName("   ", Arrays.asList("A"), null));
        assertNotNull(CategoryForm.validateName(new String(new char[41]).replace('\0', 'a'), null, null));
        assertNull(CategoryForm.validateName("Hải sản", Arrays.asList("Rau Củ"), null));
    }

    @Test
    public void categoryDuplicatesIgnoreCaseAndDiacritics() {
        List<String> existing = Arrays.asList("Thịt heo", "Rau Củ");
        assertNotNull(CategoryForm.validateName("thịt HEO", existing, null));
        assertNotNull(CategoryForm.validateName("Thit heo", existing, null));
        assertNotNull(CategoryForm.validateName("  rau   củ ", existing, null));
        // sửa chính danh mục đó (chỉ đổi hoa/thường) thì không tính trùng
        assertNull(CategoryForm.validateName("THỊT HEO", existing, "Thịt heo"));
        // nhưng đổi sang tên của danh mục khác thì vẫn trùng
        assertNotNull(CategoryForm.validateName("Rau củ", existing, "Thịt heo"));
    }

    @Test
    public void countsDuplicateCategoryNames() {
        assertEquals(2, CategoryForm.countDuplicates(Arrays.asList("Rau Củ", "Rau cu", "Thịt gà", "thịt gà", "Hải sản")));
        assertEquals(0, CategoryForm.countDuplicates(Arrays.asList("A", "B")));
        assertEquals(0, CategoryForm.countDuplicates(null));
    }

    // ------------------------------------------------------------------ ShipperForm

    @Test
    public void phoneNumbersAreCleanedAndChecked() {
        assertEquals("0984546164", ShipperForm.cleanPhone("0984 546.164"));
        assertEquals("+84984546164", ShipperForm.cleanPhone("+84 984-546-164"));
        assertTrue(ShipperForm.isValidPhone("0984 546 164"));
        assertTrue(ShipperForm.isValidPhone("+84984546164"));
        assertFalse(ShipperForm.isValidPhone("12345"));
        assertFalse(ShipperForm.isValidPhone("abc"));
        assertFalse(ShipperForm.isValidPhone("1984546164"));
    }

    @Test
    public void newShipperNeedsEveryField() {
        ShipperForm.Errors e = ShipperForm.validate("", "", "", "", Arrays.asList("shipper1"), null);
        assertNotNull(e.username);
        assertNotNull(e.password);
        assertNotNull(e.name);
        assertNotNull(e.phone);
        assertTrue(e.any());

        ShipperForm.Errors ok = ShipperForm.validate("khoa.dang", "123456", "Đặng Khoa", "0984546164",
                Arrays.asList("shipper1"), null);
        assertFalse(ok.any());
    }

    @Test
    public void shipperUsernameRulesAndDuplicates() {
        List<String> existing = Arrays.asList("Shipper1");
        assertNotNull(ShipperForm.validate("shipper1", "123456", "A", "0984546164", existing, null).username);
        assertNotNull(ShipperForm.validate("SHIPPER1", "123456", "A", "0984546164", existing, null).username);
        assertNotNull(ShipperForm.validate("có dấu", "123456", "A", "0984546164", existing, null).username);
        assertNotNull(ShipperForm.validate("ab", "123456", "A", "0984546164", existing, null).username);
        assertNull(ShipperForm.validate("shipper2", "123456", "A", "0984546164", existing, null).username);
    }

    @Test
    public void editingShipperMayKeepThePassword() {
        // mật khẩu trống khi sửa = giữ nguyên; username không bị kiểm tra
        ShipperForm.Errors keep = ShipperForm.validate("", "", "Khoa", "0984546164", Arrays.asList("shipper1"), "shipper1");
        assertFalse(keep.any());
        // nhưng nếu nhập mật khẩu mới thì phải đủ dài
        assertNotNull(ShipperForm.validate("", "123", "Khoa", "0984546164", null, "shipper1").password);
        assertNull(ShipperForm.validate("", "654321", "Khoa", "0984546164", null, "shipper1").password);
    }

    // ------------------------------------------------------------------ PromotionRules

    @Test
    public void parsesNewAndLegacyPromotionFormats() {
        Map<String, Object> raw = new HashMap<>();
        Map<String, Object> thit = new HashMap<>();
        thit.put("minProducts", 3L);
        thit.put("discountPercent", 10L);
        raw.put("Thịt heo", thit);
        raw.put("Hải sản", 15L);                // dữ liệu cũ: chỉ có phần trăm
        Map<String, Object> bad = new HashMap<>();
        bad.put("minProducts", 0);
        bad.put("discountPercent", 20);
        raw.put("Lỗi", bad);                    // minProducts 0 → bỏ
        raw.put("Rau", "abc");                  // sai kiểu → bỏ

        Map<String, PromotionRules.Rule> rules = PromotionRules.parse(raw);
        assertEquals(2, rules.size());
        assertEquals(new PromotionRules.Rule(3, 10), rules.get("Thịt heo"));
        assertEquals(new PromotionRules.Rule(1, 15), rules.get("Hải sản"));
        assertTrue(PromotionRules.parse(null).isEmpty());
        assertTrue(PromotionRules.parse("x").isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void writesBackOnlyActiveRules() {
        Map<String, PromotionRules.Rule> rules = new HashMap<>();
        rules.put("A", new PromotionRules.Rule(2, 5));
        rules.put("B", new PromotionRules.Rule(0, 5));
        rules.put("C", null);
        Map<String, Object> out = PromotionRules.toFirestore(rules);
        assertEquals(1, out.size());
        Map<String, Object> a = (Map<String, Object>) out.get("A");
        assertEquals(2, a.get("minProducts"));
        assertEquals(5, a.get("discountPercent"));
        assertTrue(PromotionRules.toFirestore(null).isEmpty());
    }

    @Test
    public void validatesPromotionInputs() {
        assertFalse(PromotionRules.hasError(PromotionRules.validate("3", "10")));
        assertTrue(PromotionRules.validate("", "10")[0] != null);
        assertTrue(PromotionRules.validate("0", "10")[0] != null);
        assertTrue(PromotionRules.validate("100", "10")[0] != null);
        assertTrue(PromotionRules.validate("3", "0")[1] != null);
        assertTrue(PromotionRules.validate("3", "101")[1] != null);
        assertTrue(PromotionRules.validate("3", "abc")[1] != null);
        assertFalse(PromotionRules.hasError(PromotionRules.validate(" 3 ", " 100 ")));
    }

    @Test
    public void describesAndComparesRules() {
        assertEquals("Mua từ 3 sản phẩm → giảm 10%", PromotionRules.describe(new PromotionRules.Rule(3, 10)));
        assertEquals("Chưa có khuyến mãi", PromotionRules.describe(null));
        Map<String, PromotionRules.Rule> a = new HashMap<>();
        a.put("X", new PromotionRules.Rule(1, 5));
        Map<String, PromotionRules.Rule> b = new HashMap<>(a);
        assertTrue(PromotionRules.same(a, b));
        b.put("X", new PromotionRules.Rule(1, 6));
        assertFalse(PromotionRules.same(a, b));
        assertTrue(PromotionRules.same(null, new HashMap<>()));
    }

    // ------------------------------------------------------------------ AdminUsers

    private static AdminOrder order(String uid, long status, long total) {
        AdminOrder o = new AdminOrder();
        o.uid = uid;
        o.status = status;
        o.total = total;
        return o;
    }

    private static AdminUsers.Row row(String uid, String name, String email, String phone) {
        AdminUsers.Row r = new AdminUsers.Row();
        r.uid = uid;
        r.name = name;
        r.email = email;
        r.phone = phone;
        return r;
    }

    @Test
    public void statsCountOrdersExceptCancelledAndSpendOnlyOnDelivered() {
        List<AdminOrder> orders = Arrays.asList(
                order("u1", OrderStatus.DA_GIAO, 100000),
                order("u1", OrderStatus.DA_GIAO, 50000),
                order("u1", OrderStatus.DANG_GIAO, 70000),
                order("u1", OrderStatus.DA_HUY, 999999),
                order("u2", OrderStatus.DA_DAT, 30000),
                order("", OrderStatus.DA_GIAO, 1));
        Map<String, AdminUsers.Stat> stats = AdminUsers.statsByUser(orders);
        assertEquals(3, stats.get("u1").orders);
        assertEquals(2, stats.get("u1").delivered);
        assertEquals(150000, stats.get("u1").spent);
        assertEquals(1, stats.get("u2").orders);
        assertEquals(0, stats.get("u2").spent);
        assertEquals(2, stats.size());
    }

    @Test
    public void displayNameFallsBackToEmailThenGeneric() {
        assertEquals("Hữu", row("a", " Hữu ", "x@y.z", "").displayName());
        assertEquals("nthuu", row("a", "", "nthuu@gmail.com", "").displayName());
        assertEquals("Khách hàng", row("a", "", "", "").displayName());
    }

    @Test
    public void usersAreSearchedWithoutDiacriticsAndByPhoneDigits() {
        List<AdminUsers.Row> all = new ArrayList<>(Arrays.asList(
                row("1", "Nguyễn Trung Hữu", "huu@gmail.com", "0923232340"),
                row("2", "Trần Thị Bình", "binh@gmail.com", "0987 000 111"),
                row("3", "", "an@gmail.com", "")));
        assertEquals(1, AdminUsers.filterAndSort(all, "trung huu", AdminUsers.SORT_NAME).size());
        assertEquals("2", AdminUsers.filterAndSort(all, "BINH@", AdminUsers.SORT_NAME).get(0).uid);
        assertEquals("2", AdminUsers.filterAndSort(all, "0987000", AdminUsers.SORT_NAME).get(0).uid);
        assertEquals("3", AdminUsers.filterAndSort(all, "an@gmail", AdminUsers.SORT_NAME).get(0).uid);
        assertEquals(3, AdminUsers.filterAndSort(all, "", AdminUsers.SORT_NAME).size());
        assertTrue(AdminUsers.filterAndSort(all, "zzz", AdminUsers.SORT_NAME).isEmpty());
        // gõ mã đơn có chữ lẫn số không bị coi là số điện thoại
        assertTrue(AdminUsers.filterAndSort(all, "r0HNTr", AdminUsers.SORT_NAME).isEmpty());
    }

    @Test
    public void usersCanBeSortedBySpendingOrOrders() {
        AdminUsers.Row a = row("a", "An", "", "");
        AdminUsers.Row b = row("b", "Bình", "", "");
        AdminUsers.Row c = row("c", "Cường", "", "");
        a.spent = 100; a.orders = 5;
        b.spent = 900; b.orders = 1;
        c.spent = 500; c.orders = 9;
        List<AdminUsers.Row> all = Arrays.asList(a, b, c);
        assertEquals("b", AdminUsers.filterAndSort(all, "", AdminUsers.SORT_SPENT).get(0).uid);
        assertEquals("c", AdminUsers.filterAndSort(all, "", AdminUsers.SORT_ORDERS).get(0).uid);
        assertEquals("a", AdminUsers.filterAndSort(all, "", AdminUsers.SORT_NAME).get(0).uid);
    }
}
