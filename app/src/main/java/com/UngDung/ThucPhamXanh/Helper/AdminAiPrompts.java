package com.UngDung.ThucPhamXanh.Helper;

import com.UngDung.ThucPhamXanh.Models.OrderStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Dựng prompt cho các tính năng AI của admin, tính thuần để unit test được.
 *
 * Quyền riêng tư: dữ liệu gửi cho AI KHÔNG chứa tên, số điện thoại, địa chỉ hay UID khách hàng —
 * đơn hàng chỉ có mã đơn, ngày, trạng thái, tổng tiền. Dữ liệu cũng được rút gọn (không URL ảnh)
 * để tiết kiệm token.
 */
public final class AdminAiPrompts {

    private AdminAiPrompts() { }

    /** Số đơn / sản phẩm tối đa đưa vào ngữ cảnh hỏi đáp (đơn mới nhất trước). */
    public static final int MAX_ORDERS = 300;
    public static final int MAX_PRODUCTS = 200;

    /** Một đơn đã rút gọn cho ngữ cảnh. */
    public static final class OrderLine {
        public final String id, ngaydat;
        public final Long status;
        public final long total;

        public OrderLine(String id, String ngaydat, Long status, long total) {
            this.id = id;
            this.ngaydat = ngaydat;
            this.status = status;
            this.total = total;
        }
    }

    /** Một sản phẩm đã rút gọn cho ngữ cảnh. */
    public static final class ProductLine {
        public final String name, category, expiry;
        public final long price, stock, ordered, delivered;

        public ProductLine(String name, String category, long price, long stock, String expiry,
                           long ordered, long delivered) {
            this.name = name;
            this.category = category;
            this.price = price;
            this.stock = stock;
            this.expiry = expiry;
            this.ordered = ordered;
            this.delivered = delivered;
        }
    }

    // ------------------------------------------------------------------ trợ lý hỏi đáp

    public static String assistantSystemPrompt(LocalDate today, List<OrderLine> orders, List<ProductLine> products) {
        return assistantSystemPrompt(today, orders, products, "");
    }

