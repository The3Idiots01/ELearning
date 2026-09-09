package com.learnova.elearning.module.course.dto.response;

import com.learnova.elearning.module.course.entity.enums.AssessmentType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AssessmentResponse {
    private Long id;
    private AssessmentType type;
    private String title;
    private String instructions;
    private Long sectionId;
    private Integer position;
    private List<Long> outcomeIds;
    private Boolean completed;
}
