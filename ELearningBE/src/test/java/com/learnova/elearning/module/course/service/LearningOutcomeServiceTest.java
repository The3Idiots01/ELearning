package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.dto.request.CreateLearningOutcomeRequest;
import com.learnova.elearning.module.course.dto.request.UpdateLearningOutcomeRequest;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.LearningOutcome;
import com.learnova.elearning.module.course.repository.LearningOutcomeRepository;
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
class LearningOutcomeServiceTest {

    @Mock private CourseOwnershipGuard ownershipGuard;
    @Mock private LearningOutcomeRepository outcomeRepository;
    @InjectMocks private LearningOutcomeService service;

    @Test
    void updateKeepsStableId() {
        LearningOutcome outcome = LearningOutcome.builder().id(21L).statement("Old").position(0).build();
        UpdateLearningOutcomeRequest request = new UpdateLearningOutcomeRequest();
        request.setStatement("  Build a REST API  ");
        when(ownershipGuard.requireOutcomeInCourse(21L, 1L)).thenReturn(outcome);
        when(outcomeRepository.save(outcome)).thenReturn(outcome);

        var response = service.update(1L, 21L, request, 9L);

        assertThat(response.getId()).isEqualTo(21L);
        assertThat(response.getStatement()).isEqualTo("Build a REST API");
        verify(ownershipGuard).requireEditableCourse(1L, 9L);
    }

    @Test
    void createAppendsAtEnd() {
        Course course = Course.builder().id(1L).build();
        CreateLearningOutcomeRequest request = new CreateLearningOutcomeRequest();
        request.setStatement("  Explain JPA  ");
        when(ownershipGuard.requireEditableCourse(1L, 9L)).thenReturn(course);
        when(outcomeRepository.countByCourse_Id(1L)).thenReturn(2L);
        when(outcomeRepository.save(any(LearningOutcome.class))).thenAnswer(invocation -> {
            LearningOutcome saved = invocation.getArgument(0);
            saved.setId(23L);
            return saved;
        });

        var response = service.create(1L, request, 9L);

        assertThat(response.getId()).isEqualTo(23L);
        assertThat(response.getPosition()).isEqualTo(2);
        assertThat(response.getStatement()).isEqualTo("Explain JPA");
    }

    @Test
    void deleteRejectsReferencedOutcome() {
        LearningOutcome outcome = LearningOutcome.builder().id(21L).position(0).build();
        when(ownershipGuard.requireOutcomeInCourse(21L, 1L)).thenReturn(outcome);
        when(outcomeRepository.countActiveAssessmentReferences(21L)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L, 21L, 9L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_OUTCOME_IN_USE);
        verify(outcomeRepository, never()).save(any());
    }

    @Test
    void reorderRequiresExactIdSetAndUpdatesPositions() {
        LearningOutcome first = LearningOutcome.builder().id(21L).position(0).build();
        LearningOutcome second = LearningOutcome.builder().id(22L).position(1).build();
        when(outcomeRepository.findByCourse_IdOrderByPositionAsc(1L)).thenReturn(List.of(first, second));

        var response = service.reorder(1L, List.of(22L, 21L), 9L);

        assertThat(response).extracting(item -> item.getId()).containsExactly(22L, 21L);
        assertThat(second.getPosition()).isZero();
        assertThat(first.getPosition()).isEqualTo(1);
        verify(outcomeRepository).saveAll(any());
    }

    @Test
    void reorderRejectsMissingId() {
        LearningOutcome first = LearningOutcome.builder().id(21L).position(0).build();
        LearningOutcome second = LearningOutcome.builder().id(22L).position(1).build();
        when(outcomeRepository.findByCourse_IdOrderByPositionAsc(1L)).thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.reorder(1L, List.of(21L), 9L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_PAYLOAD_MISMATCH);
    }
}
