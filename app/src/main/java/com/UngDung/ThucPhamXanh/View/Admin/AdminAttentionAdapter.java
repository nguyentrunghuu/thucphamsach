package com.UngDung.ThucPhamXanh.View.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.DashboardStats.ProductAlert;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.squareup.picasso.Picasso;

import java.util.Objects;

/** Danh sách "Cần chú ý" trên dashboard: sản phẩm sắp/đã hết hàng hoặc sắp/đã hết hạn. */
public class AdminAttentionAdapter extends ListAdapter<AdminAttentionAdapter.Row, AdminAttentionAdapter.ViewHolder> {

    /** Một dòng: sản phẩm + cảnh báo của nó. */
    public static final class Row {
        public final Product product;
        public final ProductAlert alert;

        public Row(Product product, ProductAlert alert) {
            this.product = product;
            this.alert = alert;
        }
    }

    public interface OnRowClick {
        void onClick(Product product);
    }

    private final OnRowClick onRowClick;

    public AdminAttentionAdapter(OnRowClick onRowClick) {
        super(DIFF);
        this.onRowClick = onRowClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_attention, parent, false);
        ViewHolder holder = new ViewHolder(v);
        v.setOnClickListener(view -> {
            int pos = holder.getAdapterPosition(); // recyclerview 1.1: chưa có getBindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onRowClick.onClick(getItem(pos).product);
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Row row = getItem(position);
        Product p = row.product;

        h.name.setText(p.getTensp());
        StringBuilder sub = new StringBuilder();
        if (p.getLoaisp() != null && !p.getLoaisp().isEmpty()) sub.append(p.getLoaisp());
        if (p.getHansudung() != null && !p.getHansudung().isEmpty()) {
            if (sub.length() > 0) sub.append(" · ");
            sub.append("HSD ").append(p.getHansudung());
        }
        h.sub.setText(sub);

        // Ảnh chờ / lỗi: ô be trơn (icon bị centerCrop phóng to trông rất thô)
        String url = p.getHinhanh();
        if (url == null || url.trim().isEmpty()) {
            Picasso.get().cancelRequest(h.image);
            h.image.setImageDrawable(null);
        } else {
            Picasso.get().load(url).fit().centerCrop().into(h.image);
        }

        bindBadge(h.expiryBadge, row.alert.expiryLabel, row.alert.expiryLevel == ProductAlert.EXPIRED);
        bindBadge(h.stockBadge, row.alert.stockLabel, row.alert.stockLevel == ProductAlert.OUT_OF_STOCK);
    }

    /** Nhãn đỏ khi nghiêm trọng (đã hết), cam khi cảnh báo (sắp hết); không có nhãn → ẩn. */
    private static void bindBadge(TextView badge, String label, boolean severe) {
        if (label == null) {
            badge.setVisibility(View.GONE);
            return;
        }
        badge.setVisibility(View.VISIBLE);
        badge.setText(label);
        int bg = severe ? R.color.admin_danger_soft : R.color.admin_warning_soft;
        int fg = severe ? R.color.admin_danger : R.color.admin_warning_text;
        badge.setBackgroundTintList(ContextCompat.getColorStateList(badge.getContext(), bg));
        badge.setTextColor(ContextCompat.getColor(badge.getContext(), fg));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name, sub, expiryBadge, stockBadge;

        ViewHolder(@NonNull View v) {
            super(v);
            image = v.findViewById(R.id.img_attention);
            name = v.findViewById(R.id.tv_attention_name);
            sub = v.findViewById(R.id.tv_attention_sub);
            expiryBadge = v.findViewById(R.id.tv_badge_expiry);
            stockBadge = v.findViewById(R.id.tv_badge_stock);
        }
    }

    private static final DiffUtil.ItemCallback<Row> DIFF = new DiffUtil.ItemCallback<Row>() {
        @Override
        public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
            return Objects.equals(a.product.getId(), b.product.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
            Product x = a.product, y = b.product;
            return Objects.equals(x.getTensp(), y.getTensp())
                    && Objects.equals(x.getHinhanh(), y.getHinhanh())
                    && Objects.equals(x.getLoaisp(), y.getLoaisp())
                    && Objects.equals(x.getHansudung(), y.getHansudung())
                    && Objects.equals(a.alert.stockLabel, b.alert.stockLabel)
                    && Objects.equals(a.alert.expiryLabel, b.alert.expiryLabel);
        }
    };
}
