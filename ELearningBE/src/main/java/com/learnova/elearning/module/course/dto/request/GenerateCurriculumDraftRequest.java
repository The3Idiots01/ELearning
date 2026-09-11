package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record GenerateCurriculumDraftRequest(
        @Min(1) @Max(12) Integer maxSections,
        @Min(1) @Max(10) Integer lessonsPerSection,
        @Size(max = 1000) String guidance
) {
}
