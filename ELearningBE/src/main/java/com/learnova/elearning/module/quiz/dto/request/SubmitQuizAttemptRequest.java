package com.learnova.elearning.module.quiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitQuizAttemptRequest {

    @NotEmpty(message = "Danh sách câu trả lời không được để trống")
    private List<@Valid QuestionAnswerItem> answers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionAnswerItem {
        @NotNull(message = "ID câu hỏi không được để trống")
        private Long questionId;

        /**
         * Danh sách các mã option mà học viên đã chọn (ví dụ: ["opt_1", "opt_3"]).
         */
        @NotEmpty(message = "Phương án chọn không được để trống")
        private List<String> selectedOptionIds;
    }
}
