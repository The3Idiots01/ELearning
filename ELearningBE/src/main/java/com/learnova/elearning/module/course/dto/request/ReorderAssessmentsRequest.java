package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReorderAssessmentsRequest {
    @NotNull(message = "assessmentIds is required")
    private List<@NotNull Long> assessmentIds;
}
