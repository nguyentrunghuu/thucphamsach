package com.UngDung.ThucPhamXanh.BotAI;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.icu.text.NumberFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Models.HoaDon;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IHoaDon;
import com.UngDung.ThucPhamXanh.my_interface.IProduct;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatBotActivity extends AppCompatActivity implements IHoaDon, IProduct {

    private RecyclerView recyclerViewChat;
    private EditText edtMessage;
    private ImageButton btnSend;
    private ImageView imgBack;
    private TextView tvChatTitle;
    private ProgressBar progressBarChat;

    private List<ChatMessage> chatMessages;
    private ChatBotAdapter chatBotAdapter;

    private FirebaseUser firebaseUser;
    private DatabaseReference reference;
    private FirebaseFirestore firestore;

    // Constants for detecting message intents
    private static final String PRODUCT_QUERY = "sản phẩm";
    private static final String ORDER_STATUS = "đơn hàng";
    private static final String THANK_YOU = "cảm ơn";
    private static final String ORDER_INTENT = "đặt hàng";
    private static final String CANCEL_INTENT = "hủy";

    // Product and order data
    private List<Product> productList = new ArrayList<>();
    private List<HoaDon> orderList = new ArrayList<>();

    // For conversation history
    private boolean isHistoryLoaded = false;
    private static final int MAX_HISTORY_MESSAGES = 10; // Limit context for API
    private List<ConversationMessage> conversationHistory = new ArrayList<>();

    // Order process
    private OrderProcess orderProcess = new OrderProcess();
    private NotificationService notificationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);

        initializeViews();
        initializeChat();
        setupListeners();

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Load product and order data
        loadProducts();
        loadUserOrders();

        // Load conversation history
        loadConversationHistory();
        initializeNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (notificationService != null) {
            notificationService.startListening();
        }

        // Check for any new messages when activity becomes active
        loadNewMessages();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (notificationService != null) {
            notificationService.stopListening();
        }
    }

    private void loadNewMessages() {
        if (firebaseUser == null) return;

        // Tìm thời gian của tin nhắn mới nhất đã tải
        long lastMessageTime = 0;
        if (!chatMessages.isEmpty()) {
            lastMessageTime = chatMessages.get(chatMessages.size() - 1).getTimestamp();
        }

        // Tải các tin nhắn mới hơn thời gian tin nhắn cuối cùng
        reference.child("BotChats").child(firebaseUser.getUid())
                .orderByChild("timestamp")
                .startAfter(lastMessageTime)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            List<ChatMessage> newMessages = new ArrayList<>();

                            for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                                String message = messageSnapshot.child("message").getValue(String.class);
                                String sender = messageSnapshot.child("sender").getValue(String.class);
                                long timestamp = Long.parseLong(messageSnapshot.getKey());

                                boolean isUser = sender != null && !sender.equals("bot");

                                // Tạo tin nhắn mới
                                ChatMessage chatMessage = new ChatMessage(message, isUser);
                                chatMessage.setTimestamp(timestamp);
                                newMessages.add(chatMessage);

                                // Thêm vào lịch sử hội thoại cho OpenAI
                                String role = isUser ? "user" : "assistant";
                                conversationHistory.add(new ConversationMessage(role, message));
                            }

                            // Sắp xếp tin nhắn theo thời gian
                            Collections.sort(newMessages, (m1, m2) ->
                                    Long.compare(m1.getTimestamp(), m2.getTimestamp()));

                            // Thêm vào danh sách tin nhắn
                            chatMessages.addAll(newMessages);
                            chatBotAdapter.notifyDataSetChanged();

                            if (!newMessages.isEmpty()) {
                                recyclerViewChat.smoothScrollToPosition(chatMessages.size() - 1);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("ChatBot", "Lỗi khi tải tin nhắn mới", error.toException());
                    }
                });
    }


    private void initializeNotifications() {
        notificationService = new NotificationService(this);
    }

    private void initializeViews() {
        recyclerViewChat = findViewById(R.id.recycler_view_chat_bot);
        edtMessage = findViewById(R.id.edt_message_chat_bot);
        btnSend = findViewById(R.id.btn_send_chat_bot);
        imgBack = findViewById(R.id.img_back_chat_bot);
        tvChatTitle = findViewById(R.id.tv_chat_bot_title);
        progressBarChat = findViewById(R.id.progress_bar_chat_bot);
    }

    private void initializeChat() {
        recyclerViewChat.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(linearLayoutManager);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference();

        chatMessages = new ArrayList<>();
        chatBotAdapter = new ChatBotAdapter(ChatBotActivity.this, chatMessages);
        recyclerViewChat.setAdapter(chatBotAdapter);
    }

    /** Bấm vào một gợi ý nhanh: điền câu hỏi rồi gửi luôn. */
    private void setupQuickSuggestions() {
        android.view.ViewGroup group = findViewById(R.id.layout_quick_suggestions);
        if (group == null) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (!(child instanceof TextView)) continue;
            child.setOnClickListener(v -> {
                // Bỏ emoji ở đầu, chỉ gửi phần chữ
                String text = ((TextView) v).getText().toString().replaceFirst("^\\S+\\s+", "");
                edtMessage.setText(text);
                edtMessage.setSelection(text.length());
                btnSend.performClick();
            });
        }
    }

    private void setupListeners() {
        setupQuickSuggestions();
        btnSend.setOnClickListener(view -> {
            sendMessage();
        });

        // Handle Enter key press
        edtMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        imgBack.setOnClickListener(view -> finish());
    }

    private void sendMessage() {
        String message = edtMessage.getText().toString().trim();
        if (!message.isEmpty()) {
            sendUserMessage(message);
            edtMessage.setText("");
            // Clear focus and hide keyboard
            edtMessage.clearFocus();
        } else {
            Toast.makeText(ChatBotActivity.this, "Vui lòng nhập tin nhắn", Toast.LENGTH_SHORT).show();
        }
    }

    // Load conversation history from Firebase
    private void loadConversationHistory() {
        progressBarChat.setVisibility(View.VISIBLE);

        if (firebaseUser == null) {
            // If not logged in, show welcome message
            sendWelcomeMessage();
            return;
        }

        String userId = firebaseUser.getUid();
        reference.child("BotChats").child(userId)
                .orderByChild("timestamp")
                .limitToLast(50) // Load last 50 messages
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            chatMessages.clear();
                            conversationHistory.clear();

                            List<ChatMessage> tempMessages = new ArrayList<>();
                            for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                                String message = messageSnapshot.child("message").getValue(String.class);
                                String sender = messageSnapshot.child("sender").getValue(String.class);
                                long timestamp = Long.parseLong(messageSnapshot.getKey());

                                boolean isUser = sender != null && !sender.equals("bot");

                                // Add to UI messages
                                ChatMessage chatMessage = new ChatMessage(message, isUser);
                                chatMessage.setTimestamp(timestamp);
                                tempMessages.add(chatMessage);

                                // Add to conversation history for OpenAI
                                String role = isUser ? "user" : "assistant";
                                conversationHistory.add(new ConversationMessage(role, message));
                            }

                            // Sort by timestamp
                            Collections.sort(tempMessages, new Comparator<ChatMessage>() {
                                @Override
                                public int compare(ChatMessage m1, ChatMessage m2) {
                                    return Long.compare(m1.getTimestamp(), m2.getTimestamp());
                                }
                            });

                            // Add to UI
                            chatMessages.addAll(tempMessages);
                            chatBotAdapter.notifyDataSetChanged();

                            // Scroll to bottom after loading messages
                            recyclerViewChat.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (chatMessages.size() > 0) {
                                        recyclerViewChat.smoothScrollToPosition(chatMessages.size() - 1);
                                    }
                                }
                            });

                            if (chatMessages.size() > 0) {
                                isHistoryLoaded = true;
                            } else {
                                // No history, show welcome message
                                sendWelcomeMessage();
                            }
                        } else {
                            // No history, show welcome message
                            sendWelcomeMessage();
                        }
                        progressBarChat.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        sendWelcomeMessage();
                        progressBarChat.setVisibility(View.GONE);
                    }
                });
    }

    private void sendWelcomeMessage() {
        isHistoryLoaded = true;
        sendBotMessage("Xin chào! Tôi là trợ lý ảo. Tôi có thể giúp bạn tư vấn sản phẩm, kiểm tra đơn hàng, đặt hàng hoặc trả lời các câu hỏi khác của bạn. Bạn có thể hỏi tôi về sản phẩm, kiểm tra đơn hàng hoặc cần tôi tư vấn gì không?");
    }

    private void sendUserMessage(String message) {
        // Make sure history is loaded before allowing new messages
        if (!isHistoryLoaded) {
            Toast.makeText(this, "Đang tải lịch sử trò chuyện, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add message to UI
        ChatMessage userMessage = new ChatMessage(message, true);
        chatMessages.add(userMessage);
        int position = chatMessages.size() - 1;
        chatBotAdapter.notifyItemInserted(position);
        
        // Scroll to bottom after a short delay to ensure layout is updated
        recyclerViewChat.post(new Runnable() {
            @Override
            public void run() {
                if (chatMessages.size() > 0) {
                    recyclerViewChat.smoothScrollToPosition(chatMessages.size() - 1);
                }
            }
        });

        // Save message to Firebase
        saveMessageToFirebase(message, false);

        // Add to conversation history for OpenAI
        conversationHistory.add(new ConversationMessage("user", message));

        // Process message to determine intent
        processUserMessage(message);
    }

    private void sendBotMessage(String message) {
        // Add message to UI
        ChatMessage botMessage = new ChatMessage(message, false);
        chatMessages.add(botMessage);
        int position = chatMessages.size() - 1;
        chatBotAdapter.notifyItemInserted(position);
        
        // Scroll to bottom after a short delay to ensure layout is updated
        recyclerViewChat.post(new Runnable() {
            @Override
            public void run() {
                if (chatMessages.size() > 0) {
                    recyclerViewChat.smoothScrollToPosition(chatMessages.size() - 1);
                }
            }
        });

        // Save message to Firebase
        saveMessageToFirebase(message, true);

        // Add to conversation history for OpenAI
        conversationHistory.add(new ConversationMessage("assistant", message));

        // Hide progress indicator
        progressBarChat.setVisibility(View.GONE);
    }

    private void saveMessageToFirebase(String message, boolean isBot) {
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            String timestamp = String.valueOf(System.currentTimeMillis());

            HashMap<String, Object> messageData = new HashMap<>();
            messageData.put("message", message);
            messageData.put("sender", isBot ? "bot" : userId);
            messageData.put("timestamp", timestamp);

            reference.child("BotChats").child(userId).child(timestamp).setValue(messageData);
        }
    }

    private void processUserMessage(String message) {
        // Show progress indicator while processing
        progressBarChat.setVisibility(View.VISIBLE);

        // Convert message to lowercase for easier pattern matching
        String lowerMessage = message.toLowerCase(Locale.getDefault());

        // Check if user is in order process
        if (orderProcess.getCurrentState() != OrderProcess.OrderState.IDLE) {
            handleOrderProcess(message);
            return;
        }

        // Check message intent for new requests
        if (lowerMessage.contains(ORDER_INTENT)) {
            startOrderProcess();
        } else if (lowerMessage.contains(PRODUCT_QUERY)) {
            if (productList.isEmpty()) {
                // If product data is not loaded yet, just use OpenAI
                getOpenAIResponse(message);
            } else {
                handleProductQuery(lowerMessage);
            }
        } else if (lowerMessage.contains(ORDER_STATUS)) {
            handleOrderStatusQuery();
        } else if (lowerMessage.contains(THANK_YOU)) {
            sendBotMessage("Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi! Chúc bạn một ngày tốt lành.");
            progressBarChat.setVisibility(View.GONE);
        } else {
            // Use OpenAI API for general conversation
            getOpenAIResponse(message);
        }
    }

    // Bắt đầu quá trình đặt hàng
    private void startOrderProcess() {
        if (firebaseUser == null) {
            sendBotMessage("Bạn cần đăng nhập để đặt hàng. Vui lòng đăng nhập và thử lại sau.");
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        if (productList.isEmpty()) {
            sendBotMessage("Đang tải danh sách sản phẩm, vui lòng thử lại sau...");
            loadProducts();
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        // Get user name from Firebase
        reference.child("Users").child(firebaseUser.getUid()).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String customerName = snapshot.exists() ? snapshot.getValue(String.class) : "Khách hàng";

                        if (customerName == null || customerName.isEmpty()) {
                            customerName = "Khách hàng";
                        }

                        // Start the order process
                        orderProcess.startNewOrder(customerName);

                        // Show product list
                        StringBuilder productInfo = new StringBuilder("Danh sách sản phẩm:\n");
                        for (int i = 0; i < productList.size(); i++) {
                            Product product = productList.get(i);
                            productInfo.append(String.format("%d. %s - %,d đ\n",
                                    i + 1, product.getTensp(), product.getGiatien()));
                        }

                        sendBotMessage("Bắt đầu quá trình đặt hàng. " + productInfo.toString() +
                                "\n" + orderProcess.getCurrentStateQuestion());
                        progressBarChat.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        orderProcess.startNewOrder("Khách hàng");
                        sendBotMessage("Bắt đầu quá trình đặt hàng.\n" + orderProcess.getCurrentStateQuestion());
                        progressBarChat.setVisibility(View.GONE);
                    }
                });
    }

    // Xử lý tin nhắn trong quá trình đặt hàng
    // Phương thức handleOrderProcess cập nhật với trạng thái ENTERING_NAME
    private void handleOrderProcess(String message) {
        switch (orderProcess.getCurrentState()) {
            case SELECTING_PRODUCTS:
                handleProductSelection(message);
                break;
            case CONFIRMING_CART:
                handleCartConfirmation(message);
                break;
            case ENTERING_NAME:
                handleNameInput(message);
                break;
            case ENTERING_ADDRESS:
                handleAddressInput(message);
                break;
            case ENTERING_PHONE:
                handlePhoneInput(message);
                break;
            case SELECTING_PAYMENT:
                handlePaymentSelection(message);
                break;
            case CONFIRMING_ORDER:
                handleOrderConfirmation(message);
                break;
            default:
                getOpenAIResponse(message);
                break;
        }
    }

    // Xử lý chọn sản phẩm
    private void handleProductSelection(String message) {
        // Hủy đặt hàng nếu người dùng yêu cầu
        if (message.toLowerCase().contains(CANCEL_INTENT)) {
            orderProcess.setCurrentState(OrderProcess.OrderState.IDLE);
            sendBotMessage("Đã hủy quá trình đặt hàng. Bạn cần giúp gì khác không?");
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        // Kiểm tra nếu người dùng đang cập nhật số lượng sản phẩm đã thêm
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("cập nhật") || lowerMessage.contains("sửa số lượng") || lowerMessage.contains("thay đổi số lượng")) {
            handleQuantityUpdate(message);
            return;
        }

        Product selectedProduct = null;
        int quantity = 1; // Mặc định là 1

        // Mẫu regex cải tiến để tìm số lượng gần với tên sản phẩm
        // Ví dụ: "tôi muốn 3 bánh mì" hoặc "bánh mì 3 cái" hoặc "bánh mì số lượng 3"
        Pattern quantityPattern = Pattern.compile("\\b(\\d+)\\s*(cái|chiếc|hộp|thùng|kg|gói|lon|chai)?\\b");

        // Kiểm tra xem có phải là số từ 1-N không (chọn theo số thứ tự)
        try {
            int productIndex = Integer.parseInt(message.trim());
            if (productIndex > 0 && productIndex <= productList.size()) {
                selectedProduct = productList.get(productIndex - 1);
            }
        } catch (NumberFormatException e) {
            // Không phải số, tìm theo tên sản phẩm
            for (Product product : productList) {
                if (product.getTensp() != null &&
                        lowerMessage.contains(product.getTensp().toLowerCase())) {
                    selectedProduct = product;

                    // Tìm số lượng trong tin nhắn gần với tên sản phẩm
                    Matcher matcher = quantityPattern.matcher(message);
                    while (matcher.find()) {
                        try {
                            int possibleQuantity = Integer.parseInt(matcher.group(1));
                            if (possibleQuantity > 0 && possibleQuantity < 100) { // Giới hạn hợp lý
                                quantity = possibleQuantity;
                                break;
                            }
                        } catch (NumberFormatException ex) {
                            // Giữ số lượng mặc định là 1
                        }
                    }
                    break;
                }
            }
        }

        if (selectedProduct != null) {
            // Kiểm tra xem có còn đủ hàng không
            if (selectedProduct.getSoluong() < quantity) {
                sendBotMessage(String.format(
                        "Rất tiếc, sản phẩm %s chỉ còn %d sản phẩm trong kho. Vui lòng chọn số lượng ít hơn hoặc sản phẩm khác.",
                        selectedProduct.getTensp(), selectedProduct.getSoluong()));
                progressBarChat.setVisibility(View.GONE);
                return;
            }

            // Nếu sản phẩm đã có trong giỏ hàng, cập nhật số lượng
            boolean isUpdate = false;
            for (Map.Entry<Product, Integer> entry : orderProcess.getCart().entrySet()) {
                if (entry.getKey().getId().equals(selectedProduct.getId())) {
                    int newQuantity = entry.getValue() + quantity;
                    orderProcess.updateProductQuantity(entry.getKey(), newQuantity);
                    sendBotMessage(String.format(
                            "Đã cập nhật %s trong giỏ hàng thành %d sản phẩm.\n\nBạn muốn thêm sản phẩm nào nữa không?\nNếu đã đủ, hãy gõ 'Xác nhận' để tiến hành đặt hàng.",
                            selectedProduct.getTensp(), newQuantity));
                    isUpdate = true;
                    break;
                }
            }

            // Nếu là sản phẩm mới, thêm vào giỏ hàng
            if (!isUpdate) {
                orderProcess.addProductToCart(selectedProduct, quantity);
                sendBotMessage(String.format(
                        "Đã thêm %d %s vào giỏ hàng.\n\nBạn muốn thêm sản phẩm nào nữa không?\nNếu muốn thay đổi số lượng, hãy gõ 'cập nhật [tên sản phẩm] số lượng [số lượng mới]'\nNếu đã đủ, hãy gõ 'Xác nhận' để tiến hành đặt hàng.",
                        quantity, selectedProduct.getTensp()));
            }

            // Nếu đã chọn ít nhất 1 sản phẩm, chuyển sang xác nhận giỏ hàng
            if (orderProcess.getCurrentState() == OrderProcess.OrderState.SELECTING_PRODUCTS &&
                    orderProcess.getCart().size() > 0) {
                orderProcess.moveToNextState();
            }
        } else {
            sendBotMessage("Không tìm thấy sản phẩm này. Vui lòng chọn lại.\n\nDanh sách sản phẩm:");

            // Hiển thị lại danh sách sản phẩm
            StringBuilder productInfo = new StringBuilder();
            for (int i = 0; i < productList.size(); i++) {
                Product product = productList.get(i);
                productInfo.append(String.format("%d. %s - %,d đ (còn %d sản phẩm)\n",
                        i + 1, product.getTensp(), product.getGiatien(), product.getSoluong()));
            }
            sendBotMessage(productInfo.toString());
        }
        progressBarChat.setVisibility(View.GONE);
    }

    // Phương thức xử lý yêu cầu cập nhật số lượng sản phẩm
    // Phương thức xử lý yêu cầu cập nhật số lượng sản phẩm
    private void handleQuantityUpdate(String message) {
        // Tìm tên sản phẩm và số lượng mới trong tin nhắn
        String lowerMessage = message.toLowerCase();

        // Tìm số lượng mới
        Pattern quantityPattern = Pattern.compile("\\b(\\d+)\\b");
        Matcher quantityMatcher = quantityPattern.matcher(lowerMessage);

        List<Integer> numbers = new ArrayList<>();
        while (quantityMatcher.find()) {
            try {
                numbers.add(Integer.parseInt(quantityMatcher.group(1)));
            } catch (NumberFormatException e) {
                // Bỏ qua nếu không phải số
            }
        }

        int newQuantity = 0;
        if (!numbers.isEmpty()) {
            // Lấy số cuối cùng làm số lượng mới
            newQuantity = numbers.get(numbers.size() - 1);
        } else {
            sendBotMessage("Vui lòng chỉ định số lượng cần cập nhật. Ví dụ: 'cập nhật bánh mì số lượng 3'");
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        // Xóa từ khóa "cập nhật" và "số lượng" để lấy tên sản phẩm
        String productNameSearch = lowerMessage
                .replaceAll("cập nhật", "")
                .replaceAll("số lượng", "")
                .replaceAll("\\d+", "")
                .trim();

        Log.d("ProductSearch", "Đang tìm sản phẩm với từ khóa: '" + productNameSearch + "'");

        // Tìm sản phẩm cần cập nhật
        Product productToUpdate = null;
        Product cartProduct = null;
        Map.Entry<Product, Integer> cartEntry = null;

        // Đầu tiên tìm trong giỏ hàng
        double bestMatchScore = 0;
        for (Map.Entry<Product, Integer> entry : orderProcess.getCart().entrySet()) {
            if (entry.getKey().getTensp() == null) continue;

            String productName = entry.getKey().getTensp().toLowerCase();

            // Kiểm tra xem productNameSearch có là substring của productName không
            if (productName.contains(productNameSearch) || productNameSearch.contains(productName)) {
                double score = calculateSimilarity(productNameSearch, productName);
                if (score > bestMatchScore) {
                    bestMatchScore = score;
                    cartProduct = entry.getKey();
                    cartEntry = entry;
                }
            }
        }

        // Ngưỡng điểm tương đồng (40%)
        if (bestMatchScore >= 0.4) {
            productToUpdate = cartProduct;
        }

        // Nếu không tìm thấy trong giỏ hàng, tìm trong danh sách sản phẩm
        if (productToUpdate == null) {
            bestMatchScore = 0;
            for (Product product : productList) {
                if (product.getTensp() == null) continue;

                String productName = product.getTensp().toLowerCase();

                // Kiểm tra xem productNameSearch có là substring của productName không
                if (productName.contains(productNameSearch) || productNameSearch.contains(productName)) {
                    double score = calculateSimilarity(productNameSearch, productName);
                    if (score > bestMatchScore) {
                        bestMatchScore = score;
                        productToUpdate = product;
                    }
                }
            }

            // Kiểm tra ngưỡng điểm tương đồng thấp hơn cho sản phẩm mới (30%)
            if (bestMatchScore < 0.3) {
                productToUpdate = null;
            }
        }

        if (productToUpdate != null) {
            // Thông báo sản phẩm đã tìm thấy để người dùng xác nhận
            String foundMessage = String.format("Đã tìm thấy sản phẩm: %s", productToUpdate.getTensp());

            // Kiểm tra số lượng tồn kho
            if (productToUpdate.getSoluong() < newQuantity) {
                sendBotMessage(foundMessage + String.format(
                        "\nRất tiếc, sản phẩm này chỉ còn %d sản phẩm trong kho. Vui lòng chọn số lượng ít hơn.",
                        productToUpdate.getSoluong()));
                progressBarChat.setVisibility(View.GONE);
                return;
            }

            if (cartProduct != null) {
                // Cập nhật số lượng sản phẩm đã có trong giỏ hàng
                if (newQuantity <= 0) {
                    orderProcess.removeProductFromCart(cartProduct);
                    sendBotMessage(foundMessage + String.format("\nĐã xóa %s khỏi giỏ hàng.", productToUpdate.getTensp()));
                } else {
                    orderProcess.updateProductQuantity(cartProduct, newQuantity);
                    sendBotMessage(foundMessage + String.format("\nĐã cập nhật số lượng thành %d.", newQuantity));
                }
            } else {
                // Thêm sản phẩm mới vào giỏ hàng
                if (newQuantity > 0) {
                    orderProcess.addProductToCart(productToUpdate, newQuantity);
                    sendBotMessage(foundMessage + String.format("\nĐã thêm %d sản phẩm vào giỏ hàng.", newQuantity));
                } else {
                    sendBotMessage(foundMessage + "\nSố lượng sản phẩm phải lớn hơn 0.");
                }
            }

            // Hiển thị giỏ hàng hiện tại
            sendBotMessage(orderProcess.getCartSummary() + "\n\nBạn muốn tiếp tục mua sắm hay xác nhận giỏ hàng?");
        } else {
            // Hiển thị gợi ý các sản phẩm có thể cập nhật
            StringBuilder suggestions = new StringBuilder("Không tìm thấy sản phẩm \"" + productNameSearch + "\".\n\nCác sản phẩm trong giỏ hàng của bạn:\n");

            if (orderProcess.getCart().isEmpty()) {
                suggestions = new StringBuilder("Giỏ hàng của bạn đang trống. Vui lòng thêm sản phẩm trước khi cập nhật số lượng.\n\nCác sản phẩm hiện có:\n");
                // Liệt kê một số sản phẩm có sẵn
                int count = 0;
                for (Product product : productList) {
                    if (count++ < 5) {
                        suggestions.append("- ").append(product.getTensp()).append("\n");
                    }
                }
                suggestions.append("...\n");
            } else {
                // Liệt kê các sản phẩm trong giỏ hàng
                for (Map.Entry<Product, Integer> entry : orderProcess.getCart().entrySet()) {
                    suggestions.append("- ").append(entry.getKey().getTensp())
                            .append(" (hiện tại: ").append(entry.getValue()).append(")\n");
                }
            }

            suggestions.append("\nVui lòng sử dụng đúng tên sản phẩm. Ví dụ: \"cập nhật Quýt giống Úc số lượng 4\"");
            sendBotMessage(suggestions.toString());
        }

        progressBarChat.setVisibility(View.GONE);
    }

    // Tính độ tương đồng giữa hai chuỗi (Dùng thuật toán Levenshtein distance)
    private double calculateSimilarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) {
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0; // Cả hai chuỗi đều rỗng
        }

        // Tính toán khoảng cách Levenshtein
        int distance = levenshteinDistance(s1, s2);
        return (longerLength - distance) / (double) longerLength;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        return dp[s1.length()][s2.length()];
    }

    // Xử lý xác nhận giỏ hàng
    // Phương thức handleCartConfirmation cập nhật
    private void handleCartConfirmation(String message) {
        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("xác nhận") || lowerMessage.contains("tiếp tục") || lowerMessage.contains("ok")) {
            // Chuyển sang bước nhập tên
            orderProcess.moveToNextState();
            sendBotMessage(orderProcess.getCurrentStateQuestion());
        } else if (lowerMessage.contains(CANCEL_INTENT)) {
            // Hủy đặt hàng
            orderProcess.setCurrentState(OrderProcess.OrderState.IDLE);
            sendBotMessage("Đã hủy quá trình đặt hàng. Bạn cần giúp gì khác không?");
        } else {
            // Xem như chọn thêm sản phẩm
            handleProductSelection(message);
        }
        progressBarChat.setVisibility(View.GONE);
    }

    // Xử lý nhập địa chỉ
    private void handleAddressInput(String message) {
        if (message.toLowerCase().contains(CANCEL_INTENT)) {
            orderProcess.setCurrentState(OrderProcess.OrderState.IDLE);
            sendBotMessage("Đã hủy quá trình đặt hàng. Bạn cần giúp gì khác không?");
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        // Lưu địa chỉ
        orderProcess.setAddress(message);

        // Chuyển sang bước nhập số điện thoại
        orderProcess.moveToNextState();
        sendBotMessage(orderProcess.getCurrentStateQuestion());
        progressBarChat.setVisibility(View.GONE);
    }

    // Xử lý nhập số điện thoại
    private void handlePhoneInput(String message) {
        if (message.toLowerCase().contains(CANCEL_INTENT)) {
            orderProcess.setCurrentState(OrderProcess.OrderState.IDLE);
            sendBotMessage("Đã hủy quá trình đặt hàng. Bạn cần giúp gì khác không?");
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        // Kiểm tra số điện thoại hợp lệ
        String phoneNumber = message.replaceAll("[^0-9]", "");
        if (phoneNumber.length() < 10 || phoneNumber.length() > 11) {
            sendBotMessage("Số điện thoại không hợp lệ. Vui lòng nhập lại (VD: 0912345678):");
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        // Lưu số điện thoại
        orderProcess.setPhone(phoneNumber);

        // Chuyển sang bước chọn phương thức thanh toán
        orderProcess.moveToNextState();
        sendBotMessage(orderProcess.getCurrentStateQuestion());
        progressBarChat.setVisibility(View.GONE);
    }

    // Xử lý chọn phương thức thanh toán
    private void handlePaymentSelection(String message) {
        if (message.toLowerCase().contains(CANCEL_INTENT)) {
            orderProcess.setCurrentState(OrderProcess.OrderState.IDLE);
            sendBotMessage("Đã hủy quá trình đặt hàng. Bạn cần giúp gì khác không?");
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        String paymentMethod;
        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("1") || lowerMessage.contains("tiền mặt") || lowerMessage.contains("cod")) {
            paymentMethod = "Tiền mặt khi nhận hàng (COD)";
        } else if (lowerMessage.contains("2") || lowerMessage.contains("chuyển khoản") || lowerMessage.contains("ngân hàng")) {
            paymentMethod = "Chuyển khoản ngân hàng";
        } else {
            sendBotMessage("Vui lòng chọn phương thức thanh toán:\n1. Tiền mặt khi nhận hàng (COD)\n2. Chuyển khoản ngân hàng");
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        // Lưu phương thức thanh toán
        orderProcess.setPaymentMethod(paymentMethod);

        // Chuyển sang bước xác nhận đơn hàng
        orderProcess.moveToNextState();
        sendBotMessage(orderProcess.getCurrentStateQuestion());
        progressBarChat.setVisibility(View.GONE);
    }

    // Xử lý xác nhận đơn hàng
    private void handleOrderConfirmation(String message) {
        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("đồng ý") || lowerMessage.contains("xác nhận") || lowerMessage.contains("ok")) {
            // Lưu đơn hàng vào Firebase
            createOrder();
        } else if (lowerMessage.contains(CANCEL_INTENT) || lowerMessage.contains("không")) {
            // Hủy đặt hàng
            orderProcess.setCurrentState(OrderProcess.OrderState.IDLE);
            sendBotMessage("Đã hủy đơn hàng. Bạn cần giúp gì khác không?");
            progressBarChat.setVisibility(View.GONE);
        } else {
            // Yêu cầu xác nhận lại
            sendBotMessage("Vui lòng xác nhận đơn hàng bằng cách gõ 'Đồng ý' hoặc 'Hủy' để hủy đơn hàng.");
            progressBarChat.setVisibility(View.GONE);
        }
    }

    // Tạo đơn hàng trong Firestore
    // Tạo đơn hàng trong Firestore với chi tiết hóa đơn
    // Sửa lỗi trong phương thức createOrder
    // Cập nhật phương thức createOrder() trong ChatBotActivity.java

    private void createOrder() {
        if (firebaseUser == null) {
            sendBotMessage("Bạn cần đăng nhập để đặt hàng. Vui lòng đăng nhập và thử lại sau.");
            orderProcess.setCurrentState(OrderProcess.OrderState.IDLE);
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        // Kiểm tra giỏ hàng
        if (orderProcess.isCartEmpty()) {
            sendBotMessage("Giỏ hàng của bạn đang trống. Vui lòng thêm sản phẩm trước khi đặt hàng.");
            orderProcess.setCurrentState(OrderProcess.OrderState.SELECTING_PRODUCTS);
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        // Tạo dữ liệu đơn hàng
        HashMap<String, Object> orderMap = orderProcess.createOrderMap(firebaseUser.getUid());

        // Lưu đơn hàng vào Firestore
        firestore.collection("HoaDon")
                .add(orderMap)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        String orderId = documentReference.getId();

                        // Tạo chuỗi mô tả sản phẩm cho tin nhắn
                        StringBuilder sanphamDesc = new StringBuilder();

                        // Kiểm tra số lượng sản phẩm còn lại trước khi đặt hàng
                        boolean insufficientStock = false;

                        // Danh sách sản phẩm cần cập nhật số lượng
                        List<Map.Entry<Product, Integer>> productsToUpdate = new ArrayList<>(orderProcess.getCart().entrySet());

                        for (Map.Entry<Product, Integer> entry : productsToUpdate) {
                            Product product = entry.getKey();
                            int quantity = entry.getValue();

                            // Kiểm tra số lượng trong kho
                            if (product.getSoluong() < quantity) {
                                insufficientStock = true;
                                sendBotMessage("Rất tiếc, sản phẩm " + product.getTensp() +
                                        " chỉ còn " + product.getSoluong() + " trong kho. Vui lòng điều chỉnh số lượng.");

                                // Xóa đơn hàng đã tạo
                                firestore.collection("HoaDon").document(orderId).delete();
                                progressBarChat.setVisibility(View.GONE);
                                return;
                            }

                            // Thêm vào mô tả sản phẩm
                            sanphamDesc.append(product.getTensp()).append(" x ").append(quantity).append("\n");

                            // Tạo chi tiết hóa đơn
                            HashMap<String, Object> orderDetail = new HashMap<>();
                            orderDetail.put("id_hoadon", orderId);
                            orderDetail.put("id_product", product.getId());
                            orderDetail.put("soluong", quantity);
                            orderDetail.put("giatien", product.getGiatien());
                            orderDetail.put("tongtien", product.getGiatien() * quantity);

                            // Lưu chi tiết hóa đơn
                            firestore.collection("ChitietHoaDon")
                                    .document(firebaseUser.getUid())
                                    .collection("ALL")
                                    .add(orderDetail)
                                    .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentReference> task) {
                                            if (task.isSuccessful()) {
                                                Log.d("OrderDetail", "Chi tiết đơn hàng đã được lưu");

                                                // Cập nhật số lượng sản phẩm trong kho
                                                updateProductStock(product.getId(), quantity);
                                            } else {
                                                Log.e("OrderDetail", "Lỗi khi lưu chi tiết đơn hàng", task.getException());
                                            }
                                        }
                                    });
                        }

                        if (insufficientStock) {
                            return; // Đã xử lý lỗi ở trên
                        }

                        // Tạo thông tin đơn hàng chi tiết
                        String orderInfo = createOrderSummary(orderId, sanphamDesc.toString());

                        // Gửi tin nhắn thông báo đặt hàng thành công
                        sendOrderConfirmationMessage(orderId, sanphamDesc.toString());

                        // Sử dụng NotificationUtils để gửi thông báo
                        NotificationUtils.sendOrderSuccessNotification(
                                ChatBotActivity.this,
                                orderId,
                                orderInfo
                        );

                        // Thông báo thành công
                        sendBotMessage("Đặt hàng thành công! Mã đơn hàng của bạn là: " + orderId.substring(0, 8) +
                                "\n\nCám ơn bạn đã mua hàng. Chúng tôi sẽ xử lý đơn hàng của bạn sớm nhất có thể.");

                        // Reset trạng thái đặt hàng
                        orderProcess.setCurrentState(OrderProcess.OrderState.IDLE);

                        // Refresh danh sách đơn hàng
                        orderList.clear();
                        loadUserOrders();

                        progressBarChat.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    sendBotMessage("Đã xảy ra lỗi khi đặt hàng. Vui lòng thử lại sau.");
                    orderProcess.setCurrentState(OrderProcess.OrderState.IDLE);
                    progressBarChat.setVisibility(View.GONE);
                });
    }

    // Phương thức tạo nội dung tóm tắt đơn hàng
    private String createOrderSummary(String orderId, String productList) {
        StringBuilder summary = new StringBuilder();
        summary.append("Mã đơn hàng: ").append(orderId.substring(0, 8)).append("\n");
        summary.append("Ngày đặt: ").append(orderProcess.getCurrentDateString()).append("\n");
        summary.append("Sản phẩm:\n").append(productList).append("\n");
        summary.append("Người nhận: ").append(orderProcess.getCustomerName()).append("\n");
        summary.append("Địa chỉ: ").append(orderProcess.getAddress()).append("\n");
        summary.append("SĐT: ").append(orderProcess.getPhone()).append("\n");
        summary.append("Phương thức thanh toán: ").append(orderProcess.getPaymentMethod()).append("\n");
        summary.append("Tổng tiền: ").append(NumberFormat.getInstance().format(orderProcess.getTotalAmount())).append(" đ");

        return summary.toString();
    }

    // Gửi tin nhắn thông báo đặt hàng thành công
    private void sendOrderConfirmationMessage(String orderId, String productDesc) {
        if (firebaseUser == null) return;

        // Realtime Database
        // Khi đặt hàng xong thì người dùng sẽ được gửi 1 tin nhắn tự động từ tk admin
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        HashMap<String, Object> map = new HashMap<>();
        map.put("sender", com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID);  // ID của admin
        map.put("receiver", firebaseUser.getUid());

        // Tạo nội dung tin nhắn
        String mess = "Đơn hàng của bạn: " + "\n" + productDesc + "\n"
                + "Mã đơn hàng: " + orderId.substring(0, 8) + "\n"
                + "Ngày đặt: " + orderProcess.getCurrentDateString() + "\n"
                + "Địa chỉ: " + orderProcess.getAddress() + "\n"
                + "SĐT: " + orderProcess.getPhone() + "\n"
                + "Người nhận: " + orderProcess.getCustomerName() + "\n"
                + "Phương thức thanh toán: " + orderProcess.getPaymentMethod() + "\n"
                + "Tổng tiền: " + NumberFormat.getInstance().format(orderProcess.getTotalAmount()) + " đ";

        map.put("message", mess);
        map.put("isseen", false);

        // Gửi qua Firestore "Chats" (màn chat đọc từ Firestore, không phải Realtime Database)
        new com.UngDung.ThucPhamXanh.Helper.ChatFirestoreHelper().sendMessage(
                com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID, (String) map.get("receiver"), (String) map.get("message"), null);

        // Add user to chat fragment
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(firebaseUser.getUid())
                .child(com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()){
                    chatRef.child("id").setValue(com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("OrderConfirmation", "Lỗi khi cập nhật danh sách chat", error.toException());
            }
        });
    }

    // Lưu chi tiết đơn hàng
    private void saveOrderDetails(String orderId) {
        List<HashMap<String, Object>> orderDetails = orderProcess.createOrderDetailsMap(orderId);

        // Lưu từng chi tiết đơn hàng
        for (HashMap<String, Object> detail : orderDetails) {
            firestore.collection("ChiTietHoaDon").add(detail);

            // Cập nhật số lượng sản phẩm
            String productId = (String) detail.get("id_sanpham");
            int quantity = (int) detail.get("soluong");
            updateProductStock(productId, quantity);
        }
    }

    // Cập nhật số lượng sản phẩm trong kho
    // Cập nhật số lượng sản phẩm trong kho
    private void updateProductStock(String productId, int orderedQuantity) {
        if (productId == null || productId.isEmpty() || orderedQuantity <= 0) {
            Log.e("UpdateStock", "Tham số không hợp lệ: productId=" + productId + ", quantity=" + orderedQuantity);
            return;
        }

        firestore.collection("SanPham").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long currentStock = documentSnapshot.getLong("soluong");
                        if (currentStock != null) {
                            int newStock = (int) (currentStock - orderedQuantity);
                            if (newStock < 0) {
                                Log.w("UpdateStock", "Số lượng tồn kho âm cho sản phẩm " + productId);
                                newStock = 0;
                            }

                            // Cập nhật trong Firestore
                            int finalNewStock = newStock;
                            firestore.collection("SanPham").document(productId)
                                    // Trừ tồn kho + cộng số đã bán (dùng cho sắp xếp "Bán chạy")
                                    .update("soluong", newStock, "daban", com.google.firebase.firestore.FieldValue.increment(orderedQuantity))
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("UpdateStock", "Đã cập nhật số lượng cho sản phẩm " + productId + ": " + finalNewStock);

                                        // Cập nhật số lượng trong danh sách sản phẩm cục bộ
                                        for (int i = 0; i < productList.size(); i++) {
                                            Product product = productList.get(i);
                                            if (product.getId().equals(productId)) {
                                                product.setSoluong((long) finalNewStock);
                                                break;
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("UpdateStock", "Lỗi khi cập nhật số lượng sản phẩm", e);
                                    });
                        } else {
                            Log.e("UpdateStock", "Số lượng sản phẩm là null cho " + productId);
                        }
                    } else {
                        Log.e("UpdateStock", "Không tìm thấy sản phẩm " + productId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UpdateStock", "Lỗi khi đọc dữ liệu sản phẩm", e);
                });
    }

    private void handleProductQuery(String message) {
        if (productList.isEmpty()) {
            sendBotMessage("Đang tải dữ liệu sản phẩm, vui lòng thử lại sau...");
            loadProducts(); // Try to load products again
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        // First try to enhance the product query with OpenAI
        StringBuilder productInfo = new StringBuilder();
        for (Product product : productList) {
            productInfo.append(String.format("Sản phẩm: %s - Giá: %d VND - Loại: %s - Mô tả: %s\n",
                    product.getTensp(),
                    product.getGiatien(),
                    product.getLoaisp(),
                    product.getMota()));
        }

        // Create a special message with product data for OpenAI
        String enhancedMessage = message + "\n\nThông tin sản phẩm của cửa hàng:\n" + productInfo.toString() +
                "\nHãy tư vấn sản phẩm phù hợp dựa trên thông tin trên. Nếu tin nhắn không liên quan đến tư vấn sản phẩm, hãy trả lời bình thường.";

        // Pass to OpenAI for smart product consultation
        getOpenAIResponse(enhancedMessage);
    }

    private void handleOrderStatusQuery() {
        if (orderList.isEmpty()) {
            sendBotMessage("Đang tải dữ liệu đơn hàng, vui lòng thử lại sau...");
            loadUserOrders(); // Try to load orders again
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        StringBuilder orderStatus = new StringBuilder("Đơn hàng của bạn:\n");
        boolean hasOrders = false;

        for (HoaDon order : orderList) {
            hasOrders = true;

            String status = com.UngDung.ThucPhamXanh.Models.OrderStatus.label(order.getType());

            String orderId = order.getId();
            if (orderId != null && orderId.length() > 8) {
                orderId = orderId.substring(0, 8);
            }

            orderStatus.append(String.format("- Mã đơn: %s\n  Ngày đặt: %s\n  Tổng tiền: %s VND\n  Trạng thái: %s\n\n",
                    orderId, // First 8 chars of order ID or the whole ID if shorter
                    order.getNgaydat(),
                    order.getTongtien(),
                    status
            ));
        }

        if (!hasOrders) {
            orderStatus = new StringBuilder("Bạn chưa có đơn hàng nào.");
        }

        sendBotMessage(orderStatus.toString());
        progressBarChat.setVisibility(View.GONE);
    }

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/";
    private static final String GEMINI_API_KEY = com.UngDung.ThucPhamXanh.BuildConfig.GEMINI_API_KEY; // đọc từ local.properties

    private void getOpenAIResponse(String message) {
        // Log basic request info (no API key)
        Log.d("GEMINI_REQUEST", "Starting request to Gemini API");

        // Chuyển danh sách sản phẩm và đơn hàng sang JSON để gửi cho AI
        Gson gson = new Gson();
        String productsJson = gson.toJson(productList);
        String ordersJson = gson.toJson(orderList);

        // Xây dựng System Prompt với dữ liệu thực tế
        String systemPrompt = "Bạn là Trợ lý ảo AI thông minh của ứng dụng \"Thực Phẩm Xanh\". Nhiệm vụ của bạn là tư vấn sản phẩm và hỗ trợ tra cứu đơn hàng dựa trên dữ liệu Firebase thực tế được truyền vào ngữ cảnh (context).\n" +
                "\n" +
                "--- TẬP DỮ LIỆU THỰC TẾ TỪ FIREBASE ---\n" +
                "[DANH SÁCH SẢN PHẨM]: " + productsJson + "\n" +
                "[LỊCH SỬ ĐƠN HÀNG]: " + ordersJson + "\n" +
                "---------------------------------------\n" +
                "\n" +
                "QUY TRẮC NGUYÊN TẮC VÀ XỬ LÝ:\n" +
                "\n" +
                "1. TRACỨU ĐƠN HÀNG:\n" +
                "- Khi người dùng gửi Mã đơn hàng, Số điện thoại hoặc Tên người nhận:\n" +
                "  + Tìm kiếm trong [LỊCH SỬ ĐƠN HÀNG].\n" +
                "  + Nếu tìm thấy: Trả về trạng thái đơn (1: Đang xử lý, 2: Đang giao, 3: Đã giao, 4: Đã hủy), ngày đặt, danh sách món hàng và tổng tiền.\n" +
                "  + Nếu KHÔNG tìm thấy: Báo nhẹ nhàng rằng chưa thấy thông tin đơn hàng tương ứng và đề nghị người dùng kiểm tra lại SĐT hoặc mã đơn.\n" +
                "\n" +
                "2. TƯ VẤN SẢN PHẨM:\n" +
                "- Chỉ giới thiệu các sản phẩm CÓ TRONG [DANH SÁCH SẢN PHẨM].\n" +
                "- Tuyệt đối không tự bịa ra sản phẩm hoặc giá tiền không tồn tại trong dữ liệu Firebase.\n" +
                "- Cung cấp rõ ràng: Tên sản phẩm, Giá bán, Tình trạng còn hàng (nếu có) và công dụng/lợi ích sức khỏe phù hợp với nhu cầu người dùng.\n" +
                "\n" +
                "3. PHONG CÁCH TRẢ LỜI:\n" +
                "- Thân thiện, lịch sự, sử dụng icon nhẹ nhàng (😊, 🥗, 📦).\n" +
                "- Câu từ ngắn gọn, rõ ràng, dễ nhìn trên giao diện màn hình điện thoại mobile.";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GEMINI_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GeminiAPI geminiAPI = retrofit.create(GeminiAPI.class);

        // Create request with system instruction and history
        GeminiRequest request = new GeminiRequest(systemPrompt);

        // Add conversation history
        int historySize = conversationHistory.size();
        int startIndex = Math.max(0, historySize - MAX_HISTORY_MESSAGES);

        for (int i = startIndex; i < historySize - 1; i++) {
            ConversationMessage historyMessage = conversationHistory.get(i);
            request.addContent(historyMessage.getRole(), historyMessage.getContent());
        }

        // Add current message
        request.addContent("user", message);

        Call<GeminiResponse> call = geminiAPI.getChatCompletion(GEMINI_API_KEY, request);

        long startTime = System.currentTimeMillis();

        call.enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                long duration = System.currentTimeMillis() - startTime;
                Log.d("GEMINI_RESPONSE", "HTTP_CODE: " + response.code() + " (took " + duration + "ms)");

                if (response.isSuccessful() && response.body() != null) {
                    GeminiResponse geminiResponse = response.body();
                    String botReply = geminiResponse.getResponseText();
                    sendBotMessage(botReply);
                } else {
                    String errorMessage;
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e("GEMINI_ERROR", "Could not read error body", e);
                    }

                    Log.e("GEMINI_ERROR", "Code: " + response.code() + " - Body: " + errorBody);

                    switch (response.code()) {
                        case 400:
                            errorMessage = "Request gửi lên không đúng định dạng (400). Hãy kiểm tra Key.";
                            break;
                        case 401:
                            errorMessage = "API key không hợp lệ hoặc đã hết hiệu lực (401).";
                            break;
                        case 403:
                            errorMessage = "Bạn không có quyền truy cập hoặc Key sai (403).";
                            break;
                        case 429:
                            errorMessage = "Hệ thống đang bận hoặc bạn đã dùng quá giới hạn miễn phí (429).";
                            break;
                        default:
                            errorMessage = "Lỗi hệ thống (" + response.code() + "). Vui lòng thử lại sau.";
                            break;
                    }
                    sendBotMessage(errorMessage);
                }
                progressBarChat.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                Log.e("GEMINI_ERROR", "API call failed: " + t.getMessage(), t);
                sendBotMessage("Không thể kết nối máy chủ Gemini. Vui lòng kiểm tra Internet.");
                progressBarChat.setVisibility(View.GONE);
            }
        });
    }

    // Thêm phương thức này vào ChatBotActivity.java
    private void handleNameInput(String message) {
        if (message.toLowerCase().contains(CANCEL_INTENT)) {
            orderProcess.setCurrentState(OrderProcess.OrderState.IDLE);
            sendBotMessage("Đã hủy quá trình đặt hàng. Bạn cần giúp gì khác không?");
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        // Chuẩn hóa tên (loại bỏ khoảng trắng thừa và ký tự đặc biệt)
        String name = message.trim();

        // Kiểm tra tên có hợp lệ không
        if (name.length() < 3) {
            sendBotMessage("Tên quá ngắn. Vui lòng nhập đầy đủ họ tên người nhận hàng (ít nhất 3 ký tự):");
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        // Kiểm tra xem tên có chứa quá nhiều số không
        int digitCount = 0;
        for (char c : name.toCharArray()) {
            if (Character.isDigit(c)) {
                digitCount++;
            }
        }

        if (digitCount > name.length() / 2) {
            sendBotMessage("Tên không hợp lệ (chứa quá nhiều số). Vui lòng nhập đúng họ tên người nhận:");
            progressBarChat.setVisibility(View.GONE);
            return;
        }

        // Thực hiện xử lý tên phù hợp (viết hoa chữ cái đầu của mỗi từ)
        String[] words = name.split("\\s+");
        StringBuilder formattedName = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                formattedName.append(Character.toUpperCase(word.charAt(0)));

                if (word.length() > 1) {
                    formattedName.append(word.substring(1).toLowerCase());
                }

                formattedName.append(" ");
            }
        }

        // Lưu tên người nhận
        orderProcess.setCustomerName(formattedName.toString().trim());

        // Chuyển sang bước nhập địa chỉ
        orderProcess.moveToNextState();
        sendBotMessage(orderProcess.getCurrentStateQuestion());
        progressBarChat.setVisibility(View.GONE);
    }

    private void loadProducts() {
        Product productModel = new Product(this);
        productModel.HandleGetDataProduct();
    }

    private void loadUserOrders() {
        if (firebaseUser != null) {
            HoaDon orderModel = new HoaDon(this);
            orderModel.HandleReadData();
        }
    }

    // IProduct interface implementation
    @Override
    public void getDataHD(String id, String uid, String ghichu, String diachi, String hoten, String ngaydat, String phuongthuc, String sdt, String tongtien, Long type) {
        if (firebaseUser != null && uid.equals(firebaseUser.getUid())) {
            HoaDon order = new HoaDon(id, uid, ghichu, diachi, hoten, ngaydat, phuongthuc, sdt, tongtien, type);
            orderList.add(order);
        }
    }

    @Override
    public void OnSucess() {
        // Not needed for this implementation
    }

    @Override
    public void OnFail() {
        // Not needed for this implementation
    }

    @Override
    public void getDataProduct(String id, String ten, Long gia, String hinhanh, String loaisp, String mota, Long soluong, String hansudung, Long type, String trongluong) {
        Product product = new Product(id, ten, gia, hinhanh, loaisp, mota, soluong, hansudung, type, trongluong);
        productList.add(product);
    }

    // For conversation history
    static class ConversationMessage {
        private String role;
        private String content;

        public ConversationMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}