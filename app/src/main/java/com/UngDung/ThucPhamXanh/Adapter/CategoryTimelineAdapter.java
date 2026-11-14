package com.UngDung.ThucPhamXanh.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.ProductListActivity;
import com.UngDung.ThucPhamXanh.my_interface.IClickOpenBottomSheet;

import java.util.ArrayList;
import java.util.List;

public class CategoryTimelineAdapter extends RecyclerView.Adapter<CategoryTimelineAdapter.ViewHolder> {

    /** Tên mục AI gợi ý ở trang chủ: không có trang "Xem tất cả" vì không phải một loại sản phẩm. */
    public static final String AI_SECTION_TITLE = "Gợi ý cho bạn";
    
    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private Context context;
    private List<CategorySection> categorySections;
    private IClickOpenBottomSheet iClickOpenBottomSheet;
    private OnProductClickListener onProductClickListener;
    
    public CategoryTimelineAdapter(Context context, OnProductClickListener onProductClickListener) {
        this.context = context;
        this.onProductClickListener = onProductClickListener;
        this.categorySections = new ArrayList<>();
    }

    public CategoryTimelineAdapter(Context context, IClickOpenBottomSheet iClickOpenBottomSheet) {
        this.context = context;
        this.iClickOpenBottomSheet = iClickOpenBottomSheet;
        this.categorySections = new ArrayList<>();
    }
    
    public void setData(List<CategorySection> sections) {
        this.categorySections = sections != null ? sections : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_timeline_category, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategorySection section = categorySections.get(position);
        
        // Set category name
        holder.tvCategoryName.setText(section.getCategoryName());
        
        // Màu sắc đã được thiết kế thống nhất (xanh lá) trong item_timeline_category.xml
        
        // Limit to 4 products
        ArrayList<Product> limitedProducts = new ArrayList<>();
        for (int i = 0; i < Math.min(4, section.getProducts().size()); i++) {
            limitedProducts.add(section.getProducts().get(i));
        }
        
        // Setup product adapter with GridLayoutManager (2 columns)
        // Create a wrapper to handle clicks - lấy product trực tiếp từ limitedProducts
        ProductAdapter productAdapter = new ProductAdapter(context, limitedProducts, section.getType(), new IClickOpenBottomSheet() {
            @Override
            public void onClickOpenBottomSheet(int position) {
                // Get the clicked product from the limited list
                if (position >= 0 && position < limitedProducts.size()) {
                    Product clickedProduct = limitedProducts.get(position);
                    if (onProductClickListener != null) {
                        onProductClickListener.onProductClick(clickedProduct);
                    } else if (iClickOpenBottomSheet != null) {
                        // Tìm product này trong full list để lấy đúng reference
                        int actualPosition = section.getProducts().indexOf(clickedProduct);
                        if (actualPosition >= 0) {
                            // Call với position trong full list
                            iClickOpenBottomSheet.onClickOpenBottomSheet(actualPosition);
                        } else {
                            // Fallback: dùng position từ limited list
                            iClickOpenBottomSheet.onClickOpenBottomSheet(position);
                        }
                    }
                }
            }
        });
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 2);
        holder.rcvCategoryProducts.setLayoutManager(gridLayoutManager);
        holder.rcvCategoryProducts.setAdapter(productAdapter);
        
        boolean isAiSection = AI_SECTION_TITLE.equals(section.getCategoryName());
        holder.tvViewAll.setVisibility(isAiSection ? View.GONE : View.VISIBLE);

        // Set click listener for "View All"
        holder.tvViewAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ProductListActivity.class);
                intent.putExtra(ProductListActivity.EXTRA_TYPE, "category");
                intent.putExtra(ProductListActivity.EXTRA_CATEGORY, section.getCategoryName());
                intent.putExtra(ProductListActivity.EXTRA_TITLE, section.getCategoryName());
                context.startActivity(intent);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return categorySections.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        View viewIndicator;
        TextView tvCategoryName;
        TextView tvViewAll;
        RecyclerView rcvCategoryProducts;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewIndicator = itemView.findViewById(R.id.view_indicator);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvViewAll = itemView.findViewById(R.id.tv_view_all_category);
            rcvCategoryProducts = itemView.findViewById(R.id.rcv_category_products);
        }
    }
    
    // Inner class to represent a category section
    public static class CategorySection {
        private String categoryName;
        private ArrayList<Product> products;
        private int type;
        
        public CategorySection(String categoryName, ArrayList<Product> products, int type) {
            this.categoryName = categoryName;
            this.products = products;
            this.type = type;
        }
        
        public String getCategoryName() {
            return categoryName;
        }
        
        public ArrayList<Product> getProducts() {
            return products;
        }
        
        public int getType() {
            return type;
        }
    }
}

