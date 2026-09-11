package com.learnova.elearning.module.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseReviewEligibilityResponse {

    private Boolean isEnrolled;
    private Double currentProgress;
    private Double requiredProgress;
    private Boolean canReview;
    private CourseReviewResponse review;
}
