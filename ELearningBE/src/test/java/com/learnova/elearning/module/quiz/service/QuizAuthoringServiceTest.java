package com.learnova.elearning.module.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.LearningOutcome;
import com.learnova.elearning.module.course.entity.enums.AssessmentType;
import com.learnova.elearning.module.course.repository.LearningOutcomeRepository;
import com.learnova.elearning.module.course.service.CourseOwnershipGuard;
import com.learnova.elearning.module.quiz.dto.QuizOptionDto;
import com.learnova.elearning.module.quiz.dto.request.UpsertQuestionRequest;
import com.learnova.elearning.module.quiz.dto.request.UpsertQuizRequest;
import com.learnova.elearning.module.quiz.dto.request.ApplyQuizDraftRequest;
import com.learnova.elearning.module.quiz.dto.response.QuizQuestionDraft;
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
    @Mock private LearningOutcomeRepository outcomeRepository;
    private QuizAuthoringService service;

    private Course course;
    private Assessment assessment;

    @BeforeEach
    void setUp() {
        service = new QuizAuthoringService(quizRepository, questionRepository,
                ownershipGuard, outcomeRepository, new ObjectMapper());
        course = Course.builder().id(1L).build();
        assessment = Assessment.builder().id(31L).course(course).type(AssessmentType.QUIZ)
                .title("REST API assessment")
                .outcomes(new LinkedHashSet<>()).build();
    }

    @Test
    void upsertUsesAssessmentAndAssessmentTitle() {
        UpsertQuizRequest request = UpsertQuizRequest.builder()
                .title("  REST API final quiz  ")
                .instructions("  Làm trong 20 phút  ")
                .outcomeIds(List.of(10L))
                .passingScore(new BigDecimal("70.00")).maxAttempts(2).build();
        LearningOutcome outcome = LearningOutcome.builder().id(10L).statement("Build API").build();
        when(ownershipGuard.requireAssessmentInCourse(31L, 1L)).thenReturn(assessment);
        when(quizRepository.findByAssessment_Id(31L)).thenReturn(Optional.empty());
        when(outcomeRepository.findByCourse_IdAndIdIn(eq(1L), anySet())).thenReturn(List.of(outcome));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz quiz = invocation.getArgument(0);
            quiz.setId(41L);
            return quiz;
        });
        when(questionRepository.findByQuiz_IdOrderByPositionAsc(41L)).thenReturn(List.of());

        var response = service.upsertQuiz(1L, 31L, request, 9L);

        assertThat(response.getAssessmentId()).isEqualTo(31L);
        assertThat(response.getTitle()).isEqualTo("REST API final quiz");
        assertThat(assessment.getInstructions()).isEqualTo("Làm trong 20 phút");
        assertThat(assessment.getOutcomes()).containsExactly(outcome);
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

    @Test
    void applyAiDraftOnlyAppendsQuestionsAndKeepsGeneralSettings() {
        Quiz quiz = Quiz.builder().id(41L).assessment(assessment).build();
        QuizQuestionDraft question = new QuizQuestionDraft(
                "Generated question", QuestionType.SINGLE_CHOICE, BigDecimal.ONE,
                questionRequest().getOptions());
        ApplyQuizDraftRequest request = new ApplyQuizDraftRequest(List.of(question));
        when(ownershipGuard.requireAssessmentInCourse(31L, 1L)).thenReturn(assessment);
        when(quizRepository.findByAssessment_Id(31L)).thenReturn(Optional.of(quiz));
        when(questionRepository.countByQuiz_Id(41L)).thenReturn(2);
        when(questionRepository.findByQuiz_IdOrderByPositionAsc(41L)).thenReturn(List.of());

        service.applyAiDraft(1L, 31L, request, 9L);

        assertThat(assessment.getTitle()).isEqualTo("REST API assessment");
        assertThat(quiz.getPassingScore()).isEqualByComparingTo("80");
        @SuppressWarnings("unchecked")
        var questions = (List<QuizQuestion>) mockingDetails(questionRepository)
                .getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("saveAll"))
                .findFirst().orElseThrow().getArgument(0);
        assertThat(questions).singleElement().satisfies(saved -> {
            assertThat(saved.getPosition()).isEqualTo(2);
            assertThat(saved.getQuestionText()).isEqualTo("Generated question");
        });
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
