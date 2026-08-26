package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.dto.request.UnpublishRequest;
import com.learnova.elearning.module.course.dto.response.CourseResponse;
import com.learnova.elearning.module.course.dto.response.CourseStatusLogResponse;
import com.learnova.elearning.module.course.dto.response.PublishCheckResponse;
import com.learnova.elearning.module.course.dto.response.PublishIssue;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.CourseStatusLog;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.exception.CourseNotReadyException;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.course.repository.CourseStatusLogRepository;
import com.learnova.elearning.module.user.repository.UserRepository;
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

    @Transactional(readOnly = true)
    public PublishCheckResponse publishCheck(Long courseId, Long userId) {
        Course course = ownershipGuard.requireOwnedCourse(courseId, userId);
        List<PublishIssue> issues = publishValidator.validate(course);
        return PublishCheckResponse.builder()
                .canPublish(issues.isEmpty())
                .issues(issues)
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
        course.setStatus(CourseStatus.PUBLISHED);
        if (course.getPublishedAt() == null) {
            course.setPublishedAt(Instant.now());
        }
        courseRepository.save(course);
        writeLog(course, userId, from, CourseStatus.PUBLISHED, null);

        log.info("Course [{}] published by user [{}] (from {})", courseId, userId, from);
        return courseService.getDetail(courseId, userId);
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
}
