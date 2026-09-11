package com.learnova.elearning.module.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnova.elearning.integration.ai.GeminiClient;
import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.LearningOutcome;
import com.learnova.elearning.module.course.entity.enums.AssessmentType;
import com.learnova.elearning.module.course.repository.LearningOutcomeRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.course.service.CourseOwnershipGuard;
import com.learnova.elearning.module.quiz.dto.QuizOptionDto;
import com.learnova.elearning.module.quiz.dto.request.GenerateQuizDraftRequest;
import com.learnova.elearning.module.quiz.dto.response.QuizDraftResponse;
import com.learnova.elearning.module.quiz.dto.response.QuizQuestionDraft;
import com.learnova.elearning.module.quiz.entity.enums.QuestionType;
import com.learnova.elearning.module.quiz.entity.enums.QuizDifficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizAiAuthoringServiceTest {

    @Mock private CourseOwnershipGuard ownershipGuard;
    @Mock private LearningOutcomeRepository outcomeRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private GeminiClient geminiClient;

    private QuizAiAuthoringService service;
    private Assessment assessment;

    @BeforeEach
    void setUp() {
        service = new QuizAiAuthoringService(ownershipGuard, outcomeRepository,
                lessonRepository, geminiClient, new ObjectMapper());
        Course course = Course.builder().id(1L).title("Spring Boot").language("vi").build();
        assessment = Assessment.builder().id(31L).course(course).type(AssessmentType.QUIZ)
                .title("Bài kiểm tra cũ")
                .instructions("Làm trong 20 phút, không sử dụng tài liệu")
                .outcomes(new LinkedHashSet<>(List.of(
                        LearningOutcome.builder().id(10L).statement("Xây dựng REST API").build())))
                .build();
        when(ownershipGuard.requireEditableCourse(1L, 9L)).thenReturn(course);
        when(ownershipGuard.requireAssessmentInCourse(31L, 1L)).thenReturn(assessment);
        when(lessonRepository.findBySection_Course_Id(1L)).thenReturn(List.of());
    }

    @Test
    void generationUsesSavedAssessmentContextAndRequestedQuestionSettings() {
        QuizDraftResponse aiDraft = new QuizDraftResponse(
                new BigDecimal("2"), QuizDifficulty.EASY,
                List.of(question("Câu 1"), question("Câu 2")));
        when(geminiClient.generateStructured(org.mockito.ArgumentMatchers.argThat(prompt ->
                        prompt.contains("Bài kiểm tra cũ")
                                && prompt.contains("Làm trong 20 phút")
                                && prompt.contains("Xây dựng REST API")),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                eq(QuizDraftResponse.class))).thenReturn(aiDraft);

        QuizDraftResponse result = service.generateDraft(1L, 31L,
                new GenerateQuizDraftRequest(2, new BigDecimal("5"), QuizDifficulty.HARD,
                        QuestionType.SINGLE_CHOICE, "validation", "tình huống"), 9L);

        assertThat(result.difficulty()).isEqualTo(QuizDifficulty.HARD);
        assertThat(result.questions()).hasSize(2);
        assertThat(result.questions()).extracting(QuizQuestionDraft::points)
                .containsExactly(new BigDecimal("2.50"), new BigDecimal("2.50"));
        assertThat(result.totalPoints()).isEqualByComparingTo("5.00");
    }

    private QuizQuestionDraft question(String text) {
        return new QuizQuestionDraft(text, QuestionType.SINGLE_CHOICE, BigDecimal.ONE, List.of(
                QuizOptionDto.builder().id("a").text("Đúng").isCorrect(true).explanation("Vì đúng").build(),
                QuizOptionDto.builder().id("b").text("Sai").isCorrect(false).explanation("Vì sai").build()));
    }
}
