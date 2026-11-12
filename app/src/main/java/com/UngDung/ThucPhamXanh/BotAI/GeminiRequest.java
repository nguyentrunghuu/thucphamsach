package com.UngDung.ThucPhamXanh.BotAI;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class GeminiRequest {
    @SerializedName("contents")
    private List<Content> contents;

    @SerializedName("system_instruction")
    private SystemInstruction systemInstruction;

    public GeminiRequest(String systemPrompt) {
        this.contents = new ArrayList<>();
        this.systemInstruction = new SystemInstruction(systemPrompt);
    }

    public void addContent(String role, String text) {
        // Gemini uses "user" and "model" roles (OpenAI uses "assistant")
        String geminiRole = role.equals("assistant") ? "model" : "user";
        this.contents.add(new Content(geminiRole, text));
    }

    public static class Content {
        @SerializedName("role")
        private String role;
        @SerializedName("parts")
        private List<Part> parts;

        public Content(String role, String text) {
            this.role = role;
            this.parts = new ArrayList<>();
            this.parts.add(new Part(text));
        }
    }

    public static class SystemInstruction {
        @SerializedName("parts")
        private List<Part> parts;

        public SystemInstruction(String text) {
            this.parts = new ArrayList<>();
            this.parts.add(new Part(text));
        }
    }

    public static class Part {
        @SerializedName("text")
        private String text;

        public Part(String text) {
            this.text = text;
        }
    }
}