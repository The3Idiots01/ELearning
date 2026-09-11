package com.learnova.elearning.module.course.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CurriculumResponse {

    private Long courseId;
    private Long resumeLessonId;
    private List<SectionResponse> sections;
    /** Instructor-only pool; public curriculum always returns an empty list. */
    @Builder.Default
    private List<AssessmentResponse> unplacedAssessments = List.of();
}
