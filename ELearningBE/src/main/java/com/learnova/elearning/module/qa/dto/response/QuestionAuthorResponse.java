package com.learnova.elearning.module.qa.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionAuthorResponse {
    private Long id;
    private String fullName;
    private String avatarUrl;
    private String role;
}
