package com.UngDung.ThucPhamXanh.View.Admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.PromotionRules;
import com.UngDung.ThucPhamXanh.Helper.TextNormalizer;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Khuyến mãi theo loại sản phẩm (Promotions/current_promotion.categoryDiscounts). Dữ liệu đọc/ghi qua
 * {@link PromotionRules}. Chỉnh sửa nằm trên máy cho đến khi bấm "Lưu khuyến mãi" (nút chỉ bật khi có thay đổi);
 * được phép lưu bộ trống để tắt toàn bộ khuyến mãi. Quy tắc của loại không còn tồn tại được đánh dấu để xóa.
 */
public class AdminPromotionActivity extends BaseAdminActivity {

    private static final String TAG = "AdminPromotionActivity";
    private static final String PROMOTION_DOC_ID = "current_promotion";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private MaterialToolbar toolbar;
    private RecyclerView rcv;
    private View emptyView, progress;
    private MaterialButton btnSave, btnReset;
    private AdminPromotionAdapter adapter;

    private final List<String> categories = new ArrayList<>();
    private Map<String, PromotionRules.Rule> saved = new LinkedHashMap<>();    // đang lưu trên Firestore
    private Map<String, PromotionRules.Rule> working = new LinkedHashMap<>();  // đang chỉnh trên máy
    private boolean categoriesLoaded, rulesLoaded, saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_promotion);
        toolbar = setupAdminToolbar("Khuyến mãi", null);
        toolbar.setNavigationOnClickListener(v -> attemptClose());

        rcv = findViewById(R.id.rcv_category_discounts);
        emptyView = findViewById(R.id.admin_empty);
        progress = findViewById(R.id.progress_promotion);
        btnSave = findViewById(R.id.btn_save_promotion);
        btnReset = findViewById(R.id.btn_reset_promotion);
        applySystemBarInsets(null, findViewById(R.id.bottom_bar_promotion));

        adapter = new AdminPromotionAdapter(this::showRuleDialog);
        rcv.setLayoutManager(new LinearLayoutManager(this));
        rcv.setAdapter(adapter);

        btnSave.setOnClickListener(v -> save());
        btnReset.setOnClickListener(v -> {
            working = new LinkedHashMap<>(saved);
            render();
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                attemptClose();
            }
        });

        loadRules();
    }

    @Override
    protected void onStartListening() {
        listen(db.collection("LoaiProduct").orderBy("tenloai", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Lỗi đọc LoaiProduct", e);
                        Toast.makeText(this, "Không tải được danh mục", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snap == null) return;
                    categories.clear();
                    Set<String> seen = new HashSet<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String name = FirestoreMapper.str(d, "tenloai").trim();
                        if (!name.isEmpty() && seen.add(name)) categories.add(name); // bỏ tên trùng hệt nhau
                    }
                    categoriesLoaded = true;
                    render();
                }));
    }

    /** Chỉ đọc một lần: không nghe liên tục để không ghi đè những gì admin đang chỉnh dở. */
    private void loadRules() {
        db.collection("Promotions").document(PROMOTION_DOC_ID).get().addOnSuccessListener(doc -> {
            if (isFinishing()) return;
            saved = doc.exists() ? PromotionRules.parse(doc.get("categoryDiscounts")) : new LinkedHashMap<>();
            working = new LinkedHashMap<>(saved);
            rulesLoaded = true;
            render();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi đọc khuyến mãi", e);
            progress.setVisibility(View.GONE);
            Toast.makeText(this, "Không tải được khuyến mãi", Toast.LENGTH_SHORT).show();
        });
    }

    private void render() {
        if (!categoriesLoaded || !rulesLoaded) return;
        progress.setVisibility(View.GONE);

        List<AdminPromotionAdapter.Row> rows = new ArrayList<>();
        // quy tắc của loại không còn tồn tại (đã xóa / đổi tên) lên đầu để admin thấy và xóa
        Set<String> known = new HashSet<>(categories);
        List<String> orphans = new ArrayList<>();
        for (String name : working.keySet()) if (!known.contains(name)) orphans.add(name);
        Collections.sort(orphans, (a, b) -> TextNormalizer.normalize(a).compareTo(TextNormalizer.normalize(b)));
        for (String name : orphans) rows.add(new AdminPromotionAdapter.Row(name, working.get(name), true));
        for (String name : categories) rows.add(new AdminPromotionAdapter.Row(name, working.get(name), false));

        adapter.submitList(rows);
        showEmpty(emptyView, rows.isEmpty(), "Chưa có danh mục nào để đặt khuyến mãi.");

        int active = working.size();
        toolbar.setSubtitle(active == 0 ? "Chưa có khuyến mãi nào" : active + " loại đang giảm giá");

        boolean dirty = isDirty();
        btnSave.setEnabled(dirty && !saving);
        btnSave.setText(saving ? "Đang lưu..." : (dirty ? "Lưu khuyến mãi" : "Đã lưu"));
        btnReset.setVisibility(dirty ? View.VISIBLE : View.GONE);
    }

    private boolean isDirty() {
        return !PromotionRules.same(saved, working);
    }

    // ------------------------------------------------------------------ chỉnh quy tắc

    private void showRuleDialog(AdminPromotionAdapter.Row row) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_admin_promotion_rule, null);
        TextInputLayout tilMin = form.findViewById(R.id.til_promo_min);
        TextInputLayout tilPercent = form.findViewById(R.id.til_promo_percent);
        TextInputEditText edtMin = form.findViewById(R.id.edt_promo_min);
        TextInputEditText edtPercent = form.findViewById(R.id.edt_promo_percent);
        TextView preview = form.findViewById(R.id.tv_promo_preview);

        if (row.rule != null) {
            edtMin.setText(String.valueOf(row.rule.minProducts));
            edtPercent.setText(String.valueOf(row.rule.percent));
        }
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                tilMin.setError(null);
                tilPercent.setError(null);
                String[] err = PromotionRules.validate(text(edtMin), text(edtPercent));
                preview.setText(PromotionRules.hasError(err) ? "" : PromotionRules.describe(
                        new PromotionRules.Rule(Integer.parseInt(text(edtMin)), Integer.parseInt(text(edtPercent)))));
            }
        };
        edtMin.addTextChangedListener(watcher);
        edtPercent.addTextChangedListener(watcher);
        watcher.afterTextChanged(null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(row.category)
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Áp dụng", null);
        if (row.rule != null) {
            builder.setNeutralButton(row.orphan ? "Xóa quy tắc" : "Tắt khuyến mãi", (d, w) -> {
                working.remove(row.category);
                render();
            });
        }
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            String[] err = PromotionRules.validate(text(edtMin), text(edtPercent));
            tilMin.setError(err[0]);
            tilPercent.setError(err[1]);
            if (PromotionRules.hasError(err)) return;
            working.put(row.category, new PromotionRules.Rule(
                    Integer.parseInt(text(edtMin)), Integer.parseInt(text(edtPercent))));
            render();
            dialog.dismiss();
        });
    }

    private static String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    // ------------------------------------------------------------------ lưu

    private void save() {
        if (saving || !isDirty()) return;
        saving = true;
        render();
        Map<String, Object> data = new HashMap<>();
        data.put("categoryDiscounts", PromotionRules.toFirestore(working));
        data.put("updatedAt", System.currentTimeMillis());
        db.collection("Promotions").document(PROMOTION_DOC_ID).set(data)
                .addOnSuccessListener(v -> {
                    saving = false;
                    saved = new LinkedHashMap<>(working);
                    Toast.makeText(this, working.isEmpty() ? "Đã tắt toàn bộ khuyến mãi"
                            : "Đã lưu khuyến mãi cho " + working.size() + " loại", Toast.LENGTH_SHORT).show();
                    render();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi lưu khuyến mãi", e);
                    saving = false;
                    Toast.makeText(this, "Lưu khuyến mãi thất bại, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                    render();
                });
    }

    private void attemptClose() {
        if (saving || !isDirty()) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Bỏ thay đổi?")
                .setMessage("Khuyến mãi bạn vừa chỉnh chưa được lưu.")
                .setNegativeButton("Tiếp tục chỉnh", null)
                .setPositiveButton("Bỏ thay đổi", (d, w) -> finish())
                .show();
    }
}
