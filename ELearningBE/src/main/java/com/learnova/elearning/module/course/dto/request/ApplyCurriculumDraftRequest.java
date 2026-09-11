package com.learnova.elearning.module.course.dto.request;

import com.learnova.elearning.module.course.dto.response.CurriculumSectionDraft;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ApplyCurriculumDraftRequest(
        @NotEmpty List<CurriculumSectionDraft> sections
) {
}
