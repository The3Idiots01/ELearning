package com.learnova.elearning.module.quiz.dto.response;

import com.learnova.elearning.module.quiz.dto.QuizOptionDto;
import com.learnova.elearning.module.quiz.entity.enums.QuestionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Chi tiết câu hỏi dành cho Giảng viên (hiển thị đầy đủ đáp án đúng).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDetailResponse {
    private Long id;
    private Long quizId;
    private String questionText;
    private QuestionType questionType;
    private BigDecimal points;
    private Integer position;
    private List<QuizOptionDto> options;
    private Instant createdAt;
    private Instant updatedAt;
}
