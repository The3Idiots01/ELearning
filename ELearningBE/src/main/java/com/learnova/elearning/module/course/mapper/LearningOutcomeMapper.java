package com.learnova.elearning.module.course.mapper;

import com.learnova.elearning.module.course.dto.response.LearningOutcomeResponse;
import com.learnova.elearning.module.course.entity.LearningOutcome;

public final class LearningOutcomeMapper {
    private LearningOutcomeMapper() {}

    public static LearningOutcomeResponse toResponse(LearningOutcome outcome) {
        return LearningOutcomeResponse.builder()
                .id(outcome.getId())
                .statement(outcome.getStatement())
                .position(outcome.getPosition())
                .build();
    }
}
