package com.learnova.elearning.module.qa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuestionRequest {

    @NotBlank(message = "Tiêu đề câu hỏi không được để trống")
    @Size(min = 5, max = 255, message = "Tiêu đề câu hỏi phải từ 5 đến 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    @Size(min = 10, max = 5000, message = "Nội dung câu hỏi phải từ 10 đến 5000 ký tự")
    private String content;
}
