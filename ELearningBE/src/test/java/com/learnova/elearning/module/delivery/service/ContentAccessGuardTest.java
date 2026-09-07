package com.learnova.elearning.module.delivery.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.CourseSection;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.entity.enums.EnrollmentStatus;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.entity.enums.UserRole;
import com.learnova.elearning.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentAccessGuardTest {

    private static final Long LECTURER_ID = 1L;
    private static final Long LEARNER_ID = 2L;
    private static final Long ADMIN_ID = 3L;
    private static final Long STRANGER_ID = 4L;
    private static final Long COURSE_ID = 10L;

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ContentAccessGuard guard;

    private Lesson publishedNonPreviewLesson;
    private Lesson publishedPreviewLesson;
    private Lesson draftLesson;

    @BeforeEach
    void setUp() {
        User lecturer = User.builder().id(LECTURER_ID).build();

        Course publishedCourse = Course.builder()
                .id(COURSE_ID)
                .lecturer(lecturer)
                .status(CourseStatus.PUBLISHED)
                .build();
        Course draftCourse = Course.builder()
                .id(COURSE_ID)
                .lecturer(lecturer)
                .status(CourseStatus.DRAFT)
                .build();

        CourseSection publishedSection = CourseSection.builder().course(publishedCourse).build();
        CourseSection draftSection = CourseSection.builder().course(draftCourse).build();

        publishedNonPreviewLesson = Lesson.builder().id(100L).section(publishedSection).isPreview(false).build();
        publishedPreviewLesson = Lesson.builder().id(101L).section(publishedSection).isPreview(true).build();
        draftLesson = Lesson.builder().id(102L).section(draftSection).isPreview(false).build();

        lenient().when(userRepository.findById(ADMIN_ID))
                .thenReturn(Optional.of(User.builder().id(ADMIN_ID).role(UserRole.ADMIN).build()));
        lenient().when(userRepository.findById(STRANGER_ID))
                .thenReturn(Optional.of(User.builder().id(STRANGER_ID).role(UserRole.USER).build()));
        lenient().when(userRepository.findById(LEARNER_ID))
                .thenReturn(Optional.of(User.builder().id(LEARNER_ID).role(UserRole.USER).build()));
    }

    @Test
    void decide_draftCourse_lecturerOwner_allowedAsOwner() {
        AccessDecision decision = guard.decide(draftLesson, LECTURER_ID);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.scope()).isEqualTo(AccessScope.OWNER);
        assertThat(decision.enrollmentId()).isNull();
    }

    @Test
    void decide_draftCourse_admin_allowedAsOwner() {
        AccessDecision decision = guard.decide(draftLesson, ADMIN_ID);

        assertThat(decision.scope()).isEqualTo(AccessScope.OWNER);
    }

    @Test
    void decide_draftCourse_stranger_notFoundNotAccessDenied() {
        assertThatThrownBy(() -> guard.decide(draftLesson, STRANGER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void decide_draftCourse_anonymous_notFound() {
        assertThatThrownBy(() -> guard.decide(draftLesson, null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void decide_publishedPreviewLesson_anonymousGuest_allowedAsPreview() {
        AccessDecision decision = guard.decide(publishedPreviewLesson, null);

        assertThat(decision.scope()).isEqualTo(AccessScope.PREVIEW);
        assertThat(decision.enrollmentId()).isNull();
    }

    @Test
    void decide_publishedPreviewLesson_authenticatedUser_allowedAsPreview() {
        AccessDecision decision = guard.decide(publishedPreviewLesson, LEARNER_ID);

        assertThat(decision.scope()).isEqualTo(AccessScope.PREVIEW);
    }

    @Test
    void decide_publishedNonPreviewLesson_anonymous_throwsUnauthenticated() {
        assertThatThrownBy(() -> guard.decide(publishedNonPreviewLesson, null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void decide_publishedNonPreviewLesson_notEnrolled_throwsAccessDenied() {
        when(enrollmentRepository.findByStudent_IdAndCourse_Id(LEARNER_ID, COURSE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.decide(publishedNonPreviewLesson, LEARNER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_ACCESS_DENIED);
    }

    @Test
    void decide_publishedNonPreviewLesson_enrollmentCancelled_throwsAccessDenied() {
        Enrollment cancelled = Enrollment.builder().id(500L).status(EnrollmentStatus.CANCELLED).build();
        when(enrollmentRepository.findByStudent_IdAndCourse_Id(LEARNER_ID, COURSE_ID))
                .thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> guard.decide(publishedNonPreviewLesson, LEARNER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_ACCESS_DENIED);
    }

    @Test
    void decide_publishedNonPreviewLesson_enrollmentActive_allowedAsEnrolled() {
        Enrollment active = Enrollment.builder().id(500L).status(EnrollmentStatus.ACTIVE).build();
        when(enrollmentRepository.findByStudent_IdAndCourse_Id(LEARNER_ID, COURSE_ID))
                .thenReturn(Optional.of(active));

        AccessDecision decision = guard.decide(publishedNonPreviewLesson, LEARNER_ID);

        assertThat(decision.scope()).isEqualTo(AccessScope.ENROLLED);
        assertThat(decision.enrollmentId()).isEqualTo(500L);
    }

    @Test
    void decide_publishedNonPreviewLesson_enrollmentCompleted_stillAllowed() {
        Enrollment completed = Enrollment.builder().id(501L).status(EnrollmentStatus.COMPLETED).build();
        when(enrollmentRepository.findByStudent_IdAndCourse_Id(LEARNER_ID, COURSE_ID))
                .thenReturn(Optional.of(completed));

        AccessDecision decision = guard.decide(publishedNonPreviewLesson, LEARNER_ID);

        assertThat(decision.scope()).isEqualTo(AccessScope.ENROLLED);
    }
}
