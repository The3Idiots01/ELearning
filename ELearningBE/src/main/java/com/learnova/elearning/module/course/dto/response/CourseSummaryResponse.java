package com.learnova.elearning.module.course.dto.response;

import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/** Bản rút gọn cho danh sách course của instructor. */
@Data
@Builder
public class CourseSummaryResponse {

    private Long id;
    private String title;
    private String subtitle;
    private String slug;
    private CourseStatus status;
    private BigDecimal price;
    private String thumbnailUrl;
    private Long categoryId;
    private String categoryName;
    private Integer totalStudents;
    private BigDecimal ratingAvg;
    private Instant updatedAt;
}
