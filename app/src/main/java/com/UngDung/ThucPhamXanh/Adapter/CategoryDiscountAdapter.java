package com.UngDung.ThucPhamXanh.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Models.CategoryDiscount;
import com.UngDung.ThucPhamXanh.R;

import java.util.ArrayList;
import java.util.HashMap;

public class CategoryDiscountAdapter extends RecyclerView.Adapter<CategoryDiscountAdapter.CategoryDiscountViewHolder> {
    
    private Context context;
    private ArrayList<String> categoryList;
    private HashMap<String, CategoryDiscount> categoryDiscounts; // categoryName -> CategoryDiscount
    
    public CategoryDiscountAdapter(Context context, ArrayList<String> categoryList) {
        this.context = context;
        this.categoryList = categoryList;
        this.categoryDiscounts = new HashMap<>();
    }
    
    public void setCategoryDiscounts(HashMap<String, CategoryDiscount> discounts) {
        this.categoryDiscounts = discounts != null ? discounts : new HashMap<>();
        notifyDataSetChanged();
    }
    
    public void updateCategoryList(ArrayList<String> newCategoryList) {
        this.categoryList = newCategoryList != null ? newCategoryList : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public HashMap<String, CategoryDiscount> getCategoryDiscounts() {
        return categoryDiscounts;
    }
    
    @NonNull
    @Override
    public CategoryDiscountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_discount, parent, false);
        return new CategoryDiscountViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryDiscountViewHolder holder, int position) {
        String categoryName = categoryList.get(position);
        CategoryDiscount discount = categoryDiscounts.get(categoryName);
        
        holder.tvCategoryName.setText(categoryName);
        if (discount != null && discount.getDiscountPercent() > 0) {
            String text = "Mua ≥" + discount.getMinProducts() + " SP: Giảm " + discount.getDiscountPercent() + "%";
            holder.tvDiscountPercent.setText(text);
        } else {
            holder.tvDiscountPercent.setText("Chưa có khuyến mãi");
        }
        
        holder.itemView.setOnClickListener(v -> {
            int minProducts = discount != null ? discount.getMinProducts() : 0;
            int discountPercent = discount != null ? discount.getDiscountPercent() : 0;
            showEditDiscountDialog(categoryName, minProducts, discountPercent, position);
        });
    }
    
    @Override
    public int getItemCount() {
        return categoryList.size();
    }
    
    private void showEditDiscountDialog(String categoryName, int currentMinProducts, int currentDiscountPercent, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Thiết lập khuyến mãi cho " + categoryName);
        
        // Tạo layout chứa 2 EditText
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        EditText edtMinProducts = new EditText(context);
        edtMinProducts.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtMinProducts.setHint("Số sản phẩm tối thiểu (VD: 3)");
        edtMinProducts.setText(currentMinProducts > 0 ? String.valueOf(currentMinProducts) : "");
        edtMinProducts.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(edtMinProducts);
        
        EditText edtDiscount = new EditText(context);
        edtDiscount.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtDiscount.setHint("% Giảm giá (0-100)");
        edtDiscount.setText(currentDiscountPercent > 0 ? String.valueOf(currentDiscountPercent) : "");
        edtDiscount.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        edtDiscount.setPadding(0, 20, 0, 0);
        layout.addView(edtDiscount);
        
        builder.setView(layout);
        
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String minProductsStr = edtMinProducts.getText().toString().trim();
            String discountStr = edtDiscount.getText().toString().trim();
            
            if (minProductsStr.isEmpty() && discountStr.isEmpty()) {
                categoryDiscounts.remove(categoryName);
            } else {
                try {
                    int minProducts = minProductsStr.isEmpty() ? 0 : Integer.parseInt(minProductsStr);
                    int discountPercent = discountStr.isEmpty() ? 0 : Integer.parseInt(discountStr);
                    
                    if (minProducts < 0) {
                        Toast.makeText(context, "Số sản phẩm tối thiểu phải >= 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (discountPercent < 0 || discountPercent > 100) {
                        Toast.makeText(context, "% giảm giá phải từ 0-100", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (minProducts > 0 && discountPercent > 0) {
                        categoryDiscounts.put(categoryName, new CategoryDiscount(minProducts, discountPercent));
                    } else {
                        categoryDiscounts.remove(categoryName);
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Vui lòng nhập số hợp lệ", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            notifyItemChanged(position);
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        
        builder.setNeutralButton("Xóa", (dialog, which) -> {
            categoryDiscounts.remove(categoryName);
            notifyItemChanged(position);
            dialog.dismiss();
        });
        
        builder.show();
    }
    
    static class CategoryDiscountViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        TextView tvDiscountPercent;
        
        public CategoryDiscountViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvDiscountPercent = itemView.findViewById(R.id.tv_discount_percent);
        }
    }
}

