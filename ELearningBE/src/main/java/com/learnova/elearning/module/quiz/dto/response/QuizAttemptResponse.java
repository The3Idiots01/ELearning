package com.learnova.elearning.module.quiz.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Kết quả lượt nộp bài của học viên.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttemptResponse {
    private Long id;
    private Long quizId;
    private BigDecimal score;
    private BigDecimal passingScore;
    private Boolean isPassed;
    private Integer totalQuestions;
    private Integer correctQuestions;
    private Instant submittedAt;
    private List<QuestionResultItem> questionResults;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionResultItem {
        private Long questionId;
        private Boolean isCorrect;
        private BigDecimal earnedPoints;
        private BigDecimal totalPoints;
        private List<String> selectedOptionIds;
        private List<String> correctOptionIds;
        private String explanation;
    }
}
