package com.learnova.elearning.module.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.integration.storage.StorageKeyFactory;
import com.learnova.elearning.integration.storage.StorageProperties;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.integration.storage.model.PresignedUpload;
import com.learnova.elearning.module.user.dto.UserResponse;
import com.learnova.elearning.module.user.dto.request.AvatarPresignRequest;
import com.learnova.elearning.module.user.dto.request.UpdateProfileRequest;
import com.learnova.elearning.module.user.dto.response.AvatarPresignResponse;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.mapper.UserMapper;
import com.learnova.elearning.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final long MAX_AVATAR_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    private final UserRepository userRepository;
    private final StorageService storageService;
    private final StorageKeyFactory keyFactory;
    private final StorageProperties storageProperties;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    /**
     * Cấp Presigned Upload URL cho Frontend tải trực tiếp ảnh đại diện lên Object Storage.
     */
    public AvatarPresignResponse presignAvatarUpload(Long userId, AvatarPresignRequest request) {
        // 1. Kiểm tra loại định dạng ảnh
        String contentType = request.getContentType() != null ? request.getContentType().toLowerCase().trim() : "";
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new AppException(ErrorCode.UPLOAD_UNSUPPORTED_MEDIA_TYPE,
                    "Chỉ hỗ trợ file ảnh định dạng PNG, JPG hoặc WebP.");
        }

        // 2. Kiểm tra kích thước ảnh
        if (request.getSizeBytes() == null || request.getSizeBytes() <= 0 || request.getSizeBytes() > MAX_AVATAR_SIZE_BYTES) {
            throw new AppException(ErrorCode.UPLOAD_FILE_TOO_LARGE,
                    "Kích thước ảnh đại diện tối đa là 5MB.");
        }

        // 3. Sinh storage key và ký URL
        String key = keyFactory.userAvatarKey(userId, request.getFileName());
        PresignedUpload presigned = storageService.presignUpload(
                key, contentType, storageProperties.getUploadTtl()
        );

        log.info("Generated presigned avatar upload URL for user [{}], key: [{}]", userId, key);
        return AvatarPresignResponse.from(presigned);
    }

    /**
     * Lấy thông tin cá nhân của người dùng (kèm avatar presigned download URL).
     */
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toResponse(user);
    }

    /**
     * Cập nhật thông tin cá nhân của người dùng.
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        // Xử lý avatar: nếu có avatarKey mới khác avatar cũ
        if (request.getAvatarKey() != null && !request.getAvatarKey().trim().isEmpty()) {
            String newKey = request.getAvatarKey().trim();
            String oldAvatar = user.getAvatarUrl();

            // Nếu avatar cũ là file nội bộ trong storage và khác key mới -> dọn dẹp file cũ
            if (oldAvatar != null && !oldAvatar.equals(newKey) && isInternalStorageKey(oldAvatar)) {
                try {
                    storageService.delete(oldAvatar);
                    log.info("Deleted old avatar object [{}] for user [{}]", oldAvatar, userId);
                } catch (Exception e) {
                    log.warn("Failed to delete old avatar object [{}]: {}", oldAvatar, e.getMessage());
                }
            }
            user.setAvatarUrl(newKey);
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio().trim().isEmpty() ? null : request.getBio().trim());
        }

        if (request.getExpertise() != null) {
            user.setExpertise(request.getExpertise().trim().isEmpty() ? null : request.getExpertise().trim());
        }

        if (request.getInterests() != null) {
            try {
                user.setInterests(objectMapper.writeValueAsString(request.getInterests()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize user interests for user [{}]: {}", userId, e.getMessage());
            }
        }

        User savedUser = userRepository.save(user);
        log.info("Updated profile for user [{}]", userId);
        return userMapper.toResponse(savedUser);
    }

    /**
     * Điền/hoàn tất thông tin hồ sơ cho người dùng mới (Onboarding Flow).
     */
    @Transactional
    public UserResponse completeProfile(Long userId, UpdateProfileRequest request) {
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new AppException(ErrorCode.FIELD_REQUIRED, "Họ và tên là bắt buộc.");
        }
        return updateProfile(userId, request);
    }

    private boolean isInternalStorageKey(String key) {
        return key != null && !key.startsWith("http://") && !key.startsWith("https://") && !key.startsWith("data:");
    }
}
