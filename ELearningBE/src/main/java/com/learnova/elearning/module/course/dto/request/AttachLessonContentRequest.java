package com.learnova.elearning.module.course.dto.request;

import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * Gắn content chính cho lesson. VIDEO/FILE cần metadata object; ARTICLE nhận text.
 */
@Data
public class AttachLessonContentRequest {

    @NotNull(message = "contentType is required")
    private LessonContentType contentType;

    private String storageKey;

    private String originalFileName;

    @PositiveOrZero(message = "fileSizeBytes must be >= 0")
    private Long fileSizeBytes;

    private String mimeType;

    private String contentText;

    /** Thời lượng video (giây) — chỉ dùng cho lesson VIDEO. */
    @PositiveOrZero(message = "durationSeconds must be >= 0")
    private Integer durationSeconds;
}
