package com.UngDung.ThucPhamXanh.Helper;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Lựa chọn giao diện Sáng / Tối / Theo hệ thống của người dùng (dùng chung cho khách, admin, shipper).
 * Lưu trong SharedPreferences "app_theme"; {@link #apply(Context)} được gọi khi app khởi động
 * (BotAI/MyApplication) và {@link #set(Context, int)} áp dụng ngay (activity đang mở tự tạo lại).
 *
 * Mặc định là {@link #MODE_LIGHT} (giữ nguyên diện mạo cũ) cho đến khi người dùng tự chọn.
 */
public final class ThemePrefs {

    public static final int MODE_LIGHT = 0;
    public static final int MODE_DARK = 1;
    public static final int MODE_SYSTEM = 2;

    public static final int DEFAULT_MODE = MODE_LIGHT;

    private static final String PREFS = "app_theme";
    private static final String KEY_MODE = "mode";

    private ThemePrefs() { }

    /** Mã của mình → hằng của AppCompatDelegate; giá trị lạ → mặc định. */
    public static int toDelegateMode(int mode) {
        switch (mode) {
            case MODE_DARK: return AppCompatDelegate.MODE_NIGHT_YES;
            case MODE_SYSTEM: return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            case MODE_LIGHT: return AppCompatDelegate.MODE_NIGHT_NO;
            default: return toDelegateMode(DEFAULT_MODE);
        }
    }

    /** Giá trị đọc từ bộ nhớ có thể hỏng / từ phiên bản khác → ép về mã hợp lệ. */
    public static int sanitize(int mode) {
        return (mode == MODE_LIGHT || mode == MODE_DARK || mode == MODE_SYSTEM) ? mode : DEFAULT_MODE;
    }

    public static String label(int mode) {
        switch (sanitize(mode)) {
            case MODE_DARK: return "Tối";
            case MODE_SYSTEM: return "Theo hệ thống";
            default: return "Sáng";
        }
    }

    public static int get(Context context) {
        try {
            SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return sanitize(sp.getInt(KEY_MODE, DEFAULT_MODE));
        } catch (Exception e) {
            return DEFAULT_MODE;
        }
    }

    /** Lưu lựa chọn và áp dụng ngay. */
    public static void set(Context context, int mode) {
        int m = sanitize(mode);
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_MODE, m).apply();
        } catch (Exception ignored) {
            // không lưu được thì vẫn áp dụng cho phiên hiện tại
        }
        AppCompatDelegate.setDefaultNightMode(toDelegateMode(m));
    }

    /** Gọi một lần khi khởi động app, trước khi activity nào được tạo. */
    public static void apply(Context context) {
        AppCompatDelegate.setDefaultNightMode(toDelegateMode(get(context)));
    }
}
