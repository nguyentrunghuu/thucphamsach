package com.UngDung.ThucPhamXanh.View.Admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Adapter.HoaDonAdapter;
import com.UngDung.ThucPhamXanh.Models.HoaDon;
import com.UngDung.ThucPhamXanh.Presenter.HoaDonPreSenter;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.HoaDonView;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminBillMainActivity extends AppCompatActivity implements HoaDonView, AdapterView.OnItemSelectedListener {

    private Toolbar toolbarBillAdmin;
    private RecyclerView rcvBillAdmin;
    private Spinner spinnerStatusAdmin, spinnerUserAdmin;
    private String[] strStatus = { "Tất cả", "Đã đặt hàng", "Đang xử lý", "Đã xác nhận", "Đang giao hàng", "Đã giao", "Đã hủy" };
    // Mã trạng thái tương ứng với từng dòng trong strStatus (0 = tất cả) - xem Models/OrderStatus
    private static final int[] FILTER_STATUS_CODES = {
            0,
            com.UngDung.ThucPhamXanh.Models.OrderStatus.DA_DAT,
            com.UngDung.ThucPhamXanh.Models.OrderStatus.DANG_XU_LY,
            com.UngDung.ThucPhamXanh.Models.OrderStatus.DA_XAC_NHAN,
            com.UngDung.ThucPhamXanh.Models.OrderStatus.DANG_GIAO,
            com.UngDung.ThucPhamXanh.Models.OrderStatus.DA_GIAO,
            com.UngDung.ThucPhamXanh.Models.OrderStatus.DA_HUY,
    };

    private HoaDonPreSenter hoaDonPreSenter;
    private ArrayList<HoaDon> mListStatus;
    private ArrayList<String> mlistUser;
    private final List<String> mListUserName = new ArrayList<>();
    private ArrayAdapter<String> userAdapter;
    private ArrayList<HoaDon> mListFilter;
    private HoaDonAdapter adapter;

    public int positionStatus;
    private String iduser;
    private ListenerRegistration hoadonListener;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_bill_main);
        InitWidget();
        Init();
        Event();

        // Initialize iduser to prevent null pointer exception
        if (iduser == null) {
            iduser = "Tất cả";
            Log.d("AdminBillMainActivity", "🔧 Initialized iduser to 'Tất cả'");
        }

        setupRealtimeListener();
    }

    private void Event() {
        // Set up back button
        View imgBack = findViewById(R.id.img_back_bill_admin);
        if (imgBack != null) {
            imgBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        }

        // Log.d("position", "iduser là: " + iduser + " + " + "trạng thái là: " +
        // positionStatus);
        // Log.d("position","trạng thái là: " + positionStatus);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                int pos = viewHolder.getAdapterPosition();
                // Trả dòng về vị trí cũ (vuốt chỉ để mở hộp cập nhật, không xóa khỏi danh sách)
                adapter.notifyItemChanged(pos);
                if (pos >= 0 && pos < mListFilter.size()) {
                    DiaLogUpDate(pos);
                }

            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(rcvBillAdmin);
    }

    private void InitWidget() {
        toolbarBillAdmin = findViewById(R.id.toolbar_bill_admin);
        rcvBillAdmin = findViewById(R.id.rcv_bill_admin);
        spinnerStatusAdmin = findViewById(R.id.spinner_status_admin);
        spinnerUserAdmin = findViewById(R.id.spinner_user_admin);
        mlistUser = new ArrayList<>();
        mListStatus = new ArrayList<>();
        mListFilter = new ArrayList<>();

        // Adapter chỉ tạo MỘT lần; dữ liệu thay đổi thì notifyDataSetChanged
        adapter = new HoaDonAdapter();
        adapter.setDataHoadon(mListFilter, new IClickCTHD() {
            @Override
            public void onClickCTHD(int pos) {
                if (pos < 0 || pos >= mListFilter.size()) return;
                Intent intent = new Intent(AdminBillMainActivity.this, AdminCTHDActivity.class);
                intent.putExtra("CTHD", mListFilter.get(pos));
                startActivity(intent);
            }
        });
        rcvBillAdmin.setLayoutManager(new LinearLayoutManager(this));
        rcvBillAdmin.setAdapter(adapter);
    }

    private void Init() {
        setSupportActionBar(toolbarBillAdmin);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        hoaDonPreSenter = new HoaDonPreSenter(this);

        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, strStatus);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatusAdmin.setAdapter(arrayAdapter);
        spinnerStatusAdmin.setOnItemSelectedListener(this);

        loadUserSpinner();
    }

    private void DiaLogUpDate(int pos) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_update_trangthai);
        dialog.show();
        // adapter.notifyDataSetChanged();

        Spinner spiner = dialog.findViewById(R.id.spinerCapNhat);
        String[] s = { "Chọn Mục", "Đang xử lý", "Đã xác nhận", "Đang giao hàng", "Đã giao" };
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, s);
        spiner.setAdapter(arrayAdapter);
        spiner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    // Đơn đã giao / đã hủy thì không đổi trạng thái được nữa
                    long current = mListFilter.get(pos).getType();
                    if (com.UngDung.ThucPhamXanh.Models.OrderStatus.isFinal(current)) {
                        Toast.makeText(AdminBillMainActivity.this, "Không thể thay đổi trạng thái đơn hàng "
                                + com.UngDung.ThucPhamXanh.Models.OrderStatus.label(current).toLowerCase(),
                                Toast.LENGTH_LONG).show();
                        dialog.cancel();
                        return;
                    }

                    // Map position to status: 1=Đang xử lý(2), 2=Đã xác nhận(5), 3=Đang giao hàng(4), 4=Đã giao(3)
                    int status;
                    if (position == 1) {
                        status = 2; // Đang xử lý
                    } else if (position == 2) {
                        status = 5; // Đã xác nhận
                    } else if (position == 3) {
                        status = 4; // Đang giao hàng
                    } else {
                        status = 3; // Đã giao
                    }

                    hoaDonPreSenter.CapNhatTrangThai(status, mListFilter.get(pos).getId());
                    Toast.makeText(AdminBillMainActivity.this, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT)
                            .show();
                    dialog.cancel();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    private void setupRealtimeListener() {
        Log.d("AdminBillMainActivity", "🔄 Setting up realtime listener for hóa đơn...");

        // Remove existing listener if any
        if (hoadonListener != null) {
            hoadonListener.remove();
        }

        // MỘT listener duy nhất, lọc ngay trên Firestore theo khách hàng + trạng thái.
        // Mỗi lần có thay đổi: dựng lại toàn bộ danh sách từ snapshot -> không bao giờ bị trùng.
        com.google.firebase.firestore.Query query = db.collection("HoaDon");
        if (iduser != null && !iduser.equals("Tất cả")) {
            query = query.whereEqualTo("UID", iduser);
        }
        if (positionStatus != 0) {
            query = query.whereEqualTo("trangthai", positionStatus);
        }

        hoadonListener = query.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                Log.e("AdminBillMainActivity", "❌ Error listening to HoaDon changes", error);
                return;
            }
            if (querySnapshot == null) return;

            mListFilter.clear();
            for (QueryDocumentSnapshot c : querySnapshot) {
                mListFilter.add(new HoaDon(c.getId(), c.getString("UID"), c.getString("ghichu"),
                        c.getString("diachi"), c.getString("hoten"), c.getString("ngaydat"),
                        c.getString("phuongthuc"), c.getString("sdt"), c.getString("tongtien"),
                        c.getLong("trangthai")));
            }
            // Đơn mới nhất lên đầu (ngaydat dạng dd/MM/yyyy)
            java.util.Collections.sort(mListFilter, (a, b) -> Long.compare(dateKey(b.getNgaydat()), dateKey(a.getNgaydat())));
            adapter.notifyDataSetChanged();
        });
    }

    /** "dd/MM/yyyy" -> yyyyMMdd để so sánh; sai định dạng -> 0. */
    private static long dateKey(String ddMMyyyy) {
        if (ddMMyyyy == null) return 0;
        String[] p = ddMMyyyy.trim().split("/");
        if (p.length != 3) return 0;
        try {
            return Long.parseLong(p[2]) * 10000 + Long.parseLong(p[1]) * 100 + Long.parseLong(p[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void refreshData() {
        setupRealtimeListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up realtime listener
        if (hoadonListener != null) {
            hoadonListener.remove();
            Log.d("AdminBillMainActivity", "🧹 Cleaned up realtime listener");
        }
    }

    @Override
    public void getDataHD(String id, String uid, String ghichu, String diachi, String hoten, String ngaydat,
            String phuongthuc, String sdt, String tongtien, Long type) {
        // Không dùng nữa: danh sách được dựng trực tiếp trong setupRealtimeListener()
    }

    @Override
    public void OnFail() {

    }

    @Override
    public void OnSucess() {
        // Listener realtime tự cập nhật danh sách, không cần tải lại
        Toast.makeText(this, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        // positionStatus chứa MÃ trạng thái (không phải vị trí dòng trong spinner)
        positionStatus = (i >= 0 && i < FILTER_STATUS_CODES.length) ? FILTER_STATUS_CODES[i] : 0;
        setupRealtimeListener();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
    private void loadUserSpinner() {
        mlistUser.clear();
        mListUserName.clear();
        mlistUser.add("Tất cả");
        mListUserName.add("Tất cả");

        userAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, mListUserName);
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserAdmin.setAdapter(userAdapter);

        spinnerUserAdmin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                iduser = mlistUser.get(i);
                setupRealtimeListener();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        db.collection("IDUser").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                String uid = d.getString("iduser");
                if (uid == null || mlistUser.contains(uid)) continue;

                mlistUser.add(uid);
                // Hiện email cho dễ nhận biết (thay vì UID khó đọc)
                String email = d.getString("email");
                mListUserName.add(email != null && !email.isEmpty() ? email : uid);
            }
            userAdapter.notifyDataSetChanged();
        });
    }
}