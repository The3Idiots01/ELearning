package com.learnova.elearning.module.enrollment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrolledCourseResponse {
    private Long enrollmentId;
    private Long courseId;
    private String courseTitle;
    private String courseSlug;
    private String courseThumbnailUrl;
    private String lecturerName;
    private String categoryName;
    private String level;
    private BigDecimal progress;
    private String status;
    private Instant enrolledAt;
    private Instant completedAt;
}
