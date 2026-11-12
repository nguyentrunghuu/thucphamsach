package com.UngDung.ThucPhamXanh.View.Admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Models.Shipper;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class AdminShipperAdapter extends RecyclerView.Adapter<AdminShipperAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Shipper> mlistShipper;
    private IClickCTHD iClickCTHD;

    public AdminShipperAdapter(Context context, ArrayList<Shipper> mlistShipper, IClickCTHD iClickCTHD) {
        this.context = context;
        this.mlistShipper = mlistShipper;
        this.iClickCTHD = iClickCTHD;
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(context).inflate(R.layout.dong_admin_shipper, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        Shipper shipper = mlistShipper.get(position);
        
        if (shipper.getName() != null && !shipper.getName().isEmpty()) {
            holder.tvNameShipper.setText(shipper.getName());
        } else {
            holder.tvNameShipper.setText("Shipper");
        }

        holder.tvUsernameShipper.setText(shipper.getUsername());
        
        if (shipper.getPhone() != null && !shipper.getPhone().isEmpty()) {
            holder.tvPhoneShipper.setText(shipper.getPhone());
            holder.tvPhoneShipper.setVisibility(View.VISIBLE);
        } else {
            holder.tvPhoneShipper.setVisibility(View.GONE);
        }

        holder.relativeItemShipper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iClickCTHD.onClickCTHD(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mlistShipper != null ? mlistShipper.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tvNameShipper, tvUsernameShipper, tvPhoneShipper;
        private View relativeItemShipper;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            tvNameShipper = itemView.findViewById(R.id.tv_name_shipper);
            tvUsernameShipper = itemView.findViewById(R.id.tv_username_shipper);
            tvPhoneShipper = itemView.findViewById(R.id.tv_phone_shipper);
            relativeItemShipper = itemView.findViewById(R.id.relative_item_shipper);
        }
    }
}

