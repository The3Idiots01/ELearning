package com.learnova.elearning.module.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.enums.AssessmentType;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.repository.AssessmentProgressRepository;
import com.learnova.elearning.module.enrollment.repository.EnrollmentRepository;
import com.learnova.elearning.module.quiz.dto.QuizAttemptSnapshot;
import com.learnova.elearning.module.quiz.dto.QuizOptionDto;
import com.learnova.elearning.module.quiz.dto.request.SubmitQuizAttemptRequest;
import com.learnova.elearning.module.quiz.entity.Quiz;
import com.learnova.elearning.module.quiz.entity.QuizAttempt;
import com.learnova.elearning.module.quiz.entity.QuizQuestion;
import com.learnova.elearning.module.quiz.entity.enums.QuestionType;
import com.learnova.elearning.module.quiz.repository.QuizAttemptRepository;
import com.learnova.elearning.module.quiz.repository.QuizQuestionRepository;
import com.learnova.elearning.module.quiz.repository.QuizRepository;
import com.learnova.elearning.module.user.entity.User;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizTakingServiceTest {

    @Mock private QuizRepository quizRepository;
    @Mock private QuizQuestionRepository questionRepository;
    @Mock private QuizAttemptRepository attemptRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private AssessmentProgressRepository progressRepository;
    private ObjectMapper objectMapper;
    private QuizTakingService service;

    private Enrollment enrollment;
    private Quiz quiz;
    private QuizQuestion single;
    private QuizQuestion multiple;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        service = new QuizTakingService(quizRepository, questionRepository, attemptRepository,
                enrollmentRepository, progressRepository, objectMapper);
        Course course = Course.builder().id(1L).build();
        Assessment assessment = Assessment.builder().id(31L).course(course)
                .type(AssessmentType.QUIZ).title("Assessment").build();
        quiz = Quiz.builder().id(41L).assessment(assessment)
                .passingScore(new BigDecimal("70.00")).maxAttempts(3).build();
        User learner = User.builder().id(9L).build();
        enrollment = Enrollment.builder().id(61L).course(course).student(learner).build();
        single = question(51L, QuestionType.SINGLE_CHOICE, "2.00", List.of(
                option("a", true, "A is correct"), option("b", false, null)));
        multiple = question(52L, QuestionType.MULTIPLE_CHOICE, "3.00", List.of(
                option("a", true, null), option("b", true, null), option("c", false, null)));
    }

    @Test
    void takingResponseDoesNotLeakCorrectAnswersOrExplanations() throws Exception {
        when(enrollmentRepository.findByStudent_IdAndCourse_Id(9L, 1L)).thenReturn(Optional.of(enrollment));
        when(quizRepository.findByAssessment_IdAndAssessment_Course_Id(31L, 1L)).thenReturn(Optional.of(quiz));
        when(questionRepository.findByQuiz_IdOrderByPositionAsc(41L)).thenReturn(List.of(single));

        var response = service.getQuizForTaking(1L, 31L, 9L);
        String json = objectMapper.writeValueAsString(response);

        assertThat(response.getAssessmentId()).isEqualTo(31L);
        assertThat(json).doesNotContain("isCorrect", "explanation");
        assertThat(response.getQuestions().getFirst().getOptions())
                .extracting(item -> item.getText()).containsExactly("a", "b");
    }

    @Test
    void submitGradesSingleAndMultipleChoiceAndMarksProgress() {
        stubSubmissionPrerequisites();
        SubmitQuizAttemptRequest request = SubmitQuizAttemptRequest.builder().answers(List.of(
                answer(51L, "a"), answer(52L, "a", "b"))).build();
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> {
            QuizAttempt attempt = invocation.getArgument(0);
            attempt.setId(71L);
            attempt.setSubmittedAt(Instant.parse("2026-01-01T00:00:00Z"));
            return attempt;
        });

        var response = service.submitAttempt(1L, 31L, request, 9L);

        assertThat(response.getScore()).isEqualByComparingTo("100.00");
        assertThat(response.getIsPassed()).isTrue();
        assertThat(response.getCorrectQuestions()).isEqualTo(2);
        assertThat(response.getQuestionResults()).hasSize(2);
        verify(progressRepository).markCompletedByQuiz(61L, 31L);
    }

    @Test
    void submitRequiresExactMultipleChoiceSet() {
        stubSubmissionPrerequisites();
        quiz.setPassingScore(new BigDecimal("100.00"));
        SubmitQuizAttemptRequest request = SubmitQuizAttemptRequest.builder().answers(List.of(
                answer(51L, "a"), answer(52L, "a"))).build();
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.submitAttempt(1L, 31L, request, 9L);

        assertThat(response.getScore()).isEqualByComparingTo("40.00");
        assertThat(response.getIsPassed()).isFalse();
        verify(progressRepository, never()).markCompletedByQuiz(anyLong(), anyLong());
    }

    @Test
    void submitRejectsWhenMaxAttemptsReached() {
        when(enrollmentRepository.findByStudent_IdAndCourse_Id(9L, 1L)).thenReturn(Optional.of(enrollment));
        when(quizRepository.findForUpdateByAssessmentAndCourse(31L, 1L)).thenReturn(Optional.of(quiz));
        when(attemptRepository.countByQuiz_IdAndLearner_Id(41L, 9L)).thenReturn(3);

        assertThatThrownBy(() -> service.submitAttempt(1L, 31L,
                SubmitQuizAttemptRequest.builder().answers(List.of(answer(51L, "a"))).build(), 9L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUIZ_MAX_ATTEMPTS_REACHED);
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void attemptHistoryUsesSavedSnapshot() throws Exception {
        when(enrollmentRepository.findByStudent_IdAndCourse_Id(9L, 1L)).thenReturn(Optional.of(enrollment));
        when(quizRepository.findByAssessment_IdAndAssessment_Course_Id(31L, 1L)).thenReturn(Optional.of(quiz));
        QuizAttemptSnapshot snapshot = QuizAttemptSnapshot.builder().version(1)
                .passingScore(new BigDecimal("70.00")).totalQuestions(2).correctQuestions(1)
                .questionResults(List.of()).build();
        QuizAttempt attempt = QuizAttempt.builder().id(71L).quiz(quiz).learner(enrollment.getStudent())
                .answersJson(objectMapper.writeValueAsString(snapshot)).score(new BigDecimal("40.00"))
                .isPassed(false).submittedAt(Instant.parse("2026-01-01T00:00:00Z")).build();
        when(attemptRepository.findByQuiz_IdAndLearner_IdOrderBySubmittedAtDesc(41L, 9L))
                .thenReturn(List.of(attempt));

        var history = service.getAttemptHistory(1L, 31L, 9L);

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().getTotalQuestions()).isEqualTo(2);
        assertThat(history.getFirst().getCorrectQuestions()).isEqualTo(1);
    }

    private void stubSubmissionPrerequisites() {
        when(enrollmentRepository.findByStudent_IdAndCourse_Id(9L, 1L)).thenReturn(Optional.of(enrollment));
        when(quizRepository.findForUpdateByAssessmentAndCourse(31L, 1L)).thenReturn(Optional.of(quiz));
        when(questionRepository.findByQuiz_IdOrderByPositionAsc(41L)).thenReturn(List.of(single, multiple));
    }

    private QuizQuestion question(Long id, QuestionType type, String points,
                                  List<QuizOptionDto> options) throws Exception {
        return QuizQuestion.builder().id(id).quiz(quiz).questionText("Q" + id).questionType(type)
                .points(new BigDecimal(points)).position(id.intValue())
                .optionsJson(objectMapper.writeValueAsString(options)).build();
    }

    private QuizOptionDto option(String id, boolean correct, String explanation) {
        return QuizOptionDto.builder().id(id).text(id).isCorrect(correct).explanation(explanation).build();
    }

    private SubmitQuizAttemptRequest.QuestionAnswerItem answer(Long questionId, String... optionIds) {
        return SubmitQuizAttemptRequest.QuestionAnswerItem.builder()
                .questionId(questionId).selectedOptionIds(List.of(optionIds)).build();
    }
}
