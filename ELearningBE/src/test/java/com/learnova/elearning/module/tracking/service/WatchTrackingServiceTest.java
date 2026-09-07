package com.learnova.elearning.module.tracking.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.CourseSection;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.delivery.service.AccessDecision;
import com.learnova.elearning.module.delivery.service.AccessScope;
import com.learnova.elearning.module.delivery.service.ContentAccessGuard;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.entity.LessonProgress;
import com.learnova.elearning.module.enrollment.entity.enums.CompletionSource;
import com.learnova.elearning.module.enrollment.entity.enums.EnrollmentStatus;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.enrollment.repository.LessonProgressRepository;
import com.learnova.elearning.module.enrollment.repository.WatchedRangesMergeResult;
import com.learnova.elearning.module.tracking.dto.HeartbeatRequest;
import com.learnova.elearning.module.tracking.dto.ProgressSnapshotResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchTrackingServiceTest {

    private static final Long COURSE_ID = 10L;
    private static final Long LESSON_ID = 42L;
    private static final Long ENROLLMENT_ID = 500L;
    private static final Long LEARNER_ID = 7L;

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private LessonProgressRepository progressRepository;
    @Mock
    private ContentAccessGuard accessGuard;
    @Mock
    private CoverageCalculator coverageCalculator;
    @Mock
    private CompletionPolicy completionPolicy;
    @Mock
    private HeartbeatRateLimiter rateLimiter;
    @Mock
    private WatchedRangesMergeResult mergeResult;

    private WatchTrackingService service;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        service = new WatchTrackingService(lessonRepository, enrollmentRepository, progressRepository,
                accessGuard, coverageCalculator, completionPolicy, rateLimiter);

        Course course = Course.builder().id(COURSE_ID).status(CourseStatus.PUBLISHED).build();
        CourseSection section = CourseSection.builder().course(course).build();
        lesson = Lesson.builder().id(LESSON_ID).section(section).durationSeconds(612).build();

        lenient().when(lessonRepository.findByIdAndSection_Course_Id(LESSON_ID, COURSE_ID))
                .thenReturn(Optional.of(lesson));
    }

    private HeartbeatRequest request(double position, List<List<Double>> ranges) {
        HeartbeatRequest req = new HeartbeatRequest();
        req.setPositionSeconds(position);
        req.setPlayedRanges(ranges);
        return req;
    }

    @Test
    void recordHeartbeat_lessonNotInCourse_throwsBadRequest() {
        when(lessonRepository.findByIdAndSection_Course_Id(LESSON_ID, COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordHeartbeat(COURSE_ID, LESSON_ID, LEARNER_ID, request(10, List.of())))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.LESSON_NOT_IN_COURSE);
    }

    @Test
    void recordHeartbeat_guestPreview_returnsEmptyWithoutTouchingRepository() {
        when(accessGuard.decide(lesson, null)).thenReturn(new AccessDecision(true, AccessScope.PREVIEW, null));

        Optional<ProgressSnapshotResponse> result = service.recordHeartbeat(COURSE_ID, LESSON_ID, null, request(10, List.of()));

        assertThat(result).isEmpty();
        verify(progressRepository, never()).mergeWatchedRanges(any(), any(), any(), anyInt());
    }

    @Test
    void recordHeartbeat_ownerPreviewingDraft_returnsEmptyWithoutPersisting() {
        when(accessGuard.decide(lesson, LEARNER_ID)).thenReturn(new AccessDecision(true, AccessScope.OWNER, null));

        Optional<ProgressSnapshotResponse> result =
                service.recordHeartbeat(COURSE_ID, LESSON_ID, LEARNER_ID, request(10, List.of()));

        assertThat(result).isEmpty();
        verify(progressRepository, never()).mergeWatchedRanges(any(), any(), any(), anyInt());
    }

    @Test
    void recordHeartbeat_enrolled_mergesAndReturnsSnapshot() {
        when(accessGuard.decide(lesson, LEARNER_ID))
                .thenReturn(new AccessDecision(true, AccessScope.ENROLLED, ENROLLMENT_ID));
        when(coverageCalculator.parseAndClamp(any(), eq(612))).thenReturn(List.of(new int[]{0, 206}));
        when(coverageCalculator.literalOf(any())).thenReturn("{[0,206)}");
        when(progressRepository.findByEnrollment_IdAndLesson_Id(ENROLLMENT_ID, LESSON_ID))
                .thenReturn(Optional.of(LessonProgress.builder().watchedSeconds(100).build()));
        when(mergeResult.getId()).thenReturn(900L);
        when(mergeResult.getWatchedSeconds()).thenReturn(206);
        when(progressRepository.mergeWatchedRanges(ENROLLMENT_ID, LESSON_ID, "{[0,206)}", 187))
                .thenReturn(mergeResult);

        LessonProgress afterMerge = LessonProgress.builder()
                .lastPositionSeconds(187)
                .maxPositionSeconds(206)
                .watchedSeconds(206)
                .coveragePercent(new BigDecimal("33.66"))
                .build();
        when(progressRepository.findById(900L)).thenReturn(Optional.of(afterMerge));
        when(enrollmentRepository.findById(ENROLLMENT_ID))
                .thenReturn(Optional.of(Enrollment.builder().id(ENROLLMENT_ID).status(EnrollmentStatus.ACTIVE).build()));

        Optional<ProgressSnapshotResponse> result = service.recordHeartbeat(
                COURSE_ID, LESSON_ID, LEARNER_ID, request(187.4, List.of(List.of(0.0, 205.2))));

        assertThat(result).isPresent();
        ProgressSnapshotResponse snapshot = result.get();
        assertThat(snapshot.getLessonId()).isEqualTo(LESSON_ID);
        assertThat(snapshot.getLastPositionSeconds()).isEqualTo(187);
        assertThat(snapshot.getMaxPositionSeconds()).isEqualTo(206);
        assertThat(snapshot.getWatchedSeconds()).isEqualTo(206);
        assertThat(snapshot.getNewWatchedSeconds()).isEqualTo(106); // 206 - 100
        assertThat(snapshot.getCoveragePercent()).isEqualByComparingTo("33.66");
        assertThat(snapshot.getLessonCompleted()).isFalse();
        assertThat(snapshot.getCompletionSource()).isNull();
        assertThat(snapshot.getCourseProgressPercent()).isNull();
        assertThat(snapshot.getEnrollmentStatus()).isEqualTo("ACTIVE");
        verify(progressRepository).materializeCoverage(900L, 206, 612);
    }

    @Test
    void recordHeartbeat_resendingSamePayload_reportsZeroNewWatchedSeconds() {
        when(accessGuard.decide(lesson, LEARNER_ID))
                .thenReturn(new AccessDecision(true, AccessScope.ENROLLED, ENROLLMENT_ID));
        when(coverageCalculator.parseAndClamp(any(), eq(612))).thenReturn(List.of(new int[]{0, 206}));
        when(coverageCalculator.literalOf(any())).thenReturn("{[0,206)}");
        when(progressRepository.findByEnrollment_IdAndLesson_Id(ENROLLMENT_ID, LESSON_ID))
                .thenReturn(Optional.of(LessonProgress.builder().watchedSeconds(206).build()));
        when(mergeResult.getId()).thenReturn(900L);
        when(mergeResult.getWatchedSeconds()).thenReturn(206);
        when(progressRepository.mergeWatchedRanges(any(), any(), any(), anyInt())).thenReturn(mergeResult);
        when(progressRepository.findById(900L)).thenReturn(Optional.of(LessonProgress.builder()
                .watchedSeconds(206).coveragePercent(BigDecimal.TEN).build()));
        when(enrollmentRepository.findById(ENROLLMENT_ID))
                .thenReturn(Optional.of(Enrollment.builder().id(ENROLLMENT_ID).status(EnrollmentStatus.ACTIVE).build()));

        Optional<ProgressSnapshotResponse> result = service.recordHeartbeat(
                COURSE_ID, LESSON_ID, LEARNER_ID, request(187, List.of(List.of(0.0, 205.2))));

        assertThat(result.get().getNewWatchedSeconds()).isZero();
    }

    @Test
    void recordHeartbeat_alreadyCompletedLesson_reflectsCompletionInSnapshot() {
        when(accessGuard.decide(lesson, LEARNER_ID))
                .thenReturn(new AccessDecision(true, AccessScope.ENROLLED, ENROLLMENT_ID));
        when(coverageCalculator.parseAndClamp(any(), eq(612))).thenReturn(List.of());
        when(coverageCalculator.literalOf(any())).thenReturn("{}");
        when(progressRepository.findByEnrollment_IdAndLesson_Id(ENROLLMENT_ID, LESSON_ID))
                .thenReturn(Optional.empty());
        when(mergeResult.getId()).thenReturn(900L);
        when(mergeResult.getWatchedSeconds()).thenReturn(600);
        when(progressRepository.mergeWatchedRanges(any(), any(), any(), anyInt())).thenReturn(mergeResult);
        when(progressRepository.findById(900L)).thenReturn(Optional.of(LessonProgress.builder()
                .watchedSeconds(600)
                .coveragePercent(BigDecimal.valueOf(98.04))
                .completedAt(Instant.now())
                .completionSource(CompletionSource.POSITION)
                .build()));
        when(enrollmentRepository.findById(ENROLLMENT_ID))
                .thenReturn(Optional.of(Enrollment.builder().id(ENROLLMENT_ID).status(EnrollmentStatus.ACTIVE).build()));

        ProgressSnapshotResponse snapshot = service.recordHeartbeat(
                COURSE_ID, LESSON_ID, LEARNER_ID, request(600, List.of())).orElseThrow();

        assertThat(snapshot.getLessonCompleted()).isTrue();
        assertThat(snapshot.getCompletionSource()).isEqualTo(CompletionSource.POSITION);
    }
}
