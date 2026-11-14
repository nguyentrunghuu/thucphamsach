package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.UngDung.ThucPhamXanh.Helper.ChatPreview;
import com.UngDung.ThucPhamXanh.Helper.ChatPreview.Msg;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class ChatPreviewTest {

    @Test
    public void picksTheNewestMessageFromEitherDirection() {
        Msg last = ChatPreview.latest(Arrays.asList(
                new Msg(100, "me", "cũ"), new Msg(300, "peer", "mới nhất"), new Msg(200, "me", "giữa")));
        assertEquals("mới nhất", last.text);
    }

    @Test
    public void missingTimestampsFallBackToLaterEntry() {
        Msg last = ChatPreview.latest(Arrays.asList(new Msg(0, "me", "a"), new Msg(0, "peer", "b")));
        assertEquals("b", last.text);
        // tin có thời gian luôn thắng tin thiếu thời gian
        assertEquals("c", ChatPreview.latest(Arrays.asList(new Msg(5, "me", "c"), new Msg(0, "peer", "d"))).text);
    }

    @Test
    public void emptyOrNullInput() {
        assertNull(ChatPreview.latest(null));
        assertNull(ChatPreview.latest(new ArrayList<>()));
        assertEquals(ChatPreview.EMPTY, ChatPreview.format(null, "me"));
        assertEquals(ChatPreview.EMPTY, ChatPreview.format(new Msg(1, "me", "   "), "me"));
    }

    @Test
    public void formatsOwnMessagesWithPrefixAndTruncates() {
        assertEquals("Bạn: xin chào", ChatPreview.format(new Msg(1, "me", " xin   chào "), "me"));
        assertEquals("xin chào", ChatPreview.format(new Msg(1, "peer", "xin chào"), "me"));
        String longText = new String(new char[200]).replace('\0', 'x');
        String f = ChatPreview.format(new Msg(1, "peer", longText), "me");
        assertEquals(ChatPreview.MAX, f.length());
    }
}
