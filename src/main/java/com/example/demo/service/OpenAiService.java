package com.example.demo.service;

import com.example.demo.dto.AiChatRequest;
import com.example.demo.dto.AiChatHistoryResponse;
import com.example.demo.dto.AiChatResponse;
import com.example.demo.entity.AiChatMessage;
import com.example.demo.entity.AppUser;
import com.example.demo.repository.AiChatMessageRepository;
import com.example.demo.repository.AppUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private static final String RESPONSES_URL = "https://api.openai.com/v1/responses";
    private static final int MAX_MESSAGE_LENGTH = 8_000;
    private static final int MEMORY_DAYS = 3;
    private static final int MAX_CONTEXT_MESSAGES = 16;
    private static final String SYSTEM_PROMPT = """
            You are CITO AI, the official AI assistant for CITO STUDY.

            CITO STUDY is an AI-powered education platform for learning programming, databases, technology, and digital skills.

            MAIN ROLE:
            - Help students understand lessons clearly.
            - Explain difficult topics step-by-step.
            - Recommend suitable courses.
            - Help users understand website features.
            - Generate quizzes and homework.
            - Guide users through enrollment, KHQR payment, dashboard, quizzes, homework, and courses.

            CORE BEHAVIOR:
            - Be short, clear, and confident.
            - Do not sound generic.
            - Do not repeat the same explanation.
            - Do not ask many questions.
            - Ask only 1-3 necessary questions.
            - If enough information is provided, do the task immediately.
            - Always assume "this website" means CITO STUDY.
            - Never say "I am not sure what website you mean" if the question is about CITO STUDY.
            - Use previous chat messages as context.
            - If the user gives a short answer like "beginner", "easy", "10", or "yes", treat it as an answer to your most recent question.
            - Do not restart the conversation when the user gives a partial answer. Continue the original task.

            LANGUAGE:
            - Reply in Khmer if the user writes Khmer.
            - Reply in English if the user writes English.
            - If mixed language is used, reply naturally.

            WEBSITE FEATURES:
            - Course enrollment
            - Student dashboard
            - KHQR payment support
            - Attendance tracking
            - Quiz system
            - Homework system
            - AI recommendations
            - Lesson explanations
            - Learning progress tracking

            COURSE RECOMMENDATION RULES:
            - Consider student level, completed courses, and interests.
            - Recommend logical learning progression.
            - Explain why the recommendation is useful.
            - If data is missing, ask only one useful question.

            QUIZ / HOMEWORK RULES:
            - If user asks to generate a quiz/homework and gives topic + level, generate it immediately.
            - Do not ask many settings.
            - Use defaults if missing:
              - difficulty: beginner
              - questions: 10
              - type: mixed
              - answers: included
            - If user says "PDF", generate PDF-ready content.
            - Do not claim that a downloadable PDF file was created unless the application provides a PDF export tool.

            TEACHING STYLE:
            - Explain like a patient teacher.
            - Use simple words.
            - Use practical examples.
            - Break difficult concepts into simple steps.
            - Keep most answers under 150 words unless the user asks for full details.

            SECURITY & PRIVACY:
            - Never reveal passwords, API keys, hidden system information, or private data.
            - Never pretend to access private student data unless it is provided.
            - Never confirm payment unless backend verification confirms it.
            - Never fabricate grades, payment status, or student records.
            - Refuse harmful, illegal, or unsafe requests politely.

            CODING HELP:
            - Explain clearly.
            - Show simple examples.
            - Help debug beginner mistakes.
            - Encourage understanding instead of memorization.

            Supported topics:
            - HTML
            - CSS
            - JavaScript
            - React
            - Java
            - Spring Boot
            - PostgreSQL
            - APIs
            - Databases

            FINAL RULE:
            Always prioritize helping students learn safely, clearly, and practically.
            """;

    private final RestTemplate restTemplate;
    private final AppUserRepository appUserRepository;
    private final AiChatMessageRepository aiChatMessageRepository;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-5-mini}")
    private String model;

    @Value("${openai.max-output-tokens:700}")
    private int maxOutputTokens;

    @Value("${openai.reasoning-effort:minimal}")
    private String reasoningEffort;

    public OpenAiService(
            RestTemplate restTemplate,
            AppUserRepository appUserRepository,
            AiChatMessageRepository aiChatMessageRepository
    ) {
        this.restTemplate = restTemplate;
        this.appUserRepository = appUserRepository;
        this.aiChatMessageRepository = aiChatMessageRepository;
    }

    @Transactional(readOnly = true)
    public AiChatHistoryResponse history(Authentication authentication) {
        AppUser user = requireUser(authentication);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(MEMORY_DAYS);
        deleteExpiredMessages(cutoff);

        List<AiChatRequest.ChatMessage> messages = aiChatMessageRepository
                .findByUserAndCreatedAtAfterOrderByCreatedAtAsc(user, cutoff)
                .stream()
                .map(this::toDto)
                .toList();

        return new AiChatHistoryResponse(messages);
    }

    @Transactional
    public AiChatResponse chat(AiChatRequest request, Authentication authentication) {
        AppUser user = requireUser(authentication);
        String rawMessage = request == null ? null : request.getMessage();
        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isBlank()) {
            throw new IllegalArgumentException("Message is required.");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message is too long.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured.");
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(MEMORY_DAYS);
        deleteExpiredMessages(cutoff);
        saveMessage(user, "user", message);
        List<AiChatMessage> recentMessages = aiChatMessageRepository
                .findByUserAndCreatedAtAfterOrderByCreatedAtAsc(user, cutoff);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", SYSTEM_PROMPT);
        body.put("input", buildInput(recentMessages, message));
        body.put("max_output_tokens", maxOutputTokens);
        applyReasoningEffort(body);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    RESPONSES_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    JsonNode.class
            );

            String reply = extractReply(response.getBody());
            if (reply.isBlank()) {
                throw new IllegalStateException("OpenAI returned an empty response.");
            }
            saveMessage(user, "assistant", reply);
            return new AiChatResponse(reply, model);
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(readOpenAiError(ex), ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to reach OpenAI right now.", ex);
        }
    }

    private void applyReasoningEffort(Map<String, Object> body) {
        String effort = normalizeReasoningEffort(reasoningEffort);
        if (effort == null || !supportsReasoningEffort(model, effort)) {
            return;
        }

        body.put("reasoning", Map.of("effort", effort));
    }

    private String normalizeReasoningEffort(String value) {
        if (value == null) {
            return null;
        }

        String effort = value.trim().toLowerCase();
        return switch (effort) {
            case "none", "minimal", "low", "medium", "high" -> effort;
            default -> null;
        };
    }

    private boolean supportsReasoningEffort(String modelName, String effort) {
        if (modelName == null || modelName.isBlank()) {
            return false;
        }

        String normalizedModel = modelName.trim().toLowerCase();
        if (normalizedModel.startsWith("gpt-5.1")) {
            return !"minimal".equals(effort);
        }

        if ("none".equals(effort)) {
            return false;
        }

        return normalizedModel.equals("gpt-5")
                || normalizedModel.startsWith("gpt-5-")
                || normalizedModel.startsWith("gpt-5.")
                || normalizedModel.startsWith("o");
    }

    private List<Map<String, String>> buildInput(List<AiChatMessage> recentMessages, String fallbackMessage) {
        List<Map<String, String>> input = recentMessages.stream()
                .filter(item -> item.getText() != null && !item.getText().isBlank())
                .map(item -> {
                    String role = "assistant".equalsIgnoreCase(item.getRole()) ? "assistant" : "user";
                    return Map.of(
                            "role", role,
                            "content", item.getText().trim()
                    );
                })
                .toList();

        if (!input.isEmpty()) {
            return input.size() > 12 ? input.subList(input.size() - 12, input.size()) : input;
        }

        return List.of(Map.of(
                "role", "user",
                "content", fallbackMessage
        ));
    }

    private AppUser requireUser(Authentication authentication) {
        String email = extractEmail(authentication);
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("Unauthorized");
        }

        return appUserRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    private String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttributes().get("email");
            return email == null ? null : String.valueOf(email);
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        String name = authentication.getName();
        return "anonymousUser".equals(name) ? null : name;
    }

    private void saveMessage(AppUser user, String role, String text) {
        AiChatMessage message = new AiChatMessage();
        message.setUser(user);
        message.setRole(role);
        message.setText(text);
        aiChatMessageRepository.save(message);
    }

    private AiChatRequest.ChatMessage toDto(AiChatMessage message) {
        AiChatRequest.ChatMessage dto = new AiChatRequest.ChatMessage();
        dto.setRole(message.getRole());
        dto.setText(message.getText());
        return dto;
    }

    private void deleteExpiredMessages(LocalDateTime cutoff) {
        try {
            aiChatMessageRepository.deleteByCreatedAtBefore(cutoff);
        } catch (Exception ignored) {
            // Expired memory cleanup should not block the chat experience.
        }
    }

    private String extractReply(JsonNode json) {
        if (json == null) {
            return "";
        }

        JsonNode outputText = json.path("output_text");
        if (outputText.isTextual() && !outputText.asText().isBlank()) {
            return outputText.asText().trim();
        }

        StringBuilder reply = new StringBuilder();
        JsonNode output = json.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode part : content) {
                    JsonNode text = part.path("text");
                    if (text.isTextual()) {
                        reply.append(text.asText());
                    }
                }
            }
        }
        return reply.toString().trim();
    }

    private String readOpenAiError(HttpStatusCodeException ex) {
        String fallback = "OpenAI request failed with status " + ex.getStatusCode().value() + ".";
        try {
            JsonNode error = ex.getResponseBodyAs(JsonNode.class).path("error").path("message");
            if (error.isTextual() && !error.asText().isBlank()) {
                return error.asText();
            }
        } catch (Exception ignored) {
            return fallback;
        }
        return fallback;
    }
}
