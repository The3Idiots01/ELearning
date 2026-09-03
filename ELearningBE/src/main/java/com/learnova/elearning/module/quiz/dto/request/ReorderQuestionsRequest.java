package com.learnova.elearning.module.quiz.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderQuestionsRequest {

    @NotEmpty(message = "Danh sách ID câu hỏi không được để trống")
    private List<Long> questionIds;
}
