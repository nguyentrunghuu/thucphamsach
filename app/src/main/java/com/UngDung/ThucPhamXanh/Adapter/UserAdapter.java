package com.UngDung.ThucPhamXanh.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.UngDung.ThucPhamXanh.Helper.ChatFirestoreHelper;
import com.UngDung.ThucPhamXanh.Models.User;
import com.UngDung.ThucPhamXanh.R;
import com.UngDung.ThucPhamXanh.my_interface.IClickCTHD;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder>{

    private Context context;
    private List<User> mListUser;
    private boolean ischat;
    private IClickCTHD iClickCTHD;

    String theLastMessage;

    public UserAdapter(Context context, List<User> mListUser, boolean ischat, IClickCTHD iClickCTHD) {
        this.context = context;
        this.mListUser = mListUser;
        this.ischat = ischat;
        this.iClickCTHD = iClickCTHD;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        User user = mListUser.get(position);
        
        // Hiển thị tên, nếu không có tên thì hiển thị email
        String displayName = user.getName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = user.getEmail();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = "User"; // Fallback nếu cả tên và email đều không có
            }
        }
        holder.tvItemUsername.setText(displayName);


        if (user.getAvatar().equals("default")){
            holder.imgItemUser.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(context).load(user.getAvatar()).into(holder.imgItemUser);
        }

        if (ischat){
            lastMessage(user.getIduser(), holder.tvLastMessage);
        } else {
            holder.tvLastMessage.setVisibility(View.GONE);
        }

        if (ischat){
            if (user.getStatus().equals("online")){
                holder.imgOn.setVisibility(View.VISIBLE);
                holder.imgOff.setVisibility(View.GONE);
            } else {
                holder.imgOn.setVisibility(View.GONE);
                holder.imgOff.setVisibility(View.VISIBLE);
            }
        } else {
            holder.imgOn.setVisibility(View.GONE);
            holder.imgOff.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iClickCTHD.onClickCTHD(position);

            }
        });
    }

    @Override
    public int getItemCount() {
        return mListUser.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        private final TextView tvItemUsername;
        private final ImageView imgItemUser;
        private ImageView imgOn, imgOff;
        private TextView tvLastMessage;

        public ViewHolder(View itemView) {
            super(itemView);

            tvItemUsername = itemView.findViewById(R.id.tv_item_username);
            imgItemUser = itemView.findViewById(R.id.img_item_user);
            imgOn = itemView.findViewById(R.id.img_on);
            imgOff = itemView.findViewById(R.id.img_off);
            tvLastMessage = itemView.findViewById(R.id.tv_lass_message);
        }
    }

    // Check for last message từ Firestore
    private void lastMessage(String userid, TextView last_msg){
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            last_msg.setText("No Message");
            return;
        }
        
        String myId = currentUser.getUid();
        ChatFirestoreHelper chatHelper = new ChatFirestoreHelper();
        
        chatHelper.getLastMessage(myId, userid, lastMessage -> {
            if (lastMessage != null && !lastMessage.equals("No Message") && !lastMessage.isEmpty()) {
                last_msg.setText(lastMessage);
            } else {
                last_msg.setText("No Message");
            }
        });
    }
}
