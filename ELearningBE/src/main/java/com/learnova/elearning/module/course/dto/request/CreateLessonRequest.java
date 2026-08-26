package com.learnova.elearning.module.course.dto.request;

import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Tạo một curriculum item. Nội dung thật (video/file/article text) gắn sau:
 * VIDEO/FILE qua bước upload+confirm (Task 9), ARTICLE qua PATCH contentText.
 */
@Data
public class CreateLessonRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    @NotNull(message = "contentType is required")
    private LessonContentType contentType;
}
