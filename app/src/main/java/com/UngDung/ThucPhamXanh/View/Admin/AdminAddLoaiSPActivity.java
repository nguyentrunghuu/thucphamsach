package com.UngDung.ThucPhamXanh.View.Admin;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.UngDung.ThucPhamXanh.Helper.ImageUploadHelper;
import com.UngDung.ThucPhamXanh.Models.LoaiProduct;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

public class AdminAddLoaiSPActivity extends AppCompatActivity {

    private ImageView imgBackThemLoai, imgThemLoai;
    private EditText edtThemLoai;
    private Button btnThemLoai;
    private String image = "";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    // Khi sửa danh mục: tên cũ và document id của danh mục đang sửa (null = thêm mới)
    private String editingOldName;
    private String editingDocId;
    private ProgressDialog progressDialog;
    
    // ActivityResultLaunchers cho chọn ảnh
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_loai_spactivity);

        initImageLaunchers();
        Init();
        Event();
    }
    
    private Uri cameraImageUri;
    
    private void initImageLaunchers() {
        // Camera launcher
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (cameraImageUri != null) {
                        uploadImageToStorage(cameraImageUri);
                    } else if (result.getData() != null && result.getData().getExtras() != null) {
                        // Nếu camera trả về Bitmap (fallback)
                        android.graphics.Bitmap bitmap = (android.graphics.Bitmap) result.getData().getExtras().get("data");
                        if (bitmap != null) {
                            Uri imageUri = saveBitmapToTempFile(bitmap);
                            if (imageUri != null) {
                                uploadImageToStorage(imageUri);
                            }
                        }
                    }
                }
            }
        );
        
        // Gallery launcher
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadImageToStorage(uri);
                }
            }
        );
    }
    
    private Uri saveBitmapToTempFile(android.graphics.Bitmap bitmap) {
        try {
            java.io.File tempFile = new java.io.File(getCacheDir(), "temp_camera_image_" + System.currentTimeMillis() + ".jpg");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
            // Sử dụng FileProvider để tạo URI an toàn
            return androidx.core.content.FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                tempFile
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void Event() {
        imgBackThemLoai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        imgThemLoai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImageSourceDialog();
            }
        });

        btnThemLoai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCategory();
            }
        });
    }

    /** Thêm mới hoặc cập nhật danh mục (kiểm tra tên trống / trùng tên). */
    private void saveCategory() {
        String newName = edtThemLoai.getText().toString().trim();
        if (newName.isEmpty()) {
            edtThemLoai.setError("Vui lòng nhập tên danh mục");
            return;
        }
        btnThemLoai.setEnabled(false);

        // Kiểm tra trùng tên (không phân biệt hoa thường, bỏ qua chính danh mục đang sửa)
        db.collection("LoaiProduct").get().addOnSuccessListener(all -> {
            for (QueryDocumentSnapshot q : all) {
                String name = q.getString("tenloai");
                if (name != null && name.trim().equalsIgnoreCase(newName) && !q.getId().equals(editingDocId)) {
                    btnThemLoai.setEnabled(true);
                    edtThemLoai.setError("Danh mục \"" + name + "\" đã tồn tại");
                    return;
                }
            }

            if (editingDocId == null) {
                // THÊM MỚI
                LoaiProduct sp = new LoaiProduct();
                sp.setTenloai(newName);
                sp.setHinhanh(image);
                db.collection("LoaiProduct").add(sp)
                        .addOnSuccessListener(ref -> onSaved("Đã thêm danh mục"))
                        .addOnFailureListener(e -> onSaveFailed(e));
            } else {
                // CẬP NHẬT danh mục + đổi tên loại trong các sản phẩm thuộc danh mục cũ
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("tenloai", newName);
                data.put("hinhanh", image);
                com.google.firebase.firestore.WriteBatch batch = db.batch();
                batch.update(db.collection("LoaiProduct").document(editingDocId), data);

                boolean renamed = !newName.equals(editingOldName);
                if (!renamed) {
                    batch.commit().addOnSuccessListener(v -> onSaved("Đã cập nhật danh mục"))
                            .addOnFailureListener(this::onSaveFailed);
                    return;
                }
                db.collection("SanPham").whereEqualTo("loaisp", editingOldName).get()
                        .addOnSuccessListener(products -> {
                            for (QueryDocumentSnapshot p : products) {
                                batch.update(p.getReference(), "loaisp", newName);
                            }
                            batch.commit().addOnSuccessListener(v -> onSaved(
                                    "Đã cập nhật danh mục (" + products.size() + " sản phẩm được đổi theo)"))
                                    .addOnFailureListener(this::onSaveFailed);
                        })
                        .addOnFailureListener(this::onSaveFailed);
            }
        }).addOnFailureListener(this::onSaveFailed);
    }

    private void onSaved(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void onSaveFailed(Exception e) {
        btnThemLoai.setEnabled(true);
        Toast.makeText(this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void Init() {
        imgBackThemLoai = findViewById(R.id.img_back_themloai);
        imgThemLoai = findViewById(R.id.image_them_loai);
        edtThemLoai = findViewById(R.id.edt_them_loai);
        btnThemLoai = findViewById(R.id.btn_them_loai);

        Intent intent = getIntent();
        String a = intent.getStringExtra("loaisp");
        editingOldName = TextUtils.isEmpty(a) ? null : a;
        edtThemLoai.setText(a);
        if (editingOldName != null) {
            btnThemLoai.setText("Lưu thay đổi");
        }
        if (editingOldName == null) return;
        db.collection("LoaiProduct").whereEqualTo("tenloai", a).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot q : queryDocumentSnapshots){
                    if (editingDocId == null) editingDocId = q.getId();
                    // Đọc field "hinhanh" (theo model) hoặc "hinhanhloai" (dữ liệu cũ)
                    String imageUrl = q.getString("hinhanh");
                    if (imageUrl == null || imageUrl.isEmpty()) {
                        imageUrl = q.getString("hinhanhloai");
                    }
                    if (!TextUtils.isEmpty(imageUrl)) {
                        Picasso.get().load(imageUrl).into(imgThemLoai);
                        image = imageUrl;
                    }
                }
            }
        });
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn nguồn ảnh");
        builder.setItems(new String[]{"Chụp ảnh", "Chọn từ thư viện"}, (dialog, which) -> {
            if (which == 0) {
                // Camera
                if (checkCameraPermission()) {
                    openCamera();
                }
            } else if (which == 1) {
                // Gallery - không cần permission với GetContent() trên Android 13+
                // Nhưng vẫn cần cho Android < 13
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ không cần permission khi dùng GetContent()
                    openGallery();
                } else {
                    // Android < 13 cần permission
                    if (checkStoragePermission()) {
                        openGallery();
                    }
                }
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
    
    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
            return false;
        }
        return true;
    }
    
    private boolean checkStoragePermission() {
        // Chỉ cần cho Android < 13
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            // Camera permission
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Cần quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 101) {
            // Storage permission
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Cần quyền truy cập thư viện để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void openCamera() {
        try {
            // Tạo file tạm để lưu ảnh
            java.io.File photoFile = new java.io.File(getCacheDir(), "camera_image_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                photoFile
            );
            
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                cameraLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Không thể mở camera", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: không chỉ định output URI
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                cameraLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Không thể mở camera", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void openGallery() {
        galleryLauncher.launch("image/*");
    }
    
    private void uploadImageToStorage(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(this, "Không thể lấy ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải ảnh lên...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        ImageUploadHelper.uploadImage(
            this,
            imageUri,
            "categories", // Folder trong Storage
            progressDialog,
            new ImageUploadHelper.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    image = imageUrl;
                    Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.pl)
                            .error(R.drawable.pl)
                            .into(imgThemLoai);
                    Toast.makeText(AdminAddLoaiSPActivity.this, "Tải ảnh lên thành công", Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(AdminAddLoaiSPActivity.this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        );
    }
}