package com.UngDung.ThucPhamXanh.Helper;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.MessageActivity;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Báo tin nhắn mới (Firestore "Chats") bằng thông báo hệ thống, cho cả khách hàng lẫn admin.
 *
 *  - Khách: lắng nghe tin gửi tới UID của mình. Admin: tin gửi tới {@link AppConstants#ADMIN_ID}
 *    (admin không có tài khoản Firebase Auth nên không thể dựa vào FirebaseAuth như NotificationService).
 *  - Chạy trong tiến trình của app: còn nhận được khi app ở nền, NHƯNG không nhận được sau khi người dùng
 *    vuốt tắt hẳn app (muốn vậy phải dùng FCM + máy chủ gửi push).
 *  - Lần đọc đầu: tin chưa đọc có sẵn được gom thành MỘT thông báo tổng; từ đó về sau mỗi tin mới báo một lần.
 *  - Không báo khi đang mở đúng cuộc trò chuyện đó ({@link #setOpenPeer}).
 *
 * Gọi {@link #start} sau khi đăng nhập và {@link #stop} khi đăng xuất.
 */
public final class ChatNotifier {

    private static final String TAG = "ChatNotifier";
    public static final String CHANNEL_ID = "chat_messages";

    private static ChatNotifier instance;

    private final Context appContext;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Set<String> notified = new HashSet<>();
    private final Map<String, String> nameCache = new HashMap<>();

    private ListenerRegistration registration;
    private String myId;
    private boolean iAmAdmin;
    private boolean firstSnapshot;
    private volatile String openPeer;

    private ChatNotifier(Context context) {
        this.appContext = context.getApplicationContext();
        createChannel();
    }

    public static synchronized ChatNotifier get(Context context) {
        if (instance == null) instance = new ChatNotifier(context);
        return instance;
    }

    /** Người đang trò chuyện hiện tại (MessageActivity đang mở) — null khi không ở trong cuộc chat nào. */
    public static void setOpenPeer(Context context, String peerId) {
        get(context).openPeer = peerId;
        if (peerId != null) get(context).cancelFor(peerId); // vào chat là xóa thông báo của người đó
    }

    /** Bắt đầu nghe tin gửi tới myId. Gọi lại với cùng myId thì không làm gì. */
    public synchronized void start(String id, boolean admin) {
        if (id == null || id.isEmpty()) return;
        if (id.equals(myId) && registration != null) return;
        stop();
        myId = id;
        iAmAdmin = admin;
        firstSnapshot = true;
        registration = db.collection("Chats").whereEqualTo("receiver", id)
                .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE, (snap, e) -> {
            if (e != null) {
                Log.e(TAG, "Lỗi nghe tin nhắn", e);
                return;
            }
            if (snap == null) return;
            onSnapshot(snap.getDocumentChanges(), snap.getDocuments(), snap.getMetadata().isFromCache());
        });
        Log.d(TAG, "Bắt đầu nghe tin nhắn cho " + (admin ? "admin" : id));
    }

    public synchronized void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        myId = null;
        notified.clear();
        NotificationManagerCompat.from(appContext).cancelAll();
    }

    // ------------------------------------------------------------------ xử lý

    private void onSnapshot(java.util.List<DocumentChange> changes, java.util.List<DocumentSnapshot> all,
                            boolean fromCache) {
        final String me = myId;
        if (me == null) return;
        Log.d(TAG, "snapshot: first=" + firstSnapshot + " cache=" + fromCache + " docs=" + all.size()
                + " changes=" + changes.size());
        // Snapshot đầu thường đến từ bộ nhớ đệm và có thể cũ → chờ snapshot từ máy chủ làm mốc,
        // nếu không các tin chưa đọc có sẵn trên máy chủ sẽ bị báo lẻ từng tin.
        if (firstSnapshot && fromCache) return;

        if (firstSnapshot) {
            firstSnapshot = false;
            int unread = 0;
            for (DocumentSnapshot d : all) {
                boolean fresh = ChatNotificationRules.shouldNotify(me, str(d, "sender"), str(d, "receiver"),
                        isSeen(d), d.getId(), notified, openPeer);
                if (fresh) {
                    unread++;
                    notified.add(d.getId());
                }
            }
            if (unread > 0) postSummary(unread);
            return;
        }
        for (DocumentChange dc : changes) {
            if (dc.getType() != DocumentChange.Type.ADDED) continue;
            DocumentSnapshot d = dc.getDocument();
            String sender = str(d, "sender");
            if (!ChatNotificationRules.shouldNotify(me, sender, str(d, "receiver"), isSeen(d),
                    d.getId(), notified, openPeer)) {
                Log.d(TAG, "bỏ qua " + d.getId() + " seen=" + isSeen(d) + " openPeer=" + openPeer);
                continue;
            }
            notified.add(d.getId());
            final String body = ChatNotificationRules.preview(str(d, "message"));
            resolveTitle(sender, title -> post(sender, title, body));
        }
    }

    private interface TitleCallback {
        void onTitle(String title);
    }

    /** Admin cần tên khách (Profile.hoten → email → "Khách hàng"); khách luôn thấy "Nhân viên cửa hàng". */
    private void resolveTitle(String senderId, TitleCallback cb) {
        if (!iAmAdmin) {
            cb.onTitle(ChatNotificationRules.senderTitle(false, null));
            return;
        }
        String cached = nameCache.get(senderId);
        if (cached != null) {
            cb.onTitle(cached);
            return;
        }
        db.collection("User").document(senderId).collection("Profile").limit(1).get()
                .addOnSuccessListener(snap -> {
                    String name = "";
                    if (!snap.isEmpty()) name = str(snap.getDocuments().get(0), "hoten");
                    if (!name.trim().isEmpty()) {
                        done(senderId, name, cb);
                        return;
                    }
                    db.collection("IDUser").whereEqualTo("iduser", senderId).limit(1).get()
                            .addOnSuccessListener(ids -> {
                                String email = ids.isEmpty() ? "" : str(ids.getDocuments().get(0), "email");
                                done(senderId, AdminUserCreator.nameFromEmail(email), cb);
                            })
                            .addOnFailureListener(x -> done(senderId, "", cb));
                })
                .addOnFailureListener(x -> done(senderId, "", cb));
    }

    private void done(String senderId, String name, TitleCallback cb) {
        String title = ChatNotificationRules.senderTitle(true, name);
        nameCache.put(senderId, title);
        cb.onTitle(title);
    }

    // ------------------------------------------------------------------ hiển thị

    private void post(String peerId, String title, String body) {
        Log.d(TAG, "post " + title + ": " + body);
        if (!canPost()) return;
        Intent open = openChatIntent(peerId);
        PendingIntent pi = PendingIntent.getActivity(appContext, ChatNotificationRules.notificationId(peerId), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = baseBuilder()
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pi);
        notifySafe(ChatNotificationRules.notificationId(peerId), b);
    }

    private void postSummary(int unread) {
        if (!canPost()) return;
        Intent open = new Intent(appContext, com.UngDung.ThucPhamXanh.View.ChatActivity.class);
        if (iAmAdmin) open.putExtra("message", 1);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(appContext, 0x4A0FFFFF, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = baseBuilder()
                .setContentTitle(iAmAdmin ? "Tin nhắn từ khách hàng" : "Tin nhắn từ cửa hàng")
                .setContentText(ChatNotificationRules.unreadSummary(unread))
                .setContentIntent(pi);
        notifySafe(0x4A0FFFFF, b);
    }

    /** Mở thẳng cuộc trò chuyện: admin → với khách (who=1), khách → với admin (who=2). */
    private Intent openChatIntent(String peerId) {
        Intent i = new Intent(appContext, MessageActivity.class);
        i.putExtra("userid", iAmAdmin ? peerId : AppConstants.ADMIN_ID);
        i.putExtra("who", iAmAdmin ? 1 : 2);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
    }

    private NotificationCompat.Builder baseBuilder() {
        return new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setColor(ContextCompat.getColor(appContext, R.color.primary_green));
    }

    private boolean canPost() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Chưa được cấp quyền thông báo");
            return false;
        }
        return NotificationManagerCompat.from(appContext).areNotificationsEnabled();
    }

    private void notifySafe(int id, NotificationCompat.Builder b) {
        try {
            NotificationManagerCompat.from(appContext).notify(id, b.build());
        } catch (SecurityException e) {
            Log.w(TAG, "Không hiện được thông báo", e);
        }
    }

    private void cancelFor(String peerId) {
        NotificationManagerCompat.from(appContext).cancel(ChatNotificationRules.notificationId(peerId));
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Tin nhắn", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Tin nhắn mới giữa khách hàng và cửa hàng");
        NotificationManager nm = appContext.getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    private static String str(DocumentSnapshot d, String field) {
        Object v = d.get(field);
        return v == null ? "" : v.toString();
    }

    private static boolean isSeen(DocumentSnapshot d) {
        Boolean b = d.getBoolean("isseen");
        return b != null && b;
    }
}
