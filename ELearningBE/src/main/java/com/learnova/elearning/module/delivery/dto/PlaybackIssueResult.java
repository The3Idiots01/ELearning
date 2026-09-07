package com.learnova.elearning.module.delivery.dto;

/**
 * Kết quả cấp playback: response trả cho client, và sessionId để controller
 * gắn cookie {@code lv_pb} (§4.5). {@code playbackSessionId} là {@code null}
 * khi ticket phát cho khách vãng lai xem preview — không cần ràng buộc cookie.
 */
public record PlaybackIssueResult(PlaybackResponse response, String playbackSessionId) {
}
