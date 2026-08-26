package com.learnova.elearning.module.course.dto.response;

import com.learnova.elearning.integration.storage.model.PresignedUpload;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class PresignUploadResponse {

    private String uploadUrl;
    private String storageKey;
    private String httpMethod;
    private long expiresInSeconds;
    private Map<String, String> requiredHeaders;

    public static PresignUploadResponse from(PresignedUpload presigned) {
        return PresignUploadResponse.builder()
                .uploadUrl(presigned.uploadUrl())
                .storageKey(presigned.storageKey())
                .httpMethod(presigned.httpMethod())
                .expiresInSeconds(presigned.expiresInSeconds())
                .requiredHeaders(presigned.requiredHeaders())
                .build();
    }
}
