package com.UngDung.ThucPhamXanh.View.Admin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.UngDung.ThucPhamXanh.Helper.DashboardStats;
import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.ImageUploadHelper;
import com.UngDung.ThucPhamXanh.Helper.ProductForm;
import com.UngDung.ThucPhamXanh.Helper.ProductVisibility;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Thêm / sửa sản phẩm trang admin.
 *
 * Mở với extra "SP" (Product) = sửa, không có = thêm mới. Kiểm tra dữ liệu ở {@link ProductForm}
 * (lỗi hiện ngay dưới từng ô). Khi sửa chỉ cập nhật các field trên form — các field khác của
 * document (giaGoc, monanLienQuan, soNgayHSD…) giữ nguyên. Xóa = xóa mềm (daxoa = true).
 */
public class AdminAddSPActivity extends BaseAdminActivity {

    private static final String TAG = "AdminAddSPActivity";
    private static final String STATE_IMAGE = "image";
    private static final String STATE_INITIAL = "initial";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Product product;   // null = thêm mới
    private boolean isNew;
    private String image = "";
    private boolean uploading, saving;
    /** Ảnh chụp nội dung form lúc vừa nạp xong, để hỏi trước khi thoát nếu đã sửa. */
    private String initialState;

    private NestedScrollView scroll;
    private ImageView imgProduct, imgPlaceholder;
    private View progressImage;
    private TextView tvImageHint;
    private MaterialButton btnPickImage, btnSave, btnDelete;
    private TextInputLayout tilName, tilPrice, tilCategory, tilType, tilQuantity, tilWeight, tilExpiry,
            tilOrigin, tilDescription;
    private EditText edtName, edtPrice, edtQuantity, edtWeight, edtExpiry, edtOrigin, edtDescription;
    private AutoCompleteTextView ddCategory, ddType;

    private final List<String> categories = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private Uri cameraImageUri;

    public static Intent edit(Context context, Product product) {
        return new Intent(context, AdminAddSPActivity.class).putExtra("SP", product);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_spactivity);

        if (getIntent().hasExtra("SP")) product = (Product) getIntent().getSerializableExtra("SP");
        isNew = product == null || TextUtils.isEmpty(product.getId());