    /** extraNote: ghi chú thêm cuối prompt (vd. không đọc được chi tiết một số đơn); rỗng thì bỏ qua. */
    public static String assistantSystemPrompt(LocalDate today, List<OrderLine> orders, List<ProductLine> products,
                                               String extraNote) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là trợ lý phân tích cho quản trị viên cửa hàng thực phẩm sạch \"Thực Phẩm Xanh\".\n")
                .append("Hôm nay là ").append(DashboardStats.formatLongDate(today)).append(".\n")
                .append("Chỉ trả lời dựa trên dữ liệu bên dưới; thiếu dữ liệu thì nói rõ là không có, tuyệt đối không bịa số.\n")
                .append("Quy ước: doanh thu chỉ tính đơn \"Đã giao\", theo ngày đặt. Tiền là đồng (VNĐ), viết dạng 1.250.000 đ.\n")
                .append("Mỗi sản phẩm có hai số lượng bán cộng dồn từ trước đến nay (tính từ chi tiết các đơn): \"đã đặt\" = tổng số lượng ")
                .append("trong mọi đơn trừ đơn đã hủy, \"đã giao\" = chỉ trong đơn đã giao. Khi được hỏi sản phẩm bán chạy mà không nói ")
                .append("rõ thì xếp theo \"đã đặt\" và nêu thêm số \"đã giao\". Số này không chia theo tuần/tháng nên nếu hỏi theo kỳ thì nói rõ ")
                .append("là chỉ có số cộng dồn toàn thời gian. Đơn nào không đọc được chi tiết (nếu có ghi chú bên dưới) thì số lượng chỉ là ước tính thấp.\n")
                .append("Trả lời tiếng Việt, ngắn gọn, dùng gạch đầu dòng khi liệt kê, không dùng bảng markdown hay in đậm bằng dấu *.\n\n");

        sb.append("[ĐƠN HÀNG] mã | ngày đặt | trạng thái | tổng tiền\n");
        int n = Math.min(orders.size(), MAX_ORDERS);
        for (int i = 0; i < n; i++) {
            OrderLine o = orders.get(i);
            sb.append(o.id).append(" | ").append(o.ngaydat).append(" | ")
                    .append(o.status == null ? "?" : OrderStatus.label(o.status)).append(" | ")
                    .append(MoneyUtil.formatNumber(o.total)).append('\n');
        }
        if (orders.size() > n) sb.append("(còn ").append(orders.size() - n).append(" đơn cũ hơn không liệt kê)\n");

        sb.append("\n[SẢN PHẨM] tên | loại | giá | tồn kho | hạn dùng | đã đặt (trừ đơn hủy) | đã giao\n");
        int m = Math.min(products.size(), MAX_PRODUCTS);
        for (int i = 0; i < m; i++) {
            ProductLine p = products.get(i);
            sb.append(p.name).append(" | ").append(p.category).append(" | ")
                    .append(MoneyUtil.formatNumber(p.price)).append(" | ").append(p.stock).append(" | ")
                    .append(p.expiry == null || p.expiry.isEmpty() ? "-" : p.expiry).append(" | ")
                    .append(p.ordered).append(" | ").append(p.delivered).append('\n');
        }
        if (products.size() > m) sb.append("(còn ").append(products.size() - m).append(" sản phẩm không liệt kê)\n");
        if (extraNote != null && !extraNote.isEmpty()) sb.append("\n[GHI CHÚ] ").append(extraNote).append('\n');
        return sb.toString();
    }

    // ------------------------------------------------------------------ nhận xét thống kê

    public static String insightSystemPrompt() {
        return "Bạn là chuyên viên phân tích kinh doanh cho cửa hàng thực phẩm sạch. "
                + "Từ số liệu được gửi, viết nhận xét cho chủ cửa hàng bằng tiếng Việt, đúng 3 đến 4 gạch đầu dòng ngắn "
                + "(mỗi dòng bắt đầu bằng \"- \"): xu hướng doanh thu, điểm đáng chú ý (đơn tồn đọng, tỉ lệ hủy, ngày/tháng nổi bật), "
                + "và 1 đề xuất hành động cụ thể. Chỉ dùng số liệu đã cho, không bịa. Không dùng markdown, không mở đầu hay kết thúc dài dòng.";
    }

    public static String insightUserMessage(RevenueReport r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kỳ: ").append(date(r.getRange().from)).append(" đến ").append(date(r.getRange().to))
                .append(" (").append(r.getRange().days()).append(" ngày)\n")
                .append("Doanh thu (đơn đã giao): ").append(MoneyUtil.formatVnd(r.getRevenue())).append('\n')
                .append("Kỳ trước cùng độ dài: ").append(MoneyUtil.formatVnd(r.getPreviousRevenue())).append('\n')
                .append("Đơn đã giao: ").append(r.getDeliveredOrders())
                .append(", trung bình ").append(MoneyUtil.formatVnd(r.getAverageOrder())).append("/đơn\n")
                .append("Tổng đơn đặt trong kỳ: ").append(r.getTotalOrders()).append(" — chờ xác nhận ")
                .append(r.getOrderCount(OrderStatus.DA_DAT) + r.getOrderCount(OrderStatus.DANG_XU_LY))
                .append(", đã xác nhận ").append(r.getOrderCount(OrderStatus.DA_XAC_NHAN))
                .append(", đang giao ").append(r.getOrderCount(OrderStatus.DANG_GIAO))
                .append(", đã giao ").append(r.getOrderCount(OrderStatus.DA_GIAO))
                .append(", đã hủy ").append(r.getOrderCount(OrderStatus.DA_HUY)).append('\n')
                .append("Đơn chưa giao xong (chưa tính doanh thu): ").append(r.getUnfinishedOrders())
                .append(" đơn, trị giá ").append(MoneyUtil.formatVnd(r.getUnfinishedValue())).append('\n')
                .append("Doanh thu theo ").append(r.isMonthly() ? "tháng" : "ngày").append(":\n");
        for (int i = 0; i < r.getBucketCount(); i++) {
            sb.append(r.getBucketLabel(i)).append(": ").append(MoneyUtil.formatNumber(r.getBucketRevenue(i))).append('\n');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ mô tả sản phẩm

    public static String descriptionSystemPrompt() {
        return "Bạn viết mô tả sản phẩm cho cửa hàng thực phẩm sạch \"Thực Phẩm Xanh\". "
                + "Viết bằng tiếng Việt, 2 đến 3 câu (tối đa khoảng 60 từ), giọng thân thiện, nêu đặc điểm, cách dùng hoặc lợi ích dinh dưỡng thông thường. "
                + "Không bịa chứng nhận, giải thưởng, số liệu y khoa hay cam kết chữa bệnh; không nhắc giá. "
                + "Chỉ trả về đoạn mô tả, không tiêu đề, không dấu ngoặc kép, không markdown.";
    }

    public static String descriptionUserMessage(String name, String category, String weight, String origin) {
        StringBuilder sb = new StringBuilder("Viết mô tả cho sản phẩm: ").append(clean(name));
        if (!clean(category).isEmpty()) sb.append("\nLoại: ").append(clean(category));
        if (!clean(weight).isEmpty()) sb.append("\nQuy cách: ").append(clean(weight));
        if (!clean(origin).isEmpty()) sb.append("\nXuất xứ: ").append(clean(origin));
        return sb.toString();
    }

    /** Làm sạch câu trả lời AI để đưa vào ô mô tả: bỏ markdown / dấu ngoặc kép bao ngoài, gộp xuống dòng thừa. */
    public static String cleanDescription(String raw) {
        if (raw == null) return "";
        String s = raw.replace("**", "").replace("__", "").replace("`", "").trim();
        s = s.replaceAll("^#+\\s*", "");
        s = s.replaceAll("\\n{3,}", "\n\n");
        if (s.length() >= 2) {
            char a = s.charAt(0), b = s.charAt(s.length() - 1);
            if ((a == '"' && b == '"') || (a == '“' && b == '”')) s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    /** Bỏ dấu * và # kiểu markdown khỏi câu trả lời hiển thị trong TextView thường. */
    public static String stripMarkdown(String raw) {
        if (raw == null) return "";
        return raw.replace("**", "").replaceAll("(?m)^\\s*\\*\\s+", "- ").replaceAll("(?m)^#+\\s*", "").trim();
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private static String date(LocalDate d) {
        return String.format(Locale.ROOT, "%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }
}
