package com.learnova.elearning.module.course.dto.request;

import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Tạo bài học. Content type có thể được chọn trước; nội dung chính vẫn được gắn
 * ở attach-content flow.
 */
@Data
public class CreateLessonRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    private LessonContentType contentType;

    private List<Long> outcomeIds;
}
