package com.learnova.elearning.module.review.moderation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Lớp 2: Kiểm duyệt ngữ cảnh bằng AI (Google Gemini API).
 * Nhận diện từ lóng, nói giảm nói tránh, gạ tình, nhận xét khiếm nhã ngoại hình giảng viên.
 * Có cơ chế Graceful Degradation: Nếu không có API key hoặc mạng lỗi, tự động bỏ qua an toàn.
 */
@Service
@Slf4j
public class AiContentModerationService {

    @Value("${gemini.api-key:${GEMINI_API_KEY:}}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.6-flash}")
    private String geminiModel;

    private final Environment environment;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AiContentModerationService(ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.environment = environment;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(4));
        requestFactory.setReadTimeout(Duration.ofSeconds(6));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public ProfanityFilterService.ModerationCheckResult check(String text) {
        if (text == null || text.isBlank() || text.length() < 3) {
            return ProfanityFilterService.ModerationCheckResult.pass();
        }

        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is not configured or blank, skipping AI moderation layer.");
            return ProfanityFilterService.ModerationCheckResult.pass();
        }

        try {
            String prompt = """
                    Bạn là hệ thống kiểm duyệt nội dung (Content Moderation) cho nền tảng học trực tuyến E-Learning.
                    Hãy đánh giá xem nội dung đánh giá khóa học sau đây có vi phạm một trong các tiêu chuẩn cộng đồng này không:
                    1. Ngôn từ tục tĩu, chửi thề, lăng mạ, thù ghét hoặc công kích cá nhân (bằng tiếng Việt hoặc tiếng Anh).
                    2. Nội dung tình dục, 18+, chat sex, gạ gẫm, mại dâm, gợi dục hoặc nhận xét khiếm nhã về ngoại hình/cơ thể của giảng viên hoặc học viên khác (ví dụ: 'nhìn ngon', 'múp', 'muốn húp', 'body mướt', 'xinh thế làm quen đi').
                    3. Spam link lạ, lừa đảo, cờ bạc cá độ trực tuyến, chất cấm.

                    Nội dung cần kiểm duyệt:
                    \"\"\"%s\"\"\"

                    CHỈ phản hồi duy nhất 1 JSON object hợp lệ theo cấu trúc sau (không kèm markdown):
                    {"allowed": true, "reason": null}
                    hoặc
                    {"allowed": false, "reason": "Mô tả ngắn gọn lý do vi phạm bằng tiếng Việt"}
                    """.formatted(text.replace("\"", "\\\""));

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.0,
                            "responseMimeType", "application/json"
                    )
            );

            String activeModel = (geminiModel != null && !geminiModel.isBlank()) ? geminiModel.trim() : "gemini-3.6-flash";
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + activeModel + ":generateContent?key=" + apiKey;

            log.info("Sending review text to Gemini AI moderation (model: {}): \"{}\"", activeModel, text);

            String responseString = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseString == null || responseString.isBlank()) {
                return ProfanityFilterService.ModerationCheckResult.pass();
            }

            JsonNode root = objectMapper.readTree(responseString);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                if (!textNode.isMissingNode()) {
                    String jsonText = textNode.asText().trim();
                    // Loại bỏ format markdown nếu model trả về ```json ... ```
                    if (jsonText.startsWith("```json")) {
                        jsonText = jsonText.substring(7);
                    } else if (jsonText.startsWith("```")) {
                        jsonText = jsonText.substring(3);
                    }
                    if (jsonText.endsWith("```")) {
                        jsonText = jsonText.substring(0, jsonText.length() - 3);
                    }
                    jsonText = jsonText.trim();

                    JsonNode evalResult = objectMapper.readTree(jsonText);
                    boolean isAllowed = evalResult.path("allowed").asBoolean(true);
                    String reason = evalResult.path("reason").asText(null);

                    if (!isAllowed) {
                        log.warn("Gemini AI Moderation flagged content as prohibited: {}", reason);
                        return ProfanityFilterService.ModerationCheckResult.fail(
                                (reason != null && !reason.isBlank()) ? reason : "Nội dung vi phạm tiêu chuẩn cộng đồng."
                        );
                    }
                }
            }

            log.info("Gemini AI Moderation passed content: \"{}\"", text);
            return ProfanityFilterService.ModerationCheckResult.pass();
        } catch (Exception e) {
            log.error("Error or timeout during Gemini AI moderation call: {}. Gracefully falling back to allow.", e.getMessage(), e);
            return ProfanityFilterService.ModerationCheckResult.pass();
        }
    }

    private String resolveApiKey() {
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            return geminiApiKey.trim();
        }
        if (environment != null) {
            String envProp = environment.getProperty("gemini.api-key");
            if (envProp != null && !envProp.isBlank()) {
                return envProp.trim();
            }
            String envKey = environment.getProperty("GEMINI_API_KEY");
            if (envKey != null && !envKey.isBlank()) {
                return envKey.trim();
            }
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        return null;
    }
}
