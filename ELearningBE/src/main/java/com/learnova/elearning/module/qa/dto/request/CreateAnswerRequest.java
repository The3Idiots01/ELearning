package com.learnova.elearning.module.qa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAnswerRequest {

    @NotBlank(message = "Nội dung câu trả lời không được để trống")
    @Size(min = 2, max = 5000, message = "Nội dung phản hồi phải từ 2 đến 5000 ký tự")
    private String content;
}
