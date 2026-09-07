package com.learnova.elearning.module.revenue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRevenueItem {

    private Long courseId;
    private String courseTitle;
    private BigDecimal revenue;
}
