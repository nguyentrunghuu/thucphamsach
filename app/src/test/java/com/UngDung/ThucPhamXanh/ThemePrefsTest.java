package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;

import androidx.appcompat.app.AppCompatDelegate;

import com.UngDung.ThucPhamXanh.Helper.ThemePrefs;

import org.junit.Test;

public class ThemePrefsTest {

    @Test
    public void mapsEachModeToTheMatchingNightMode() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemePrefs.toDelegateMode(ThemePrefs.MODE_LIGHT));
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemePrefs.toDelegateMode(ThemePrefs.MODE_DARK));
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemePrefs.toDelegateMode(ThemePrefs.MODE_SYSTEM));
    }

    @Test
    public void unknownValuesFallBackToTheDefault() {
        assertEquals(ThemePrefs.DEFAULT_MODE, ThemePrefs.sanitize(-5));
        assertEquals(ThemePrefs.DEFAULT_MODE, ThemePrefs.sanitize(99));
        assertEquals(ThemePrefs.toDelegateMode(ThemePrefs.DEFAULT_MODE), ThemePrefs.toDelegateMode(42));
        assertEquals(ThemePrefs.MODE_DARK, ThemePrefs.sanitize(ThemePrefs.MODE_DARK));
    }

    @Test
    public void labelsAreVietnamese() {
        assertEquals("Sáng", ThemePrefs.label(ThemePrefs.MODE_LIGHT));
        assertEquals("Tối", ThemePrefs.label(ThemePrefs.MODE_DARK));
        assertEquals("Theo hệ thống", ThemePrefs.label(ThemePrefs.MODE_SYSTEM));
        assertEquals("Sáng", ThemePrefs.label(123));
    }
}
