package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCourseRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    /** Có thể để trống khi mới tạo nháp, gán category sau. */
    private Long categoryId;
}
