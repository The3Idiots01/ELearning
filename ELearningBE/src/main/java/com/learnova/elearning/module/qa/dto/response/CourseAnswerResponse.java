package com.learnova.elearning.module.qa.dto.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAnswerResponse {
    private Long id;
    private Long questionId;
    private String content;
    private Boolean isInstructorReply;
    private QuestionAuthorResponse author;
    private Instant createdAt;
    private Instant updatedAt;
}
