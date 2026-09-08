package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Partial update lesson plan và mapping outcome.
 */
@Data
public class UpdateLessonRequest {

    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    private Boolean isPreview;

    private List<Long> outcomeIds;
}
