package com.learnova.elearning.module.recommendation.engine;

import com.learnova.elearning.integration.ai.GeminiClient;
import com.learnova.elearning.module.recommendation.dto.request.AiRecommendationRequest;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiRecommendationEngine {

    private final GeminiClient geminiClient;

    @Data
    public static class RawAiRecommendationResult {
        private String summaryAdvice;
        private List<RawAiCourseItem> recommendations;
    }

    @Data
    public static class RawAiCourseItem {
        private Long courseId;
        private BigDecimal score;
        private String reason;
    }

    @Data
    @Builder
    public static class CoursePromptContext {
        private Long id;
        private String title;
        private String headline;
        private String category;
        private String level;
        private BigDecimal ratingAvg;
        private Integer totalStudents;
    }

    /**
     * Gọi Gemini AI với Structured JSON Schema để chọn ra Top 3 khóa học phù hợp nhất.
     * Trả về null nếu AI gặp lỗi hoặc quota vượt hạn mức để Service thực hiện fallback.
     */
    public RawAiRecommendationResult getRecommendations(
            AiRecommendationRequest request,
            List<CoursePromptContext> candidateCourses,
            List<String> enrolledCourseTitles
    ) {
        if (candidateCourses == null || candidateCourses.isEmpty()) {
            return null;
        }

        try {
            String prompt = buildPrompt(request, candidateCourses, enrolledCourseTitles);
            Map<String, Object> schema = buildJsonSchema();

            return geminiClient.generateStructured(prompt, schema, RawAiRecommendationResult.class);
        } catch (Exception ex) {
            log.warn("Gemini AI recommendation failed, falling back to system logic: {}", ex.getMessage());
            return null;
        }
    }

    private String buildPrompt(
            AiRecommendationRequest request,
            List<CoursePromptContext> candidates,
            List<String> enrolledCourseTitles
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là chuyên gia tư vấn lộ trình học tập và cố vấn khóa học thông minh của nền tảng Learnova.\n");
        sb.append("Nhiệm vụ của bạn là phân tích hồ sơ và mục tiêu của người học, sau đó chọn ra ĐÚNG TỐI ĐA 3 KHÓA HỌC PHÙ HỢP NHẤT từ danh sách khóa học khả dụng bên dưới.\n\n");

        sb.append("=== THÔNG TIN NGƯỜI HỌC ===\n");
        if (request != null && request.getGoal() != null && !request.getGoal().isBlank()) {
            sb.append("- Mục tiêu nghề nghiệp/học tập: ").append(request.getGoal().trim()).append("\n");
        } else {
            sb.append("- Mục tiêu: Tìm kiếm các khóa học chất lượng cao để nâng cao kỹ năng nghề nghiệp.\n");
        }

        if (request != null && request.getInterests() != null && !request.getInterests().isEmpty()) {
            sb.append("- Chủ đề/kỹ năng quan tâm: ").append(String.join(", ", request.getInterests())).append("\n");
        }

        if (request != null && request.getPreferredLevel() != null) {
            sb.append("- Trình độ mong muốn: ").append(request.getPreferredLevel().name()).append("\n");
        }

        if (enrolledCourseTitles != null && !enrolledCourseTitles.isEmpty()) {
            sb.append("- Các khóa học đã/đang tham gia: ").append(String.join("; ", enrolledCourseTitles)).append("\n");
            sb.append("  (Hãy ưu tiên đề xuất các khóa học bổ trợ hoặc nâng cao tiếp theo, KHÔNG đề xuất lại các khóa đã học)\n");
        }

        sb.append("\n=== DANH SÁCH KHÓA HỌC KHẢ DỤNG ĐỂ LỰA CHỌN ===\n");
        for (CoursePromptContext c : candidates) {
            sb.append(String.format("ID: %d | Tiêu đề: %s | Danh mục: %s | Cấp độ: %s | Đánh giá: %s/5.0 (%d học viên) | Mô tả: %s\n",
                    c.getId(),
                    c.getTitle(),
                    c.getCategory() != null ? c.getCategory() : "Khác",
                    c.getLevel() != null ? c.getLevel() : "All Levels",
                    c.getRatingAvg() != null ? c.getRatingAvg().toString() : "0.0",
                    c.getTotalStudents() != null ? c.getTotalStudents() : 0,
                    c.getHeadline() != null ? c.getHeadline() : ""
            ));
        }

        sb.append("\n=== YÊU CẦU ĐẦU RA ===\n");
        sb.append("1. Chọn ra tối đa 3 khóa học từ danh sách ID ở trên có độ tương thích cao nhất.\n");
        sb.append("2. Chấm điểm độ phù hợp (score) từ 0.50 đến 0.99 cho từng khóa được chọn.\n");
        sb.append("3. Viết lời giải thích (reason) bằng tiếng Việt ngắn gọn, súc tích (1-2 câu), nêu rõ lý do tại sao khóa học này giúp ích trực tiếp cho mục tiêu của học viên.\n");
        sb.append("4. Viết lời khuyên tổng quan (summaryAdvice) bằng tiếng Việt hướng dẫn lộ trình học tập hiệu quả.\n");

        return sb.toString();
    }

    private Map<String, Object> buildJsonSchema() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "summaryAdvice", Map.of(
                                "type", "STRING",
                                "description", "Lời khuyên lộ trình học tập tổng quan cho học viên"
                        ),
                        "recommendations", Map.of(
                                "type", "ARRAY",
                                "description", "Danh sách tối đa 3 khóa học được đề xuất",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "courseId", Map.of("type", "INTEGER"),
                                                "score", Map.of("type", "NUMBER"),
                                                "reason", Map.of("type", "STRING")
                                        ),
                                        "required", List.of("courseId", "score", "reason")
                                )
                        )
                ),
                "required", List.of("recommendations", "summaryAdvice")
        );
    }
}
