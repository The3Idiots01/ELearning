package com.learnova.elearning.module.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorReviewResponse {

    private Long id;
    private Long courseId;
    private Long userId;
    private String studentName;
    private String studentEmail;
    private String studentAvatarUrl;
    private Integer rating;
    private String comment;
    private Double progressPercent;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isEdited;
}
