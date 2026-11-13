package com.UngDung.ThucPhamXanh.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatToggleButton;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.UngDung.ThucPhamXanh.Adapter.BinhLuanAdapter;
import com.UngDung.ThucPhamXanh.MainActivity;
import com.UngDung.ThucPhamXanh.Models.Binhluan;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.Presenter.BinhLuanPresenter;
import com.UngDung.ThucPhamXanh.Presenter.GioHangPresenter;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.BinhLuanView;
import com.UngDung.ThucPhamXanh.my_interface.GioHangView;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // (giữ import cho tương thích)
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class DetailSPActivity extends AppCompatActivity implements BinhLuanView, GioHangView {

    private ImageView viewAnimation;
    private View viewEndAnimation;

    private AppCompatToggleButton toggleButtonFavorite;
    private View btnAddCartDetail; // nút "Thêm vào giỏ" ở thanh dưới cùng
    private LinearLayout linearShowAllCmt;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private TextView tvGiaDetail, tvHansudungDetail, tvTrongluongDetail, tvMoTaDetail, tvSoluongDetail, tvLoaispDetail;
    private ImageView imgDetail;
    private Toolbar toolbar;
    private RecyclerView rcvBinhluan;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private BinhLuanPresenter binhLuanPresenter;
    private ArrayList<Binhluan> mListBinhluan;
    private BinhLuanAdapter adapter;
    private Product product;

    private BottomSheetDialog bottomSheetDialog;
    private TextView tvTenBottom, tvGiaBottom, tvSoluongBottom, tvMotaBottom;
    private ImageView imgBottom;
    private View btnMinusBottom, btnPlusBottom;
    private Button btnBottom;
    private int slBottom = 1;
    private GioHangPresenter gioHangPresenter;

    private MainActivity mainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_spactivity);

        InitWidget();
        Init();
        Event();

    }

    private void Event() {
        linearShowAllCmt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DetailSPActivity.this, CommentActivity.class);
                intent.putExtra("allcmt", product);
                startActivity(intent);
            }
        });

        bindAvailability();
        btnAddCartDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Kiểm tra lại lúc bấm (màn hình có thể mở từ hôm trước)
                String reason = unavailableReason();
                if (reason != null) {
                    bindAvailability();
                    Toast.makeText(DetailSPActivity.this, reason, Toast.LENGTH_SHORT).show();
                    return;
                }
                setBottomSheetDialog();
                tvTenBottom.setText(product.getTensp());
                tvGiaBottom.setText(NumberFormat.getInstance().format(product.getGiatien()) + "");
                tvMotaBottom.setText(product.getMota());
                if (product.getHinhanh() != null && !product.getHinhanh().isEmpty()) {
                    Picasso.get().load(product.getHinhanh()).into(imgBottom);
                } else {
                    imgBottom.setImageResource(R.drawable.logo_new);
                }
                initBottomSheet();
                bottomSheetDialog.show();
            }
        });
    }

    /** Hết hạn / hết hàng → lý do (để báo khách); còn bán → null. */
    private String unavailableReason() {
        return com.UngDung.ThucPhamXanh.Helper.ProductVisibility.unavailableReason(
                product.getHansudung(), product.getSoluong(), java.time.LocalDate.now());
    }

    /** Nút "Thêm vào giỏ" mờ đi và đổi chữ khi sản phẩm không bán được. */
    private void bindAvailability() {
        boolean expired = com.UngDung.ThucPhamXanh.Helper.ProductVisibility.isExpired(
                product.getHansudung(), java.time.LocalDate.now());
        boolean available = unavailableReason() == null;
        btnAddCartDetail.setAlpha(available ? 1f : 0.45f);
        TextView label = findViewById(R.id.tv_addcart_detail);
        if (label != null) {
            label.setText(available ? "Thêm vào giỏ" : (expired ? "Đã hết hạn" : "Hết hàng"));
        }
    }

    // Nút yêu thích
    public void onDefaultToggleClick(View view) {
        final String[] idlove = { "" };
        boolean ToggleButtonState = toggleButtonFavorite.isChecked();
        if (ToggleButtonState) {
            // Nếu thích thì làm gì và ngược lại
            HashMap<String, String> map = new HashMap<>();
            map.put("idproduct", product.getId());
            map.put("iduser", FirebaseAuth.getInstance().getCurrentUser().getUid());
            db.collection("Favorite").add(map).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    idlove[0] = documentReference.getId();
                }
            });

        } else {
            db.collection("Favorite").whereEqualTo("idproduct", product.getId())
                    .whereEqualTo("iduser", FirebaseAuth.getInstance().getCurrentUser().getUid()).get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            for (QueryDocumentSnapshot q : queryDocumentSnapshots) {
                                db.collection("Favorite").document(q.getId()).delete();
                            }
                        }
                    });
        }

    }

    /** Trả về "—" nếu chuỗi rỗng để ô thông tin không bị trống. */
    private static String textOrDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s;
    }

    private void setBottomSheetDialog() {
        // Bottom sheet Dialog
        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.layout_persistent_bottom_sheet);
        // Nền trong suốt để thấy góc bo tròn của layout
        View sheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) sheet.setBackgroundResource(android.R.color.transparent);
        bottomSheetDialog.setOnShowListener(d -> {
            View s = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (s != null) s.setBackgroundResource(android.R.color.transparent);
        });
        tvTenBottom = bottomSheetDialog.findViewById(R.id.tv_ten_bottom);
        imgBottom = bottomSheetDialog.findViewById(R.id.img_bottom);
        tvGiaBottom = bottomSheetDialog.findViewById(R.id.tv_gia_bottom);
        btnMinusBottom = bottomSheetDialog.findViewById(R.id.btn_minus_bottom);
        btnPlusBottom = bottomSheetDialog.findViewById(R.id.btn_plus_bottom);
        tvSoluongBottom = bottomSheetDialog.findViewById(R.id.tv_soluong_bottom);
        tvMotaBottom = bottomSheetDialog.findViewById(R.id.tv_mota_bottom);
        btnBottom = bottomSheetDialog.findViewById(R.id.btn_bottom);
    }

    public void initBottomSheet() {
        // Khởi tạo số lượng ban đầu
        slBottom = 1;
        tvSoluongBottom.setText(String.valueOf(slBottom));

        Log.d("DetailSPActivity",
                "initBottomSheet: slBottom = " + slBottom + ", product.getSoluong() = " + product.getSoluong());

        btnMinusBottom.setVisibility(View.INVISIBLE);
        btnMinusBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slBottom = Integer.parseInt(tvSoluongBottom.getText().toString()) - 1;
                tvSoluongBottom.setText(String.valueOf(slBottom));
                // btnPlusBottom.setVisibility(View.VISIBLE); // Nếu muốn giới hạn lượng đặt sản
                // phẩm
                if (slBottom < 2) {
                    btnMinusBottom.setVisibility(View.INVISIBLE);
                } else
                    btnMinusBottom.setVisibility(View.VISIBLE);
            }
        });
        btnPlusBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("DetailSPActivity", "Plus button clicked. Current slBottom: " + slBottom + ", Product quantity: "
                        + product.getSoluong());

                // Kiểm tra số lượng trước khi tăng
                if (slBottom >= product.getSoluong()) {
                    Toast.makeText(DetailSPActivity.this, "Sản phẩm chỉ còn " + product.getSoluong() + " sản phẩm",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Tăng số lượng
                slBottom = Integer.parseInt(tvSoluongBottom.getText().toString()) + 1;
                tvSoluongBottom.setText(String.valueOf(slBottom));
                Log.d("DetailSPActivity", "After increment. New slBottom: " + slBottom);

                // Hiển thị/ẩn nút minus
                if (slBottom < 2) {
                    btnMinusBottom.setVisibility(View.INVISIBLE);
                } else {
                    btnMinusBottom.setVisibility(View.VISIBLE);
                }

                // Kiểm tra sau khi tăng để ẩn nút plus nếu đã đạt giới hạn
                if (slBottom >= product.getSoluong()) {
                    btnPlusBottom.setVisibility(View.GONE);
                    Toast.makeText(DetailSPActivity.this, "Đã đạt giới hạn số lượng có sẵn", Toast.LENGTH_SHORT).show();
                } else {
                    btnPlusBottom.setVisibility(View.VISIBLE);
                }
            }
        });

        btnBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Validate số lượng trước khi thêm vào giỏ hàng
                if (slBottom > product.getSoluong()) {
                    Toast.makeText(DetailSPActivity.this,
                            "Số lượng đặt vượt quá số lượng có sẵn (" + product.getSoluong() + " sản phẩm)",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (slBottom <= 0) {
                    Toast.makeText(DetailSPActivity.this, "Số lượng phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                gioHangPresenter.AddCart(product.getId(), Long.valueOf(slBottom));
                // AnimationUtil.translateAnimation(viewAnimation, viewAnimation,
                // viewEndAnimation, new Animation.AnimationListener() {
                // @Override
                // public void onAnimationStart(Animation animation) {
                //
                // }
                //
                // @Override
                // public void onAnimationEnd(Animation animation) {
                // gioHangPresenter.AddCart(product.getId(), Long.valueOf(slBottom));
                // mainActivity.setCountProductInCart(mainActivity.getmCountProduct() + 1);
                // }
                //
                // @Override
                // public void onAnimationRepeat(Animation animation) {
                //
                // }
                // });
            }
        });
    }

    private void Init() {
        Intent intent = getIntent();
        product = (Product) intent.getSerializableExtra("search");

        // Check sp này đã được yêu thích từ trước hay chưa, nếu có thì sẽ set cho
        // tooglebutton = true;
        db.collection("Favorite").whereEqualTo("iduser", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot q : queryDocumentSnapshots) {
                            if (q.getString("idproduct").equals(product.getId())) {
                                toggleButtonFavorite.setChecked(true);
                            }
                        }
                    }
                });

        collapsingToolbarLayout.setTitle(product.getTensp());
        tvGiaDetail.setText(NumberFormat.getInstance().format(product.getGiatien()));
        tvHansudungDetail.setText(textOrDash(product.getHansudung()));
        tvTrongluongDetail.setText(textOrDash(product.getTrongluong()));
        tvMoTaDetail.setText(product.getMota());
        tvSoluongDetail.setText(String.valueOf(product.getSoluong()));
        
        // Hiển thị loại sản phẩm
        if (tvLoaispDetail != null) {
            String loaisp = product.getLoaisp();
            if (loaisp != null && !loaisp.trim().isEmpty()) {
                tvLoaispDetail.setText(loaisp);
            } else {
                tvLoaispDetail.setText("Chưa phân loại");
            }
        }
        
        if (product.getHinhanh() != null && !product.getHinhanh().isEmpty()) {
            Picasso.get().load(product.getHinhanh()).into(imgDetail);
        } else {
            imgDetail.setImageResource(R.drawable.logo_new);
        }

        Log.d("soluong", "Số lượng sp là: " + product.getSoluong());
        binhLuanPresenter = new BinhLuanPresenter(this);
        binhLuanPresenter.HandleGetBinhLuanLimit(product.getId());

    }

    private void InitWidget() {
        mainActivity = new MainActivity();
        viewAnimation = findViewById(R.id.view_animation);
        viewEndAnimation = findViewById(R.id.view_end_animation);
        toggleButtonFavorite = findViewById(R.id.toogle_btn_favorite);
        btnAddCartDetail = findViewById(R.id.btn_addcart_detail);
        linearShowAllCmt = findViewById(R.id.linear_show_all_cmt);
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        tvGiaDetail = findViewById(R.id.tv_gia_detail);
        tvHansudungDetail = findViewById(R.id.tv_hansudung_detail);
        tvTrongluongDetail = findViewById(R.id.tv_trongluong_detail);
        tvMoTaDetail = findViewById(R.id.tv_mota_detail);
        tvSoluongDetail = findViewById(R.id.tv_soluong_detail);
        tvLoaispDetail = findViewById(R.id.tv_loaisp_detail);
        imgDetail = findViewById(R.id.img_detail);
        rcvBinhluan = findViewById(R.id.rcv_binhluan);
        toolbar = findViewById(R.id.toolbar_detail);

        toolbar.setNavigationIcon(R.drawable.ic_nav_back_circle);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mListBinhluan = new ArrayList<>();
        gioHangPresenter = new GioHangPresenter(this);

    }

    @Override
    public void getDataBinhLuan(String idbinhluan, String idproduct, String iduser, String rate, String noidung) {
        mListBinhluan.add(new Binhluan(idbinhluan, idproduct, iduser, rate, noidung));
        adapter = new BinhLuanAdapter(DetailSPActivity.this, mListBinhluan, new IClickCTHD() {
            @Override
            public void onClickCTHD(int pos) {

            }
        });

        rcvBinhluan.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rcvBinhluan.setAdapter(adapter);
    }

    @Override
    public void OnSucess() {
        Toast.makeText(this, "Thêm giỏ hàng thành công", Toast.LENGTH_SHORT).show();
        bottomSheetDialog.dismiss();
    }

    @Override
    public void OnFail() {
        Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void getDataSanPham(String id, String idsp, String tensp, Long giatien, String hinhanh, String loaisp,
            String mota, Long soluong, String hansudung, Long type, String trongluong) {

    }
}