        setupAdminToolbar(isNew ? "Thêm sản phẩm" : "Sửa sản phẩm", isNew ? null : product.getTensp())
                .setNavigationOnClickListener(v -> attemptClose());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                attemptClose();
            }
        });

        bindViews();
        applyBottomInsets(findViewById(R.id.bottom_bar));
        initImageLaunchers();
        setupDropdowns();
        setupEvents();

        if (savedInstanceState != null) {
            // Ô nhập tự khôi phục chữ; chỉ cần ảnh và mốc "chưa sửa"
            image = savedInstanceState.getString(STATE_IMAGE, "");
            initialState = savedInstanceState.getString(STATE_INITIAL);
            showImage();
        } else if (isNew) {
            ddType.setText(ProductForm.typeLabel(1), false);
            initialState = formState();
        } else {
            fillFrom(product);
            initialState = formState();
            loadLatest();
        }

        btnDelete.setVisibility(isNew ? View.GONE : View.VISIBLE);
        btnSave.setText(isNew ? "Thêm sản phẩm" : "Lưu thay đổi");
        updateExpiryHint();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_IMAGE, image);
        outState.putString(STATE_INITIAL, initialState);
    }

    @Override
    protected void onStartListening() {
        listen(db.collection("LoaiProduct").orderBy("tenloai", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { Log.e(TAG, "Lỗi đọc LoaiProduct", e); return; }
                    if (snap == null) return;
                    categories.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String name = d.getString("tenloai");
                        if (name == null || name.trim().isEmpty()) continue;
                        if (!categories.contains(name.trim())) categories.add(name.trim()); // LoaiProduct có bản ghi trùng tên
                    }
                    // Giữ được loại cũ của sản phẩm dù đã bị đổi tên / xóa khỏi LoaiProduct
                    String current = ddCategory.getText().toString().trim();
                    if (!current.isEmpty() && !categories.contains(current)) categories.add(current);
                    categoryAdapter.notifyDataSetChanged();
                }));
    }

    // ------------------------------------------------------------------ nạp dữ liệu

    private void bindViews() {
        scroll = findViewById(R.id.form_scroll);
        imgProduct = findViewById(R.id.image_add);
        imgPlaceholder = findViewById(R.id.img_image_placeholder);
        progressImage = findViewById(R.id.progress_image);
        tvImageHint = findViewById(R.id.tv_image_hint);
        btnPickImage = findViewById(R.id.btn_pick_image);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);

        tilName = findViewById(R.id.til_name);
        tilPrice = findViewById(R.id.til_price);
        tilCategory = findViewById(R.id.til_category);
        tilType = findViewById(R.id.til_type);
        tilQuantity = findViewById(R.id.til_quantity);
        tilWeight = findViewById(R.id.til_weight);
        tilExpiry = findViewById(R.id.til_expiry);
        tilOrigin = findViewById(R.id.til_origin);
        tilDescription = findViewById(R.id.til_description);

        edtName = findViewById(R.id.edt_tensp);
        edtPrice = findViewById(R.id.edt_giatiensp);
        edtQuantity = findViewById(R.id.edt_soluongsp);
        edtWeight = findViewById(R.id.edt_trongluongsp);
        edtExpiry = findViewById(R.id.edt_hansudungsp);
        edtOrigin = findViewById(R.id.edt_xuatxu);
        edtDescription = findViewById(R.id.edt_motasp);
        ddCategory = findViewById(R.id.dd_category);
        ddType = findViewById(R.id.dd_type);
    }

    private void fillFrom(Product p) {
        edtName.setText(p.getTensp());
        edtPrice.setText(p.getGiatien() > 0 ? String.valueOf(p.getGiatien()) : "");
        ddCategory.setText(p.getLoaisp(), false);
        setType(p.getType());
        edtQuantity.setText(String.valueOf(Math.max(0, p.getSoluong())));
        edtWeight.setText(p.getTrongluong());
        edtExpiry.setText(p.getHansudung());
        edtDescription.setText(p.getMota());
        image = p.getHinhanh() == null ? "" : p.getHinhanh();
        showImage();
        updateExpiryHint();
    }

    /** Đọc lại document (dữ liệu truyền qua Intent có thể cũ) và lấy thêm xuất xứ. */
    private void loadLatest() {
        db.collection("SanPham").document(product.getId()).get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || !doc.exists()) return;
                    if (ProductVisibility.isHidden(doc)) {
                        Toast.makeText(this, "Sản phẩm này đã bị xóa", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    if (isDirty()) return; // không ghi đè thứ admin đang gõ
                    fillFrom(FirestoreMapper.toProduct(doc));
                    edtOrigin.setText(FirestoreMapper.str(doc, ProductForm.F_ORIGIN));
                    initialState = formState();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi đọc sản phẩm", e));
    }

    // ------------------------------------------------------------------ giao diện

    private void setupDropdowns() {
        categoryAdapter = new NoFilterAdapter(this, categories);
        ddCategory.setAdapter(categoryAdapter);
        ddCategory.setOnItemClickListener((parent, view, position, id) -> tilCategory.setError(null));

        List<String> types = new ArrayList<>();
        for (long t : ProductForm.TYPE_VALUES) types.add(ProductForm.typeLabel(t));
        ddType.setAdapter(new NoFilterAdapter(this, types));
    }

    private void setType(long type) {
        if (type <= 0) type = 1; // thiếu field → "Thường" (trước đây không hiện ở mục nào trên trang chủ)
        String label = ProductForm.typeLabel(type);
        @SuppressWarnings("unchecked")
        ArrayAdapter<String> a = (ArrayAdapter<String>) ddType.getAdapter();
        if (a.getPosition(label) < 0) a.add(label); // giá trị lạ trong dữ liệu cũ: vẫn giữ
        ddType.setText(label, false);
    }

    private void setupEvents() {
        View.OnClickListener pick = v -> showImageSourceDialog();
        imgProduct.setOnClickListener(pick);
        btnPickImage.setOnClickListener(pick);

        findViewById(R.id.btn_add_category).setOnClickListener(v ->
                startActivity(new Intent(this, AdminAddLoaiSPActivity.class)));

        edtExpiry.setOnClickListener(v -> showDatePicker());
        tilExpiry.setEndIconOnClickListener(v -> showDatePicker());

        clearErrorOnEdit(edtName, tilName);
        clearErrorOnEdit(edtPrice, tilPrice);
        clearErrorOnEdit(edtQuantity, tilQuantity);
        clearErrorOnEdit(edtWeight, tilWeight);
        clearErrorOnEdit(edtDescription, tilDescription);

        btnSave.setOnClickListener(v -> save());
        btnDelete.setOnClickListener(v -> confirm("Xóa sản phẩm?",
                "\"" + edtName.getText().toString().trim() + "\" sẽ bị gỡ khỏi cửa hàng. "
                        + "Các đơn hàng cũ có sản phẩm này vẫn được giữ nguyên.",
                "Xóa", this::deleteProduct));
    }

    private static void clearErrorOnEdit(EditText edt, TextInputLayout til) {
        edt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (til.getError() != null) til.setError(null);
            }
        });
    }

    /** Thanh nút đáy: chừa chỗ cho thanh điều hướng và bàn phím (edge-to-edge trên Android 15+). */
    private void applyBottomInsets(View bar) {
        final int base = bar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(bar, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    base + Math.max(sys.bottom, ime.bottom));
            return insets;
        });
    }

    private void showImage() {
        boolean has = !TextUtils.isEmpty(image);
        imgPlaceholder.setVisibility(has || uploading ? View.GONE : View.VISIBLE);
        btnPickImage.setText(has ? "Đổi ảnh" : "Chọn ảnh");
        if (has) {
            Picasso.get().load(image).fit().centerCrop().into(imgProduct);
        } else {
            Picasso.get().cancelRequest(imgProduct);
            imgProduct.setImageDrawable(null);
        }
    }

    private void setImageError(String error) {
        tvImageHint.setText(error == null ? "Ảnh vuông, rõ nét, nền sáng" : error);
        tvImageHint.setTextColor(ContextCompat.getColor(this,
                error == null ? R.color.admin_text_muted : R.color.admin_danger));
    }

    // ------------------------------------------------------------------ hạn sử dụng

    private void showDatePicker() {
        LocalDate today = LocalDate.now();
        LocalDate current = DashboardStats.parseDate(edtExpiry.getText().toString());
        LocalDate selected = current != null && !current.isBefore(today) ? current : today;

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Hạn sử dụng")
                .setSelection(selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointForward.from(
                                today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()))
                        .build())
                .build();
        picker.addOnPositiveButtonClickListener(millis -> {
            LocalDate d = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate();
            edtExpiry.setText(formatDate(d));
            tilExpiry.setError(null);
            updateExpiryHint();
        });
        picker.show(getSupportFragmentManager(), "expiry");
    }

    private static String formatDate(LocalDate d) {
        return String.format(Locale.ROOT, "%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    private void updateExpiryHint() {
        String hint = ProductForm.expiryHint(edtExpiry.getText().toString(), LocalDate.now());
        tilExpiry.setHelperText(hint.isEmpty() ? null : hint);
    }

    // ------------------------------------------------------------------ lưu / xóa

    private ProductForm.Input readInput() {
        ProductForm.Input in = new ProductForm.Input();
        in.image = image;
        in.name = edtName.getText().toString();
        in.price = edtPrice.getText().toString();
        in.category = ddCategory.getText().toString();
        in.quantity = edtQuantity.getText().toString();
        in.weight = edtWeight.getText().toString();
        in.expiry = edtExpiry.getText().toString();
        in.description = edtDescription.getText().toString();
        in.origin = edtOrigin.getText().toString();
        return in;
    }

    private void save() {
        if (saving) return;
        if (uploading) {
            Toast.makeText(this, "Ảnh đang được tải lên, đợi một chút", Toast.LENGTH_SHORT).show();
            return;
        }
        ProductForm.Input in = readInput();
        Map<String, String> errors = ProductForm.validate(in, LocalDate.now(), isNew);
        showErrors(errors);
        if (!errors.isEmpty()) return;

        Long type = ProductForm.typeOf(ddType.getText().toString());
        Map<String, Object> data = new HashMap<>();
        data.put(ProductForm.F_NAME, in.name.trim());
        data.put(ProductForm.F_PRICE, ProductForm.parseWholeNumber(in.price));
        data.put(ProductForm.F_CATEGORY, in.category.trim());
        data.put(ProductForm.F_TYPE, type == null ? 1L : type);
        data.put(ProductForm.F_QUANTITY, ProductForm.parseWholeNumber(in.quantity));
        data.put(ProductForm.F_WEIGHT, in.weight.trim());
        data.put(ProductForm.F_EXPIRY, formatDate(DashboardStats.parseDate(in.expiry)));
        data.put(ProductForm.F_DESCRIPTION, in.description.trim());
        data.put(ProductForm.F_IMAGE, image);
        String origin = in.origin == null ? "" : in.origin.trim();
        if (!origin.isEmpty()) data.put(ProductForm.F_ORIGIN, origin);
        else if (!isNew) data.put(ProductForm.F_ORIGIN, FieldValue.delete());

        setSaving(true);
        if (isNew) {
            db.collection("SanPham").add(data)
                    .addOnSuccessListener(ref -> done("Đã thêm sản phẩm"))
                    .addOnFailureListener(this::saveFailed);
        } else {
            db.collection("SanPham").document(product.getId()).update(data)
                    .addOnSuccessListener(unused -> done("Đã lưu thay đổi"))
                    .addOnFailureListener(this::saveFailed);
        }
    }

    private void showErrors(Map<String, String> errors) {
        setImageError(errors.get(ProductForm.F_IMAGE));
        tilName.setError(errors.get(ProductForm.F_NAME));
        tilPrice.setError(errors.get(ProductForm.F_PRICE));
        tilCategory.setError(errors.get(ProductForm.F_CATEGORY));
        tilQuantity.setError(errors.get(ProductForm.F_QUANTITY));
        tilWeight.setError(errors.get(ProductForm.F_WEIGHT));
        tilExpiry.setError(errors.get(ProductForm.F_EXPIRY));
        tilDescription.setError(errors.get(ProductForm.F_DESCRIPTION));
        if (errors.isEmpty()) return;

        String first = errors.keySet().iterator().next();
        View target;
        switch (first) {
            case ProductForm.F_IMAGE: target = imgProduct; break;
            case ProductForm.F_NAME: target = tilName; break;
            case ProductForm.F_PRICE: target = tilPrice; break;
            case ProductForm.F_CATEGORY: target = tilCategory; break;
            case ProductForm.F_QUANTITY: target = tilQuantity; break;
            case ProductForm.F_WEIGHT: target = tilWeight; break;
            case ProductForm.F_EXPIRY: target = tilExpiry; break;
            default: target = tilDescription;
        }
        scrollTo(target);
        Toast.makeText(this, errors.get(first), Toast.LENGTH_SHORT).show();
    }

    /** Cuộn để view (nằm sâu trong form) hiện ra, chừa một khoảng phía trên. */
    private void scrollTo(View v) {
        int y = 0;
        View cur = v;
        while (cur != null && cur != scroll) {
            y += cur.getTop();
            cur = cur.getParent() instanceof View ? (View) cur.getParent() : null;
        }
        int margin = getResources().getDimensionPixelSize(R.dimen.admin_space_xl);
        scroll.smoothScrollTo(0, Math.max(0, y - margin));
    }

    private void setSaving(boolean s) {
        saving = s;
        btnSave.setEnabled(!s);
        btnDelete.setEnabled(!s);
        btnSave.setText(s ? "Đang lưu…" : (isNew ? "Thêm sản phẩm" : "Lưu thay đổi"));
    }

    private void done(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void saveFailed(Exception e) {
        Log.e(TAG, "Lưu sản phẩm thất bại", e);
        setSaving(false);
        Toast.makeText(this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void deleteProduct() {
        // XÓA MỀM: chỉ đánh dấu daxoa = true. Sản phẩm biến mất khỏi cửa hàng và danh sách admin,
        // nhưng hóa đơn / chi tiết hóa đơn / thống kê / lịch sử mua hàng vẫn còn nguyên.
        Map<String, Object> hide = new HashMap<>();
        hide.put(ProductVisibility.FIELD_DELETED, true);
        hide.put("ngayxoa", System.currentTimeMillis());
        setSaving(true);
        db.collection("SanPham").document(product.getId()).update(hide)
                .addOnSuccessListener(unused -> done("Đã xóa sản phẩm khỏi cửa hàng"))
                .addOnFailureListener(this::saveFailed);
    }

    // ------------------------------------------------------------------ thoát khi chưa lưu

    private String formState() {
        return image + '\u0001' + edtName.getText() + '\u0001' + edtPrice.getText() + '\u0001'
                + ddCategory.getText() + '\u0001' + ddType.getText() + '\u0001' + edtQuantity.getText()
                + '\u0001' + edtWeight.getText() + '\u0001' + edtExpiry.getText() + '\u0001'
                + edtOrigin.getText() + '\u0001' + edtDescription.getText();
    }

    private boolean isDirty() {
        return initialState != null && !initialState.equals(formState());
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

    // ------------------------------------------------------------------ ảnh

    private void initImageLaunchers() {
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && cameraImageUri != null) {
                        uploadImage(cameraImageUri);
                    }
                });
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) uploadImage(uri); });
    }

    private void showImageSourceDialog() {
        if (uploading) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Ảnh sản phẩm")
                .setItems(new String[]{"Chụp ảnh", "Chọn từ thư viện"}, (dialog, which) -> {
                    if (which == 0) {
                        if (hasPermission(Manifest.permission.CAMERA, 100)) openCamera();
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            || hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 101)) {
                        galleryLauncher.launch("image/*"); // Android 13+ không cần quyền với GetContent
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private boolean hasPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) return true;
        ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (requestCode == 100) {
            if (granted) openCamera();
            else Toast.makeText(this, "Cần quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show();
        } else if (requestCode == 101) {
            if (granted) galleryLauncher.launch("image/*");
            else Toast.makeText(this, "Cần quyền truy cập thư viện để chọn ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera() {
        try {
            File photo = new File(getCacheDir(), "camera_image_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photo);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            cameraLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Không mở được camera", e);
            Toast.makeText(this, "Không thể mở camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImage(Uri uri) {
        setUploading(true);
        ImageUploadHelper.uploadImage(this, uri, "products", null, new ImageUploadHelper.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                if (isFinishing()) return;
                image = imageUrl;
                setUploading(false);
                setImageError(null);
            }

            @Override
            public void onFailure(Exception e) {
                // ImageUploadHelper đã báo lỗi bằng Toast
                if (!isFinishing()) setUploading(false);
            }
        });
    }

    private void setUploading(boolean u) {
        uploading = u;
        progressImage.setVisibility(u ? View.VISIBLE : View.GONE);
        btnPickImage.setEnabled(!u);
        showImage();
    }

    // ------------------------------------------------------------------ adapter dropdown

    /**
     * ArrayAdapter mặc định lọc theo chữ đang có trong ô → sau khi chọn (hoặc xoay màn hình)
     * dropdown chỉ còn đúng một mục. Adapter này luôn hiện đủ danh sách.
     */
    private static final class NoFilterAdapter extends ArrayAdapter<String> {
        private final List<String> items;

        NoFilterAdapter(Context context, List<String> items) {
            super(context, android.R.layout.simple_list_item_1, items);
            this.items = items;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            TextView tv = (TextView) super.getView(position, convertView, parent);
            tv.setTextAppearance(R.style.Admin_Text);
            return tv;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults r = new FilterResults();
                    r.values = items;
                    r.count = items.size();
                    return r;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    notifyDataSetChanged();
                }
            };
        }
    }
}
