package com.UngDung.ThucPhamXanh.Adapter;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.CustomerOrders;
import com.UngDung.ThucPhamXanh.Helper.CustomerOrders.Entry;
import com.UngDung.ThucPhamXanh.Helper.MoneyUtil;
import com.UngDung.ThucPhamXanh.Helper.OrderMath;
import com.UngDung.ThucPhamXanh.Models.OrderStatus;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import java.util.Objects;

/**
 * Danh sách "Đơn hàng của tôi". Mỗi thẻ cho thấy ngay: mã đơn, trạng thái, ảnh + tên sản phẩm,
 * đơn đang ở đâu (kèm lịch giao / shipper), tổng tiền và nút hành động hợp với trạng thái
 * (Hủy đơn / Theo dõi / Mua lại).
 */
public class CustomerOrderAdapter extends ListAdapter<Entry, CustomerOrderAdapter.ViewHolder> {

    public interface Listener {
        void onOpen(Entry entry);
        void onCancel(Entry entry);
        void onReorder(Entry entry);
    }

    private static final int ACTION_NONE = 0;
    private static final int ACTION_OPEN = 1;
    private static final int ACTION_CANCEL = 2;
    private static final int ACTION_REORDER = 3;

    private final Listener listener;

    public CustomerOrderAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer_order, parent, false);
        ViewHolder h = new ViewHolder(v);
        v.setOnClickListener(x -> dispatch(h, ACTION_OPEN));
        h.btnSecondary.setOnClickListener(x -> dispatch(h, h.secondaryAction));
        h.btnPrimary.setOnClickListener(x -> dispatch(h, h.primaryAction));
        return h;
    }

    private void dispatch(ViewHolder h, int action) {
        int pos = h.getAdapterPosition(); // recyclerview 1.1
        if (pos == RecyclerView.NO_POSITION) return;
        Entry e = getItem(pos);
        switch (action) {
            case ACTION_OPEN: listener.onOpen(e); break;
            case ACTION_CANCEL: listener.onCancel(e); break;
            case ACTION_REORDER: listener.onReorder(e); break;
            default: break;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Entry e = getItem(position);
        AdminOrder o = e.order;

        h.code.setText(o.displayCode());
        h.date.setText(o.date.isEmpty() ? "" : "Đặt ngày " + o.date);
        h.date.setVisibility(o.date.isEmpty() ? View.GONE : View.VISIBLE);
        bindStatusPill(h.status, o.status);
        bindThumbs(h, e);
        bindMessage(h, o);

        String summary = CustomerOrders.itemsSummary(e.lines);
        h.items.setText(summary);
        h.items.setVisibility(summary.isEmpty() ? View.GONE : View.VISIBLE);

        StringBuilder count = new StringBuilder();
        int qty = e.itemCount();
        if (qty > 0) count.append(qty).append(" sản phẩm");
        if (CustomerOrders.isPrepaid(o.payment)) {
            append(count, "Đã thanh toán");
        } else if (o.status != OrderStatus.DA_GIAO && o.status != OrderStatus.DA_HUY) {
            append(count, "Thanh toán khi nhận");
        }
        h.count.setText(count);
        h.total.setText(MoneyUtil.formatVnd(o.total));

        bindActions(h, o.status);
    }

    private static void append(StringBuilder sb, String s) {
        if (sb.length() > 0) sb.append(" · ");
        sb.append(s);
    }

    private static void bindStatusPill(TextView tv, long status) {
        tv.setText(OrderStatus.label(status));
        tv.setTextColor(OrderStatus.textColor(status));
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(OrderStatus.backgroundColor(status));
        pill.setCornerRadius(100 * tv.getResources().getDisplayMetrics().density);
        tv.setBackground(pill);
    }

    private static void bindThumbs(ViewHolder h, Entry e) {
        int n = e.lines.size();
        h.rowThumbs.setVisibility(n == 0 ? View.GONE : View.VISIBLE);
        ImageView[] imgs = {h.thumb1, h.thumb2, h.thumb3};
        int showMore = n > 3 ? n - 3 : 0;
        for (int i = 0; i < imgs.length; i++) {
            if (i < n) {
                imgs[i].setVisibility(View.VISIBLE);
                String url = e.lines.get(i).image;
                if (url != null && !url.trim().isEmpty()) {
                    Picasso.get().load(url.trim()).into(imgs[i]);
                } else {
                    Picasso.get().cancelRequest(imgs[i]);
                    imgs[i].setImageDrawable(null);
                }
            } else {
                Picasso.get().cancelRequest(imgs[i]);
                imgs[i].setVisibility(View.GONE);
            }
        }
        h.thumbMore.setVisibility(showMore > 0 ? View.VISIBLE : View.GONE);
        h.thumbMore.setText("+" + showMore);
    }

    private static void bindMessage(ViewHolder h, AdminOrder o) {
        StringBuilder msg = new StringBuilder(CustomerOrders.statusMessage(o));
        String schedule = CustomerOrders.deliverySchedule(o);
        if (!schedule.isEmpty() && (o.status == OrderStatus.DANG_GIAO || o.status == OrderStatus.DA_XAC_NHAN
                || o.status == OrderStatus.DA_DAT || o.status == OrderStatus.DANG_XU_LY)) {
            msg.append("\nGiao dự kiến: ").append(schedule);
        }
        int color = OrderStatus.textColor(o.status);
        h.message.setText(msg);
        h.message.setTextColor(color);
        h.message.setVisibility(msg.length() == 0 ? View.GONE : View.VISIBLE);
        h.boxMessage.setVisibility(msg.length() == 0 ? View.GONE : View.VISIBLE);

        GradientDrawable box = new GradientDrawable();
        box.setColor(OrderStatus.backgroundColor(o.status));
        box.setCornerRadius(12 * h.itemView.getResources().getDisplayMetrics().density);
        h.boxMessage.setBackground(box);

        int icon;
        switch ((int) o.status) {
            case OrderStatus.DANG_GIAO:
            case OrderStatus.DA_XAC_NHAN: icon = R.drawable.ic_ui_truck; break;
            case OrderStatus.DA_GIAO: icon = R.drawable.ic_ui_leaf; break;
            case OrderStatus.DA_HUY: icon = R.drawable.ic_admin_warning; break;
            default: icon = R.drawable.ic_admin_order; break;
        }
        h.imgMessage.setImageResource(icon);
        ImageViewCompat.setImageTintList(h.imgMessage, ColorStateList.valueOf(color));
    }

    /** Nút phụ (viền) + nút chính (đặc) theo trạng thái. */
    private static void bindActions(ViewHolder h, long status) {
        h.secondaryAction = ACTION_NONE;
        h.primaryAction = ACTION_NONE;
        switch ((int) status) {
            case OrderStatus.DA_DAT:
            case OrderStatus.DANG_XU_LY:
                h.secondaryAction = ACTION_CANCEL;
                styleSecondary(h.btnSecondary, "Hủy đơn", c(h, R.color.ui_danger), 0xFFEF9A9A);
                h.primaryAction = ACTION_OPEN;
                h.btnPrimary.setText("Xem chi tiết");
                break;
            case OrderStatus.DA_XAC_NHAN:
                h.primaryAction = ACTION_OPEN;
                h.btnPrimary.setText("Xem chi tiết");
                break;
            case OrderStatus.DANG_GIAO:
                h.primaryAction = ACTION_OPEN;
                h.btnPrimary.setText("Theo dõi đơn");
                break;
            case OrderStatus.DA_GIAO:
                h.secondaryAction = ACTION_OPEN;
                styleSecondary(h.btnSecondary, "Đánh giá", c(h, R.color.ui_green_text), 0xFFA5D6A7);
                h.primaryAction = ACTION_REORDER;
                h.btnPrimary.setText("Mua lại");
                break;
            case OrderStatus.DA_HUY:
                h.secondaryAction = ACTION_OPEN;
                styleSecondary(h.btnSecondary, "Chi tiết", c(h, R.color.ui_green_text), 0xFFA5D6A7);
                h.primaryAction = ACTION_REORDER;
                h.btnPrimary.setText("Mua lại");
                break;
            default:
                h.primaryAction = ACTION_OPEN;
                h.btnPrimary.setText("Xem chi tiết");
                break;
        }
        h.btnSecondary.setVisibility(h.secondaryAction == ACTION_NONE ? View.GONE : View.VISIBLE);
        h.btnPrimary.setVisibility(h.primaryAction == ACTION_NONE ? View.GONE : View.VISIBLE);
    }

    private static int c(ViewHolder h, int res) {
        return androidx.core.content.ContextCompat.getColor(h.itemView.getContext(), res);
    }

    private static void styleSecondary(MaterialButton b, String text, int textColor, int strokeColor) {
        b.setText(text);
        b.setTextColor(textColor);
        b.setStrokeColor(ColorStateList.valueOf(strokeColor));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView code, date, status, items, message, count, total, thumbMore;
        final ImageView thumb1, thumb2, thumb3, imgMessage;
        final View rowThumbs, boxMessage;
        final MaterialButton btnSecondary, btnPrimary;
        int secondaryAction = ACTION_NONE;
        int primaryAction = ACTION_NONE;

        ViewHolder(@NonNull View v) {
            super(v);
            code = v.findViewById(R.id.tv_order_code);
            date = v.findViewById(R.id.tv_order_date);
            status = v.findViewById(R.id.tv_order_status);
            items = v.findViewById(R.id.tv_order_items);
            message = v.findViewById(R.id.tv_order_message);
            count = v.findViewById(R.id.tv_order_count);
            total = v.findViewById(R.id.tv_order_total);
            thumbMore = v.findViewById(R.id.tv_thumb_more);
            thumb1 = v.findViewById(R.id.img_thumb_1);
            thumb2 = v.findViewById(R.id.img_thumb_2);
            thumb3 = v.findViewById(R.id.img_thumb_3);
            imgMessage = v.findViewById(R.id.img_message);
            rowThumbs = v.findViewById(R.id.row_thumbs);
            boxMessage = v.findViewById(R.id.box_message);
            btnSecondary = v.findViewById(R.id.btn_cancel);
            btnPrimary = v.findViewById(R.id.btn_primary);
        }
    }

    private static final DiffUtil.ItemCallback<Entry> DIFF = new DiffUtil.ItemCallback<Entry>() {
        @Override
        public boolean areItemsTheSame(@NonNull Entry a, @NonNull Entry b) {
            return Objects.equals(a.order.id, b.order.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Entry a, @NonNull Entry b) {
            AdminOrder x = a.order, y = b.order;
            if (x.status != y.status || x.total != y.total || a.lines.size() != b.lines.size()
                    || !Objects.equals(x.shipperName, y.shipperName)
                    || !Objects.equals(x.cancelReason, y.cancelReason)
                    || !Objects.equals(x.cancelledBy, y.cancelledBy)
                    || !Objects.equals(x.deliveryDate, y.deliveryDate)
                    || !Objects.equals(x.deliverySlot, y.deliverySlot)
                    || !Objects.equals(x.payment, y.payment)) {
                return false;
            }
            for (int i = 0; i < a.lines.size(); i++) {
                OrderMath.Line p = a.lines.get(i), q = b.lines.get(i);
                if (!Objects.equals(p.name, q.name) || !Objects.equals(p.image, q.image)
                        || p.quantity != q.quantity) {
                    return false;
                }
            }
            return true;
        }
    };
}
