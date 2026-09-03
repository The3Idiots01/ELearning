package com.learnova.elearning.module.quiz.dto.response;

import com.learnova.elearning.module.quiz.entity.enums.QuestionType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Câu hỏi trong đề thi dành cho Học viên.
 * ĐẢM BẢO: Tuyệt đối không chứa thuộc tính `isCorrect` để tuân thủ BR-16.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionTakingResponse {
    private Long id;
    private String questionText;
    private QuestionType questionType;
    private BigDecimal points;
    private Integer position;
    private List<StudentOptionItem> options;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentOptionItem {
        private String id;
        private String text;
    }
}
