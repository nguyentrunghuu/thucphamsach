package com.UngDung.ThucPhamXanh.View.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.AdminOrderFlow;
import com.UngDung.ThucPhamXanh.Helper.MoneyUtil;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.button.MaterialButton;

import java.util.Objects;

/**
 * Danh sách đơn trang admin (riêng cho admin — không dùng chung HoaDonAdapter của khách).
 * Bấm dòng → chi tiết; nút "Cập nhật" (chỉ hiện khi còn đổi được) → bottom sheet trạng thái.
 */
public class AdminOrderAdapter extends ListAdapter<AdminOrder, AdminOrderAdapter.ViewHolder> {

    public interface Listener {
        void onOpen(AdminOrder order);
        void onUpdateStatus(AdminOrder order);
    }

    private final Listener listener;

    public AdminOrderAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_order, parent, false);
        ViewHolder h = new ViewHolder(v);
        v.setOnClickListener(x -> {
            int pos = h.getAdapterPosition(); // recyclerview 1.1
            if (pos != RecyclerView.NO_POSITION) listener.onOpen(getItem(pos));
        });
        h.btnUpdate.setOnClickListener(x -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onUpdateStatus(getItem(pos));
        });
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AdminOrder o = getItem(position);
        h.code.setText(o.displayCode());
        AdminOrderStatusSheet.bindStatusBadge(h.status, o.status);
        h.name.setText(o.name.isEmpty() ? "(Không có tên)" : o.name);

        StringBuilder sub = new StringBuilder();
        append(sub, o.phone);
        append(sub, o.date);
        append(sub, o.payment);
        h.sub.setText(sub);

        h.address.setText(o.address);
        h.address.setVisibility(o.address.isEmpty() ? View.GONE : View.VISIBLE);
        h.total.setText(MoneyUtil.formatVnd(o.total));
        h.btnUpdate.setVisibility(AdminOrderFlow.nextStatuses(o.status).length > 0 ? View.VISIBLE : View.GONE);
    }

    private static void append(StringBuilder sb, String s) {
        if (s == null || s.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append(" · ");
        sb.append(s.trim());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView code, status, name, sub, address, total;
        final MaterialButton btnUpdate;

        ViewHolder(@NonNull View v) {
            super(v);
            code = v.findViewById(R.id.tv_order_code);
            status = v.findViewById(R.id.tv_order_status);
            name = v.findViewById(R.id.tv_order_name);
            sub = v.findViewById(R.id.tv_order_sub);
            address = v.findViewById(R.id.tv_order_address);
            total = v.findViewById(R.id.tv_order_total);
            btnUpdate = v.findViewById(R.id.btn_order_update);
        }
    }

    private static final DiffUtil.ItemCallback<AdminOrder> DIFF = new DiffUtil.ItemCallback<AdminOrder>() {
        @Override
        public boolean areItemsTheSame(@NonNull AdminOrder a, @NonNull AdminOrder b) {
            return Objects.equals(a.id, b.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull AdminOrder a, @NonNull AdminOrder b) {
            return a.status == b.status && a.total == b.total
                    && Objects.equals(a.code, b.code) && Objects.equals(a.name, b.name)
                    && Objects.equals(a.phone, b.phone) && Objects.equals(a.date, b.date)
                    && Objects.equals(a.payment, b.payment) && Objects.equals(a.address, b.address);
        }
    };
}
