package com.UngDung.ThucPhamXanh;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.fragment.BillFragment;
import com.UngDung.ThucPhamXanh.fragment.ChatFragment;
import com.UngDung.ThucPhamXanh.fragment.HomeFragment;
import com.UngDung.ThucPhamXanh.fragment.ProfileFragment;
import com.UngDung.ThucPhamXanh.ultil.MyReceiver;
import com.UngDung.ThucPhamXanh.View.MapTestActivity;
import com.UngDung.ThucPhamXanh.View.MapLocationActivity;
import com.UngDung.ThucPhamXanh.View.MapTestOSMActivity;
import com.UngDung.ThucPhamXanh.View.MapLocationOSMActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity {

    public static CountDownTimer countDownTimer;
    private static final int TIME_DELAY = 2000;
    private static long back_pressed;
    private BottomNavigationView bottomNavigation;
    private HomeFragment homeFragment = new HomeFragment();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    // Check Internet
    private BroadcastReceiver MyReceiver = null;

    private int mCountProduct;

    private static final String STATE_NAV_ID = "nav_selected_id";

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomNavigation != null) outState.putInt(STATE_NAV_ID, bottomNavigation.getSelectedItemId());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initWidget();
        init();
        MyReceiver = new MyReceiver(); // Check Internet
        broadcastIntent(); // Check Internet

        setupBottomNavigation();
        // Thông báo tin nhắn mới từ cửa hàng + xin quyền hiện thông báo (Android 13+)
        com.UngDung.ThucPhamXanh.Helper.NotificationPermission.request(this);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            com.UngDung.ThucPhamXanh.Helper.ChatNotifier.get(this)
                    .start(FirebaseAuth.getInstance().getCurrentUser().getUid(), false);
        }
        // Activity tạo lại (vd. vừa đổi Sáng/Tối ở tab Tài khoản) → quay lại đúng tab đang mở thay vì về Trang chủ
        int restoredNav = savedInstanceState != null ? savedInstanceState.getInt(STATE_NAV_ID, 0) : 0;
        if (restoredNav != 0 && restoredNav != R.id.menu_home) {
            bottomNavigation.setSelectedItemId(restoredNav);
        }
        setProFile();
    }

    public void setProFile() {
        db = FirebaseFirestore.getInstance();
        if (homeFragment.getTvNameHome() == null) {
            return;
        }
        if (homeFragment.getTvEmailHome() == null) {
            return;
        }
        if (homeFragment.getCirAvatarHome() == null) {
            return;
        }
        homeFragment.getTvEmailHome().setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        db.collection("User").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("Profile")
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                            if (documentSnapshot != null) {
                                try {
                                    homeFragment.getTvNameHome()
                                            .setText(documentSnapshot.getString("hoten").length() > 0
                                                    ? documentSnapshot.getString("hoten")
                                                    : "");

                                    if (documentSnapshot.getString("avatar").length() > 0) {
                                        Picasso.get().load(documentSnapshot.getString("avatar").trim())
                                                .into(homeFragment.getCirAvatarHome());
                                    }
                                } catch (Exception e) {
                                    Log.d("ERROR", e.getMessage());
                                }
                            }
                        }
                    }
                });
    }

    @SuppressLint("WrongViewCast")
    private void initWidget() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void init() {
        // Set the home fragment as default
        replace(new HomeFragment());
    }

    public void setCountProductInCart(int count) {
        mCountProduct = count;
        // Badge có thể được thêm vào menu item khác nếu cần
    }

    public int getmCountProduct() {
        return mCountProduct;
    }

    public void replace(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame, fragment);
        transaction.commit();
    }
    
    public BottomNavigationView getBottomNavigation() {
        return bottomNavigation;
    }

    private void setupBottomNavigation() {
        // Set the Home tab as selected by default
        bottomNavigation.setSelectedItemId(R.id.menu_home);

        // Set up the listener for item selection
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                // Add bounce effect when item is selected
                if (bottomNavigation instanceof com.UngDung.ThucPhamXanh.View.FloatingBottomNavigationView) {
                    ((com.UngDung.ThucPhamXanh.View.FloatingBottomNavigationView) bottomNavigation).bounceSelectedItem();
                }

                if (id == R.id.menu_home) {
                    replace(new HomeFragment());
                    return true;
                } else if (id == R.id.menu_chat) {
                    replace(new ChatFragment());
                    return true;
                } else if (id == R.id.menu_bill) {
                    replace(new BillFragment());
                    return true;
                } else if (id == R.id.menu_profile) {
                    replace(new ProfileFragment());
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Sự kiện nhấn back 2 lần để out app
        if (back_pressed + TIME_DELAY > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            Toast.makeText(getBaseContext(), "Nhấn lần nữa để thoát!",
                    Toast.LENGTH_SHORT).show();
        }
        back_pressed = System.currentTimeMillis();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (countDownTimer != null) {
            countDownTimer.start();
        }
    }

    // Check Internet
    public void broadcastIntent() {
        registerReceiver(MyReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(MyReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(MyReceiver);
    }

    // Method để test map - có thể gọi từ bất kỳ đâu
    public void testMap() {
        Intent intent = new Intent(this, MapTestActivity.class);
        startActivity(intent);
    }

    // Method để test MapLocationActivity
    public void testMapLocation() {
        Intent intent = new Intent(this, MapLocationActivity.class);
        startActivity(intent);
    }

    // Method để test OpenStreetMap
    public void testOSMMap() {
        Intent intent = new Intent(this, MapTestOSMActivity.class);
        startActivity(intent);
    }

    // Method để test MapLocationOSMActivity
    public void testOSMMapLocation() {
        Intent intent = new Intent(this, MapLocationOSMActivity.class);
        startActivity(intent);
    }
}