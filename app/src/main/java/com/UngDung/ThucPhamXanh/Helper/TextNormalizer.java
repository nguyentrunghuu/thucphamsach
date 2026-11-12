package com.UngDung.ThucPhamXanh.Helper;

import java.text.Normalizer;
import java.util.Locale;

/** Chuẩn hóa chữ tiếng Việt để tìm kiếm: chữ thường, bỏ dấu, gộp khoảng trắng. "Rau Củ" -> "rau cu". */
public final class TextNormalizer {

    private TextNormalizer() { }

    public static String normalize(String s) {
        if (s == null) return "";
        String r = s.toLowerCase(Locale.ROOT).replace('đ', 'd');
        r = Normalizer.normalize(r, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return r.replaceAll("[^a-z0-9]+", " ").trim();
    }
}
