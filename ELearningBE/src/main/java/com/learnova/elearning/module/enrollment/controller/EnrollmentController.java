package com.learnova.elearning.module.enrollment.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.enrollment.dto.response.EnrolledCourseResponse;
import com.learnova.elearning.module.enrollment.dto.response.EnrollmentResponse;
import com.learnova.elearning.module.enrollment.dto.response.ProgressResponse;
import com.learnova.elearning.module.enrollment.service.EnrollmentService;
import com.learnova.elearning.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping("/enrolled")
    public ResponseEntity<ApiResponse<List<EnrolledCourseResponse>>> getMyEnrolledCourses(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        List<EnrolledCourseResponse> response = enrollmentService.getEnrolledCourses(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{courseId}/enroll")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        EnrollmentResponse response = enrollmentService.enroll(courseId, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Successfully enrolled in course", response));
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/complete")
    public ResponseEntity<ApiResponse<ProgressResponse>> completeLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        ProgressResponse response = enrollmentService.completeLesson(courseId, lessonId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Successfully marked lesson completed", response));
    }
}
