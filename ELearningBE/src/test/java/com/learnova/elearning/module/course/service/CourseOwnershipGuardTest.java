package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.LearningOutcome;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.course.repository.CourseSectionRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.course.repository.AssessmentRepository;
import com.learnova.elearning.module.course.repository.LearningOutcomeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseOwnershipGuardTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseSectionRepository sectionRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LearningOutcomeRepository outcomeRepository;
    @Mock
    private AssessmentRepository assessmentRepository;

    @InjectMocks
    private CourseOwnershipGuard guard;

    private Course course(Long id, CourseStatus status) {
        return Course.builder().id(id).status(status).build();
    }

    @Test
    @DisplayName("requireOwnedCourse: trả về course khi đúng chủ sở hữu")
    void requireOwnedCourse_ownerOk() {
        Course course = course(1L, CourseStatus.DRAFT);
        when(courseRepository.findByIdAndLecturer_Id(1L, 10L)).thenReturn(Optional.of(course));

        assertThat(guard.requireOwnedCourse(1L, 10L)).isSameAs(course);
    }

    @Test
    @DisplayName("requireOwnedCourse: course tồn tại nhưng không phải của mình -> 403 (admin cũng bị chặn)")
    void requireOwnedCourse_notOwner_forbidden() {
        when(courseRepository.findByIdAndLecturer_Id(1L, 99L)).thenReturn(Optional.empty());
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, CourseStatus.DRAFT)));

        assertThatThrownBy(() -> guard.requireOwnedCourse(1L, 99L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COURSE_ACCESS_DENIED);
    }

    @Test
    @DisplayName("requireOwnedCourse: course không tồn tại -> 404")
    void requireOwnedCourse_notFound() {
        when(courseRepository.findByIdAndLecturer_Id(1L, 10L)).thenReturn(Optional.empty());
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireOwnedCourse(1L, 10L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("requireEditableCourse: course bị admin SUSPENDED -> khóa chỉnh sửa")
    void requireEditableCourse_suspended_locked() {
        Course suspended = course(1L, CourseStatus.SUSPENDED);
        when(courseRepository.findByIdAndLecturer_Id(1L, 10L)).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> guard.requireEditableCourse(1L, 10L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COURSE_LOCKED_BY_ADMIN);
    }

    @Test
    @DisplayName("requireOutcomeInCourse: outcome thuộc course khác -> từ chối")
    void requireOutcomeInCourse_wrongCourse() {
        LearningOutcome outcome = LearningOutcome.builder().id(21L).build();
        when(outcomeRepository.findByIdAndCourse_Id(21L, 1L)).thenReturn(Optional.empty());
        when(outcomeRepository.findById(21L)).thenReturn(Optional.of(outcome));

        assertThatThrownBy(() -> guard.requireOutcomeInCourse(21L, 1L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.OUTCOME_NOT_IN_COURSE);
    }

    @Test
    @DisplayName("requireAssessmentInCourse: assessment thuộc course khác -> từ chối")
    void requireAssessmentInCourse_wrongCourse() {
        Assessment assessment = Assessment.builder().id(31L).build();
        when(assessmentRepository.findWithDetailsByIdAndCourse_Id(31L, 1L)).thenReturn(Optional.empty());
        when(assessmentRepository.findById(31L)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> guard.requireAssessmentInCourse(31L, 1L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ASSESSMENT_NOT_IN_COURSE);
    }
}
