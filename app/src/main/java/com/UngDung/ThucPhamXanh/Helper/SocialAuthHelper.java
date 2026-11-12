package com.UngDung.ThucPhamXanh.Helper;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.UngDung.ThucPhamXanh.R;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Đăng nhập / đăng ký bằng Google và Facebook qua Firebase Authentication.
 * Lần đầu đăng nhập, tài khoản sẽ được tạo dữ liệu giống như khi đăng ký bằng email
 * (Firestore "IDUser", Realtime "Users" và "Chatlist").
 *
 * Phải khởi tạo trong onCreate() của Activity (trước khi Activity ở trạng thái STARTED).
 */
public class SocialAuthHelper {

    public interface Callback {
        void onStart();

        void onSuccess(FirebaseUser user);

        void onError(String message);

        void onCancel();
    }

    private static final String TAG = "SocialAuthHelper";
    // ID cuộc hội thoại với Admin (giống SignUpActivity)
    private static final String ADMIN_CHAT_ID = com.UngDung.ThucPhamXanh.Helper.AppConstants.ADMIN_ID;

    private final AppCompatActivity activity;
    private final Callback callback;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final GoogleSignInClient googleSignInClient;
    private final ActivityResultLauncher<Intent> googleLauncher;
    private final CallbackManager facebookCallbackManager = CallbackManager.Factory.create();

    public SocialAuthHelper(AppCompatActivity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getGoogleWebClientId())
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(activity, gso);

        googleLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK && result.getData() == null) {
                        callback.onCancel();
                        return;
                    }
                    try {
                        GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                                .getResult(ApiException.class);
                        firebaseSignIn(GoogleAuthProvider.getCredential(account.getIdToken(), null));
                    } catch (ApiException e) {
                        Log.w(TAG, "Google sign in failed, code=" + e.getStatusCode(), e);
                        if (e.getStatusCode() == 12501) { // SIGN_IN_CANCELLED
                            callback.onCancel();
                        } else {
                            callback.onError("Đăng nhập Google thất bại (mã " + e.getStatusCode() + ")");
                        }
                    }
                });

        LoginManager.getInstance().registerCallback(facebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AccessToken token = loginResult.getAccessToken();
                firebaseSignIn(FacebookAuthProvider.getCredential(token.getToken()));
            }

            @Override
            public void onCancel() {
                callback.onCancel();
            }

            @Override
            public void onError(FacebookException error) {
                Log.w(TAG, "Facebook sign in failed", error);
                callback.onError("Đăng nhập Facebook thất bại: " + error.getMessage());
            }
        });
    }

    public void signInWithGoogle() {
        callback.onStart();
        // Đăng xuất phiên Google cũ để luôn hiện hộp chọn tài khoản
        googleSignInClient.signOut().addOnCompleteListener(t ->
                googleLauncher.launch(googleSignInClient.getSignInIntent()));
    }

    public void signInWithFacebook() {
        callback.onStart();
        LoginManager.getInstance().logOut();
        LoginManager.getInstance().logInWithReadPermissions(
                activity, facebookCallbackManager, Arrays.asList("email", "public_profile"));
    }

    /** Gọi trong onDestroy() của Activity. */
    public void release() {
        LoginManager.getInstance().unregisterCallback(facebookCallbackManager);
    }

    private void firebaseSignIn(AuthCredential credential) {
        auth.signInWithCredential(credential).addOnCompleteListener(activity, task -> {
            if (task.isSuccessful() && auth.getCurrentUser() != null) {
                ensureUserData(auth.getCurrentUser());
            } else {
                Exception e = task.getException();
                Log.w(TAG, "Firebase signInWithCredential failed", e);
                if (e instanceof FirebaseAuthUserCollisionException) {
                    callback.onError("Email này đã được đăng ký bằng phương thức khác.\nVui lòng đăng nhập bằng phương thức đó.");
                } else {
                    callback.onError("Xác thực thất bại. Vui lòng thử lại!");
                }
            }
        });
    }

    /** Tạo dữ liệu người dùng nếu đây là lần đầu tài khoản đăng nhập vào app. */
    private void ensureUserData(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("IDUser").whereEqualTo("iduser", user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().isEmpty()) {
                        createUserData(user);
                    }
                    callback.onSuccess(user);
                });
    }

    private void createUserData(FirebaseUser user) {
        String uid = user.getUid();
        String email = user.getEmail() != null ? user.getEmail() : "";
        String name = !TextUtils.isEmpty(user.getDisplayName()) ? user.getDisplayName() : "any name";
        String avatar = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "default";

        HashMap<String, String> idUser = new HashMap<>();
        idUser.put("iduser", uid);
        idUser.put("email", email);
        FirebaseFirestore.getInstance().collection("IDUser").add(idUser);

        HashMap<String, String> realtimeUser = new HashMap<>();
        realtimeUser.put("iduser", uid);
        realtimeUser.put("name", name);
        realtimeUser.put("avatar", avatar);
        realtimeUser.put("status", "online");
        realtimeUser.put("search", name.toLowerCase());
        FirebaseDatabase.getInstance().getReference("Users").child(uid).setValue(realtimeUser);

        HashMap<String, String> chatList = new HashMap<>();
        chatList.put("id", uid);
        FirebaseDatabase.getInstance().getReference("Chatlist").child(ADMIN_CHAT_ID).child(uid).setValue(chatList);
    }

    /**
     * Ưu tiên "default_web_client_id" do plugin google-services tự sinh
     * (khi google-services.json đã bật Google Sign-In), nếu không có thì dùng giá trị trong strings.xml.
     */
    private String getGoogleWebClientId() {
        int resId = activity.getResources().getIdentifier("default_web_client_id", "string", activity.getPackageName());
        return activity.getString(resId != 0 ? resId : R.string.google_web_client_id);
    }
}
