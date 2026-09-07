package com.learnova.elearning.module.revenue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorRevenueResponse {

    @Builder.Default
    private String currency = "VND";

    private BigDecimal totalRevenue;

    private List<CourseRevenueItem> courses;
}
