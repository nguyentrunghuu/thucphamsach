package com.UngDung.ThucPhamXanh.View.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.PromotionRules;
import com.UngDung.ThucPhamXanh.R;

import java.util.Objects;

/** Danh sách loại sản phẩm + quy tắc khuyến mãi (ListAdapter/DiffUtil). Bấm một dòng → đặt / sửa quy tắc. */
public class AdminPromotionAdapter extends ListAdapter<AdminPromotionAdapter.Row, AdminPromotionAdapter.ViewHolder> {

    public static final class Row {
        public final String category;
        public final PromotionRules.Rule rule;   // null = chưa có khuyến mãi
        public final boolean orphan;              // loại không còn trong danh mục

        public Row(String category, PromotionRules.Rule rule, boolean orphan) {
            this.category = category;
            this.rule = rule;
            this.orphan = orphan;
        }
    }

    public interface Listener {
        void onEdit(Row row);
    }

    private final Listener listener;

    public AdminPromotionAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_promotion, parent, false);
        ViewHolder h = new ViewHolder(v);
        v.setOnClickListener(x -> {
            int pos = h.getAdapterPosition(); // recyclerview 1.1
            if (pos != RecyclerView.NO_POSITION) listener.onEdit(getItem(pos));
        });
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Row r = getItem(position);
        h.category.setText(r.category);
        if (r.orphan) {
            h.rule.setText("Danh mục không còn tồn tại · " + PromotionRules.describe(r.rule).toLowerCase());
            h.rule.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.admin_danger));
        } else {
            h.rule.setText(PromotionRules.describe(r.rule));
            h.rule.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.admin_text_muted));
        }
        if (r.rule != null) {
            h.badge.setVisibility(View.VISIBLE);
            h.badge.setText("-" + r.rule.percent + "%");
        } else {
            h.badge.setVisibility(View.GONE);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView category, rule, badge;

        ViewHolder(@NonNull View v) {
            super(v);
            category = v.findViewById(R.id.tv_promo_category);
            rule = v.findViewById(R.id.tv_promo_rule);
            badge = v.findViewById(R.id.tv_promo_badge);
        }
    }

    private static final DiffUtil.ItemCallback<Row> DIFF = new DiffUtil.ItemCallback<Row>() {
        @Override
        public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
            return Objects.equals(a.category, b.category);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
            return Objects.equals(a.rule, b.rule) && a.orphan == b.orphan;
        }
    };
}
