package com.example.demo.controller;

import com.example.demo.dto.AiChatRequest;
import com.example.demo.service.OpenAiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final OpenAiService openAiService;

    public AiController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @PostMapping(value = "/chat", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> chat(@RequestBody AiChatRequest request) {
        try {
            return ResponseEntity.ok(openAiService.chat(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ApiError(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(ex.getMessage()));
        }
    }

    public record ApiError(String message) {
    }
}
