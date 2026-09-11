package com.learnova.elearning.module.course.dto.response;

import java.util.List;

public record CurriculumSectionDraft(
        String title,
        String description,
        List<CurriculumLessonDraft> lessons
) {
}
