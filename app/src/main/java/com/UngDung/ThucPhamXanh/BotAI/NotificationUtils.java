package com.UngDung.ThucPhamXanh.BotAI;


import android.content.Context;
import android.content.Intent;

import com.UngDung.ThucPhamXanh.Models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

/**
 * Lớp tiện ích để gửi thông báo đến chatbot và broadcast receiver
 */
public class NotificationUtils {

    /**
     * Gửi thông báo khi đặt hàng thành công
     *
     * @param context Context của activity
     * @param orderId ID đơn hàng
     * @param orderInfo Thông tin đơn hàng
     */
    public static void sendOrderSuccessNotification(Context context, String orderId, String orderInfo) {
        // Gửi thông báo đến chatbot
        sendChatbotMessage("Cảm ơn bạn đã đặt hàng! Đơn hàng #" + orderId.substring(0, 8) + " đã được xác nhận.\n\n" + orderInfo);

        // Gửi broadcast
        Intent intent = new Intent(context, OrderStatusReceiver.class);
        intent.setAction(OrderStatusReceiver.ACTION_ORDER_PLACED);
        intent.putExtra(OrderStatusReceiver.EXTRA_ORDER_ID, orderId);
        intent.putExtra(OrderStatusReceiver.EXTRA_MESSAGE, orderInfo);
        context.sendBroadcast(intent);
    }

    /**
     * Gửi thông báo khi có sản phẩm mới
     *
     * @param context Context của activity
     * @param product Sản phẩm mới
     */
    public static void sendNewProductNotification(Context context, Product product) {
        if (product == null || product.getTensp() == null) return;

        // Gửi thông báo đến chatbot
        String message = "Có sản phẩm mới: " + product.getTensp() +
                "\nGiá: " + product.getGiatien() + " VND" +
                "\nLoại: " + product.getLoaisp();

        sendChatbotMessage(message);

        // Gửi broadcast
        Intent intent = new Intent(context, OrderStatusReceiver.class);
        intent.setAction(OrderStatusReceiver.ACTION_NEW_PRODUCT);
        intent.putExtra(OrderStatusReceiver.EXTRA_PRODUCT_ID, product.getId());
        intent.putExtra(OrderStatusReceiver.EXTRA_PRODUCT_NAME, product.getTensp());
        context.sendBroadcast(intent);
    }

    /**
     * Gửi thông báo khi trạng thái đơn hàng thay đổi
     *
     * @param context Context của activity
     * @param orderId ID đơn hàng
     * @param status Trạng thái mới
     */
    public static void sendOrderStatusChangeNotification(Context context, String orderId, int status) {
        // Lấy văn bản trạng thái
        String statusText = getStatusText(status);

        // Gửi thông báo đến chatbot
        sendChatbotMessage("Trạng thái đơn hàng #" + orderId.substring(0, 8) + " đã thay đổi: " + statusText);

        // Gửi broadcast
        Intent intent = new Intent(context, OrderStatusReceiver.class);
        intent.setAction(OrderStatusReceiver.ACTION_ORDER_UPDATED);
        intent.putExtra(OrderStatusReceiver.EXTRA_ORDER_ID, orderId);
        intent.putExtra(OrderStatusReceiver.EXTRA_ORDER_STATUS, status);
        context.sendBroadcast(intent);
    }

    /**
     * Gửi tin nhắn đến chatbot
     *
     * @param message Nội dung tin nhắn
     */
    private static void sendChatbotMessage(String message) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference chatReference = FirebaseDatabase.getInstance().getReference();
        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> messageData = new HashMap<>();
        messageData.put("message", message);
        messageData.put("sender", "bot");
        messageData.put("timestamp", timestamp);

        chatReference.child("BotChats")
                .child(currentUser.getUid())
                .child(timestamp)
                .setValue(messageData);
    }

    /**
     * Chuyển đổi mã trạng thái thành văn bản
     *
     * @param status Mã trạng thái
     * @return Văn bản trạng thái
     */
    private static String getStatusText(int status) {
        return com.UngDung.ThucPhamXanh.Models.OrderStatus.label(status);
    }
}