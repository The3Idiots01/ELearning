package com.learnova.elearning.module.delivery.strategy;

import com.learnova.elearning.module.course.entity.Lesson;
import org.springframework.http.ResponseEntity;

/**
 * Chiến lược phát byte nội dung tại gateway — §3 / §4.7 design_us15_us17.md.
 * Đúng một cài đặt được kích hoạt tùy {@code learnova.storage.provider}:
 * {@link RedirectDeliveryStrategy} (s3) hoặc {@link StreamDeliveryStrategy} (local).
 */
public interface ContentDeliveryStrategy {

    /**
     * @param lesson      lesson đã qua {@code ContentAccessGuard}, chắc chắn có {@code storageKey}
     * @param rangeHeader giá trị header {@code Range} của request, hoặc {@code null} nếu không có
     */
    ResponseEntity<?> deliver(Lesson lesson, String rangeHeader);
}
