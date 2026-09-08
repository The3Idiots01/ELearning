package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateLearningOutcomeRequest {
    @NotBlank(message = "statement is required")
    @Size(max = 500, message = "statement must not exceed 500 characters")
    private String statement;
}
