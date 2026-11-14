package com.UngDung.ThucPhamXanh.View.Admin;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.UngDung.ThucPhamXanh.BotAI.GeminiClient;
import com.UngDung.ThucPhamXanh.Helper.AdminAiPrompts;
import com.UngDung.ThucPhamXanh.Helper.DashboardStats;
import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.MoneyUtil;
import com.UngDung.ThucPhamXanh.Helper.ProductVisibility;
import com.UngDung.ThucPhamXanh.Helper.SalesTally;
import com.UngDung.ThucPhamXanh.Models.OrderStatus;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

/**
 * Trợ lý AI hỏi đáp số liệu cho admin.
 *
 * Khi mở: đọc 1 lần HoaDon + SanPham (không dùng listener), rút gọn thành văn bản (không có tên /
 * SĐT / địa chỉ khách — xem {@link AdminAiPrompts}) và gửi kèm mỗi câu hỏi tới Gemini làm ngữ cảnh.
 * AI chỉ trả lời, không có quyền ghi dữ liệu. Dữ liệu là bản chụp lúc mở màn hình; muốn số mới thì thoát vào lại.
 */
public class AdminAiChatActivity extends BaseAdminActivity {

    private static final String TAG = "AdminAiChat";
    /** Số lượt hỏi–đáp gần nhất gửi lại cho AI để hiểu câu hỏi nối tiếp. */
    private static final int MAX_HISTORY_TURNS = 6;

    private static final String[] SUGGESTIONS = {
            "Doanh thu tháng này?",
            "Đang có bao nhiêu đơn chờ xử lý?",
            "Sản phẩm nào sắp hết hạn?",
            "Sản phẩm nào sắp hết hàng?",
            "Top 5 sản phẩm bán chạy nhất?",
    };

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private LinearLayout messages;
    private ScrollView scroll;
    private EditText edtQuestion;
    private ImageButton btnSend;
    private TextView typingBubble;

