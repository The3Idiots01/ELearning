package com.learnova.elearning.module.course.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnova.elearning.integration.ai.GeminiClient;
import com.learnova.elearning.module.course.dto.request.ApplyCurriculumDraftRequest;
import com.learnova.elearning.module.course.dto.request.CreateLessonRequest;
import com.learnova.elearning.module.course.dto.request.GenerateCurriculumDraftRequest;
import com.learnova.elearning.module.course.dto.response.CurriculumResponse;
import com.learnova.elearning.module.course.dto.response.CurriculumDraftResponse;
import com.learnova.elearning.module.course.dto.response.CurriculumLessonDraft;
import com.learnova.elearning.module.course.dto.response.CurriculumSectionDraft;
import com.learnova.elearning.module.course.dto.response.OutcomeSuggestion;
import com.learnova.elearning.module.course.dto.response.OutcomeSuggestionResponse;
import com.learnova.elearning.module.course.dto.response.SectionResponse;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.LearningOutcome;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.repository.CourseBulletRepository;
import com.learnova.elearning.module.course.repository.LearningOutcomeRepository;
import com.learnova.elearning.module.quiz.entity.Quiz;
import com.learnova.elearning.module.quiz.entity.QuizQuestion;
import com.learnova.elearning.module.quiz.repository.QuizQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAiAuthoringServiceTest {

    @Mock private CourseOwnershipGuard ownershipGuard;
    @Mock private LearningOutcomeRepository outcomeRepository;
    @Mock private CourseBulletRepository bulletRepository;
    @Mock private QuizQuestionRepository quizQuestionRepository;
    @Mock private LessonContentTextExtractor textExtractor;
    @Mock private GeminiClient geminiClient;
    @Mock private CurriculumService curriculumService;

    private CourseAiAuthoringService service;

    @BeforeEach
    void setUp() {
        service = new CourseAiAuthoringService(
                ownershipGuard,
                outcomeRepository,
                bulletRepository,
                quizQuestionRepository,
                textExtractor,
                geminiClient,
                curriculumService,
                new ObjectMapper());
    }

    @Test
    void suggestOutcomesKeepsOnlyCourseOutcomesAndClampsConfidence() {
        Lesson lesson = Lesson.builder().id(5L).title("Spring Data JPA").build();
        List<LearningOutcome> outcomes = List.of(
                outcome(10L, "Thiết kế persistence layer"),
                outcome(11L, "Xây dựng REST API"));
        OutcomeSuggestionResponse aiResponse = new OutcomeSuggestionResponse(List.of(
                new OutcomeSuggestion(10L, 1.4, "Phù hợp trực tiếp"),
                new OutcomeSuggestion(999L, 0.9, "ID không tồn tại"),
                new OutcomeSuggestion(10L, 0.2, "Bản trùng")), null);

        when(ownershipGuard.requireLessonInCourse(5L, 1L)).thenReturn(lesson);
        when(outcomeRepository.findByCourse_IdOrderByPositionAsc(1L)).thenReturn(outcomes);
        when(textExtractor.extract(lesson)).thenReturn("Nội dung về repository và entity mapping");
        when(geminiClient.generateStructured(
                anyString(), org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                eq(OutcomeSuggestionResponse.class))).thenReturn(aiResponse);

        OutcomeSuggestionResponse result = service.suggestOutcomes(1L, 5L, 7L);

        assertThat(result.suggestions()).hasSize(1);
        assertThat(result.suggestions().getFirst().outcomeId()).isEqualTo(10L);
        assertThat(result.suggestions().getFirst().confidence()).isEqualTo(1.0);
        assertThat(result.warning()).isNull();
    }

    @Test
    void generateCurriculumDraftSanitizesOutcomeIdsAndMissingContentType() {
        Course course = Course.builder().id(1L).title("Spring Boot").language("vi").build();
        List<LearningOutcome> outcomes = List.of(
                outcome(10L, "Thiết kế persistence layer"),
                outcome(11L, "Xây dựng REST API"));
        CurriculumDraftResponse aiResponse = new CurriculumDraftResponse(List.of(
                new CurriculumSectionDraft("  Nền tảng  ", "  Kiến thức cốt lõi  ", List.of(
                        new CurriculumLessonDraft("  JPA căn bản  ", null, List.of(10L, 999L, 10L))))));

        when(ownershipGuard.requireEditableCourse(1L, 7L)).thenReturn(course);
        when(outcomeRepository.findByCourse_IdOrderByPositionAsc(1L)).thenReturn(outcomes);
        when(bulletRepository.findByCourse_IdOrderByBulletTypeAscPositionAsc(1L)).thenReturn(List.of());
        when(geminiClient.generateStructured(
                anyString(), org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                eq(CurriculumDraftResponse.class))).thenReturn(aiResponse);

        CurriculumDraftResponse result = service.generateCurriculumDraft(
                1L, new GenerateCurriculumDraftRequest(4, 3, null), 7L);

        assertThat(result.sections()).hasSize(1);
        assertThat(result.sections().getFirst().title()).isEqualTo("Nền tảng");
        CurriculumLessonDraft lesson = result.sections().getFirst().lessons().getFirst();
        assertThat(lesson.title()).isEqualTo("JPA căn bản");
        assertThat(lesson.recommendedContentType()).isEqualTo(LessonContentType.ARTICLE);
        assertThat(lesson.outcomeIds()).containsExactly(10L);
    }

    @Test
    void generateCurriculumDraftRemovesAssessmentItems() {
        Course course = Course.builder().id(1L).title("Spring Boot").language("vi").build();
        List<LearningOutcome> outcomes = List.of(
                outcome(10L, "Thiết kế persistence layer"),
                outcome(11L, "Xây dựng REST API"));
        CurriculumDraftResponse aiResponse = new CurriculumDraftResponse(List.of(
                new CurriculumSectionDraft("Nền tảng", null, List.of(
                        new CurriculumLessonDraft("JPA căn bản", LessonContentType.ARTICLE, List.of(10L)),
                        new CurriculumLessonDraft("Bài tập cuối chương", LessonContentType.FILE, List.of(10L)),
                        new CurriculumLessonDraft("Câu hỏi ôn tập", LessonContentType.ARTICLE, List.of(10L)))),
                new CurriculumSectionDraft("Quiz tổng kết", null, List.of(
                        new CurriculumLessonDraft("Câu hỏi trắc nghiệm", LessonContentType.ARTICLE, List.of(11L))))));

        when(ownershipGuard.requireEditableCourse(1L, 7L)).thenReturn(course);
        when(outcomeRepository.findByCourse_IdOrderByPositionAsc(1L)).thenReturn(outcomes);
        when(bulletRepository.findByCourse_IdOrderByBulletTypeAscPositionAsc(1L)).thenReturn(List.of());
        when(geminiClient.generateStructured(
                anyString(), org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                eq(CurriculumDraftResponse.class))).thenReturn(aiResponse);

        CurriculumDraftResponse result = service.generateCurriculumDraft(
                1L, new GenerateCurriculumDraftRequest(4, 4, null), 7L);

        assertThat(result.sections()).hasSize(1);
        assertThat(result.sections().getFirst().title()).isEqualTo("Nền tảng");
        assertThat(result.sections().getFirst().lessons())
                .extracting(CurriculumLessonDraft::title)
                .containsExactly("JPA căn bản");
    }

    @Test
    void generateCurriculumDraftUsesQuestionTextAndQuizMetadataWithoutAnswerData() {
        Course course = Course.builder().id(1L).title("Spring Boot").language("vi").build();
        LearningOutcome persistence = outcome(10L, "Thiết kế persistence layer");
        LearningOutcome api = outcome(11L, "Xây dựng REST API");
        Assessment assessment = Assessment.builder()
                .id(101L)
                .course(course)
                .title("Quiz REST API")
                .instructions("Chọn câu trả lời phù hợp nhất")
                .outcomes(new LinkedHashSet<>(List.of(persistence, api)))
                .build();
        Quiz quiz = Quiz.builder().id(201L).assessment(assessment).build();
        QuizQuestion question = QuizQuestion.builder()
                .id(301L)
                .quiz(quiz)
                .outcome(api)
                .questionText("Annotation nào khai báo REST controller?")
                .optionsJson("SECRET_CORRECT_ANSWER_AND_EXPLANATION")
                .position(0)
                .build();

        when(ownershipGuard.requireEditableCourse(1L, 7L)).thenReturn(course);
        when(outcomeRepository.findByCourse_IdOrderByPositionAsc(1L))
                .thenReturn(List.of(persistence, api));
        when(bulletRepository.findByCourse_IdOrderByBulletTypeAscPositionAsc(1L)).thenReturn(List.of());
        when(quizQuestionRepository.findCurriculumReferencesByCourseId(1L)).thenReturn(List.of(question));
        when(geminiClient.generateStructured(
                anyString(), org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                eq(CurriculumDraftResponse.class))).thenReturn(validCurriculumDraft());

        service.generateCurriculumDraft(1L, new GenerateCurriculumDraftRequest(4, 3, null), 7L);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generateStructured(
                promptCaptor.capture(), org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                eq(CurriculumDraftResponse.class));
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains(
                "Quiz REST API",
                "Chọn câu trả lời phù hợp nhất",
                "Annotation nào khai báo REST controller?",
                "\"outcomeIds\":[10,11]",
                "\"outcomeId\":11");
        assertThat(prompt).doesNotContain("SECRET_CORRECT_ANSWER_AND_EXPLANATION");
    }

    @Test
    void generateCurriculumDraftSharesQuestionBudgetAcrossQuizzes() throws Exception {
        Course course = Course.builder().id(1L).title("Spring Boot").language("vi").build();
        List<LearningOutcome> outcomes = List.of(
                outcome(10L, "Thiết kế persistence layer"),
                outcome(11L, "Xây dựng REST API"));
        List<QuizQuestion> questions = new java.util.ArrayList<>();
        for (int quizIndex = 0; quizIndex < 3; quizIndex++) {
            Assessment assessment = Assessment.builder()
                    .id(100L + quizIndex)
                    .course(course)
                    .title("Quiz " + quizIndex)
                    .outcomes(new LinkedHashSet<>(outcomes))
                    .build();
            Quiz quiz = Quiz.builder().id(200L + quizIndex).assessment(assessment).build();
            for (int questionIndex = 0; questionIndex < 10; questionIndex++) {
                questions.add(QuizQuestion.builder()
                        .id((long) (quizIndex * 10 + questionIndex))
                        .quiz(quiz)
                        .questionText("Q" + quizIndex + "-" + questionIndex + "-" + "x".repeat(1_200))
                        .optionsJson("[]")
                        .position(questionIndex)
                        .build());
            }
        }

        when(ownershipGuard.requireEditableCourse(1L, 7L)).thenReturn(course);
        when(outcomeRepository.findByCourse_IdOrderByPositionAsc(1L)).thenReturn(outcomes);
        when(bulletRepository.findByCourse_IdOrderByBulletTypeAscPositionAsc(1L)).thenReturn(List.of());
        when(quizQuestionRepository.findCurriculumReferencesByCourseId(1L)).thenReturn(questions);
        when(geminiClient.generateStructured(
                anyString(), org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                eq(CurriculumDraftResponse.class))).thenReturn(validCurriculumDraft());

        service.generateCurriculumDraft(1L, new GenerateCurriculumDraftRequest(4, 3, null), 7L);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generateStructured(
                promptCaptor.capture(), org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                eq(CurriculumDraftResponse.class));
        String marker = "Course metadata: ";
        String prompt = promptCaptor.getValue();
        var courseContext = new ObjectMapper().readTree(
                prompt.substring(prompt.indexOf(marker) + marker.length()).trim());
        var quizReferences = courseContext.get("quizReferences");
        assertThat(quizReferences).hasSize(3);

        int questionCharacters = 0;
        for (var quizReference : quizReferences) {
            assertThat(quizReference.get("questions")).isNotEmpty();
            for (var question : quizReference.get("questions")) {
                questionCharacters += question.get("questionText").asText().length();
            }
        }
        assertThat(questionCharacters).isLessThanOrEqualTo(12_000);
    }

    @Test
    void generateCurriculumDraftKeepsOriginalContextWhenThereAreNoQuizQuestions() {
        Course course = Course.builder().id(1L).title("Spring Boot").language("vi").build();
        List<LearningOutcome> outcomes = List.of(
                outcome(10L, "Thiết kế persistence layer"),
                outcome(11L, "Xây dựng REST API"));
        when(ownershipGuard.requireEditableCourse(1L, 7L)).thenReturn(course);
        when(outcomeRepository.findByCourse_IdOrderByPositionAsc(1L)).thenReturn(outcomes);
        when(bulletRepository.findByCourse_IdOrderByBulletTypeAscPositionAsc(1L)).thenReturn(List.of());
        when(quizQuestionRepository.findCurriculumReferencesByCourseId(1L)).thenReturn(List.of());
        when(geminiClient.generateStructured(
                anyString(), org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                eq(CurriculumDraftResponse.class))).thenReturn(validCurriculumDraft());

        service.generateCurriculumDraft(1L, new GenerateCurriculumDraftRequest(4, 3, null), 7L);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generateStructured(
                promptCaptor.capture(), org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                eq(CurriculumDraftResponse.class));
        assertThat(promptCaptor.getValue()).doesNotContain("\"quizReferences\"");
    }

    @Test
    void applyCurriculumDraftKeepsAiSelectedContentType() {
        List<LearningOutcome> outcomes = List.of(
                outcome(10L, "Thiết kế persistence layer"),
                outcome(11L, "Xây dựng REST API"));
        CurriculumResponse expected = CurriculumResponse.builder().courseId(1L).sections(List.of()).build();
        ApplyCurriculumDraftRequest request = new ApplyCurriculumDraftRequest(List.of(
                new CurriculumSectionDraft("Nền tảng", null, List.of(
                        new CurriculumLessonDraft("Video JPA", LessonContentType.VIDEO, List.of(10L))))));

        when(outcomeRepository.findByCourse_IdOrderByPositionAsc(1L)).thenReturn(outcomes);
        when(curriculumService.addSection(eq(1L), any(), eq(7L)))
                .thenReturn(SectionResponse.builder().id(20L).build());
        when(curriculumService.getCurriculum(1L, 7L)).thenReturn(expected);

        CurriculumResponse result = service.applyCurriculumDraft(1L, request, 7L);

        ArgumentCaptor<CreateLessonRequest> lessonCaptor = ArgumentCaptor.forClass(CreateLessonRequest.class);
        verify(curriculumService).addLesson(eq(1L), eq(20L), lessonCaptor.capture(), eq(7L));
        assertThat(lessonCaptor.getValue().getContentType()).isEqualTo(LessonContentType.VIDEO);
        assertThat(result).isSameAs(expected);
    }

    private LearningOutcome outcome(Long id, String statement) {
        return LearningOutcome.builder().id(id).statement(statement).position(id.intValue()).build();
    }

    private CurriculumDraftResponse validCurriculumDraft() {
        return new CurriculumDraftResponse(List.of(
                new CurriculumSectionDraft("Nền tảng", null, List.of(
                        new CurriculumLessonDraft("REST căn bản", LessonContentType.ARTICLE, List.of(11L))))));
    }
}
