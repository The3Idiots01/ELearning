package com.learnova.elearning.module.delivery.controller;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.common.util.CookieUtil;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.delivery.config.DeliveryProperties;
import com.learnova.elearning.module.delivery.event.ContentAccessAuditEvent;
import com.learnova.elearning.module.delivery.event.ContentAccessOutcome;
import com.learnova.elearning.module.delivery.service.PlaybackService;
import com.learnova.elearning.module.delivery.service.PlaybackTicketClaims;
import com.learnova.elearning.module.delivery.service.PlaybackTicketCodec;
import com.learnova.elearning.module.delivery.service.PlaybackTicketExpiredException;
import com.learnova.elearning.module.delivery.strategy.ContentDeliveryStrategy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * §4.6 / §4.7 / §4.8 design_us15_us17.md — gateway phát nội dung. Không nhận
 * header {@code Authorization} (thẻ {@code <video>} không gửi được) — xác
 * thực hoàn toàn bằng ticket + cookie {@code lv_pb} (§4.5). Việc phát byte
 * thực sự được ủy quyền cho {@link ContentDeliveryStrategy} — đúng một cài
 * đặt được kích hoạt tùy {@code learnova.storage.provider} (§4.7). Mỗi lần
 * đối chiếu quyền được ghi vào {@code content_access_logs} bất đồng bộ (Task
 * 9) — phục vụ điều tra chia sẻ link, không nằm trên đường phát byte.
 */
@RestController
@RequestMapping("/api/v1/content/stream")
@RequiredArgsConstructor
public class ContentStreamController {

    private final PlaybackTicketCodec ticketCodec;
    private final PlaybackService playbackService;
    private final ContentDeliveryStrategy deliveryStrategy;
    private final DeliveryProperties deliveryProperties;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping
    public ResponseEntity<?> stream(
            @RequestParam("t") String ticket,
            HttpServletRequest request
    ) {
        PlaybackTicketClaims claims;
        try {
            claims = ticketCodec.decode(ticket);
        } catch (PlaybackTicketExpiredException e) {
            publishAudit(e.getClaims(), ContentAccessOutcome.TICKET_EXPIRED, request);
            throw e;
        }

        String sessionCookie = CookieUtil.getCookieValue(request, deliveryProperties.getCookieName()).orElse(null);
        Lesson lesson;
        try {
            lesson = playbackService.verifyStillAllowed(claims, sessionCookie);
        } catch (AppException e) {
            ContentAccessOutcome outcome = e.getErrorCode() == ErrorCode.PLAYBACK_TICKET_MISMATCH
                    ? ContentAccessOutcome.TICKET_MISMATCH
                    : ContentAccessOutcome.ACCESS_DENIED;
            publishAudit(claims, outcome, request);
            throw e;
        }
        publishAudit(claims, ContentAccessOutcome.GRANTED, request);

        if (lesson.getStorageKey() == null) {
            throw new AppException(ErrorCode.LESSON_CONTENT_NOT_READY);
        }

        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        return deliveryStrategy.deliver(lesson, rangeHeader);
    }

    private void publishAudit(PlaybackTicketClaims claims, ContentAccessOutcome outcome, HttpServletRequest request) {
        eventPublisher.publishEvent(new ContentAccessAuditEvent(
                claims.isGuest() ? null : claims.userId(),
                claims.lessonId(),
                claims.jti(),
                claims.scope().name(),
                outcome,
                clientIp(request),
                request.getHeader(HttpHeaders.USER_AGENT)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
