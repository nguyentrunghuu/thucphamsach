package com.UngDung.ThucPhamXanh.View.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.AdminUsers;
import com.UngDung.ThucPhamXanh.Helper.MoneyUtil;
import com.UngDung.ThucPhamXanh.R;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

/** Danh sách khách hàng trang admin (ListAdapter/DiffUtil). Bấm thẻ → thông tin chi tiết. */
public class AdminUsersAdapter extends ListAdapter<AdminUsers.Row, AdminUsersAdapter.ViewHolder> {

    public interface Listener {
        void onOpen(AdminUsers.Row user);
    }

    private final Listener listener;

    public AdminUsersAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.dong_admin_users, parent, false);
        ViewHolder h = new ViewHolder(v);
        v.setOnClickListener(x -> {
            int pos = h.getAdapterPosition(); // recyclerview 1.1
            if (pos != RecyclerView.NO_POSITION) listener.onOpen(getItem(pos));
        });
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AdminUsers.Row u = getItem(position);
        h.name.setText(u.displayName());
        h.email.setText(u.email.isEmpty() ? "(chưa có email)" : u.email);
        h.phone.setText(u.phone.trim().isEmpty() ? "Chưa có số điện thoại" : u.phone.trim());

        if (u.orders > 0) {
            h.orders.setText(u.orders + " đơn");
            h.spent.setText(u.spent > 0 ? MoneyUtil.formatCompact(u.spent) : "chưa giao");
        } else {
            h.orders.setText("Chưa mua");
            h.spent.setText("");
        }

        String avatar = u.avatar == null ? "" : u.avatar.trim();
        if (!avatar.isEmpty() && avatar.startsWith("http")) {
            Picasso.get().load(avatar).placeholder(R.drawable.ic_user).error(R.drawable.ic_user).into(h.avatar);
        } else {
            Picasso.get().cancelRequest(h.avatar);
            h.avatar.setImageResource(R.drawable.ic_user);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView avatar;
        final TextView name, email, phone, orders, spent;

        ViewHolder(@NonNull View v) {
            super(v);
            avatar = v.findViewById(R.id.cir_user);
            name = v.findViewById(R.id.tv_user_name);
            email = v.findViewById(R.id.tv_user_email);
            phone = v.findViewById(R.id.tv_user_phone);
            orders = v.findViewById(R.id.tv_user_orders);
            spent = v.findViewById(R.id.tv_user_spent);
        }
    }

    private static final DiffUtil.ItemCallback<AdminUsers.Row> DIFF = new DiffUtil.ItemCallback<AdminUsers.Row>() {
        @Override
        public boolean areItemsTheSame(@NonNull AdminUsers.Row a, @NonNull AdminUsers.Row b) {
            return Objects.equals(a.uid, b.uid);
        }

        @Override
        public boolean areContentsTheSame(@NonNull AdminUsers.Row a, @NonNull AdminUsers.Row b) {
            return Objects.equals(a.name, b.name) && Objects.equals(a.email, b.email)
                    && Objects.equals(a.phone, b.phone) && Objects.equals(a.avatar, b.avatar)
                    && a.orders == b.orders && a.spent == b.spent;
        }
    };
}
