package com.learnova.elearning.module.enrollment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {
    private Long lessonId;
    private boolean completed;
    private BigDecimal totalProgress;
    private String courseStatus;
}
