package com.UngDung.ThucPhamXanh.BotAI;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

// Request class for OpenAI API
public class OpenAIRequest {
    @SerializedName("model")
    private String model = "gpt-4o-mini";
    
    @SerializedName("messages")
    private List<Message> messages;
    
    @SerializedName("temperature")
    private double temperature = 0.7;

    public OpenAIRequest(String userMessage) {
        this.messages = new ArrayList<>();
        // Add system message to guide the AI
        this.messages.add(new Message("system",
                "Bạn là trợ lý ảo của cửa hàng thực phẩm, giúp khách hàng tư vấn về sản phẩm, đơn hàng. " +
                        "Trả lời ngắn gọn, lịch sự, và thân thiện. Sử dụng tiếng Việt. " +
                        "Nếu được hỏi về sản phẩm cụ thể mà bạn không biết, hãy đề nghị khách hàng mô tả rõ hơn hoặc xem danh sách sản phẩm."));
        this.messages.add(new Message("user", userMessage));
    }

    public void setModel(String model) {
        this.model = model;
    }

    public static class Message {
        @SerializedName("role")
        private String role;
        
        @SerializedName("content")
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}

