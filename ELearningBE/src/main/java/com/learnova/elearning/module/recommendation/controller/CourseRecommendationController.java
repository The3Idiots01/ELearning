package com.learnova.elearning.module.recommendation.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.recommendation.dto.request.AiRecommendationRequest;
import com.learnova.elearning.module.recommendation.dto.response.AiRecommendationResponse;
import com.learnova.elearning.module.recommendation.dto.response.ContinuousRecommendationResponse;
import com.learnova.elearning.module.recommendation.service.CourseRecommendationService;
import com.learnova.elearning.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "API gợi ý và đề xuất khóa học (Logic hệ thống Top 5 & Gemini AI Top 3)")
public class CourseRecommendationController {

    private final CourseRecommendationService recommendationService;

    @GetMapping("/continuous")
    @Operation(summary = "Đề xuất liên tục Top 5 khóa học liên quan",
               description = "Dựa vào quy tắc hệ thống (category, level, rating, độ phổ biến, khóa học đang xem, hoặc lịch sử học viên)")
    public ResponseEntity<ApiResponse<ContinuousRecommendationResponse>> getContinuousRecommendations(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        ContinuousRecommendationResponse response =
                recommendationService.getContinuousRecommendations(courseId, categoryId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/ai")
    @Operation(summary = "Đề xuất Top 3 khóa học với AI Gemini",
               description = "Gọi Gemini AI phân tích mục tiêu, sở thích học viên và chọn ra Top 3 khóa học phù hợp nhất kèm điểm match & lý do")
    public ResponseEntity<ApiResponse<AiRecommendationResponse>> getAiRecommendations(
            @RequestBody(required = false) AiRecommendationRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        AiRecommendationResponse response = recommendationService.getAiRecommendations(request, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/ai/quick")
    @Operation(summary = "Đề xuất nhanh Top 3 bằng AI cho học viên đang đăng nhập",
               description = "Tự động phân tích lịch sử các khóa học đã ghi danh của học viên để AI tư vấn các bước học tiếp theo")
    public ResponseEntity<ApiResponse<AiRecommendationResponse>> getQuickAiRecommendations(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        AiRecommendationResponse response = recommendationService.getQuickAiRecommendations(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/ai/latest")
    @Operation(summary = "Lấy đề xuất AI gần nhất đã lưu của học viên",
               description = "Trả về phiên đề xuất gần nhất đã được AI phân tích và lưu trong DB của học viên đang đăng nhập")
    public ResponseEntity<ApiResponse<AiRecommendationResponse>> getLatestAiRecommendation(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        AiRecommendationResponse response = recommendationService.getLatestAiRecommendation(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
