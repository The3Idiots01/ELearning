package com.learnova.elearning.module.quiz.dto.request;

import com.learnova.elearning.module.quiz.dto.response.QuizQuestionDraft;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ApplyQuizDraftRequest(
        @NotEmpty @Size(max = 30) List<@Valid QuizQuestionDraft> questions
) {
}
