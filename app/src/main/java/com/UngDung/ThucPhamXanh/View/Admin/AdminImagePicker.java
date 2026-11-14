package com.UngDung.ThucPhamXanh.View.Admin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.UngDung.ThucPhamXanh.Helper.ImageUploadHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

/**
 * Chọn ảnh (chụp bằng camera hoặc lấy từ thư viện) rồi tải lên Firebase Storage — dùng chung cho các form admin.
 *
 * Phải tạo trong onCreate() của activity (đăng ký ActivityResult launcher trước khi màn hình STARTED).
 * Quyền camera / thư viện được xin ngay trong lớp này (không cần onRequestPermissionsResult).
 */
public final class AdminImagePicker {

    private static final String TAG = "AdminImagePicker";

    public interface Listener {
        /** Bắt đầu tải ảnh lên (hiện vòng xoay, khóa nút). */
        void onUploadStart();

        /** Tải xong: imageUrl là đường dẫn tải về. */
        void onUploaded(String imageUrl);

        /** Tải thất bại (ImageUploadHelper đã tự báo lỗi bằng Toast). */
        void onUploadFailed();
    }

    private final AppCompatActivity activity;
    private final String folder;
    private final String title;
    private final Listener listener;

    private final ActivityResultLauncher<Intent> cameraLauncher;
    private final ActivityResultLauncher<String> galleryLauncher;
    private final ActivityResultLauncher<String> cameraPermission;
    private final ActivityResultLauncher<String> storagePermission;

    private Uri cameraUri;
    private boolean uploading;

    /**
     * @param folder thư mục trong Storage (vd. "products", "categories")
     * @param title  tiêu đề hộp chọn nguồn ảnh
     */
    public AdminImagePicker(AppCompatActivity activity, String folder, String title, Listener listener) {
        this.activity = activity;
        this.folder = folder;
        this.title = title;
        this.listener = listener;

        cameraLauncher = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && cameraUri != null) upload(cameraUri);
                });
        galleryLauncher = activity.registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) upload(uri); });
        cameraPermission = activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) openCamera();
                    else toast("Cần quyền camera để chụp ảnh");
                });
        storagePermission = activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) galleryLauncher.launch("image/*");
                    else toast("Cần quyền truy cập thư viện để chọn ảnh");
                });
    }

    public boolean isUploading() {
        return uploading;
    }

    /** Hiện hộp chọn nguồn ảnh. */
    public void show() {
        if (uploading) return;
        new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setItems(new String[]{"Chụp ảnh", "Chọn từ thư viện"}, (dialog, which) -> {
                    if (which == 0) {
                        if (granted(Manifest.permission.CAMERA)) openCamera();
                        else cameraPermission.launch(Manifest.permission.CAMERA);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            || granted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        galleryLauncher.launch("image/*"); // Android 13+ không cần quyền với GetContent
                    } else {
                        storagePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private boolean granted(String permission) {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void openCamera() {
        try {
            File photo = new File(activity.getCacheDir(), "camera_image_" + System.currentTimeMillis() + ".jpg");
            cameraUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", photo);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            cameraLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Không mở được camera", e);
            toast("Không thể mở camera");
        }
    }

    private void upload(Uri uri) {
        uploading = true;
        listener.onUploadStart();
        ImageUploadHelper.uploadImage(activity, uri, folder, null, new ImageUploadHelper.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                if (activity.isFinishing()) return;
                uploading = false;
                listener.onUploaded(imageUrl);
            }

            @Override
            public void onFailure(Exception e) {
                if (activity.isFinishing()) return;
                uploading = false;
                listener.onUploadFailed();
            }
        });
    }

    private void toast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }
}
