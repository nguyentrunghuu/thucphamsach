package com.UngDung.ThucPhamXanh.Helper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Xin quyền hiện thông báo (Android 13+). Trước đây POST_NOTIFICATIONS chỉ được khai báo trong Manifest mà
 * không bao giờ xin lúc chạy nên mọi thông báo của app bị hệ thống chặn.
 *
 * Gọi {@link #request} trong onCreate() của activity (launcher phải đăng ký trước khi màn hình STARTED).
 * Chỉ hỏi khi chưa được cấp; nếu người dùng từ chối thì hệ thống tự giới hạn số lần hỏi lại.
 */
public final class NotificationPermission {

    private NotificationPermission() { }

    public static void request(AppCompatActivity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityResultLauncher<String> launcher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> { /* không cần làm gì thêm */ });
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
}
