package com.UngDung.ThucPhamXanh.BotAI;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

/**
 * BroadcastReceiver để xử lý các thông báo về đơn hàng từ các dịch vụ FCM hoặc local
 */
public class OrderStatusReceiver extends BroadcastReceiver {
    private static final String TAG = "OrderStatusReceiver";

    // Intent action để xác định loại thông báo
    public static final String ACTION_ORDER_UPDATED = "com.example.doanfood.ORDER_UPDATED";
    public static final String ACTION_NEW_PRODUCT = "com.example.doanfood.NEW_PRODUCT";
    public static final String ACTION_ORDER_PLACED = "com.example.doanfood.ORDER_PLACED";

    // Extra keys để truyền dữ liệu
    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_ORDER_STATUS = "order_status";
    public static final String EXTRA_PRODUCT_ID = "product_id";
    public static final String EXTRA_PRODUCT_NAME = "product_name";
    public static final String EXTRA_MESSAGE = "message";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference chatReference = FirebaseDatabase.getInstance().getReference();

        switch (action) {
            case ACTION_ORDER_UPDATED:
                String orderId = intent.getStringExtra(EXTRA_ORDER_ID);
                int status = intent.getIntExtra(EXTRA_ORDER_STATUS, -1);

                if (orderId != null && status != -1) {
                    String statusText = getStatusText(status);
                    String message = "Trạng thái đơn hàng #" + orderId + " đã cập nhật: " + statusText;
                    sendChatbotMessage(currentUser, chatReference, message);
                    Log.d(TAG, "Đã gửi thông báo cập nhật đơn hàng: " + message);
                }
                break;

            case ACTION_NEW_PRODUCT:
                String productName = intent.getStringExtra(EXTRA_PRODUCT_NAME);

                if (productName != null) {
                    String message = "Sản phẩm mới đã được thêm vào cửa hàng: " + productName;
                    sendChatbotMessage(currentUser, chatReference, message);
                    Log.d(TAG, "Đã gửi thông báo sản phẩm mới: " + message);
                }
                break;

            case ACTION_ORDER_PLACED:
                String orderMessage = intent.getStringExtra(EXTRA_MESSAGE);

                if (orderMessage != null) {
                    String message = "Đặt hàng thành công: " + orderMessage;
                    sendChatbotMessage(currentUser, chatReference, message);
                    Log.d(TAG, "Đã gửi thông báo đặt hàng thành công: " + message);
                }
                break;
        }
    }

    private void sendChatbotMessage(FirebaseUser user, DatabaseReference reference, String message) {
        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> messageData = new HashMap<>();
        messageData.put("message", message);
        messageData.put("sender", "bot");
        messageData.put("timestamp", timestamp);

        reference.child("BotChats")
                .child(user.getUid())
                .child(timestamp)
                .setValue(messageData)
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi gửi tin nhắn chatbot", e));
    }

    private String getStatusText(int status) {
        return com.UngDung.ThucPhamXanh.Models.OrderStatus.label(status);
    }
}