    private String systemPrompt;      // null cho tới khi tải xong dữ liệu
    private boolean waiting;          // đang chờ AI trả lời
    private boolean loadStarted;
    private final List<String[]> history = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ai_chat);

        setupAdminToolbar("Trợ lý AI", "Hỏi đáp số liệu cửa hàng");
        messages = findViewById(R.id.ai_messages);
        scroll = findViewById(R.id.ai_scroll);
        edtQuestion = findViewById(R.id.edt_ai_question);
        btnSend = findViewById(R.id.btn_ai_send);

        applyInputBarInsets(findViewById(R.id.ai_input_bar));
        setupSuggestions();

        btnSend.setOnClickListener(v -> send(edtQuestion.getText().toString()));
        edtQuestion.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send(edtQuestion.getText().toString());
                return true;
            }
            return false;
        });

        if (!GeminiClient.hasKey()) {
            addMessage("Chưa cấu hình khóa AI (GEMINI_API_KEY trong local.properties) nên trợ lý chưa dùng được.", false);
            return;
        }
        loadData();
    }

    // ------------------------------------------------------------------ nạp dữ liệu

    private void loadData() {
        if (loadStarted) return;
        loadStarted = true;
        typingBubble = addMessage("Đang đọc dữ liệu cửa hàng…", false);
        setInputEnabled(false);

        db.collection("HoaDon").get().addOnSuccessListener(orderSnap -> {
            List<AdminAiPrompts.OrderLine> orders = new ArrayList<>();
            List<QueryDocumentSnapshot> docs = new ArrayList<>();
            for (QueryDocumentSnapshot d : orderSnap) docs.add(d);
            // Đơn mới nhất lên trước để khi bị cắt bớt thì mất đơn cũ
            Collections.sort(docs, (a, b) -> Long.compare(
                    DashboardStats.sortKey(FirestoreMapper.str(b, "ngaydat")),
                    DashboardStats.sortKey(FirestoreMapper.str(a, "ngaydat"))));
            for (QueryDocumentSnapshot d : docs) {
                orders.add(new AdminAiPrompts.OrderLine(d.getId(), FirestoreMapper.str(d, "ngaydat"),
                        FirestoreMapper.numOrNull(d, "trangthai"), MoneyUtil.parseVnd(d.get("tongtien"))));
            }

            // Mỗi đơn: trạng thái + UID, để biết cần đọc chi tiết ở ChitietHoaDon/{uid}/ALL của ai
            Map<String, Long> statusByOrder = new HashMap<>();
            Set<String> uids = new HashSet<>();
            for (QueryDocumentSnapshot d : docs) {
                Long st = FirestoreMapper.numOrNull(d, "trangthai");
                statusByOrder.put(d.getId(), st);
                String uid = FirestoreMapper.str(d, "UID");
                if (!uid.isEmpty() && st != null && st != OrderStatus.DA_HUY) uids.add(uid);
            }
            loadSales(orders, statusByOrder, uids);
        }).addOnFailureListener(this::onLoadFailed);
    }

    /**
     * SanPham.daban không có trong dữ liệu thật → tính số lượng bán từ chi tiết các đơn
     * (đọc ChitietHoaDon/{uid}/ALL của từng khách có đơn không hủy, chỉ giữ dòng thuộc đơn hợp lệ).
     */
    private void loadSales(List<AdminAiPrompts.OrderLine> orders, Map<String, Long> statusByOrder, Set<String> uids) {
        SalesTally tally = new SalesTally();
        if (uids.isEmpty()) { loadProducts(orders, tally, 0); return; }
        int[] pending = {uids.size()};
        int[] failed = {0};
        for (String uid : uids) {
            db.collection("ChitietHoaDon").document(uid).collection("ALL").get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (QueryDocumentSnapshot line : task.getResult()) {
                                String orderId = FirestoreMapper.str(line, "id_hoadon");
                                if (!statusByOrder.containsKey(orderId)) continue; // dòng của đơn không còn
                                tally.add(statusByOrder.get(orderId), FirestoreMapper.str(line, "id_product"),
                                        FirestoreMapper.num(line, "soluong"));
                            }
                        } else {
                            Log.e(TAG, "Không đọc được chi tiết đơn của " + uid, task.getException());
                            failed[0]++;
                        }
                        if (--pending[0] == 0) loadProducts(orders, tally, failed[0]);
                    });
        }
    }

    private void loadProducts(List<AdminAiPrompts.OrderLine> orders, SalesTally tally, int failedCustomers) {
        db.collection("SanPham").get().addOnSuccessListener(prodSnap -> {
            List<AdminAiPrompts.ProductLine> products = new ArrayList<>();
            for (DocumentSnapshot d : prodSnap) {
                if (ProductVisibility.isHidden(d)) continue; // đã xóa mềm
                products.add(new AdminAiPrompts.ProductLine(FirestoreMapper.str(d, "tensp"),
                        FirestoreMapper.str(d, "loaisp"), FirestoreMapper.num(d, "giatien"),
                        FirestoreMapper.num(d, "soluong"), FirestoreMapper.str(d, "hansudung"),
                        tally.ordered(d.getId()), tally.delivered(d.getId())));
            }
            String note = failedCustomers == 0 ? ""
                    : "Không đọc được chi tiết đơn của " + failedCustomers + " khách, nên số lượng bán có thể thiếu.";
            systemPrompt = AdminAiPrompts.assistantSystemPrompt(LocalDate.now(), orders, products, note);
            removeTyping();
            addMessage("Xin chào! Tôi đã đọc " + orders.size() + " đơn hàng và " + products.size()
                    + " sản phẩm. Bạn muốn hỏi gì?", false);
            setInputEnabled(true);
        }).addOnFailureListener(this::onLoadFailed);
    }

    private void onLoadFailed(Exception e) {
        Log.e(TAG, "Lỗi đọc dữ liệu", e);
        removeTyping();
        addMessage("Không đọc được dữ liệu cửa hàng. Kiểm tra Internet rồi mở lại màn này.", false);
    }

    // ------------------------------------------------------------------ hỏi đáp

    private void setupSuggestions() {
        ChipGroup group = findViewById(R.id.ai_suggestions);
        for (String s : SUGGESTIONS) {
            Chip chip = new Chip(this);
            chip.setText(s);
            chip.setCheckable(false);
            chip.setOnClickListener(v -> send(s));
            group.addView(chip);
        }
    }

    private void send(String raw) {
        String question = raw == null ? "" : raw.trim();
        if (question.isEmpty() || waiting || systemPrompt == null) return;

        edtQuestion.setText("");
        addMessage(question, true);
        typingBubble = addMessage("AI đang trả lời…", false);
        waiting = true;
        setInputEnabled(false);

        GeminiClient.ask(systemPrompt, new ArrayList<>(history), question, new GeminiClient.Result() {
            @Override
            public void onSuccess(String text) {
                if (isFinishing() || isDestroyed()) return;
                String answer = AdminAiPrompts.stripMarkdown(text);
                if (answer.isEmpty()) answer = "AI không trả về nội dung, bạn thử hỏi lại nhé.";
                history.add(new String[]{"user", question});
                history.add(new String[]{"assistant", answer});
                while (history.size() > MAX_HISTORY_TURNS * 2) history.remove(0);
                finishTurn(answer);
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                finishTurn(message);
            }
        });
    }

    private void finishTurn(String answer) {
        removeTyping();
        addMessage(answer, false);
        waiting = false;
        setInputEnabled(true);
    }

    private void setInputEnabled(boolean enabled) {
        btnSend.setEnabled(enabled);
        btnSend.setAlpha(enabled ? 1f : 0.4f);
        edtQuestion.setEnabled(enabled);
    }

    // ------------------------------------------------------------------ giao diện

    private TextView addMessage(String text, boolean mine) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_admin_ai_message, messages, false);
        TextView tv = row.findViewById(R.id.ai_msg_text);
        tv.setText(text);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) tv.getLayoutParams();
        lp.gravity = mine ? Gravity.END : Gravity.START;
        tv.setLayoutParams(lp);
        int bg = mine ? R.color.admin_primary : R.color.admin_surface;
        tv.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, bg)));
        tv.setTextColor(ContextCompat.getColor(this, mine ? R.color.white : R.color.admin_text));
        messages.addView(row);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        return tv;
    }

    private void removeTyping() {
        if (typingBubble != null && typingBubble.getParent() != null) {
            messages.removeView((View) typingBubble.getParent());
        }
        typingBubble = null;
    }

    /** Ô nhập chừa chỗ cho thanh điều hướng và bàn phím (edge-to-edge trên Android 15+). */
    private void applyInputBarInsets(View bar) {
        final int base = bar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(bar, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    base + Math.max(sys.bottom, ime.bottom));
            return insets;
        });
    }
}
