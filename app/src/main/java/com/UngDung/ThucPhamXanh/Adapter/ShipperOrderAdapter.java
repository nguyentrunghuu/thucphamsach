package com.UngDung.ThucPhamXanh.Adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.ShipperOrders;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.Admin.AdminOrderStatusSheet;

import java.util.Objects;

/**
 * Danh sách đơn của shipper: ListAdapter + DiffUtil (chỉ vẽ lại dòng đổi). Bấm thẻ → chi tiết,
 * bấm biểu tượng điện thoại → gọi khách ngay. Đơn thu hộ (COD) hiện số tiền màu cam, đơn trả trước hiện "Đã thanh toán" màu xanh.
 */
public class ShipperOrderAdapter extends ListAdapter<AdminOrder, ShipperOrderAdapter.ViewHolder> {

    public interface Listener {
        void onOpen(AdminOrder order);
        void onCall(AdminOrder order);
    }

    private final Listener listener;
    private final String me;

    public ShipperOrderAdapter(String shipperUsername, Listener listener) {
        super(DIFF);
        this.me = shipperUsername;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shipper_order, parent, false);
        ViewHolder h = new ViewHolder(v);
        v.setOnClickListener(x -> {
            int pos = h.getAdapterPosition(); // recyclerview 1.1
            if (pos != RecyclerView.NO_POSITION) listener.onOpen(getItem(pos));
        });
        h.btnCall.setOnClickListener(x -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onCall(getItem(pos));
        });
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AdminOrder o = getItem(position);
        h.code.setText(o.displayCode());
        AdminOrderStatusSheet.bindStatusBadge(h.status, o.status);
        if (ShipperOrders.isWaitingForShipper(o)) h.status.setText("Chờ shipper nhận"); // admin đã chuyển "Đang giao" nhưng chưa gán ai
        h.name.setText(o.name.isEmpty() ? "(Không có tên)" : o.name);
        h.phone.setText(o.phone);
        h.btnCall.setVisibility(ShipperOrders.dialNumber(o.phone).isEmpty() ? View.GONE : View.VISIBLE);
        h.address.setText(o.address.isEmpty() ? "(Chưa có địa chỉ)" : o.address);

        String slot = ShipperOrders.deliverySlot(o);
        h.slot.setVisibility(slot.isEmpty() ? View.GONE : View.VISIBLE);
        h.slot.setText("Giao: " + slot);

        boolean cod = ShipperOrders.isCod(o.payment);
        h.payment.setText(ShipperOrders.paymentLabel(o));
        h.payment.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                cod ? R.color.admin_warning_text : R.color.admin_primary));

        if (ShipperOrders.canClaim(o)) h.hint.setText("Nhận đơn");
        else if (ShipperOrders.canComplete(o, me)) h.hint.setText("Xác nhận giao");
        else h.hint.setText("Xem chi tiết");
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView code, status, name, phone, address, slot, payment, hint;
        final ImageButton btnCall;

        ViewHolder(@NonNull View v) {
            super(v);
            code = v.findViewById(R.id.tv_order_id);
            status = v.findViewById(R.id.tv_status);
            name = v.findViewById(R.id.tv_customer_name);
            phone = v.findViewById(R.id.tv_phone);
            address = v.findViewById(R.id.tv_address);
            slot = v.findViewById(R.id.tv_slot);
            payment = v.findViewById(R.id.tv_payment);
            hint = v.findViewById(R.id.tv_action_hint);
            btnCall = v.findViewById(R.id.btn_call);
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
                    && Objects.equals(a.phone, b.phone) && Objects.equals(a.address, b.address)
                    && Objects.equals(a.payment, b.payment) && Objects.equals(a.shipperId, b.shipperId)
                    && Objects.equals(a.deliverySlot, b.deliverySlot) && Objects.equals(a.deliveryDate, b.deliveryDate);
        }
    };
}
