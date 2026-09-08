package com.learnova.elearning.module.course.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LearningOutcomeResponse {
    private Long id;
    private String statement;
    private Integer position;
}
