package com.UngDung.ThucPhamXanh.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Models.Product;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.View.CartActivity;
import com.UngDung.ThucPhamXanh.fragment.CartFragment;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.ArrayList;

public class GiohangAdapter extends RecyclerView.Adapter<GiohangAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Product> mListGiohang;
    private int number;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Fragment fragment;
    private CartFragment cartFragment = new CartFragment();
    private CartActivity cartActivity;


    public GiohangAdapter(Context context, ArrayList<Product> mListGiohang, Fragment fragment) {
        this.context = context;
        this.mListGiohang = mListGiohang;
        this.fragment = fragment;
    }

    public GiohangAdapter(Context context, ArrayList<Product> mListGiohang, CartActivity cartActivity) {
        this.context = context;
        this.mListGiohang = mListGiohang;
        this.cartActivity = cartActivity;
    }

    public void updateListGioHang(ArrayList<Product> newArr){
        this.mListGiohang.clear();
        for (int i = 0; i < newArr.size(); i++)
            this.mListGiohang.add(newArr.get(i));
        this.notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(context).inflate(R.layout.dong_giohang, parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {

        Product product = mListGiohang.get(position);
        holder.tvTenGiohang.setText(product.getTensp());
        holder.tvGiatienGiohang.setText(NumberFormat.getInstance().format(product.getGiatien()));
        holder.tvNumberGiohang.setText(String.valueOf(product.getSoluong()));

        holder.btnMinusGiohang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                long current = product.getSoluong();
                if (current <= 1) {
                    // Không cho giảm xuống 0 / số âm: hỏi người dùng có muốn xóa sản phẩm không
                    if (cartActivity != null) cartActivity.confirmRemoveItem(pos);
                    return;
                }
                updateQuantity(holder, product, current - 1);
            }
        });
        holder.btnPlusGiohang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long current = product.getSoluong();
                updateQuantity(holder, product, current + 1);
            }
        });

        holder.btnMinusGiohang.setVisibility(View.VISIBLE);
        holder.tvTrongluongGiohang.setText(product.getTrongluong());
        showLineTotal(holder, product);
        Picasso.get().load(product.getHinhanh()).into(holder.imgGiohang);

    }

    /** Cập nhật số lượng (luôn >= 1) trên giao diện và trong Firestore. */
    private void updateQuantity(ViewHolder holder, Product product, long newQuantity) {
        if (newQuantity < 1) newQuantity = 1;
        product.setSoluong(newQuantity);
        holder.tvNumberGiohang.setText(String.valueOf(newQuantity));
        showLineTotal(holder, product);

        // product.getId() là document ID của sản phẩm trong GioHang/{uid}/ALL
        if (product.getId() != null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            db.collection("GioHang").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .collection("ALL").document(product.getId())
                    .update("soluong", newQuantity)
                    .addOnFailureListener(e -> Log.e("GiohangAdapter", "Lỗi cập nhật số lượng: " + e.getMessage()));
        }
        if (cartActivity != null) cartActivity.TongTienGioHang();
    }

    private void showLineTotal(ViewHolder holder, Product product) {
        long qty = product.getSoluong();
        long price = product.getGiatien();
        holder.tvTotalGiohang.setText(NumberFormat.getInstance().format(qty * price));
    }

    @Override
    public int getItemCount() {
        return mListGiohang.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView tvTenGiohang, tvTrongluongGiohang, tvNumberGiohang, tvGiatienGiohang, tvTotalGiohang;
        private ImageView btnMinusGiohang, btnPlusGiohang, imgGiohang;

        public ViewHolder(@NotNull View itemView) {
            super(itemView);

            tvTenGiohang = itemView.findViewById(R.id.tv_ten_giohang);
            tvTrongluongGiohang = itemView.findViewById(R.id.tv_trongluong_giohang);
            tvNumberGiohang = itemView.findViewById(R.id.tv_number_giohang);
            tvGiatienGiohang = itemView.findViewById(R.id.tv_giatien_giohang);
            tvTotalGiohang = itemView.findViewById(R.id.tv_total_giohang);
            imgGiohang = itemView.findViewById(R.id.img_giohang);
            btnMinusGiohang = itemView.findViewById(R.id.btn_minus_giohang);
            btnPlusGiohang = itemView.findViewById(R.id.btn_plus_giohang);
        }
    }
}
