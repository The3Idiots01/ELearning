package com.learnova.elearning.module.course.dto.response;

import com.learnova.elearning.module.course.entity.enums.LessonContentType;

import java.util.List;

public record CurriculumLessonDraft(
        String title,
        LessonContentType recommendedContentType,
        List<Long> outcomeIds
) {
}
