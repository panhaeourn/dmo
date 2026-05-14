package com.example.demo.dto;

import java.util.List;

public record AiChatHistoryResponse(List<AiChatRequest.ChatMessage> messages) {
}
