package com.UngDung.ThucPhamXanh.BotAI;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.UngDung.ThucPhamXanh.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatBotAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;

    private Context context;
    private List<ChatMessage> chatMessages;

    public ChatBotAdapter(Context context, List<ChatMessage> chatMessages) {
        this.context = context;
        this.chatMessages = chatMessages;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(context).inflate(R.layout.chat_item_right, parent, false);
            // Tin nhắn người dùng: chừa lề trái 48dp, bong bóng tối đa ~ phần còn lại
            view.setPadding(dp(56), view.getPaddingTop(), dp(12), view.getPaddingBottom());
            ((TextView) view.findViewById(R.id.tv_show_msg)).setMaxWidth(screenWidth - dp(48 + 12 + 8));
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.chat_item_left, parent, false);
            // Tin nhắn bot: kéo bong bóng gần hết chiều rộng màn hình
            // (trừ avatar 40dp + lề 12dp hai bên + khoảng cách 8dp/4dp)
            view.setPadding(dp(12), view.getPaddingTop(), dp(12), view.getPaddingBottom());
            ((TextView) view.findViewById(R.id.tv_show_msg)).setMaxWidth(screenWidth - dp(12 + 32 + 8 + 12));
            return new BotMessageViewHolder(view);
        }
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    /** Chuyển Markdown đơn giản của AI (**đậm**, gạch đầu dòng "* ") thành chữ định dạng. */
    static CharSequence formatBotMessage(String raw) {
        if (raw == null) return "";
        String html = android.text.TextUtils.htmlEncode(raw)
                .replaceAll("(?m)^\\s*[*-]\\s+", "• ")                 // "* item" -> "• item"
                .replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>")          // **đậm**
                .replaceAll("(?m)^#{1,6}\\s*(.+)$", "<b>$1</b>")       // # Tiêu đề
                .replace("\n", "<br>");
        return android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);

        if (holder.getItemViewType() == VIEW_TYPE_USER) {
            ((UserMessageViewHolder) holder).bind(message);
        } else {
            ((BotMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return chatMessages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView tvSeen;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.tv_show_msg);
            tvSeen = itemView.findViewById(R.id.tv_seen);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.getMessage());
            // Seen status is hidden by default in the layout
        }
    }

    static class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView tvSeen;
        CircleImageView imgChat;

        BotMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.tv_show_msg);
            tvSeen = itemView.findViewById(R.id.tv_seen);
            imgChat = itemView.findViewById(R.id.img_chat);
        }

        void bind(ChatMessage message) {
            textMessage.setText(formatBotMessage(message.getMessage()));
            // Bot's avatar is set in the layout as the app icon
            // Seen status is hidden by default in the layout
        }
    }
}