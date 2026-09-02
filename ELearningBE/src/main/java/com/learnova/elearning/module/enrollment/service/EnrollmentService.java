package com.learnova.elearning.module.enrollment.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.integration.storage.StorageProperties;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.enrollment.dto.response.EnrolledCourseResponse;
import com.learnova.elearning.module.enrollment.dto.response.EnrollmentResponse;
import com.learnova.elearning.module.enrollment.dto.response.ProgressResponse;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.entity.LessonProgress;
import com.learnova.elearning.module.enrollment.entity.enums.EnrollmentStatus;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.enrollment.repository.LessonProgressRepository;
import com.learnova.elearning.module.payment.entity.enums.PaymentStatus;
import com.learnova.elearning.module.payment.repository.PaymentOrderRepository;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository progressRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final StorageService storageService;
    private final StorageProperties storageProperties;

    @Transactional
    public EnrollmentResponse enroll(Long courseId, Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        // BR-EN-01: Course must be PUBLISHED to allow enrollment
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new AppException(ErrorCode.COURSE_NOT_PUBLISHED);
        }

        // BR-EN-02: Lecturer cannot enroll in their own course
        if (course.getLecturer().getId().equals(studentId)) {
            throw new AppException(ErrorCode.LECTURER_CANNOT_ENROLL_OWN_COURSE);
        }

        // BR-EN-03: Cannot enroll duplicate active/completed enrollments
        if (enrollmentRepository.existsByStudent_IdAndCourse_Id(studentId, courseId)) {
            throw new AppException(ErrorCode.ALREADY_ENROLLED);
        }

        // BR-EN-05: If course is paid (> 0), check for paid payment order
        if (course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            boolean isPaid = paymentOrderRepository.existsByStudent_IdAndCourse_IdAndStatus(studentId, courseId, PaymentStatus.PAID);
            if (!isPaid) {
                throw new AppException(ErrorCode.PAYMENT_REQUIRED);
            }
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .progress(BigDecimal.ZERO)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);

        // BR-EN-04: Update total_students counter in Course
        int currentStudents = course.getTotalStudents() != null ? course.getTotalStudents() : 0;
        course.setTotalStudents(currentStudents + 1);
        courseRepository.save(course);

        return toEnrollmentResponse(saved);
    }

    @Transactional
    public ProgressResponse completeLesson(Long courseId, Long lessonId, Long studentId) {
        Enrollment enrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(studentId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        // Check if lesson belongs to course
        if (!lesson.getSection().getCourse().getId().equals(courseId)) {
            throw new AppException(ErrorCode.LESSON_NOT_IN_COURSE);
        }

        // Mark completed if not already marked
        if (!progressRepository.existsByEnrollment_IdAndLesson_Id(enrollment.getId(), lessonId)) {
            LessonProgress progress = LessonProgress.builder()
                    .enrollment(enrollment)
                    .lesson(lesson)
                    .build();
            progressRepository.save(progress);
        }

        // Calculate progress percentage
        long totalLessons = lessonRepository.countBySection_Course_Id(courseId);
        long completedLessons = progressRepository.countByEnrollment_Id(enrollment.getId());

        BigDecimal progressPercent = BigDecimal.ZERO;
        if (totalLessons > 0) {
            double percent = ((double) completedLessons / totalLessons) * 100.00;
            progressPercent = BigDecimal.valueOf(percent).setScale(2, RoundingMode.HALF_UP);
        }

        enrollment.setProgress(progressPercent);

        // Complete enrollment if progress reaches 100%
        if (progressPercent.compareTo(new BigDecimal("100.00")) >= 0) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollment.setCompletedAt(Instant.now());
        } else {
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollment.setCompletedAt(null);
        }
        enrollmentRepository.save(enrollment);

        return ProgressResponse.builder()
                .lessonId(lessonId)
                .completed(true)
                .totalProgress(progressPercent)
                .courseStatus(enrollment.getStatus().name())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isStudentEnrolled(Long courseId, Long studentId) {
        return enrollmentRepository.existsByStudent_IdAndCourse_Id(studentId, courseId);
    }

    @Transactional(readOnly = true)
    public List<EnrolledCourseResponse> getEnrolledCourses(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudent_IdOrderByEnrolledAtDesc(studentId);
        return enrollments.stream().map(this::toEnrolledCourseResponse).toList();
    }

    private EnrolledCourseResponse toEnrolledCourseResponse(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        String thumbnailUrl = null;
        if (course.getThumbnailKey() != null) {
            thumbnailUrl = storageService.presignDownload(course.getThumbnailKey(), storageProperties.getDownloadTtl());
        }

        return EnrolledCourseResponse.builder()
                .enrollmentId(enrollment.getId())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .courseSlug(course.getSlug())
                .courseThumbnailUrl(thumbnailUrl)
                .lecturerName(course.getLecturer() != null ? course.getLecturer().getFullName() : null)
                .categoryName(course.getCategory() != null ? course.getCategory().getName() : null)
                .level(course.getLevel() != null ? course.getLevel().name() : null)
                .progress(enrollment.getProgress())
                .status(enrollment.getStatus().name())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .build();
    }

    private EnrollmentResponse toEnrollmentResponse(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .courseId(enrollment.getCourse().getId())
                .status(enrollment.getStatus().name())
                .progress(enrollment.getProgress())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .build();
    }
}
