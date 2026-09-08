package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReorderLearningOutcomesRequest {
    @NotNull(message = "outcomeIds is required")
    private List<@NotNull Long> outcomeIds;
}
