package com.learnova.elearning.module.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Cấu trúc phương án trả lời trong options_json.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuizOptionDto {

    /** Định danh phương án (ví dụ: "opt_1", "opt_2" hoặc uuid) */
    private String id;

    /** Nội dung phương án */
    @NotBlank(message = "Nội dung đáp án không được để trống")
    private String text;

    /**
     * Cờ đánh dấu đáp án đúng.
     * Khi trả về cho học viên làm bài, trường này BẮT BUỘC để null để không bị serialize ra JSON (BR-16).
     */
    private Boolean isCorrect;

    /** Giải thích vì sao đúng/sai (tùy chọn) */
    private String explanation;
}
