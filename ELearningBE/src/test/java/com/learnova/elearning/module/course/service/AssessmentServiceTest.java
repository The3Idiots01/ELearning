package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.dto.request.CreateAssessmentRequest;
import com.learnova.elearning.module.course.dto.request.PlaceAssessmentRequest;
import com.learnova.elearning.module.course.dto.request.UpdateAssessmentRequest;
import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.CourseSection;
import com.learnova.elearning.module.course.entity.LearningOutcome;
import com.learnova.elearning.module.course.entity.enums.AssessmentType;
import com.learnova.elearning.module.course.repository.AssessmentRepository;
import com.learnova.elearning.module.course.repository.LearningOutcomeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock private CourseOwnershipGuard ownershipGuard;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private LearningOutcomeRepository outcomeRepository;
    @InjectMocks private AssessmentService service;

    @Test
    void createQuizBeforeAnySectionAndMapMultipleOutcomes() {
        Course course = Course.builder().id(1L).build();
        LearningOutcome first = outcome(21L, course, 0);
        LearningOutcome second = outcome(22L, course, 1);
        CreateAssessmentRequest request = new CreateAssessmentRequest();
        request.setType(AssessmentType.QUIZ);
        request.setTitle("  REST API check  ");
        request.setInstructions(" Choose the correct answer ");
        request.setOutcomeIds(List.of(21L, 22L));

        when(ownershipGuard.requireEditableCourse(1L, 9L)).thenReturn(course);
        when(outcomeRepository.findByCourse_IdAndIdIn(eq(1L), anyCollection()))
                .thenReturn(List.of(first, second));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> {
            Assessment saved = invocation.getArgument(0);
            saved.setId(31L);
            return saved;
        });

        var response = service.create(1L, request, 9L);

        assertThat(response.getId()).isEqualTo(31L);
        assertThat(response.getSectionId()).isNull();
        assertThat(response.getOutcomeIds()).containsExactly(21L, 22L);
        assertThat(response.getTitle()).isEqualTo("REST API check");
    }

    @Test
    void createRejectsOutcomeFromAnotherCourse() {
        Course course = Course.builder().id(1L).build();
        CreateAssessmentRequest request = new CreateAssessmentRequest();
        request.setType(AssessmentType.QUIZ);
        request.setTitle("Quiz");
        request.setOutcomeIds(List.of(21L, 99L));
        when(ownershipGuard.requireEditableCourse(1L, 9L)).thenReturn(course);
        when(outcomeRepository.findByCourse_IdAndIdIn(eq(1L), anyCollection()))
                .thenReturn(List.of(outcome(21L, course, 0)));

        assertThatThrownBy(() -> service.create(1L, request, 9L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ASSESSMENT_OUTCOME_MISMATCH);
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void placeInSectionInsertsAtRequestedPosition() {
        Course course = Course.builder().id(1L).build();
        CourseSection section = CourseSection.builder().id(7L).course(course).position(0).build();
        Assessment moving = assessment(31L, course, null, 0);
        Assessment existing = assessment(32L, course, section, 0);
        PlaceAssessmentRequest request = new PlaceAssessmentRequest();
        request.setSectionId(7L);
        request.setPosition(0);
        when(ownershipGuard.requireAssessmentInCourse(31L, 1L)).thenReturn(moving);
        when(ownershipGuard.requireSectionInCourse(7L, 1L)).thenReturn(section);
        when(assessmentRepository.findBySection_IdOrderByPositionAscIdAsc(7L))
                .thenReturn(List.of(existing));

        var response = service.place(1L, 31L, request, 9L);

        assertThat(response.getSectionId()).isEqualTo(7L);
        assertThat(moving.getPosition()).isZero();
        assertThat(existing.getPosition()).isEqualTo(1);
        verify(assessmentRepository).saveAll(any());
    }

    @Test
    void unassignFromSectionPreservesAssessments() {
        Course course = Course.builder().id(1L).build();
        CourseSection section = CourseSection.builder().id(7L).course(course).build();
        Assessment first = assessment(31L, course, section, 0);
        Assessment second = assessment(32L, course, section, 1);
        when(assessmentRepository.findBySection_IdOrderByPositionAscIdAsc(7L))
                .thenReturn(List.of(first, second));

        service.unassignFromSection(7L);

        assertThat(first.getSection()).isNull();
        assertThat(second.getSection()).isNull();
        verify(assessmentRepository).saveAll(List.of(first, second));
        verify(assessmentRepository, never()).deleteAll(any());
    }

    @Test
    void updateCanRemoveOutcomeUsedByLegacyQuestion() {
        Course course = Course.builder().id(1L).build();
        LearningOutcome first = outcome(21L, course, 0);
        LearningOutcome second = outcome(22L, course, 1);
        Assessment assessment = Assessment.builder().id(31L).course(course).type(AssessmentType.QUIZ)
                .title("Quiz").outcomes(new LinkedHashSet<>(List.of(first, second))).build();
        UpdateAssessmentRequest request = new UpdateAssessmentRequest();
        request.setOutcomeIds(List.of(21L));
        when(ownershipGuard.requireAssessmentInCourse(31L, 1L)).thenReturn(assessment);
        when(outcomeRepository.findByCourse_IdAndIdIn(eq(1L), anyCollection())).thenReturn(List.of(first));
        when(assessmentRepository.save(assessment)).thenReturn(assessment);
        var response = service.update(1L, 31L, request, 9L);

        assertThat(response.getOutcomeIds()).containsExactly(21L);
        verify(assessmentRepository).save(assessment);
    }

    private LearningOutcome outcome(Long id, Course course, int position) {
        return LearningOutcome.builder().id(id).course(course).statement("LO" + id).position(position).build();
    }

    private Assessment assessment(Long id, Course course, CourseSection section, int position) {
        return Assessment.builder().id(id).course(course).section(section).type(AssessmentType.QUIZ)
                .title("A" + id).position(position).outcomes(new LinkedHashSet<>()).build();
    }
}
