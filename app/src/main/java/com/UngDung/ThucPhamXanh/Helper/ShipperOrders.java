package com.UngDung.ThucPhamXanh.Helper;

import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Logic hiển thị cho màn shipper, tính thuần (không đụng Android/Firebase) để unit test được.
 *
 * Ba tab:  NEW    = đơn "Đã xác nhận" chưa có shipper (ai cũng nhận được)
 *          (NEW gồm cả đơn "Đang giao" admin đã chuyển nhưng chưa gán shipper)
 *          ACTIVE = đơn "Đang giao" của chính shipper này
 *          DONE   = đơn "Đã giao" của chính shipper này (mới nhất trước)
 * Mọi mã trạng thái lấy từ {@link OrderStatus}.
 */
public final class ShipperOrders {

    public static final int TAB_NEW = 0, TAB_ACTIVE = 1, TAB_DONE = 2;

    /** Tab "Đã giao" chỉ hiện chừng này đơn gần nhất. */
    public static final int DONE_LIMIT = 30;

    private ShipperOrders() { }

    // ------------------------------------------------------------------ phân loại

    /**
     * Đơn nhận được: "Đã xác nhận", hoặc "Đang giao" mà admin chưa chọn shipper (admin đã bấm giao nhưng chưa gán ai;
     * nếu không cho nhận, đơn đó sẽ không hiện ở shipper nào).
     */
    private static boolean claimableStatus(long status) {
        return status == OrderStatus.DA_XAC_NHAN || status == OrderStatus.DANG_GIAO;
    }

    /** Đơn admin đã chuyển "Đang giao" nhưng chưa có shipper — đang chờ shipper nhận. */
    public static boolean isWaitingForShipper(AdminOrder o) {
        return o.status == OrderStatus.DANG_GIAO && !hasShipper(o);
    }

    public static boolean hasShipper(AdminOrder o) {
        return o.shipperId != null && !o.shipperId.trim().isEmpty();
    }

    private static boolean isMine(AdminOrder o, String me) {
        return hasShipper(o) && o.shipperId.trim().equals(me);
    }

    public static boolean belongsTo(AdminOrder o, int tab, String me) {
        switch (tab) {
            case TAB_NEW:    return claimableStatus(o.status) && !hasShipper(o);
            case TAB_ACTIVE: return o.status == OrderStatus.DANG_GIAO && isMine(o, me);
            case TAB_DONE:   return o.status == OrderStatus.DA_GIAO && isMine(o, me);
            default:         return false;
        }
    }

    /** Lọc và sắp xếp cho một tab: đơn mới / đang giao theo thứ tự đặt (cũ trước), đã giao thì mới nhất trước. */
    public static List<AdminOrder> forTab(List<AdminOrder> all, int tab, String me) {
        List<AdminOrder> out = new ArrayList<>();
        for (AdminOrder o : all) {
            if (belongsTo(o, tab, me)) out.add(o);
        }
        final boolean newestFirst = tab == TAB_DONE;
        Collections.sort(out, new Comparator<AdminOrder>() {
            @Override
            public int compare(AdminOrder a, AdminOrder b) {
                int c = Long.compare(DashboardStats.sortKey(a.date), DashboardStats.sortKey(b.date));
                if (c == 0) c = a.id.compareTo(b.id); // ổn định khi cùng ngày
                return newestFirst ? -c : c;
            }
        });
        if (tab == TAB_DONE && out.size() > DONE_LIMIT) return new ArrayList<>(out.subList(0, DONE_LIMIT));
        return out;
    }

    public static int count(List<AdminOrder> all, int tab, String me) {
        return count(all, tab, me, "");
    }

    // ------------------------------------------------------------------ tìm kiếm

    /**
     * Đơn có khớp từ khóa không (không phân biệt hoa thường / dấu): tên, mã đơn, id, địa chỉ,
     * hoặc số điện thoại (so theo chữ số nên "0349 132.622" hay "2622" đều khớp). Từ khóa rỗng → khớp hết.
     */
    public static boolean matches(AdminOrder o, String query) {
        String q = TextNormalizer.normalize(query);
        if (q.isEmpty()) return true;
        if (TextNormalizer.normalize(o.name).contains(q)
                || TextNormalizer.normalize(o.code).contains(q)
                || TextNormalizer.normalize(o.id).contains(q)
                || TextNormalizer.normalize(o.address).contains(q)) {
            return true;
        }
        // Chỉ so theo số điện thoại khi từ khóa LÀ số (cho phép dấu cách . - ( ) +). Nếu không, mã đơn như "r0HNTr"
        // hay "TPS0512" bị rút còn "0" / "0512" rồi khớp nhầm hàng loạt số điện thoại.
        if (!query.trim().matches("[0-9+\\s.\\-()]+")) return false;
        String digits = dialNumber(query);
        return !digits.isEmpty() && dialNumber(o.phone).contains(digits);
    }

