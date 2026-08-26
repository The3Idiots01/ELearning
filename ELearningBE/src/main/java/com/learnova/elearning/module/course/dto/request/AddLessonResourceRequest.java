package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Thêm một tài liệu đính kèm (đã upload xong) vào lesson. */
@Data
public class AddLessonResourceRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "storageKey is required")
    private String storageKey;

    @NotBlank(message = "originalFileName is required")
    private String originalFileName;

    @NotNull(message = "fileSizeBytes is required")
    @Positive(message = "fileSizeBytes must be positive")
    private Long fileSizeBytes;

    private String mimeType;
}
