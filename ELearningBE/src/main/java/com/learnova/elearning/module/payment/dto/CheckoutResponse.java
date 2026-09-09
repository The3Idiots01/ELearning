package com.learnova.elearning.module.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class CheckoutResponse {

    private Long orderCode;
    private Long courseId;
    private String checkoutUrl;
    private BigDecimal amount;
    private String status; // PENDING, PAID

    @JsonProperty("isFree")
    private boolean isFree;

    @JsonProperty("isEnrolled")
    private boolean isEnrolled;

    @JsonProperty("free")
    public boolean isFreeAlias() {
        return this.isFree;
    }

    @JsonProperty("enrolled")
    public boolean isEnrolledAlias() {
        return this.isEnrolled;
    }
}
