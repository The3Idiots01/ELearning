package com.learnova.elearning.module.quiz.dto.request;

import com.learnova.elearning.module.quiz.dto.QuizOptionDto;
import com.learnova.elearning.module.quiz.entity.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpsertQuestionRequest {

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String questionText;

    @NotNull(message = "Loại câu hỏi không được để trống")
    private QuestionType questionType;

    @NotNull(message = "Điểm câu hỏi không được để trống")
    @DecimalMin(value = "0.01", message = "Điểm câu hỏi phải lớn hơn 0")
    private BigDecimal points;

    @NotNull(message = "Danh sách đáp án không được để trống")
    @Size(min = 2, message = "Câu hỏi phải có ít nhất 2 đáp án lựa chọn")
    private List<@Valid QuizOptionDto> options;
}
