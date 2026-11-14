package com.UngDung.ThucPhamXanh.View;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.CustomerOrders;
import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.MoneyUtil;
import com.UngDung.ThucPhamXanh.Helper.OrderLineMapper;
import com.UngDung.ThucPhamXanh.Helper.OrderMath;
import com.UngDung.ThucPhamXanh.Models.HoaDon;
import com.UngDung.ThucPhamXanh.Models.OrderStatus;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chi tiết đơn hàng của khách. Cập nhật trực tiếp: tiến trình đơn, shipper, lý do hủy tự đổi khi
 * cửa hàng / shipper xử lý. Có hủy đơn (chọn lý do), mua lại, đánh giá từng sản phẩm của đơn đã giao.
 *
 * Mở bằng {@link #EXTRA_ORDER_ID} (id document HoaDon).
 */
public class CTHDActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "order_id";
    private static final String TAG = "CTHDActivity";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private TextView tvStatusBig, tvStatusMessage, tvCode, tvItemsTitle, tvItemsEmpty, tvTotal, tvPaymentNote;
    private TextView tvCancelTitle, tvCancelDetail;
    private View header, cardProgress, cardCancelled, progressItems, barActions;
    private LinearLayout stepper, boxDelivery, boxItems, boxPayment;
    private MaterialButton btnCancel, btnReorder;

    private String orderId;
    private AdminOrder order;
    private List<OrderMath.Line> lines = new ArrayList<>();
    private boolean linesRequested = false;
    private boolean linesLoaded = false;
    private ListenerRegistration orderReg;

    private final Set<String> reviewed = new HashSet<>();
    private final Set<String> reviewChecked = new HashSet<>();
    private final Map<String, MaterialButton> reviewButtons = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cthdactivity);

        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        if (orderId == null) { // tương thích Intent cũ (extra "HD" là Models/HoaDon)
            Object hd = getIntent().getSerializableExtra("HD");
            if (hd instanceof HoaDon) orderId = ((HoaDon) hd).getId();
        }
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        applyInsets();

        findViewById(R.id.img_back_cthd).setOnClickListener(v -> finish());
        findViewById(R.id.row_code).setOnClickListener(v -> copyCode());
        btnCancel.setOnClickListener(v -> {
            if (order != null) OrderUi.confirmCancel(this, order.id, null); // trang tự cập nhật (live)
        });
        btnReorder.setOnClickListener(v -> {
            if (!linesLoaded || lines.isEmpty()) {
                Toast.makeText(this, "Đang tải sản phẩm, vui lòng thử lại sau giây lát", Toast.LENGTH_SHORT).show();
            } else {
                OrderUi.reorder(this, lines);
            }
        });
    }

    private void bindViews() {
        tvStatusBig = findViewById(R.id.tv_status_big);
        tvStatusMessage = findViewById(R.id.tv_status_message);
        tvCode = findViewById(R.id.tv_ma_cthd);
        tvItemsTitle = findViewById(R.id.tv_items_title);
        tvItemsEmpty = findViewById(R.id.tv_items_empty);
        tvTotal = findViewById(R.id.tv_total_cthd);
        tvPaymentNote = findViewById(R.id.tv_payment_note);
        tvCancelTitle = findViewById(R.id.tv_cancel_title);
        tvCancelDetail = findViewById(R.id.tv_cancel_detail);
        header = findViewById(R.id.header_cthd);
        cardProgress = findViewById(R.id.card_progress);
        cardCancelled = findViewById(R.id.card_cancelled);
        progressItems = findViewById(R.id.progress_items);
        barActions = findViewById(R.id.bar_actions);
        stepper = findViewById(R.id.stepper);
        boxDelivery = findViewById(R.id.box_delivery_rows);
        boxItems = findViewById(R.id.box_items);
        boxPayment = findViewById(R.id.box_payment_rows);
        btnCancel = findViewById(R.id.btn_capnhat_cthd);
        btnReorder = findViewById(R.id.btn_reorder_cthd);
    }

    /** targetSdk 35: màn hình tràn viền → chừa chỗ cho thanh điều hướng ở thanh nút phía dưới. */
    private void applyInsets() {
        final int pad = dp(14);
        ViewCompat.setOnApplyWindowInsetsListener(barActions, (v, insets) -> {
            int nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(pad, pad, pad, pad + nav);
            return insets;
        });
        ViewCompat.requestApplyInsets(barActions);
    }

    // ---------------------------------------------------------------- dữ liệu

    @Override
    protected void onStart() {
        super.onStart();
        orderReg = db.collection("HoaDon").document(orderId).addSnapshotListener((doc, e) -> {
            if (isFinishing()) return;
            if (e != null) {
                Log.e(TAG, "Không đọc được đơn " + orderId, e);
                Toast.makeText(this, "Không tải được đơn hàng, vui lòng kiểm tra mạng", Toast.LENGTH_LONG).show();
                return;
            }
            if (doc == null || !doc.exists()) {
                Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            order = FirestoreMapper.toAdminOrder(doc);
            bindOrder();
            if (!linesRequested) loadLines();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (orderReg != null) {
            orderReg.remove();
            orderReg = null;
        }
    }

    /** Chi tiết đơn nằm ở ChitietHoaDon/{uid}/ALL (uid của chủ đơn). */
    private void loadLines() {
        String uid = order.uid;
        if (uid == null || uid.isEmpty()) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        }
        if (uid.isEmpty()) {
            showItemsFailed();
            return;
        }
        linesRequested = true;
        db.collection("ChitietHoaDon").document(uid).collection("ALL")
                .whereEqualTo("id_hoadon", orderId).get()
                .addOnSuccessListener(snap -> {
                    if (isFinishing()) return;
                    List<DocumentSnapshot> docs = new ArrayList<>();
                    Set<String> pids = new LinkedHashSet<>();
                    for (QueryDocumentSnapshot d : snap) {
                        docs.add(d);
                        String pid = FirestoreMapper.str(d, "id_product");
                        if (!pid.isEmpty()) pids.add(pid);
                    }
                    loadProductsThenBind(docs, new ArrayList<>(pids));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Không đọc được chi tiết đơn", e);
                    linesRequested = false;
                    showItemsFailed();
                });
    }

    private void loadProductsThenBind(List<DocumentSnapshot> lineDocs, List<String> pids) {
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String pid : pids) tasks.add(db.collection("SanPham").document(pid).get());
        Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
            if (isFinishing()) return;
            Map<String, DocumentSnapshot> products = new HashMap<>();
            for (int i = 0; i < pids.size(); i++) {
                Task<DocumentSnapshot> task = tasks.get(i);
                if (task.isSuccessful() && task.getResult() != null) products.put(pids.get(i), task.getResult());
            }
            List<OrderMath.Line> built = new ArrayList<>();
            for (DocumentSnapshot d : lineDocs) {
                built.add(OrderLineMapper.toLine(d, products.get(FirestoreMapper.str(d, "id_product"))));
            }
            lines = built;
            linesLoaded = true;
            progressItems.setVisibility(View.GONE);
            bindItems();
            bindPayment();
        });
    }

    private void showItemsFailed() {
        progressItems.setVisibility(View.GONE);
        tvItemsEmpty.setVisibility(View.VISIBLE);
    }

    // ---------------------------------------------------------------- hiển thị đơn

    private void bindOrder() {
        AdminOrder o = order;
        boolean cancelled = o.status == OrderStatus.DA_HUY;

        tvStatusBig.setText(OrderStatus.label(o.status));
        tvStatusMessage.setText(CustomerOrders.statusMessage(o));
        tvCode.setText("Mã đơn: " + o.displayCode());
        tintHeader(cancelled);

        cardProgress.setVisibility(cancelled ? View.GONE : View.VISIBLE);
        cardCancelled.setVisibility(cancelled ? View.VISIBLE : View.GONE);
        if (cancelled) {
            boolean byAdmin = "admin".equals(o.cancelledBy);
            tvCancelTitle.setText(byAdmin ? "Cửa hàng đã hủy đơn này" : "Bạn đã hủy đơn này");
            tvCancelDetail.setText(o.cancelReason.trim().isEmpty()
                    ? "Sản phẩm trong đơn đã được hoàn lại kho."
                    : "Lý do: " + o.cancelReason.trim());
        } else {
            bindStepper(o.status);
        }

        bindDelivery();
        bindPayment();
        bindActions();
        refreshReviewButtons();
    }

    /** Đơn hủy → đầu trang đỏ; còn lại xanh lá. */
    private void tintHeader(boolean cancelled) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                cancelled ? new int[]{0xFF8E1B1B, 0xFFD32F2F} : new int[]{0xFF1B5E20, 0xFF43A047});
        float r = dp(28);
        g.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        header.setBackground(g);
    }

    private void bindStepper(long status) {
        stepper.removeAllViews();
        int[] states = CustomerOrders.stageStates(status);
        for (int i = 0; i < states.length; i++) {
            stepper.addView(buildStep(i, states));
        }
    }

    private View buildStep(int i, int[] states) {
        int green = 0xFF2E7D32, grey = c(R.color.ui_stroke);
        int greenText = c(R.color.ui_green_text);
        int state = states[i];

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        col.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));

        View left = new View(this);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, dp(3), 1f));
        left.setBackgroundColor(i == 0 ? Color.TRANSPARENT : (state != CustomerOrders.STAGE_TODO ? green : grey));
        row.addView(left);

        TextView dot = new TextView(this);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(30), dp(30));
        dot.setLayoutParams(dotLp);
        dot.setGravity(Gravity.CENTER);
        dot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        dot.setTypeface(dot.getTypeface(), android.graphics.Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        if (state == CustomerOrders.STAGE_DONE) {
            bg.setColor(green);
            dot.setText("✓");
            dot.setTextColor(Color.WHITE);
        } else if (state == CustomerOrders.STAGE_CURRENT) {
            bg.setColor(c(R.color.ui_surface));
            bg.setStroke(dp(3), greenText);
            dot.setText(String.valueOf(i + 1));
            dot.setTextColor(greenText);
        } else {
            bg.setColor(c(R.color.ui_surface_alt));
            bg.setStroke(dp(2), grey);
            dot.setText(String.valueOf(i + 1));
            dot.setTextColor(c(R.color.ui_text_hint));
        }
        dot.setBackground(bg);
        row.addView(dot);

        View right = new View(this);
        right.setLayoutParams(new LinearLayout.LayoutParams(0, dp(3), 1f));
        right.setBackgroundColor(i == states.length - 1 ? Color.TRANSPARENT
                : (state == CustomerOrders.STAGE_DONE ? green : grey));
        row.addView(right);

        TextView label = new TextView(this);
        label.setText(CustomerOrders.STAGE_LABELS[i]);
        label.setGravity(Gravity.CENTER);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        label.setPadding(0, dp(8), 0, 0);
        boolean active = state != CustomerOrders.STAGE_TODO;
        label.setTextColor(active ? c(R.color.ui_text) : c(R.color.ui_text_hint));
        if (state == CustomerOrders.STAGE_CURRENT) label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);
        col.addView(label);
        return col;
    }

    private void bindDelivery() {
        AdminOrder o = order;
        boxDelivery.removeAllViews();
        String who = o.name.trim();
        if (!o.phone.trim().isEmpty()) who = who.isEmpty() ? o.phone.trim() : who + " · " + o.phone.trim();
        addInfoRow(R.drawable.ic_admin_users, "Người nhận", who);
        addInfoRow(R.drawable.ic_admin_calendar, "Ngày đặt hàng", o.date);
        addInfoRow(R.drawable.ic_ui_location, "Địa chỉ nhận hàng", o.address);
        addInfoRow(R.drawable.ic_admin_calendar, "Thời gian giao dự kiến", CustomerOrders.deliverySchedule(o));
        if (o.status == OrderStatus.DANG_GIAO || o.status == OrderStatus.DA_GIAO) {
            addInfoRow(R.drawable.ic_ui_truck, "Shipper", o.shipperName);
        }
        addInfoRow(R.drawable.ic_admin_order, "Ghi chú", o.note);
    }

    private void addInfoRow(int icon, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        View row = LayoutInflater.from(this).inflate(R.layout.item_order_info_row, boxDelivery, false);
        ((ImageView) row.findViewById(R.id.img_info_icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.tv_info_label)).setText(label);
        ((TextView) row.findViewById(R.id.tv_info_value)).setText(value.trim());
        boxDelivery.addView(row);
    }

    // ---------------------------------------------------------------- sản phẩm & thanh toán

    private void bindItems() {
        boxItems.removeAllViews();
        reviewButtons.clear();
        tvItemsEmpty.setVisibility(lines.isEmpty() ? View.VISIBLE : View.GONE);
        long qty = OrderMath.totalQuantity(lines);
        tvItemsTitle.setText(lines.isEmpty() ? "Sản phẩm" : "Sản phẩm (" + qty + ")");

        for (int i = 0; i < lines.size(); i++) {
            OrderMath.Line l = lines.get(i);
            View row = LayoutInflater.from(this).inflate(R.layout.item_order_line, boxItems, false);
            ((TextView) row.findViewById(R.id.tv_line_name)).setText(l.name);
            ((TextView) row.findViewById(R.id.tv_line_unit)).setText(
                    MoneyUtil.formatVnd(l.unitPrice) + " × " + l.quantity + (l.priceIsCurrent ? " *" : ""));
            ((TextView) row.findViewById(R.id.tv_line_total)).setText(MoneyUtil.formatVnd(l.lineTotal()));
            ImageView img = row.findViewById(R.id.img_line);
            if (l.image != null && !l.image.trim().isEmpty()) Picasso.get().load(l.image.trim()).into(img);

            MaterialButton btn = row.findViewById(R.id.btn_line_review);
            if (!l.productId.isEmpty()) {
                reviewButtons.put(l.productId, btn);
                btn.setOnClickListener(v -> showReviewDialog(l));
            }
            if (i == lines.size() - 1) {
                // dòng cuối không cần khoảng đệm dưới
                row.setPadding(row.getPaddingLeft(), row.getPaddingTop(), row.getPaddingRight(), 0);
            }
            boxItems.addView(row);
            if (i < lines.size() - 1) boxItems.addView(divider());
        }
        refreshReviewButtons();
    }

    private View divider() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        v.setBackgroundColor(c(R.color.ui_divider));
        return v;
    }

    private void bindPayment() {
        if (order == null) return;
        AdminOrder o = order;
        boxPayment.removeAllViews();
        addPayRow("Phương thức", o.payment.trim().isEmpty() ? "Thanh toán khi nhận hàng" : o.payment.trim(), c(R.color.ui_text));

        boolean anyCurrentPrice = false;
        for (OrderMath.Line l : lines) if (l.priceIsCurrent) anyCurrentPrice = true;
        if (linesLoaded && !lines.isEmpty()) {
            long sub = OrderMath.subtotal(lines);
            addPayRow("Tạm tính", MoneyUtil.formatVnd(sub) + (anyCurrentPrice ? " *" : ""), c(R.color.ui_text));
            addPayRow("Phí vận chuyển", o.shippingFee > 0 ? MoneyUtil.formatVnd(o.shippingFee) : "Miễn phí",
                    o.shippingFee > 0 ? c(R.color.ui_text) : c(R.color.ui_green_text));
            long discount = OrderMath.discount(sub, o.shippingFee, o.total);
            if (discount > 0 && !anyCurrentPrice) {
                addPayRow("Khuyến mãi", "-" + MoneyUtil.formatVnd(discount), c(R.color.ui_green_text));
            }
        }
        tvTotal.setText(MoneyUtil.formatVnd(o.total));
        tvPaymentNote.setVisibility(anyCurrentPrice ? View.VISIBLE : View.GONE);
        tvPaymentNote.setText("* Đơn cũ chưa lưu giá lúc đặt nên hiển thị theo giá hiện tại.");
    }

    private void addPayRow(String label, String value, int valueColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(5), 0, dp(5));

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        l.setTextColor(c(R.color.ui_text_secondary));
        row.addView(l, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        v.setTextColor(valueColor);
        v.setGravity(Gravity.END);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        vlp.setMarginStart(dp(16));
        row.addView(v, vlp);
        boxPayment.addView(row);
    }

    private void bindActions() {
        boolean canCancel = OrderStatus.customerCanCancel(order.status);
        boolean canReorder = CustomerOrders.canReorder(order.status);
        btnCancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);
        btnReorder.setVisibility(canReorder ? View.VISIBLE : View.GONE);
        barActions.setVisibility(canCancel || canReorder ? View.VISIBLE : View.GONE);
    }

    // ---------------------------------------------------------------- đánh giá

    /** Nút "Đánh giá" chỉ hiện với đơn đã giao; sản phẩm đã đánh giá thì khóa lại. */
    private void refreshReviewButtons() {
        if (order == null) return;
        boolean can = CustomerOrders.canReview(order.status);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        for (Map.Entry<String, MaterialButton> en : reviewButtons.entrySet()) {
            String pid = en.getKey();
            MaterialButton btn = en.getValue();
            btn.setVisibility(can ? View.VISIBLE : View.GONE);
            if (!can) continue;
            boolean done = reviewed.contains(pid);
            btn.setEnabled(!done);
            btn.setText(done ? "Đã đánh giá" : "★  Đánh giá");
            if (!done && auth.getCurrentUser() != null && !reviewChecked.contains(pid)) {
                reviewChecked.add(pid);
                db.collection("BinhLuan")
                        .whereEqualTo("iduser", auth.getCurrentUser().getUid())
                        .whereEqualTo("idproduct", pid).limit(1).get()
                        .addOnSuccessListener(s -> {
                            if (!s.isEmpty() && !isFinishing()) {
                                reviewed.add(pid);
                                refreshReviewButtons();
                            }
                        });
            }
        }
    }

    private void showReviewDialog(OrderMath.Line line) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_danhgia);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            // Rộng theo màn hình (lề 20dp mỗi bên), co lại khi bàn phím mở → nút Gửi/Hủy luôn thấy được
            int margin = dp(20);
            window.setBackgroundDrawable(new InsetDrawable(new ColorDrawable(Color.TRANSPARENT),
                    margin, margin, margin, margin));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        bindReviewDialog(dialog, line);
    }

    private void bindReviewDialog(Dialog dialog, OrderMath.Line line) {
        ImageView img = dialog.findViewById(R.id.img_danhgia);
        TextView tvName = dialog.findViewById(R.id.tv_name_danhgia);
        TextView tvNumber = dialog.findViewById(R.id.tv_number_danhgia);
        RadioGroup group = dialog.findViewById(R.id.rdg_danhgia);
        EditText edtOther = dialog.findViewById(R.id.edt_danhgia_khac);
        RatingBar rating = dialog.findViewById(R.id.rating_danhgia);
        MaterialButton btnSend = dialog.findViewById(R.id.btn_danhgia_gui);
        MaterialButton btnCancelDialog = dialog.findViewById(R.id.btn_danhgia_huy);

        if (line.image != null && !line.image.trim().isEmpty()) Picasso.get().load(line.image.trim()).into(img);
        tvName.setText(line.name);
        tvNumber.setText(String.valueOf(line.quantity));
        group.clearCheck();
        edtOther.setEnabled(false);
        rating.setRating(5f);

        // Đọc trạng thái trực tiếp từ các view khi bấm Gửi (không dùng biến dùng chung giữa các lần mở)
        group.setOnCheckedChangeListener((g, checkedId) -> {
            boolean other = checkedId == R.id.rdo_danhgia_khac;
            edtOther.setEnabled(other);
            if (other) edtOther.requestFocus();
        });

        btnSend.setOnClickListener(v -> {
            String content;
            int checked = group.getCheckedRadioButtonId();
            if (checked == R.id.rdo_danhgia_tot) {
                content = "Sản phẩm rất tốt";
            } else if (checked == R.id.rdo_danhgia_khongtot) {
                content = "Không hài lòng";
            } else if (checked == R.id.rdo_danhgia_khac) {
                content = edtOther.getText().toString().trim();
                if (content.isEmpty()) {
                    edtOther.setError("Vui lòng nhập nhận xét");
                    return;
                }
            } else {
                Toast.makeText(this, "Vui lòng chọn nhận xét", Toast.LENGTH_SHORT).show();
                return;
            }
            float stars = rating.getRating();
            if (stars <= 0f) {
                Toast.makeText(this, "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show();
                return;
            }
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                return;
            }

            HashMap<String, String> map = new HashMap<>();
            map.put("idproduct", line.productId);
            map.put("iduser", FirebaseAuth.getInstance().getCurrentUser().getUid());
            map.put("noidung", content);
            map.put("rate", String.valueOf(stars));
            btnSend.setEnabled(false); // chống bấm nhiều lần tạo đánh giá trùng
            db.collection("BinhLuan").add(map)
                    .addOnSuccessListener(ref -> {
                        reviewed.add(line.productId);
                        refreshReviewButtons();
                        Toast.makeText(this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    })
                    .addOnFailureListener(e -> {
                        btnSend.setEnabled(true);
                        Toast.makeText(this, "Gửi đánh giá thất bại, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                    });
        });
        btnCancelDialog.setOnClickListener(v -> dialog.cancel());
    }

    // ---------------------------------------------------------------- tiện ích

    private void copyCode() {
        if (order == null) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Mã đơn", order.displayCode()));
            Toast.makeText(this, "Đã sao chép mã đơn", Toast.LENGTH_SHORT).show();
        }
    }

    private int c(int colorRes) {
        return androidx.core.content.ContextCompat.getColor(this, colorRes);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
