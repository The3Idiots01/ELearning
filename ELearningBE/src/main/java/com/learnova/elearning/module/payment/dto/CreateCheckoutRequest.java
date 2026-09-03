package com.learnova.elearning.module.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCheckoutRequest {

    @NotNull(message = "Course ID is required")
    private Long courseId;
}
