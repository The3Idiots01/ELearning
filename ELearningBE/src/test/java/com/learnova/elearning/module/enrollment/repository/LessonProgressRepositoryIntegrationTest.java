package com.learnova.elearning.module.enrollment.repository;

import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.CourseSection;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.repository.CourseRepository;
import com.learnova.elearning.module.course.repository.CourseSectionRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.enrollment.entity.Enrollment;
import com.learnova.elearning.module.enrollment.entity.LessonProgress;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.entity.enums.UserRole;
import com.learnova.elearning.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Xác nhận câu native SQL merge nguyên tử (§5.5 design_us15_us17.md) chạy đúng
 * trên PostgreSQL thật — {@code int4multirange}/{@code unnest(multirange)}
 * không tồn tại trên H2 nên không thể giả lập bằng in-memory DB (§11.2). Test
 * này vẫn chạy trực tiếp trên DB dev (Neon) đã cấu hình qua {@code .env}
 * thay vì container riêng, và tự rollback nhờ
 * {@code @Transactional} nên không để lại dữ liệu thật.
 */
@SpringBootTest
@Transactional
class LessonProgressRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseSectionRepository sectionRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private LessonProgressRepository progressRepository;

    private Long enrollmentId;
    private Long lessonId;

    @BeforeEach
    void setUp() {
        User lecturer = userRepository.save(User.builder()
                .fullName("Lecturer Test")
                .email("lecturer-" + System.nanoTime() + "@test.local")
                .role(UserRole.USER)
                .build());
        User student = userRepository.save(User.builder()
                .fullName("Student Test")
                .email("student-" + System.nanoTime() + "@test.local")
                .role(UserRole.USER)
                .build());

        Course course = courseRepository.save(Course.builder()
                .lecturer(lecturer)
                .title("Course for LessonProgress IT")
                .slug("course-lp-it-" + System.nanoTime())
                .status(CourseStatus.PUBLISHED)
                .build());
        CourseSection section = sectionRepository.save(CourseSection.builder()
                .course(course)
                .title("Section 1")
                .build());
        Lesson lesson = lessonRepository.save(Lesson.builder()
                .section(section)
                .title("Lesson 1")
                .contentType(LessonContentType.VIDEO)
                .durationSeconds(612)
                .build());
        Enrollment enrollment = enrollmentRepository.save(Enrollment.builder()
                .student(student)
                .course(course)
                .build());

        lessonId = lesson.getId();
        enrollmentId = enrollment.getId();
    }

    @Test
    void mergeWatchedRanges_firstHeartbeat_insertsRowWithCorrectAggregates() {
        WatchedRangesMergeResult result = progressRepository.mergeWatchedRanges(
                enrollmentId, lessonId, "{[0,205),[280,431)}", 187);

        assertThat(result.getWatchedSeconds()).isEqualTo(205 + 151); // 356
        assertThat(result.getMaxPositionSeconds()).isEqualTo(187);

        LessonProgress saved = progressRepository.findById(result.getId()).orElseThrow();
        assertThat(saved.getLastPositionSeconds()).isEqualTo(187);
        assertThat(saved.getEnrollment().getId()).isEqualTo(enrollmentId);
        assertThat(saved.getLesson().getId()).isEqualTo(lessonId);
    }

    @Test
    void mergeWatchedRanges_overlappingRangesAcrossHeartbeats_unionsWithoutDoubleCounting() {
        // T-KT-03: xem 0->50, tua về 0, xem lại 0->50 — watchedSeconds phải là 50, không phải 100.
        progressRepository.mergeWatchedRanges(enrollmentId, lessonId, "{[0,50)}", 50);
        WatchedRangesMergeResult result = progressRepository.mergeWatchedRanges(
                enrollmentId, lessonId, "{[0,50)}", 50);

        assertThat(result.getWatchedSeconds()).isEqualTo(50);
    }

    @Test
    void mergeWatchedRanges_disjointRangesAcrossHeartbeats_sumsBothIntervals() {
        // T-KT-04 style (single-threaded): A xem [0,50), B xem [200,250)
        progressRepository.mergeWatchedRanges(enrollmentId, lessonId, "{[0,50)}", 50);
        WatchedRangesMergeResult result = progressRepository.mergeWatchedRanges(
                enrollmentId, lessonId, "{[200,250)}", 250);

        assertThat(result.getWatchedSeconds()).isEqualTo(100);
    }

    @Test
    void mergeWatchedRanges_positionMovesBackward_maxPositionStaysMonotonic() {
        progressRepository.mergeWatchedRanges(enrollmentId, lessonId, "{[0,300)}", 300);
        WatchedRangesMergeResult result = progressRepository.mergeWatchedRanges(
                enrollmentId, lessonId, "{[0,10)}", 10);

        assertThat(result.getMaxPositionSeconds()).isEqualTo(300); // GREATEST — không tụt
        LessonProgress saved = progressRepository.findById(result.getId()).orElseThrow();
        assertThat(saved.getLastPositionSeconds()).isEqualTo(10); // last-write-wins
    }

    @Test
    void materializeCoverage_persistsCoveragePercentRoundedToTwoDecimals() {
        WatchedRangesMergeResult result = progressRepository.mergeWatchedRanges(
                enrollmentId, lessonId, "{[0,205),[280,431)}", 430);

        progressRepository.materializeCoverage(result.getId(), result.getWatchedSeconds(), 612);

        LessonProgress saved = progressRepository.findById(result.getId()).orElseThrow();
        assertThat(saved.getWatchedSeconds()).isEqualTo(356);
        assertThat(saved.getCoveragePercent()).isEqualByComparingTo(new BigDecimal("58.17"));
    }

    @Test
    void materializeCoverage_watchedExceedsDuration_clampedToOneHundred() {
        WatchedRangesMergeResult result = progressRepository.mergeWatchedRanges(
                enrollmentId, lessonId, "{[0,612)}", 612);

        progressRepository.materializeCoverage(result.getId(), result.getWatchedSeconds(), 300); // duration đổi nhỏ hơn

        LessonProgress saved = progressRepository.findById(result.getId()).orElseThrow();
        assertThat(saved.getCoveragePercent()).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
