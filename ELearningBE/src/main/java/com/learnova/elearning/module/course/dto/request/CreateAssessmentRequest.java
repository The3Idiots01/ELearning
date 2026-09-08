package com.learnova.elearning.module.course.dto.request;

import com.learnova.elearning.module.course.entity.enums.AssessmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateAssessmentRequest {
    @NotNull(message = "type is required")
    private AssessmentType type;

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    private String instructions;

    @NotNull(message = "outcomeIds is required (empty array allowed for drafts)")
    private List<@NotNull Long> outcomeIds;
}
