package com.learnova.elearning.module.tracking.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.delivery.service.AccessDecision;
import com.learnova.elearning.module.delivery.service.AccessScope;
import com.learnova.elearning.module.delivery.service.ContentAccessGuard;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.entity.LessonProgress;
import com.learnova.elearning.module.enrollment.entity.enums.CompletionSource;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.enrollment.repository.LessonProgressRepository;
import com.learnova.elearning.module.enrollment.repository.WatchedRangesMergeResult;
import com.learnova.elearning.module.tracking.dto.HeartbeatRequest;
import com.learnova.elearning.module.tracking.dto.ProgressSnapshotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Ghi nhận heartbeat và merge nguyên tử vào {@code lesson_progress} — §5.4 /
 * §5.5 design_us15_us17.md. Trước khi merge, {@link HeartbeatRateLimiter}
 * (§5.6) xén bớt coverage đề nghị nếu vượt ngân sách Δt × tốc độ tối đa — phép
 * hợp {@code +} một khi đã ghi vào {@code watched_ranges} là không lùi lại
 * được, nên việc cắt bớt phải xảy ra TRƯỚC merge, không phải sau. Sau merge,
 * đối chiếu {@link CompletionPolicy} (§5.7): lần đầu đạt ngưỡng thì đặt
 * {@code completed_at} và tính lại % course qua
 * {@code EnrollmentRepository.recalculateCourseProgress} — chỉ gọi đúng lúc
 * chuyển trạng thái, không phải mỗi heartbeat.
 */
@Service
@RequiredArgsConstructor
public class WatchTrackingService {

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository progressRepository;
    private final ContentAccessGuard accessGuard;
    private final CoverageCalculator coverageCalculator;
    private final CompletionPolicy completionPolicy;
    private final HeartbeatRateLimiter rateLimiter;

    /**
     * @return empty khi không có enrollment để gắn tiến độ vào — khách vãng
     * lai xem preview, hoặc owner/admin xem thử course chưa publish (§5.6:
     * "không lưu, không lỗi" → controller trả {@code 204}).
     */
    @Transactional
    public Optional<ProgressSnapshotResponse> recordHeartbeat(Long courseId, Long lessonId, Long userId,
                                                                HeartbeatRequest request) {
        Lesson lesson = lessonRepository.findByIdAndSection_Course_Id(lessonId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_IN_COURSE));

        AccessDecision decision = accessGuard.decide(lesson, userId);
        if (decision.scope() != AccessScope.ENROLLED) {
            return Optional.empty();
        }
        Long enrollmentId = decision.enrollmentId();

        // §5.6: lesson đang bị chặn tạm thời do vi phạm liên tiếp — từ chối trước khi làm bất cứ việc gì khác.
        if (rateLimiter.isBlocked(userId, lessonId)) {
            throw new AppException(ErrorCode.PROGRESS_RATE_LIMITED);
        }

        int durationSeconds = lesson.getDurationSeconds() != null ? lesson.getDurationSeconds() : 0;
        int position = clampPosition(request.getPositionSeconds(), durationSeconds);
        List<int[]> ranges = coverageCalculator.parseAndClamp(request.getPlayedRanges(), durationSeconds);
        String rangesLiteral = coverageCalculator.literalOf(ranges);

        int watchedSecondsBefore = progressRepository.findByEnrollment_IdAndLesson_Id(enrollmentId, lesson.getId())
                .map(LessonProgress::getWatchedSeconds)
                .orElse(0);

        // resolveBudgetSeconds phải luôn được gọi (kể cả ranges rỗng) để giữ mốc Δt cho heartbeat kế tiếp.
        int budgetSeconds = rateLimiter.resolveBudgetSeconds(userId, lessonId);
        int proposedNewSeconds = ranges.isEmpty() ? 0
                : progressRepository.previewNewWatchedSeconds(enrollmentId, lesson.getId(), rangesLiteral);
        if (proposedNewSeconds > budgetSeconds) {
            ranges = coverageCalculator.truncateNearPosition(ranges, position, budgetSeconds);
            rangesLiteral = coverageCalculator.literalOf(ranges);
            if (rateLimiter.recordViolationAndCheckBlock(userId, lessonId)) {
                throw new AppException(ErrorCode.PROGRESS_RATE_LIMITED);
            }
        }

        WatchedRangesMergeResult merged = progressRepository.mergeWatchedRanges(
                enrollmentId, lesson.getId(), rangesLiteral, position);
        progressRepository.materializeCoverage(merged.getId(), merged.getWatchedSeconds(), durationSeconds);

        LessonProgress progress = progressRepository.findById(merged.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "lesson_progress " + merged.getId() + " biến mất ngay sau khi merge"));

        // Unit tests and a few non-JPA callers do not provide a persistence
        // context. The repository query above is already the fresh snapshot
        // in those cases; refresh when an EntityManager is available.
        if (entityManager != null) entityManager.refresh(progress);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalStateException("enrollment " + enrollmentId + " biến mất ngay sau khi merge"));

        if (entityManager != null) entityManager.refresh(enrollment);
        boolean justCompleted = false;
        if (progress.getCompletedAt() == null && completionPolicy.isComplete(lesson, progress)) {
            progress.setCompletedAt(Instant.now());
            progress.setCompletionSource(CompletionSource.POSITION);
            progress = progressRepository.save(progress);
            justCompleted = true;
        }

        if (justCompleted) {
            enrollmentRepository.recalculateCourseProgress(enrollmentId, courseId);
            enrollment = enrollmentRepository.findById(enrollmentId)
                    .orElseThrow(() -> new IllegalStateException("enrollment " + enrollmentId + " biến mất sau rollup"));
        }

        if (justCompleted && entityManager != null) entityManager.refresh(enrollment);

        return Optional.of(ProgressSnapshotResponse.builder()
                .lessonId(lesson.getId())
                .lastPositionSeconds(progress.getLastPositionSeconds())
                .maxPositionSeconds(progress.getMaxPositionSeconds())
                .coveragePercent(progress.getCoveragePercent())
                .watchedSeconds(progress.getWatchedSeconds())
                .newWatchedSeconds(merged.getWatchedSeconds() - watchedSecondsBefore)
                .lessonCompleted(progress.getCompletedAt() != null)
                .completionSource(progress.getCompletionSource())
                .courseProgressPercent(justCompleted ? enrollment.getProgress() : null)
                .enrollmentStatus(enrollment.getStatus().name())
                .build());
    }

    private int clampPosition(Double positionSeconds, int durationSeconds) {
        if (positionSeconds == null) {
            return 0;
        }
        long rounded = Math.max(0, Math.round(positionSeconds));
        if (durationSeconds > 0) {
            rounded = Math.min(rounded, durationSeconds);
        }
        return (int) rounded;
    }
}
