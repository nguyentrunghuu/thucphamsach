package com.UngDung.ThucPhamXanh.View.Admin;

import static android.view.View.GONE;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.UngDung.ThucPhamXanh.Helper.ImageUploadHelper;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminAddSPActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private TextView tvTaoMaQR;
    private ImageView imgQRProduct;
    private Button btnQRProduct, btnDownQRProduct;

    private CircleImageView imgAddLoaiProduct;
    private ImageView btnAddBack, btnRefresh, btnSave;
    private EditText edtTenSP, edtGiatienSP, edtHansudungSP, edtTrongluongSP, edtSoluongSP, edtTypeSP, edtMotaSP;
    private ImageView imgAdd;
    private TextView btnDanhmuc, btnDelete;
    private AppCompatButton btnEdit;
    private Spinner spinnerDanhMuc;
    private TextView tvTitle;
    private Calendar calendar;
    private View layoutActionButtons;

    private FirebaseFirestore db;
    private List<String> list;
    private Product product;
    private String image = "";
    private String loaisp = "";
    private ProgressDialog progressDialog;
    
    // ActivityResultLaunchers cho chọn ảnh
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private Uri cameraImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_spactivity);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        initImageLaunchers();
        InitWidget();
        Init();

//        // Test: Lấy ra id của hóa đơn từ bảng HoaDon
//        db.collection("HoaDon").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//            @Override
//            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
//                for (QueryDocumentSnapshot q: queryDocumentSnapshots){
//                    ArrayList<String> list = new ArrayList<>();
//                    list.add(q.getId());
//                    Log.d("checkid", q.getId());
//                }
//            }
//        });
        Event();
        setupSaveAndEditListeners();
        
        btnQRProduct.setVisibility(GONE);
        btnDownQRProduct.setVisibility(GONE);
    }

    private void Event() {
        btnAddBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        imgAddLoaiProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminAddSPActivity.this, AdminAddLoaiSPActivity.class);
                intent.putExtra("loaisp", loaisp);
                startActivity(intent);
            }
        });

        // Date picker cho hạn sử dụng
        calendar = Calendar.getInstance();
        edtHansudungSP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog();
            }
        });
        
        // Chọn danh mục với dialog
        btnDanhmuc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCategoryDialog();
            }
        });
        spinnerDanhMuc.setOnItemSelectedListener(this);
        imgAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImageSourceDialog();
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                edtHansudungSP.setText("");
                edtTrongluongSP.setText("");
                edtTenSP.setText("");
                edtGiatienSP.setText("");
                edtTypeSP.setText("");
                edtMotaSP.setText("");
                edtSoluongSP.setText("");
                image = "";
                imgAdd.setImageResource(R.drawable.pl);
            }
        });

        // Nếu xóa bất kỳ 1 sản phẩm nào đó thì những hóa đơn có chứa sản phẩm đó cũng phải bị xóa hoặc dùng nhiều cách khác.
        // Ở đây lựa chọn xóa luôn hóa đơn chứa sản phẩm bị xóa.
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (product == null || product.getId() == null) {
                    Toast.makeText(AdminAddSPActivity.this, "Không tìm thấy sản phẩm để xóa", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Xác nhận trước khi xóa
                new AlertDialog.Builder(AdminAddSPActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Sản phẩm sẽ bị gỡ khỏi cửa hàng. Các đơn hàng cũ có sản phẩm này vẫn được giữ nguyên.\nBạn có chắc chắn muốn xóa?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            deleteProduct();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
    }
    
    private void deleteProduct() {
        if (product == null || product.getId() == null) {
            return;
        }
        
        // XÓA MỀM: chỉ đánh dấu daxoa = true. Sản phẩm biến mất khỏi cửa hàng và danh sách admin,
        // nhưng hóa đơn / chi tiết hóa đơn / thống kê / lịch sử mua hàng vẫn còn nguyên.
        Map<String, Object> hide = new HashMap<>();
        hide.put(com.UngDung.ThucPhamXanh.Helper.ProductVisibility.FIELD_DELETED, true);
        hide.put("ngayxoa", System.currentTimeMillis());
        db.collection("SanPham").document(product.getId()).update(hide).addOnSuccessListener(unused -> {
                    Toast.makeText(AdminAddSPActivity.this, "Đã xóa sản phẩm khỏi cửa hàng", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }).addOnFailureListener(e -> {
                    Toast.makeText(AdminAddSPActivity.this, "Xoá sản phẩm thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("DeleteProduct", "Error deleting product", e);
                });
    }

    private void setupSaveAndEditListeners() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!validate()) {
                    return;
                }
                try {
                    Product sp = new Product();
                    sp.setGiatien(Long.parseLong(edtGiatienSP.getText().toString()));
                    sp.setMota(edtMotaSP.getText().toString());
                    sp.setHansudung(edtHansudungSP.getText().toString());
                    sp.setType(Long.parseLong(edtTypeSP.getText().toString()));
                    sp.setTensp(edtTenSP.getText().toString());
                    sp.setSoluong(Long.parseLong(edtSoluongSP.getText().toString()));
                    sp.setTrongluong(edtTrongluongSP.getText().toString());
                    sp.setLoaisp(spinnerDanhMuc.getSelectedItem().toString());
                    sp.setHinhanh(image);

                    db.collection("SanPham").add(sp).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(@NonNull DocumentReference documentReference) {
                            Toast.makeText(AdminAddSPActivity.this, "Thêm sản phẩm thành công!!!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(AdminAddSPActivity.this, "Thêm sản phẩm thất bại!!!", Toast.LENGTH_SHORT).show();
                            Log.e("AddProduct", "Error adding product", e);
                        }
                    });
                } catch (NumberFormatException e) {
                    Toast.makeText(AdminAddSPActivity.this, "Vui lòng nhập đúng định dạng số", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (Exception e) {
                    Toast.makeText(AdminAddSPActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (product == null || product.getId() == null) {
                    Toast.makeText(AdminAddSPActivity.this, "Không tìm thấy sản phẩm để cập nhật", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (!validate()) {
                    return;
                }
                
                try {
                    // Tạo Map để cập nhật các field
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("tensp", edtTenSP.getText().toString());
                    updateData.put("giatien", Long.parseLong(edtGiatienSP.getText().toString()));
                    updateData.put("mota", edtMotaSP.getText().toString());
                    updateData.put("hansudung", edtHansudungSP.getText().toString());
                    updateData.put("type", Long.parseLong(edtTypeSP.getText().toString()));
                    updateData.put("soluong", Long.parseLong(edtSoluongSP.getText().toString()));
                    updateData.put("trongluong", edtTrongluongSP.getText().toString());
                    updateData.put("loaisp", spinnerDanhMuc.getSelectedItem().toString());
                    updateData.put("hinhanh", image);
                    
                    // Cập nhật sản phẩm trong Firestore
                    db.collection("SanPham").document(product.getId())
                            .update(updateData)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(@NonNull Void unused) {
                                    Toast.makeText(AdminAddSPActivity.this, "Cập nhật sản phẩm thành công!!!", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(AdminAddSPActivity.this, "Cập nhật sản phẩm thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("UpdateProduct", "Error updating product", e);
                        }
                    });
                } catch (NumberFormatException e) {
                    Toast.makeText(AdminAddSPActivity.this, "Vui lòng nhập đúng định dạng số", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (Exception e) {
                    Toast.makeText(AdminAddSPActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }

    private void Init() {
        db = FirebaseFirestore.getInstance();
        list = new ArrayList<>();
        list.add("Chọn Danh mục");

        // Nhận data từ AdminProductActivity
        if (getIntent() != null && getIntent().hasExtra("SP")) {
            product = (Product) getIntent().getSerializableExtra("SP");
        }
        if (product != null) {
            btnQRProduct.setVisibility(View.VISIBLE);
            imgQRProduct.setVisibility(View.VISIBLE);
            tvTaoMaQR.setVisibility(View.VISIBLE);
            db.collection("QRProduct").whereEqualTo("idproduct", product.getId())
                    .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    for (QueryDocumentSnapshot q : queryDocumentSnapshots){
                        if (q.getString("idproduct").equals(product.getId())){
                            btnQRProduct.setVisibility(GONE);
                            btnDownQRProduct.setVisibility(View.VISIBLE);
                            Picasso.get().load(q.getString("hinhanh_qr")).into(imgQRProduct);
                            tvTaoMaQR.setText("Mã QR sản phẩm");
                        }
                    }
                }
            });

            edtTrongluongSP.setText(product.getTrongluong());
            edtMotaSP.setText(product.getMota());
            edtHansudungSP.setText(product.getHansudung());
            edtSoluongSP.setText(product.getSoluong() + "");
            edtGiatienSP.setText(product.getGiatien() + "");
            edtTenSP.setText(product.getTensp());
            edtTypeSP.setText(product.getType()+"");
            // Hiển thị container chứa các button Edit và Delete
            if (layoutActionButtons != null) {
                layoutActionButtons.setVisibility(View.VISIBLE);
            }
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
            btnSave.setVisibility(GONE);
            tvTitle.setText("Edit sản phẩm");
            if (!TextUtils.isEmpty(product.getHinhanh())) {
                Picasso.get().load(product.getHinhanh()).into(imgAdd);
                image = product.getHinhanh();
            }
        } else {
            // Chế độ thêm mới - ẩn container Edit/Delete, hiển thị btnSave
            if (layoutActionButtons != null) {
                layoutActionButtons.setVisibility(View.GONE);
            }
            btnSave.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }

        db.collection("LoaiProduct").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot q : queryDocumentSnapshots) {
                    list.add(q.getString("tenloai"));
                    Log.d("TAG", "onSuccess: " + q.getString("tenloai"));
                }
                ArrayAdapter arrayAdapter = new ArrayAdapter(AdminAddSPActivity.this, android.R.layout.simple_list_item_1, list);
                spinnerDanhMuc.setAdapter(arrayAdapter);
                if (list.size() > 1) {
                    spinnerDanhMuc.setSelection(1);
                    if (product != null) {
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).equals(product.getLoaisp())) {
                                spinnerDanhMuc.setSelection(i);
                                btnDanhmuc.setText(product.getLoaisp());
                                loaisp = product.getLoaisp();
                                break;
                            }
                        }
                    } else {
                        // Set default selection
                        btnDanhmuc.setText(list.get(1));
                        loaisp = list.get(1);
                    }
                }

            }
        });

    }

    private boolean validate() {
        if (TextUtils.isEmpty(image)) {
            Toast.makeText(this, "Vui lòng chọn hình ảnh", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(edtGiatienSP.getText().toString())) {
            Toast.makeText(this, "Vui lòng nhập giá tiền", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(edtTenSP.getText().toString())) {
            Toast.makeText(this, "Vui lòng nhập tên sản phẩm", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(edtHansudungSP.getText().toString())) {
            Toast.makeText(this, "Vui lòng nhập hạn sử dụng", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(edtTrongluongSP.getText().toString())) {
            Toast.makeText(this, "Vui lòng nhập Trọng lượng", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(edtSoluongSP.getText().toString())) {
            Toast.makeText(this, "Vui lòng nhập số lượng", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(edtTypeSP.getText().toString())) {
            Toast.makeText(this, "Vui lòng nhập type", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(edtMotaSP.getText().toString())) {
            Toast.makeText(this, "Vui lòng nhập mô tả", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void InitWidget() {
        btnDownQRProduct = findViewById(R.id.btn_down_qr_product);
        tvTaoMaQR = findViewById(R.id.tv_taomaqr);
        imgQRProduct = findViewById(R.id.img_qr_product);
        btnQRProduct = findViewById(R.id.btn_qr_product);
        tvTitle = findViewById(R.id.tv_title);
        imgAddLoaiProduct = findViewById(R.id.img_add_loaiproduct);
        btnAddBack = findViewById(R.id.btn_add_back);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnSave = findViewById(R.id.btn_save);
        edtTenSP = findViewById(R.id.edt_tensp);
        edtGiatienSP = findViewById(R.id.edt_giatiensp);
        edtHansudungSP = findViewById(R.id.edt_hansudungsp);
        edtTrongluongSP = findViewById(R.id.edt_trongluongsp);
        edtSoluongSP = findViewById(R.id.edt_soluongsp);
        edtTypeSP = findViewById(R.id.edt_typesp);
        edtMotaSP = findViewById(R.id.edt_motasp);
        imgAdd = findViewById(R.id.image_add);
        btnDanhmuc = findViewById(R.id.btn_danhmuc);
        // Đảm bảo btnDanhmuc là TextView (từ layout mới)
        btnDelete = findViewById(R.id.btn_delete);
        btnEdit = findViewById(R.id.btn_edit);
        spinnerDanhMuc = findViewById(R.id.spinner_danhmuc);
        layoutActionButtons = findViewById(R.id.layout_action_buttons);
    }

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
            "products", // Folder trong Storage
            progressDialog,
            new ImageUploadHelper.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    image = imageUrl;
                    Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.logo_new)
                            .error(R.drawable.logo_new)
                            .into(imgAdd);
                    Toast.makeText(AdminAddSPActivity.this, "Tải ảnh lên thành công", Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(AdminAddSPActivity.this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void showDatePickerDialog() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    edtHansudungSP.setText(sdf.format(calendar.getTime()));
                }
            },
            year, month, day
        );
        
        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }
    
    private void showCategoryDialog() {
        if (list == null || list.size() <= 1) {
            Toast.makeText(this, "Chưa có danh mục nào. Vui lòng thêm danh mục trước!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Tạo danh sách danh mục (bỏ "Chọn Danh mục")
        List<String> categories = new ArrayList<>();
        for (int i = 1; i < list.size(); i++) {
            categories.add(list.get(i));
        }
        
        String[] categoryArray = categories.toArray(new String[0]);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn danh mục");
        builder.setItems(categoryArray, (dialog, which) -> {
            String selectedCategory = categoryArray[which];
            btnDanhmuc.setText(selectedCategory);
            loaisp = selectedCategory;
            
            // Cập nhật spinner để đồng bộ
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).equals(selectedCategory)) {
                    spinnerDanhMuc.setSelection(i);
                    break;
                }
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        if (position > 0) {
            btnDanhmuc.setText(spinnerDanhMuc.getSelectedItem().toString());
            String s = list.get(position);
            loaisp = s;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}