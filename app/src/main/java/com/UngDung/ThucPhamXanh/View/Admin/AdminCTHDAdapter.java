package com.UngDung.ThucPhamXanh.View.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.MoneyUtil;
import com.UngDung.ThucPhamXanh.Helper.OrderMath;
import com.UngDung.ThucPhamXanh.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/** Các sản phẩm trong chi tiết đơn admin. Giá lấy từ {@link OrderMath.Line} (giá lúc đặt nếu có). */
public class AdminCTHDAdapter extends RecyclerView.Adapter<AdminCTHDAdapter.ViewHolder> {

    private final List<OrderMath.Line> lines = new ArrayList<>();

    public void setLines(List<OrderMath.Line> data) {
        lines.clear();
        if (data != null) lines.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_order_line, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        OrderMath.Line l = lines.get(position);
        h.name.setText(l.name);
        h.qty.setText(l.quantity + " × " + MoneyUtil.formatVnd(l.unitPrice) + (l.priceIsCurrent ? " *" : ""));
        h.total.setText(MoneyUtil.formatVnd(l.lineTotal()));
        if (l.image == null || l.image.trim().isEmpty()) {
            Picasso.get().cancelRequest(h.image);
            h.image.setImageDrawable(null);
        } else {
            Picasso.get().load(l.image).fit().centerCrop().into(h.image);
        }
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name, qty, total;

        ViewHolder(@NonNull View v) {
            super(v);
            image = v.findViewById(R.id.img_order_line);
            name = v.findViewById(R.id.tv_order_line_name);
            qty = v.findViewById(R.id.tv_order_line_qty);
            total = v.findViewById(R.id.tv_order_line_total);
        }
    }
}
