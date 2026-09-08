package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Ghi đè requirements và target audiences. Learning outcomes có API riêng để
 * giữ ID ổn định cho alignment. Mảng rỗng = xóa hết khối đó. Nội dung từng dòng được
 * làm sạch và kiểm tra ở service (trim, bỏ dòng rỗng, tối đa 20 dòng, ≤ 500 ký tự).
 */
@Data
public class UpdateBulletsRequest {

    @NotNull(message = "requirements is required (empty array allowed)")
    private List<String> requirements;

    @NotNull(message = "targetAudiences is required (empty array allowed)")
    private List<String> targetAudiences;

    private Long version;
}
