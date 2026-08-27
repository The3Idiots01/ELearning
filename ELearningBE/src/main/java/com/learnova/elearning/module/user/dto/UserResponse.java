package com.learnova.elearning.module.user.dto;

import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.entity.enums.AuthProvider;
import com.learnova.elearning.module.user.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private UserRole role;
    private AuthProvider authProvider;
    private String avatarUrl;
    private String avatarKey;
    private String bio;
    private String interests;
    private String expertise;
    private Boolean isActive;
    private Boolean isProfileCompleted;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserResponse fromEntity(User user) {
        return fromEntity(user, user != null ? user.getAvatarUrl() : null);
    }

    public static UserResponse fromEntity(User user, String resolvedAvatarUrl) {
        if (user == null) {
            return null;
        }

        boolean isCompleted = user.getFullName() != null && !user.getFullName().trim().isEmpty()
                && ((user.getBio() != null && !user.getBio().trim().isEmpty())
                || (user.getExpertise() != null && !user.getExpertise().trim().isEmpty())
                || (user.getInterests() != null && !user.getInterests().trim().isEmpty())
                || (user.getAvatarUrl() != null && !user.getAvatarUrl().trim().isEmpty()));

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .avatarUrl(resolvedAvatarUrl != null ? resolvedAvatarUrl : user.getAvatarUrl())
                .avatarKey(user.getAvatarUrl())
                .bio(user.getBio())
                .interests(user.getInterests())
                .expertise(user.getExpertise())
                .isActive(user.getIsActive())
                .isProfileCompleted(isCompleted)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
