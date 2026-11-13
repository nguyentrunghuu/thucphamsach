package com.UngDung.ThucPhamXanh.View.Admin;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.AdminOrderFlow;
import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.InvoicePdf;
import com.UngDung.ThucPhamXanh.Helper.MoneyUtil;
import com.UngDung.ThucPhamXanh.Helper.OrderActions;
import com.UngDung.ThucPhamXanh.Helper.OrderMath;
import com.UngDung.ThucPhamXanh.Models.OrderStatus;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Chi tiết đơn hàng trang admin.
 *
 * Nhận {@link AdminOrder} qua {@link #intent}, rồi nghe document HoaDon/{id} để trạng thái luôn mới
 * (shipper / khách đổi đơn thì màn hình tự cập nhật). Dòng sản phẩm đọc một lần từ
 * ChitietHoaDon/{uid}/ALL; giá dùng "giaBan" (giá lúc đặt) nếu có, đơn cũ thì giá hiện tại kèm dấu *.
 * Đổi trạng thái qua {@link AdminOrderStatusSheet}; "Xuất hóa đơn" tạo PDF ({@link InvoicePdf}) rồi chia sẻ.
 */
public class AdminCTHDActivity extends BaseAdminActivity {

    private static final String TAG = "AdminCTHDActivity";
    private static final String EXTRA_ORDER = "admin_order";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private AdminOrder order;
    private List<OrderMath.Line> lines;   // null = đang tải

    private TextView tvTotalBig, tvStatus, tvDate, tvCancel, tvItemsTitle, tvItemsNote, tvTotal;
    private ViewGroup boxCustomer, boxDelivery, boxPayment;
    private View cardDelivery, progressItems;
    private MaterialButton btnUpdate, btnExport, btnCall;
    private final AdminCTHDAdapter adapter = new AdminCTHDAdapter();

    public static Intent intent(Context context, AdminOrder order) {
        return new Intent(context, AdminCTHDActivity.class).putExtra(EXTRA_ORDER, order);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_cthdactivity);

        order = (AdminOrder) getIntent().getSerializableExtra(EXTRA_ORDER);
        if (order == null) {
            Toast.makeText(this, "Không có thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setupAdminToolbar("Chi tiết đơn", order.displayCode());
        applySystemBarInsets(null, findViewById(R.id.bottom_bar_detail));

        tvTotalBig = findViewById(R.id.tv_detail_total_big);
        tvStatus = findViewById(R.id.tv_detail_status);
        tvDate = findViewById(R.id.tv_detail_date);
        tvCancel = findViewById(R.id.tv_detail_cancel);
        tvItemsTitle = findViewById(R.id.tv_detail_items_title);
        tvItemsNote = findViewById(R.id.tv_detail_items_note);
        tvTotal = findViewById(R.id.tv_detail_total);
        boxCustomer = findViewById(R.id.box_detail_customer);
        boxDelivery = findViewById(R.id.box_detail_delivery);
        boxPayment = findViewById(R.id.box_detail_payment);
        cardDelivery = findViewById(R.id.card_detail_delivery);
        progressItems = findViewById(R.id.progress_detail_items);
        btnUpdate = findViewById(R.id.btn_admin_updatehd);
        btnExport = findViewById(R.id.btn_admin_export_hd);
        btnCall = findViewById(R.id.btn_detail_call);

        RecyclerView rcv = findViewById(R.id.rcv_admin_hd);
        rcv.setLayoutManager(new LinearLayoutManager(this));
        rcv.setAdapter(adapter);

        btnUpdate.setOnClickListener(v -> AdminOrderStatusSheet.show(this, order, null)); // listener tự vẽ lại
        btnExport.setOnClickListener(v -> exportInvoice());
        btnCall.setOnClickListener(v -> {
            if (order.phone.trim().isEmpty()) return;
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + order.phone.replace(" ", ""))));
        });

        bindOrder();
        loadLines();
    }

    @Override
    protected void onStartListening() {
        if (order == null) return;
        listen(db.collection("HoaDon").document(order.id).addSnapshotListener((doc, e) -> {
            if (e != null) { Log.e(TAG, "Lỗi đọc đơn " + order.id, e); return; }
            if (doc == null || !doc.exists()) return;
            order = FirestoreMapper.toAdminOrder(doc);
            bindOrder();
        }));
    }

    // ------------------------------------------------------------------ hiển thị

    private void bindOrder() {
        tvTotalBig.setText(MoneyUtil.formatVnd(order.total));
        AdminOrderStatusSheet.bindStatusBadge(tvStatus, order.status);
        tvDate.setText("Đặt ngày " + (order.date.isEmpty() ? "?" : order.date)
                + (order.payment.isEmpty() ? "" : " · " + order.payment));

        if (order.status == OrderStatus.DA_HUY) {
            String by = OrderActions.BY_ADMIN.equals(order.cancelledBy) ? "cửa hàng"
                    : OrderActions.BY_CUSTOMER.equals(order.cancelledBy) ? "khách" : "";
            String reason = order.cancelReason.trim();
            tvCancel.setText("Đơn đã bị hủy" + (by.isEmpty() ? "" : " bởi " + by)
                    + (reason.isEmpty() ? "" : ": " + reason));
            tvCancel.setVisibility(View.VISIBLE);
        } else {
            tvCancel.setVisibility(View.GONE);
        }

        boxCustomer.removeAllViews();
        addRow(boxCustomer, "Họ tên", order.name);
        addRow(boxCustomer, "Điện thoại", order.phone);
        addRow(boxCustomer, "Địa chỉ", order.address);
        addRow(boxCustomer, "Ghi chú", order.note);
        btnCall.setVisibility(order.phone.trim().isEmpty() ? View.GONE : View.VISIBLE);

        boxDelivery.removeAllViews();
        String when = (order.deliveryDate + " " + order.deliverySlot).trim();
        addRow(boxDelivery, "Ngày giao", when);
        addRow(boxDelivery, "Shipper", order.shipperName);
        cardDelivery.setVisibility(boxDelivery.getChildCount() == 0 ? View.GONE : View.VISIBLE);

        btnUpdate.setVisibility(AdminOrderFlow.nextStatuses(order.status).length > 0 ? View.VISIBLE : View.GONE);
        bindPayment();
    }

    private void bindPayment() {
        boxPayment.removeAllViews();
        tvTotal.setText(MoneyUtil.formatVnd(order.total));
        if (lines == null) return;
        long subtotal = OrderMath.subtotal(lines);
        addRow(boxPayment, "Tạm tính (" + OrderMath.totalQuantity(lines) + " sp)", MoneyUtil.formatVnd(subtotal));
        addRow(boxPayment, "Phí vận chuyển", order.shippingFee > 0 ? MoneyUtil.formatVnd(order.shippingFee) : "Miễn phí");
        long discount = discount();
        if (discount > 0) addRow(boxPayment, "Khuyến mãi", "−" + MoneyUtil.formatVnd(discount));
    }

    /** Khuyến mãi suy ra; chỉ tin được khi mọi dòng đều có giá lúc đặt. */
    private long discount() {
        if (lines == null) return 0;
        for (OrderMath.Line l : lines) if (l.priceIsCurrent) return 0;
        return OrderMath.discount(OrderMath.subtotal(lines), order.shippingFee, order.total);
    }

    private void addRow(ViewGroup box, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        View row = LayoutInflater.from(this).inflate(R.layout.item_admin_kv, box, false);
        ((TextView) row.findViewById(R.id.tv_kv_label)).setText(label);
        ((TextView) row.findViewById(R.id.tv_kv_value)).setText(value.trim());
        box.addView(row);
    }

    // ------------------------------------------------------------------ dòng sản phẩm

    private void loadLines() {
        if (order.uid.isEmpty()) {
            showLines(new ArrayList<>());
            return;
        }
        db.collection("ChitietHoaDon").document(order.uid).collection("ALL")
                .whereEqualTo("id_hoadon", order.id).get()
                .addOnSuccessListener(snap -> {
                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) docs.add(d);
                    if (docs.isEmpty()) { showLines(new ArrayList<>()); return; }

                    OrderMath.Line[] result = new OrderMath.Line[docs.size()];
                    int[] pending = {docs.size()};
                    for (int i = 0; i < docs.size(); i++) {
                        final int idx = i;
                        QueryDocumentSnapshot line = docs.get(i);
                        String productId = line.getString("id_product");
                        long qty = FirestoreMapper.num(line, "soluong");
                        Long priceAtOrder = FirestoreMapper.numOrNull(line, "giaBan");
                        if (productId == null || productId.isEmpty()) {
                            result[idx] = new OrderMath.Line("", "(Không rõ sản phẩm)", "", qty,
                                    priceAtOrder == null ? 0 : priceAtOrder, priceAtOrder == null);
                            if (--pending[0] == 0) showLines(Arrays.asList(result));
                            continue;
                        }
                        db.collection("SanPham").document(productId).get()
                                .addOnCompleteListener(t -> {
                                    DocumentSnapshot p = t.isSuccessful() ? t.getResult() : null;
                                    boolean exists = p != null && p.exists();
                                    String name = exists ? FirestoreMapper.str(p, "tensp") : "(Sản phẩm đã bị xóa)";
                                    String image = exists ? FirestoreMapper.str(p, "hinhanh") : "";
                                    long current = exists ? FirestoreMapper.num(p, "giatien") : 0;
                                    boolean priceIsCurrent = priceAtOrder == null || priceAtOrder <= 0;
                                    result[idx] = new OrderMath.Line(productId, name, image, qty,
                                            OrderMath.unitPrice(priceAtOrder, current), priceIsCurrent);
                                    if (--pending[0] == 0) showLines(Arrays.asList(result));
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi đọc chi tiết đơn " + order.id, e);
                    progressItems.setVisibility(View.GONE);
                    tvItemsNote.setVisibility(View.VISIBLE);
                    tvItemsNote.setText("Không tải được danh sách sản phẩm");
                });
    }

    private void showLines(List<OrderMath.Line> data) {
        if (isFinishing()) return;
        lines = new ArrayList<>(data);
        progressItems.setVisibility(View.GONE);
        adapter.setLines(lines);
        tvItemsTitle.setText("Sản phẩm (" + lines.size() + ")");

        boolean anyCurrent = false;
        for (OrderMath.Line l : lines) anyCurrent |= l.priceIsCurrent;
        if (lines.isEmpty()) {
            tvItemsNote.setText("Đơn không có dòng sản phẩm nào");
        } else if (anyCurrent) {
            tvItemsNote.setText("* Đơn cũ không lưu giá lúc đặt — dòng có dấu * đang dùng giá hiện tại, "
                    + "nên tạm tính có thể khác tổng tiền.");
        }
        tvItemsNote.setVisibility(lines.isEmpty() || anyCurrent ? View.VISIBLE : View.GONE);
        btnExport.setEnabled(!lines.isEmpty());
        bindPayment();
    }

    // ------------------------------------------------------------------ xuất hóa đơn

    private void exportInvoice() {
        if (lines == null || lines.isEmpty()) return;
        try {
            File file = InvoicePdf.create(this, order, lines, OrderMath.subtotal(lines), discount());
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent share = new Intent(Intent.ACTION_SEND)
                    .setType("application/pdf")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_SUBJECT, "Hóa đơn " + order.displayCode())
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Gửi / lưu hóa đơn"));
        } catch (Exception e) {
            Log.e(TAG, "Xuất hóa đơn thất bại", e);
            Toast.makeText(this, "Không tạo được hóa đơn: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
