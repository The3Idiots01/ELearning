package com.learnova.elearning.module.tracking.dto;

import com.learnova.elearning.module.enrollment.entity.enums.CompletionSource;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Response của heartbeat — §5.4 design_us15_us17.md.
 * <p>
 * {@code courseProgressPercent} là {@code null} trừ khi heartbeat này vừa làm
 * lesson chuyển sang hoàn thành — rollup % course chỉ tính lại đúng lúc đó
 * (§5.7), không phải ở mọi heartbeat.
 */
@Data
@Builder
public class ProgressSnapshotResponse {

    private Long lessonId;
    private Integer lastPositionSeconds;
    private Integer maxPositionSeconds;
    /** Analytics — KHÔNG gác completion (§5.1). */
    private BigDecimal coveragePercent;
    private Integer watchedSeconds;
    /** Số giây coverage thực sự tăng thêm ở heartbeat này (0 nếu gửi lại y hệt lần trước). */
    private Integer newWatchedSeconds;
    private Boolean lessonCompleted;
    private CompletionSource completionSource;
    private BigDecimal courseProgressPercent;
    private String enrollmentStatus;
}
