package com.UngDung.ThucPhamXanh.View.Shipper;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.MoneyUtil;
import com.UngDung.ThucPhamXanh.Helper.ShipperOrders;
import com.UngDung.ThucPhamXanh.Models.OrderStatus;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.Admin.AdminOrderStatusSheet;
import com.UngDung.ThucPhamXanh.View.Admin.BaseAdminActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chi tiết một đơn cho shipper.
 *
 * Khác bản cũ: không còn dựa vào dữ liệu truyền qua Intent (bản chụp có thể đã cũ) — màn hình nghe
 * trực tiếp document HoaDon, nên nếu đơn vừa bị shipper khác nhận hoặc bị hủy thì nút hành động
 * biến mất ngay và hiện lý do. Quy tắc "được làm gì" lấy từ {@link ShipperOrders}.
 *
 * Hành động (mỗi hành động ghi MỘT lần update, không tách đôi như trước):
 *   - Nhận đơn      : trangthai 5 → 4 + shipperId + shipperName + assignedAt
 *   - Đã giao       : trangthai 4 → 3 + deliveredAt (+ codCollected nếu đơn thu tiền mặt)
 * Chưa có giao dịch chống hai shipper bấm cùng lúc — việc đó nằm ở bước "ghi nguyên tử" trong kế hoạch.
 */
public class ShipperUpdateOrderActivity extends BaseAdminActivity {

    private static final String TAG = "ShipperUpdateOrder";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String orderId, shipperUsername, shipperName;
    private AdminOrder order;            // dữ liệu mới nhất từ Firestore
    private boolean linesRequested;
    private boolean busy;                // đang ghi, chặn bấm lặp

    private MaterialToolbar toolbar;
    private TextView tvStatus, tvDate, tvLocked, tvPaymentTitle, tvPaymentBig, tvPaymentSub;
    private TextView tvName, tvPhone, tvAddress, tvSlot, tvNote, tvItemsTitle, tvItemsState;
    private View boxPayment;
    private LinearLayout boxItems;
    private MaterialButton btnPrimary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        orderId = intent.getStringExtra("order_id");
        shipperUsername = intent.getStringExtra("shipper_username");
        shipperName = intent.getStringExtra("shipper_name");
        if (orderId == null || orderId.isEmpty() || shipperUsername == null || shipperUsername.isEmpty()) {
            Toast.makeText(this, "Lỗi: thiếu thông tin đơn hàng hoặc shipper", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (shipperName == null || shipperName.isEmpty()) shipperName = shipperUsername;

        setContentView(R.layout.activity_shipper_update_order);
        toolbar = setupAdminToolbar("Chi tiết đơn", null);
        applySystemBarInsets(null, findViewById(R.id.bottom_bar));

        tvStatus = findViewById(R.id.tv_status_badge);
        tvDate = findViewById(R.id.tv_order_date);
        tvLocked = findViewById(R.id.tv_locked_reason);
        boxPayment = findViewById(R.id.box_payment);
        tvPaymentTitle = findViewById(R.id.tv_payment_title);
        tvPaymentBig = findViewById(R.id.tv_payment_big);
        tvPaymentSub = findViewById(R.id.tv_payment_sub);
        tvName = findViewById(R.id.tv_customer_name_detail);
        tvPhone = findViewById(R.id.tv_phone_detail);
        tvAddress = findViewById(R.id.tv_address_detail);
        tvSlot = findViewById(R.id.tv_slot_detail);
        tvNote = findViewById(R.id.tv_note_detail);
        tvItemsTitle = findViewById(R.id.tv_items_title);
        tvItemsState = findViewById(R.id.tv_items_state);
        boxItems = findViewById(R.id.box_items);
        btnPrimary = findViewById(R.id.btn_primary);
        btnPrimary.setVisibility(View.GONE);

        ImageButton btnCall = findViewById(R.id.btn_call_detail);
        btnCall.setOnClickListener(v -> callCustomer());
        findViewById(R.id.btn_directions).setOnClickListener(v -> openMaps());
        btnPrimary.setOnClickListener(v -> onPrimaryClick());
    }

    @Override
    protected void onStartListening() {
        if (orderId == null) return; // đã finish() ở onCreate
        listen(db.collection("HoaDon").document(orderId).addSnapshotListener((doc, e) -> {
            if (e != null) {
                Log.e(TAG, "Lỗi đọc đơn", e);
                tvLocked.setVisibility(View.VISIBLE);
                tvLocked.setText("Không tải được đơn. Kiểm tra kết nối mạng.");
                return;
            }
            if (doc == null) return;
            if (!doc.exists()) {
                Toast.makeText(this, "Đơn hàng không còn tồn tại", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            order = FirestoreMapper.toAdminOrder(doc);
            bind();
            if (!linesRequested) { linesRequested = true; loadLines(); }
        }));
    }

    // ------------------------------------------------------------------ hiển thị

    private void bind() {
        AdminOrder o = order;
        toolbar.setSubtitle(o.displayCode());
        AdminOrderStatusSheet.bindStatusBadge(tvStatus, o.status);
        tvDate.setText(o.date.isEmpty() ? "" : "Đặt ngày " + o.date);

        // Tiền: COD → số tiền cần thu (cam), trả trước → đã thanh toán (xanh)
        boolean cod = ShipperOrders.isCod(o.payment);
        boxPayment.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                cod ? R.color.admin_warning_soft : R.color.admin_primary_soft)));
        tvPaymentTitle.setText(o.payment.isEmpty() ? "Chưa rõ phương thức thanh toán" : o.payment);
        tvPaymentBig.setText(ShipperOrders.paymentLabel(o));
        tvPaymentBig.setTextColor(ContextCompat.getColor(this, cod ? R.color.admin_warning_text : R.color.admin_accent_text));
        if (cod) {
            tvPaymentSub.setText(o.shippingFee > 0
                    ? "Gồm phí vận chuyển " + MoneyUtil.formatVnd(o.shippingFee) + " · thu tiền mặt khi giao"
                    : "Thu tiền mặt khi giao");
        } else {
            tvPaymentSub.setText("Không thu thêm tiền của khách (tổng đơn " + MoneyUtil.formatVnd(o.total) + ")");
        }

        tvName.setText(o.name.isEmpty() ? "(Không có tên)" : o.name);
        tvPhone.setText(o.phone);
        tvAddress.setText(o.address.isEmpty() ? "(Chưa có địa chỉ)" : o.address);

        String slot = ShipperOrders.deliverySlot(o);
        tvSlot.setVisibility(slot.isEmpty() ? View.GONE : View.VISIBLE);
        tvSlot.setText("Giao: " + slot);
        tvNote.setVisibility(o.note.trim().isEmpty() ? View.GONE : View.VISIBLE);
        tvNote.setText("Ghi chú của khách: " + o.note.trim());

        bindAction();
    }

