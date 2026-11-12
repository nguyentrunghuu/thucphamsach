package com.UngDung.ThucPhamXanh.BotAI;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class OpenAIRequestWithHistory {
    @SerializedName("model")
    private String model = "gpt-4o-mini";

    @SerializedName("messages")
    private List<Message> messages;

    @SerializedName("temperature")
    private double temperature = 0.7;

    @SerializedName("max_tokens")
    private int maxTokens = 500;

    public OpenAIRequestWithHistory() {
        this.messages = new ArrayList<>();
    }

    public void addMessage(String role, String content) {
        this.messages.add(new Message(role, content));
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

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}