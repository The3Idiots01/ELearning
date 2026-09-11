package com.learnova.elearning.module.course.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublishChangesResponse {
    private CurriculumResponse curriculum;
    private long publishedLessons;
    private long publishedAssessments;
    private long publishedVideoReplacements;
}
