package com.learnova.elearning.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GeminiClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public GeminiClient(AiProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper,
                RestClient.builder().baseUrl(properties.getBaseUrl()).build());
    }

    GeminiClient(AiProperties properties, ObjectMapper objectMapper, RestClient restClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    public <T> T generateStructured(String prompt, Map<String, Object> jsonSchema, Class<T> responseType) {
        requireConfigured();

        // Cấu hình chuẩn theo Google Gemini REST API v1beta
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        if (jsonSchema != null) {
            generationConfig.put("responseSchema", jsonSchema);
        }
        generationConfig.put("maxOutputTokens", 8192);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", generationConfig);

        try {
            String responseBody = restClient.post()
                    .uri("/models/{model}:generateContent", properties.getModel())
                    .header("x-goog-api-key", properties.getApiKey())
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode response = responseBody == null || responseBody.isBlank()
                    ? null : objectMapper.readTree(responseBody);

            String text = extractResponseText(response).trim();

            // Xóa bỏ markdown wrapper nếu có
            if (text.startsWith("```json")) {
                text = text.substring(7);
            } else if (text.startsWith("```")) {
                text = text.substring(3);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }

            return objectMapper.readValue(text.trim(), responseType);
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            String providerMessage = providerMessage(ex);
            log.warn("Gemini request failed with HTTP {}: {}", status.value(), providerMessage);
            if (status.value() == 429) {
                throw new AppException(ErrorCode.AI_QUOTA_EXCEEDED, providerMessage, ex);
            }
            throw new AppException(ErrorCode.AI_PROVIDER_ERROR,
                    "Gemini request failed (HTTP " + status.value() + "): " + providerMessage, ex);
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Could not parse Gemini structured response: {}", ex.getMessage());
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID, ex);
        }
    }

    private void requireConfigured() {
        if (!properties.isEnabled() || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AppException(ErrorCode.AI_NOT_CONFIGURED);
        }
    }

    private String extractResponseText(JsonNode response) {
        JsonNode parts = response == null ? null : response.at("/candidates/0/content/parts");
        StringBuilder text = new StringBuilder();
        if (parts != null && parts.isArray()) {
            for (JsonNode part : parts) {
                JsonNode textNode = part.get("text");
                if (textNode != null && textNode.isTextual() && !textNode.asText().isBlank()) {
                    text.append(textNode.asText());
                }
            }
        }
        if (!text.isEmpty()) {
            return text.toString();
        }

        String finishReason = response == null ? "NO_RESPONSE"
                : response.at("/candidates/0/finishReason").asText("UNKNOWN");
        String blockReason = response == null ? ""
                : response.at("/promptFeedback/blockReason").asText("");
        log.warn("Gemini returned no text (finishReason={}, blockReason={}, partCount={})",
                finishReason, blockReason, parts != null && parts.isArray() ? parts.size() : 0);
        throw new AppException(ErrorCode.AI_RESPONSE_INVALID,
                "Gemini returned no JSON output (finishReason=" + finishReason + ")");
    }

    private String providerMessage(RestClientResponseException ex) {
        try {
            JsonNode message = objectMapper.readTree(ex.getResponseBodyAsString()).at("/error/message");
            if (message.isTextual() && !message.asText().isBlank()) {
                String value = message.asText().trim();
                return value.length() <= 500 ? value : value.substring(0, 500);
            }
        } catch (Exception ignored) {
            // Never include request headers or the API key in error details.
        }
        return "No provider error detail";
    }
}