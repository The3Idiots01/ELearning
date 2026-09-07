package com.learnova.elearning.module.tracking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Payload heartbeat — §5.4 design_us15_us17.md. {@code playedRanges} là nguyên
 * văn {@code video.played} của trình duyệt: TÍCH LUỸ, không phải delta — gửi
 * lại cùng một tập nhiều lần vô hại vì merge phía server là phép hợp (§5.5).
 */
@Data
public class HeartbeatRequest {

    @NotNull
    private Double positionSeconds;

    /** Mỗi phần tử là {@code [start, end]} giây, giống hệt {@code video.played}. */
    private List<List<Double>> playedRanges;

    private Double playbackRate;

    private String clientSessionId;

    /** Client tự khai — KHÔNG dùng để clamp ranges (server luôn dùng duration đã probe, §5.6). */
    private Integer durationSeconds;
}