    /** Nút ở đáy luôn theo dữ liệu mới nhất; hết quyền thao tác thì ẩn nút và nói rõ lý do. */
    private void bindAction() {
        AdminOrder o = order;
        String locked = ShipperOrders.lockedReason(o, shipperUsername);
        tvLocked.setVisibility(locked.isEmpty() ? View.GONE : View.VISIBLE);
        tvLocked.setText(locked);

        if (ShipperOrders.canClaim(o)) {
            // Đơn admin đã chuyển "Đang giao" nhưng chưa gán shipper: chỉ cần nhận, không còn "bắt đầu giao"
            btnPrimary.setText(ShipperOrders.isWaitingForShipper(o) ? "Nhận đơn này" : "Nhận đơn và bắt đầu giao");
            btnPrimary.setVisibility(View.VISIBLE);
        } else if (ShipperOrders.canComplete(o, shipperUsername)) {
            btnPrimary.setText(ShipperOrders.isCod(o.payment)
                    ? "Đã giao và thu " + MoneyUtil.formatVnd(o.total)
                    : "Đã giao thành công");
            btnPrimary.setVisibility(View.VISIBLE);
        } else {
            btnPrimary.setVisibility(View.GONE);
        }
        btnPrimary.setEnabled(!busy);
    }

    // ------------------------------------------------------------------ danh sách món

