package com.learnova.elearning.module.quiz.dto;

import com.learnova.elearning.module.quiz.dto.response.QuizAttemptResponse.QuestionResultItem;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** Immutable-at-write grading details stored with each attempt. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttemptSnapshot {
    private Integer version;
    private BigDecimal passingScore;
    private Integer totalQuestions;
    private Integer correctQuestions;
    private List<QuestionResultItem> questionResults;
}
