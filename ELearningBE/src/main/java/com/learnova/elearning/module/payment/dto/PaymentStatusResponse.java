package com.learnova.elearning.module.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusResponse {

    private Long orderCode;
    private Long courseId;
    private String courseTitle;
    private BigDecimal amount;
    private String status; // PENDING, PAID, CANCELLED, FAILED
    private boolean enrolled;
}
