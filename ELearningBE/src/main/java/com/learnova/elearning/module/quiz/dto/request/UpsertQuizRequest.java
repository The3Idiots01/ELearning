package com.learnova.elearning.module.quiz.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpsertQuizRequest {

    @NotNull(message = "Điểm đạt không được để trống")
    @DecimalMin(value = "0.00", message = "Điểm đạt tối thiểu là 0%")
    @DecimalMax(value = "100.00", message = "Điểm đạt tối đa là 100%")
    private BigDecimal passingScore;

    @Min(value = 1, message = "Số lần làm tối đa phải ít nhất là 1")
    private Integer maxAttempts;
}
