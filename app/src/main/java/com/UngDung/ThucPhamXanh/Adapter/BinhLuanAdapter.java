package com.UngDung.ThucPhamXanh.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.ReviewUtil;
import com.UngDung.ThucPhamXanh.Models.Binhluan;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class BinhLuanAdapter extends RecyclerView.Adapter<BinhLuanAdapter.ViewHolder>{

    private Context context;
    private ArrayList<Binhluan> mlist;
    private IClickCTHD iClickCTHD;

    public BinhLuanAdapter(Context context, ArrayList<Binhluan> mlist, IClickCTHD iClickCTHD) {
        this.context = context;
        this.mlist = mlist;
        this.iClickCTHD = iClickCTHD;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.dong_binhluan, parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {

        Binhluan binhluan = mlist.get(position);
        String noidung = binhluan.getNoidung();
        holder.tvBinhLuan.setText(noidung == null || noidung.trim().isEmpty() ? "(Không có nhận xét)" : noidung);
        holder.tvRate.setText(ReviewUtil.formatRate(ReviewUtil.parseRate(binhluan.getRate())));
        ReviewUtil.bindReviewer(binhluan.getIduser(), holder.tvNameBinhLuan, holder.cirHinhAnhBinhLuan);
    }

    @Override
    public int getItemCount() {
        return mlist.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private CircleImageView cirHinhAnhBinhLuan;
        private TextView tvBinhLuan, tvRate, tvNameBinhLuan;
        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            cirHinhAnhBinhLuan = itemView.findViewById(R.id.cir_hinhanh_binhluan);
            tvBinhLuan = itemView.findViewById(R.id.tv_binhluan);
            tvRate = itemView.findViewById(R.id.tv_rate);
            tvNameBinhLuan = itemView.findViewById(R.id.tv_name_binhluan);
        }
    }
}
