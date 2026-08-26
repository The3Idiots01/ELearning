package com.learnova.elearning.module.course.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SectionResponse {

    private Long id;
    private String title;
    private String description;
    private Integer position;
    private Integer totalLessons;
    private Integer totalDurationSeconds;
    private List<LessonResponse> lessons;
}
