package com.learnova.elearning.module.course.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.course.dto.request.CreateLessonRequest;
import com.learnova.elearning.module.course.dto.request.CreateSectionRequest;
import com.learnova.elearning.module.course.dto.request.MoveLessonRequest;
import com.learnova.elearning.module.course.dto.request.ReorderLessonsRequest;
import com.learnova.elearning.module.course.dto.request.ReorderSectionsRequest;
import com.learnova.elearning.module.course.dto.request.UpdateLessonRequest;
import com.learnova.elearning.module.course.dto.request.UpdateSectionRequest;
import com.learnova.elearning.module.course.dto.response.CurriculumResponse;
import com.learnova.elearning.module.course.dto.response.LessonResponse;
import com.learnova.elearning.module.course.dto.response.SectionResponse;
import com.learnova.elearning.module.course.service.CurriculumService;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lecturer/courses/{courseId}")
@RequiredArgsConstructor
public class CurriculumController {

    private final CurriculumService curriculumService;

    @GetMapping("/curriculum")
    public ResponseEntity<ApiResponse<CurriculumResponse>> getCurriculum(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                curriculumService.getCurriculum(courseId, user.getId())));
    }

    // ---- Section ----------------------------------------------------------

    @PostMapping("/sections")
    public ResponseEntity<ApiResponse<SectionResponse>> addSection(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateSectionRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        SectionResponse response = curriculumService.addSection(courseId, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Section created", response));
    }

    @PatchMapping("/sections/{sectionId}")
    public ResponseEntity<ApiResponse<SectionResponse>> updateSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @Valid @RequestBody UpdateSectionRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        SectionResponse response = curriculumService.updateSection(courseId, sectionId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Section updated", response));
    }

    @DeleteMapping("/sections/{sectionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        curriculumService.deleteSection(courseId, sectionId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Section deleted", null));
    }

    @PutMapping("/sections/order")
    public ResponseEntity<ApiResponse<CurriculumResponse>> reorderSections(
            @PathVariable Long courseId,
            @Valid @RequestBody ReorderSectionsRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CurriculumResponse response =
                curriculumService.reorderSections(courseId, request.getSectionIds(), user.getId());
        return ResponseEntity.ok(ApiResponse.success("Sections reordered", response));
    }

    // ---- Lesson -----------------------------------------------------------

    @PostMapping("/sections/{sectionId}/lessons")
    public ResponseEntity<ApiResponse<LessonResponse>> addLesson(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @Valid @RequestBody CreateLessonRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        LessonResponse response = curriculumService.addLesson(courseId, sectionId, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lesson created", response));
    }

    @PatchMapping("/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @Valid @RequestBody UpdateLessonRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        LessonResponse response = curriculumService.updateLesson(courseId, lessonId, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lesson updated", response));
    }

    @DeleteMapping("/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        curriculumService.deleteLesson(courseId, lessonId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lesson deleted", null));
    }

    @PutMapping("/sections/{sectionId}/lessons/order")
    public ResponseEntity<ApiResponse<CurriculumResponse>> reorderLessons(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @Valid @RequestBody ReorderLessonsRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CurriculumResponse response =
                curriculumService.reorderLessons(courseId, sectionId, request.getLessonIds(), user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lessons reordered", response));
    }

    @PatchMapping("/lessons/{lessonId}/move")
    public ResponseEntity<ApiResponse<CurriculumResponse>> moveLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @Valid @RequestBody MoveLessonRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CurriculumResponse response = curriculumService.moveLesson(
                courseId, lessonId, request.getTargetSectionId(), request.getPosition(), user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lesson moved", response));
    }
}
