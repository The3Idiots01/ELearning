package com.learnova.elearning.module.course.service;

import com.learnova.elearning.module.category.entity.Category;
import com.learnova.elearning.module.course.config.CoursePublishProperties;
import com.learnova.elearning.module.course.dto.response.PublishIssue;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.CourseSection;
import com.learnova.elearning.module.course.entity.LearningOutcome;
import com.learnova.elearning.module.course.entity.enums.AssessmentType;
import com.learnova.elearning.module.course.entity.enums.BulletType;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import com.learnova.elearning.module.course.repository.CourseBulletRepository;
import com.learnova.elearning.module.course.repository.CourseSectionRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.course.repository.AssessmentRepository;
import com.learnova.elearning.module.course.repository.LearningOutcomeRepository;
import com.learnova.elearning.module.quiz.entity.Quiz;
import com.learnova.elearning.module.quiz.entity.QuizQuestion;
import com.learnova.elearning.module.quiz.repository.QuizQuestionRepository;
import com.learnova.elearning.module.quiz.repository.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoursePublishValidatorTest {

    @Mock
    private CourseBulletRepository bulletRepository;
    @Mock
    private CourseSectionRepository sectionRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LearningOutcomeRepository outcomeRepository;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    private CoursePublishValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CoursePublishValidator(
                bulletRepository, sectionRepository, lessonRepository, new CoursePublishProperties());
    }

    /** Dựng một course đã đủ mọi điều kiện; từng test sẽ phá 1 điều kiện. */
    private Course validCourse() {
        return Course.builder()
                .id(1L)
                .title("Khóa học Spring Boot")
                .description("a".repeat(200))
                .category(Category.builder().id(1L).name("Web").build())
                .thumbnailKey("courses/1/thumbnail/x.png")
                .price(new BigDecimal("499000"))
                .build();
    }

    private void stubAllConditionsMet(Course course) {
        lenient().when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.LEARNING_OBJECTIVE)).thenReturn(4L);
        lenient().when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.REQUIREMENT)).thenReturn(1L);
        lenient().when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.TARGET_AUDIENCE)).thenReturn(1L);
        lenient().when(sectionRepository.countByCourse_Id(1L)).thenReturn(1L);
        lenient().when(lessonRepository.findBySection_Course_Id(1L)).thenReturn(List.of(
                Lesson.builder().id(5L).title("Bài 1").contentType(LessonContentType.VIDEO)
                        .uploadStatus(LessonUploadStatus.READY).build()));
    }

    @Test
    @DisplayName("Validate: đủ điều kiện -> không có issue")
    void validate_allGood() {
        Course course = validCourse();
        stubAllConditionsMet(course);

        assertThat(validator.validate(course)).isEmpty();
    }

    @Test
    @DisplayName("Validate: mô tả quá ngắn -> DESCRIPTION_TOO_SHORT")
    void validate_shortDescription() {
        Course course = validCourse();
        course.setDescription("ngắn");
        stubAllConditionsMet(course);

        assertThat(validator.validate(course))
                .extracting(PublishIssue::code).contains("DESCRIPTION_TOO_SHORT");
    }

    @Test
    @DisplayName("Validate: thiếu outcomes -> OUTCOMES_NOT_ENOUGH")
    void validate_notEnoughObjectives() {
        Course course = validCourse();
        stubAllConditionsMet(course);
        when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.LEARNING_OBJECTIVE)).thenReturn(1L);

        assertThat(validator.validate(course))
                .extracting(PublishIssue::code).contains("OUTCOMES_NOT_ENOUGH");
    }

    @Test
    @DisplayName("Validate: chưa có section -> NO_SECTION")
    void validate_noSection() {
        Course course = validCourse();
        stubAllConditionsMet(course);
        when(sectionRepository.countByCourse_Id(1L)).thenReturn(0L);

        assertThat(validator.validate(course))
                .extracting(PublishIssue::code).contains("NO_SECTION");
    }

    @Test
    @DisplayName("Validate: lesson video chưa upload xong -> LESSON_CONTENT_INCOMPLETE")
    void validate_incompleteLesson() {
        Course course = validCourse();
        lenient().when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.LEARNING_OBJECTIVE)).thenReturn(4L);
        lenient().when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.REQUIREMENT)).thenReturn(1L);
        lenient().when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.TARGET_AUDIENCE)).thenReturn(1L);
        when(sectionRepository.countByCourse_Id(1L)).thenReturn(1L);
        when(lessonRepository.findBySection_Course_Id(1L)).thenReturn(List.of(
                Lesson.builder().id(5L).title("Chưa upload").contentType(LessonContentType.VIDEO)
                        .uploadStatus(LessonUploadStatus.EMPTY).build()));

        assertThat(validator.validate(course))
                .extracting(PublishIssue::code)
                .contains("NO_LESSON_WITH_CONTENT", "LESSON_CONTENT_INCOMPLETE");
    }

    @Test
    @DisplayName("Validate: lesson plan chưa gắn content -> LESSON_CONTENT_INCOMPLETE")
    void validate_planWithoutContent() {
        Course course = validCourse();
        lenient().when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.LEARNING_OBJECTIVE)).thenReturn(4L);
        lenient().when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.REQUIREMENT)).thenReturn(1L);
        lenient().when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.TARGET_AUDIENCE)).thenReturn(1L);
        when(sectionRepository.countByCourse_Id(1L)).thenReturn(1L);
        when(lessonRepository.findBySection_Course_Id(1L)).thenReturn(List.of(
                Lesson.builder().id(9L).title("Bài học chưa gắn nội dung")
                        .uploadStatus(LessonUploadStatus.EMPTY).build()));

        assertThat(validator.validate(course))
                .extracting(PublishIssue::code).contains("LESSON_CONTENT_INCOMPLETE");
    }

    @Test
    @DisplayName("Validate: bắt đủ các lỗi alignment của outcome, lesson, assessment và quiz")
    void validate_alignmentIssues() {
        Course course = validCourse();
        validator.setOutcomeRepository(outcomeRepository);
        validator.setAlignmentRepositories(assessmentRepository, quizRepository, quizQuestionRepository);
        when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.REQUIREMENT)).thenReturn(1L);
        when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.TARGET_AUDIENCE)).thenReturn(1L);
        when(sectionRepository.countByCourse_Id(1L)).thenReturn(1L);

        LearningOutcome outcome1 = LearningOutcome.builder().id(101L).course(course).statement("Build API").position(0).build();
        LearningOutcome outcome2 = LearningOutcome.builder().id(102L).course(course).statement("Test API").position(1).build();
        LearningOutcome outcome3 = LearningOutcome.builder().id(103L).course(course).statement("Deploy API").position(2).build();
        Lesson noOutcomeLesson = Lesson.builder().id(201L).title("No mapping").contentType(LessonContentType.VIDEO).uploadStatus(LessonUploadStatus.READY).build();
        Lesson mappedLesson = Lesson.builder().id(202L).title("Mapped").contentType(LessonContentType.VIDEO).uploadStatus(LessonUploadStatus.READY).outcomes(Set.of(outcome1)).build();
        when(lessonRepository.findBySection_Course_Id(1L)).thenReturn(List.of(noOutcomeLesson, mappedLesson));

        CourseSection section = CourseSection.builder().id(301L).course(course).title("Chapter").position(0).build();
        Assessment emptyAssessment = Assessment.builder().id(401L).course(course).type(AssessmentType.QUIZ).title("Empty").build();
        Assessment mismatchAssessment = Assessment.builder().id(402L).course(course).section(section).type(AssessmentType.QUIZ).title("Mismatch").outcomes(Set.of(outcome1)).build();
        Assessment missingQuiz = Assessment.builder().id(403L).course(course).section(section).type(AssessmentType.QUIZ).title("Missing quiz").outcomes(Set.of(outcome1)).build();
        Assessment nullQuestionAssessment = Assessment.builder().id(404L).course(course).section(section).type(AssessmentType.QUIZ).title("Null question outcome").outcomes(Set.of(outcome1)).build();
        when(assessmentRepository.findByCourse_Id(1L)).thenReturn(List.of(emptyAssessment, mismatchAssessment, missingQuiz, nullQuestionAssessment));

        Quiz emptyQuiz = Quiz.builder().id(501L).assessment(emptyAssessment).passingScore(BigDecimal.valueOf(80)).maxAttempts(3).build();
        Quiz mismatchQuiz = Quiz.builder().id(502L).assessment(mismatchAssessment).passingScore(BigDecimal.valueOf(80)).maxAttempts(3).build();
        Quiz nullQuestionQuiz = Quiz.builder().id(504L).assessment(nullQuestionAssessment).passingScore(BigDecimal.valueOf(80)).maxAttempts(3).build();
        when(quizRepository.findByAssessment_IdAndAssessment_Course_Id(401L, 1L)).thenReturn(java.util.Optional.of(emptyQuiz));
        when(quizRepository.findByAssessment_IdAndAssessment_Course_Id(402L, 1L)).thenReturn(java.util.Optional.of(mismatchQuiz));
        when(quizRepository.findByAssessment_IdAndAssessment_Course_Id(403L, 1L)).thenReturn(java.util.Optional.empty());
        when(quizRepository.findByAssessment_IdAndAssessment_Course_Id(404L, 1L)).thenReturn(java.util.Optional.of(nullQuestionQuiz));
        QuizQuestion mismatchQuestion = QuizQuestion.builder().id(601L).quiz(mismatchQuiz).outcome(outcome2).questionText("Q").build();
        QuizQuestion nullOutcomeQuestion = QuizQuestion.builder().id(602L).quiz(nullQuestionQuiz).questionText("Q").build();
        when(quizQuestionRepository.findByQuiz_IdOrderByPositionAsc(501L)).thenReturn(List.of());
        when(quizQuestionRepository.findByQuiz_IdOrderByPositionAsc(502L)).thenReturn(List.of(mismatchQuestion));
        when(quizQuestionRepository.findByQuiz_IdOrderByPositionAsc(504L)).thenReturn(List.of(nullOutcomeQuestion));
        when(outcomeRepository.findByCourse_IdOrderByPositionAsc(1L)).thenReturn(List.of(outcome1, outcome2, outcome3));

        assertThat(validator.validate(course)).extracting(PublishIssue::code).contains(
                "OUTCOME_WITHOUT_LESSON", "OUTCOME_WITHOUT_ASSESSMENT",
                "LESSON_WITHOUT_OUTCOME", "ASSESSMENT_WITHOUT_OUTCOME", "ASSESSMENT_NOT_PLACED",
                "QUIZ_WITHOUT_QUESTION", "QUIZ_CONFIG_INCOMPLETE");
    }
}
