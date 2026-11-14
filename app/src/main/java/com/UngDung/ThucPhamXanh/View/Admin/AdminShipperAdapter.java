package com.UngDung.ThucPhamXanh.View.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.R;

import java.util.Objects;

/** Danh sách shipper trang admin (ListAdapter/DiffUtil). Bấm thẻ → thông tin, sửa, xóa. */
public class AdminShipperAdapter extends ListAdapter<AdminShipperAdapter.Row, AdminShipperAdapter.ViewHolder> {

    /** Một shipper + số đơn của họ (tính từ HoaDon.shipperId). */
    public static final class Row {
        public final String docId;
        public final String username;
        public final String name;
        public final String phone;
        public final int delivering;   // đơn đang giao
        public final int delivered;    // đơn đã giao

        public Row(String docId, String username, String name, String phone, int delivering, int delivered) {
            this.docId = docId;
            this.username = username == null ? "" : username;
            this.name = name == null ? "" : name;
            this.phone = phone == null ? "" : phone;
            this.delivering = delivering;
            this.delivered = delivered;
        }

        public String displayName() {
            return name.trim().isEmpty() ? username : name.trim();
        }
    }

    public interface Listener {
        void onOpen(Row shipper);
    }

    private final Listener listener;

    public AdminShipperAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.dong_admin_shipper, parent, false);
        ViewHolder h = new ViewHolder(v);
        v.setOnClickListener(x -> {
            int pos = h.getAdapterPosition(); // recyclerview 1.1
            if (pos != RecyclerView.NO_POSITION) listener.onOpen(getItem(pos));
        });
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Row r = getItem(position);
        String display = r.displayName();
        h.initial.setText(display.isEmpty() ? "?" : display.substring(0, 1).toUpperCase());
        h.name.setText(display);
        StringBuilder sub = new StringBuilder("@").append(r.username);
        if (!r.phone.trim().isEmpty()) sub.append(" · ").append(r.phone.trim());
        h.sub.setText(sub);
        if (r.delivering > 0) {
            h.badge.setText(r.delivering + " đơn đang giao");
        } else if (r.delivered > 0) {
            h.badge.setText(r.delivered + " đơn đã giao");
        } else {
            h.badge.setText("Rảnh");
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView initial, name, sub, badge;

        ViewHolder(@NonNull View v) {
            super(v);
            initial = v.findViewById(R.id.tv_shipper_initial);
            name = v.findViewById(R.id.tv_shipper_name);
            sub = v.findViewById(R.id.tv_shipper_sub);
            badge = v.findViewById(R.id.tv_shipper_badge);
        }
    }

    private static final DiffUtil.ItemCallback<Row> DIFF = new DiffUtil.ItemCallback<Row>() {
        @Override
        public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
            return Objects.equals(a.docId, b.docId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
            return a.username.equals(b.username) && a.name.equals(b.name) && a.phone.equals(b.phone)
                    && a.delivering == b.delivering && a.delivered == b.delivered;
        }
    };
}
