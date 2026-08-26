package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Partial update — chỉ field khác null mới cập nhật. */
@Data
public class UpdateSectionRequest {

    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    @Size(max = 500, message = "description must not exceed 500 characters")
    private String description;
}
