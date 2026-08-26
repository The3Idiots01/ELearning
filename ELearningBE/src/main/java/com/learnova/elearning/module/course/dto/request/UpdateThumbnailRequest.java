package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateThumbnailRequest {

    /** storageKey đã upload xong qua /uploads/presign (purpose=COURSE_THUMBNAIL). */
    @NotBlank(message = "storageKey is required")
    private String storageKey;
}
