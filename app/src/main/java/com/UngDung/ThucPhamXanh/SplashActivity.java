package com.UngDung.ThucPhamXanh;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.UngDung.ThucPhamXanh.View.SignInActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private ImageView logoImageView;
    private TextView appNameTextView;
    private TextView taglineTextView;
    private ProgressBar loadingProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // Initialize views
        logoImageView = findViewById(R.id.logoImageView);
        appNameTextView = findViewById(R.id.appNameTextView);
        taglineTextView = findViewById(R.id.taglineTextView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        
        // Start animations
        startAnimations();
        
        new Handler().postDelayed(new Runnable() {
            @Override
            // check ktra đã đăng nhập vào thẳng vào Home
            public void run() {
                nextActivity();
            }

        }, 2500);
    }
    
    private void startAnimations() {
        // Fade in and scale animation for logo
        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale);
        logoImageView.startAnimation(logoAnimation);
        
        // Fade in animation for app name (delayed)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation nameAnimation = AnimationUtils.loadAnimation(SplashActivity.this, R.anim.fade_in);
                appNameTextView.setVisibility(View.VISIBLE);
                appNameTextView.startAnimation(nameAnimation);
            }
        }, 300);
        
        // Fade in animation for tagline (delayed)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation taglineAnimation = AnimationUtils.loadAnimation(SplashActivity.this, R.anim.fade_in);
                taglineTextView.setVisibility(View.VISIBLE);
                taglineTextView.startAnimation(taglineAnimation);
            }
        }, 600);
        
        // Fade in animation for progress bar (delayed)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation progressAnimation = AnimationUtils.loadAnimation(SplashActivity.this, R.anim.fade_in);
                loadingProgressBar.setVisibility(View.VISIBLE);
                loadingProgressBar.startAnimation(progressAnimation);
            }
        }, 900);
    }

    private void nextActivity() {
        // Check user đã login hay chưa, Firebase sẽ cung cấp 1 hàm lấy dc thông tin của user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Thực hiện check đối tượng user để biết người dùng đã login hay chưa
        if (user == null) {
            // Chưa login
            Intent intent = new Intent(this, SignInActivity.class);
            startActivity(intent);
        } else {
            // Đã login
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        finish();
    }
}