package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.UngDung.ThucPhamXanh.Helper.ChatNotificationRules;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class ChatNotificationRulesTest {

    @Test
    public void notifiesOnlyForUnreadMessagesAddressedToMe() {
        Set<String> done = new HashSet<>();
        assertTrue(ChatNotificationRules.shouldNotify("me", "other", "me", false, "d1", done, null));
        // gửi cho người khác
        assertFalse(ChatNotificationRules.shouldNotify("me", "other", "x", false, "d1", done, null));
        // chính mình gửi
        assertFalse(ChatNotificationRules.shouldNotify("me", "me", "me", false, "d1", done, null));
        // đã đọc
        assertFalse(ChatNotificationRules.shouldNotify("me", "other", "me", true, "d1", done, null));
    }

    @Test
    public void doesNotRepeatOrInterruptAnOpenConversation() {
        Set<String> done = new HashSet<>();
        done.add("d1");
        assertFalse(ChatNotificationRules.shouldNotify("me", "other", "me", false, "d1", done, null));
        assertTrue(ChatNotificationRules.shouldNotify("me", "other", "me", false, "d2", done, null));
        // đang mở đúng cuộc trò chuyện với người này → không báo
        assertFalse(ChatNotificationRules.shouldNotify("me", "other", "me", false, "d2", done, "other"));
        // đang mở chat với người khác → vẫn báo
        assertTrue(ChatNotificationRules.shouldNotify("me", "other", "me", false, "d2", done, "someoneElse"));
    }

    @Test
    public void rejectsMissingIds() {
        assertFalse(ChatNotificationRules.shouldNotify(null, "a", "b", false, "d", null, null));
        assertFalse(ChatNotificationRules.shouldNotify("", "a", "", false, "d", null, null));
        assertFalse(ChatNotificationRules.shouldNotify("me", null, "me", false, "d", null, null));
    }

    @Test
    public void previewIsOneShortLine() {
        assertEquals("Xin chào", ChatNotificationRules.preview("  Xin   chào \n"));
        assertEquals("Đã gửi một tin nhắn", ChatNotificationRules.preview(null));
        assertEquals("Đã gửi một tin nhắn", ChatNotificationRules.preview("   "));
        String longText = new String(new char[300]).replace('\0', 'a');
        String p = ChatNotificationRules.preview(longText);
        assertEquals(ChatNotificationRules.MAX_BODY, p.length());
        assertTrue(p.endsWith("…"));
    }

    @Test
    public void titlesAndSummary() {
        assertEquals("Nhân viên cửa hàng", ChatNotificationRules.senderTitle(false, "Hữu"));
        assertEquals("Hữu", ChatNotificationRules.senderTitle(true, " Hữu "));
        assertEquals("Khách hàng", ChatNotificationRules.senderTitle(true, ""));
        assertEquals("Bạn có 1 tin nhắn chưa đọc", ChatNotificationRules.unreadSummary(1));
        assertEquals("Bạn có 5 tin nhắn chưa đọc", ChatNotificationRules.unreadSummary(5));
    }

    @Test
    public void notificationIdIsStablePerPeer() {
        assertEquals(ChatNotificationRules.notificationId("abc"), ChatNotificationRules.notificationId("abc"));
        assertNotEquals(ChatNotificationRules.notificationId("abc"), ChatNotificationRules.notificationId("abd"));
        ChatNotificationRules.notificationId(null); // không ném lỗi
    }
}
