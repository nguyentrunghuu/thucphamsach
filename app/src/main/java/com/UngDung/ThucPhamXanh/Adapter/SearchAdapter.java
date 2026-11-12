package com.UngDung.ThucPhamXanh.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.ProductSearch;
import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.squareup.picasso.Picasso;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Danh sách kết quả tìm kiếm. Logic lọc / sắp xếp nằm ở {@link ProductSearch};
 * adapter chỉ giữ dữ liệu và hiển thị.
 */
public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    public static final int SORT_RELEVANT = ProductSearch.SORT_RELEVANT;
    public static final int SORT_BESTSELLER = ProductSearch.SORT_BESTSELLER;
    public static final int SORT_PRICE_ASC = ProductSearch.SORT_PRICE_ASC;
    public static final int SORT_PRICE_DESC = ProductSearch.SORT_PRICE_DESC;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private final OnProductClickListener listener;
    private final ProductSearch search = new ProductSearch();
    private final List<Product> all = new ArrayList<>();
    private final List<Product> shown = new ArrayList<>();

    public SearchAdapter(Context context, OnProductClickListener listener) {
        this.listener = listener;
    }

    // ---------- Dữ liệu & bộ lọc ----------

    public void setProducts(List<Product> products, Map<String, Long> sold) {
        all.clear();
        all.addAll(products);
        search.setSoldCounts(sold);
        apply();
    }

    public List<Product> getAllProducts() { return all; }

    public void setQuery(String q) { search.setQuery(q); apply(); }
    public void setSort(int sort) { search.setSort(sort); apply(); }
    public void setFilters(String category, long min, long max, boolean inStockOnly) {
        search.setFilters(category, min, max, inStockOnly);
        apply();
    }

    public int getSort() { return search.getSort(); }
    public String getCategory() { return search.getCategory(); }
    public long getMinPrice() { return search.getMinPrice(); }
    public long getMaxPrice() { return search.getMaxPrice(); }
    public boolean isInStockOnly() { return search.isInStockOnly(); }
    public int activeFilterCount() { return search.activeFilterCount(); }
    public boolean hasQueryOrFilter() { return search.hasQueryOrFilter(); }
    public int getShownCount() { return shown.size(); }

    private void apply() {
        shown.clear();
        shown.addAll(search.apply(all));
        notifyDataSetChanged();
    }

    // ---------- RecyclerView ----------

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.dong_search_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = shown.get(position);
        holder.tvName.setText(product.getTensp());
        holder.tvMota.setText(product.getMota());
        holder.tvGia.setText(NumberFormat.getInstance().format(product.getGiatien()));
        Picasso.get().load(product.getHinhanh()).into(holder.img);

        String loai = product.getLoaisp();
        holder.tvLoai.setVisibility(loai == null || loai.isEmpty() ? View.GONE : View.VISIBLE);
        holder.tvLoai.setText(loai);

        long sold = search.soldOf(product);
        holder.tvDaBan.setVisibility(sold > 0 ? View.VISIBLE : View.GONE);
        holder.tvDaBan.setText("Đã bán " + NumberFormat.getInstance().format(sold));

        boolean outOfStock = product.getSoluong() <= 0;
        holder.tvHetHang.setVisibility(outOfStock ? View.VISIBLE : View.GONE);
        holder.itemView.setAlpha(outOfStock ? 0.6f : 1f);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvMota, tvGia, tvLoai, tvDaBan, tvHetHang;
        final ImageView img;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name_search);
            tvMota = itemView.findViewById(R.id.tv_mota_search);
            tvGia = itemView.findViewById(R.id.tv_gia_search);
            img = itemView.findViewById(R.id.img_seach);
            tvLoai = itemView.findViewById(R.id.tv_loai_search);
            tvDaBan = itemView.findViewById(R.id.tv_daban_search);
            tvHetHang = itemView.findViewById(R.id.tv_hethang_search);
        }
    }
}
