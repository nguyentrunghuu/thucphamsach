package com.UngDung.ThucPhamXanh.Helper;

/** Hằng số dùng chung toàn app. */
public final class AppConstants {

    /**
     * ID của tài khoản Admin trong hệ thống chat (Firestore "Chats" + Realtime "Chatlist").
     * Trước đây bị ghi cứng thành 3 giá trị khác nhau ở nhiều nơi -> tin nhắn không tới đúng admin.
     * MỌI nơi cần ID admin phải dùng hằng số này.
     */
    public static final String ADMIN_ID = "0Uq5FBdsROECM8irQ2Uq";

    private AppConstants() { }
}
