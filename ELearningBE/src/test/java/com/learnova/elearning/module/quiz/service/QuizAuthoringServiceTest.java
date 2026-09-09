package com.learnova.elearning.module.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.LearningOutcome;
import com.learnova.elearning.module.course.entity.enums.AssessmentType;
import com.learnova.elearning.module.course.service.CourseOwnershipGuard;
import com.learnova.elearning.module.quiz.dto.QuizOptionDto;
import com.learnova.elearning.module.quiz.dto.request.UpsertQuestionRequest;
import com.learnova.elearning.module.quiz.dto.request.UpsertQuizRequest;
import com.learnova.elearning.module.quiz.entity.Quiz;
import com.learnova.elearning.module.quiz.entity.QuizQuestion;
import com.learnova.elearning.module.quiz.entity.enums.QuestionType;
import com.learnova.elearning.module.quiz.repository.QuizQuestionRepository;
import com.learnova.elearning.module.quiz.repository.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizAuthoringServiceTest {

    @Mock private QuizRepository quizRepository;
    @Mock private QuizQuestionRepository questionRepository;
    @Mock private CourseOwnershipGuard ownershipGuard;
    private QuizAuthoringService service;

    private Course course;
    private Assessment assessment;

    @BeforeEach
    void setUp() {
        service = new QuizAuthoringService(quizRepository, questionRepository,
                ownershipGuard, new ObjectMapper());
        course = Course.builder().id(1L).build();
        assessment = Assessment.builder().id(31L).course(course).type(AssessmentType.QUIZ)
                .title("REST API assessment")
                .outcomes(new LinkedHashSet<>()).build();
    }

    @Test
    void upsertUsesAssessmentAndAssessmentTitle() {
        UpsertQuizRequest request = UpsertQuizRequest.builder()
                .passingScore(new BigDecimal("70.00")).maxAttempts(2).build();
        when(ownershipGuard.requireAssessmentInCourse(31L, 1L)).thenReturn(assessment);
        when(quizRepository.findByAssessment_Id(31L)).thenReturn(Optional.empty());
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz quiz = invocation.getArgument(0);
            quiz.setId(41L);
            return quiz;
        });
        when(questionRepository.findByQuiz_IdOrderByPositionAsc(41L)).thenReturn(List.of());

        var response = service.upsertQuiz(1L, 31L, request, 9L);

        assertThat(response.getAssessmentId()).isEqualTo(31L);
        assertThat(response.getTitle()).isEqualTo("REST API assessment");
        assertThat(response.getPassingScore()).isEqualByComparingTo("70.00");
        verify(ownershipGuard).requireEditableCourse(1L, 9L);
    }

    @Test
    void addQuestionWithoutOutcomeMapping() {
        Quiz quiz = Quiz.builder().id(41L).assessment(assessment).build();
        UpsertQuestionRequest request = questionRequest();
        when(ownershipGuard.requireAssessmentInCourse(31L, 1L)).thenReturn(assessment);
        when(quizRepository.findByAssessment_Id(31L)).thenReturn(Optional.of(quiz));
        when(questionRepository.save(any(QuizQuestion.class))).thenAnswer(invocation -> {
            QuizQuestion question = invocation.getArgument(0);
            question.setId(51L);
            return question;
        });

        var response = service.addQuestion(1L, 31L, request, 9L);

        assertThat(response.getOptions()).extracting(QuizOptionDto::getId)
                .containsExactly("a", "b");
        assertThat(response.getQuestionText()).isEqualTo("Which annotation?");
    }

    private UpsertQuestionRequest questionRequest() {
        return UpsertQuestionRequest.builder()
                .questionText("Which annotation?")
                .questionType(QuestionType.SINGLE_CHOICE)
                .points(BigDecimal.ONE)
                .options(List.of(
                        QuizOptionDto.builder().id("a").text("@RestController").isCorrect(true).build(),
                        QuizOptionDto.builder().id("b").text("@Service").isCorrect(false).build()))
                .build();
    }
}
