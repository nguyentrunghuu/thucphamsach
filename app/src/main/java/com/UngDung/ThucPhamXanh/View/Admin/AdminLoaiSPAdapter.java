package com.UngDung.ThucPhamXanh.View.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Models.LoaiProduct;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class AdminLoaiSPAdapter extends RecyclerView.Adapter<AdminLoaiSPAdapter.ViewHolder> {

    private AdminLoaiSPActivity context;
    private ArrayList<LoaiProduct> list;
    private IClickCTHD iClickCTHD;

    public AdminLoaiSPAdapter(AdminLoaiSPActivity context, ArrayList<LoaiProduct> list, IClickCTHD iClickCTHD) {
        this.context = context;
        this.list = list;
        this.iClickCTHD = iClickCTHD;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_loai_sp, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LoaiProduct loaiSP = list.get(position);
        
        holder.tvTenLoai.setText(loaiSP.getTenloai());
        
        if (loaiSP.getHinhanh() != null && !loaiSP.getHinhanh().isEmpty()) {
            Picasso.get()
                    .load(loaiSP.getHinhanh())
                    .placeholder(R.drawable.logo_new)
                    .error(R.drawable.logo_new)
                    .into(holder.imgLoaiSP);
        } else {
            holder.imgLoaiSP.setImageResource(R.drawable.logo_new);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iClickCTHD.onClickCTHD(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgLoaiSP;
        TextView tvTenLoai;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgLoaiSP = itemView.findViewById(R.id.img_loai_sp);
            tvTenLoai = itemView.findViewById(R.id.tv_ten_loai_sp);
        }
    }
}

