package com.UngDung.ThucPhamXanh.View.Admin;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.AdminOrderFlow;
import com.UngDung.ThucPhamXanh.Helper.OrderActions;
import com.UngDung.ThucPhamXanh.Models.OrderStatus;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Bottom sheet đổi trạng thái đơn — MỘT chỗ duy nhất cho cả danh sách đơn và chi tiết đơn
 * (thay cho hai DiaLogUpDate cũ vốn cho nhảy cóc "Đã đặt → Đã giao" và báo thành công trước khi ghi).
 *
 * Lựa chọn lấy từ {@link AdminOrderFlow#nextStatuses}; hủy đơn bắt buộc nhập lý do.
 * Ghi qua {@link OrderActions} (transaction + cộng lại kho khi hủy); chỉ đóng khi ghi thành công.
 */
public final class AdminOrderStatusSheet {

    public interface OnChanged {
        void onChanged(int newStatus);
    }

    private AdminOrderStatusSheet() { }

    public static void show(Context context, AdminOrder order, OnChanged onChanged) {
        int[] options = AdminOrderFlow.nextStatuses(order.status);
        if (options.length == 0) {
            Toast.makeText(context, "Đơn " + OrderStatus.label(order.status).toLowerCase()
                    + " — không đổi trạng thái được nữa", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View v = LayoutInflater.from(context).inflate(R.layout.sheet_admin_order_status, null);
        dialog.setContentView(v);

        TextView tvOrder = v.findViewById(R.id.tv_sheet_order);
        TextView tvCurrent = v.findViewById(R.id.tv_sheet_current);
        RadioGroup group = v.findViewById(R.id.rg_sheet_status);
        TextInputLayout tilReason = v.findViewById(R.id.til_sheet_reason);
        TextView edtReason = v.findViewById(R.id.edt_sheet_reason);
        TextView tvHint = v.findViewById(R.id.tv_sheet_hint);
        MaterialButton btnApply = v.findViewById(R.id.btn_sheet_apply);
        v.findViewById(R.id.btn_sheet_close).setOnClickListener(x -> dialog.dismiss());

        tvOrder.setText(order.displayCode() + (order.name.isEmpty() ? "" : " · " + order.name));
        bindStatusBadge(tvCurrent, order.status);

        final int[] selected = {0};
        for (int status : options) {
            RadioButton rb = new RadioButton(context);
            rb.setId(View.generateViewId());
            rb.setTag(status);
            rb.setText(optionLabel(status));
            rb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            rb.setTypeface(ResourcesCompat.getFont(context, R.font.be_vietnam_pro));
            int color = ContextCompat.getColor(context,
                    status == OrderStatus.DA_HUY ? R.color.admin_danger : R.color.admin_text);
            rb.setTextColor(color);
            rb.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(context,
                    status == OrderStatus.DA_HUY ? R.color.admin_danger : R.color.admin_primary)));
            rb.setMinHeight(context.getResources().getDimensionPixelSize(R.dimen.admin_touch_min));
            group.addView(rb);
        }

        Runnable refresh = () -> {
            int s = selected[0];
            boolean cancel = s == OrderStatus.DA_HUY;
            tilReason.setVisibility(cancel ? View.VISIBLE : View.GONE);
            String hint = hintFor(s);
            tvHint.setVisibility(hint == null ? View.GONE : View.VISIBLE);
            tvHint.setText(hint);
            boolean ready = s != 0 && (!cancel || edtReason.getText().toString().trim().length() >= 3);
            btnApply.setEnabled(ready);
            btnApply.setText(s == 0 ? "Chọn trạng thái" : (cancel ? "Hủy đơn" : "Chuyển: " + OrderStatus.label(s)));
            btnApply.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context,
                    !ready ? R.color.admin_outline : (cancel ? R.color.admin_danger : R.color.admin_primary))));
        };
        group.setOnCheckedChangeListener((g, id) -> {
            View rb = g.findViewById(id);
            selected[0] = rb == null ? 0 : (int) rb.getTag();
            refresh.run();
        });
        edtReason.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { refresh.run(); }
        });
        // Chỉ có một bước tiếp theo hợp lệ (ngoài hủy) thì chọn sẵn
        if (options.length >= 1 && options[0] != OrderStatus.DA_HUY) group.check(group.getChildAt(0).getId());
        refresh.run();

        btnApply.setOnClickListener(x -> {
            int to = selected[0];
            if (to == 0) return;
            btnApply.setEnabled(false);
            btnApply.setText("Đang lưu…");
            dialog.setCancelable(false);
            OrderActions.adminChangeStatus(order.id, to, edtReason.getText().toString(), new OrderActions.Callback() {
                @Override
                public void onSuccess(int newStatus) {
                    dialog.dismiss();
                    Toast.makeText(context, "Đã chuyển đơn sang \"" + OrderStatus.label(newStatus) + "\""
                            + (newStatus == OrderStatus.DA_HUY ? ", đã trả hàng về kho" : ""), Toast.LENGTH_SHORT).show();
                    if (onChanged != null) onChanged.onChanged(newStatus);
                }

                @Override
                public void onError(String message) {
                    dialog.setCancelable(true);
                    refresh.run();
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.setOnShowListener(d -> {
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) BottomSheetBehavior.from(sheet).setState(BottomSheetBehavior.STATE_EXPANDED);
        });
        dialog.show();
    }

    private static String optionLabel(int status) {
        switch (status) {
            case OrderStatus.DANG_XU_LY: return "Đang xử lý — đã tiếp nhận, đang chuẩn bị";
            case OrderStatus.DA_XAC_NHAN: return "Đã xác nhận — shipper có thể nhận đơn";
            case OrderStatus.DANG_GIAO: return "Đang giao — đã giao cho shipper";
            case OrderStatus.DA_GIAO: return "Đã giao — khách đã nhận hàng";
            case OrderStatus.DA_HUY: return "Hủy đơn";
            default: return OrderStatus.label(status);
        }
    }

    private static String hintFor(int status) {
        switch (status) {
            case OrderStatus.DANG_GIAO: return "Từ bước này khách và admin không hủy đơn được nữa.";
            case OrderStatus.DA_GIAO: return "Trạng thái cuối, không đổi lại được. Doanh thu được tính từ đơn đã giao.";
            case OrderStatus.DA_HUY: return "Số lượng sản phẩm trong đơn sẽ được cộng lại vào kho.";
            default: return null;
        }
    }

    /** Nhãn trạng thái dạng viên thuốc, màu theo {@link OrderStatus}. */
    public static void bindStatusBadge(TextView tv, long status) {
        tv.setText(OrderStatus.label(status));
        tv.setTextColor(OrderStatus.textColor(status));
        tv.setBackgroundTintList(ColorStateList.valueOf(OrderStatus.backgroundColor(status)));
    }
}
