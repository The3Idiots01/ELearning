package com.learnova.elearning.module.course.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.common.dto.PageResponse;
import com.learnova.elearning.module.course.dto.response.CourseResponse;
import com.learnova.elearning.module.course.dto.response.CourseSummaryResponse;
import com.learnova.elearning.module.course.dto.response.CurriculumResponse;
import com.learnova.elearning.module.course.entity.enums.CourseLevel;
import com.learnova.elearning.module.course.service.CourseService;
import com.learnova.elearning.module.course.service.CurriculumService;
import com.learnova.elearning.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class PublicCourseController {

    private final CourseService courseService;
    private final CurriculumService curriculumService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CourseSummaryResponse>>> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) CourseLevel level,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<CourseSummaryResponse> response = courseService.searchPublic(categoryId, level, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> getDetail(@PathVariable Long courseId) {
        CourseResponse response = courseService.getPublicDetail(courseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<CourseResponse>> getDetailBySlug(@PathVariable String slug) {
        CourseResponse response = courseService.getPublicDetailBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{courseId}/curriculum")
    public ResponseEntity<ApiResponse<CurriculumResponse>> getCurriculum(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Long studentId = user != null ? user.getId() : null;
        CurriculumResponse response = curriculumService.getPublicCurriculum(courseId, studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
