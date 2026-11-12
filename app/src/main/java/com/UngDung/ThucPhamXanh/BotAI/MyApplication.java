package com.UngDung.ThucPhamXanh.BotAI;


import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.google.firebase.FirebaseApp;

public class MyApplication extends Application {
    public static final String CHANNEL_ID = "food_app_channel";
    private NotificationService notificationService;

    @Override
    public void onCreate() {
        super.onCreate();

        // Khởi tạo Firebase
        FirebaseApp.initializeApp(this);

        // Tạo kênh thông báo
        createNotificationChannel();

        // Khởi tạo dịch vụ thông báo
        notificationService = new NotificationService(this);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Food App Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo từ ứng dụng đặt đồ ăn");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
