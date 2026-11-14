package com.UngDung.ThucPhamXanh.View.Admin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.AdminOrder;
import com.UngDung.ThucPhamXanh.Helper.AdminUserCreator;
import com.UngDung.ThucPhamXanh.Helper.AdminUsers;
import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.MoneyUtil;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Khách hàng trang admin. Đọc (gỡ ở onStop): IDUser (uid + email), nhóm collection "Profile" (tên, SĐT, địa chỉ…
 * — 1 listener thay cho N lần đọc theo từng khách), HoaDon (thống kê số đơn / chi tiêu theo UID).
 * Tìm kiếm và sắp xếp làm trên máy bằng {@link AdminUsers}.
 */
public class AdminUsersActivity extends BaseAdminActivity {

    private static final String TAG = "AdminUsersActivity";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private MaterialToolbar toolbar;
    private RecyclerView rcv;
    private TextView tvResult;
    private View emptyView, progress;
    private AdminUsersAdapter adapter;

    private String query = "";
    private int sort = AdminUsers.SORT_NAME;

    private final Map<String, String> emails = new LinkedHashMap<>();        // uid -> email (theo IDUser)
    private final Map<String, DocumentSnapshot> profiles = new HashMap<>();  // uid -> Profile
    private final List<AdminOrder> orders = new ArrayList<>();
    private boolean usersLoaded;
    private List<AdminUsers.Row> allRows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);
        toolbar = setupAdminToolbar("Khách hàng", null);

        rcv = findViewById(R.id.rcv_users);
        tvResult = findViewById(R.id.tv_user_result);
        emptyView = findViewById(R.id.admin_empty);
        progress = findViewById(R.id.progress_users);
        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add_user);
        applySystemBarInsets(null, rcv);
        applySystemBarInsets(null, findViewById(R.id.fab_container_users));

        adapter = new AdminUsersAdapter(this::showUserSheet);
        rcv.setLayoutManager(new LinearLayoutManager(this));
        rcv.setAdapter(adapter);

        TextInputEditText search = findViewById(R.id.edt_search_user);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                query = s == null ? "" : s.toString();
                render(true);
            }
        });
        ChipGroup sortGroup = findViewById(R.id.chip_group_user_sort);
        sortGroup.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.chip_sort_spent) sort = AdminUsers.SORT_SPENT;
            else if (id == R.id.chip_sort_orders) sort = AdminUsers.SORT_ORDERS;
            else sort = AdminUsers.SORT_NAME;
            render(true);
        });
        fab.setOnClickListener(v -> showAddDialog());
    }

    @Override
    protected void onStartListening() {
        listen(db.collection("IDUser").addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.e(TAG, "Lỗi đọc IDUser", e);
                progress.setVisibility(View.GONE);
                Toast.makeText(this, "Không tải được danh sách khách hàng", Toast.LENGTH_SHORT).show();
                return;
            }
            if (snap == null) return;
            emails.clear();
            for (DocumentSnapshot d : snap.getDocuments()) {
                String uid = FirestoreMapper.str(d, "iduser");
                if (uid.isEmpty() || emails.containsKey(uid)) continue; // IDUser có thể ghi trùng một uid
                emails.put(uid, FirestoreMapper.str(d, "email"));
            }
            usersLoaded = true;
            rebuild();
        }));
        listen(db.collectionGroup("Profile").addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.e(TAG, "Lỗi đọc Profile", e);
                return;
            }
            if (snap == null) return;
            profiles.clear();
            for (DocumentSnapshot d : snap.getDocuments()) {
                // User/{uid}/Profile/{id}: uid là document cha
                if (d.getReference().getParent().getParent() == null) continue;
                String uid = d.getReference().getParent().getParent().getId();
                if (!profiles.containsKey(uid)) profiles.put(uid, d);
            }
            rebuild();
        }));
        listen(db.collection("HoaDon").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            orders.clear();
            for (DocumentSnapshot d : snap.getDocuments()) orders.add(FirestoreMapper.toAdminOrder(d));
            rebuild();
        }));
    }

    private void rebuild() {
        if (!usersLoaded) return;
        List<AdminUsers.Row> rows = new ArrayList<>();
        for (Map.Entry<String, String> en : emails.entrySet()) {
            AdminUsers.Row r = new AdminUsers.Row();
            r.uid = en.getKey();
            r.email = en.getValue();
            DocumentSnapshot p = profiles.get(r.uid);
            if (p != null) {
                r.name = FirestoreMapper.str(p, "hoten");
                r.phone = FirestoreMapper.str(p, "sdt");
                r.address = FirestoreMapper.str(p, "diachi");
                r.birth = FirestoreMapper.str(p, "ngaysinh");
                r.gender = FirestoreMapper.str(p, "gioitinh");
                r.avatar = FirestoreMapper.str(p, "avatar");
            }
            rows.add(r);
        }
        AdminUsers.applyStats(rows, AdminUsers.statsByUser(orders));
        allRows = rows;
        render(false);
    }

    private void render(boolean scrollTop) {
        progress.setVisibility(View.GONE);
        List<AdminUsers.Row> shown = AdminUsers.filterAndSort(allRows, query, sort);
        adapter.submitList(shown, () -> {
            if (scrollTop) rcv.scrollToPosition(0);
        });
        boolean searching = !query.trim().isEmpty();
        showEmpty(emptyView, shown.isEmpty(),
                allRows.isEmpty() ? "Chưa có khách hàng nào.\nBấm \"Thêm khách hàng\" để tạo tài khoản."
                        : "Không có khách hàng nào khớp \"" + query.trim() + "\"");
        tvResult.setText(searching ? shown.size() + " / " + allRows.size() + " khách hàng"
                : allRows.size() + " khách hàng");
        toolbar.setSubtitle(allRows.isEmpty() ? null : allRows.size() + " tài khoản");
    }

    // ------------------------------------------------------------------ chi tiết

    private void showUserSheet(AdminUsers.Row u) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this).inflate(R.layout.sheet_admin_user, null);
        dialog.setContentView(v);

        CircleImageView avatar = v.findViewById(R.id.cir_sheet_user);
        ((TextView) v.findViewById(R.id.tv_sheet_user_name)).setText(u.displayName());
        ((TextView) v.findViewById(R.id.tv_sheet_user_email)).setText(u.email.isEmpty() ? "(chưa có email)" : u.email);
        ((TextView) v.findViewById(R.id.tv_sheet_orders)).setText(String.valueOf(u.orders));
        ((TextView) v.findViewById(R.id.tv_sheet_spent)).setText(MoneyUtil.formatVnd(u.spent));
        String a = u.avatar == null ? "" : u.avatar.trim();
        if (a.startsWith("http")) Picasso.get().load(a).placeholder(R.drawable.ic_user).error(R.drawable.ic_user).into(avatar);

        LinearLayout box = v.findViewById(R.id.box_user_rows);
        addRow(box, "Số điện thoại", u.phone);
        addRow(box, "Địa chỉ", u.address);
        addRow(box, "Ngày sinh", u.birth);
        addRow(box, "Giới tính", u.gender);
        addRow(box, "Mã người dùng", u.uid);
        if (box.getChildCount() == 0) {
            addRow(box, "Hồ sơ", "Khách chưa cập nhật thông tin cá nhân");
        }

        MaterialButton btnCall = v.findViewById(R.id.btn_user_call);
        String phone = u.phone == null ? "" : u.phone.replaceAll("[^0-9+]", "");
        if (phone.isEmpty()) {
            btnCall.setEnabled(false);
        } else {
            btnCall.setOnClickListener(x -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))));
        }
        MaterialButton btnCopy = v.findViewById(R.id.btn_user_copy);
        if (u.email.isEmpty()) {
            btnCopy.setEnabled(false);
        } else {
            btnCopy.setOnClickListener(x -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("Email", u.email));
                    Toast.makeText(this, "Đã sao chép email", Toast.LENGTH_SHORT).show();
                }
            });
        }
        dialog.show();
    }

    private void addRow(LinearLayout box, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        View row = LayoutInflater.from(this).inflate(R.layout.item_admin_kv, box, false);
        ((TextView) row.findViewById(R.id.tv_kv_label)).setText(label);
        ((TextView) row.findViewById(R.id.tv_kv_value)).setText(value.trim());
        box.addView(row);
    }

    // ------------------------------------------------------------------ thêm khách hàng

    private void showAddDialog() {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_admin_add_user, null);
        TextInputLayout tilEmail = form.findViewById(R.id.til_add_email);
        TextInputLayout tilPass = form.findViewById(R.id.til_add_pass);
        TextInputLayout tilConfirm = form.findViewById(R.id.til_add_confirm);
        TextInputEditText edtEmail = form.findViewById(R.id.edt_add_email_users);
        TextInputEditText edtPass = form.findViewById(R.id.edt_add_pass_users);
        TextInputEditText edtConfirm = form.findViewById(R.id.edt_add_confirm_users);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm khách hàng")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Tạo tài khoản", null) // gắn sau để không tự đóng khi có lỗi
                .create();
        dialog.show();

        final android.widget.Button btnCreate = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
        btnCreate.setOnClickListener(x -> {
            tilEmail.setError(null);
            tilPass.setError(null);
            tilConfirm.setError(null);
            String email = text(edtEmail);
            String pass = text(edtPass);
            String confirm = text(edtConfirm);
            if (email.isEmpty()) { tilEmail.setError("Vui lòng nhập email"); return; }
            if (!AdminUserCreator.isEmailValid(email)) { tilEmail.setError("Email không đúng định dạng"); return; }
            if (pass.length() < 6) { tilPass.setError("Mật khẩu tối thiểu 6 ký tự"); return; }
            if (!pass.equals(confirm)) { tilConfirm.setError("Mật khẩu nhập lại không khớp"); return; }

            btnCreate.setEnabled(false);
            btnCreate.setText("Đang tạo...");
            AdminUserCreator.create(this, email, pass, new AdminUserCreator.Callback() {
                @Override
                public void onCreated(String createdEmail) {
                    if (isFinishing()) return;
                    Toast.makeText(AdminUsersActivity.this, "Đã tạo tài khoản " + createdEmail, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onError(String message) {
                    if (isFinishing()) return;
                    btnCreate.setEnabled(true);
                    btnCreate.setText("Tạo tài khoản");
                    tilEmail.setError(message.startsWith("Email") ? message : null);
                    if (!message.startsWith("Email")) Toast.makeText(AdminUsersActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private static String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}
