package com.UngDung.ThucPhamXanh.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.UngDung.ThucPhamXanh.Adapter.MessageAdapter;
import com.UngDung.ThucPhamXanh.Adapter.QuickReplyAdapter;
import com.UngDung.ThucPhamXanh.BotAI.MsgModal;
import com.UngDung.ThucPhamXanh.BotAI.RetrofitAPI;
import com.UngDung.ThucPhamXanh.Helper.ChatFirestoreHelper;
import com.UngDung.ThucPhamXanh.Models.Chat;
import com.UngDung.ThucPhamXanh.Models.User;
import com.UngDung.ThucPhamXanh.ThongBao.MyApplication;
import com.UngDung.ThucPhamXanh.R;

import android.text.TextWatcher;
import android.text.Editable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import com.UngDung.ThucPhamXanh.BotAI.GeminiAPI;
import com.UngDung.ThucPhamXanh.BotAI.GeminiRequest;
import com.UngDung.ThucPhamXanh.BotAI.GeminiResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MessageActivity extends AppCompatActivity {

    String userid;
    private CircleImageView profileImageMessage;
    private TextView tvUserMessage;
    private ImageButton btnSendMessage;
    private EditText edtSendMessage;

    FirebaseUser firebaseUser;
    DatabaseReference reference; // Vẫn dùng cho Users collection (Realtime Database)
    Intent intent;

    MessageAdapter messageAdapter;
    List<Chat> mChat = new ArrayList<>();
    RecyclerView rcvMessage;
    
    // Quick Replies
    private RecyclerView rcvQuickReplies;
    private CardView cardQuickReplies;
    private QuickReplyAdapter quickReplyAdapter;
    private List<String> quickReplyList;
    private boolean shouldShowQuickReplies = true;

    ValueEventListener seenListener;
    private ChatFirestoreHelper chatHelper;
    private ListenerRegistration[] messageListeners;

    boolean notify = false;
    private int whomess;

    private String read;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);


        Toolbar toolbar = findViewById(R.id.toolbar_message);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        rcvMessage = findViewById(R.id.rcv_message);
        rcvMessage.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        rcvMessage.setLayoutManager(linearLayoutManager);

        profileImageMessage = findViewById(R.id.profile_image_message);
        tvUserMessage = findViewById(R.id.tv_username_message);
        btnSendMessage = findViewById(R.id.btn_send_message);
        edtSendMessage = findViewById(R.id.edt_send_message);
        
        // Quick Replies - Setup sau khi có whomess
        rcvQuickReplies = findViewById(R.id.rcv_quick_replies);
        cardQuickReplies = findViewById(R.id.card_quick_replies);

        intent = getIntent();
        userid = intent.getStringExtra("userid");
        whomess = intent.getIntExtra("who", 0);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);
        
        // Khởi tạo ChatFirestoreHelper
        chatHelper = new ChatFirestoreHelper();
        
        // Admin ID nhất quán
        final String ADMIN_ID = com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID;
        
        if (whomess == 1) {
            // Admin đang chat với user
            // read = admin ID (người gửi là admin)
            read = ADMIN_ID;
            // userid = user ID (người nhận là user)
        } else if (whomess == 2) {
            // User đang chat với admin
            // User - cần firebaseUser
            if (firebaseUser == null) {
                Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            // read = user ID (người gửi là user)
            read = firebaseUser.getUid();
            // userid = admin ID (người nhận là admin) - đảm bảo userid là admin ID
            if (userid == null || !userid.equals(ADMIN_ID)) {
                userid = ADMIN_ID;
                Log.d("MessageActivity", "Setting userid to ADMIN_ID: " + ADMIN_ID);
            }
        }
        
        Log.d("MessageActivity", "whomess: " + whomess + ", read: " + read + ", userid: " + userid);

        // Setup Quick Replies sau khi đã có whomess
        setupQuickReplies();

        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageToUser();
            }
        });

        // Handle Enter key press
        edtSendMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessageToUser();
                return true;
            }
            return false;
        });
        
        // Update quick replies visibility when focus changes
        edtSendMessage.setOnFocusChangeListener((v, hasFocus) -> {
            updateQuickRepliesVisibility();
        });
        
        // Update quick replies visibility when text changes
        edtSendMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateQuickRepliesVisibility();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    // Load đầy đủ thông tin từ Firestore nếu chưa có
                    loadFullUserInfo(user, () -> {
                        updateUserDisplay(user);
                    });
                    // Cập nhật UI ngay với thông tin có sẵn
                    updateUserDisplay(user);
                } else {
                    tvUserMessage.setText("Admin");
                    profileImageMessage.setImageResource(R.drawable.logo_new);
                    readMessage(read, userid, "default");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MessageActivity", "Error loading user: " + error.getMessage());
            }
        });
    }
    
    /**
     * Cập nhật hiển thị thông tin user
     */
    private void updateUserDisplay(User user) {
        if (user == null) {
            tvUserMessage.setText("Admin");
            profileImageMessage.setImageResource(R.drawable.logo_new);
            readMessage(read, userid, "default");
            return;
        }
        
        // Hiển thị tên, nếu không có tên thì hiển thị email
        String displayName = user.getName();
        if (displayName == null || displayName.trim().isEmpty() || displayName.equals("any name")) {
            displayName = user.getEmail();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = "Admin"; // Fallback
            }
        }
        tvUserMessage.setText(displayName);
        
        // Load avatar
        if (user.getAvatar() != null && !user.getAvatar().equals("default") && !user.getAvatar().isEmpty()) {
            Glide.with(getApplicationContext()).load(user.getAvatar()).into(profileImageMessage);
        } else {
            profileImageMessage.setImageResource(R.drawable.logo_new);
        }
        
        String imageUrl = user.getAvatar() == null || user.getAvatar().isEmpty() ? "default" : user.getAvatar();
        readMessage(read, userid, imageUrl);
        
        // Cập nhật quick replies sau khi load user
        if (cardQuickReplies != null && edtSendMessage != null) {
            String text = edtSendMessage.getText() != null ? edtSendMessage.getText().toString().trim() : "";
            if (text.isEmpty()) {
                cardQuickReplies.postDelayed(() -> updateQuickRepliesVisibility(), 500);
            }
        }
    }
    
    /**
     * Load đầy đủ thông tin User từ Firestore
     */
    private void loadFullUserInfo(User user, Runnable onComplete) {
        if (user == null || user.getIduser() == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        
        // Nếu đã có đầy đủ thông tin, không cần load lại
        if (user.getEmail() != null && !user.getEmail().isEmpty() && 
            user.getName() != null && !user.getName().isEmpty() && !user.getName().equals("any name")) {
            if (onComplete != null) onComplete.run();
            return;
        }
        
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        
        // Load email từ IDUser collection
        db.collection("IDUser")
                .whereEqualTo("iduser", user.getIduser())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        com.google.firebase.firestore.QueryDocumentSnapshot doc = 
                            (com.google.firebase.firestore.QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        String email = doc.getString("email");
                        if (email != null && !email.isEmpty()) {
                            user.setEmail(email);
                        }
                    }
                    
                    // Load tên thật từ User/Profile collection
                    db.collection("User").document(user.getIduser())
                            .collection("Profile")
                            .limit(1)
                            .get()
                            .addOnSuccessListener(profileSnapshots -> {
                                if (profileSnapshots != null && !profileSnapshots.isEmpty()) {
                                    com.google.firebase.firestore.QueryDocumentSnapshot profileDoc = 
                                        (com.google.firebase.firestore.QueryDocumentSnapshot) profileSnapshots.getDocuments().get(0);
                                    String hoten = profileDoc.getString("hoten");
                                    if (hoten != null && !hoten.trim().isEmpty()) {
                                        user.setName(hoten);
                                    }
                                    String avatar = profileDoc.getString("avatar");
                                    if (avatar != null && !avatar.trim().isEmpty()) {
                                        user.setAvatar(avatar);
                                    }
                                }
                                if (onComplete != null) onComplete.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("MessageActivity", "Error loading profile: " + e.getMessage());
                                if (onComplete != null) onComplete.run();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("MessageActivity", "Error loading email: " + e.getMessage());
                    if (onComplete != null) onComplete.run();
        });
    }

    private void sendPushNotification(String sender, String message) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_food2);
        Intent notifyIntent = new Intent(this, ChatActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(
                this, getNotificationId(), notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification notification = new NotificationCompat.Builder(this, MyApplication.CHANNEL_ID)
                .setContentTitle(sender)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(bitmap)
                .setAutoCancel(true)    // Tự động xóa Notification sau khi user đã click vào

                // truyền vào notifyPendingIntent đã khai báo ở trên đối với Special Activity
                .setContentIntent(notifyPendingIntent)

                .setColor(getResources().getColor(R.color.Orange))
                .build();

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(getNotificationId(), notification);
    }

    private int getNotificationId() {
        return (int) new Date().getTime();
    }

    private void sendMessageToUser() {
        sendMessageToUser(edtSendMessage.getText().toString().trim());
    }
    
    private void sendMessageToUser(String msg) {
        notify = true;
        if (!msg.isEmpty()) {
            final String ADMIN_ID = com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID;
            
            // Xác định sender và receiver
            String sender = read; // Người gửi
            String receiver = userid; // Người nhận
            
            // Đảm bảo user luôn gửi đến admin
            if (whomess == 2) {
                // User gửi tin nhắn
                receiver = ADMIN_ID; // User luôn gửi đến admin
                getResponse(msg); // Gọi bot response nếu cần
            }
            
            Log.d("MessageActivity", "sendMessageToUser - sender: " + sender + ", receiver: " + receiver);
            sendMessage(sender, receiver, msg);
            edtSendMessage.setText("");
            // Clear focus and hide keyboard
            edtSendMessage.clearFocus();
            updateQuickRepliesVisibility();
        } else {
            Toast.makeText(MessageActivity.this, "Không thể gửi tin nhắn trống", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Setup Quick Replies - Khác nhau cho Admin và User
     */
    private void setupQuickReplies() {
        quickReplyList = new ArrayList<>();
        
        // Lấy whomess từ intent để xác định admin hay user
        int who = getIntent().getIntExtra("who", 0);
        
        if (who == 1) {
            // Admin quick replies
            quickReplyList.add("Xin chào! Tôi có thể giúp gì cho bạn?");
            quickReplyList.add("Bạn cần hỗ trợ gì?");
            quickReplyList.add("Đơn hàng của bạn đang được xử lý");
            quickReplyList.add("Cảm ơn bạn đã tin tưởng!");
            quickReplyList.add("Chúc bạn một ngày tốt lành!");
        } else {
            // User quick replies
            quickReplyList.add("Xin chào");
            quickReplyList.add("Tôi muốn đặt hàng");
            quickReplyList.add("Giá sản phẩm?");
            quickReplyList.add("Có giao hàng không?");
            quickReplyList.add("Thời gian giao hàng?");
            quickReplyList.add("Cảm ơn");
        }
        
        quickReplyAdapter = new QuickReplyAdapter(quickReplyList, message -> {
            // Khi click vào quick reply, tự động điền vào EditText và gửi
            sendMessageToUser(message);
        });
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rcvQuickReplies.setLayoutManager(layoutManager);
        rcvQuickReplies.setAdapter(quickReplyAdapter);
        
        // Hiển thị quick replies ngay lập tức - đảm bảo luôn hiển thị khi EditText trống
        if (cardQuickReplies != null && edtSendMessage != null && rcvQuickReplies != null) {
            // Đảm bảo RecyclerView có dữ liệu
            if (quickReplyList != null && !quickReplyList.isEmpty() && quickReplyAdapter != null) {
                cardQuickReplies.postDelayed(() -> {
                    String text = edtSendMessage.getText() != null ? edtSendMessage.getText().toString().trim() : "";
                    if (text.isEmpty()) {
                        // Force hiển thị ngay
                        cardQuickReplies.setVisibility(View.VISIBLE);
                        cardQuickReplies.setAlpha(1.0f);
                        // Đảm bảo adapter được notify
                        quickReplyAdapter.notifyDataSetChanged();
                        // Cập nhật padding của RecyclerView tin nhắn
                        updateQuickRepliesVisibility();
                        Log.d("MessageActivity", "Quick replies displayed with " + quickReplyList.size() + " items");
                    }
                }, 500);
            }
        }
    }
    
    /**
     * Cập nhật hiển thị Quick Replies dựa trên trạng thái
     * Đồng thời cập nhật padding của RecyclerView tin nhắn để không bị che
     */
    private void updateQuickRepliesVisibility() {
        if (cardQuickReplies == null || edtSendMessage == null || rcvMessage == null) return;
        
        // Hiển thị quick replies khi EditText trống (bất kể có tin nhắn hay không)
        String text = edtSendMessage.getText() != null ? edtSendMessage.getText().toString().trim() : "";
        boolean shouldShow = shouldShowQuickReplies && text.isEmpty();
        
        if (shouldShow) {
            if (cardQuickReplies.getVisibility() != View.VISIBLE) {
                showQuickReplies();
            }
            // Tăng paddingBottom của RecyclerView tin nhắn để không bị che bởi quick replies
            rcvMessage.post(() -> {
                rcvMessage.setPadding(
                    rcvMessage.getPaddingLeft(),
                    rcvMessage.getPaddingTop(),
                    rcvMessage.getPaddingRight(),
                    200 // Đủ không gian cho quick replies (88dp margin + ~112dp height)
                );
            });
        } else {
            if (cardQuickReplies.getVisibility() == View.VISIBLE) {
                hideQuickReplies();
            }
            // Giảm paddingBottom khi ẩn quick replies
            rcvMessage.post(() -> {
                rcvMessage.setPadding(
                    rcvMessage.getPaddingLeft(),
                    rcvMessage.getPaddingTop(),
                    rcvMessage.getPaddingRight(),
                    160 // Padding mặc định
                );
            });
        }
    }
    
    /**
     * Hiển thị Quick Replies với animation
     */
    private void showQuickReplies() {
        if (cardQuickReplies != null) {
            cardQuickReplies.setVisibility(View.VISIBLE);
            cardQuickReplies.setAlpha(0f);
            cardQuickReplies.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }
    }
    
    /**
     * Ẩn Quick Replies với animation
     */
    private void hideQuickReplies() {
        if (cardQuickReplies != null && cardQuickReplies.getVisibility() == View.VISIBLE) {
            cardQuickReplies.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        if (cardQuickReplies != null) {
                            cardQuickReplies.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }

    private void sendMessage(String sender, String receiver, String message) {
        // Gửi tin nhắn qua Firestore
        Log.d("MessageActivity", "Sending message - sender: " + sender + ", receiver: " + receiver + ", message: " + message);
        chatHelper.sendMessage(sender, receiver, message, task -> {
            if (task.isSuccessful()) {
                Log.d("MessageActivity", "Message sent successfully to Firestore");
                // ChatList đã được cập nhật tự động trong sendMessage
            } else {
                Log.e("MessageActivity", "Error sending message: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                Toast.makeText(MessageActivity.this, "Lỗi gửi tin nhắn", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void readMessage(String myid, String userid, String imageurl) {
        // Clear existing messages
                mChat.clear();

        // Initialize adapter once
                    messageAdapter = new MessageAdapter(MessageActivity.this, mChat, imageurl);
                    messageAdapter.Who(userid);
                    rcvMessage.setAdapter(messageAdapter);

        // Map để lưu tin nhắn theo ID (tránh trùng lặp)
        HashMap<String, Chat> chatMap = new HashMap<>();
        
        // Hàm merge và cập nhật UI
        Runnable updateUI = () -> {
            // Chuyển từ map sang list
            mChat.clear();
            mChat.addAll(chatMap.values());
            
            // Sắp xếp theo timestamp
            mChat.sort((c1, c2) -> {
                if (c1.getTimestamp() == null || c2.getTimestamp() == null) {
                    return 0;
                }
                return c1.getTimestamp().compareTo(c2.getTimestamp());
            });

            Log.d("MessageActivity", "Updating UI with " + mChat.size() + " messages");
            
            // Notify adapter after all messages are loaded
            messageAdapter.notifyDataSetChanged();
            
            // Cập nhật hiển thị quick replies
            updateQuickRepliesVisibility();
            
            // Scroll to bottom after messages are loaded
            rcvMessage.post(() -> {
                if (mChat.size() > 0) {
                    rcvMessage.smoothScrollToPosition(mChat.size() - 1);
                }
            });
        };
        
        Log.d("MessageActivity", "Setting up realtime listeners - myid: " + myid + ", userid: " + userid);
        
        // Lắng nghe tin nhắn từ Firestore - Listener 1: từ myId đến userId
        messageListeners = new ListenerRegistration[2];
        
        // Helper function để setup listener với fallback
        Runnable setupListener1 = () -> {
            messageListeners[0] = chatHelper.db.collection("Chats")
                    .whereEqualTo("sender", myid)
                    .whereEqualTo("receiver", userid)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                    .addSnapshotListener((querySnapshot, error) -> {
                        if (error != null) {
                            Log.e("MessageActivity", "Error in listener 1: " + error.getMessage());
                            // Nếu lỗi do thiếu index, thử query không có orderBy
                            if (error.getMessage() != null && error.getMessage().contains("index")) {
                                Log.w("MessageActivity", "Index missing for listener 1, trying without orderBy");
                                if (messageListeners[0] != null) {
                                    messageListeners[0].remove();
                                }
                                messageListeners[0] = chatHelper.db.collection("Chats")
                                        .whereEqualTo("sender", myid)
                                        .whereEqualTo("receiver", userid)
                                        .addSnapshotListener((snapshot, e) -> {
                                            if (e == null && snapshot != null) {
                                                Log.d("MessageActivity", "Listener 1 (no orderBy) received " + snapshot.size() + " messages");
                                                for (QueryDocumentSnapshot doc : snapshot) {
                                                    Chat chat = ChatFirestoreHelper.documentToChat(doc);
                                                    if (chat != null && chat.getId() != null) {
                                                        chatMap.put(chat.getId(), chat);
                                                    }
                                                }
                                                updateUI.run();
                                            }
                                        });
                            }
                            return;
                        }
                        
                        if (querySnapshot != null) {
                            Log.d("MessageActivity", "Listener 1 received " + querySnapshot.size() + " messages");
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                Chat chat = ChatFirestoreHelper.documentToChat(document);
                                if (chat != null && chat.getId() != null) {
                                    chatMap.put(chat.getId(), chat);
                                }
                            }
                            updateUI.run();
                            chatHelper.markAllAsSeen(myid, userid);
                        }
                    });
        };
        
        Runnable setupListener2 = () -> {
            messageListeners[1] = chatHelper.db.collection("Chats")
                    .whereEqualTo("sender", userid)
                    .whereEqualTo("receiver", myid)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                    .addSnapshotListener((querySnapshot, error) -> {
                        if (error != null) {
                            Log.e("MessageActivity", "Error in listener 2: " + error.getMessage());
                            // Nếu lỗi do thiếu index, thử query không có orderBy
                            if (error.getMessage() != null && error.getMessage().contains("index")) {
                                Log.w("MessageActivity", "Index missing for listener 2, trying without orderBy");
                                if (messageListeners[1] != null) {
                                    messageListeners[1].remove();
                                }
                                messageListeners[1] = chatHelper.db.collection("Chats")
                                        .whereEqualTo("sender", userid)
                                        .whereEqualTo("receiver", myid)
                                        .addSnapshotListener((snapshot, e) -> {
                                            if (e == null && snapshot != null) {
                                                Log.d("MessageActivity", "Listener 2 (no orderBy) received " + snapshot.size() + " messages");
                                                for (QueryDocumentSnapshot doc : snapshot) {
                                                    Chat chat = ChatFirestoreHelper.documentToChat(doc);
                                                    if (chat != null && chat.getId() != null) {
                                                        chatMap.put(chat.getId(), chat);
                                                    }
                                                }
                                                updateUI.run();
                                            }
                                        });
                            }
                            return;
                        }
                        
                        if (querySnapshot != null) {
                            Log.d("MessageActivity", "Listener 2 received " + querySnapshot.size() + " messages");
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                Chat chat = ChatFirestoreHelper.documentToChat(document);
                                if (chat != null && chat.getId() != null) {
                                    chatMap.put(chat.getId(), chat);
                                }
                            }
                            updateUI.run();
                            chatHelper.markAllAsSeen(myid, userid);
                        }
                    });
        };
        
        // Setup cả 2 listeners
        setupListener1.run();
        setupListener2.run();
    }

    private void status(String status) {
        // Admin hoặc User đều có thể cập nhật status
        reference = FirebaseDatabase.getInstance().getReference("Users").child(read);

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }

//    private void seenMessage(String userid){
//
//        reference = FirebaseDatabase.getInstance().getReference("Chats");
//        seenListener = reference.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                for (DataSnapshot dataSnapshot: snapshot.getChildren()){
//                    Chat chat = dataSnapshot.getValue(Chat.class);
//                    HashMap<String, Object> hashMap = new HashMap<>();
//                    Log.d("mess", "Người nhận: " + chat.getReceiver() +"\n"+"Người gửi: " + chat.getSender());
//                    if (chat.getReceiver().equals(read) && chat.getSender().equals(userid)) {
//                        hashMap.put("isseen", true);
//                    }
//                    dataSnapshot.getRef().updateChildren(hashMap);
//                }
//
//
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });
//    }

    private void getResponse(String message) {
        final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/";
        final String GEMINI_API_KEY = com.UngDung.ThucPhamXanh.BuildConfig.GEMINI_API_KEY; // đọc từ local.properties
        final String ADMIN_ID = com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GEMINI_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        GeminiAPI geminiAPI = retrofit.create(GeminiAPI.class);

        // System prompt cho Admin
        String systemPrompt = "Bạn là Admin (người quản lý) của ứng dụng 'Thực Phẩm Xanh'. " +
                "Nhiệm vụ của bạn là hỗ trợ khách hàng trả lời các thắc mắc về sản phẩm, đơn hàng và chính sách cửa hàng. " +
                "Hãy trả lời một cách chuyên nghiệp, lịch sự và hỗ trợ tận tình. Sử dụng tiếng Việt.";

        GeminiRequest request = new GeminiRequest(systemPrompt);
        request.addContent("user", message);

        Call<GeminiResponse> call = geminiAPI.getChatCompletion(GEMINI_API_KEY, request);
        call.enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GeminiResponse geminiResponse = response.body();
                    String botReply = geminiResponse.getResponseText();
                    
                    // Gửi tin nhắn trả lời từ Admin qua Firestore
                    chatHelper.sendMessage(ADMIN_ID, firebaseUser.getUid(), botReply, task -> {
                        if (task.isSuccessful()) {
                            Log.d("MessageActivity", "Admin AI response sent successfully");
                        }
                    });
                } else {
                    Log.e("MessageActivity", "Gemini API error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                Log.e("MessageActivity", "Gemini API call failed: " + t.getMessage());
                // Không gửi tin nhắn lỗi "Cung cấp thêm thông tin" nữa để tránh làm phiền user
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        status("offline");
        // Remove listeners khi activity pause
        if (messageListeners != null) {
            for (ListenerRegistration listener : messageListeners) {
                if (listener != null) {
                    listener.remove();
                }
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listeners khi activity destroy
        if (messageListeners != null) {
            for (ListenerRegistration listener : messageListeners) {
                if (listener != null) {
                    listener.remove();
                }
            }
        }
    }
}