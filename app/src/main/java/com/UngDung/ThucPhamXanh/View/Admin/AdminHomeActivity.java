package com.UngDung.ThucPhamXanh.View.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.AdminProductFilter;
import com.UngDung.ThucPhamXanh.Helper.DashboardStats;
import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.MoneyUtil;
import com.UngDung.ThucPhamXanh.Helper.ProductVisibility;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.ChatActivity;
import com.UngDung.ThucPhamXanh.View.SignInActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dashboard admin: doanh thu hôm nay / tháng, đơn chờ xác nhận, sản phẩm sắp hết hàng,
 * lưới chức năng và danh sách sản phẩm cần chú ý (hết hàng / hết hạn).
 *
 * Đọc Firestore: 1 listener HoaDon + 1 listener SanPham (chỉ khi màn hình đang hiện — xem
 * BaseAdminActivity) và 1 truy vấn count() cho số khách hàng. Toàn bộ phép tính ở DashboardStats.
 */
public class AdminHomeActivity extends BaseAdminActivity {

    private static final String TAG = "AdminHomeActivity";
    /** Số dòng "Cần chú ý" hiện khi chưa bấm "Xem tất cả". */
    private static final int ATTENTION_PREVIEW = 5;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private TextView tvDate, tvSummary, tvAttentionCount;
    private View statRevenueToday, statRevenueMonth, statPending, statLowStock, emptyView;
    private MaterialButton btnAttentionMore;
    private AdminAttentionAdapter attentionAdapter;

    private final List<AdminAttentionAdapter.Row> attentionRows = new ArrayList<>();
    private boolean attentionExpanded;

    // Số tổng cho dòng tóm tắt ở đầu trang (-1 = chưa tải xong)
    private int totalOrders = -1;
    private int totalProducts = -1;
    private long totalUsers = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        applySystemBarInsets(findViewById(R.id.home_header), findViewById(R.id.home_content));

        tvDate = findViewById(R.id.tv_home_date);
        tvSummary = findViewById(R.id.tv_home_summary);
        tvAttentionCount = findViewById(R.id.tv_attention_count);
        btnAttentionMore = findViewById(R.id.btn_attention_more);
        emptyView = findViewById(R.id.admin_empty);

        tvDate.setText(DashboardStats.formatLongDate(LocalDate.now()));
        findViewById(R.id.btn_sign_out).setOnClickListener(v -> confirmSignOut());

