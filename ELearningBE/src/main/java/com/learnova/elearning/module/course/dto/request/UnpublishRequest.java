package com.learnova.elearning.module.course.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UnpublishRequest {

    /** Lý do gỡ khỏi catalog (tùy chọn) — lưu vào course_status_logs. */
    @Size(max = 1000, message = "reason must not exceed 1000 characters")
    private String reason;
}
