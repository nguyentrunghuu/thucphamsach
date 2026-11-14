package com.UngDung.ThucPhamXanh.Helper;

import android.util.Log;

import com.UngDung.ThucPhamXanh.Models.OrderStatus;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Đổi trạng thái đơn hàng — dùng chung cho admin (bottom sheet) và khách (tự hủy đơn).
 *
 *  - Chạy trong transaction: đọc trạng thái MỚI NHẤT rồi mới kiểm tra quy tắc, nên khi shipper / khách /
 *    admin khác vừa đổi đơn thì lần ghi này bị từ chối thay vì ghi đè.
 *  - Chỉ báo thành công sau khi Firestore ghi xong.
 *  - Hủy đơn (→ 6): lưu cancelledBy / cancelReason / thoiGianHuy và CỘNG LẠI tồn kho (soluong += sl,
 *    daban −= sl) cho từng dòng ChitietHoaDon/{uid}/ALL của đơn — tồn kho đã bị trừ lúc đặt hàng.
 */
public final class OrderActions {

    private static final String TAG = "OrderActions";

    public static final String BY_ADMIN = "admin";
    public static final String BY_CUSTOMER = "customer";

    private OrderActions() { }

    public interface Callback {
        void onSuccess(int newStatus);
        void onError(String message);
    }

    /** Lỗi nghiệp vụ (đơn đã đổi trạng thái...) để phân biệt với lỗi mạng. */
    private static final class RuleException extends RuntimeException {
        RuleException(String message) { super(message); }
    }

    /**
     * Admin đổi trạng thái theo {@link AdminOrderFlow}. reason chỉ dùng khi hủy (có thể rỗng).
     * Riêng "Đang giao" phải đi qua {@link #adminShip} vì cần biết shipper nào giao (nếu không, đơn sẽ
     * không hiện ra ở shipper nào cả).
     */
    public static void adminChangeStatus(String orderId, int to, String reason, Callback cb) {
        if (to == OrderStatus.DANG_GIAO) {
            cb.onError("Hãy chọn shipper để chuyển đơn sang \"Đang giao\"");
            return;
        }
        change(orderId, to, BY_ADMIN, reason, cb);
    }

    /** Admin giao đơn cho một shipper: "Đã xác nhận" → "Đang giao" + shipperId/shipperName/assignedAt, cùng một lần ghi. */
    public static void adminShip(String orderId, String shipperUsername, String shipperName, Callback cb) {
        assign(orderId, shipperUsername, shipperName, true, cb);
    }

    /** Admin gán / đổi shipper cho đơn đang "Đang giao" (không đổi trạng thái). Dùng cho đơn cũ chưa có shipper. */
    public static void adminAssignShipper(String orderId, String shipperUsername, String shipperName, Callback cb) {
        assign(orderId, shipperUsername, shipperName, false, cb);
    }

