package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.dto.request.UnpublishRequest;
import com.learnova.elearning.module.course.dto.response.CourseResponse;
import com.learnova.elearning.module.course.dto.response.CourseStatusLogResponse;
import com.learnova.elearning.module.course.dto.response.PublishCheckResponse;
import com.learnova.elearning.module.course.dto.response.PublishIssue;
import com.learnova.elearning.module.course.dto.response.PublishChangesResponse;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.CourseStatusLog;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import com.learnova.elearning.module.course.entity.enums.PublicationStatus;
import com.learnova.elearning.module.course.exception.CourseNotReadyException;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.course.repository.CourseStatusLogRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.course.repository.AssessmentRepository;
import com.learnova.elearning.module.user.repository.UserRepository;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * State machine trạng thái khóa học + điều kiện publish. Mọi lần đổi trạng thái
 * đều ghi course_status_logs. published_at chỉ set ở lần publish đầu tiên.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CoursePublishService {

    private final CourseOwnershipGuard ownershipGuard;
    private final CourseRepository courseRepository;
    private final CourseStatusLogRepository statusLogRepository;
    private final CoursePublishValidator publishValidator;
    private final CourseService courseService;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CurriculumService curriculumService;
    private LessonRepository lessonRepository;
    private AssessmentRepository assessmentRepository;

    @org.springframework.beans.factory.annotation.Autowired
    void setPublicationRepositories(LessonRepository lessonRepository,
                                    AssessmentRepository assessmentRepository) {
        this.lessonRepository = lessonRepository;
        this.assessmentRepository = assessmentRepository;
    }

    @Transactional(readOnly = true)
    public PublishCheckResponse publishCheck(Long courseId, Long userId) {
        Course course = ownershipGuard.requireOwnedCourse(courseId, userId);
        List<PublishIssue> issues = publishValidator.validate(course);
        List<Lesson> lessons = curriculumLessons(courseId);
        long draftLessons = lessons.stream().filter(l -> l.getPublicationStatus() == PublicationStatus.DRAFT).count();
        long pendingVideos = lessons.stream().filter(l -> l.getPendingUploadStatus() != null).count();
        long draftAssessments = curriculumAssessments(courseId).stream()
                .filter(a -> a.getPublicationStatus() == PublicationStatus.DRAFT).count();
        return PublishCheckResponse.builder()
                .canPublish(issues.isEmpty())
                .issues(issues)
                .requirements(new PublishCheckResponse.Requirements(publishValidator.minOutcomes()))
                .hasPendingChanges(draftLessons > 0 || draftAssessments > 0 || pendingVideos > 0)
                .draftLessons(draftLessons)
                .draftAssessments(draftAssessments)
                .pendingVideos(pendingVideos)
                .build();
    }

    @Transactional
    public CourseResponse publish(Long courseId, Long userId) {
        Course course = ownershipGuard.requireOwnedCourse(courseId, userId);
        assertNotSuspended(course);
        if (course.getStatus() != CourseStatus.DRAFT && course.getStatus() != CourseStatus.UNPUBLISHED) {
            throw new AppException(ErrorCode.COURSE_INVALID_STATUS_TRANSITION,
                    "Chỉ publish được khóa học ở trạng thái DRAFT hoặc UNPUBLISHED");
        }

        List<PublishIssue> issues = publishValidator.validate(course);
        if (!issues.isEmpty()) {
            throw new CourseNotReadyException(issues);
        }

        CourseStatus from = course.getStatus();
        if (lessonRepository != null && assessmentRepository != null) {
            applyPendingChanges(courseId);
            List<Lesson> lessons = curriculumLessons(courseId);
            lessons.forEach(lesson -> lesson.setPublicationStatus(PublicationStatus.PUBLISHED));
            lessonRepository.saveAll(lessons);
            List<Assessment> assessments = curriculumAssessments(courseId);
            assessments.forEach(assessment -> assessment.setPublicationStatus(PublicationStatus.PUBLISHED));
            assessmentRepository.saveAll(assessments);
        }
        course.setStatus(CourseStatus.PUBLISHED);
        if (course.getPublishedAt() == null) {
            course.setPublishedAt(Instant.now());
        }
        courseRepository.save(course);
        writeLog(course, userId, from, CourseStatus.PUBLISHED, null);
        recalculateActiveEnrollments(courseId);

        log.info("Course [{}] published by user [{}] (from {})", courseId, userId, from);
        return courseService.getDetail(courseId, userId);
    }

    @Transactional
    public PublishChangesResponse publishChanges(Long courseId, Long userId) {
        ownershipGuard.requireOwnedCourse(courseId, userId);
        Course course = courseRepository.findForPublishUpdate(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        assertNotSuspended(course);
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            // Initial publish and republish use the same atomic application.
            List<Lesson> lessons = curriculumLessons(courseId);
            List<Assessment> assessments = curriculumAssessments(courseId);
            long draftLessons = lessons.stream().filter(l -> l.getPublicationStatus() == PublicationStatus.DRAFT).count();
            long draftAssessments = assessments.stream().filter(a -> a.getPublicationStatus() == PublicationStatus.DRAFT).count();
            long videoReplacements = lessons.stream().filter(l -> l.getPendingUploadStatus() == LessonUploadStatus.READY).count();
            publish(courseId, userId);
            return PublishChangesResponse.builder()
                    .curriculum(curriculumService.getCurriculum(courseId, userId))
                    .publishedLessons(draftLessons)
                    .publishedAssessments(draftAssessments)
                    .publishedVideoReplacements(videoReplacements)
                    .build();
        }
        List<PublishIssue> issues = publishValidator.validate(course);
        if (!issues.isEmpty()) throw new CourseNotReadyException(issues);
        List<Lesson> lessons = curriculumLessons(courseId);
        long draftLessons = lessons.stream().filter(l -> l.getPublicationStatus() == PublicationStatus.DRAFT).count();
        long videoReplacements = lessons.stream().filter(l -> l.getPendingUploadStatus() == LessonUploadStatus.READY).count();
        long draftAssessments = curriculumAssessments(courseId).stream()
                .filter(a -> a.getPublicationStatus() == PublicationStatus.DRAFT).count();
        applyPendingChanges(courseId);
        for (Lesson lesson : lessons) lesson.setPublicationStatus(PublicationStatus.PUBLISHED);
        for (Assessment assessment : curriculumAssessments(courseId)) assessment.setPublicationStatus(PublicationStatus.PUBLISHED);
        long publishedLessons = draftLessons;
        long publishedAssessments = draftAssessments;
        lessonRepository.saveAll(lessons);
        assessmentRepository.saveAll(curriculumAssessments(courseId));
        recalculateActiveEnrollments(courseId);
        return PublishChangesResponse.builder()
                .curriculum(curriculumService.getCurriculum(courseId, userId))
                .publishedLessons(publishedLessons)
                .publishedAssessments(publishedAssessments)
                .publishedVideoReplacements(videoReplacements)
                .build();
    }

    @Transactional
    public CourseResponse unpublish(Long courseId, UnpublishRequest request, Long userId) {
        Course course = ownershipGuard.requireOwnedCourse(courseId, userId);
        assertNotSuspended(course);
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new AppException(ErrorCode.COURSE_INVALID_STATUS_TRANSITION,
                    "Chỉ gỡ được khóa học đang PUBLISHED");
        }

        CourseStatus from = course.getStatus();
        course.setStatus(CourseStatus.UNPUBLISHED);
        courseRepository.save(course);
        writeLog(course, userId, from, CourseStatus.UNPUBLISHED,
                request != null ? request.getReason() : null);

        log.info("Course [{}] unpublished by user [{}]", courseId, userId);
        return courseService.getDetail(courseId, userId);
    }

    @Transactional(readOnly = true)
    public List<CourseStatusLogResponse> getStatusLogs(Long courseId, Long userId) {
        ownershipGuard.requireOwnedCourse(courseId, userId);
        return statusLogRepository.findByCourseIdWithActor(courseId).stream()
                .map(CourseStatusLogResponse::fromEntity)
                .toList();
    }

    // ---- Helpers ----------------------------------------------------------

    private void assertNotSuspended(Course course) {
        if (course.getStatus() == CourseStatus.SUSPENDED) {
            throw new AppException(ErrorCode.COURSE_LOCKED_BY_ADMIN);
        }
    }

    private void writeLog(Course course, Long actorId, CourseStatus from, CourseStatus to, String comment) {
        CourseStatusLog entry = CourseStatusLog.builder()
                .course(course)
                .actor(userRepository.getReferenceById(actorId))
                .fromStatus(from)
                .toStatus(to)
                .comment(comment)
                .build();
        statusLogRepository.save(entry);
    }

    private List<Lesson> curriculumLessons(Long courseId) {
        return lessonRepository == null ? List.of() : lessonRepository.findBySection_Course_Id(courseId);
    }

    private List<Assessment> curriculumAssessments(Long courseId) {
        return assessmentRepository == null ? List.of() : assessmentRepository.findByCourse_Id(courseId);
    }

    private void applyPendingChanges(Long courseId) {
        for (Lesson lesson : curriculumLessons(courseId)) {
            if (lesson.getPendingUploadStatus() == LessonUploadStatus.READY) {
                lesson.setStorageKey(lesson.getPendingStorageKey());
                lesson.setOriginalFileName(lesson.getPendingOriginalFileName());
                lesson.setFileSizeBytes(lesson.getPendingFileSizeBytes());
                lesson.setMimeType(lesson.getPendingMimeType());
                lesson.setDurationSeconds(lesson.getPendingDurationSeconds());
                lesson.setUploadStatus(LessonUploadStatus.READY);
                lesson.setPendingStorageKey(null);
                lesson.setPendingOriginalFileName(null);
                lesson.setPendingFileSizeBytes(null);
                lesson.setPendingMimeType(null);
                lesson.setPendingDurationSeconds(0);
                lesson.setPendingUploadStatus(null);
            }
        }
    }

    private void recalculateActiveEnrollments(Long courseId) {
        if (enrollmentRepository == null) return;
        enrollmentRepository.findByCourse_Id(courseId).stream()
                .filter(e -> e.getStatus().name().equals("ACTIVE"))
                .forEach(e -> enrollmentRepository.recalculateCourseProgress(e.getId(), courseId));
    }

}
