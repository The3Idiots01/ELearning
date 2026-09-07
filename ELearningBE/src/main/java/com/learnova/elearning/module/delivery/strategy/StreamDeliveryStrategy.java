package com.learnova.elearning.module.delivery.strategy;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.course.entity.Lesson;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * provider=local — phát byte trực tiếp từ app, hỗ trợ HTTP Range qua
 * {@link ResourceRegion} (§4.7 design_us15_us17.md, khắc phục khoảng trống
 * G3: {@code ResponseEntity<Resource>} không tự sinh {@code 206}).
 */
@Component
@ConditionalOnProperty(name = "learnova.storage.provider", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class StreamDeliveryStrategy implements ContentDeliveryStrategy {

    private static final long CHUNK_SIZE = 1024 * 1024; // 1 MiB — chỉ hỗ trợ 1 range, đủ cho <video>

    private final StorageService storageService;

    @Override
    public ResponseEntity<?> deliver(Lesson lesson, String rangeHeader) {
        Resource resource = storageService.openReadable(lesson.getStorageKey());
        MediaType mediaType = resolveMediaType(lesson.getMimeType());
        long length = contentLengthOf(resource);

        List<HttpRange> ranges = parseRanges(rangeHeader);
        if (ranges.isEmpty()) {
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .cacheControl(CacheControl.noStore().cachePrivate())
                    .header("X-Content-Type-Options", "nosniff")
                    .contentLength(length)
                    .body(resource);
        }

        try {
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(length);
            long end = range.getRangeEnd(length);
            long count = Math.min(end - start + 1, CHUNK_SIZE);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(mediaType)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .cacheControl(CacheControl.noStore().cachePrivate())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(new ResourceRegion(resource, start, count));
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.CONTENT_RANGE_NOT_SATISFIABLE, e);
        }
    }

    private List<HttpRange> parseRanges(String rangeHeader) {
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return List.of();
        }
        try {
            return HttpRange.parseRanges(rangeHeader);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.CONTENT_RANGE_NOT_SATISFIABLE, e);
        }
    }

    private long contentLengthOf(Resource resource) {
        try {
            return resource.contentLength();
        } catch (IOException e) {
            throw new AppException(ErrorCode.STORAGE_UNAVAILABLE, e);
        }
    }

    private MediaType resolveMediaType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (IllegalArgumentException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
