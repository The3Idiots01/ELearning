package com.learnova.elearning.module.course.dto.request;

import com.learnova.elearning.integration.storage.model.UploadPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PresignUploadRequest {

    @NotNull(message = "purpose is required")
    private UploadPurpose purpose;

    @NotNull(message = "courseId is required")
    private Long courseId;

    /** Bắt buộc với các purpose thuộc lesson (LESSON_*); bỏ trống với COURSE_*. */
    private Long lessonId;

    @NotBlank(message = "fileName is required")
    private String fileName;

    @NotBlank(message = "contentType is required")
    private String contentType;

    @NotNull(message = "sizeBytes is required")
    @Positive(message = "sizeBytes must be positive")
    private Long sizeBytes;
}
