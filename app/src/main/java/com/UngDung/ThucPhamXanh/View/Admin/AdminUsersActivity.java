package com.UngDung.ThucPhamXanh.View.Admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Models.User;
import com.UngDung.ThucPhamXanh.Presenter.UserPresenter;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.UngDung.ThucPhamXanh.my_interface.UserView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AdminUsersActivity extends AppCompatActivity implements UserView {

    private ImageView imgBackUsers;
    private RecyclerView rcvUsers;
    private AppCompatImageView imageAddUsers;
    private AdminUsersAdapter adapter;
    private ArrayList<User> mlistUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration usersListener;

    private UserPresenter userPresenter;

    DatabaseReference reference1, reference2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        Init();
        InitWidget();
        Event();
        setupRealtimeListener();
    }

    private void Event() {
        imgBackUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        imageAddUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dialog = new Dialog(AdminUsersActivity.this);
                dialog.setContentView(R.layout.dialog_add_users);
                dialog.show();
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                EditText edtAddEmailUsers = dialog.findViewById(R.id.edt_add_email_users);
                EditText edtAddPassUsers = dialog.findViewById(R.id.edt_add_pass_users);
                EditText edtAddConfirmUsers = dialog.findViewById(R.id.edt_add_confirm_users);
                Button btnAddUsers = dialog.findViewById(R.id.btn_add_users);
                Button btnHuyUsers = dialog.findViewById(R.id.btn_huy_users);
                dialog.setCanceledOnTouchOutside(true);

                btnAddUsers.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String strEmail = edtAddEmailUsers.getText().toString().trim();
                        String strPass = edtAddPassUsers.getText().toString().trim();
                        String strConfirm = edtAddConfirmUsers.getText().toString().trim();
                        if (strEmail.length() > 0){
                            if (strPass.length() > 0) {
                                if (strPass.equals(strConfirm)){
                                    createUserAccount(strEmail, strPass, dialog);
                                } else {
                                    Toast.makeText(AdminUsersActivity.this, "Mật khẩu xác nhận không khớp.\nVui lòng nhập lại!", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(AdminUsersActivity.this, "Bạn chưa nhập mật khẩu", Toast.LENGTH_SHORT).show();
                            }

                        } else{
                            Toast.makeText(AdminUsersActivity.this, "Bạn chưa nhập Email", Toast.LENGTH_SHORT).show();
                        }


                    }
                });
                btnHuyUsers.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                    }
                });
            }
        });
    }

    private void Init() {
        mlistUser = new ArrayList<>();
    }

    /**
     * Tạo tài khoản khách hàng mới.
     * Dùng một FirebaseApp PHỤ để tạo tài khoản -> phiên đăng nhập chính trên máy không bị
     * chuyển sang tài khoản vừa tạo (createUserWithEmailAndPassword tự đăng nhập tài khoản mới).
     */
    private void createUserAccount(String email, String password, Dialog dialog) {
        if (!isEmailValid(email)) {
            Toast.makeText(this, "Email định dạng không đúng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.firebase.FirebaseApp secondaryApp;
        try {
            secondaryApp = com.google.firebase.FirebaseApp.getInstance("AdminCreateUser");
        } catch (IllegalStateException notCreatedYet) {
            secondaryApp = com.google.firebase.FirebaseApp.initializeApp(this,
                    com.google.firebase.FirebaseApp.getInstance().getOptions(), "AdminCreateUser");
        }
        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);

        secondaryAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null || task.getResult().getUser() == null) {
                Exception e = task.getException();
                String msg;
                if (e instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    msg = "Email này đã được đăng ký";
                } else if (e instanceof com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
                    msg = "Mật khẩu quá yếu (tối thiểu 6 ký tự)";
                } else {
                    msg = "Tạo tài khoản thất bại" + (e != null ? ": " + e.getMessage() : "");
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                return;
            }

            String newUid = task.getResult().getUser().getUid();
            secondaryAuth.signOut(); // không giữ phiên của tài khoản mới

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            HashMap<String, String> idUser = new HashMap<>();
            idUser.put("iduser", newUid);
            idUser.put("email", email);
            db.collection("IDUser").add(idUser);

            Map<String, Object> profile = new HashMap<>();
            profile.put("iduser", newUid);
            profile.put("hoten", "");
            profile.put("diachi", "");
            profile.put("sdt", "");
            profile.put("ngaysinh", "");
            profile.put("gioitinh", "");
            profile.put("avatar", "");
            db.collection("User").document(newUid).collection("Profile").add(profile);

            String username = "any name";
            HashMap<String, String> realtimeUser = new HashMap<>();
            realtimeUser.put("iduser", newUid);
            realtimeUser.put("name", username);
            realtimeUser.put("avatar", "default");
            realtimeUser.put("status", "offline");
            realtimeUser.put("search", username.toLowerCase());
            FirebaseDatabase.getInstance().getReference("Users").child(newUid).setValue(realtimeUser);

            HashMap<String, String> chatList = new HashMap<>();
            chatList.put("id", newUid);
            FirebaseDatabase.getInstance().getReference("Chatlist")
                    .child(com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID).child(newUid).setValue(chatList);

            Toast.makeText(this, "Đã tạo tài khoản " + email, Toast.LENGTH_SHORT).show();
            dialog.cancel();
        });
    }

    private void InitWidget() {
        imgBackUsers = findViewById(R.id.img_back_users);
        rcvUsers = findViewById(R.id.rcv_users);
        imageAddUsers = findViewById(R.id.image_add_users);

        userPresenter = new UserPresenter(AdminUsersActivity.this);
        adapter = new AdminUsersAdapter(AdminUsersActivity.this, mlistUser, new IClickCTHD() {
            @Override
            public void onClickCTHD(int pos) {
                User user = mlistUser.get(pos);
                showUserInfoDialog(user);
            }
        });
        rcvUsers.setLayoutManager(new GridLayoutManager(AdminUsersActivity.this, 2));
        rcvUsers.setAdapter(adapter);
    }
    
    private void setupRealtimeListener() {
        if (usersListener != null) {
            usersListener.remove();
        }
        
        usersListener = db.collection("IDUser")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }
                        
                        if (snapshots != null) {
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                QueryDocumentSnapshot doc = dc.getDocument();
                                String iduser = doc.getString("iduser");
                                String email = doc.getString("email");
                                
                                int index = findUserIndex(iduser);
                                
                                switch (dc.getType()) {
                                    case ADDED:
                                        if (index == -1) {
                                            userPresenter.HandleGetUsers(iduser, email);
                                        }
                                        break;
                                    case MODIFIED:
                                        if (index != -1) {
                                            mlistUser.remove(index);
                                            adapter.notifyItemRemoved(index);
                                            userPresenter.HandleGetUsers(iduser, email);
                                        }
                                        break;
                                    case REMOVED:
                                        if (index != -1) {
                                            mlistUser.remove(index);
                                            adapter.notifyItemRemoved(index);
                                        }
                                        break;
                                }
                            }
                        }
                    }
                });
    }
    
    private int findUserIndex(String iduser) {
        for (int i = 0; i < mlistUser.size(); i++) {
            if (mlistUser.get(i).getIduser().equals(iduser)) {
                return i;
            }
        }
        return -1;
    }
    
    private void showUserInfoDialog(User user) {
        Dialog dialog = new Dialog(AdminUsersActivity.this);
        dialog.setContentView(R.layout.dialog_info_users);
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        TextView tvIdusers = dialog.findViewById(R.id.tv_id_users);
        TextView tvEmailUsers = dialog.findViewById(R.id.tv_email_users);
        TextView tvNameUsers = dialog.findViewById(R.id.tv_name_users);
        TextView tvAddressUsers = dialog.findViewById(R.id.tv_address_users);
        TextView tvSDTUsers = dialog.findViewById(R.id.tv_sdt_users);
        TextView tvDateUsers = dialog.findViewById(R.id.tv_date_users);
        TextView tvGenderUsers = dialog.findViewById(R.id.tv_gender_users);
        ImageView btnCancelUsers = dialog.findViewById(R.id.btn_cancel_users);
        dialog.setCanceledOnTouchOutside(true);

        tvIdusers.setText(user.getIduser());
        tvEmailUsers.setText(user.getEmail());
        tvNameUsers.setText(user.getName());
        tvAddressUsers.setText(user.getAddress());
        tvSDTUsers.setText(user.getPhone());
        tvDateUsers.setText(user.getDate());
        tvGenderUsers.setText(user.getSex());

        btnCancelUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersListener != null) {
            usersListener.remove();
        }
    }

    private boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Override
    public void OnSucess() {

    }

    @Override
    public void OnFail() {

    }

    @Override
    public void getDataUser(String id, String email, String name, String address, String phone, String date, String gender, String avatar) {
        // Check if user already exists
        int index = findUserIndex(id);
        User newUser = new User(id, email, name, address, phone, date, gender, avatar);
        
        if (index == -1) {
            mlistUser.add(newUser);
            adapter.notifyItemInserted(mlistUser.size() - 1);
        } else {
            mlistUser.set(index, newUser);
            adapter.notifyItemChanged(index);
        }
    }

}