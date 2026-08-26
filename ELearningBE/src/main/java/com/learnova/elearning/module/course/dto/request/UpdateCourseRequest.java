package com.learnova.elearning.module.course.dto.request;

import com.learnova.elearning.module.course.entity.enums.CourseLevel;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Sửa thông tin landing page. Mọi field optional — chỉ field khác null mới cập nhật
 * (partial update). version optional: nếu gửi kèm sẽ check optimistic lock (7.6).
 */
@Data
public class UpdateCourseRequest {

    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    @Size(max = 255, message = "subtitle must not exceed 255 characters")
    private String subtitle;

    private String description;

    private Long categoryId;

    private CourseLevel level;

    @Size(max = 20, message = "language must not exceed 20 characters")
    private String language;

    private Long version;
}
