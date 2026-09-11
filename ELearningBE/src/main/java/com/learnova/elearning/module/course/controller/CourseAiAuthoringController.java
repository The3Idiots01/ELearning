package com.learnova.elearning.module.course.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.course.dto.request.ApplyCurriculumDraftRequest;
import com.learnova.elearning.module.course.dto.request.GenerateCurriculumDraftRequest;
import com.learnova.elearning.module.course.dto.response.CurriculumDraftResponse;
import com.learnova.elearning.module.course.dto.response.CurriculumResponse;
import com.learnova.elearning.module.course.dto.response.OutcomeSuggestionResponse;
import com.learnova.elearning.module.course.service.CourseAiAuthoringService;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lecturer/courses/{courseId}")
@RequiredArgsConstructor
public class CourseAiAuthoringController {

    private final CourseAiAuthoringService aiAuthoringService;

    @PostMapping("/lessons/{lessonId}/ai/outcome-suggestions")
    public ResponseEntity<ApiResponse<OutcomeSuggestionResponse>> suggestOutcomes(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(
                aiAuthoringService.suggestOutcomes(courseId, lessonId, user.getId())));
    }

    @PostMapping("/ai/curriculum-draft")
    public ResponseEntity<ApiResponse<CurriculumDraftResponse>> generateCurriculumDraft(
            @PathVariable Long courseId,
            @Valid @RequestBody(required = false) GenerateCurriculumDraftRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(
                aiAuthoringService.generateCurriculumDraft(courseId, request, user.getId())));
    }

    @PostMapping("/ai/curriculum-draft/apply")
    public ResponseEntity<ApiResponse<CurriculumResponse>> applyCurriculumDraft(
            @PathVariable Long courseId,
            @Valid @RequestBody ApplyCurriculumDraftRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("AI curriculum draft applied",
                aiAuthoringService.applyCurriculumDraft(courseId, request, user.getId())));
    }
}
