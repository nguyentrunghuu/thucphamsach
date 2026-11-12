package com.UngDung.ThucPhamXanh.BotAI;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.UngDung.ThucPhamXanh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationService {
    private static final String TAG = "NotificationService";
    private static final String CHANNEL_ID = "food_app_channel";
    private static final String CHANNEL_NAME = "Food App Notifications";
    private static final String CHANNEL_DESC = "Thông báo từ ứng dụng đặt đồ ăn";

    private Context context;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private DatabaseReference chatReference;

    // Lưu trữ các đăng ký lắng nghe để có thể hủy khi cần
    private List<ListenerRegistration> registrations = new ArrayList<>();

    // Lưu trữ ID sản phẩm đã xem để tránh thông báo lặp lại
    private List<String> seenProductIds = new ArrayList<>();

    // Lưu trữ trạng thái đơn hàng đã biết
    private Map<String, Long> knownOrderStatuses = new HashMap<>();

    public NotificationService(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
        this.chatReference = FirebaseDatabase.getInstance().getReference();

        // Tạo kênh thông báo (yêu cầu cho Android 8.0+)
        createNotificationChannel();
    }

    // Bắt đầu lắng nghe các thay đổi
    public void startListening() {
        if (currentUser == null) return;

        // Lắng nghe thay đổi trạng thái đơn hàng
        listenForOrderStatusChanges();

        // Lắng nghe sản phẩm mới
        listenForNewProducts();
    }

    // Ngừng lắng nghe các thay đổi
    public void stopListening() {
        for (ListenerRegistration registration : registrations) {
            registration.remove();
        }
        registrations.clear();
    }

    // Lắng nghe thay đổi trạng thái đơn hàng
    private void listenForOrderStatusChanges() {
        ListenerRegistration registration = firestore.collection("HoaDon")
                .whereEqualTo("UID", currentUser.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Lỗi khi lắng nghe thay đổi đơn hàng", e);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    // Không cần thông báo khi đơn hàng mới được thêm vào
                                    // vì người dùng đã biết thông qua màn hình xác nhận đặt hàng
                                    String orderId = dc.getDocument().getId();
                                    Long status = dc.getDocument().getLong("trangthai");
                                    if (status != null) {
                                        knownOrderStatuses.put(orderId, status);
                                    }
                                    break;
                                case MODIFIED:
                                    // Kiểm tra nếu trạng thái đơn hàng thay đổi
                                    String id = dc.getDocument().getId();
                                    Long newStatus = dc.getDocument().getLong("trangthai");

                                    if (newStatus != null && knownOrderStatuses.containsKey(id) &&
                                            !knownOrderStatuses.get(id).equals(newStatus)) {

                                        // Trạng thái đã thay đổi
                                        String orderInfo = "Mã đơn: " + id.substring(0, 8);
                                        String statusText = getOrderStatusText(newStatus);

                                        // Gửi thông báo
                                        sendNotification(
                                                "Cập nhật đơn hàng",
                                                "Đơn hàng " + orderInfo + " đã " + statusText,
                                                null
                                        );

                                        // Gửi tin nhắn tới chatbot
                                        sendChatbotMessage(
                                                "Thông báo cập nhật đơn hàng: " + orderInfo + " đã " + statusText
                                        );

                                        // Cập nhật trạng thái đã biết
                                        knownOrderStatuses.put(id, newStatus);
                                    }
                                    break;
                            }
                        }
                    }
                });

        registrations.add(registration);
    }

    // Lắng nghe sản phẩm mới
    private void listenForNewProducts() {
        // Lắng nghe sản phẩm mới được thêm vào
        ListenerRegistration registration = firestore.collection("SanPham")
                .orderBy("ngaythem", Query.Direction.DESCENDING) // Giả sử có trường ngày thêm
                .limit(10) // Chỉ quan tâm đến 10 sản phẩm mới nhất
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Lỗi khi lắng nghe sản phẩm mới", e);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                String productId = dc.getDocument().getId();

                                // Kiểm tra xem sản phẩm đã được thông báo chưa
                                if (!seenProductIds.contains(productId)) {
                                    String productName = dc.getDocument().getString("tensp");

                                    if (productName != null) {
                                        // Gửi thông báo
                                        sendNotification(
                                                "Sản phẩm mới",
                                                "Sản phẩm mới: " + productName,null
                                        );

                                        // Gửi tin nhắn tới chatbot
                                        sendChatbotMessage(
                                                "Có sản phẩm mới: " + productName + ". Bạn có muốn xem thông tin chi tiết không?"
                                        );

                                        // Thêm vào danh sách đã xem
                                        seenProductIds.add(productId);

                                        // Giới hạn danh sách để tránh dùng quá nhiều bộ nhớ
                                        if (seenProductIds.size() > 100) {
                                            seenProductIds.remove(0);
                                        }
                                    }
                                }
                            }
                        }
                    }
                });

        registrations.add(registration);
    }

    // Gửi thông báo
    private void sendNotification(String title, String messageBody, PendingIntent pendingIntent) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setLargeIcon(largeIcon)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
        }
    }

    // Gửi tin nhắn đến chatbot
    private void sendChatbotMessage(String message) {
        if (currentUser == null) return;

        // Lưu tin nhắn vào Firebase Realtime Database
        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> messageData = new HashMap<>();
        messageData.put("message", message);
        messageData.put("sender", "bot");
        messageData.put("timestamp", timestamp);

        chatReference.child("BotChats")
                .child(currentUser.getUid())
                .child(timestamp)
                .setValue(messageData)
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi gửi tin nhắn chatbot", e));
    }

    // Tạo kênh thông báo (bắt buộc từ Android 8.0)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // Tạo Intent cho chatbot
    private PendingIntent createChatbotIntent() {
        Intent intent = new Intent(context, ChatBotActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                flags
        );
    }

    // Chuyển đổi mã trạng thái thành văn bản
    private String getOrderStatusText(long status) {
        return com.UngDung.ThucPhamXanh.Models.OrderStatus.notifyPhrase(status);
    }
}