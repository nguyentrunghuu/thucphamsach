package com.UngDung.ThucPhamXanh.Helper;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Mua lại": đưa các sản phẩm của một đơn cũ vào giỏ hàng (GioHang/{uid}/ALL).
 * Chỉ thêm sản phẩm còn bán (chưa xóa mềm, chưa hết hạn) và còn hàng; số lượng được giới hạn theo tồn kho;
 * sản phẩm đã có trong giỏ thì cộng dồn (cũng không vượt tồn kho). Sản phẩm không thêm được được đếm
 * vào "skipped" để báo cho khách.
 */
public final class OrderReorder {

    private static final String TAG = "OrderReorder";

    private OrderReorder() { }

    public interface Callback {
        void onDone(int added, int skipped);
        void onError(String message);
    }

    public static void reorder(List<OrderMath.Line> lines, Callback cb) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            cb.onError("Vui lòng đăng nhập lại");
            return;
        }
        if (lines == null || lines.isEmpty()) {
            cb.onError("Đơn này không có sản phẩm để mua lại");
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Gộp các dòng trùng sản phẩm
        Map<String, Long> wanted = new LinkedHashMap<>();
        for (OrderMath.Line l : lines) {
            if (l == null || l.productId == null || l.productId.isEmpty() || l.quantity <= 0) continue;
            Long cur = wanted.get(l.productId);
            wanted.put(l.productId, (cur == null ? 0 : cur) + l.quantity);
        }
        if (wanted.isEmpty()) {
            cb.onError("Đơn này không có sản phẩm để mua lại");
            return;
        }

        List<Task<DocumentSnapshot>> productTasks = new ArrayList<>();
        List<String> ids = new ArrayList<>(wanted.keySet());
        for (String id : ids) productTasks.add(db.collection("SanPham").document(id).get());
        CollectionReference cart = db.collection("GioHang").document(uid).collection("ALL");

        Tasks.whenAllComplete(productTasks)
                .continueWithTask(t -> cart.get())
                .addOnSuccessListener(cartSnap -> {
                    Map<String, DocumentSnapshot> inCart = new HashMap<>();
                    Map<String, Long> cartQty = new HashMap<>();
                    for (QueryDocumentSnapshot d : cartSnap) {
                        String pid = d.getString("id_product");
                        if (pid == null) continue;
                        inCart.put(pid, d);
                        cartQty.put(pid, Math.max(0, FirestoreMapper.num(d, "soluong")));
                    }

                    int skipped = 0;
                    List<Task<?>> writes = new ArrayList<>();
                    for (int i = 0; i < ids.size(); i++) {
                        String pid = ids.get(i);
                        Task<DocumentSnapshot> pt = productTasks.get(i);
                        DocumentSnapshot p = pt.isSuccessful() ? pt.getResult() : null;
                        if (p == null || !p.exists() || ProductVisibility.isHiddenFromShop(p)) {
                            skipped++;
                            continue;
                        }
                        long stock = FirestoreMapper.num(p, "soluong");
                        if (stock <= 0) {
                            skipped++;
                            continue;
                        }
                        long existing = cartQty.containsKey(pid) ? cartQty.get(pid) : 0;
                        long qty = Math.min(stock, existing + wanted.get(pid));
                        if (qty <= existing && existing > 0) {
                            // đã đủ trong giỏ (đạt tồn kho) — coi như đã thêm
                            continue;
                        }
                        if (inCart.containsKey(pid)) {
                            writes.add(cart.document(inCart.get(pid).getId()).update("soluong", qty));
                        } else {
                            Map<String, Object> row = new HashMap<>();
                            row.put("id_product", pid);
                            row.put("soluong", qty);
                            writes.add(cart.add(row));
                        }
                    }
                    final int skippedFinal = skipped;
                    final int addedFinal = ids.size() - skipped;
                    Tasks.whenAllComplete(writes).addOnSuccessListener(done -> {
                        for (Task<?> w : writes) {
                            if (!w.isSuccessful()) {
                                Log.e(TAG, "Ghi giỏ hàng thất bại", w.getException());
                                cb.onError("Không thêm được vào giỏ hàng, vui lòng thử lại");
                                return;
                            }
                        }
                        cb.onDone(addedFinal, skippedFinal);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Mua lại thất bại", e);
                    cb.onError("Không thể mua lại lúc này, vui lòng kiểm tra mạng");
                });
    }
}
