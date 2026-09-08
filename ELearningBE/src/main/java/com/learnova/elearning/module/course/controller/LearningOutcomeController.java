package com.learnova.elearning.module.course.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.course.dto.request.CreateLearningOutcomeRequest;
import com.learnova.elearning.module.course.dto.request.ReorderLearningOutcomesRequest;
import com.learnova.elearning.module.course.dto.request.UpdateLearningOutcomeRequest;
import com.learnova.elearning.module.course.dto.response.LearningOutcomeResponse;
import com.learnova.elearning.module.course.service.LearningOutcomeService;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lecturer/courses/{courseId}/outcomes")
@RequiredArgsConstructor
public class LearningOutcomeController {

    private final LearningOutcomeService outcomeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LearningOutcomeResponse>>> list(
            @PathVariable Long courseId, @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(outcomeService.list(courseId, user.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LearningOutcomeResponse>> create(
            @PathVariable Long courseId, @Valid @RequestBody CreateLearningOutcomeRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Learning outcome created",
                        outcomeService.create(courseId, request, user.getId())));
    }

    @PatchMapping("/{outcomeId}")
    public ResponseEntity<ApiResponse<LearningOutcomeResponse>> update(
            @PathVariable Long courseId, @PathVariable Long outcomeId,
            @Valid @RequestBody UpdateLearningOutcomeRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Learning outcome updated",
                outcomeService.update(courseId, outcomeId, request, user.getId())));
    }

    @DeleteMapping("/{outcomeId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long courseId, @PathVariable Long outcomeId,
            @AuthenticationPrincipal CustomUserDetails user) {
        outcomeService.delete(courseId, outcomeId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Learning outcome deleted", null));
    }

    @PutMapping("/order")
    public ResponseEntity<ApiResponse<List<LearningOutcomeResponse>>> reorder(
            @PathVariable Long courseId, @Valid @RequestBody ReorderLearningOutcomesRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Learning outcomes reordered",
                outcomeService.reorder(courseId, request.getOutcomeIds(), user.getId())));
    }
}
