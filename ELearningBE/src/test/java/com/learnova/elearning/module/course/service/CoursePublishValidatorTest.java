package com.learnova.elearning.module.course.service;

import com.learnova.elearning.module.category.entity.Category;
import com.learnova.elearning.module.course.config.CoursePublishProperties;
import com.learnova.elearning.module.course.dto.response.PublishIssue;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.BulletType;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import com.learnova.elearning.module.course.repository.CourseBulletRepository;
import com.learnova.elearning.module.course.repository.CourseSectionRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

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
    @DisplayName("Validate: thiếu objectives -> OBJECTIVES_NOT_ENOUGH")
    void validate_notEnoughObjectives() {
        Course course = validCourse();
        stubAllConditionsMet(course);
        when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.LEARNING_OBJECTIVE)).thenReturn(2L);

        assertThat(validator.validate(course))
                .extracting(PublishIssue::code).contains("OBJECTIVES_NOT_ENOUGH");
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
    @DisplayName("Validate: lesson QUIZ (module chưa có) -> QUIZ_INCOMPLETE")
    void validate_quizIncomplete() {
        Course course = validCourse();
        lenient().when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.LEARNING_OBJECTIVE)).thenReturn(4L);
        lenient().when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.REQUIREMENT)).thenReturn(1L);
        lenient().when(bulletRepository.countByCourse_IdAndBulletType(1L, BulletType.TARGET_AUDIENCE)).thenReturn(1L);
        when(sectionRepository.countByCourse_Id(1L)).thenReturn(1L);
        when(lessonRepository.findBySection_Course_Id(1L)).thenReturn(List.of(
                Lesson.builder().id(9L).title("Kiểm tra").contentType(LessonContentType.QUIZ)
                        .uploadStatus(LessonUploadStatus.EMPTY).build()));

        assertThat(validator.validate(course))
                .extracting(PublishIssue::code).contains("QUIZ_INCOMPLETE");
    }
}
