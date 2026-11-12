package com.UngDung.ThucPhamXanh.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class HoaDonDaGiaoAdapter extends RecyclerView.Adapter<HoaDonDaGiaoAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Product> list = new ArrayList<>();
    private IClickCTHD iClickCTHD;
    private int type;

    public void setDataHoaDonDaGiao(Context context, ArrayList<Product> list, int type, IClickCTHD iClickCTHD) {
        Log.d("HoaDonDaGiaoAdapter", "📦 setDataHoaDonDaGiao: Nhận dữ liệu");
        Log.d("HoaDonDaGiaoAdapter", "📦 Số lượng sản phẩm: " + (list != null ? list.size() : "null"));
        Log.d("HoaDonDaGiaoAdapter", "📦 Type: " + type);

        this.context = context;
        this.type = type;
        this.iClickCTHD = iClickCTHD;
        
        // Clear và add all để tránh duplicate
        if (this.list == null) {
            this.list = new ArrayList<>();
        }
        this.list.clear();
        if (list != null && !list.isEmpty()) {
            this.list.addAll(list);
        }

        Log.d("HoaDonDaGiaoAdapter", "📦 notifyDataSetChanged() được gọi, số lượng: " + this.list.size());
        notifyDataSetChanged();

    }

    // public Boolean TrangThaiDaGiao(boolean b){
    // return b;
    // }

    @NotNull
    @Override
    public HoaDonDaGiaoAdapter.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view;
        if (type == 1) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.dong_hoadon_dagiao, parent, false);
        } else
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.dong_hoadon_khac, parent, false);

        return new HoaDonDaGiaoAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull HoaDonDaGiaoAdapter.ViewHolder holder, int position) {
        if (list == null || list.isEmpty() || position >= list.size()) {
            Log.e("HoaDonDaGiaoAdapter", "❌ Lỗi: list null hoặc position không hợp lệ");
            return;
        }
        Log.d("HoaDonDaGiaoAdapter", "📦 onBindViewHolder: position=" + position + ", list.size()=" + list.size());
        Product product = list.get(position);
        Log.d("HoaDonDaGiaoAdapter", "📦 Sản phẩm: " + product.getTensp() + " - Số lượng: " + product.getSoluong());

        holder.tvTen.setText(product.getTensp());
        String hsd = product.getHansudung();
        holder.tvHansudung.setText(hsd == null || hsd.trim().isEmpty() ? "—" : hsd);
        holder.tvSoluong.setText(product.getSoluong() + "");
        holder.tvDongia.setText(NumberFormat.getInstance().format(product.getGiatien()));
        holder.tvTotal.setText(NumberFormat.getInstance().format(product.getGiatien() * product.getSoluong()));
        Picasso.get().load(product.getHinhanh()).into(holder.img);

        Log.d("HoaDonDaGiaoAdapter", "📦 Đã bind dữ liệu cho sản phẩm: " + product.getTensp());

        // Chỉ xử lý nút đánh giá khi type = 1 (có thể đánh giá)
        if (type == 1 && holder.btnDongDanhGia != null) {
            holder.btnDongDanhGia.setVisibility(View.VISIBLE);
            // Reset trạng thái vì RecyclerView tái sử dụng dòng (tránh "Đã đánh giá" dính sang sản phẩm khác)
            holder.btnDongDanhGia.setEnabled(true);
            holder.btnDongDanhGia.setText("★  Đánh giá");
            final String idsp = product.getIdsp();
            holder.btnDongDanhGia.setTag(idsp);

            if (FirebaseAuth.getInstance().getCurrentUser() != null && idsp != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("BinhLuan")
                        .whereEqualTo("iduser", FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .whereEqualTo("idproduct", idsp)
                        .limit(1)
                        .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                if (!idsp.equals(holder.btnDongDanhGia.getTag())) return; // dòng đã đổi sản phẩm
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    holder.btnDongDanhGia.setEnabled(false);
                                    holder.btnDongDanhGia.setText("Đã đánh giá");
                                }
                            }
                        });
            }

            holder.btnDongDanhGia.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (iClickCTHD != null) {
                        iClickCTHD.onClickCTHD(position);
                    }
                }
            });
        } else if (holder.btnDongDanhGia != null) {
            // Ẩn nút khi type != 1
            holder.btnDongDanhGia.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private View constraintDongDanhGia;
        private TextView tvTen, tvHansudung, tvSoluong, tvDongia, tvTotal;
        private CircleImageView img;
        private Button btnDongDanhGia;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            tvTen = itemView.findViewById(R.id.tv_name_dong_danhgia);
            tvHansudung = itemView.findViewById(R.id.tv_hansudung_dong_danhgia);
            tvSoluong = itemView.findViewById(R.id.tv_number_dong_danhgia);
            tvDongia = itemView.findViewById(R.id.tv_giatien_dong_danhgia);
            tvTotal = itemView.findViewById(R.id.tv_total_dong_danhgia);
            img = itemView.findViewById(R.id.img_dong_danhgia);
            constraintDongDanhGia = itemView.findViewById(R.id.constraint_dong_danhgia);
            
            // Nút đánh giá chỉ có trong layout dong_hoadon_dagiao
            btnDongDanhGia = itemView.findViewById(R.id.btn_dong_danhgia);
        }
    }
}
