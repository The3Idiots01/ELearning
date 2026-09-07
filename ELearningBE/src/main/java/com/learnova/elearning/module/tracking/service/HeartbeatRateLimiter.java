package com.learnova.elearning.module.tracking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Chống gian lận heartbeat qua Redis — §5.6 design_us15_us17.md. Toàn bộ
 * trạng thái là TTL ngắn, không phải nguồn sự thật (đó là Postgres); mất Redis
 * không được chặn ghi tiến độ (R5) — mọi lỗi Redis ở đây bị nuốt, ghi log
 * ERROR, và suy giảm về hướng "cho qua" (không giới hạn, không chặn).
 */
@Component
@Slf4j
public class HeartbeatRateLimiter {

    private static final String HEARTBEAT_KEY_PREFIX = "kt:hb:";
    private static final String VIOLATION_KEY_PREFIX = "kt:violation:";
    private static final String BLOCKED_KEY_PREFIX = "kt:blocked:";

    /** Cửa sổ Δt và khoá đếm vi phạm — hết hạn thì coi như phiên mới (§5.6). */
    private static final Duration WINDOW = Duration.ofMinutes(5);
    /** Cho phép xem nhanh nhất 2x tốc độ thật — vượt ngưỡng này mới bị coi là đáng ngờ. */
    private static final double MAX_RATE = 2.0;
    /** Biên nới cho jitter mạng / heartbeat đến hơi trễ. */
    private static final int GRACE_SECONDS = 10;
    /** Δt mặc định cho heartbeat đầu tiên của phiên — phiên mới không được nhảy vọt. */
    private static final long DEFAULT_DELTA_SECONDS = 10;
    private static final long MAX_VIOLATIONS_BEFORE_BLOCK = 3;

    private final RedisTemplate<String, Object> redisTemplate;

    public HeartbeatRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Ngưỡng số giây coverage mới được chấp nhận ở heartbeat này, tính từ Δt kể
     * từ heartbeat trước của đúng (user, lesson). Có tác dụng phụ: cập nhật mốc
     * thời gian trong Redis cho lần gọi kế tiếp.
     */
    public int resolveBudgetSeconds(long userId, long lessonId) {
        String key = HEARTBEAT_KEY_PREFIX + userId + ":" + lessonId;
        long nowMillis = Instant.now().toEpochMilli();
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            long deltaSeconds = raw instanceof Number number
                    ? Math.max(0, (nowMillis - number.longValue()) / 1000)
                    : DEFAULT_DELTA_SECONDS;
            redisTemplate.opsForValue().set(key, nowMillis, WINDOW);
            return (int) Math.round(deltaSeconds * MAX_RATE) + GRACE_SECONDS;
        } catch (DataAccessException e) {
            log.error("Redis unavailable while resolving heartbeat budget for user {} lesson {}: {} — cho qua (R5)",
                    userId, lessonId, e.getMessage());
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Lesson đang bị chặn tạm thời (đã vi phạm {@code MAX_VIOLATIONS_BEFORE_BLOCK}
     * lần trong cửa sổ hiện tại) hay chưa.
     */
    public boolean isBlocked(long userId, long lessonId) {
        String key = BLOCKED_KEY_PREFIX + userId + ":" + lessonId;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (DataAccessException e) {
            log.error("Redis unavailable while checking heartbeat block for user {} lesson {}: {} — cho qua (R5)",
                    userId, lessonId, e.getMessage());
            return false;
        }
    }

    /**
     * Ghi nhận một lần vi phạm ngân sách của {@code userId} (không riêng theo
     * lesson — người gian lận thường thử nhiều lesson). Đạt ngưỡng thì khoá
     * heartbeat của đúng lesson đang vi phạm trong {@link #WINDOW} và báo hiệu
     * caller nên trả {@code 429}.
     *
     * @return {@code true} nếu vừa đạt ngưỡng chặn ở lần gọi này
     */
    public boolean recordViolationAndCheckBlock(long userId, long lessonId) {
        String violationKey = VIOLATION_KEY_PREFIX + userId;
        try {
            Long count = redisTemplate.opsForValue().increment(violationKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(violationKey, WINDOW);
            }
            log.warn("Heartbeat vượt ngân sách coverage cho user {} lesson {} — lần vi phạm thứ {}",
                    userId, lessonId, count);
            if (count != null && count >= MAX_VIOLATIONS_BEFORE_BLOCK) {
                redisTemplate.opsForValue().set(BLOCKED_KEY_PREFIX + userId + ":" + lessonId, 1, WINDOW);
                return true;
            }
            return false;
        } catch (DataAccessException e) {
            log.error("Redis unavailable while recording heartbeat violation for user {}: {} — cho qua (R5)",
                    userId, e.getMessage());
            return false;
        }
    }
}
