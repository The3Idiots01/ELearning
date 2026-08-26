package com.learnova.elearning.module.course.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.common.dto.PageResponse;
import com.learnova.elearning.module.course.dto.request.CreateCourseRequest;
import com.learnova.elearning.module.course.dto.request.UpdateBulletsRequest;
import com.learnova.elearning.module.course.dto.request.UpdateCourseRequest;
import com.learnova.elearning.module.course.dto.request.UpdatePriceRequest;
import com.learnova.elearning.module.course.dto.request.UpdateThumbnailRequest;
import com.learnova.elearning.module.course.dto.response.CourseResponse;
import com.learnova.elearning.module.course.dto.response.CourseSummaryResponse;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.service.CourseService;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lecturer/courses")
@RequiredArgsConstructor
public class InstructorCourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> create(
            @Valid @RequestBody CreateCourseRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CourseResponse response = courseService.create(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Course created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CourseSummaryResponse>>> listMine(
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        PageResponse<CourseSummaryResponse> response =
                courseService.listMine(user.getId(), status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> getDetail(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.getDetail(courseId, user.getId())));
    }

    @PatchMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> update(
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CourseResponse response = courseService.update(courseId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Course updated", response));
    }

    @PutMapping("/{courseId}/price")
    public ResponseEntity<ApiResponse<CourseResponse>> updatePrice(
            @PathVariable Long courseId,
            @Valid @RequestBody UpdatePriceRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CourseResponse response = courseService.updatePrice(courseId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Price updated", response));
    }

    @PutMapping("/{courseId}/bullets")
    public ResponseEntity<ApiResponse<CourseResponse>> updateBullets(
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateBulletsRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CourseResponse response = courseService.updateBullets(courseId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Course description updated", response));
    }

    @PutMapping("/{courseId}/thumbnail")
    public ResponseEntity<ApiResponse<CourseResponse>> updateThumbnail(
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateThumbnailRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CourseResponse response = courseService.updateThumbnail(courseId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Thumbnail updated", response));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        courseService.delete(courseId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Course deleted", null));
    }
}
