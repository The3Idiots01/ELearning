package com.learnova.elearning.module.quiz.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Chi tiết bài Quiz dành cho Giảng viên quản trị nội dung.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizDetailResponse {
    private Long id;
    private Long lessonId;
    private String title;
    private BigDecimal passingScore;
    private Integer maxAttempts;
    private BigDecimal totalPoints;
    private List<QuestionDetailResponse> questions;
    private Instant createdAt;
    private Instant updatedAt;
}
