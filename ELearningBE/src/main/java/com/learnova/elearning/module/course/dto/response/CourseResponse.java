package com.learnova.elearning.module.course.dto.response;

import com.learnova.elearning.module.course.entity.enums.CourseLevel;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Chi tiết landing page course (kèm 3 khối mô tả). Không lộ storage_key (BR-12). */
@Data
@Builder
public class CourseResponse {

    private Long id;
    private String title;
    private String subtitle;
    private String slug;
    private String description;
    private String thumbnailUrl;
    private String language;
    private CourseLevel level;
    private BigDecimal price;
    private CourseStatus status;

    private Long categoryId;
    private String categoryName;

    private BigDecimal ratingAvg;
    private Integer totalStudents;
    private Long version;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;

    private List<String> learningObjectives;
    private List<String> requirements;
    private List<String> targetAudiences;
}
