package com.learnova.elearning.module.quiz.dto.response;

import com.learnova.elearning.module.quiz.dto.QuizOptionDto;
import com.learnova.elearning.module.quiz.entity.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record QuizQuestionDraft(
        @NotBlank @Size(max = 4000) String questionText,
        @NotNull QuestionType questionType,
        @NotNull @DecimalMin("0.01") BigDecimal points,
        @NotEmpty @Size(min = 2, max = 6) List<@Valid QuizOptionDto> options
) {
}
