package com.UngDung.ThucPhamXanh.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.Helper.ReviewUtil;
import com.UngDung.ThucPhamXanh.Models.Binhluan;
import com.UngDung.ThucPhamXanh.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class AllCommentAdapter extends RecyclerView.Adapter<AllCommentAdapter.ViewHolder>{

    private Context context;
    private ArrayList<Binhluan> mlist;

    public AllCommentAdapter(Context context, ArrayList<Binhluan> mlist) {
        this.context = context;
        this.mlist = mlist;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.dong_all_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {

        Binhluan binhluan = mlist.get(position);
        ReviewUtil.bindReviewer(binhluan.getIduser(), holder.tvNameDongAllCmt, holder.imgDongAllCmt);
        String noidung = binhluan.getNoidung();
        holder.tvNoidungDongAllCmt.setText(noidung == null || noidung.trim().isEmpty() ? "(Không có nhận xét)" : noidung);
        holder.ratingBarDongAllCmt.setRating(ReviewUtil.parseRate(binhluan.getRate()));
        holder.ratingBarDongAllCmt.setIsIndicator(true); //Không cho phép thay đổi giá trị ratingbar
    }

    @Override
    public int getItemCount() {
        return mlist.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private CircleImageView imgDongAllCmt;
        private TextView tvNameDongAllCmt, tvNoidungDongAllCmt;
        private RatingBar ratingBarDongAllCmt;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            imgDongAllCmt = itemView.findViewById(R.id.img_dong_all_cmt);
            tvNameDongAllCmt = itemView.findViewById(R.id.tv_name_dong_all_cmt);
            tvNoidungDongAllCmt = itemView.findViewById(R.id.tv_noidung_dong_all_cmt);
            ratingBarDongAllCmt = itemView.findViewById(R.id.rating_bar_dong_all_cmt);
        }
    }
}
