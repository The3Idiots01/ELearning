package com.learnova.elearning.module.course.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CurriculumResponse {

    private Long courseId;
    private List<SectionResponse> sections;
}
