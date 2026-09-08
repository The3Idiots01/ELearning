package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateAssessmentRequest {
    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;
    private String instructions;
    private List<Long> outcomeIds;
}
