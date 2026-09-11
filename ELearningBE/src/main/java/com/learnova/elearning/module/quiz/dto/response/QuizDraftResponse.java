package com.learnova.elearning.module.quiz.dto.response;

import com.learnova.elearning.module.quiz.entity.enums.QuizDifficulty;

import java.math.BigDecimal;
import java.util.List;

public record QuizDraftResponse(
        BigDecimal totalPoints,
        QuizDifficulty difficulty,
        List<QuizQuestionDraft> questions
) {
}
