package com.learnova.elearning.module.quiz.repository;

import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.LearningOutcome;
import com.learnova.elearning.module.course.entity.enums.AssessmentType;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.repository.AssessmentRepository;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.course.repository.LearningOutcomeRepository;
import com.learnova.elearning.module.quiz.entity.Quiz;
import com.learnova.elearning.module.quiz.entity.QuizQuestion;
import com.learnova.elearning.module.quiz.entity.enums.QuestionType;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.repository.UserRepository;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QuizQuestionRepositoryIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private LearningOutcomeRepository outcomeRepository;
    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private QuizQuestionRepository questionRepository;

    @Test
    void curriculumReferencesOnlyContainActiveAssessmentsFromRequestedCourse() {
        String suffix = UUID.randomUUID().toString();
        User lecturer = userRepository.save(User.builder()
                .fullName("Lecturer")
                .email("quiz-reference-" + suffix + "@example.test")
                .build());
        Course requestedCourse = courseRepository.save(Course.builder()
                .lecturer(lecturer)
                .title("Requested course")
                .slug("requested-course-" + suffix)
                .status(CourseStatus.DRAFT)
                .build());
        Course otherCourse = courseRepository.save(Course.builder()
                .lecturer(lecturer)
                .title("Other course")
                .slug("other-course-" + suffix)
                .status(CourseStatus.DRAFT)
                .build());

        LearningOutcome requestedOutcome = outcomeRepository.save(LearningOutcome.builder()
                .course(requestedCourse)
                .statement("Build a REST API")
                .position(0)
                .build());
        LearningOutcome secondRequestedOutcome = outcomeRepository.save(LearningOutcome.builder()
                .course(requestedCourse)
                .statement("Persist application data")
                .position(1)
                .build());
        LearningOutcome otherOutcome = outcomeRepository.save(LearningOutcome.builder()
                .course(otherCourse)
                .statement("Unrelated outcome")
                .position(0)
                .build());

        Assessment active = assessmentRepository.save(assessment(
                requestedCourse, "Active quiz", requestedOutcome, null));
        active.getOutcomes().add(secondRequestedOutcome);
        assessmentRepository.save(active);
        Assessment deleted = assessmentRepository.save(assessment(
                requestedCourse, "Deleted quiz", requestedOutcome, Instant.now()));
        Assessment other = assessmentRepository.save(assessment(
                otherCourse, "Other quiz", otherOutcome, null));

        Quiz activeQuiz = quizRepository.save(Quiz.builder().assessment(active).build());
        Quiz deletedQuiz = quizRepository.save(Quiz.builder().assessment(deleted).build());
        Quiz otherQuiz = quizRepository.save(Quiz.builder().assessment(other).build());
        questionRepository.saveAllAndFlush(List.of(
                question(activeQuiz, requestedOutcome, "Included question"),
                question(deletedQuiz, requestedOutcome, "Deleted question"),
                question(otherQuiz, otherOutcome, "Other-course question")));

        List<QuizQuestion> result = questionRepository
                .findCurriculumReferencesByCourseId(requestedCourse.getId());

        assertThat(result).extracting(QuizQuestion::getQuestionText)
                .containsExactly("Included question");
        assertThat(Hibernate.isInitialized(result.getFirst().getQuiz().getAssessment().getOutcomes()))
                .isTrue();
        assertThat(result.getFirst().getQuiz().getAssessment().getOutcomes())
                .extracting(LearningOutcome::getId)
                .containsExactlyInAnyOrder(requestedOutcome.getId(), secondRequestedOutcome.getId());
    }

    private Assessment assessment(Course course, String title,
                                  LearningOutcome outcome, Instant deletedAt) {
        return Assessment.builder()
                .course(course)
                .type(AssessmentType.QUIZ)
                .title(title)
                .position(0)
                .outcomes(new LinkedHashSet<>(List.of(outcome)))
                .deletedAt(deletedAt)
                .build();
    }

    private QuizQuestion question(Quiz quiz, LearningOutcome outcome, String text) {
        return QuizQuestion.builder()
                .quiz(quiz)
                .outcome(outcome)
                .questionText(text)
                .questionType(QuestionType.SINGLE_CHOICE)
                .optionsJson("[]")
                .points(BigDecimal.ONE)
                .position(0)
                .build();
    }
}
