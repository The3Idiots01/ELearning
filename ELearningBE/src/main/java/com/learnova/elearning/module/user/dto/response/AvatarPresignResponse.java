package com.learnova.elearning.module.user.dto.response;

import com.learnova.elearning.integration.storage.model.PresignedUpload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarPresignResponse {

    private String uploadUrl;
    private String storageKey;
    private String httpMethod;
    private long expiresInSeconds;
    private Map<String, String> requiredHeaders;

    public static AvatarPresignResponse from(PresignedUpload presigned) {
        return AvatarPresignResponse.builder()
                .uploadUrl(presigned.uploadUrl())
                .storageKey(presigned.storageKey())
                .httpMethod(presigned.httpMethod())
                .expiresInSeconds(presigned.expiresInSeconds())
                .requiredHeaders(presigned.requiredHeaders())
                .build();
    }
}
