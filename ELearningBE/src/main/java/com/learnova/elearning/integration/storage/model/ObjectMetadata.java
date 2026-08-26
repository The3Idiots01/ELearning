package com.learnova.elearning.integration.storage.model;

/**
 * Metadata thực tế của object trên storage (đọc bằng HEAD) — dùng ở bước confirm
 * upload để đối chiếu với khai báo của client (BR-12, chống khai gian size/type).
 */
public record ObjectMetadata(
        String key,
        long sizeBytes,
        String contentType
) {}
