package com.learnova.elearning.module.quiz.dto.request;

import com.learnova.elearning.module.quiz.entity.enums.QuestionType;
import com.learnova.elearning.module.quiz.entity.enums.QuizDifficulty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Optional lecturer constraints. Null values are intentionally left for AI to suggest. */
public record GenerateQuizDraftRequest(
        @Min(1) @Max(30) Integer questionCount,
        @DecimalMin("0.01") @DecimalMax("1000.00") BigDecimal totalPoints,
        QuizDifficulty difficulty,
        QuestionType questionType,
        @Size(max = 12000) String contentGuidance,
        @Size(max = 2000) String questionGuidance
) {
}
