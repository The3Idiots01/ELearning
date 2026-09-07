package com.learnova.elearning.module.delivery.service;

/**
 * Nội dung giải mã của một PlaybackTicket — §4.4 design_us15_us17.md.
 *
 * @param userId  chủ sở hữu ticket, 0 = khách vãng lai xem preview
 * @param lessonId lesson mà ticket được cấp — ràng buộc ticket vào đúng một lesson
 * @param scope   cơ sở cấp quyền lúc phát hành, phục vụ audit
 * @param exp     epoch seconds hết hạn
 * @param jti     định danh ticket (audit / thu hồi lẻ)
 */
public record PlaybackTicketClaims(long userId, long lessonId, AccessScope scope, long exp, String jti) {

    public boolean isExpired() {
        return exp < java.time.Instant.now().getEpochSecond();
    }

    public boolean isGuest() {
        return userId == 0L;
    }
}
