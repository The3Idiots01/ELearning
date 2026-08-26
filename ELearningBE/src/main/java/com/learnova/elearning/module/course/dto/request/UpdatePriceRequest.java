package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Đặt giá bán. Khoảng hợp lệ 0–10.000.000 VND (BR-04) kiểm ở service để trả đúng
 * mã lỗi COURSE_PRICE_OUT_OF_RANGE; giá 0 = khóa học miễn phí.
 */
@Data
public class UpdatePriceRequest {

    @NotNull(message = "price is required")
    private BigDecimal price;

    private Long version;
}
