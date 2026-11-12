package com.UngDung.ThucPhamXanh.BotAI;

import com.UngDung.ThucPhamXanh.Models.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lớp quản lý quy trình đặt hàng qua chatbot
 */
public class OrderProcess {
    // Các trạng thái của quá trình đặt hàng
    public enum OrderState {
        IDLE,                // Không trong quá trình đặt hàng
        SELECTING_PRODUCTS,  // Đang chọn sản phẩm
        CONFIRMING_CART,     // Xác nhận giỏ hàng
        ENTERING_NAME,       // Nhập tên người nhận
        ENTERING_ADDRESS,    // Nhập địa chỉ
        ENTERING_PHONE,      // Nhập số điện thoại
        SELECTING_PAYMENT,   // Chọn phương thức thanh toán
        CONFIRMING_ORDER     // Xác nhận đặt hàng
    }

    private OrderState currentState = OrderState.IDLE;
    private Map<Product, Integer> cart = new HashMap<>(); // Sản phẩm và số lượng
    private String customerName;
    private String address;
    private String phone;
    private String paymentMethod;
    private String note;
    private long totalAmount = 0;

    // Khởi tạo quá trình đặt hàng mới
    public void startNewOrder(String defaultName) {
        this.customerName = defaultName; // Tên mặc định, có thể thay đổi sau
        cart.clear();
        address = null;
        phone = null;
        paymentMethod = null;
        note = null;
        totalAmount = 0;
        currentState = OrderState.SELECTING_PRODUCTS;
    }

    // Thêm sản phẩm vào giỏ hàng
    public void addProductToCart(Product product, int quantity) {
        if (cart.containsKey(product)) {
            cart.put(product, cart.get(product) + quantity);
        } else {
            cart.put(product, quantity);
        }
        updateTotalAmount();
    }

    // Cập nhật số lượng sản phẩm trong giỏ hàng
    public void updateProductQuantity(Product product, int quantity) {
        if (quantity <= 0) {
            cart.remove(product);
        } else {
            cart.put(product, quantity);
        }
        updateTotalAmount();
    }

    // Xóa sản phẩm khỏi giỏ hàng
    public void removeProductFromCart(Product product) {
        cart.remove(product);
        updateTotalAmount();
    }

