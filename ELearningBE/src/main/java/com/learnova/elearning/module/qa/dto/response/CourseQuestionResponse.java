package com.learnova.elearning.module.qa.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseQuestionResponse {
    private Long id;
    private Long courseId;
    private Long lessonId;
    private String lessonTitle;
    private String title;
    private String content;
    private QuestionAuthorResponse author;
    private int answersCount;
    private boolean hasInstructorReply;
    @Builder.Default
    private List<CourseAnswerResponse> answers = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}
