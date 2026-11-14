package com.UngDung.ThucPhamXanh.Helper;

import java.util.List;

/**
 * Dòng xem trước tin nhắn cuối trong danh sách chat — thuần Java, test được.
 * Chọn tin mới nhất trong số các tin của cuộc trò chuyện (cả hai chiều) rồi định dạng:
 * "Bạn: nội dung" nếu mình là người gửi, ngược lại chỉ nội dung.
 */
public final class ChatPreview {

    public static final String EMPTY = "Chưa có tin nhắn";
    public static final int MAX = 60;

    private ChatPreview() { }

    /** Một tin: thời điểm (mili giây, 0 nếu thiếu), người gửi, nội dung. */
    public static final class Msg {
        public final long time;
        public final String sender;
        public final String text;

        public Msg(long time, String sender, String text) {
            this.time = time;
            this.sender = sender == null ? "" : sender;
            this.text = text == null ? "" : text;
        }
    }

    /** Tin mới nhất; cùng thời điểm (hoặc thiếu thời gian) thì lấy tin đứng sau. Null nếu không có tin. */
    public static Msg latest(List<Msg> messages) {
        Msg best = null;
        if (messages == null) return null;
        for (Msg m : messages) {
            if (m == null) continue;
            if (best == null || m.time >= best.time) best = m;
        }
        return best;
    }

    /** Chuỗi hiển thị cho tin cuối; {@link #EMPTY} nếu chưa có tin nào. */
    public static String format(Msg last, String myId) {
        if (last == null) return EMPTY;
        String t = last.text.replaceAll("\\s+", " ").trim();
        if (t.isEmpty()) return EMPTY;
        if (t.length() > MAX) t = t.substring(0, MAX - 1).trim() + "…";
        return myId != null && myId.equals(last.sender) ? "Bạn: " + t : t;
    }
}
