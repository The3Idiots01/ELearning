package com.learnova.elearning.module.delivery.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.delivery.config.DeliveryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

/**
 * Ràng buộc người xem qua playback cookie, và giới hạn phiên phát đồng thời —
 * §4.5 / §4.9 design_us15_us17.md. Toàn bộ trạng thái nằm trong Redis, TTL
 * ngắn — mất Redis không được kéo sập luồng xem (R5): mọi lỗi Redis ở đây bị
 * nuốt, ghi log ERROR, và suy giảm về hướng "cho qua" thay vì chặn.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlaybackSessionStore {

    private static final String SESSION_KEY_PREFIX = "pb:sess:";
    private static final String CONCURRENCY_KEY_PREFIX = "pb:concurrent:";
    private static final java.time.Duration CONCURRENCY_WINDOW = java.time.Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeliveryProperties deliveryProperties;

    /**
     * Phát hành một playback session mới cho {@code userId}, trả về sessionId
     * để controller gắn vào cookie {@code lv_pb}. Trả {@code null} nếu Redis
     * không sẵn sàng — caller khi đó bỏ qua việc set cookie, ticket vẫn hoạt
     * động (chỉ giảm khả năng chặn T3 trong lúc Redis phục hồi).
     */
    public String issueSession(long userId, String userAgent) {
        String sessionId = newSessionId();
        String key = SESSION_KEY_PREFIX + sessionId;
        PlaybackSessionData data = new PlaybackSessionData(userId, Instant.now().toEpochMilli(), hashUserAgent(userAgent));
        try {
            redisTemplate.opsForValue().set(key, data, deliveryProperties.getPlaybackSessionTtl());
            return sessionId;
        } catch (DataAccessException e) {
            log.error("Redis unavailable while issuing playback session for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Đối chiếu cookie với ticket: {@code sessionId} phải tồn tại và thuộc về
     * đúng {@code userId} (§4.5). TTL được gia hạn khi khớp ("TTL 8 giờ trượt").
     */
    public boolean matches(String sessionId, long userId) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        String key = SESSION_KEY_PREFIX + sessionId;
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (!(raw instanceof PlaybackSessionData data)) {
                return false;
            }
            boolean matches = data.userId() != null && data.userId() == userId;
            if (matches) {
                redisTemplate.expire(key, deliveryProperties.getPlaybackSessionTtl());
            }
            return matches;
        } catch (DataAccessException e) {
            log.error("Redis unavailable while verifying playback session {}: {} — degrading to ticket-only check (R5)",
                    sessionId, e.getMessage());
            return true;
        }
    }

    /**
     * Giới hạn số phiên phát đồng thời mỗi user (§4.9, giảm nhẹ T5). Ném
     * {@link ErrorCode#TOO_MANY_ACTIVE_STREAMS} nếu vượt ngưỡng cấu hình. Lỗi
     * Redis không chặn cấp ticket — chỉ bỏ qua kiểm tra và ghi log ERROR.
     */
    public void assertConcurrencyAllowed(long userId, String jti) {
        String key = CONCURRENCY_KEY_PREFIX + userId;
        long nowMillis = Instant.now().toEpochMilli();
        double cutoff = nowMillis - CONCURRENCY_WINDOW.toMillis();
        try {
            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            zSetOps.removeRangeByScore(key, Double.NEGATIVE_INFINITY, cutoff);
            zSetOps.add(key, jti, nowMillis);
            redisTemplate.expire(key, CONCURRENCY_WINDOW);

            Set<Object> active = zSetOps.range(key, 0, -1);
            long activeCount = active != null ? active.size() : 0;
            if (activeCount > deliveryProperties.getMaxConcurrentStreams()) {
                throw new AppException(ErrorCode.TOO_MANY_ACTIVE_STREAMS);
            }
        } catch (DataAccessException e) {
            log.error("Redis unavailable while checking concurrent streams for user {}: {}", userId, e.getMessage());
        }
    }

    private String newSessionId() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(userAgent.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
