package com.UngDung.ThucPhamXanh.View;

import android.content.Context;
import android.view.View;

import com.UngDung.ThucPhamXanh.Helper.ThemePrefs;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.button.MaterialButtonToggleGroup;

/** Gắn logic cho khung chọn giao diện (layout view_theme_picker.xml). */
public final class ThemePicker {

    private ThemePicker() { }

    /** root = view chứa khung chọn (hoặc chính khung đó). Chọn xong là áp dụng ngay, activity tự tạo lại. */
    public static void bind(View root) {
        MaterialButtonToggleGroup group = root.findViewById(R.id.toggle_theme);
        if (group == null) return;
        Context context = root.getContext().getApplicationContext();

        int current = ThemePrefs.get(context);
        group.check(buttonFor(current));

        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) return;
            int mode = modeFor(checkedId);
            if (mode != ThemePrefs.get(context)) ThemePrefs.set(context, mode);
        });
    }

    private static int buttonFor(int mode) {
        switch (mode) {
            case ThemePrefs.MODE_DARK: return R.id.btn_theme_dark;
            case ThemePrefs.MODE_SYSTEM: return R.id.btn_theme_system;
            default: return R.id.btn_theme_light;
        }
    }

    private static int modeFor(int buttonId) {
        if (buttonId == R.id.btn_theme_dark) return ThemePrefs.MODE_DARK;
        if (buttonId == R.id.btn_theme_system) return ThemePrefs.MODE_SYSTEM;
        return ThemePrefs.MODE_LIGHT;
    }
}
