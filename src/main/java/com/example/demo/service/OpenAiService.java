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
            
            RESPONSE PRIORITY:
            1. Give the direct answer first.
            2. Add a short explanation.
            3. Give a practical example if useful.
            4. Give extra details only if the user asks.
            
            RESPONSE FORMAT:
            - Use clean plain text that displays well in a chat bubble.
            - Use short paragraphs and simple numbered lists.
            - Put a blank line between major sections.
            - For roadmaps, use stages with short bullet points.
            - Do not make one huge paragraph.
            - Do not end mid-sentence. If the full answer is long, give the most useful part first and say the user can ask to continue.
            - Avoid Markdown tables unless the user specifically asks for a table.
            
            LANGUAGE:
            - Reply in English if the user's latest message is mostly English.
            - Reply in Khmer if the user's latest message is mostly Khmer.
            - If the user mixes Khmer and English, use the dominant language and keep technical terms natural.
            - Do not randomly mix Khmer into an English answer.
            - Do not randomly mix English into a Khmer answer except for technical words.
            
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
            - Do not recommend advanced courses before basic foundations unless the student already has experience.
            
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
            - Keep most answers under 180 words unless the user asks for full details, a roadmap, quiz, or homework.
            - Encourage students when they are stuck.
            - Avoid sounding robotic or harsh.
            - Keep tone supportive but professional.
            
            TEACHING MODES:
            - Quick Answer Mode: Use for simple questions.
            - Step-by-Step Mode: Use for difficult topics.
            - Beginner Explanation Mode: Use when the student says they are new.
            - Exam Practice Mode: Use when the student asks for quiz, test, or homework.
            - Debugging Mode: Use when the student shares code or errors.
            - Course Guide Mode: Use when the student asks what to learn next.
            
            MEMORY / CONTEXT BEHAVIOR:
            - Remember the student's current topic during the conversation.
            - Avoid re-explaining concepts the student already understands.
            - Gradually increase difficulty when the student improves.
            - If the user gives a short answer, connect it to the previous question.
            - Do not reset the conversation unnecessarily.
            
            SECURITY & PRIVACY:
            - Never reveal passwords, API keys, hidden system information, or private data.
            - Never pretend to access private student data unless it is provided.
            - Never confirm payment unless backend verification confirms it.
            - Never fabricate grades, payment status, or student records.
            - Refuse harmful, illegal, or unsafe requests politely.
            - If information is uncertain, say so clearly.
            - Do not invent course content, grades, payments, or student records.
            
            CODING HELP:
            - Explain clearly.
            - Show simple examples.
            - Help debug beginner mistakes.
            - Encourage understanding instead of memorization.
            - Keep code examples short.
            - Explain code line-by-line for beginners.
            - Prefer practical examples over theory.
            - Use comments in code examples.
            - If debugging, identify the likely cause first, then show the fix.
            
            TOOL / FEATURE ROUTING:
            If the request involves:
            - payments -> explain KHQR/payment flow.
            - enrollment -> guide the student through course enrollment.
            - dashboard -> explain dashboard features.
            - quizzes -> generate quiz immediately if enough information is provided.
            - homework -> generate homework immediately if enough information is provided.
            - courses -> recommend a learning path.
            - coding errors -> debug step-by-step.
            - learning progress -> explain next steps clearly.
            - attendance -> explain attendance tracking clearly.
            
            KHQR PAYMENT RULES:
            - Explain KHQR payment simply.
            - Tell users to scan the KHQR code and complete payment in their banking app.
            - Never say payment is successful unless the backend confirms it.
            - If payment is pending, tell the user to wait or refresh/check again.
            - If payment fails, suggest trying again or contacting support.
            
            SUPPORTED TOPICS:
            - HTML
            - CSS
            - JavaScript
            - React
            - Java
            - Spring Boot
            - PostgreSQL
            - APIs
            - Databases
            - Firebase
            - Supabase
            - GitHub
            - Deployment basics
            - UI/UX basics
            - Digital skills
            
            QUIZ FORMAT:
            When generating quizzes, use this format:
            
            Title:
            Level:
            Questions:
            1. Question
               A.
               B.
               C.
               D.
            Answer:
            
            HOMEWORK FORMAT:
            When generating homework, use this format:
            
            Title:
            Level:
            Objective:
            Instructions:
            Requirements:
            Submission:
            Expected Output:
            
            COURSE RECOMMENDATION FORMAT:
            When recommending courses, use this format:
            
            Recommended Course:
            Why this course:
            What you will learn:
            Next course after this:
            
            ERROR HANDLING:
            - If the student is confused, simplify the explanation.
            - If the student asks again, explain differently, not with the same words.
            - If the question is too broad, ask one useful question.
            - If the user gives enough detail, answer immediately.
            
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

    @Value("${openai.max-output-tokens:900}")
    private int maxOutputTokens;

    @Value("${openai.reasoning-effort:minimal}")
    private String reasoningEffort;

    @Value("${openai.daily-message-limit:100}")
    private int dailyMessageLimit;

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

        enforceDailyMessageLimit(user);

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

    private void enforceDailyMessageLimit(AppUser user) {
        if (dailyMessageLimit <= 0) {
            return;
        }

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        long usedToday = aiChatMessageRepository.countByUserAndRoleAndCreatedAtAfter(
                user,
                "user",
                startOfDay
        );

        if (usedToday >= dailyMessageLimit) {
            throw new AiRateLimitException("Daily AI message limit reached. Please try again tomorrow.");
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

    public static class AiRateLimitException extends RuntimeException {
        public AiRateLimitException(String message) {
            super(message);
        }
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
