package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** Danh sách toàn bộ sectionId của course theo thứ tự mới mong muốn. */
@Data
public class ReorderSectionsRequest {

    @NotEmpty(message = "sectionIds is required")
    private List<Long> sectionIds;
}
