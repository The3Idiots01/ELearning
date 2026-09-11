package com.learnova.elearning.module.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseReviewSummaryResponse {

    private Long courseId;
    private BigDecimal ratingAvg;
    private Long totalReviews;
    private Map<Integer, Long> ratingCounts;
    private Map<Integer, Double> ratingPercentages;
}