    /** Như {@link #forTab(List, int, String)} nhưng chỉ giữ đơn khớp từ khóa. */
    public static List<AdminOrder> forTab(List<AdminOrder> all, int tab, String me, String query) {
        List<AdminOrder> matching = new ArrayList<>();
        for (AdminOrder o : all) if (matches(o, query)) matching.add(o);
        return forTab(matching, tab, me);
    }

    /** Số đơn của tab (chỉ tính đơn khớp từ khóa) — để ghi lên chip. */
    public static int count(List<AdminOrder> all, int tab, String me, String query) {
        int n = 0;
        for (AdminOrder o : all) if (belongsTo(o, tab, me) && matches(o, query)) n++;
        return n;
    }

    // ------------------------------------------------------------------ hành động được phép (theo dữ liệu mới nhất)

    /** Có thể nhận đơn này không (đơn đã xác nhận, chưa ai nhận). */
    public static boolean canClaim(AdminOrder o) {
        return claimableStatus(o.status) && !hasShipper(o);
    }

    /** Có thể báo đã giao không (đơn đang giao và là của mình). */
    public static boolean canComplete(AdminOrder o, String me) {
        return o.status == OrderStatus.DANG_GIAO && isMine(o, me);
    }

    /** Dòng giải thích khi shipper không còn thao tác được với đơn; rỗng nếu còn thao tác. */
    public static String lockedReason(AdminOrder o, String me) {
        if (canClaim(o) || canComplete(o, me)) return "";
        if (o.status == OrderStatus.DA_HUY) return "Đơn đã bị hủy";
        if (o.status == OrderStatus.DA_GIAO) return "Đơn đã giao xong";
        if (hasShipper(o) && !isMine(o, me)) {
            return "Đơn đã được " + (o.shipperName.isEmpty() ? "shipper khác" : o.shipperName) + " nhận";
        }
        return "Đơn chưa sẵn sàng để giao (" + OrderStatus.label(o.status) + ")";
    }

    // ------------------------------------------------------------------ thanh toán

    /**
     * Đơn thu tiền mặt khi giao (COD). Chuỗi lạ / rỗng coi như COD cho an toàn
     * (shipper thu rồi báo lại sai còn hơn bỏ sót).
     */
    public static boolean isCod(String payment) {
        String p = TextNormalizer.normalize(payment);
        return !(p.contains("vnpay") || p.contains("chuyen khoan") || p.contains("momo"));
    }

    /** "Thu hộ 370.000 đ" cho COD, "Đã thanh toán" cho đơn trả trước. */
    public static String paymentLabel(AdminOrder o) {
        return isCod(o.payment) ? "Thu hộ " + MoneyUtil.formatVnd(o.total) : "Đã thanh toán";
    }

    // ------------------------------------------------------------------ giao hàng

    /** Khung giờ giao để hiển thị, vd. "15/11/2026 · 9h–11h"; rỗng nếu đơn không có. */
    public static String deliverySlot(AdminOrder o) {
        String d = o.deliveryDate == null ? "" : o.deliveryDate.trim();
        String s = o.deliverySlot == null ? "" : o.deliverySlot.trim();
        if (d.isEmpty()) return s;
        return s.isEmpty() ? d : d + " · " + s;
    }

    /** Địa chỉ → đường dẫn mở bản đồ (geo: tìm theo chữ). */
    public static String mapsUri(String address) {
        String a = address == null ? "" : address.trim();
        try {
            return "geo:0,0?q=" + URLEncoder.encode(a, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "geo:0,0?q=";
        }
    }

    /** Số điện thoại → chỉ giữ chữ số và dấu +, để gọi được dù khách nhập có dấu cách / chấm. */
    public static String dialNumber(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9+]", "");
    }
}
