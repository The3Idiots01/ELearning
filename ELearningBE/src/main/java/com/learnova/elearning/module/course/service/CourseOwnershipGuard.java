package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.CourseSection;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.course.repository.CourseSectionRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Kiểm quyền sở hữu tập trung — chống IDOR. Luôn xác thực chuỗi quan hệ
 * course -> section -> lesson theo courseId lấy từ path, không tin id trong body.
 */
@Component
@RequiredArgsConstructor
public class CourseOwnershipGuard {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;
    private final LessonRepository lessonRepository;

    /** Course tồn tại và thuộc về user hiện tại. */
    public Course requireOwnedCourse(Long courseId, Long userId) {
        return courseRepository.findByIdAndLecturer_Id(courseId, userId)
                .orElseThrow(() -> {
                    // Phân biệt "không tồn tại" với "không phải của bạn" để instructor biết mình gõ nhầm
                    if (courseRepository.findById(courseId).isPresent()) {
                        return new AppException(ErrorCode.COURSE_ACCESS_DENIED);
                    }
                    return new AppException(ErrorCode.COURSE_NOT_FOUND);
                });
    }

    /** Course thuộc về user và đang cho phép chỉnh sửa (không bị admin SUSPENDED). */
    public Course requireEditableCourse(Long courseId, Long userId) {
        Course course = requireOwnedCourse(courseId, userId);
        if (course.getStatus() == CourseStatus.SUSPENDED) {
            throw new AppException(ErrorCode.COURSE_LOCKED_BY_ADMIN);
        }
        return course;
    }

    public CourseSection requireSectionInCourse(Long sectionId, Long courseId) {
        return sectionRepository.findByIdAndCourse_Id(sectionId, courseId)
                .orElseThrow(() -> {
                    if (sectionRepository.findById(sectionId).isPresent()) {
                        return new AppException(ErrorCode.SECTION_NOT_IN_COURSE);
                    }
                    return new AppException(ErrorCode.SECTION_NOT_FOUND);
                });
    }

    public Lesson requireLessonInCourse(Long lessonId, Long courseId) {
        return lessonRepository.findByIdAndSection_Course_Id(lessonId, courseId)
                .orElseThrow(() -> {
                    if (lessonRepository.findById(lessonId).isPresent()) {
                        return new AppException(ErrorCode.LESSON_NOT_IN_COURSE);
                    }
                    return new AppException(ErrorCode.LESSON_NOT_FOUND);
                });
    }
}
