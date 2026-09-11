package com.learnova.elearning.module.delivery.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.delivery.config.DeliveryProperties;
import com.learnova.elearning.module.delivery.dto.PlaybackIssueResult;
import com.learnova.elearning.module.delivery.dto.PlaybackResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Cấp PlaybackTicket và đối chiếu lại quyền tại thời điểm phát byte. §4.3 /
 * §4.6 design_us15_us17.md. Quyết định quyền truy cập được ủy quyền hoàn toàn
 * cho {@link ContentAccessGuard} — service này chỉ điều phối: resolve lesson,
 * gọi guard, mã hóa ticket, và (task 3) ràng buộc phiên xem qua
 * {@link PlaybackSessionStore}.
 */
@Service
@RequiredArgsConstructor
public class PlaybackService {

    private final LessonRepository lessonRepository;
    private final ContentAccessGuard accessGuard;
    private final PlaybackSessionStore sessionStore;
    private final PlaybackTicketCodec ticketCodec;
    private final DeliveryProperties deliveryProperties;

    @Transactional(readOnly = true)
    public PlaybackIssueResult issuePlayback(Long courseId, Long lessonId, Long userId, String userAgent) {
        Lesson lesson = lessonRepository.findByIdAndSection_Course_Id(lessonId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        if (lesson.getUploadStatus() != LessonUploadStatus.READY) {
            throw new AppException(ErrorCode.LESSON_CONTENT_NOT_READY);
        }

        AccessDecision decision = accessGuard.decide(lesson, userId);

        long ticketUserId = decision.scope() == AccessScope.PREVIEW && userId == null ? 0L : userId;
        long exp = Instant.now().plus(deliveryProperties.getTicketTtl()).getEpochSecond();
        PlaybackTicketIssued issued = ticketCodec.encodeWithJti(ticketUserId, lesson.getId(), decision.scope(), exp);

        String sessionId = null;
        if (ticketUserId != 0L) {
            // Khách vãng lai (userId=0) không có phiên để ràng buộc — preview vốn công khai (§4.5).
            sessionStore.assertConcurrencyAllowed(ticketUserId, issued.jti());
            sessionId = sessionStore.issueSession(ticketUserId, userAgent);
        }

        PlaybackResponse response = PlaybackResponse.builder()
                .lessonId(lesson.getId())
                .contentType(lesson.getContentType())
                .streamUrl("/api/v1/content/stream?t=" + issued.ticket())
                .expiresAt(Instant.ofEpochSecond(exp))
                .ttlSeconds(deliveryProperties.getTicketTtl().toSeconds())
                .durationSeconds(lesson.getDurationSeconds())
                .mimeType(lesson.getMimeType())
                .build();

        return new PlaybackIssueResult(response, sessionId);
    }


    /**
     * Đối chiếu lại quyền tại thời điểm phát byte (gateway) — dùng
     * {@code lessonId}/{@code userId} từ ticket đã giải mã, không tin lại
     * {@code claims.scope()} vì quyền có thể đã bị thu hồi từ lúc cấp ticket
     * (thu hồi tức thì — §4.2 ADR-01). Với ticket có cookie ràng buộc (mọi
     * ticket không phải khách vãng lai), {@code sessionCookie} phải khớp
     * đúng người đã được cấp ticket (§4.5) — đây là chốt chặn T3.
     */
    @Transactional(readOnly = true)
    public Lesson verifyStillAllowed(PlaybackTicketClaims claims, String sessionCookie) {
        Lesson lesson = lessonRepository.findById(claims.lessonId())
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        if (!claims.isGuest() && !sessionStore.matches(sessionCookie, claims.userId())) {
            throw new AppException(ErrorCode.PLAYBACK_TICKET_MISMATCH);
        }

        Long guardUserId = claims.isGuest() ? null : claims.userId();
        accessGuard.decide(lesson, guardUserId);
        return lesson;
    }
}
