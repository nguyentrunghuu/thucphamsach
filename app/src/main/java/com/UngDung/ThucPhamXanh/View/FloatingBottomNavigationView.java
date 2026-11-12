package com.UngDung.ThucPhamXanh.View;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class FloatingBottomNavigationView extends BottomNavigationView {

    public FloatingBottomNavigationView(@NonNull Context context) {
        super(context);
        init();
    }

    public FloatingBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FloatingBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Set initial state
        setTranslationY(100);
        setAlpha(0f);
        
        // Animate in
        animateIn();
    }

    private void animateIn() {
        post(new Runnable() {
            @Override
            public void run() {
                animate()
                        .translationY(0)
                        .alpha(1f)
                        .setDuration(400)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        });
    }

    /**
     * Show bottom navigation with animation
     */
    public void show() {
        if (getVisibility() != View.VISIBLE) {
            setVisibility(View.VISIBLE);
            setTranslationY(100);
            setAlpha(0f);
            animate()
                    .translationY(0)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    /**
     * Hide bottom navigation with animation
     */
    public void hide() {
        if (getVisibility() == View.VISIBLE) {
            animate()
                    .translationY(100)
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }

    /**
     * Add bounce effect when item is selected
     */
    public void bounceSelectedItem() {
        animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                    }
                })
                .start();
    }
}

