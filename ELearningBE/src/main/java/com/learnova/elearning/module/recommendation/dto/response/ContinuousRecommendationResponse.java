package com.learnova.elearning.module.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContinuousRecommendationResponse {

    /** Tối đa 5 khóa học được đề xuất liên tục */
    private List<RecommendedCourseResponse> items;

    /** Chiến lược gợi ý: RELATED_COURSE, LEARNER_PERSONALIZED, CATEGORY_TRENDING, POPULAR_DISCOVERY */
    private String strategy;

    /** Tổng số khóa học được đề xuất */
    private int total;
}
