package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.CourseSection;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.repository.CourseSectionRepository;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.course.repository.LessonResourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumServiceTest {

    @Mock private CourseOwnershipGuard ownershipGuard;
    @Mock private CourseSectionRepository sectionRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonResourceRepository resourceRepository;
    @Mock private com.learnova.elearning.integration.storage.StorageService storageService;
    @Mock private LessonResponseAssembler lessonAssembler;

    @InjectMocks
    private CurriculumService curriculumService;

    private Lesson lesson(Long id, CourseSection section, int position) {
        return Lesson.builder().id(id).section(section).title("L" + id)
                .contentType(LessonContentType.VIDEO).position(position).build();
    }

    @Test
    @DisplayName("Reorder lessons: payload thiếu id -> ORDER_PAYLOAD_MISMATCH")
    void reorderLessons_mismatch() {
        CourseSection section = CourseSection.builder().id(5L).build();
        when(ownershipGuard.requireEditableCourse(1L, 10L)).thenReturn(Course.builder().id(1L).build());
        when(ownershipGuard.requireSectionInCourse(5L, 1L)).thenReturn(section);
        when(lessonRepository.findBySection_IdOrderByPositionAsc(5L)).thenReturn(List.of(
                lesson(101L, section, 0), lesson(102L, section, 1)));

        assertThatThrownBy(() -> curriculumService.reorderLessons(1L, 5L, List.of(101L), 10L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_PAYLOAD_MISMATCH);

        verify(lessonRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Reorder lessons: cập nhật position bằng UPDATE (không delete+insert)")
    void reorderLessons_updatesPositionsOnly() {
        CourseSection section = CourseSection.builder().id(5L).build();
        Lesson l1 = lesson(101L, section, 0);
        Lesson l2 = lesson(102L, section, 1);
        when(ownershipGuard.requireEditableCourse(1L, 10L)).thenReturn(Course.builder().id(1L).build());
        when(ownershipGuard.requireSectionInCourse(5L, 1L)).thenReturn(section);
        when(lessonRepository.findBySection_IdOrderByPositionAsc(5L)).thenReturn(List.of(l1, l2));
        when(sectionRepository.findByCourse_IdOrderByPositionAsc(1L)).thenReturn(List.of());

        curriculumService.reorderLessons(1L, 5L, List.of(102L, 101L), 10L);

        assertThat(l2.getPosition()).isZero();
        assertThat(l1.getPosition()).isEqualTo(1);
        verify(lessonRepository).saveAll(any());
        verify(lessonRepository, never()).deleteAll(any());
        verify(lessonRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Move lesson: chuyển sang section khác + đổi con trỏ section, dồn lại section cũ")
    void moveLesson_acrossSections() {
        CourseSection sectionA = CourseSection.builder().id(5L).build();
        CourseSection sectionB = CourseSection.builder().id(6L).build();
        Lesson moving = lesson(101L, sectionA, 0);
        Lesson bExisting = lesson(201L, sectionB, 0);

        when(ownershipGuard.requireEditableCourse(1L, 10L))
                .thenReturn(Course.builder().id(1L).status(CourseStatus.DRAFT).build());
        when(ownershipGuard.requireLessonInCourse(101L, 1L)).thenReturn(moving);
        when(ownershipGuard.requireSectionInCourse(6L, 1L)).thenReturn(sectionB);
        when(lessonRepository.findBySection_IdOrderByPositionAsc(6L)).thenReturn(List.of(bExisting));
        // normalize section cũ (5) sau khi lesson đã chuyển đi
        when(lessonRepository.findBySection_IdOrderByPositionAsc(5L)).thenReturn(List.of());
        when(sectionRepository.findByCourse_IdOrderByPositionAsc(1L)).thenReturn(List.of());

        curriculumService.moveLesson(1L, 101L, 6L, 0, 10L);

        assertThat(moving.getSection()).isSameAs(sectionB);
        assertThat(moving.getPosition()).isZero();
        assertThat(bExisting.getPosition()).isEqualTo(1);
    }
}
