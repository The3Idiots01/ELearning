package com.learnova.elearning.module.delivery.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.delivery.config.DeliveryProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Mã hóa/giải mã PlaybackTicket — chuỗi opaque, tự chứa (stateless), không cần
 * tra DB để giải mã. §4.4 design_us15_us17.md.
 *
 * <pre>
 * canonical = "1" | userId | lessonId | scope | exp | jti
 * ticket    = base64url(canonical) . base64url(HMAC-SHA256(secret, canonical))
 * </pre>
 */
@Component
public class PlaybackTicketCodec {

    private static final String VERSION = "1";
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String DELIMITER = "\\|";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] secret;

    public PlaybackTicketCodec(DeliveryProperties properties) {
        this.secret = properties.getTicketSecret().getBytes(StandardCharsets.UTF_8);
    }

    public String encode(long userId, long lessonId, AccessScope scope, long expEpochSeconds) {
        return encodeWithJti(userId, lessonId, scope, expEpochSeconds).ticket();
    }

    /**
     * Như {@link #encode}, nhưng trả kèm {@code jti} — cần cho giới hạn phiên
     * đồng thời (§4.9), vốn theo dõi từng ticket bằng jti thay vì đọc lại ticket.
     */
    public PlaybackTicketIssued encodeWithJti(long userId, long lessonId, AccessScope scope, long expEpochSeconds) {
        String jti = newJti();
        String canonical = canonical(userId, lessonId, scope, expEpochSeconds, jti);
        String signature = sign(canonical);
        String ticket = base64UrlEncode(canonical) + "." + signature;
        return new PlaybackTicketIssued(ticket, jti);
    }

    public PlaybackTicketClaims decode(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new AppException(ErrorCode.PLAYBACK_TICKET_INVALID);
        }
        int dot = ticket.lastIndexOf('.');
        if (dot < 0) {
            throw new AppException(ErrorCode.PLAYBACK_TICKET_INVALID);
        }
        String encodedCanonical = ticket.substring(0, dot);
        String signature = ticket.substring(dot + 1);

        String canonical;
        try {
            canonical = new String(Base64.getUrlDecoder().decode(encodedCanonical), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.PLAYBACK_TICKET_INVALID);
        }

        if (!constantTimeEquals(sign(canonical), signature)) {
            throw new AppException(ErrorCode.PLAYBACK_TICKET_INVALID);
        }

        PlaybackTicketClaims claims = parse(canonical);
        if (claims.isExpired()) {
            // Subclass mang theo claims: caller (ContentStreamController) cần lessonId/jti
            // để ghi content_access_logs outcome=TICKET_EXPIRED trước khi trả lỗi.
            throw new PlaybackTicketExpiredException(claims);
        }
        return claims;
    }

    private String canonical(long userId, long lessonId, AccessScope scope, long expEpochSeconds, String jti) {
        return VERSION + "|" + userId + "|" + lessonId + "|" + scope.name() + "|" + expEpochSeconds + "|" + jti;
    }

    private PlaybackTicketClaims parse(String canonical) {
        String[] parts = canonical.split(DELIMITER, -1);
        if (parts.length != 6 || !VERSION.equals(parts[0])) {
            throw new AppException(ErrorCode.PLAYBACK_TICKET_INVALID);
        }
        try {
            long userId = Long.parseLong(parts[1]);
            long lessonId = Long.parseLong(parts[2]);
            AccessScope scope = AccessScope.valueOf(parts[3]);
            long exp = Long.parseLong(parts[4]);
            String jti = parts[5];
            return new PlaybackTicketClaims(userId, lessonId, scope, exp, jti);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.PLAYBACK_TICKET_INVALID);
        }
    }

    private String sign(String canonical) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret, HMAC_ALGO));
            byte[] raw = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign playback ticket", e);
        }
    }

    private String newJti() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String base64UrlEncode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
