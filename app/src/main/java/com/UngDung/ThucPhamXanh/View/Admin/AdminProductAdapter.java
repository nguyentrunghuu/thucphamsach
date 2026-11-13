package com.UngDung.ThucPhamXanh.View.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.DashboardStats;
import com.UngDung.ThucPhamXanh.Helper.DashboardStats.ProductAlert;
import com.UngDung.ThucPhamXanh.Helper.MoneyUtil;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.squareup.picasso.Picasso;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Danh sách sản phẩm trang admin. ListAdapter + DiffUtil: chỉ dòng thay đổi mới vẽ lại,
 * listener bấm gắn một lần trong onCreateViewHolder (không theo position cũ).
 */
public class AdminProductAdapter extends ListAdapter<Product, AdminProductAdapter.ViewHolder> {

    public interface OnProductClick {
        void onClick(Product product);
    }

    private final OnProductClick onClick;
    private LocalDate today = LocalDate.now();

    public AdminProductAdapter(OnProductClick onClick) {
        super(DIFF);
        this.onClick = onClick;
    }

    /** Đổi ngày tính hạn dùng (gọi khi màn hình mở lại sau nửa đêm). */
    public void setToday(LocalDate today) {
        if (today != null && !today.equals(this.today)) {
            this.today = today;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_product, parent, false);
        ViewHolder holder = new ViewHolder(v);
        v.setOnClickListener(view -> {
            int pos = holder.getAdapterPosition(); // recyclerview 1.1: chưa có getBindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onClick.onClick(getItem(pos));
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Product p = getItem(position);

        String name = p.getTensp();
        h.name.setText(name == null || name.trim().isEmpty() ? "(Chưa đặt tên)" : name);

        StringBuilder sub = new StringBuilder();
        appendPart(sub, p.getLoaisp(), null);
        appendPart(sub, p.getTrongluong(), null);
        appendPart(sub, p.getHansudung(), "HSD ");
        h.sub.setText(sub.length() == 0 ? "Chưa có loại / hạn dùng" : sub);

        h.price.setText(MoneyUtil.formatVnd(p.getGiatien()));

        // Ảnh chờ / lỗi: ô be trơn
        String url = p.getHinhanh();
        if (url == null || url.trim().isEmpty()) {
            Picasso.get().cancelRequest(h.image);
            h.image.setImageDrawable(null);
        } else {
            Picasso.get().load(url).fit().centerCrop().into(h.image);
        }

        ProductAlert alert = DashboardStats.checkProduct(p.getSoluong(), p.getHansudung(), today);

        // Tồn kho: luôn hiện; đỏ = hết, cam = sắp hết, xanh = đủ
        if (alert != null && alert.stockLevel == ProductAlert.OUT_OF_STOCK) {
            badge(h.stockBadge, "Hết hàng", R.color.admin_danger_soft, R.color.admin_danger);
        } else if (alert != null && alert.stockLevel == ProductAlert.LOW_STOCK) {
            badge(h.stockBadge, "Kho " + p.getSoluong(), R.color.admin_warning_soft, R.color.admin_warning_text);
        } else {
            badge(h.stockBadge, "Kho " + MoneyUtil.formatNumber(p.getSoluong()),
                    R.color.admin_primary_soft, R.color.admin_primary_dark);
        }

        // Hạn dùng: chỉ hiện khi đã / sắp hết hạn
        if (alert != null && alert.expiryLabel != null) {
            boolean expired = alert.expiryLevel == ProductAlert.EXPIRED;
            badge(h.expiryBadge, alert.expiryLabel,
                    expired ? R.color.admin_danger_soft : R.color.admin_warning_soft,
                    expired ? R.color.admin_danger : R.color.admin_warning_text);
        } else {
            h.expiryBadge.setVisibility(View.GONE);
        }
    }

    private static void appendPart(StringBuilder sb, String value, String prefix) {
        if (value == null || value.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append(" · ");
        if (prefix != null) sb.append(prefix);
        sb.append(value.trim());
    }

    private static void badge(TextView tv, String text, @ColorRes int bg, @ColorRes int fg) {
        tv.setVisibility(View.VISIBLE);
        tv.setText(text);
        tv.setBackgroundTintList(ContextCompat.getColorStateList(tv.getContext(), bg));
        tv.setTextColor(ContextCompat.getColor(tv.getContext(), fg));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name, sub, price, expiryBadge, stockBadge;

        ViewHolder(@NonNull View v) {
            super(v);
            image = v.findViewById(R.id.img_admin_product);
            name = v.findViewById(R.id.tv_admin_product_name);
            sub = v.findViewById(R.id.tv_admin_product_sub);
            price = v.findViewById(R.id.tv_admin_product_price);
            expiryBadge = v.findViewById(R.id.tv_badge_product_expiry);
            stockBadge = v.findViewById(R.id.tv_badge_product_stock);
        }
    }

    private static final DiffUtil.ItemCallback<Product> DIFF = new DiffUtil.ItemCallback<Product>() {
        @Override
        public boolean areItemsTheSame(@NonNull Product a, @NonNull Product b) {
            return Objects.equals(a.getId(), b.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Product a, @NonNull Product b) {
            return Objects.equals(a.getTensp(), b.getTensp())
                    && Objects.equals(a.getHinhanh(), b.getHinhanh())
                    && Objects.equals(a.getLoaisp(), b.getLoaisp())
                    && Objects.equals(a.getTrongluong(), b.getTrongluong())
                    && Objects.equals(a.getHansudung(), b.getHansudung())
                    && a.getGiatien() == b.getGiatien()
                    && a.getSoluong() == b.getSoluong();
        }
    };
}
