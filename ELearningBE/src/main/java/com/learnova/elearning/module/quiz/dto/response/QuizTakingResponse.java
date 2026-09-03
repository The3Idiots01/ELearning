package com.learnova.elearning.module.quiz.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Đề thi Quiz gửi cho Học viên làm bài.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizTakingResponse {
    private Long id;
    private Long lessonId;
    private String title;
    private BigDecimal passingScore;
    private Integer maxAttempts;
    private Integer attemptsUsed;
    private Integer attemptsRemaining;
    private Boolean hasPassed;
    private List<QuestionTakingResponse> questions;
}
