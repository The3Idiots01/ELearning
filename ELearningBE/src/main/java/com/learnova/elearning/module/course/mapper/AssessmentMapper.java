package com.learnova.elearning.module.course.mapper;

import com.learnova.elearning.module.course.dto.response.AssessmentResponse;
import com.learnova.elearning.module.course.entity.Assessment;
import com.learnova.elearning.module.course.entity.LearningOutcome;

import java.util.Comparator;
import java.util.Set;

public final class AssessmentMapper {
    private AssessmentMapper() {}

    public static AssessmentResponse toResponse(Assessment assessment) {
        return toResponse(assessment, null);
    }

    public static AssessmentResponse toResponse(Assessment assessment, Boolean completed) {
        return AssessmentResponse.builder()
                .id(assessment.getId())
                .type(assessment.getType())
                .publicationStatus(assessment.getPublicationStatus())
                .title(assessment.getTitle())
                .instructions(assessment.getInstructions())
                .sectionId(assessment.getSection() != null ? assessment.getSection().getId() : null)
                .position(assessment.getPosition())
                .outcomeIds((assessment.getOutcomes() == null ? Set.<LearningOutcome>of() : assessment.getOutcomes()).stream()
                        .sorted(Comparator.comparing(LearningOutcome::getPosition)
                                .thenComparing(LearningOutcome::getId))
                        .map(LearningOutcome::getId)
                        .toList())
                .completed(completed)
                .build();
    }
}
