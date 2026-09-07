package com.learnova.elearning.module.delivery.strategy;

import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.delivery.config.DeliveryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * provider=s3 — chuyển hướng tới presigned URL của storage, ký lại ở mỗi
 * request (ADR-01 phương án C, §4.2). Byte video đi thẳng từ storage/CDN,
 * không qua app. TTL dùng {@code learnova.delivery.storage-url-ttl} (≤ 15
 * phút — BR-25), không dùng {@code storage.download-ttl} vốn là TTL chung
 * cho mọi tác vụ tải file khác.
 */
@Component
@ConditionalOnProperty(name = "learnova.storage.provider", havingValue = "s3")
@RequiredArgsConstructor
public class RedirectDeliveryStrategy implements ContentDeliveryStrategy {

    private final StorageService storageService;
    private final DeliveryProperties deliveryProperties;

    @Override
    public ResponseEntity<?> deliver(Lesson lesson, String rangeHeader) {
        String presignedUrl = storageService.presignDownload(lesson.getStorageKey(), deliveryProperties.getStorageUrlTtl());

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(presignedUrl))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .build();
    }
}
