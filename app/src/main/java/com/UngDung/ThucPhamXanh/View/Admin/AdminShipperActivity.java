package com.UngDung.ThucPhamXanh.View.Admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Models.Shipper;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AdminShipperActivity extends AppCompatActivity {

    private static final String TAG = "AdminShipperActivity";
    private ImageView imgBackShipper;
    private RecyclerView rcvShippers;
    private TextView tvEmptyShippers;
    private AppCompatImageView imageAddShipper;
    private AdminShipperAdapter adapter;
    private ArrayList<Shipper> mlistShipper;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_shipper);

        Init();
        InitWidget();
        Event();
    }

    private void Event() {
        imgBackShipper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        imageAddShipper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddShipperDialog(null);
            }
        });
    }

    private void showAddShipperDialog(Shipper shipperToEdit) {
        Dialog dialog = new Dialog(AdminShipperActivity.this);
        dialog.setContentView(R.layout.dialog_add_shipper);
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        EditText edtUsername = dialog.findViewById(R.id.edt_shipper_username);
        EditText edtPassword = dialog.findViewById(R.id.edt_shipper_password);
        EditText edtName = dialog.findViewById(R.id.edt_shipper_name);
        EditText edtPhone = dialog.findViewById(R.id.edt_shipper_phone);
        Button btnSave = dialog.findViewById(R.id.btn_save_shipper);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel_shipper);
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        
        dialog.setCanceledOnTouchOutside(true);

        // Nếu là edit mode, điền dữ liệu
        if (shipperToEdit != null) {
            tvTitle.setText("Sửa thông tin shipper");
            edtUsername.setText(shipperToEdit.getUsername());
            edtUsername.setEnabled(false); // Không cho sửa username
            edtPassword.setText(shipperToEdit.getPassword());
            edtName.setText(shipperToEdit.getName());
            edtPhone.setText(shipperToEdit.getPhone());
            btnSave.setText("Cập nhật");
        } else {
            tvTitle.setText("Thêm shipper mới");
            edtUsername.setEnabled(true);
            btnSave.setText("Thêm");
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = edtUsername.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();
                String name = edtName.getText().toString().trim();
                String phone = edtPhone.getText().toString().trim();

                if (username.isEmpty()) {
                    edtUsername.setError("Vui lòng nhập username");
                    edtUsername.requestFocus();
                    return;
                }

                if (password.isEmpty()) {
                    edtPassword.setError("Vui lòng nhập mật khẩu");
                    edtPassword.requestFocus();
                    return;
                }

                if (password.length() < 6) {
                    edtPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
                    edtPassword.requestFocus();
                    return;
                }

                if (name.isEmpty()) {
                    edtName.setError("Vui lòng nhập tên");
                    edtName.requestFocus();
                    return;
                }

                if (phone.isEmpty()) {
                    edtPhone.setError("Vui lòng nhập số điện thoại");
                    edtPhone.requestFocus();
                    return;
                }

                // Lưu vào Firestore
                Map<String, Object> shipperData = new HashMap<>();
                shipperData.put("username", username);
                shipperData.put("password", password);
                shipperData.put("name", name);
                shipperData.put("phone", phone);
                shipperData.put("createdAt", System.currentTimeMillis());

                if (shipperToEdit != null) {
                    // Update existing shipper
                    db.collection("Shipper")
                            .whereEqualTo("username", username)
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    if (queryDocumentSnapshots.size() > 0) {
                                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                                        db.collection("Shipper")
                                                .document(docId)
                                                .update(shipperData)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Toast.makeText(AdminShipperActivity.this, "Cập nhật shipper thành công", Toast.LENGTH_SHORT).show();
                                                        dialog.cancel();
                                                        LoadShippers(); // Reload danh sách
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.e(TAG, "Lỗi cập nhật shipper: " + e.getMessage());
                                                        Toast.makeText(AdminShipperActivity.this, "Lỗi cập nhật shipper", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            });
                } else {
                    // Check if username already exists
                    db.collection("Shipper")
                            .whereEqualTo("username", username)
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    if (queryDocumentSnapshots.size() > 0) {
                                        Toast.makeText(AdminShipperActivity.this, "Username đã tồn tại", Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Add new shipper
                                        db.collection("Shipper")
                                                .add(shipperData)
                                                .addOnSuccessListener(new OnSuccessListener<com.google.firebase.firestore.DocumentReference>() {
                                                    @Override
                                                    public void onSuccess(com.google.firebase.firestore.DocumentReference documentReference) {
                                                        Toast.makeText(AdminShipperActivity.this, "Thêm shipper thành công", Toast.LENGTH_SHORT).show();
                                                        dialog.cancel();
                                                        LoadShippers(); // Reload danh sách
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.e(TAG, "Lỗi thêm shipper: " + e.getMessage());
                                                        Toast.makeText(AdminShipperActivity.this, "Lỗi thêm shipper", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            });
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
    }

    private void Init() {
        mlistShipper = new ArrayList<>();
    }

    private void LoadShippers() {
        if (mlistShipper == null) {
            mlistShipper = new ArrayList<>();
        }
        mlistShipper.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateEmptyState();
        
        db.collection("Shipper")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        mlistShipper.clear();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String username = doc.getString("username");
                            String password = doc.getString("password");
                            String name = doc.getString("name");
                            String phone = doc.getString("phone");
                            
                            if (username != null && !username.isEmpty()) {
                                Shipper shipper = new Shipper(username, password, name, phone);
                                mlistShipper.add(shipper);
                            }
                        }
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                        updateEmptyState();
                        Log.d(TAG, "✅ Đã load " + mlistShipper.size() + " shipper");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "❌ Lỗi load shipper: " + e.getMessage());
                        Toast.makeText(AdminShipperActivity.this, "Lỗi tải danh sách shipper", Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                    }
                });
    }
    
    private void updateEmptyState() {
        if (tvEmptyShippers != null && rcvShippers != null) {
            if (mlistShipper == null || mlistShipper.isEmpty()) {
                tvEmptyShippers.setVisibility(View.VISIBLE);
                rcvShippers.setVisibility(View.GONE);
            } else {
                tvEmptyShippers.setVisibility(View.GONE);
                rcvShippers.setVisibility(View.VISIBLE);
            }
        }
    }

    private void InitWidget() {
        imgBackShipper = findViewById(R.id.img_back_shipper_admin);
        rcvShippers = findViewById(R.id.rcv_shippers);
        tvEmptyShippers = findViewById(R.id.tv_empty_shippers);
        imageAddShipper = findViewById(R.id.image_add_shipper);

        adapter = new AdminShipperAdapter(AdminShipperActivity.this, mlistShipper, new IClickCTHD() {
            @Override
            public void onClickCTHD(int pos) {
                if (pos >= 0 && pos < mlistShipper.size()) {
                    Shipper shipper = mlistShipper.get(pos);
                    showShipperOptionsDialog(shipper, pos);
                }
            }
        });
        rcvShippers.setLayoutManager(new GridLayoutManager(AdminShipperActivity.this, 2));
        rcvShippers.setAdapter(adapter);
        
        // Load dữ liệu sau khi adapter đã được setup
        LoadShippers();
    }

    private void showShipperOptionsDialog(Shipper shipper, int position) {
        Dialog dialog = new Dialog(AdminShipperActivity.this);
        dialog.setContentView(R.layout.dialog_info_shipper);
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        TextView tvUsername = dialog.findViewById(R.id.tv_shipper_username_info);
        TextView tvName = dialog.findViewById(R.id.tv_shipper_name_info);
        TextView tvPhone = dialog.findViewById(R.id.tv_shipper_phone_info);
        Button btnEdit = dialog.findViewById(R.id.btn_edit_shipper);
        Button btnDelete = dialog.findViewById(R.id.btn_delete_shipper);
        ImageView btnCancel = dialog.findViewById(R.id.btn_cancel_shipper_info);
        
        dialog.setCanceledOnTouchOutside(true);

        tvUsername.setText("Username: " + shipper.getUsername());
        tvName.setText("Tên: " + (shipper.getName() != null && !shipper.getName().isEmpty() ? shipper.getName() : "Chưa có"));
        tvPhone.setText("SĐT: " + (shipper.getPhone() != null && !shipper.getPhone().isEmpty() ? shipper.getPhone() : "Chưa có"));

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                showAddShipperDialog(shipper);
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                new AlertDialog.Builder(AdminShipperActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa shipper " + shipper.getUsername() + "?")
                        .setPositiveButton("Xóa", new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialogInterface, int which) {
                                deleteShipper(shipper.getUsername(), position);
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
    }

    private void deleteShipper(String username, int position) {
        db.collection("Shipper")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() > 0) {
                            String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                            db.collection("Shipper")
                                    .document(docId)
                                    .delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(AdminShipperActivity.this, "Đã xóa shipper", Toast.LENGTH_SHORT).show();
                                            mlistShipper.remove(position);
                                            adapter.notifyDataSetChanged();
                                            updateEmptyState();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(TAG, "Lỗi xóa shipper: " + e.getMessage());
                                            Toast.makeText(AdminShipperActivity.this, "Lỗi xóa shipper", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Lỗi tìm shipper: " + e.getMessage());
                        Toast.makeText(AdminShipperActivity.this, "Lỗi xóa shipper", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

