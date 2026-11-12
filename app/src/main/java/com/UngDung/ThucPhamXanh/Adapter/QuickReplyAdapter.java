package com.UngDung.ThucPhamXanh.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.R;

import java.util.List;

public class QuickReplyAdapter extends RecyclerView.Adapter<QuickReplyAdapter.ViewHolder> {

    private List<String> quickReplies;
    private OnQuickReplyClickListener listener;

    public interface OnQuickReplyClickListener {
        void onQuickReplyClick(String message);
    }

    public QuickReplyAdapter(List<String> quickReplies, OnQuickReplyClickListener listener) {
        this.quickReplies = quickReplies;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_reply, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String reply = quickReplies.get(position);
        holder.tvQuickReply.setText(reply);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuickReplyClick(reply);
            }
        });
    }

    @Override
    public int getItemCount() {
        return quickReplies != null ? quickReplies.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuickReply;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuickReply = itemView.findViewById(R.id.tv_quick_reply);
        }
    }
}

