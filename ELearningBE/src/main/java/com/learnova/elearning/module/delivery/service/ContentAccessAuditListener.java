package com.learnova.elearning.module.delivery.service;

import com.learnova.elearning.module.delivery.event.ContentAccessAuditEvent;
import com.learnova.elearning.module.delivery.repository.ContentAccessLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Ghi {@code content_access_logs} bất đồng bộ — §4.x / ghi chú "Ghi log bất
 * đồng bộ" trong V12__content_delivery_and_tracking.sql. Gateway nằm trên
 * đường phát video; lỗi ghi log KHÔNG BAO GIỜ được làm hỏng phiên xem, nên mọi
 * exception ở đây bị nuốt và chỉ ghi {@code WARN}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContentAccessAuditListener {

    private final ContentAccessLogRepository auditRepository;

    @Async
    @EventListener
    public void onContentAccess(ContentAccessAuditEvent event) {
        try {
            auditRepository.insert(
                    event.userId(),
                    event.lessonId(),
                    event.ticketJti(),
                    event.scope(),
                    event.outcome().name(),
                    event.clientIp(),
                    event.userAgent());
        } catch (Exception e) {
            log.warn("Không ghi được content_access_logs cho lesson {} outcome {}: {}",
                    event.lessonId(), event.outcome(), e.getMessage());
        }
    }
}
