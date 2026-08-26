package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * Xác nhận file đã upload xong cho lesson VIDEO/FILE. BE sẽ HEAD lại object để đối
 * chiếu size/type thực tế với khai báo (chống khai gian).
 */
@Data
public class AttachLessonContentRequest {

    @NotBlank(message = "storageKey is required")
    private String storageKey;

    @NotBlank(message = "originalFileName is required")
    private String originalFileName;

    @NotNull(message = "fileSizeBytes is required")
    @PositiveOrZero(message = "fileSizeBytes must be >= 0")
    private Long fileSizeBytes;

    @NotBlank(message = "mimeType is required")
    private String mimeType;

    /** Thời lượng video (giây) — chỉ dùng cho lesson VIDEO. */
    @PositiveOrZero(message = "durationSeconds must be >= 0")
    private Integer durationSeconds;
}
