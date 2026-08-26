package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Partial update một lesson. contentText chỉ áp dụng cho lesson ARTICLE.
 */
@Data
public class UpdateLessonRequest {

    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    private Boolean isPreview;

    private String contentText;
}
