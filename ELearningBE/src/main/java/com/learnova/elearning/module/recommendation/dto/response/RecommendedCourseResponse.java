package com.learnova.elearning.module.recommendation.dto.response;

import com.learnova.elearning.module.course.dto.response.CourseSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedCourseResponse {

    /** Thông tin cơ bản của khóa học */
    private CourseSummaryResponse course;

    /** Điểm số phù hợp từ 0.00 đến 1.00 */
    private BigDecimal matchScore;

    /** Lý do đề xuất (do AI hoặc quy tắc hệ thống sinh ra) */
    private String recommendationReason;

    /** Nguồn gốc đề xuất: "SYSTEM_RULE" hoặc "GEMINI_AI" */
    private String recommendationType;
}
