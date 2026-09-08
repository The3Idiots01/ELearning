package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Tạo lesson plan. Content type và nội dung chính được gắn ở attach-content flow.
 */
@Data
public class CreateLessonRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    private List<Long> outcomeIds;
}
