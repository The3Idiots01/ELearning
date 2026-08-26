package com.learnova.elearning.module.course.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.course.dto.request.UnpublishRequest;
import com.learnova.elearning.module.course.dto.response.CourseResponse;
import com.learnova.elearning.module.course.dto.response.CourseStatusLogResponse;
import com.learnova.elearning.module.course.dto.response.PublishCheckResponse;
import com.learnova.elearning.module.course.service.CoursePublishService;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lecturer/courses/{courseId}")
@RequiredArgsConstructor
public class CoursePublishController {

    private final CoursePublishService publishService;

    @GetMapping("/publish-check")
    public ResponseEntity<ApiResponse<PublishCheckResponse>> publishCheck(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                publishService.publishCheck(courseId, user.getId())));
    }

    @PostMapping("/publish")
    public ResponseEntity<ApiResponse<CourseResponse>> publish(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CourseResponse response = publishService.publish(courseId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Course published", response));
    }

    @PostMapping("/unpublish")
    public ResponseEntity<ApiResponse<CourseResponse>> unpublish(
            @PathVariable Long courseId,
            @Valid @RequestBody(required = false) UnpublishRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CourseResponse response = publishService.unpublish(courseId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Course unpublished", response));
    }

    @GetMapping("/status-logs")
    public ResponseEntity<ApiResponse<List<CourseStatusLogResponse>>> statusLogs(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                publishService.getStatusLogs(courseId, user.getId())));
    }
}
