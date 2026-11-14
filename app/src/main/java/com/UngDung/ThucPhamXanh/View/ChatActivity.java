package com.UngDung.ThucPhamXanh.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Adapter.UserAdapter;
import com.UngDung.ThucPhamXanh.Helper.ChatFirestoreHelper;
import com.UngDung.ThucPhamXanh.Models.ChatList;
import com.UngDung.ThucPhamXanh.Models.User;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private ProgressBar progressBarChat;
    private RecyclerView recyclerViewChat;
    private List<User> mUsers;
    private UserAdapter userAdapter;
    private ImageView imgBackChat;

    FirebaseUser firebaseUser;
    DatabaseReference reference; // Vẫn dùng cho Users collection (Realtime Database)
    private List<ChatList> usersList;
    private ChatFirestoreHelper chatHelper;
    private ListenerRegistration chatListListener;
    private FirebaseFirestore db;

    private int who;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        InitWidget();
        Init();
        
        // Admin ID - sử dụng ID nhất quán
        final String ADMIN_ID = com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID;
        
        // Khởi tạo ChatFirestoreHelper
        chatHelper = new ChatFirestoreHelper();
        db = FirebaseFirestore.getInstance();
        
        // nếu là admin đăng nhập (who == 1)
        if (who == 1) {
            // Admin không cần Firebase Auth, có thể vào chat trực tiếp
            // Admin xem danh sách users đã chat từ Firestore
            loadChatListFromFirestore(ADMIN_ID);
        } else { // nếu là user đăng nhập (who == 2 hoặc 0)
            // Kiểm tra null cho firebaseUser (chỉ user mới cần)
            if (firebaseUser == null) {
                Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            // User luôn thấy Admin ngay lập tức (không cần đợi Firestore)
            AdminChatsWithUsers();
            // Sau đó load chat list từ Firestore (để cập nhật nếu có)
            loadChatListFromFirestore(firebaseUser.getUid());
        }


        imgBackChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


    }

    private void Init() {
        recyclerViewChat.setHasFixedSize(true);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(ChatActivity.this));
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        usersList = new ArrayList<>();
        mUsers = new ArrayList<>();
        progressBarChat.setVisibility(View.VISIBLE);
        who = getIntent().getIntExtra("message", 0);
        
        // Initialize adapter once
        // Admin chat bằng ADMIN_ID (không có tài khoản Firebase Auth); khách chat bằng uid của mình
        final String myChatId = who == 1
                ? com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID
                : (firebaseUser != null ? firebaseUser.getUid() : null);
        userAdapter = new UserAdapter(ChatActivity.this, mUsers, true, myChatId, new IClickCTHD() {
            @Override
            public void onClickCTHD(int pos) {
                if (pos >= 0 && pos < mUsers.size()) {
                    User user = mUsers.get(pos);
                    Intent intent = new Intent(ChatActivity.this, MessageActivity.class);
                    intent.putExtra("userid", user.getIduser());
                    intent.putExtra("who", who == 1 ? 1 : 2);
                    startActivity(intent);
                }
            }
        });
        recyclerViewChat.setAdapter(userAdapter);
    }

    private void InitWidget() {
        imgBackChat = findViewById(R.id.img_back_chat);
        progressBarChat = findViewById(R.id.progressBar_chat);
        recyclerViewChat = findViewById(R.id.recycler_view_chat);
    }

    private void AdminChatsWithUsers() {
        Log.d("account", "user đăng nhập");
        mUsers.clear();
        // User luôn thấy Admin trong danh sách chat (không cần kiểm tra chatList)
        final String ADMIN_ID = com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID;
        mUsers.add(new User(ADMIN_ID, "Admin", "default", "offline", "Admin"));
        
        // Cập nhật adapter thay vì tạo mới
        if (userAdapter != null) {
            userAdapter.notifyDataSetChanged();
        }
        
        // Ẩn progressBar ngay lập tức
        progressBarChat.setVisibility(View.INVISIBLE);
        Log.d("ChatActivity", "AdminChatsWithUsers: Added Admin to list, users count: " + mUsers.size());
    }

    /**
     * Load chat list từ Firestore
     */
    private void loadChatListFromFirestore(String userId) {
        Log.d("ChatActivity", "Loading chat list from Firestore for userId: " + userId);
        chatHelper.getChatList(userId, queryDocumentSnapshots -> {
            try {
                usersList.clear();
                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    Log.d("ChatActivity", "ChatList query returned " + queryDocumentSnapshots.size() + " documents");
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ChatList chatList = ChatFirestoreHelper.documentToChatList(document);
                        if (chatList != null && chatList.getId() != null) {
                            usersList.add(chatList);
                            Log.d("ChatActivity", "Added to chatList: " + chatList.getId());
                        }
                    }
                } else {
                    Log.d("ChatActivity", "ChatList query returned empty or null");
                }
                
                Log.d("ChatActivity", "Final usersList size: " + usersList.size());
                
                // Sau khi có chat list, load users (chỉ cho admin)
                if (who == 1) {
                    UsersChatWithAdmin();
                } else {
                    // User đã được hiển thị Admin ngay từ đầu, không cần làm gì thêm
                    progressBarChat.setVisibility(View.INVISIBLE);
                }
            } catch (Exception e) {
                Log.e("ChatActivity", "Error processing chat list: " + e.getMessage(), e);
                progressBarChat.setVisibility(View.INVISIBLE);
                // Nếu là user, đảm bảo Admin vẫn được hiển thị
                if (who != 1 && mUsers.isEmpty()) {
                    AdminChatsWithUsers();
                } else if (who == 1) {
                    // Nếu là admin và có lỗi, thử load trực tiếp từ Chats
                    Log.d("ChatActivity", "Error loading ChatList, trying to load from Chats collection");
                    loadUsersFromChats(userId);
                }
            }
        });
    }
    
    private void UsersChatWithAdmin() {
        Log.d("ChatActivity", "UsersChatWithAdmin called - usersList size: " + usersList.size());
        final String ADMIN_ID = com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID;
        
        // Nếu không có chat list, thử load trực tiếp từ Chats collection
        if (usersList.isEmpty()) {
            Log.d("ChatActivity", "ChatList is empty, loading users directly from Chats collection");
            loadUsersFromChats(ADMIN_ID);
            return;
        }
        
        // Load users từ Realtime Database (Users collection vẫn ở Realtime Database)
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mUsers.clear();
                Log.d("ChatActivity", "Loading users from Realtime DB, total users: " + snapshot.getChildrenCount());
                
                // Tạo set các user IDs từ chatList để match nhanh hơn
                java.util.Set<String> chatUserIds = new java.util.HashSet<>();
                for (ChatList chatlist : usersList) {
                    if (chatlist != null && chatlist.getId() != null) {
                        chatUserIds.add(chatlist.getId());
                        Log.d("ChatActivity", "ChatList user ID: " + chatlist.getId());
                    }
                }
                
                // Load users từ Realtime Database và bổ sung email từ Firestore
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && user.getIduser() != null) {
                        // Kiểm tra xem user có trong danh sách chat không
                        if (chatUserIds.contains(user.getIduser())) {
                            // Load email từ Firestore IDUser collection
                            loadUserEmail(user, () -> {
                                mUsers.add(user);
                                Log.d("ChatActivity", "Added user to list: " + 
                                    (user.getName() != null && !user.getName().isEmpty() ? user.getName() : user.getEmail()) + 
                                    " (" + user.getIduser() + ")");
                                
                                // Cập nhật adapter sau mỗi user được thêm
                                if (userAdapter != null) {
                                    userAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                }
                
                // Nếu không có users nào được thêm, vẫn ẩn progressBar sau một khoảng thời gian
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (mUsers.isEmpty()) {
                        Log.d("ChatActivity", "No users found after loading");
                    }
                    progressBarChat.setVisibility(View.INVISIBLE);
                }, 2000);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatActivity", "Error loading users: " + error.getMessage());
                progressBarChat.setVisibility(View.INVISIBLE);
            }
        });
    }
    
    /**
     * Load đầy đủ thông tin User từ Firestore (email và tên thật)
     */
    private void loadUserEmail(User user, Runnable onComplete) {
        if (user == null || user.getIduser() == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        
        // Nếu đã có email và tên hợp lệ, không cần load lại
        if (user.getEmail() != null && !user.getEmail().isEmpty() && 
            user.getName() != null && !user.getName().isEmpty() && !user.getName().equals("any name")) {
            if (onComplete != null) onComplete.run();
            return;
        }
        
        // Load email từ Firestore IDUser collection
        db.collection("IDUser")
                .whereEqualTo("iduser", user.getIduser())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String email = null;
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        email = doc.getString("email");
                        if (email != null && !email.isEmpty()) {
                            user.setEmail(email);
                            Log.d("ChatActivity", "Loaded email for user " + user.getIduser() + ": " + email);
                        }
                    }
                    
                    // Load tên thật từ Firestore User/Profile collection
                    db.collection("User").document(user.getIduser())
                            .collection("Profile")
                            .limit(1)
                            .get()
                            .addOnSuccessListener(profileSnapshots -> {
                                if (profileSnapshots != null && !profileSnapshots.isEmpty()) {
                                    QueryDocumentSnapshot profileDoc = (QueryDocumentSnapshot) profileSnapshots.getDocuments().get(0);
                                    String hoten = profileDoc.getString("hoten");
                                    if (hoten != null && !hoten.trim().isEmpty()) {
                                        user.setName(hoten);
                                        Log.d("ChatActivity", "Loaded real name for user " + user.getIduser() + ": " + hoten);
                                    }
                                }
                                if (onComplete != null) onComplete.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ChatActivity", "Error loading profile for user " + user.getIduser() + ": " + e.getMessage());
                                if (onComplete != null) onComplete.run();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Error loading email for user " + user.getIduser() + ": " + e.getMessage());
                    if (onComplete != null) onComplete.run();
                });
    }
    
    /**
     * Load users trực tiếp từ Chats collection nếu ChatList rỗng
     */
    private void loadUsersFromChats(String adminId) {
        Log.d("ChatActivity", "Loading users directly from Chats collection for admin: " + adminId);
        
        // Lấy tất cả tin nhắn có receiver là admin (users gửi cho admin)
        chatHelper.db.collection("Chats")
                .whereEqualTo("receiver", adminId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.Set<String> userIds = new java.util.HashSet<>();
                    
                    Log.d("ChatActivity", "Chats query (receiver=admin) returned " + queryDocumentSnapshots.size() + " messages");
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String sender = document.getString("sender");
                        if (sender != null && !sender.equals(adminId)) {
                            userIds.add(sender);
                            Log.d("ChatActivity", "Found sender: " + sender);
                        }
                    }
                    
                    // Cũng lấy tin nhắn có sender là admin (admin gửi cho users)
                    chatHelper.db.collection("Chats")
                            .whereEqualTo("sender", adminId)
                            .get()
                            .addOnSuccessListener(senderSnapshots -> {
                                Log.d("ChatActivity", "Chats query (sender=admin) returned " + senderSnapshots.size() + " messages");
                                
                                for (QueryDocumentSnapshot document : senderSnapshots) {
                                    String receiver = document.getString("receiver");
                                    if (receiver != null && !receiver.equals(adminId)) {
                                        userIds.add(receiver);
                                        Log.d("ChatActivity", "Found receiver: " + receiver);
                                    }
                                }
                                
                                Log.d("ChatActivity", "Total unique user IDs found: " + userIds.size());
                                
                                // Load users từ Realtime Database
                                if (!userIds.isEmpty()) {
                                    loadUsersByIds(userIds);
                                } else {
                                    progressBarChat.setVisibility(View.INVISIBLE);
                                    Log.d("ChatActivity", "No users found in Chats collection");
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ChatActivity", "Error loading sender messages: " + e.getMessage());
                                // Vẫn tiếp tục với danh sách đã có từ receiver
                                if (!userIds.isEmpty()) {
                                    loadUsersByIds(userIds);
                                } else {
                                    progressBarChat.setVisibility(View.INVISIBLE);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Error loading from Chats (receiver): " + e.getMessage());
                    progressBarChat.setVisibility(View.INVISIBLE);
                });
    }
    
    /**
     * Load users từ Realtime Database theo danh sách IDs
     */
    private void loadUsersByIds(java.util.Set<String> userIds) {
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mUsers.clear();
                int totalUsers = 0;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && user.getIduser() != null && userIds.contains(user.getIduser())) {
                        totalUsers++;
                        // Load email từ Firestore
                        int finalTotalUsers = totalUsers;
                        loadUserEmail(user, () -> {
                            mUsers.add(user);
                            Log.d("ChatActivity", "Added user from Chats: " + 
                                (user.getName() != null && !user.getName().isEmpty() ? user.getName() : user.getEmail()));
                            
                            // Cập nhật adapter sau mỗi user
                            if (userAdapter != null) {
                                userAdapter.notifyDataSetChanged();
                            }
                            
                            // Ẩn progressBar khi đã load xong tất cả
                            if (mUsers.size() == finalTotalUsers) {
                                progressBarChat.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                }
                
                Log.d("ChatActivity", "Loading " + totalUsers + " users from Chats collection");
                
                // Nếu không có users nào, ẩn progressBar ngay
                if (totalUsers == 0) {
                    progressBarChat.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatActivity", "Error loading users by IDs: " + error.getMessage());
                progressBarChat.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void status(String status) {
        final String ADMIN_ID = com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID;
        String userId;
        
        if (who == 1) {
            // Admin - không cần firebaseUser
            userId = ADMIN_ID;
        } else {
            // User - cần firebaseUser
            if (firebaseUser == null) {
                return;
            }
            userId = firebaseUser.getUid();
        }

        reference = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);
        reference.updateChildren(hashMap);
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
        // Remove listener khi activity pause
        if (chatListListener != null) {
            chatListListener.remove();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener khi activity destroy
        if (chatListListener != null) {
            chatListListener.remove();
        }
    }
}