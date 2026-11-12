package com.UngDung.ThucPhamXanh.Helper;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Sản phẩm bị admin "xóa" thực chất chỉ được ẩn (field daxoa = true) để giữ nguyên
 * hóa đơn, thống kê và lịch sử mua hàng. Mọi màn hình phía khách phải bỏ qua sản phẩm này.
 */
public final class ProductVisibility {
    public static final String FIELD_DELETED = "daxoa";

    private ProductVisibility() { }

    public static boolean isHidden(DocumentSnapshot doc) {
        return doc == null || !doc.exists() || Boolean.TRUE.equals(doc.getBoolean(FIELD_DELETED));
    }
}
