package com.learnova.elearning.module.enrollment.repository;

import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.enums.AssessmentType;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.repository.AssessmentRepository;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AssessmentProgressRepositoryIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private AssessmentProgressRepository progressRepository;

    @Test
    void quizCompletionUpsertIsIdempotent() {
        String suffix = UUID.randomUUID().toString();
        User lecturer = userRepository.save(User.builder()
                .fullName("Lecturer").email("lecturer-" + suffix + "@example.test").build());
        User learner = userRepository.save(User.builder()
                .fullName("Learner").email("learner-" + suffix + "@example.test").build());
        Course course = courseRepository.save(Course.builder()
                .lecturer(lecturer).title("Course").slug("course-" + suffix)
                .status(CourseStatus.DRAFT).build());
        Assessment assessment = assessmentRepository.save(Assessment.builder()
                .course(course).type(AssessmentType.QUIZ).title("Quiz").position(0).build());
        Enrollment enrollment = enrollmentRepository.saveAndFlush(Enrollment.builder()
                .student(learner).course(course).build());
        assessmentRepository.flush();

        int firstInsert = progressRepository.markCompletedByQuiz(enrollment.getId(), assessment.getId());
        int secondInsert = progressRepository.markCompletedByQuiz(enrollment.getId(), assessment.getId());

        assertThat(firstInsert).isEqualTo(1);
        assertThat(secondInsert).isZero();
        assertThat(progressRepository.countByEnrollment_IdAndAssessment_Id(
                enrollment.getId(), assessment.getId())).isEqualTo(1);
    }
}
