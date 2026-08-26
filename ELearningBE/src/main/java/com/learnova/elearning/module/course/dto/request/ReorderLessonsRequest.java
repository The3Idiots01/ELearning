package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** Danh sách toàn bộ lessonId của section theo thứ tự mới mong muốn. */
@Data
public class ReorderLessonsRequest {

    @NotEmpty(message = "lessonIds is required")
    private List<Long> lessonIds;
}
