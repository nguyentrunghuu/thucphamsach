package com.UngDung.ThucPhamXanh.Helper;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageUploadHelper {
    private static final String TAG = "ImageUploadHelper";
    private static FirebaseStorage storage = FirebaseStorage.getInstance();
    
    /**
     * Upload ảnh lên Firebase Storage
     * @param context Context của activity/fragment
     * @param imageUri URI của ảnh cần upload
     * @param folderPath Đường dẫn folder trong Storage (ví dụ: "products", "categories", "avatars")
     * @param progressDialog ProgressDialog để hiển thị tiến trình (có thể null)
     * @param callback Callback để nhận kết quả
     */
    public static void uploadImage(Context context, Uri imageUri, String folderPath, 
                                   ProgressDialog progressDialog, 
                                   UploadCallback callback) {
        if (imageUri == null) {
            if (callback != null) {
                callback.onFailure(new Exception("Image URI is null"));
            }
            return;
        }
        
        try {
            // Tạo tên file unique
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = folderPath + "/" + timestamp + "_" + System.currentTimeMillis() + ".jpg";
            
            // Reference đến Storage
            StorageReference storageRef = storage.getReference();
            StorageReference imageRef = storageRef.child(fileName);
            
            // Hiển thị progress dialog nếu có
            if (progressDialog != null) {
                progressDialog.setMessage("Đang tải ảnh lên...");
                progressDialog.show();
            }
            
            // Upload ảnh
            UploadTask uploadTask = imageRef.putFile(imageUri);
            
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Lấy download URL
                    imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri downloadUri) {
                            String imageUrl = downloadUri.toString();
                            Log.d(TAG, "Upload successful: " + imageUrl);
                            
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            
                            if (callback != null) {
                                callback.onSuccess(imageUrl);
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Error getting download URL: " + e.getMessage());
                            
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            
                            Toast.makeText(context, "Lỗi lấy URL ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            
                            if (callback != null) {
                                callback.onFailure(e);
                            }
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Upload failed: " + e.getMessage());
                    
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    
                    Toast.makeText(context, "Lỗi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                }
            }).addOnProgressListener(new com.google.firebase.storage.OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    // Cập nhật progress nếu cần
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.setMessage("Đang tải ảnh lên... " + (int) progress + "%");
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading image: " + e.getMessage());
            
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            
            Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }
    
    /**
     * Xóa ảnh từ Firebase Storage
     * @param imageUrl URL của ảnh cần xóa
     * @param callback Callback để nhận kết quả
     */
    public static void deleteImage(String imageUrl, DeleteCallback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            if (callback != null) {
                callback.onComplete(true);
            }
            return;
        }
        
        try {
            // Lấy reference từ URL
            StorageReference storageRef = storage.getReferenceFromUrl(imageUrl);
            storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Image deleted successfully");
                    if (callback != null) {
                        callback.onComplete(true);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error deleting image: " + e.getMessage());
                    if (callback != null) {
                        callback.onComplete(false);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error deleting image: " + e.getMessage());
            if (callback != null) {
                callback.onComplete(false);
            }
        }
    }
    
    /**
     * Interface cho callback upload
     */
    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(Exception e);
    }
    
    /**
     * Interface cho callback delete
     */
    public interface DeleteCallback {
        void onComplete(boolean success);
    }
}

