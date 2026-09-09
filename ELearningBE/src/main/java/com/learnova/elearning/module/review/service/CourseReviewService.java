package com.learnova.elearning.module.review.service;

import com.learnova.elearning.module.review.dto.request.CreateOrUpdateReviewRequest;
import com.learnova.elearning.module.review.dto.response.CourseReviewResponse;
import com.learnova.elearning.module.review.dto.response.CourseReviewSummaryResponse;
import com.learnova.elearning.module.review.dto.response.InstructorReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseReviewService {

    CourseReviewResponse submitReview(Long courseId, Long userId, CreateOrUpdateReviewRequest request);

    void deleteReview(Long courseId, Long userId);

    CourseReviewResponse getMyReview(Long courseId, Long userId);

    com.learnova.elearning.module.review.dto.response.CourseReviewEligibilityResponse getReviewEligibility(Long courseId, Long userId);

    Page<CourseReviewResponse> getCourseReviews(Long courseId, Integer ratingFilter, Pageable pageable);

    CourseReviewSummaryResponse getCourseReviewSummary(Long courseId);

    Page<InstructorReviewResponse> getInstructorReviews(Long courseId, Long lecturerId, Integer ratingFilter, Pageable pageable);
}
