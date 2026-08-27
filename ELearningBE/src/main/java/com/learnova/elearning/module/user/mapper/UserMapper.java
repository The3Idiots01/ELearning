package com.learnova.elearning.module.user.mapper;

import com.learnova.elearning.integration.storage.StorageProperties;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.user.dto.UserResponse;
import com.learnova.elearning.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserMapper {

    private final StorageService storageService;
    private final StorageProperties storageProperties;

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        String resolvedAvatarUrl = resolveAvatarUrl(user.getAvatarUrl());
        return UserResponse.fromEntity(user, resolvedAvatarUrl);
    }

    public String resolveAvatarUrl(String rawAvatar) {
        if (rawAvatar == null || rawAvatar.isBlank()) {
            return null;
        }

        // Nếu là URL tuyệt đối từ bên ngoài (Google OAuth, Gravatar, v.v.)
        if (rawAvatar.startsWith("http://") || rawAvatar.startsWith("https://") || rawAvatar.startsWith("data:")) {
            return rawAvatar;
        }

        // Nếu là Storage Key nội bộ (users/.../avatar/...) -> sinh presigned download URL
        try {
            return storageService.presignDownload(rawAvatar, storageProperties.getDownloadTtl());
        } catch (Exception e) {
            log.warn("Failed to generate presigned download URL for avatar key: {}", rawAvatar, e);
            return rawAvatar;
        }
    }
}
