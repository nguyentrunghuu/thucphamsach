package com.UngDung.ThucPhamXanh.Helper;

import java.util.Set;

/**
 * Quy tắc báo tin nhắn mới (collection "Chats": sender, receiver, message, isseen, timestamp) — thuần Java.
 * ChatNotifier gọi các hàm này để quyết định có hiện thông báo hay không và hiện chữ gì.
 */
public final class ChatNotificationRules {

    public static final int MAX_BODY = 120;

    private ChatNotificationRules() { }

    /**
     * Một tin có nên báo không: gửi tới mình, chưa đọc, chưa báo lần nào, không do chính mình gửi,
     * và mình không đang mở đúng cuộc trò chuyện đó.
     */
    public static boolean shouldNotify(String myId, String sender, String receiver, boolean seen,
                                       String docId, Set<String> alreadyNotified, String openPeer) {
        if (myId == null || myId.isEmpty() || sender == null || sender.isEmpty()) return false;
        if (!myId.equals(receiver)) return false;
        if (myId.equals(sender)) return false;
        if (seen) return false;
        if (docId != null && alreadyNotified != null && alreadyNotified.contains(docId)) return false;
        return !sender.equals(openPeer);
    }

    /** Nội dung gọn cho thông báo: một dòng, cắt bớt nếu dài. */
    public static String preview(String message) {
        if (message == null) return "Đã gửi một tin nhắn";
        String m = message.replaceAll("\\s+", " ").trim();
        if (m.isEmpty()) return "Đã gửi một tin nhắn";
        if (m.length() <= MAX_BODY) return m;
        return m.substring(0, MAX_BODY - 1).trim() + "…";
    }

    /** Tên người gửi hiển thị: admin thấy tên khách, khách thấy "Nhân viên cửa hàng". */
    public static String senderTitle(boolean iAmAdmin, String customerName) {
        if (!iAmAdmin) return "Nhân viên cửa hàng";
        return customerName == null || customerName.trim().isEmpty() ? "Khách hàng" : customerName.trim();
    }

    /** "Bạn có 3 tin nhắn chưa đọc" — dùng khi mở app mà đã có tin chờ từ trước. */
    public static String unreadSummary(int count) {
        return count == 1 ? "Bạn có 1 tin nhắn chưa đọc" : "Bạn có " + count + " tin nhắn chưa đọc";
    }

    /** Một id thông báo ổn định cho mỗi người gửi (tin mới của cùng người thì thay thế thông báo cũ). */
    public static int notificationId(String peerId) {
        return 0x4A000000 | ((peerId == null ? 0 : peerId.hashCode()) & 0x00FFFFFF);
    }
}
