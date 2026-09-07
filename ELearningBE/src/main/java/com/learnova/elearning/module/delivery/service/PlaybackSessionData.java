package com.learnova.elearning.module.delivery.service;

/**
 * Giá trị lưu trong Redis tại {@code pb:sess:{sessionId}} — §4.5 design_us15_us17.md.
 * {@code userAgentHash} chỉ phục vụ audit/điều tra chia sẻ tài khoản, không dùng để
 * đối chiếu khi verify (đối chiếu chỉ dựa trên {@code userId}, xem {@code PlaybackSessionStore}).
 */
public record PlaybackSessionData(Long userId, long issuedAtEpochMilli, String userAgentHash) {
}
