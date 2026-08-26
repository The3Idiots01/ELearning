package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Ghi đè toàn bộ 3 khối mô tả landing page (kiểu Udemy). FE gửi nguyên cả 3 mảng
 * theo đúng thứ tự hiển thị; mảng rỗng = xóa hết khối đó. Nội dung từng dòng được
 * làm sạch và kiểm tra ở service (trim, bỏ dòng rỗng, tối đa 20 dòng, ≤ 500 ký tự).
 */
@Data
public class UpdateBulletsRequest {

    @NotNull(message = "learningObjectives is required (empty array allowed)")
    private List<String> learningObjectives;

    @NotNull(message = "requirements is required (empty array allowed)")
    private List<String> requirements;

    @NotNull(message = "targetAudiences is required (empty array allowed)")
    private List<String> targetAudiences;

    private Long version;
}
