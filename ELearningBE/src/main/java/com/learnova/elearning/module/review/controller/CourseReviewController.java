package com.learnova.elearning.module.review.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.review.dto.request.CreateOrUpdateReviewRequest;
import com.learnova.elearning.module.review.dto.response.CourseReviewEligibilityResponse;
import com.learnova.elearning.module.review.dto.response.CourseReviewResponse;
import com.learnova.elearning.module.review.dto.response.CourseReviewSummaryResponse;
import com.learnova.elearning.module.review.service.CourseReviewService;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/reviews")
@RequiredArgsConstructor
public class CourseReviewController {

    private final CourseReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CourseReviewResponse>>> getCourseReviews(
            @PathVariable Long courseId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        Sort sortObj = switch (sort.toLowerCase()) {
            case "highest" -> Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "lowest" -> Sort.by(Sort.Direction.ASC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Page<CourseReviewResponse> responses = reviewService.getCourseReviews(
                courseId, rating, PageRequest.of(page, size, sortObj)
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công", responses));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<CourseReviewSummaryResponse>> getCourseReviewSummary(
            @PathVariable Long courseId
    ) {
        CourseReviewSummaryResponse summary = reviewService.getCourseReviewSummary(courseId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê đánh giá thành công", summary));
    }

    @GetMapping("/my-status")
    public ResponseEntity<ApiResponse<CourseReviewEligibilityResponse>> getMyReviewStatus(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.success("Chưa đăng nhập",
                    CourseReviewEligibilityResponse.builder()
                            .isEnrolled(false)
                            .currentProgress(0.0)
                            .requiredProgress(20.0)
                            .canReview(false)
                            .review(null)
                            .build()
            ));
        }
        CourseReviewEligibilityResponse response = reviewService.getReviewEligibility(courseId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái đánh giá thành công", response));
    }

    @GetMapping("/my-review")
    public ResponseEntity<ApiResponse<CourseReviewResponse>> getMyReview(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.success("Chưa đăng nhập", null));
        }
        CourseReviewResponse response = reviewService.getMyReview(courseId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy đánh giá của tôi thành công", response));
    }

    @PostMapping("/my-review")
    public ResponseEntity<ApiResponse<CourseReviewResponse>> submitReview(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateOrUpdateReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        CourseReviewResponse response = reviewService.submitReview(courseId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đánh giá khóa học thành công", response));
    }

    @DeleteMapping("/my-review")
    public ResponseEntity<ApiResponse<Void>> deleteMyReview(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        reviewService.deleteReview(courseId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Xóa đánh giá thành công", null));
    }
}