    private static void assign(String orderId, String shipperUsername, String shipperName, boolean ship, Callback cb) {
        if (shipperUsername == null || shipperUsername.trim().isEmpty()) {
            cb.onError("Chưa chọn shipper");
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("HoaDon").document(orderId);
        final String name = shipperName == null || shipperName.trim().isEmpty() ? shipperUsername : shipperName.trim();

        db.runTransaction(tx -> {
            DocumentSnapshot doc = tx.get(ref);
            if (!doc.exists()) throw new RuleException("Không tìm thấy đơn hàng");
            long current = FirestoreMapper.num(doc, "trangthai");
            if (ship && !AdminOrderFlow.canShip(current)) {
                throw new RuleException("Đơn đang \"" + OrderStatus.label(current)
                        + "\", không giao cho shipper được. Danh sách sẽ tự cập nhật.");
            }
            if (!ship && !AdminOrderFlow.canAssignShipper(current)) {
                throw new RuleException("Chỉ gán shipper được cho đơn đang \"Đang giao\" (đơn đang \""
                        + OrderStatus.label(current) + "\").");
            }
            Map<String, Object> update = new HashMap<>();
            if (ship) update.put("trangthai", OrderStatus.DANG_GIAO);
            update.put("shipperId", shipperUsername.trim());
            update.put("shipperName", name);
            update.put("assignedAt", FieldValue.serverTimestamp());
            tx.update(ref, update);
            return null;
        }).addOnSuccessListener(unused -> cb.onSuccess(OrderStatus.DANG_GIAO))
          .addOnFailureListener(e -> {
              Log.e(TAG, "Gán shipper cho đơn " + orderId + " thất bại", e);
              cb.onError(messageOf(e));
          });
    }

    /** Khách tự hủy đơn (chỉ khi {@link OrderStatus#customerCanCancel}). */
    public static void customerCancel(String orderId, String reason, Callback cb) {
        change(orderId, OrderStatus.DA_HUY, BY_CUSTOMER, reason, cb);
    }

    private static void change(String orderId, int to, String by, String reason, Callback cb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("HoaDon").document(orderId);
        final String[] uidHolder = new String[1];

        db.runTransaction(tx -> {
            DocumentSnapshot doc = tx.get(ref);
            if (!doc.exists()) throw new RuleException("Không tìm thấy đơn hàng");
            long current = FirestoreMapper.num(doc, "trangthai");
            boolean allowed = BY_CUSTOMER.equals(by)
                    ? (to == OrderStatus.DA_HUY && OrderStatus.customerCanCancel(current))
                    : AdminOrderFlow.canTransition(current, to);
            if (!allowed) {
                throw new RuleException(current == to
                        ? "Đơn đã ở trạng thái \"" + OrderStatus.label(to) + "\""
                        : "Đơn đang \"" + OrderStatus.label(current) + "\", không chuyển sang \""
                        + OrderStatus.label(to) + "\" được. Danh sách sẽ tự cập nhật.");
            }
            uidHolder[0] = FirestoreMapper.str(doc, "UID");

            Map<String, Object> update = new HashMap<>();
            update.put("trangthai", to);
            if (to == OrderStatus.DA_HUY) {
                update.put("cancelledBy", by);
                update.put("cancelReason", reason == null ? "" : reason.trim());
                update.put("thoiGianHuy", FieldValue.serverTimestamp());
            }
            tx.update(ref, update);
            return null;
        }).addOnSuccessListener(unused -> {
            if (to == OrderStatus.DA_HUY) restock(db, uidHolder[0], orderId);
            cb.onSuccess(to);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Đổi trạng thái đơn " + orderId + " → " + to + " thất bại", e);
            cb.onError(messageOf(e));
        });
    }

    private static String messageOf(Exception e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof RuleException) return t.getMessage();
            t = t.getCause();
        }
        if (e instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
            return "Không có kết nối mạng, thử lại sau";
        }
        return "Cập nhật thất bại: " + e.getMessage();
    }

    /** Trả hàng về kho cho đơn vừa hủy. Lỗi chỉ ghi log (trạng thái đã đổi xong). */
    private static void restock(FirebaseFirestore db, String uid, String orderId) {
        if (uid == null || uid.isEmpty()) {
            Log.w(TAG, "Đơn " + orderId + " không có UID, bỏ qua cộng kho");
            return;
        }
        db.collection("ChitietHoaDon").document(uid).collection("ALL")
                .whereEqualTo("id_hoadon", orderId).get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot line : snap) {
                        String productId = line.getString("id_product");
                        long qty = FirestoreMapper.num(line, "soluong");
                        if (productId == null || productId.isEmpty() || qty <= 0) continue;
                        // update riêng từng dòng: sản phẩm đã bị xóa hẳn thì chỉ dòng đó lỗi
                        // (không dùng set/merge vì sẽ tạo ra document "sản phẩm ma" chỉ có soluong)
                        // transaction: "daban" không bao giờ xuống dưới 0 (kể cả khi sản phẩm chưa có trường này)
                        DocumentReference ref = db.collection("SanPham").document(productId);
                        db.runTransaction(tx -> {
                            DocumentSnapshot p = tx.get(ref);
                            if (!p.exists()) {
                                throw new FirebaseFirestoreException("Sản phẩm không tồn tại",
                                        FirebaseFirestoreException.Code.NOT_FOUND);
                            }
                            long sold = FirestoreMapper.num(p, "daban");
                            Map<String, Object> upd = new HashMap<>();
                            upd.put("soluong", FieldValue.increment(qty));
                            upd.put("daban", Math.max(0, sold - qty));
                            tx.update(ref, upd);
                            return null;
                        })
                                .addOnSuccessListener(v -> Log.d(TAG, "Cộng lại " + qty + " vào kho " + productId))
                                .addOnFailureListener(e -> Log.e(TAG, "Cộng lại kho thất bại: " + productId, e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Không đọc được chi tiết đơn " + orderId, e));
    }
}
