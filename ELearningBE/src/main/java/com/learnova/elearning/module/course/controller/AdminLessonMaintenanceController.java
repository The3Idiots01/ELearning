package com.learnova.elearning.module.course.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.course.dto.response.VideoDurationBackfillResponse;
import com.learnova.elearning.module.course.service.VideoDurationBackfillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Việc bảo trì nội dung do admin kích hoạt thủ công — không phải CRUD nghiệp
 * vụ thông thường. Được bảo vệ bởi {@code /api/v1/admin/**} hasRole(ADMIN)
 * trong SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/admin/lessons")
@RequiredArgsConstructor
public class AdminLessonMaintenanceController {

    private final VideoDurationBackfillService backfillService;

    /** §7.5 Task 0c — đo lại duration thật cho lesson VIDEO đã publish từ trước. */
    @PostMapping("/video-duration-backfill")
    public ResponseEntity<ApiResponse<VideoDurationBackfillResponse>> backfillVideoDuration() {
        VideoDurationBackfillResponse response = backfillService.backfill();
        return ResponseEntity.ok(ApiResponse.success("Video duration backfill completed", response));
    }
}
