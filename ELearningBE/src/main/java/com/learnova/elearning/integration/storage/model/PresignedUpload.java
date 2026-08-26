package com.learnova.elearning.integration.storage.model;

import lombok.Builder;

import java.util.Map;

/**
 * Kết quả ký upload: URL để FE PUT thẳng file lên storage, storageKey đã sinh,
 * và các header bắt buộc phải gửi kèm khi PUT (S3 có thể yêu cầu Content-Type).
 */
@Builder
public record PresignedUpload(
        String uploadUrl,
        String storageKey,
        String httpMethod,
        long expiresInSeconds,
        Map<String, String> requiredHeaders
) {}
