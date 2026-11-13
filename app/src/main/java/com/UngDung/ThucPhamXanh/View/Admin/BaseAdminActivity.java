package com.UngDung.ThucPhamXanh.View.Admin;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp cha cho các màn admin (dùng với theme Theme.ThucPhamXanh.Admin).
 *
 *  - {@link #setupAdminToolbar}: toolbar xanh chung (layout/view_admin_toolbar.xml) có nút quay lại.
 *  - {@link #applySystemBarInsets}: targetSdk 35 bắt buộc vẽ tràn viền trên Android 15+,
 *    nên phần đầu/cuối màn hình phải tự chừa chỗ cho status bar và thanh điều hướng.
 *  - {@link #listen}: giữ các listener Firestore và tự gỡ ở onStop (không nghe khi màn hình bị che,
 *    tránh tốn lượt đọc). Màn con gắn listener trong {@link #onStartListening()}.
 *  - {@link #confirm}: hộp xác nhận trước thao tác không hoàn tác được.
 *  - {@link #showEmpty}: hiện/ẩn layout/view_admin_empty.xml.
 */
public abstract class BaseAdminActivity extends AppCompatActivity {

    private final List<ListenerRegistration> listeners = new ArrayList<>();

    @Override
    protected void onStart() {
        super.onStart();
        onStartListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        for (ListenerRegistration l : listeners) l.remove();
        listeners.clear();
    }

    /** Gắn các listener Firestore ở đây và bọc mỗi cái bằng {@link #listen}. Mặc định không làm gì. */
    protected void onStartListening() { }

    /** Ghi nhận listener để tự gỡ ở onStop. */
    protected void listen(ListenerRegistration registration) {
        if (registration != null) listeners.add(registration);
    }

    /** Dùng toolbar chung (include layout/view_admin_toolbar.xml). subtitle có thể null. */
    protected MaterialToolbar setupAdminToolbar(CharSequence title, @Nullable CharSequence subtitle) {
        MaterialToolbar toolbar = findViewById(R.id.admin_toolbar);
        toolbar.setTitle(title);
        toolbar.setSubtitle(subtitle);
        toolbar.setNavigationOnClickListener(v -> finish());
        applySystemBarInsets(findViewById(R.id.admin_toolbar_container), null);
        return toolbar;
    }

    /**
     * Cộng chiều cao status bar vào paddingTop của {@code top} và chiều cao thanh điều hướng
     * vào paddingBottom của {@code bottom} (mỗi view có thể null). Giữ nguyên padding gốc trong XML.
     */
    protected void applySystemBarInsets(@Nullable View top, @Nullable View bottom) {
        if (top != null) {
            final int base = top.getPaddingTop();
            ViewCompat.setOnApplyWindowInsetsListener(top, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), base + bars.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
        if (bottom != null) {
            final int base = bottom.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(bottom, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), base + bars.bottom);
                return insets;
            });
        }
    }

    /** Hộp xác nhận; onYes chạy khi bấm nút đồng ý. */
    protected void confirm(CharSequence title, CharSequence message, CharSequence positive, Runnable onYes) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Hủy", null)
                .setPositiveButton(positive, (d, w) -> onYes.run())
                .show();
    }

    /** Hiện view trống (include layout/view_admin_empty.xml) với nội dung message khi empty = true. */
    protected void showEmpty(View emptyView, boolean empty, @Nullable CharSequence message) {
        if (emptyView == null) return;
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty && message != null) {
            TextView tv = emptyView.findViewById(R.id.admin_empty_text);
            if (tv != null) tv.setText(message);
        }
    }
}
