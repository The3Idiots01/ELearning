package com.learnova.elearning.module.course.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.course.service.CourseOwnershipGuard;
import com.learnova.elearning.module.course.service.VideoMetadataJobs;
import com.learnova.elearning.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/lecturer/courses/{courseId}")
@RequiredArgsConstructor
public class VideoProcessingController {
    private final CourseOwnershipGuard ownership;
    private final LessonRepository lessons;
    private final VideoMetadataJobs jobs;

    @GetMapping("/video-processing")
    @Transactional(readOnly = true)
    public ApiResponse<List<VideoMetadataJobs.Status>> statuses(@PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails user) {
        ownership.requireOwnedCourse(courseId, user.getId());
        return ApiResponse.success(jobs.statuses(courseId));
    }

    @PostMapping("/lessons/{lessonId}/content/reprocess")
    @Transactional
    public ApiResponse<Void> retry(@PathVariable Long courseId, @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails user) {
        ownership.requireEditableCourse(courseId, user.getId());
        Lesson lesson = lessons.findForMetadataUpdate(lessonId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_IN_COURSE));
        if (lesson.getContentType() != LessonContentType.VIDEO
                || (lesson.getStorageKey() == null && lesson.getPendingStorageKey() == null))
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Bài học chưa có video để xử lý lại.");
        if (lesson.getPendingUploadStatus() == LessonUploadStatus.FAILED) {
            lesson.setPendingUploadStatus(LessonUploadStatus.PROCESSING);
            lessons.saveAndFlush(lesson);
            jobs.enqueue(lesson.getId(), lesson.getPendingStorageKey());
        } else if (lesson.getUploadStatus() == LessonUploadStatus.FAILED) {
            lesson.setUploadStatus(LessonUploadStatus.PROCESSING);
            lessons.saveAndFlush(lesson);
            jobs.enqueue(lesson.getId(), lesson.getStorageKey());
        }
        return ApiResponse.success("Đã tiếp nhận yêu cầu xử lý video", null);
    }
}
