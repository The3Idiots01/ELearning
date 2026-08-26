package com.learnova.elearning.module.course.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.course.dto.request.AddLessonResourceRequest;
import com.learnova.elearning.module.course.dto.request.AttachLessonContentRequest;
import com.learnova.elearning.module.course.dto.response.LessonResponse;
import com.learnova.elearning.module.course.service.LessonContentService;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lecturer/courses/{courseId}/lessons/{lessonId}")
@RequiredArgsConstructor
public class LessonContentController {

    private final LessonContentService lessonContentService;

    @PutMapping("/content")
    public ResponseEntity<ApiResponse<LessonResponse>> attachContent(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @Valid @RequestBody AttachLessonContentRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        LessonResponse response = lessonContentService.attachContent(courseId, lessonId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lesson content attached", response));
    }

    @DeleteMapping("/content")
    public ResponseEntity<ApiResponse<LessonResponse>> removeContent(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        LessonResponse response = lessonContentService.removeContent(courseId, lessonId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lesson content removed", response));
    }

    @PostMapping("/resources")
    public ResponseEntity<ApiResponse<LessonResponse>> addResource(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @Valid @RequestBody AddLessonResourceRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        LessonResponse response = lessonContentService.addResource(courseId, lessonId, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource added", response));
    }

    @DeleteMapping("/resources/{resourceId}")
    public ResponseEntity<ApiResponse<LessonResponse>> deleteResource(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @PathVariable Long resourceId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        LessonResponse response = lessonContentService.deleteResource(courseId, lessonId, resourceId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Resource deleted", response));
    }
}
