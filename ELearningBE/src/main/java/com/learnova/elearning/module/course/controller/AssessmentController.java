package com.learnova.elearning.module.course.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.course.dto.request.CreateAssessmentRequest;
import com.learnova.elearning.module.course.dto.request.PlaceAssessmentRequest;
import com.learnova.elearning.module.course.dto.request.ReorderAssessmentsRequest;
import com.learnova.elearning.module.course.dto.request.UpdateAssessmentRequest;
import com.learnova.elearning.module.course.dto.request.ArchiveConfirmationRequest;
import com.learnova.elearning.module.course.dto.response.AssessmentResponse;
import com.learnova.elearning.module.course.service.AssessmentService;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lecturer/courses/{courseId}")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping("/assessments")
    public ResponseEntity<ApiResponse<List<AssessmentResponse>>> list(
            @PathVariable Long courseId, @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.list(courseId, user.getId())));
    }

    @PostMapping("/assessments")
    public ResponseEntity<ApiResponse<AssessmentResponse>> create(
            @PathVariable Long courseId, @Valid @RequestBody CreateAssessmentRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assessment created",
                        assessmentService.create(courseId, request, user.getId())));
    }

    @PatchMapping("/assessments/{assessmentId}")
    public ResponseEntity<ApiResponse<AssessmentResponse>> update(
            @PathVariable Long courseId, @PathVariable Long assessmentId,
            @Valid @RequestBody UpdateAssessmentRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Assessment updated",
                assessmentService.update(courseId, assessmentId, request, user.getId())));
    }

    @DeleteMapping("/assessments/{assessmentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long courseId, @PathVariable Long assessmentId,
            @RequestBody(required = false) ArchiveConfirmationRequest request,
            @RequestParam(defaultValue = "false") boolean confirm,
            @AuthenticationPrincipal CustomUserDetails user) {
        assessmentService.delete(courseId, assessmentId, user.getId(),
                confirm || request != null && Boolean.TRUE.equals(request.getConfirm()));
        return ResponseEntity.ok(ApiResponse.success("Assessment deleted", null));
    }

    @PatchMapping("/assessments/{assessmentId}/placement")
    public ResponseEntity<ApiResponse<AssessmentResponse>> place(
            @PathVariable Long courseId, @PathVariable Long assessmentId,
            @Valid @RequestBody PlaceAssessmentRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Assessment placement updated",
                assessmentService.place(courseId, assessmentId, request, user.getId())));
    }

    @PutMapping("/sections/{sectionId}/assessments/order")
    public ResponseEntity<ApiResponse<List<AssessmentResponse>>> reorder(
            @PathVariable Long courseId, @PathVariable Long sectionId,
            @Valid @RequestBody ReorderAssessmentsRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Assessments reordered",
                assessmentService.reorder(courseId, sectionId, request.getAssessmentIds(), user.getId())));
    }
}
