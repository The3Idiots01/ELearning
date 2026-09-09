package com.learnova.elearning.module.qa;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.qa.dto.request.CreateAnswerRequest;
import com.learnova.elearning.module.qa.dto.request.CreateQuestionRequest;
import com.learnova.elearning.module.qa.entity.CourseQuestion;
import com.learnova.elearning.module.qa.repository.CourseAnswerRepository;
import com.learnova.elearning.module.qa.repository.CourseQuestionRepository;
import com.learnova.elearning.module.qa.service.impl.QaServiceImpl;
import com.learnova.elearning.module.review.moderation.ContentModerationService;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.entity.enums.UserRole;
import com.learnova.elearning.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QaContentModerationTest {

    @Mock
    private CourseQuestionRepository questionRepository;
    @Mock
    private CourseAnswerRepository answerRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private ContentModerationService contentModerationService;

    private QaServiceImpl qaService;

    private Course sampleCourse;
    private Lesson sampleLesson;
    private User sampleUser;
    private User lecturer;

    @BeforeEach
    void setUp() {
        qaService = new QaServiceImpl(
                questionRepository,
                answerRepository,
                courseRepository,
                lessonRepository,
                userRepository,
                enrollmentRepository,
                contentModerationService
        );

        lecturer = User.builder().id(10L).fullName("Lecturer").role(UserRole.USER).build();
        sampleCourse = Course.builder().id(1L).title("Spring Boot Master").lecturer(lecturer).build();
        sampleLesson = Lesson.builder().id(20L).title("Lesson 1").build();
        sampleUser = User.builder().id(100L).fullName("Student A").role(UserRole.USER).build();
    }

    @Test
    @DisplayName("Create question throws QA_CONTENT_VIOLATION when title or content contains inappropriate language")
    void createQuestion_fails_whenContentModerationFails() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(lessonRepository.findByIdAndSection_Course_Id(20L, 1L)).thenReturn(Optional.of(sampleLesson));
        when(userRepository.findById(100L)).thenReturn(Optional.of(sampleUser));
        when(enrollmentRepository.existsByStudent_IdAndCourse_Id(100L, 1L)).thenReturn(true);

        doThrow(new AppException(ErrorCode.QA_CONTENT_VIOLATION))
                .when(contentModerationService).validateQaQuestion("Tiêu đề xấu", "Nội dung phản cảm");

        CreateQuestionRequest request = new CreateQuestionRequest();
        request.setTitle("Tiêu đề xấu");
        request.setContent("Nội dung phản cảm");

        assertThatThrownBy(() -> qaService.createQuestion(1L, 20L, request, 100L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QA_CONTENT_VIOLATION);

        verify(questionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create answer throws QA_CONTENT_VIOLATION when answer content fails moderation")
    void createAnswer_fails_whenContentModerationFails() {
        CourseQuestion question = CourseQuestion.builder()
                .id(50L)
                .course(sampleCourse)
                .lesson(sampleLesson)
                .user(sampleUser)
                .build();

        when(questionRepository.findById(50L)).thenReturn(Optional.of(question));
        when(userRepository.findById(100L)).thenReturn(Optional.of(sampleUser));
        when(enrollmentRepository.existsByStudent_IdAndCourse_Id(100L, 1L)).thenReturn(true);

        doThrow(new AppException(ErrorCode.QA_CONTENT_VIOLATION))
                .when(contentModerationService).validateQaAnswer("Phản hồi xúc phạm");

        CreateAnswerRequest request = new CreateAnswerRequest();
        request.setContent("Phản hồi xúc phạm");

        assertThatThrownBy(() -> qaService.createAnswer(1L, 50L, request, 100L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QA_CONTENT_VIOLATION);

        verify(answerRepository, never()).save(any());
    }
}
