package com.UngDung.ThucPhamXanh.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.UngDung.ThucPhamXanh.MainActivity;
import com.UngDung.ThucPhamXanh.Helper.SocialAuthHelper;
import com.UngDung.ThucPhamXanh.Models.Admin;
import com.UngDung.ThucPhamXanh.Models.Shipper;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.Admin.AdminHomeActivity;
import com.UngDung.ThucPhamXanh.View.Shipper.ShipperHomeActivity;
import com.UngDung.ThucPhamXanh.ultil.MyReceiver;
import com.UngDung.ThucPhamXanh.ultil.NetworkUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class SignInActivity extends AppCompatActivity {

    private Button btnDangNhap, btnDangKy;
    private EditText edtEmailUser, edtMatKhauUser;
    private TextView tvForgotPassword;
    private CircleImageView cirDangNhapFacebook, cirDangNhapGoogle;
    private static final int TIME_DELAY = 2000;
    private static long back_pressed;
    private String sosanh;
    private ProgressDialog progressDialog;
    private SocialAuthHelper socialAuthHelper;

    // Check Internet
    private BroadcastReceiver MyReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        InitWidget();
        initSocialAuth();
        MyReceiver = new MyReceiver();      // Check Internet
        broadcastIntent();                  // Check Internet
        if (NetworkUtil.isNetworkConnected(this)){
            Event();

        }

    }

    private void Event() {
        btnDangNhap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.show();
                progressDialog.setContentView(R.layout.layout_loading);
                progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                String strEmail = edtEmailUser.getText().toString().trim();
                String strMatKhau = edtMatKhauUser.getText().toString().trim();
                ArrayList<Admin> arrayListAdmin = new ArrayList<>();
                ArrayList<Shipper> arrayListShipper = new ArrayList<>();
                if(strEmail.length()>0 ){
                    if(strMatKhau.length()>0){
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            
                            // Check Admin
                            db.collection("Admin").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    for (QueryDocumentSnapshot q : queryDocumentSnapshots) {
                                        arrayListAdmin.add(new Admin(q.getString("username"), q.getString("pass")));
                                    }
                                    
                                    // Check Shipper
                                    db.collection("Shipper").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                            for (QueryDocumentSnapshot q : queryDocumentSnapshots) {
                                                arrayListShipper.add(new Shipper(q.getString("username"), q.getString("password")));
                                            }
                                            
                                            String tkadmin, mkadmin;
                                            String tkshipper, mkshipper;
                                            int tk = 0;
                                            
                                            // Check Admin first
                                            for (int i = 0; i < arrayListAdmin.size(); i++) {
                                                Admin admin = arrayListAdmin.get(i);
                                                tkadmin = admin.getUsername();
                                                mkadmin = admin.getPassword();
                                                sosanh = tkadmin;
                                                if (strEmail.equals(tkadmin) && strMatKhau.equals(mkadmin)) {
                                                    tk = 1; // Admin
                                                    break;
                                                }
                                            }
                                            
                                            // Check Shipper if not admin
                                            if (tk != 1) {
                                                for (int i = 0; i < arrayListShipper.size(); i++) {
                                                    Shipper shipper = arrayListShipper.get(i);
                                                    tkshipper = shipper.getUsername();
                                                    mkshipper = shipper.getPassword();
                                                    if (strEmail.equals(tkshipper) && strMatKhau.equals(mkshipper)) {
                                                        tk = 3; // Shipper
                                                        sosanh = tkshipper;
                                                        break;
                                                    }
                                                }
                                            }
                                            
                                            if (tk == 0) {
                                                tk = 2; // User
                                            }
                                            
                                            switch (tk){
                                                case 1: // Admin
                                                    progressDialog.dismiss();
                                                    Intent intentAdmin = new Intent(SignInActivity.this, AdminHomeActivity.class);
                                                    startActivity(intentAdmin);
                                                    finish();
                                                    break;
                                                case 3: // Shipper
                                                    progressDialog.dismiss();
                                                    Intent intentShipper = new Intent(SignInActivity.this, ShipperHomeActivity.class);
                                                    intentShipper.putExtra("shipper_username", strEmail);
                                                    startActivity(intentShipper);
                                                    finish();
                                                    break;
                                                case 2: // User
                                                    FirebaseAuth auth = FirebaseAuth.getInstance();
                                                    auth.signInWithEmailAndPassword(strEmail, strMatKhau).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                                        @Override
                                                        public void onComplete(@NonNull @NotNull Task<AuthResult> task) {
                                                            if (task.isSuccessful()){
                                                                Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                                                startActivity(intent);
                                                                finishAffinity();
                                                                progressDialog.dismiss();
                                                            } else if(!isEmailValid(strEmail) && !sosanh.equals(strEmail)){
                                                                progressDialog.dismiss();
                                                                Toast.makeText(SignInActivity.this, "Email định dạng không đúng", Toast.LENGTH_SHORT).show();

                                                            } else {
                                                                progressDialog.dismiss();
                                                                Toast.makeText(SignInActivity.this, "Tài khoản hoặc mật khẩu không đúng.\nVui lòng kiểm tra lại!", Toast.LENGTH_SHORT).show();
                                                            }

                                                        }
                                                    });
                                                    break;
                                            }
                                        }
                                    });
                                }
                            });

                    }else{
                        progressDialog.dismiss();
                        Toast.makeText(SignInActivity.this, "Bạn chưa nhập mật khẩu", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    progressDialog.dismiss();
                    Toast.makeText(SignInActivity.this, "Bạn chưa nhập tài khoản", Toast.LENGTH_SHORT).show();
                }

            }
        });

        btnDangKy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentSignUp = new Intent(SignInActivity.this, SignUpActivity.class);
                startActivity(intentSignUp);
            }
        });

        cirDangNhapGoogle.setOnClickListener(v -> socialAuthHelper.signInWithGoogle());
        cirDangNhapFacebook.setOnClickListener(v -> socialAuthHelper.signInWithFacebook());

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentForgot = new Intent(SignInActivity.this, ForgotPasswordActivity.class);
                startActivity(intentForgot);
            }
        });
    }

    private void initSocialAuth() {
        socialAuthHelper = new SocialAuthHelper(this, new SocialAuthHelper.Callback() {
            @Override
            public void onStart() {
                progressDialog.show();
                progressDialog.setContentView(R.layout.layout_loading);
                progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            @Override
            public void onSuccess(FirebaseUser user) {
                progressDialog.dismiss();
                Toast.makeText(SignInActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignInActivity.this, MainActivity.class));
                finishAffinity();
            }

            @Override
            public void onError(String message) {
                progressDialog.dismiss();
                Toast.makeText(SignInActivity.this, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancel() {
                progressDialog.dismiss();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (socialAuthHelper != null) socialAuthHelper.release();
        super.onDestroy();
    }

    private boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Override
    public void onBackPressed() {
        progressDialog.dismiss();
        // Sự kiện nhấn back 2 lần để out app
        if (back_pressed + TIME_DELAY > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            Toast.makeText(getBaseContext(), "Press once again to exit!",
                    Toast.LENGTH_SHORT).show();
        }
        back_pressed = System.currentTimeMillis();

    }

    private void InitWidget() {
        btnDangNhap = findViewById(R.id.btn_dangnhap);
        btnDangKy = findViewById(R.id.btn_dangky);
        edtEmailUser = findViewById(R.id.edt_email_user);
        edtMatKhauUser = findViewById(R.id.edt_matkhau_user);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        cirDangNhapFacebook = findViewById(R.id.cir_dangnhap_facebook);
        cirDangNhapGoogle = findViewById(R.id.cir_dangnhap_google);

        progressDialog = new ProgressDialog(this);
    }

    // Check Internet
    public void broadcastIntent() {
        registerReceiver(MyReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

    }

    // Check Internet
    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(MyReceiver);
    }
}