package com.learnova.elearning.module.course.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record PublishCheckResponse(
        boolean canPublish,
        List<PublishIssue> issues
) {}
