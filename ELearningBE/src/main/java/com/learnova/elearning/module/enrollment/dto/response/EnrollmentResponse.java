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
public class EnrollmentResponse {
    private Long id;
    private Long studentId;
    private Long courseId;
    private String status;
    private BigDecimal progress;
    private Instant enrolledAt;
    private Instant completedAt;
}
