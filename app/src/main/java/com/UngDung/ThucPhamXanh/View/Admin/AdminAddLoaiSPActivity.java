package com.UngDung.ThucPhamXanh.View.Admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;

import com.UngDung.ThucPhamXanh.Helper.CategoryForm;
import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thêm / sửa danh mục (LoaiProduct: tenloai, hinhanh). Mở với {@link #EXTRA_DOC_ID} + {@link #EXTRA_NAME} để sửa.
 * Sửa tên → các sản phẩm có loaisp = tên cũ được đổi theo trong cùng một lần ghi (batch).
 * Kiểm tra tên trống / quá dài / trùng (không phân biệt hoa-thường, dấu) bằng {@link CategoryForm}.
 */
public class AdminAddLoaiSPActivity extends BaseAdminActivity {

    private static final String TAG = "AdminAddLoaiSPActivity";
    public static final String EXTRA_DOC_ID = "loai_doc_id";
    public static final String EXTRA_NAME = "loai_name";
    private static final String STATE_IMAGE = "image";
    private static final int BATCH_LIMIT = 400; // Firestore tối đa 500 thao tác / batch

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ShapeableImageView imgCategory;
    private ImageView imgPlaceholder;
    private View progressImage;
    private MaterialButton btnPick, btnSave;
    private TextInputLayout tilName;
    private TextInputEditText edtName;
    private View renameNote;

    private AdminImagePicker imagePicker;

    private String editingDocId;      // null = thêm mới
    private String editingOldName;
    private String image = "";
    private String initialName = "";
    private String initialImage = "";
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_loai_spactivity);

        editingDocId = getIntent().getStringExtra(EXTRA_DOC_ID);
        editingOldName = getIntent().getStringExtra(EXTRA_NAME);
        boolean editing = !TextUtils.isEmpty(editingDocId);

        setupAdminToolbar(editing ? "Sửa danh mục" : "Thêm danh mục", null)
                .setNavigationOnClickListener(v -> attemptClose());
        applySystemBarInsets(null, findViewById(R.id.bottom_bar_loai));

        imgCategory = findViewById(R.id.image_them_loai);
        imgPlaceholder = findViewById(R.id.img_loai_placeholder);
        progressImage = findViewById(R.id.progress_image_loai);
        btnPick = findViewById(R.id.btn_pick_image_loai);
        btnSave = findViewById(R.id.btn_them_loai);
        tilName = findViewById(R.id.til_loai_name);
        edtName = findViewById(R.id.edt_them_loai);
        renameNote = findViewById(R.id.tv_loai_rename_note);
        btnSave.setText(editing ? "Lưu thay đổi" : "Thêm danh mục");

        imagePicker = new AdminImagePicker(this, "categories", "Ảnh danh mục", new AdminImagePicker.Listener() {
            @Override
            public void onUploadStart() {
                setUploading(true);
            }

            @Override
            public void onUploaded(String imageUrl) {
                image = imageUrl;
                setUploading(false);
            }

            @Override
            public void onUploadFailed() {
                setUploading(false);
            }
        });
        View.OnClickListener pick = v -> imagePicker.show();
        imgCategory.setOnClickListener(pick);
        btnPick.setOnClickListener(pick);
        btnSave.setOnClickListener(v -> save());

        edtName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                tilName.setError(null);
                renameNote.setVisibility(editing && !CategoryForm.cleanName(s.toString())
                        .equals(CategoryForm.cleanName(editingOldName)) ? View.VISIBLE : View.GONE);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                attemptClose();
            }
        });

        if (savedInstanceState != null) {
            image = savedInstanceState.getString(STATE_IMAGE, "");
            initialName = CategoryForm.cleanName(editingOldName);
            initialImage = savedInstanceState.getString("initial_image", "");
            showImage();
        } else if (editing) {
            edtName.setText(editingOldName);
            initialName = CategoryForm.cleanName(editingOldName);
            loadCurrent();
        } else {
            showImage();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_IMAGE, image);
        outState.putString("initial_image", initialImage);
    }

    /** Đọc lại bản ghi hiện tại (ảnh, tên mới nhất). */
    private void loadCurrent() {
        db.collection("LoaiProduct").document(editingDocId).get().addOnSuccessListener(d -> {
            if (isFinishing()) return;
            if (!d.exists()) {
                Toast.makeText(this, "Danh mục này không còn tồn tại", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            String name = FirestoreMapper.str(d, "tenloai");
            String img = FirestoreMapper.str(d, "hinhanh");
            if (img.isEmpty()) img = FirestoreMapper.str(d, "hinhanhloai"); // dữ liệu cũ
            editingOldName = name;
            initialName = CategoryForm.cleanName(name);
            initialImage = img;
            image = img;
            edtName.setText(name);
            showImage();
        }).addOnFailureListener(e -> Log.e(TAG, "Không đọc được danh mục", e));
    }

    private void showImage() {
        boolean has = !TextUtils.isEmpty(image);
        imgPlaceholder.setVisibility(has ? View.GONE : View.VISIBLE);
        if (has) {
            Picasso.get().load(image.trim()).placeholder(R.drawable.logo_new).error(R.drawable.logo_new).into(imgCategory);
        } else {
            Picasso.get().cancelRequest(imgCategory);
            imgCategory.setImageDrawable(null);
        }
    }

    private void setUploading(boolean uploading) {
        progressImage.setVisibility(uploading ? View.VISIBLE : View.GONE);
        btnPick.setEnabled(!uploading);
        btnSave.setEnabled(!uploading && !saving);
        if (uploading) imgPlaceholder.setVisibility(View.GONE);
        else showImage();
    }

    // ------------------------------------------------------------------ lưu

    private void save() {
        if (saving || imagePicker.isUploading()) return;
        final String newName = CategoryForm.cleanName(edtName.getText() == null ? "" : edtName.getText().toString());

        // Kiểm tra nhanh các lỗi không cần dữ liệu (rỗng / quá dài)
        String quick = CategoryForm.validateName(newName, null, null);
        if (quick != null) {
            tilName.setError(quick);
            edtName.requestFocus();
            return;
        }
        setSaving(true);
        // Đọc danh sách mới nhất để kiểm tra trùng tên
        db.collection("LoaiProduct").get().addOnSuccessListener(all -> {
            List<String> others = new ArrayList<>();
            for (QueryDocumentSnapshot q : all) {
                if (q.getId().equals(editingDocId)) continue; // chính danh mục đang sửa không tính là trùng
                others.add(FirestoreMapper.str(q, "tenloai"));
            }
            String error = CategoryForm.validateName(newName, others, null);
            if (error != null) {
                setSaving(false);
                tilName.setError(error);
                edtName.requestFocus();
                return;
            }
            if (editingDocId == null) create(newName);
            else update(newName);
        }).addOnFailureListener(this::onFailed);
    }

    private void create(String name) {
        Map<String, Object> data = new HashMap<>();
        data.put("tenloai", name);
        data.put("hinhanh", image == null ? "" : image);
        db.collection("LoaiProduct").add(data)
                .addOnSuccessListener(ref -> onSaved("Đã thêm danh mục"))
                .addOnFailureListener(this::onFailed);
    }

    private void update(String newName) {
        Map<String, Object> data = new HashMap<>();
        data.put("tenloai", newName);
        data.put("hinhanh", image == null ? "" : image);
        final boolean renamed = !newName.equals(editingOldName);

        if (!renamed) {
            db.collection("LoaiProduct").document(editingDocId).update(data)
                    .addOnSuccessListener(v -> onSaved("Đã cập nhật danh mục"))
                    .addOnFailureListener(this::onFailed);
            return;
        }
        // Đổi tên → đổi loaisp của các sản phẩm thuộc danh mục cũ; ghi theo từng batch ≤ 400 thao tác
        db.collection("SanPham").whereEqualTo("loaisp", editingOldName).get().addOnSuccessListener(products -> {
            List<DocumentSnapshot> docs = new ArrayList<>(products.getDocuments());
            WriteBatch first = db.batch();
            first.update(db.collection("LoaiProduct").document(editingDocId), data);
            int inFirst = Math.min(docs.size(), BATCH_LIMIT - 1);
            for (int i = 0; i < inFirst; i++) first.update(docs.get(i).getReference(), "loaisp", newName);
            first.commit().addOnSuccessListener(v -> commitRest(docs, inFirst, newName))
                    .addOnFailureListener(this::onFailed);
        }).addOnFailureListener(this::onFailed);
    }

    /** Các sản phẩm còn lại (danh mục có hơn ~400 sản phẩm); đệ quy từng batch. */
    private void commitRest(List<DocumentSnapshot> docs, int from, String newName) {
        if (from >= docs.size()) {
            onSaved(docs.isEmpty() ? "Đã cập nhật danh mục"
                    : "Đã cập nhật danh mục (" + docs.size() + " sản phẩm được đổi theo)");
            return;
        }
        WriteBatch b = db.batch();
        int to = Math.min(docs.size(), from + BATCH_LIMIT);
        for (int i = from; i < to; i++) b.update(docs.get(i).getReference(), "loaisp", newName);
        b.commit().addOnSuccessListener(v -> commitRest(docs, to, newName)).addOnFailureListener(this::onFailed);
    }

    private void setSaving(boolean s) {
        saving = s;
        btnSave.setEnabled(!s);
        btnSave.setText(s ? "Đang lưu..." : (editingDocId == null ? "Thêm danh mục" : "Lưu thay đổi"));
    }

    private void onSaved(String message) {
        saving = false;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void onFailed(Exception e) {
        Log.e(TAG, "Lưu danh mục thất bại", e);
        setSaving(false);
        Toast.makeText(this, "Lưu thất bại, vui lòng thử lại", Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------ thoát

    private boolean isDirty() {
        String name = CategoryForm.cleanName(edtName.getText() == null ? "" : edtName.getText().toString());
        return !name.equals(initialName) || !TextUtils.equals(image == null ? "" : image, initialImage == null ? "" : initialImage);
    }

    private void attemptClose() {
        if (saving || !isDirty()) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Bỏ thay đổi?")
                .setMessage("Những gì bạn vừa nhập sẽ không được lưu.")
                .setNegativeButton("Tiếp tục sửa", null)
                .setPositiveButton("Bỏ thay đổi", (d, w) -> finish())
                .show();
    }
}