    // Tính tổng tiền
    private void updateTotalAmount() {
        totalAmount = 0;
        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            totalAmount += entry.getKey().getGiatien() * entry.getValue();
        }
    }

    // Chuyển sang trạng thái tiếp theo
    public void moveToNextState() {
        switch (currentState) {
            case SELECTING_PRODUCTS:
                currentState = OrderState.CONFIRMING_CART;
                break;
            case CONFIRMING_CART:
                currentState = OrderState.ENTERING_NAME;
                break;
            case ENTERING_NAME:
                currentState = OrderState.ENTERING_ADDRESS;
                break;
            case ENTERING_ADDRESS:
                currentState = OrderState.ENTERING_PHONE;
                break;
            case ENTERING_PHONE:
                currentState = OrderState.SELECTING_PAYMENT;
                break;
            case SELECTING_PAYMENT:
                currentState = OrderState.CONFIRMING_ORDER;
                break;
            case CONFIRMING_ORDER:
                currentState = OrderState.IDLE;
                break;
            default:
                break;
        }
    }

    // Tạo đối tượng HashMap để lưu vào Firebase
    public HashMap<String, Object> createOrderMap(String userId) {
        // Tính phí ship dựa trên địa chỉ
        long shippingFee = com.UngDung.ThucPhamXanh.Helper.ShippingFeeHelper.calculateShippingFee(address);
        
        HashMap<String, Object> orderMap = new HashMap<>();
        orderMap.put("UID", userId);
        orderMap.put("hoten", customerName);
        orderMap.put("diachi", address);
        orderMap.put("sdt", phone);
        orderMap.put("phuongthuc", paymentMethod);
        orderMap.put("ghichu", note);
        orderMap.put("tongtien", String.valueOf(totalAmount + shippingFee));
        orderMap.put("phivanchuyen", shippingFee); // Lưu phí ship vào hóa đơn
        orderMap.put("trangthai", 1); // Trạng thái: Đang xử lý
        orderMap.put("ngaydat", getCurrentDateString());

        return orderMap;
    }

    // Tạo đối tượng HashMap cho chi tiết đơn hàng
    public List<HashMap<String, Object>> createOrderDetailsMap(String orderId) {
        List<HashMap<String, Object>> orderDetails = new ArrayList<>();

        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();

            HashMap<String, Object> detail = new HashMap<>();
            detail.put("id_hoadon", orderId);
            detail.put("id_product", product.getId());
            detail.put("soluong", quantity);
            detail.put("giatien", product.getGiatien());
            detail.put("tongtien", product.getGiatien() * quantity);

            orderDetails.add(detail);
        }

        return orderDetails;
    }

    // Tạo chuỗi tóm tắt giỏ hàng
    public String getCartSummary() {
        StringBuilder summary = new StringBuilder("Giỏ hàng của bạn:\n");
        int count = 1;

        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            long itemTotal = product.getGiatien() * quantity;

            summary.append(String.format("%d. %s - %d x %,d đ = %,d đ\n",
                    count++, product.getTensp(), quantity, product.getGiatien(), itemTotal));
        }

        summary.append(String.format("\nTổng cộng: %,d đ", totalAmount));
        return summary.toString();
    }

    // Tạo chuỗi tóm tắt đơn hàng hoàn chỉnh
    public String getOrderSummary() {
        // Tính phí ship
        long shippingFee = com.UngDung.ThucPhamXanh.Helper.ShippingFeeHelper.calculateShippingFee(address);
        long totalWithShipping = totalAmount + shippingFee;
        
        StringBuilder summary = new StringBuilder("Xác nhận đơn hàng:\n\n");
        summary.append(getCartSummary()).append("\n");
        summary.append("- Phí vận chuyển: ").append(String.format("%,d đ", shippingFee)).append("\n");
        summary.append("- Tổng cộng: ").append(String.format("%,d đ", totalWithShipping)).append("\n\n");
        summary.append("Thông tin giao hàng:\n");
        summary.append("- Họ tên: ").append(customerName).append("\n");
        summary.append("- Địa chỉ: ").append(address).append("\n");
        summary.append("- Số điện thoại: ").append(phone).append("\n");
        summary.append("- Phương thức thanh toán: ").append(paymentMethod).append("\n");

        if (note != null && !note.isEmpty()) {
            summary.append("- Ghi chú: ").append(note).append("\n");
        }

        return summary.toString();
    }

    // Lấy câu hỏi dựa vào trạng thái hiện tại
    public String getCurrentStateQuestion() {
        switch (currentState) {
            case SELECTING_PRODUCTS:
                return "Bạn muốn đặt sản phẩm gì? Vui lòng cho biết tên sản phẩm và số lượng.";
            case CONFIRMING_CART:
                return getCartSummary() + "\n\nBạn có muốn thêm sản phẩm nào khác không? Nếu không, hãy nhập 'Xác nhận' để tiếp tục.";
            case ENTERING_NAME:
                return "Vui lòng nhập tên người nhận hàng:";
            case ENTERING_ADDRESS:
                return "Vui lòng nhập địa chỉ giao hàng:";
            case ENTERING_PHONE:
                return "Vui lòng nhập số điện thoại liên hệ:";
            case SELECTING_PAYMENT:
                return "Vui lòng chọn phương thức thanh toán:\n1. Tiền mặt khi nhận hàng (COD)\n2. Chuyển khoản ngân hàng";
            case CONFIRMING_ORDER:
                return getOrderSummary() + "\n\nVui lòng xác nhận đơn hàng bằng cách nhập 'Đồng ý' hoặc 'Hủy' để hủy đơn hàng.";
            default:
                return "";
        }
    }

    // Getters and Setters
    public OrderState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(OrderState currentState) {
        this.currentState = currentState;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isCartEmpty() {
        return cart.isEmpty();
    }

    public Map<Product, Integer> getCart() {
        return cart;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    // Lấy ngày hiện tại dưới dạng chuỗi
    public String getCurrentDateString() {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy");
        return dateFormat.format(new java.util.Date());
    }
}