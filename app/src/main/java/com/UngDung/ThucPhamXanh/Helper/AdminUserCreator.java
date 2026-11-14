package com.UngDung.ThucPhamXanh.Helper;

import android.content.Context;
import android.util.Patterns;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin tạo tài khoản khách hàng (email + mật khẩu).
 *
 * Dùng một FirebaseApp PHỤ để tạo tài khoản → phiên đăng nhập chính trên máy không bị chuyển sang
 * tài khoản vừa tạo (createUserWithEmailAndPassword tự đăng nhập tài khoản mới). Sau khi tạo, ghi các
 * bản ghi mà app khách hàng cần: IDUser, User/{uid}/Profile, Realtime Users/{uid} và Chatlist của admin.
 */
public final class AdminUserCreator {

    private static final String SECONDARY_APP = "AdminCreateUser";

    private AdminUserCreator() { }

    public interface Callback {
        void onCreated(String email);

        void onError(String message);
    }

    public static boolean isEmailValid(CharSequence email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /** Tên hiển thị mặc định từ email: "an.nguyen@gmail.com" → "an.nguyen". */
    public static String nameFromEmail(String email) {
        if (email == null) return "";
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    public static void create(Context context, String email, String password, Callback cb) {
        if (!isEmailValid(email)) {
            cb.onError("Email không đúng định dạng");
            return;
        }
        if (password == null || password.length() < 6) {
            cb.onError("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }
        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance(SECONDARY_APP);
        } catch (IllegalStateException notCreatedYet) {
            secondaryApp = FirebaseApp.initializeApp(context.getApplicationContext(),
                    FirebaseApp.getInstance().getOptions(), SECONDARY_APP);
        }
        final FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
        secondaryAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null || task.getResult().getUser() == null) {
                Exception e = task.getException();
                if (e instanceof FirebaseAuthUserCollisionException) {
                    cb.onError("Email này đã được đăng ký");
                } else if (e instanceof FirebaseAuthWeakPasswordException) {
                    cb.onError("Mật khẩu quá yếu (tối thiểu 6 ký tự)");
                } else {
                    cb.onError("Tạo tài khoản thất bại" + (e != null ? ": " + e.getMessage() : ""));
                }
                return;
            }
            String uid = task.getResult().getUser().getUid();
            secondaryAuth.signOut(); // không giữ phiên của tài khoản mới
            writeRecords(uid, email);
            cb.onCreated(email);
        });
    }

    private static void writeRecords(String uid, String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> idUser = new HashMap<>();
        idUser.put("iduser", uid);
        idUser.put("email", email);
        db.collection("IDUser").add(idUser);

        Map<String, Object> profile = new HashMap<>();
        profile.put("iduser", uid);
        profile.put("hoten", "");
        profile.put("diachi", "");
        profile.put("sdt", "");
        profile.put("ngaysinh", "");
        profile.put("gioitinh", "");
        profile.put("avatar", "");
        db.collection("User").document(uid).collection("Profile").add(profile);

        String username = nameFromEmail(email);
        Map<String, String> realtimeUser = new HashMap<>();
        realtimeUser.put("iduser", uid);
        realtimeUser.put("name", username);
        realtimeUser.put("avatar", "default");
        realtimeUser.put("status", "offline");
        realtimeUser.put("search", username.toLowerCase());
        FirebaseDatabase.getInstance().getReference("Users").child(uid).setValue(realtimeUser);

        Map<String, String> chatList = new HashMap<>();
        chatList.put("id", uid);
        FirebaseDatabase.getInstance().getReference("Chatlist").child(AppConstants.ADMIN_ID).child(uid).setValue(chatList);
    }
}
