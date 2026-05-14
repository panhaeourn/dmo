package com.example.demo.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {
    private String message;
    private List<ChatMessage> messages;

    @Data
    public static class ChatMessage {
        private String role;
        private String text;
    }
}
