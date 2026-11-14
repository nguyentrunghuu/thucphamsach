package com.UngDung.ThucPhamXanh.View;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Helper.OrderActions;
import com.UngDung.ThucPhamXanh.Helper.OrderMath;
import com.UngDung.ThucPhamXanh.Helper.OrderReorder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** Thao tác dùng chung của màn danh sách đơn và màn chi tiết đơn phía khách. */
public final class OrderUi {

    private static final String[] CANCEL_REASONS = {
            "Muốn thay đổi địa chỉ / số điện thoại",
            "Muốn đổi sản phẩm hoặc số lượng",
            "Đặt nhầm đơn",
            "Tìm được giá tốt hơn",
            "Không còn nhu cầu",
            "Lý do khác"
    };

    private OrderUi() { }

    public interface Done {
        void run();
    }

    /** Hỏi lý do rồi hủy đơn (transaction kiểm tra lại trạng thái + cộng lại kho). */
    public static void confirmCancel(Activity activity, String orderId, Done onDone) {
        final int[] chosen = {0};
        new MaterialAlertDialogBuilder(activity)
                .setTitle("Hủy đơn hàng?")
                .setSingleChoiceItems(CANCEL_REASONS, 0, (d, which) -> chosen[0] = which)
                .setNegativeButton("Giữ đơn", null)
                .setPositiveButton("Hủy đơn", (d, w) -> {
                    OrderActions.customerCancel(orderId, CANCEL_REASONS[chosen[0]], new OrderActions.Callback() {
                        @Override
                        public void onSuccess(int newStatus) {
                            if (activity.isFinishing()) return;
                            Toast.makeText(activity, "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show();
                            if (onDone != null) onDone.run();
                        }

                        @Override
                        public void onError(String message) {
                            if (activity.isFinishing()) return;
                            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .show();
    }

    /** Thêm lại các sản phẩm của đơn vào giỏ rồi mở giỏ hàng. */
    public static void reorder(Activity activity, List<OrderMath.Line> lines) {
        Toast.makeText(activity, "Đang thêm vào giỏ hàng...", Toast.LENGTH_SHORT).show();
        OrderReorder.reorder(lines, new OrderReorder.Callback() {
            @Override
            public void onDone(int added, int skipped) {
                if (activity.isFinishing()) return;
                if (added <= 0) {
                    Toast.makeText(activity, "Các sản phẩm trong đơn này hiện đã hết hàng hoặc ngừng bán",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                String msg = skipped > 0
                        ? "Đã thêm " + added + " sản phẩm vào giỏ (" + skipped + " sản phẩm hết hàng / ngừng bán)"
                        : "Đã thêm " + added + " sản phẩm vào giỏ hàng";
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                openCart(activity);
            }

            @Override
            public void onError(String message) {
                if (activity.isFinishing()) return;
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static void openCart(Context context) {
        context.startActivity(new Intent(context, CartActivity.class));
    }
}
