package com.UngDung.ThucPhamXanh.View.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.R;
import com.squareup.picasso.Picasso;

import java.util.Objects;

/**
 * Danh mục sản phẩm trang admin. Bấm thẻ → sửa; nút thùng rác → xóa (Activity hỏi xác nhận và chặn
 * khi danh mục còn sản phẩm).
 */
public class AdminLoaiSPAdapter extends ListAdapter<AdminLoaiSPAdapter.Row, AdminLoaiSPAdapter.ViewHolder> {

    /** Một danh mục: id document, tên, ảnh, số sản phẩm đang bán thuộc danh mục. */
    public static final class Row {
        public final String id;
        public final String name;
        public final String image;
        public final int productCount;

        public Row(String id, String name, String image, int productCount) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.image = image == null ? "" : image;
            this.productCount = productCount;
        }
    }

    public interface Listener {
        void onEdit(Row row);

        void onDelete(Row row);
    }

    private final Listener listener;

    public AdminLoaiSPAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_loai_sp, parent, false);
        ViewHolder h = new ViewHolder(v);
        v.setOnClickListener(x -> {
            int pos = h.getAdapterPosition(); // recyclerview 1.1
            if (pos != RecyclerView.NO_POSITION) listener.onEdit(getItem(pos));
        });
        h.btnDelete.setOnClickListener(x -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onDelete(getItem(pos));
        });
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Row r = getItem(position);
        h.name.setText(r.name);
        h.count.setText(r.productCount > 0 ? r.productCount + " sản phẩm" : "Chưa có sản phẩm");
        if (!r.image.trim().isEmpty()) {
            Picasso.get().load(r.image.trim()).placeholder(R.drawable.logo_new).error(R.drawable.logo_new).into(h.img);
        } else {
            Picasso.get().cancelRequest(h.img);
            h.img.setImageResource(R.drawable.logo_new);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView img;
        final TextView name, count;
        final ImageButton btnDelete;

        ViewHolder(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.img_loai_sp);
            name = v.findViewById(R.id.tv_ten_loai_sp);
            count = v.findViewById(R.id.tv_loai_sp_count);
            btnDelete = v.findViewById(R.id.btn_delete_loai_sp);
        }
    }

    private static final DiffUtil.ItemCallback<Row> DIFF = new DiffUtil.ItemCallback<Row>() {
        @Override
        public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
            return Objects.equals(a.id, b.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
            return a.name.equals(b.name) && a.image.equals(b.image) && a.productCount == b.productCount;
        }
    };
}
