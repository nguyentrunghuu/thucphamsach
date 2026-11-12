package com.UngDung.ThucPhamXanh.BotAI;

import com.google.gson.annotations.SerializedName;

import java.util.List;

// Response class for OpenAI API
public class OpenAIResponse {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    public String getResponseText() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getMessage().getContent();
        }
        return "Xin lỗi, tôi không thể trả lời vào lúc này.";
    }

    public static class Choice {
        private int index;
        @SerializedName("message")
        private Message message;
        @SerializedName("finish_reason")
        private String finishReason;

        public Message getMessage() {
            return message;
        }
    }

    public static class Message {
        private String role;
        private String content;

        public String getContent() {
            return content;
        }
    }

    public static class Usage {
        @SerializedName("prompt_tokens")
        private int promptTokens;
        @SerializedName("completion_tokens")
        private int completionTokens;
        @SerializedName("total_tokens")
        private int totalTokens;
    }
}