        setupStats();
        setupMenu();
        setupAttentionList();
    }

    @Override
    protected void onStartListening() {
        tvDate.setText(DashboardStats.formatLongDate(LocalDate.now())); // qua ngày khi app để mở
        listen(db.collection("HoaDon").addSnapshotListener((snap, e) -> {
            if (e != null) { Log.e(TAG, "Lỗi đọc HoaDon", e); return; }
            if (snap != null) onOrders(snap);
        }));
        listen(db.collection("SanPham").addSnapshotListener((snap, e) -> {
            if (e != null) { Log.e(TAG, "Lỗi đọc SanPham", e); return; }
            if (snap != null) onProducts(snap);
        }));
        db.collection("IDUser").count().get(AggregateSource.SERVER)
                .addOnSuccessListener(r -> { totalUsers = r.getCount(); updateSummary(); })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi đếm IDUser", e));
    }

    // ------------------------------------------------------------------ thẻ số liệu

    private void setupStats() {
        statRevenueToday = findViewById(R.id.stat_revenue_today);
        statRevenueMonth = findViewById(R.id.stat_revenue_month);
        statPending = findViewById(R.id.stat_pending);
        statLowStock = findViewById(R.id.stat_low_stock);

        bindStatStatic(statRevenueToday, R.drawable.ic_money, "Doanh thu hôm nay",
                R.color.admin_primary_soft, R.color.admin_primary);
        bindStatStatic(statRevenueMonth, R.drawable.ic_admin_chart, "Doanh thu tháng",
                R.color.admin_secondary_soft, R.color.admin_secondary);
        bindStatStatic(statPending, R.drawable.ic_admin_order, "Đơn chờ xác nhận",
                R.color.admin_warning_soft, R.color.admin_warning);
        bindStatStatic(statLowStock, R.drawable.ic_admin_stock, "Sắp hết hàng",
                R.color.admin_danger_soft, R.color.admin_danger);

        statRevenueToday.setOnClickListener(v -> open(AdminChartBillActivity.class));
        statRevenueMonth.setOnClickListener(v -> open(AdminChartBillActivity.class));
        statPending.setOnClickListener(v -> startActivity(
                AdminBillMainActivity.intent(this, com.UngDung.ThucPhamXanh.Helper.AdminOrderFilter.TAB_PENDING)));
        statLowStock.setOnClickListener(v -> startActivity(
                AdminProductActivity.intent(this, AdminProductFilter.STATUS_LOW_STOCK)));
    }

    private void bindStatStatic(View card, @DrawableRes int icon, String label,
                                @ColorRes int boxColor, @ColorRes int iconColor) {
        ImageView iv = card.findViewById(R.id.stat_icon);
        iv.setImageResource(icon);
        iv.setImageTintList(ContextCompat.getColorStateList(this, iconColor));
        card.findViewById(R.id.stat_icon_box)
                .setBackgroundTintList(ContextCompat.getColorStateList(this, boxColor));
        ((TextView) card.findViewById(R.id.stat_label)).setText(label);
        card.setContentDescription(label);
    }

    private static void setStat(View card, String value, String hint) {
        ((TextView) card.findViewById(R.id.stat_value)).setText(value);
        ((TextView) card.findViewById(R.id.stat_hint)).setText(hint);
    }

    private void onOrders(QuerySnapshot snap) {
        LocalDate today = LocalDate.now();
        DashboardStats s = new DashboardStats(today);
        for (QueryDocumentSnapshot doc : snap) {
            s.addOrder(FirestoreMapper.numOrNull(doc, "trangthai"),
                    FirestoreMapper.str(doc, "ngaydat"), doc.get("tongtien"));
        }
        setStat(statRevenueToday, MoneyUtil.formatVnd(s.getRevenueToday()),
                s.getOrdersToday() + " đơn mới hôm nay");
        setStat(statRevenueMonth, MoneyUtil.formatVnd(s.getRevenueMonth()),
                "Tháng " + today.getMonthValue() + "/" + today.getYear());
        setStat(statPending, String.valueOf(s.getPendingOrders()),
                s.getPendingOrders() > 0 ? "Bấm để xử lý" : "Không có đơn chờ");
        totalOrders = s.getTotalOrders();
        updateSummary();
    }

    // ------------------------------------------------------------------ sản phẩm cần chú ý

    private void setupAttentionList() {
        RecyclerView rcv = findViewById(R.id.rcv_attention);
        rcv.setLayoutManager(new LinearLayoutManager(this));
        attentionAdapter = new AdminAttentionAdapter(product -> {
            Intent i = new Intent(this, AdminAddSPActivity.class);
            i.putExtra("SP", product);
            startActivity(i);
        });
        rcv.setAdapter(attentionAdapter);
        btnAttentionMore.setOnClickListener(v -> {
            attentionExpanded = !attentionExpanded;
            showAttention();
        });
    }

    private void onProducts(QuerySnapshot snap) {
        LocalDate today = LocalDate.now();
        attentionRows.clear();
        int visible = 0, lowStock = 0, expiryIssues = 0;
        for (QueryDocumentSnapshot doc : snap) {
            if (ProductVisibility.isHidden(doc)) continue; // đã xóa mềm
            visible++;
            Product p = FirestoreMapper.toProduct(doc);
            DashboardStats.ProductAlert alert = DashboardStats.checkProduct(p.getSoluong(), p.getHansudung(), today);
            if (alert == null) continue;
            if (alert.stockLevel != 0) lowStock++;
            if (alert.expiryLevel != 0) expiryIssues++;
            attentionRows.add(new AdminAttentionAdapter.Row(p, alert));
        }
        Collections.sort(attentionRows, (a, b) -> DashboardStats.ProductAlert.URGENCY.compare(a.alert, b.alert) != 0
                ? DashboardStats.ProductAlert.URGENCY.compare(a.alert, b.alert)
                : String.valueOf(a.product.getTensp()).compareToIgnoreCase(String.valueOf(b.product.getTensp())));

        setStat(statLowStock, String.valueOf(lowStock),
                "Tồn kho ≤ " + DashboardStats.LOW_STOCK_THRESHOLD);
        tvAttentionCount.setText(attentionRows.isEmpty() ? ""
                : lowStock + " tồn kho · " + expiryIssues + " hạn dùng");
        totalProducts = visible;
        updateSummary();
        showAttention();
    }

    private void showAttention() {
        int n = attentionRows.size();
        boolean collapsed = !attentionExpanded && n > ATTENTION_PREVIEW;
        attentionAdapter.submitList(new ArrayList<>(collapsed ? attentionRows.subList(0, ATTENTION_PREVIEW) : attentionRows));
        showEmpty(emptyView, n == 0, "Mọi sản phẩm đều còn hàng và còn hạn");
        btnAttentionMore.setVisibility(n > ATTENTION_PREVIEW ? View.VISIBLE : View.GONE);
        btnAttentionMore.setText(collapsed ? "Xem tất cả (" + n + ")" : "Thu gọn");
    }

    // ------------------------------------------------------------------ đầu trang

    private void updateSummary() {
        List<String> parts = new ArrayList<>();
        if (totalOrders >= 0) parts.add(totalOrders + " đơn hàng");
        if (totalProducts >= 0) parts.add(totalProducts + " sản phẩm");
        if (totalUsers >= 0) parts.add(totalUsers + " khách hàng");
        tvSummary.setText(android.text.TextUtils.join(" · ", parts));
    }

    private void confirmSignOut() {
        confirm("Đăng xuất", "Bạn muốn thoát khỏi trang quản trị?", "Đăng xuất", () -> {
            Intent i = new Intent(this, SignInActivity.class);
            // Xóa toàn bộ màn admin khỏi back stack: bấm Back ở màn đăng nhập không quay lại được
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    // ------------------------------------------------------------------ lưới chức năng

    private void setupMenu() {
        bindMenu(R.id.menu_orders, R.drawable.ic_admin_order, "Đơn hàng",
                R.color.admin_primary_soft, R.color.admin_primary, v -> open(AdminBillMainActivity.class));
        bindMenu(R.id.menu_products, R.drawable.ic_admin_product, "Sản phẩm",
                R.color.admin_primary_soft, R.color.admin_primary, v -> open(AdminProductActivity.class));
        bindMenu(R.id.menu_categories, R.drawable.ic_admin_category, "Danh mục",
                R.color.admin_primary_soft, R.color.admin_primary, v -> open(AdminLoaiSPActivity.class));
        bindMenu(R.id.menu_promotions, R.drawable.ic_admin_promotion, "Khuyến mãi",
                R.color.admin_warning_soft, R.color.admin_warning, v -> open(AdminPromotionActivity.class));
        bindMenu(R.id.menu_users, R.drawable.ic_admin_users, "Người dùng",
                R.color.admin_info_soft, R.color.admin_info, v -> open(AdminUsersActivity.class));
        bindMenu(R.id.menu_shippers, R.drawable.ic_ui_truck, "Shipper",
                R.color.admin_info_soft, R.color.admin_info, v -> open(AdminShipperActivity.class));
        bindMenu(R.id.menu_stats, R.drawable.ic_admin_chart, "Thống kê",
                R.color.admin_secondary_soft, R.color.admin_secondary, v -> open(AdminChartBillActivity.class));
        bindMenu(R.id.menu_chat, R.drawable.ic_admin_chat, "Tư vấn",
                R.color.admin_secondary_soft, R.color.admin_secondary, v -> {
                    Intent i = new Intent(this, ChatActivity.class);
                    i.putExtra("message", 1);
                    startActivity(i);
                });
    }

    private void bindMenu(int includeId, @DrawableRes int icon, String label,
                          @ColorRes int boxColor, @ColorRes int iconColor, View.OnClickListener onClick) {
        View item = findViewById(includeId);
        ImageView iv = item.findViewById(R.id.menu_icon);
        iv.setImageResource(icon);
        iv.setImageTintList(ContextCompat.getColorStateList(this, iconColor));
        item.findViewById(R.id.menu_icon_box)
                .setBackgroundTintList(ContextCompat.getColorStateList(this, boxColor));
        ((TextView) item.findViewById(R.id.menu_label)).setText(label);
        item.setContentDescription(label);
        item.setOnClickListener(onClick);
    }

    private void open(Class<?> screen) {
        startActivity(new Intent(this, screen));
    }
}