    private void loadLines() {
        if (order.uid.isEmpty()) {
            tvItemsState.setText("Không có thông tin khách để tải danh sách món.");
            return;
        }
        db.collection("ChitietHoaDon").document(order.uid).collection("ALL")
                .whereEqualTo("id_hoadon", order.id).get()
                .addOnSuccessListener(snap -> {
                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) docs.add(d);
                    if (docs.isEmpty()) { tvItemsState.setText("Đơn này không có dòng sản phẩm."); return; }

                    String[] names = new String[docs.size()];
                    long[] qty = new long[docs.size()];
                    int[] pending = {docs.size()};
                    for (int i = 0; i < docs.size(); i++) {
                        final int idx = i;
                        QueryDocumentSnapshot line = docs.get(i);
                        qty[idx] = FirestoreMapper.num(line, "soluong");
                        String productId = line.getString("id_product");
                        if (productId == null || productId.isEmpty()) {
                            names[idx] = "(Không rõ sản phẩm)";
                            if (--pending[0] == 0) showLines(names, qty);
                            continue;
                        }
                        db.collection("SanPham").document(productId).get().addOnCompleteListener(t -> {
                            DocumentSnapshot p = t.isSuccessful() ? t.getResult() : null;
                            names[idx] = p != null && p.exists() ? FirestoreMapper.str(p, "tensp") : "(Sản phẩm đã bị xóa)";
                            if (--pending[0] == 0) showLines(names, qty);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi đọc dòng sản phẩm", e);
                    tvItemsState.setText("Không tải được danh sách món. Kiểm tra kết nối mạng.");
                });
    }

    private void showLines(String[] names, long[] qty) {
        if (isFinishing() || isDestroyed()) return;
        boxItems.removeAllViews();
        long total = 0;
        for (int i = 0; i < names.length; i++) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_admin_kv, (ViewGroup) boxItems, false);
            ((TextView) row.findViewById(R.id.tv_kv_label)).setText(names[i]);
            ((TextView) row.findViewById(R.id.tv_kv_value)).setText("× " + qty[i]);
            boxItems.addView(row);
            total += qty[i];
        }
        tvItemsTitle.setText("Các món cần giao (" + names.length + " loại, " + total + " sản phẩm)");
        tvItemsState.setVisibility(View.GONE);
    }

    // ------------------------------------------------------------------ gọi / chỉ đường

    private void callCustomer() {
        if (order == null) return;
        String number = ShipperOrders.dialNumber(order.phone);
        if (number.isEmpty()) { Toast.makeText(this, "Đơn không có số điện thoại", Toast.LENGTH_SHORT).show(); return; }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)));
    }

    private void openMaps() {
        if (order == null || order.address.trim().isEmpty()) {
            Toast.makeText(this, "Đơn không có địa chỉ", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ShipperOrders.mapsUri(order.address))));
        } catch (ActivityNotFoundException e) {
            // Máy không có app bản đồ: mở trên trình duyệt
            String q = Uri.encode(order.address.trim());
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" + q)));
        }
    }

    // ------------------------------------------------------------------ nhận đơn / hoàn thành

    private void onPrimaryClick() {
        if (order == null || busy) return;
        if (ShipperOrders.canClaim(order)) {
            confirm("Nhận đơn", "Bạn nhận giao đơn " + order.displayCode() + " tới " + order.name + "?",
                    "Nhận đơn", this::claim);
        } else if (ShipperOrders.canComplete(order, shipperUsername)) {
            boolean cod = ShipperOrders.isCod(order.payment);
            confirm("Xác nhận đã giao",
                    cod ? "Bạn đã thu đủ " + MoneyUtil.formatVnd(order.total) + " tiền mặt từ khách chưa?"
                            : "Xác nhận đã giao hàng thành công cho khách?",
                    "Đã giao", this::complete);
        }
    }

    private void claim() {
        Map<String, Object> update = new HashMap<>();
        // Đơn đã ở "Đang giao" (admin chuyển nhưng chưa gán ai) thì giữ nguyên trạng thái, chỉ gán shipper
        if (!ShipperOrders.isWaitingForShipper(order)) update.put("trangthai", OrderStatus.DANG_GIAO);
        update.put("shipperId", shipperUsername);
        update.put("shipperName", shipperName);
        update.put("assignedAt", FieldValue.serverTimestamp());
        write(update, "Đã nhận đơn, hãy liên hệ khách và giao hàng", false);
    }

    private void complete() {
        Map<String, Object> update = new HashMap<>();
        update.put("trangthai", OrderStatus.DA_GIAO);
        update.put("deliveredAt", FieldValue.serverTimestamp());
        if (ShipperOrders.isCod(order.payment)) update.put("codCollected", true);
        write(update, "Đã ghi nhận giao hàng thành công", true);
    }

    /** Một lần update duy nhất; chỉ báo thành công sau khi Firestore ghi xong. */
    private void write(Map<String, Object> update, String successMessage, boolean finishAfter) {
        busy = true;
        btnPrimary.setEnabled(false);
        DocumentReference ref = db.collection("HoaDon").document(orderId);
        ref.update(update).addOnCompleteListener(task -> {
            busy = false;
            if (isFinishing() || isDestroyed()) return;
            if (task.isSuccessful()) {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
                if (finishAfter) finish(); else bindAction();
            } else {
                Log.e(TAG, "Ghi đơn thất bại", task.getException());
                Toast.makeText(this, "Không cập nhật được đơn. Kiểm tra mạng rồi thử lại.", Toast.LENGTH_LONG).show();
                bindAction();
            }
        });
    }
}
