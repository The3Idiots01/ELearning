package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceAssessmentRequest {
    private Long sectionId;

    @NotNull(message = "position is required")
    @Min(value = 0, message = "position must be at least 0")
    private Integer position;
}
