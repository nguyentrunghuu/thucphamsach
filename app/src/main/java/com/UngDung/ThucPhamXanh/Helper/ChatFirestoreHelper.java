package com.UngDung.ThucPhamXanh.Helper;

import android.util.Log;

import com.UngDung.ThucPhamXanh.Models.Chat;
import com.UngDung.ThucPhamXanh.Models.ChatList;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatFirestoreHelper {
    
    private static final String TAG = "ChatFirestoreHelper";
    private static final String COLLECTION_CHATS = "Chats";
    private static final String COLLECTION_CHAT_LISTS = "ChatLists";
    
    public FirebaseFirestore db;
    
    public ChatFirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Gửi tin nhắn mới
     */
    public void sendMessage(String sender, String receiver, String message, OnCompleteListener<DocumentReference> listener) {
        Log.d(TAG, "sendMessage called - sender: " + sender + ", receiver: " + receiver + ", message: " + message);
        
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("sender", sender);
        chatData.put("receiver", receiver);
        chatData.put("message", message);
        chatData.put("isseen", false);
        chatData.put("timestamp", Timestamp.now());
        
        db.collection(COLLECTION_CHATS)
                .add(chatData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Message sent successfully, document ID: " + (task.getResult() != null ? task.getResult().getId() : "null"));
                    } else {
                        Log.e(TAG, "Error sending message: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                    if (listener != null) {
                        listener.onComplete(task);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message (onFailure): " + e.getMessage());
                });
        
        // Cập nhật ChatList cho cả sender và receiver
        updateChatList(sender, receiver);
        updateChatList(receiver, sender);
    }
    
    /**
     * Cập nhật ChatList - danh sách người đã chat
     */
    private void updateChatList(String userId, String chatWithUserId) {
        Map<String, Object> chatListData = new HashMap<>();
        chatListData.put("userId", userId);
        chatListData.put("chatWithUserId", chatWithUserId);
        chatListData.put("lastMessageTime", Timestamp.now());
        
        // Tạo document ID duy nhất cho cặp user
        String docId = userId + "_" + chatWithUserId;
        if (userId.compareTo(chatWithUserId) > 0) {
            docId = chatWithUserId + "_" + userId;
        }
        
        db.collection(COLLECTION_CHAT_LISTS)
                .document(docId)
                .set(chatListData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "ChatList updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating ChatList: " + e.getMessage());
                });
    }
    
    /**
     * Lắng nghe tin nhắn giữa 2 người dùng
     * Firestore không hỗ trợ whereIn với 2 trường, nên ta lắng nghe 2 query riêng và merge kết quả
     */
    public ListenerRegistration[] listenToMessages(String myId, String userId, 
                                                   EventListener<QuerySnapshot> listener) {
        ListenerRegistration[] registrations = new ListenerRegistration[2];
        final List<Chat> allMessages = new ArrayList<>();
        
        // EventListener để merge kết quả từ 2 query
        EventListener<QuerySnapshot> mergeListener = (querySnapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error in message listener: " + error.getMessage());
                if (listener != null) {
                    listener.onEvent(null, error);
                }
                return;
            }
            
            if (querySnapshot != null) {
                // Thêm/xóa tin nhắn từ query này
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Chat chat = documentToChat(doc);
                    // Kiểm tra xem tin nhắn có trong danh sách chưa
                    boolean exists = false;
                    for (int i = 0; i < allMessages.size(); i++) {
                        if (allMessages.get(i).getId() != null && allMessages.get(i).getId().equals(chat.getId())) {
                            // Cập nhật tin nhắn đã tồn tại
                            allMessages.set(i, chat);
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        allMessages.add(chat);
                    }
                }
                
                // Sắp xếp theo timestamp
                allMessages.sort((c1, c2) -> {
                    if (c1.getTimestamp() == null || c2.getTimestamp() == null) {
                        return 0;
                    }
                    return c1.getTimestamp().compareTo(c2.getTimestamp());
                });
                
                // Tạo QuerySnapshot giả để truyền cho listener
                // Vì không thể tạo QuerySnapshot thực, ta sẽ gọi listener với null và để client xử lý
                // Hoặc ta có thể tạo một wrapper
                if (listener != null) {
                    // Gọi listener với querySnapshot gốc (client sẽ tự lọc)
                    listener.onEvent(querySnapshot, null);
                }
            }
        };
        
        // Lắng nghe tin nhắn từ myId đến userId
        registrations[0] = db.collection(COLLECTION_CHATS)
                .whereEqualTo("sender", myId)
                .whereEqualTo("receiver", userId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(mergeListener);
        
        // Lắng nghe tin nhắn từ userId đến myId
        registrations[1] = db.collection(COLLECTION_CHATS)
                .whereEqualTo("sender", userId)
                .whereEqualTo("receiver", myId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(mergeListener);
        
        return registrations;
    }
    
    /**
     * Lấy danh sách người đã chat với user
     */
    public void getChatList(String userId, OnSuccessListener<QuerySnapshot> listener) {
        db.collection(COLLECTION_CHAT_LISTS)
                .whereEqualTo("userId", userId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Chat list loaded for userId: " + userId + ", count: " + queryDocumentSnapshots.size());
                    if (listener != null) {
                        listener.onSuccess(queryDocumentSnapshots);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting chat list: " + e.getMessage());
                    // Trả về empty snapshot nếu có lỗi
                    if (listener != null) {
                        listener.onSuccess(null);
                    }
                });
    }
    
    /**
     * Lấy tin nhắn cuối cùng giữa 2 người
     */
    public void getLastMessage(String userId1, String userId2, 
                               OnSuccessListener<String> listener) {
        // Thử lấy từ cả 2 hướng
        Task<QuerySnapshot> task1 = db.collection(COLLECTION_CHATS)
                .whereEqualTo("sender", userId1)
                .whereEqualTo("receiver", userId2)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get();
        
        Task<QuerySnapshot> task2 = db.collection(COLLECTION_CHATS)
                .whereEqualTo("sender", userId2)
                .whereEqualTo("receiver", userId1)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get();
        
        // Kết hợp kết quả từ cả 2 task
        com.google.android.gms.tasks.Tasks.whenAllSuccess(task1, task2)
                .addOnSuccessListener(results -> {
                    String lastMessage = "No Message";
                    Timestamp lastTime = null;
                    
                    for (Object result : results) {
                        if (result instanceof QuerySnapshot) {
                            QuerySnapshot snapshot = (QuerySnapshot) result;
                            for (QueryDocumentSnapshot doc : snapshot) {
                                Timestamp timestamp = doc.getTimestamp("timestamp");
                                if (lastTime == null || (timestamp != null && timestamp.compareTo(lastTime) > 0)) {
                                    lastTime = timestamp;
                                    lastMessage = doc.getString("message");
                                }
                            }
                        }
                    }
                    
                    listener.onSuccess(lastMessage);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting last message: " + e.getMessage());
                    listener.onSuccess("No Message");
                });
    }
    
    /**
     * Đánh dấu tin nhắn đã đọc
     */
    public void markAsSeen(String chatId) {
        db.collection(COLLECTION_CHATS)
                .document(chatId)
                .update("isseen", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message marked as seen");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking message as seen: " + e.getMessage());
                });
    }
    
    /**
     * Đánh dấu tất cả tin nhắn từ một người là đã đọc
     */
    public void markAllAsSeen(String myId, String userId) {
        db.collection(COLLECTION_CHATS)
                .whereEqualTo("sender", userId)
                .whereEqualTo("receiver", myId)
                .whereEqualTo("isseen", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().update("isseen", true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking all messages as seen: " + e.getMessage());
                });
    }
    
    /**
     * Chuyển đổi DocumentSnapshot thành Chat object
     */
    public static Chat documentToChat(DocumentSnapshot document) {
        Chat chat = new Chat();
        chat.setId(document.getId());
        chat.setSender(document.getString("sender"));
        chat.setReceiver(document.getString("receiver"));
        chat.setMessage(document.getString("message"));
        chat.setIsseen(document.getBoolean("isseen") != null ? document.getBoolean("isseen") : false);
        chat.setTimestamp(document.getTimestamp("timestamp"));
        return chat;
    }
    
    /**
     * Chuyển đổi QueryDocumentSnapshot thành ChatList object
     */
    public static ChatList documentToChatList(QueryDocumentSnapshot document) {
        ChatList chatList = new ChatList();
        String chatWithUserId = document.getString("chatWithUserId");
        if (chatWithUserId != null && !chatWithUserId.isEmpty()) {
            chatList.setId(chatWithUserId);
            Log.d("ChatFirestoreHelper", "Converted ChatList - chatWithUserId: " + chatWithUserId);
        } else {
            Log.w("ChatFirestoreHelper", "chatWithUserId is null or empty in document: " + document.getId());
        }
        return chatList;
    }
}

