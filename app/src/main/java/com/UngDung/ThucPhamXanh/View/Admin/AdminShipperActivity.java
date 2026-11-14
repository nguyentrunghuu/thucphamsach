package com.UngDung.ThucPhamXanh.View.Admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.FirestoreMapper;
import com.UngDung.ThucPhamXanh.Helper.ShipperForm;
import com.UngDung.ThucPhamXanh.Models.OrderStatus;
import com.UngDung.ThucPhamXanh.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shipper trang admin. Đọc (gỡ ở onStop): 1 listener "Shipper" + 1 listener "HoaDon" để biết mỗi shipper
 * đang giao / đã giao bao nhiêu đơn. Thêm / sửa / xóa theo ID document (không còn xóa theo vị trí cũ).
 * Sửa: để trống mật khẩu = giữ mật khẩu cũ. Không xóa shipper đang có đơn "Đang giao".
 */
public class AdminShipperActivity extends BaseAdminActivity {

    private static final String TAG = "AdminShipperActivity";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private MaterialToolbar toolbar;
    private RecyclerView rcv;
    private View emptyView, progress;
    private AdminShipperAdapter adapter;

    private final List<DocumentSnapshot> shipperDocs = new ArrayList<>();
    private final Map<String, int[]> counts = new HashMap<>(); // shipperId -> {đang giao, đã giao}
    private boolean shippersLoaded;
    private List<AdminShipperAdapter.Row> rows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_shipper);
        toolbar = setupAdminToolbar("Shipper", null);

        rcv = findViewById(R.id.rcv_shippers);
        emptyView = findViewById(R.id.admin_empty);
        progress = findViewById(R.id.progress_shippers);
        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add_shipper);
        applySystemBarInsets(null, rcv);
        applySystemBarInsets(null, findViewById(R.id.fab_container_shipper));

        adapter = new AdminShipperAdapter(this::showSheet);
        rcv.setLayoutManager(new LinearLayoutManager(this));
        rcv.setAdapter(adapter);
        fab.setOnClickListener(v -> showForm(null));
    }

    @Override
    protected void onStartListening() {
        listen(db.collection("Shipper").addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.e(TAG, "Lỗi đọc Shipper", e);
                progress.setVisibility(View.GONE);
                Toast.makeText(this, "Không tải được danh sách shipper", Toast.LENGTH_SHORT).show();
                return;
            }
            if (snap == null) return;
            shipperDocs.clear();
            shipperDocs.addAll(snap.getDocuments());
            shippersLoaded = true;
            rebuild();
        }));
        listen(db.collection("HoaDon").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            counts.clear();
            for (DocumentSnapshot d : snap.getDocuments()) {
                String sid = FirestoreMapper.str(d, "shipperId").trim();
                if (sid.isEmpty()) continue;
                long st = FirestoreMapper.num(d, "trangthai");
                int[] c = counts.get(sid);
                if (c == null) {
                    c = new int[2];
                    counts.put(sid, c);
                }
                if (st == OrderStatus.DANG_GIAO) c[0]++;
                else if (st == OrderStatus.DA_GIAO) c[1]++;
            }
            rebuild();
        }));
    }

    private void rebuild() {
        if (!shippersLoaded) return;
        List<AdminShipperAdapter.Row> list = new ArrayList<>();
        for (DocumentSnapshot d : shipperDocs) {
            String username = FirestoreMapper.str(d, "username").trim();
            if (username.isEmpty()) continue;
            int[] c = counts.get(username);
            list.add(new AdminShipperAdapter.Row(d.getId(), username, FirestoreMapper.str(d, "name"),
                    FirestoreMapper.str(d, "phone"), c == null ? 0 : c[0], c == null ? 0 : c[1]));
        }
        Collections.sort(list, (a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));
        rows = list;
        adapter.submitList(list);
        progress.setVisibility(View.GONE);
        showEmpty(emptyView, list.isEmpty(), "Chưa có shipper nào.\nBấm \"Thêm shipper\" để tạo tài khoản giao hàng.");
        toolbar.setSubtitle(list.isEmpty() ? null : list.size() + " shipper");
    }

    // ------------------------------------------------------------------ chi tiết

    private void showSheet(AdminShipperAdapter.Row r) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this).inflate(R.layout.sheet_admin_shipper, null);
        dialog.setContentView(v);

        ((TextView) v.findViewById(R.id.tv_sheet_shipper_name)).setText(r.displayName());
        ((TextView) v.findViewById(R.id.tv_sheet_shipper_user)).setText("Tên đăng nhập: " + r.username);
        ((TextView) v.findViewById(R.id.tv_sheet_delivering)).setText(String.valueOf(r.delivering));
        ((TextView) v.findViewById(R.id.tv_sheet_delivered)).setText(String.valueOf(r.delivered));

        LinearLayout box = v.findViewById(R.id.box_shipper_rows);
        addRow(box, "Số điện thoại", r.phone);

        MaterialButton btnCall = v.findViewById(R.id.btn_shipper_call);
        String phone = ShipperForm.cleanPhone(r.phone);
        if (phone.isEmpty()) {
            btnCall.setEnabled(false);
        } else {
            btnCall.setOnClickListener(x -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))));
        }
        v.findViewById(R.id.btn_shipper_edit).setOnClickListener(x -> {
            dialog.dismiss();
            showForm(r);
        });
        v.findViewById(R.id.btn_shipper_delete).setOnClickListener(x -> {
            dialog.dismiss();
            askDelete(r);
        });
        dialog.show();
    }

    private void addRow(LinearLayout box, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        View row = LayoutInflater.from(this).inflate(R.layout.item_admin_kv, box, false);
        ((TextView) row.findViewById(R.id.tv_kv_label)).setText(label);
        ((TextView) row.findViewById(R.id.tv_kv_value)).setText(value.trim());
        box.addView(row);
    }

    // ------------------------------------------------------------------ thêm / sửa

    /** editing == null → thêm mới. */
    private void showForm(AdminShipperAdapter.Row editing) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_admin_shipper_form, null);
        TextInputLayout tilUser = form.findViewById(R.id.til_shipper_username);
        TextInputLayout tilPass = form.findViewById(R.id.til_shipper_password);
        TextInputLayout tilName = form.findViewById(R.id.til_shipper_name);
        TextInputLayout tilPhone = form.findViewById(R.id.til_shipper_phone);
        TextInputEditText edtUser = form.findViewById(R.id.edt_shipper_username);
        TextInputEditText edtPass = form.findViewById(R.id.edt_shipper_password);
        TextInputEditText edtName = form.findViewById(R.id.edt_shipper_name);
        TextInputEditText edtPhone = form.findViewById(R.id.edt_shipper_phone);

        final boolean isEdit = editing != null;
        if (isEdit) {
            edtUser.setText(editing.username);
            edtUser.setEnabled(false); // tên đăng nhập là khóa để shipper đăng nhập và gắn đơn → không đổi
            edtName.setText(editing.name);
            edtPhone.setText(editing.phone);
            tilPass.setHint("Mật khẩu mới (để trống nếu không đổi)");
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(isEdit ? "Sửa shipper" : "Thêm shipper")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton(isEdit ? "Lưu" : "Thêm", null)
                .create();
        dialog.show();

        final android.widget.Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnSave.setOnClickListener(x -> {
            tilUser.setError(null);
            tilPass.setError(null);
            tilName.setError(null);
            tilPhone.setError(null);

            String username = ShipperForm.cleanUsername(text(edtUser));
            String password = text(edtPass);
            String name = text(edtName);
            String phone = text(edtPhone);

            List<String> existing = new ArrayList<>();
            for (AdminShipperAdapter.Row r : rows) existing.add(r.username);
            ShipperForm.Errors err = ShipperForm.validate(username, password, name, phone, existing,
                    isEdit ? editing.username : null);
            tilUser.setError(err.username);
            tilPass.setError(err.password);
            tilName.setError(err.name);
            tilPhone.setError(err.phone);
            if (err.any()) return;

            btnSave.setEnabled(false);
            if (isEdit) {
                Map<String, Object> update = new HashMap<>();
                update.put("name", name.trim());
                update.put("phone", ShipperForm.cleanPhone(phone));
                if (!password.isEmpty()) update.put("password", password);
                db.collection("Shipper").document(editing.docId).update(update)
                        .addOnSuccessListener(v -> {
                            Toast.makeText(this, "Đã cập nhật shipper", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> onSaveFailed(e, btnSave));
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("username", username);
                data.put("password", password);
                data.put("name", name.trim());
                data.put("phone", ShipperForm.cleanPhone(phone));
                data.put("createdAt", System.currentTimeMillis());
                // kiểm tra lại trên máy chủ phòng khi có người vừa tạo cùng tên đăng nhập
                db.collection("Shipper").whereEqualTo("username", username).get()
                        .addOnSuccessListener(found -> {
                            if (!found.isEmpty()) {
                                btnSave.setEnabled(true);
                                tilUser.setError("Tên đăng nhập đã tồn tại");
                                return;
                            }
                            db.collection("Shipper").add(data)
                                    .addOnSuccessListener(ref -> {
                                        Toast.makeText(this, "Đã thêm shipper", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> onSaveFailed(e, btnSave));
                        })
                        .addOnFailureListener(e -> onSaveFailed(e, btnSave));
            }
        });
    }

    private void onSaveFailed(Exception e, android.widget.Button btn) {
        Log.e(TAG, "Lưu shipper thất bại", e);
        btn.setEnabled(true);
        Toast.makeText(this, "Lưu thất bại, vui lòng thử lại", Toast.LENGTH_SHORT).show();
    }

    private static String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    // ------------------------------------------------------------------ xóa

    private void askDelete(AdminShipperAdapter.Row r) {
        if (r.delivering > 0) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Không thể xóa shipper")
                    .setMessage(r.displayName() + " đang giao " + r.delivering
                            + " đơn. Hãy đợi giao xong hoặc gán các đơn đó cho shipper khác trước.")
                    .setPositiveButton("Đã hiểu", null)
                    .show();
            return;
        }
        confirm("Xóa shipper?", "Bạn có chắc muốn xóa shipper " + r.displayName() + " (" + r.username
                + ")? Họ sẽ không đăng nhập được nữa.", "Xóa", () ->
                db.collection("Shipper").document(r.docId).delete()
                        .addOnSuccessListener(v -> Toast.makeText(this, "Đã xóa shipper", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Lỗi xóa shipper", e);
                            Toast.makeText(this, "Xóa shipper thất bại, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                        }));
    }
}
