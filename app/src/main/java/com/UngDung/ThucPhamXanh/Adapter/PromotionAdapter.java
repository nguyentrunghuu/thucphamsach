package com.UngDung.ThucPhamXanh.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.ViewHolder> {
    private Context context;
    private ArrayList<String> promotionList;
    private IClickCTHD iClickCTHD;

    public PromotionAdapter(Context context, ArrayList<String> promotionList, IClickCTHD iClickCTHD) {
        this.context = context;
        this.promotionList = promotionList != null ? promotionList : new ArrayList<>();
        this.iClickCTHD = iClickCTHD;
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.dong_banner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        if (position < promotionList.size()) {
            String imageUrl = promotionList.get(position);
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                Picasso.get().load(imageUrl.trim()).into(holder.imagePromotion);
            }
            
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (iClickCTHD != null) {
                        iClickCTHD.onClickCTHD(position);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return promotionList != null ? promotionList.size() : 0;
    }

    public void updateList(ArrayList<String> newList) {
        this.promotionList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView imagePromotion;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            imagePromotion = itemView.findViewById(R.id.image_banner);
        }
    }
}

