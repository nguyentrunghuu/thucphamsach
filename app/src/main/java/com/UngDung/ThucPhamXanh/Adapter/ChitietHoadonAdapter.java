package com.UngDung.ThucPhamXanh.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.ArrayList;

public class ChitietHoadonAdapter extends RecyclerView.Adapter<ChitietHoadonAdapter.ViewHolder> {
    private ArrayList<Product> list;
    private IClickCTHD iClickCTHD;

    public void setDataChitietHoadon(ArrayList<Product> list, IClickCTHD iClickCTHD){
        this.list = list;
        this.iClickCTHD = iClickCTHD;
        notifyDataSetChanged();
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.dong_chitiet_hoadon,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        Product product = list.get(position);
        holder.tvTenCTHD.setText(product.getTensp());
        holder.tvHansudungCTHD.setText(product.getHansudung());
        holder.tvSoluongCTHD.setText(product.getSoluong()+"");
        holder.tvDongiaCTHD.setText(NumberFormat.getInstance().format(product.getGiatien()));
        holder.tvTotalCTHD.setText(NumberFormat.getInstance().format(product.getGiatien() * product.getSoluong()));
        Picasso.get().load(product.getHinhanh()).into(holder.imgCTHD);

        holder.constraintLayoutItemCTHD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iClickCTHD.onClickCTHD(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private View constraintLayoutItemCTHD;
        private TextView tvTenCTHD, tvHansudungCTHD, tvSoluongCTHD, tvDongiaCTHD, tvTotalCTHD;
        private ImageView imgCTHD;
        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            tvTenCTHD = itemView.findViewById(R.id.tv_ten_cthd);
            tvHansudungCTHD = itemView.findViewById(R.id.tv_hansudung_cthd);
            tvSoluongCTHD = itemView.findViewById(R.id.tv_number_cthd);
            tvDongiaCTHD = itemView.findViewById(R.id.tv_giatien_cthd);
            tvTotalCTHD = itemView.findViewById(R.id.tv_total_cthd);
            imgCTHD = itemView.findViewById(R.id.img_cthd);
            constraintLayoutItemCTHD = itemView.findViewById(R.id.constraint_item_cthd);
        }
    }

}
