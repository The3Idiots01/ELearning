package com.learnova.elearning.module.delivery.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.entity.enums.PublicationStatus;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.entity.enums.EnrollmentStatus;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.user.entity.enums.UserRole;
import com.learnova.elearning.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Điểm ra quyết định duy nhất cho quyền truy cập nội dung — §4.3
 * design_us15_us17.md. Mọi đường vào nội dung (cấp ticket lẫn gateway) đều
 * phải đi qua đây; không kiểm quyền rải rác ở nơi khác.
 */
@Component
@RequiredArgsConstructor
public class ContentAccessGuard {

    private static final Set<EnrollmentStatus> ALLOWED_ENROLLMENT_STATUSES =
            EnumSet.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED);

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    /**
     * Thứ tự kiểm dừng ở điều kiện đầu tiên khớp (§4.3):
     * course chưa publish → chỉ owner/admin; lesson preview → công khai;
     * còn lại bắt buộc đăng nhập và có enrollment hiệu lực.
     */
    public AccessDecision decide(Lesson lesson, Long userId) {
        Course course = lesson.getSection().getCourse();

        if (lesson.getPublicationStatus() != null
                && lesson.getPublicationStatus() != PublicationStatus.PUBLISHED) {
            if (userId != null && (isLecturerOfCourse(course, userId) || isAdmin(userId))) {
                return new AccessDecision(true, AccessScope.OWNER, null);
            }
            throw new AppException(ErrorCode.LESSON_NOT_FOUND);
        }

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            if (userId != null && (isLecturerOfCourse(course, userId) || isAdmin(userId))) {
                return new AccessDecision(true, AccessScope.OWNER, null);
            }
            // Không lộ sự tồn tại của course chưa publish cho người ngoài (BR-24).
            throw new AppException(ErrorCode.COURSE_NOT_FOUND);
        }

        if (Boolean.TRUE.equals(lesson.getIsPreview())) {
            return new AccessDecision(true, AccessScope.PREVIEW, null);
        }

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(userId, course.getId())
                .filter(e -> ALLOWED_ENROLLMENT_STATUSES.contains(e.getStatus()))
                .orElseThrow(() -> new AppException(ErrorCode.CONTENT_ACCESS_DENIED));

        return new AccessDecision(true, AccessScope.ENROLLED, enrollment.getId());
    }

    private boolean isLecturerOfCourse(Course course, Long userId) {
        return course.getLecturer() != null && userId.equals(course.getLecturer().getId());
    }

    private boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRole() == UserRole.ADMIN)
                .orElse(false);
    }
}
