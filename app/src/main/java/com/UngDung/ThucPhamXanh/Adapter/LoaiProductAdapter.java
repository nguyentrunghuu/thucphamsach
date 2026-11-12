package com.UngDung.ThucPhamXanh.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Models.LoaiProduct;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IClickLoaiProduct;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LoaiProductAdapter extends RecyclerView.Adapter<LoaiProductAdapter.ViewHolder>{
    private List<LoaiProduct> mList;
    private IClickLoaiProduct iClickLoaiProduct;
    
    // Màu nền pastel cho ô biểu tượng danh mục (lặp lại theo vị trí)
    private int[] backgroundColors = {
        0xFFE8F5E9, // Xanh lá nhạt
        0xFFFFF3E0, // Cam nhạt
        0xFFE3F2FD, // Xanh dương nhạt
        0xFFFFF8E1, // Vàng nhạt
        0xFFFCE4EC, // Hồng nhạt
        0xFFE0F2F1, // Xanh ngọc nhạt
        0xFFF3E5F5, // Tím nhạt
    };

    /** Chọn biểu tượng (emoji) phù hợp theo tên danh mục. */
    static String iconFor(String name) {
        if (name == null) return "🛒";
        String n = name.toLowerCase(java.util.Locale.ROOT);
        if (n.contains("rau") || n.contains("củ") || n.contains("nấm")) return "🥬";
        if (n.contains("trái") || n.contains("hoa quả") || n.contains("quả")) return "🍎";
        if (n.contains("hải sản") || n.contains("tôm") || n.contains("mực")) return "🦐";
        if (n.contains("cá")) return "🐟";
        if (n.contains("thịt") || n.contains("bò") || n.contains("heo") || n.contains("gà")) return "🥩";
        if (n.contains("trứng")) return "🥚";
        if (n.contains("sữa")) return "🥛";
        if (n.contains("gia vị") || n.contains("nước mắm") || n.contains("dầu")) return "🧂";
        if (n.contains("đồ uống") || n.contains("nước") || n.contains("uống")) return "🧃";
        if (n.contains("gạo") || n.contains("ngũ cốc") || n.contains("hạt")) return "🌾";
        if (n.contains("khô") || n.contains("mì") || n.contains("bún")) return "🍜";
        if (n.contains("bánh") || n.contains("kẹo") || n.contains("snack")) return "🍪";
        if (n.contains("đông lạnh")) return "🧊";
        return "🛒";
    }
    
    public void setData(List<LoaiProduct> list, IClickLoaiProduct iClickLoaiProduct){
        this.mList = list;
        this.iClickLoaiProduct = iClickLoaiProduct;
        notifyDataSetChanged();
    }
    
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.dong_loai_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        LoaiProduct loaiProduct = mList.get(position);
        if (loaiProduct == null){
            return;
        }

        holder.tvLoaiProduct.setText(loaiProduct.getTenloai());
        
        // Biểu tượng + màu nền pastel theo vị trí
        if (holder.tvLoaiIcon != null) {
            holder.tvLoaiIcon.setText(iconFor(loaiProduct.getTenloai()));
            android.graphics.drawable.Drawable bg = holder.tvLoaiIcon.getBackground();
            if (bg != null) {
                bg = bg.mutate();
                if (bg instanceof android.graphics.drawable.GradientDrawable) {
                    ((android.graphics.drawable.GradientDrawable) bg)
                            .setColor(backgroundColors[position % backgroundColors.length]);
                }
            }
        }

        // Click itemLoaiProduct
        holder.itemLoaiProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iClickLoaiProduct.onClickItemLoaiProduct(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mList != null){
            return mList.size();
        }
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView tvLoaiProduct, tvLoaiIcon;
        private View itemLoaiProduct;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            tvLoaiProduct = itemView.findViewById(R.id.tv_loai_product);
            tvLoaiIcon = itemView.findViewById(R.id.tv_loai_icon);
            itemLoaiProduct = itemView.findViewById(R.id.item_loai_product);
        }
    }
}
