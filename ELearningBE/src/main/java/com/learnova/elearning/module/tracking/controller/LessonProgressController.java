package com.learnova.elearning.module.tracking.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.tracking.dto.HeartbeatRequest;
import com.learnova.elearning.module.tracking.dto.ProgressSnapshotResponse;
import com.learnova.elearning.module.tracking.service.WatchTrackingService;
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

/** §5.4 design_us15_us17.md — nhận heartbeat xem video/tài liệu. */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/lessons/{lessonId}/progress")
@RequiredArgsConstructor
public class LessonProgressController {

    private final WatchTrackingService watchTrackingService;

    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<ProgressSnapshotResponse>> heartbeat(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody HeartbeatRequest request
    ) {
        Long userId = user != null ? user.getId() : null;
        return watchTrackingService.recordHeartbeat(courseId, lessonId, userId, request)
                .map(snapshot -> ResponseEntity.ok(ApiResponse.success(snapshot)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
