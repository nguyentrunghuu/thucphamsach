package com.UngDung.ThucPhamXanh.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.UngDung.ThucPhamXanh.Helper.ImageUploadHelper;
import com.UngDung.ThucPhamXanh.MainActivity;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.MapLocationOSMActivity;
import com.UngDung.ThucPhamXanh.View.SignInActivity;
import com.UngDung.ThucPhamXanh.ultil.NetworkUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private Uri mUri;
    private ProgressDialog progressDialog;
    private MainActivity mMainActivity;

    private View view;
    private CircleImageView imgAvatar;
    private EditText edtFullName, edtPhoneNumber, edtDate;
    private Button btnSelectAddressProfile;
    private TextView tvSelectedAddressProfile, tvEmail;
    private RadioButton rdoNam, rdoNu;
    private RadioGroup rdoGroup;
    private Button btnUpdateprofile;
    private CardView layoutLogout;
    private CardView layoutLogoutBottom;
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    // ActivityResultLauncher for MapLocationActivity
    private ActivityResultLauncher<Intent> mapLocationLauncher;
    
    // ActivityResultLaunchers cho chọn ảnh
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private Uri cameraImageUri;

    private String key = "";

    private boolean reloadData = false;

    DatabaseReference reference;
    FirebaseUser firebaseUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);
        com.UngDung.ThucPhamXanh.View.ThemePicker.bind(view);
        InitWidget();
        Init();
        initMapLocationLauncher();
        initImageLaunchers();
        // Log.e("ep", mainActivity.Password());

        if (NetworkUtil.isNetworkConnected(getContext())) {
            reloadData = true;
            LoadInfo();
            setUserInformation();
            Event();
        } else {
            reloadData = false;
        }
        getFragmentManager().beginTransaction().detach(this).attach(this).commit();
        Log.d("fmt", "onCreateView");
        reload();
        return view;
    }

    private void initMapLocationLauncher() {
        mapLocationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        String selectedAddress = result.getData().getStringExtra("selected_address");
                        double latitude = result.getData().getDoubleExtra("latitude", 0.0);
                        double longitude = result.getData().getDoubleExtra("longitude", 0.0);

                        if (selectedAddress != null && !selectedAddress.isEmpty()) {
                            tvSelectedAddressProfile.setText(selectedAddress);
                            tvSelectedAddressProfile.setVisibility(View.VISIBLE);
                            btnSelectAddressProfile.setText("Thay đổi địa chỉ");
                        }
                    }
                });
    }
    
    private void initImageLaunchers() {
        // Camera launcher
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK) {
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
            java.io.File tempFile = new java.io.File(getActivity().getCacheDir(), "temp_camera_image_" + System.currentTimeMillis() + ".jpg");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
            // Sử dụng FileProvider để tạo URI an toàn
            return androidx.core.content.FileProvider.getUriForFile(
                getActivity(),
                getActivity().getPackageName() + ".fileprovider",
                tempFile
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    void reload() {
        if (reloadData) {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            Fragment currentFragment = fragmentManager.findFragmentByTag("main_fragment");
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if (currentFragment != null) {
                fragmentTransaction.detach(currentFragment);
                fragmentTransaction.attach(currentFragment);
                fragmentTransaction.commit();
            }
        } else {
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            Fragment currentFragment = fragmentManager.findFragmentByTag("main_fragment");
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if (currentFragment != null) {
                fragmentTransaction.detach(currentFragment);
                fragmentTransaction.attach(currentFragment);
                fragmentTransaction.commit();
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("fmt", "onResume");
        if (!reloadData) {
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
            onAttach(getContext());

            reloadData = false;
        } else {
            reloadData = true;
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
            onAttach(getContext());
        }
    }

    // @Override
    // public void onAttach(@NonNull @NotNull Context context) {
    // super.onAttach(context);
    // Log.d("fmt", "onAttach");
    // if (NetworkUtil.isNetworkConnected(getContext())){
    // reloadData = true;
    // LoadInfo();
    // setUserInformation();
    // Event();
    // getFragmentManager().beginTransaction().detach(this).attach(this).commit();
    // } else {
    // reloadData = false;
    // getFragmentManager().beginTransaction().detach(this).attach(this).commit();
    // }
    // }

    private void LoadInfo() {
        Log.d("ProfileFragment", "LoadInfo: Starting to load user data...");

        firestore.collection("User").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("Profile")
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
                        Log.d("ProfileFragment", "LoadInfo: Query successful, size: " + queryDocumentSnapshots.size());

                        if (queryDocumentSnapshots.size() > 0) {
                            DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                            if (documentSnapshot != null) {
                                key = documentSnapshot.getId();
                                Log.d("ProfileFragment", "LoadInfo: Document ID: " + key);

                                try {
                                    // Load address
                                    String savedAddress = documentSnapshot.getString("diachi");
                                    if (savedAddress != null && savedAddress.length() > 0) {
                                        tvSelectedAddressProfile.setText(savedAddress);
                                        tvSelectedAddressProfile.setVisibility(View.VISIBLE);
                                        btnSelectAddressProfile.setText("Thay đổi địa chỉ");
                                        Log.d("ProfileFragment", "LoadInfo: Address loaded: " + savedAddress);
                                    } else {
                                        Log.d("ProfileFragment", "LoadInfo: No address found");
                                    }

                                    // Load full name
                                    String hoten = documentSnapshot.getString("hoten");
                                    if (hoten != null && hoten.length() > 0) {
                                        edtFullName.setText(hoten);
                                        Log.d("ProfileFragment", "LoadInfo: Full name loaded: " + hoten);
                                    } else {
                                        edtFullName.setText("");
                                        Log.d("ProfileFragment", "LoadInfo: No full name found");
                                    }

                                    // Load phone number
                                    String sdt = documentSnapshot.getString("sdt");
                                    if (sdt != null && sdt.length() > 0) {
                                        edtPhoneNumber.setText(sdt);
                                        Log.d("ProfileFragment", "LoadInfo: Phone loaded: " + sdt);
                                    } else {
                                        edtPhoneNumber.setText("");
                                        Log.d("ProfileFragment", "LoadInfo: No phone found");
                                    }

                                    // Load birth date
                                    String ngaysinh = documentSnapshot.getString("ngaysinh");
                                    if (ngaysinh != null && ngaysinh.length() > 0) {
                                        edtDate.setText(ngaysinh);
                                        Log.d("ProfileFragment", "LoadInfo: Birth date loaded: " + ngaysinh);
                                    } else {
                                        edtDate.setText("");
                                        Log.d("ProfileFragment", "LoadInfo: No birth date found");
                                    }

                                    // Load gender
                                    String sex = documentSnapshot.getString("gioitinh");
                                    if (sex != null && sex.length() > 0) {
                                        if (sex.equals("Nam")) {
                                            rdoNam.setChecked(true);
                                        } else {
                                            rdoNu.setChecked(true);
                                        }
                                        Log.d("ProfileFragment", "LoadInfo: Gender loaded: " + sex);
                                    } else {
                                        rdoGroup.clearCheck();
                                        Log.d("ProfileFragment", "LoadInfo: No gender found");
                                    }

                                    // Load avatar
                                    String avatar = documentSnapshot.getString("avatar");
                                    if (avatar != null && avatar.length() > 0) {
                                        Picasso.get().load(avatar.trim()).into(imgAvatar);
                                        Log.d("ProfileFragment", "LoadInfo: Avatar loaded: " + avatar);
                                    } else {
                                        Log.d("ProfileFragment", "LoadInfo: No avatar found");
                                    }

                                    Log.d("ProfileFragment", "LoadInfo: All data loaded successfully");

                                } catch (Exception e) {
                                    Log.e("ProfileFragment", "LoadInfo: Error loading data: " + e.getMessage());
                                    e.printStackTrace();
                                }

                            } else {
                                Log.d("ProfileFragment", "LoadInfo: Document snapshot is null, creating new profile");
                                createNewProfile();
                            }
                        } else {
                            Log.d("ProfileFragment", "LoadInfo: No documents found, creating new profile");
                            createNewProfile();
                        }
                    }
                }).addOnFailureListener(e -> {
                    Log.e("ProfileFragment", "LoadInfo: Failed to load data: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    private void createNewProfile() {
        Log.d("ProfileFragment", "createNewProfile: Creating new profile...");
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("diachi", "");
        hashMap.put("hoten", "");
        hashMap.put("sdt", "");
        hashMap.put("ngaysinh", "");
        hashMap.put("gioitinh", "");
        hashMap.put("avatar", "");
        hashMap.put("iduser", FirebaseAuth.getInstance().getCurrentUser().getUid());

        firestore.collection("User")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("Profile").add(hashMap)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(@NonNull DocumentReference documentReference) {
                        key = documentReference.getId();
                        Log.d("ProfileFragment", "createNewProfile: New profile created with ID: " + key);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileFragment", "createNewProfile: Failed to create profile: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    // Hàm set thông tin người dùng hiện tại khi click vào My Profile
    private void setUserInformation() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        edtFullName.setText(user.getDisplayName());
        tvEmail.setText(user.getEmail());
        Glide.with(getActivity()).load(user.getPhotoUrl()).error(R.drawable.ic_avatar_default).into(imgAvatar);
    }

    private void Init() {
        progressDialog = new ProgressDialog(getActivity());
        mMainActivity = (MainActivity) getActivity();
    }

    private void Event() {

        imgAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImageSourceDialog();
            }
        });

        edtDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = Calendar.getInstance();
                int ngay = calendar.get(Calendar.DATE);
                int thang = calendar.get(Calendar.MONTH);
                int nam = calendar.get(Calendar.YEAR);
                DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                                // i:năm - i1:tháng - i2:ngày
                                calendar.set(i, i1, i2);
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/YYYY");
                                edtDate.setText(simpleDateFormat.format(calendar.getTime()));
                            }
                        }, nam, thang, ngay);
                datePickerDialog.show();
            }
        });

        // Set click listener for address selection button
        btnSelectAddressProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(getActivity(), MapLocationOSMActivity.class);
                mapLocationLauncher.launch(mapIntent);
            }
        });

        btnUpdateprofile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String strFullName = edtFullName.getText().toString().trim();
                String strAddress = tvSelectedAddressProfile.getText().toString().trim();
                String strSDT = edtPhoneNumber.getText().toString().trim();
                String strDate = edtDate.getText().toString().trim();
                String strSex;
                if (rdoNam.isChecked()) {
                    strSex = "Nam";
                } else {
                    strSex = "Nữ";
                }

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    return;
                }
                progressDialog.show();
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(strFullName)
                        .setPhotoUri(mUri)
                        .build();

                user.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            mMainActivity.setProFile();
                        }
                    }
                });

                Map<String, Object> chinh = new HashMap<>();
                chinh.put("hoten", strFullName);
                chinh.put("diachi", strAddress);
                chinh.put("sdt", strSDT);
                chinh.put("ngaysinh", strDate);
                chinh.put("gioitinh", strSex);

                firestore.collection("User").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .collection("Profile").document(key)
                        .update(chinh).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getContext(), "Cập nhật thông tin thành công", Toast.LENGTH_SHORT)
                                            .show();

                                }

                            }
                        });

                // Import vào Realtime của Firebase
                reference = FirebaseDatabase.getInstance().getReference("Users")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                HashMap<String, Object> map = new HashMap<>();
                map.put("iduser", FirebaseAuth.getInstance().getCurrentUser().getUid());
                map.put("name", strFullName);
                map.put("search", strFullName.toLowerCase());
                reference.updateChildren(map);
            }
        });

        // Logout button ở header
        layoutLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLogoutDialog();
            }
        });

        // Logout button ở bottom
        layoutLogoutBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLogoutDialog();
            }
        });

        tvEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.dialog_profile);
                dialog.show();
                EditText edtNewEmail = dialog.findViewById(R.id.edt_new_email_dialog);
                ImageView imgCancelDialog = dialog.findViewById(R.id.img_cancel_dialog);
                Button btnCapnhatDialog = dialog.findViewById(R.id.btn_capnhat_dialog);

                edtNewEmail.setText(user.getEmail());
                imgCancelDialog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                    }
                });

                btnCapnhatDialog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String strNewEmail = edtNewEmail.getText().toString().trim();
                        progressDialog.show();
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        user.updateEmail(strNewEmail)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            progressDialog.dismiss();
                                            dialog.cancel();
                                            Toast.makeText(getActivity(), "User Email Address Updated",
                                                    Toast.LENGTH_SHORT).show();
                                            mMainActivity.setProFile();
                                        }
                                    }
                                });
                    }
                });
            }
        });

    }

    private void InitWidget() {
        imgAvatar = view.findViewById(R.id.img_avatar);
        edtFullName = view.findViewById(R.id.edt_full_name);
        btnSelectAddressProfile = view.findViewById(R.id.btn_select_address_profile);
        tvSelectedAddressProfile = view.findViewById(R.id.tv_selected_address_profile);
        edtPhoneNumber = view.findViewById(R.id.edt_phone);
        edtDate = view.findViewById(R.id.edt_date);
        rdoNam = view.findViewById(R.id.rdo_nam);
        rdoNu = view.findViewById(R.id.rdo_nu);
        rdoGroup = view.findViewById(R.id.rdo_group);
        tvEmail = view.findViewById(R.id.tv_email_profile);
        btnUpdateprofile = view.findViewById(R.id.btn_update_profile);
        layoutLogout = view.findViewById(R.id.layout_logout);
        layoutLogoutBottom = view.findViewById(R.id.layout_logout_bottom);
    }

    /**
     * Hiển thị dialog xác nhận đăng xuất
     */
    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Thông báo");
        builder.setIcon(R.drawable.icons8_shutdown);
        builder.setMessage("Bạn có thực sự muốn đăng xuất khỏi tài khoản này không?");
        builder.setPositiveButton("Đăng xuất", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Ngừng báo tin nhắn của tài khoản này
                com.UngDung.ThucPhamXanh.Helper.ChatNotifier.get(requireContext()).stop();
                // Đăng xuất khỏi Firebase
                FirebaseAuth.getInstance().signOut();
                
                // Chuyển đến màn hình đăng nhập
                Intent intent = new Intent(getActivity(), SignInActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                
                // Đóng activity hiện tại
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
        builder.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setCancelable(true);
        builder.show();
    }

    private void showImageSourceDialog() {
        if (getActivity() == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
        if (getActivity() == null) return false;
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            return false;
        }
        return true;
    }
    
    private boolean checkStoragePermission() {
        if (getActivity() == null) return false;
        // Chỉ cần cho Android < 13
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
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
                Toast.makeText(getActivity(), "Cần quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 101) {
            // Storage permission
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(getActivity(), "Cần quyền truy cập thư viện để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void openCamera() {
        if (getActivity() == null) return;
        try {
            // Tạo file tạm để lưu ảnh
            java.io.File photoFile = new java.io.File(getActivity().getCacheDir(), "camera_image_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = androidx.core.content.FileProvider.getUriForFile(
                getActivity(),
                getActivity().getPackageName() + ".fileprovider",
                photoFile
            );
            
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                cameraLauncher.launch(intent);
            } else {
                Toast.makeText(getActivity(), "Không thể mở camera", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: không chỉ định output URI
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                cameraLauncher.launch(intent);
            } else {
                Toast.makeText(getActivity(), "Không thể mở camera", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void openGallery() {
        galleryLauncher.launch("image/*");
    }
    
    private void uploadImageToStorage(Uri imageUri) {
        if (getActivity() == null || imageUri == null) {
            Toast.makeText(getActivity(), "Không thể lấy ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Đang tải ảnh lên...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        ImageUploadHelper.uploadImage(
            getActivity(),
            imageUri,
            "avatars", // Folder trong Storage
            progressDialog,
            new ImageUploadHelper.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    // Hiển thị ảnh
                    Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_avatar_default)
                            .error(R.drawable.ic_avatar_default)
                            .into(imgAvatar);
                    
                    // Lưu URL vào Firestore
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    firestore.collection("User")
                            .document(uid)
                            .collection("Profile").document(key)
                            .update("avatar", imageUrl)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        // Update vào realtime database
                                        reference = FirebaseDatabase.getInstance().getReference("Users")
                                                .child(uid);
                                        HashMap<String, Object> map = new HashMap<>();
                                        map.put("avatar", imageUrl);
                                        reference.updateChildren(map);
                                        Toast.makeText(getActivity(), "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getActivity(), "Lỗi cập nhật ảnh đại diện", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
                
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getActivity(), "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        );
    }
}