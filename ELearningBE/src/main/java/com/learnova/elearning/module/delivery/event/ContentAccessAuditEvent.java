package com.learnova.elearning.module.delivery.event;

/**
 * Một lần đối chiếu quyền truy cập nội dung tại gateway {@code /content/stream}
 * — ghi lại để điều tra chia sẻ link (§4.x, ghi chú "Audit truy cập nội dung"
 * trong V12__content_delivery_and_tracking.sql). {@code userId} là
 * {@code null} cho khách vãng lai xem preview.
 */
public record ContentAccessAuditEvent(
        Long userId,
        Long lessonId,
        String ticketJti,
        String scope,
        ContentAccessOutcome outcome,
        String clientIp,
        String userAgent
) {
}
