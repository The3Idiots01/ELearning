package com.learnova.elearning.module.review.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.review.dto.response.CourseReviewSummaryResponse;
import com.learnova.elearning.module.review.dto.response.InstructorReviewResponse;
import com.learnova.elearning.module.review.service.CourseReviewService;
import com.learnova.elearning.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lecturer/courses/{courseId}/reviews")
@RequiredArgsConstructor
public class InstructorReviewController {

    private final CourseReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InstructorReviewResponse>>> getInstructorReviews(
            @PathVariable Long courseId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails lecturer
    ) {
        Page<InstructorReviewResponse> responses = reviewService.getInstructorReviews(
                courseId,
                lecturer.getId(),
                rating,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá của học viên thành công", responses));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<CourseReviewSummaryResponse>> getInstructorReviewSummary(
            @PathVariable Long courseId
    ) {
        CourseReviewSummaryResponse summary = reviewService.getCourseReviewSummary(courseId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê đánh giá thành công", summary));
    }
}
