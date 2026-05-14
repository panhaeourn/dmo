package com.example.demo.service;

import com.example.demo.dto.AiChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OpenAiService {

    private static final String RESPONSES_URL = "https://api.openai.com/v1/responses";
    private static final int MAX_MESSAGE_LENGTH = 8_000;

    private final RestTemplate restTemplate;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-5-mini}")
    private String model;

    @Value("${openai.max-output-tokens:700}")
    private int maxOutputTokens;

    public OpenAiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public AiChatResponse chat(String rawMessage) {
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

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", "You are a helpful study assistant for CITO. Answer clearly, keep responses practical, and reply in the user's language when possible.");
        body.put("input", message);
        body.put("max_output_tokens", maxOutputTokens);

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
            return new AiChatResponse(reply, model);
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(readOpenAiError(ex), ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to reach OpenAI right now.", ex);
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
