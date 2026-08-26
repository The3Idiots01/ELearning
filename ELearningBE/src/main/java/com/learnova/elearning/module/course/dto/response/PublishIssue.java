package com.learnova.elearning.module.course.dto.response;

import lombok.Builder;

/** Một điều kiện publish chưa đạt. */
@Builder
public record PublishIssue(
        String code,
        String field,
        String message
) {}
