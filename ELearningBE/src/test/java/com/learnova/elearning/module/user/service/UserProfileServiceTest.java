package com.learnova.elearning.module.user.service;

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
import com.learnova.elearning.module.user.entity.enums.AuthProvider;
import com.learnova.elearning.module.user.entity.enums.UserRole;
import com.learnova.elearning.module.user.mapper.UserMapper;
import com.learnova.elearning.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private StorageKeyFactory keyFactory;

    private StorageProperties storageProperties;
    private UserMapper userMapper;
    private ObjectMapper objectMapper;
    private UserProfileService userProfileService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setUploadTtl(Duration.ofMinutes(15));
        storageProperties.setDownloadTtl(Duration.ofMinutes(15));

        userMapper = new UserMapper(storageService, storageProperties);
        objectMapper = new ObjectMapper();

        userProfileService = new UserProfileService(
                userRepository,
                storageService,
                keyFactory,
                storageProperties,
                userMapper,
                objectMapper
        );

        sampleUser = User.builder()
                .id(1L)
                .fullName("Nguyễn Văn A")
                .email("vana@example.com")
                .role(UserRole.LEARNER)
                .authProvider(AuthProvider.LOCAL)
                .avatarUrl("users/1/avatar/old-uuid.png")
                .bio("Lập trình viên đam mê học hỏi")
                .expertise("Fullstack Developer")
                .interests("[\"Lập trình\",\"Web\"]")
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("presignAvatarUpload Tests")
    class PresignAvatarUploadTests {

        @Test
        @DisplayName("Should generate presigned upload URL for valid image")
        void presignAvatarUpload_Success() {
            AvatarPresignRequest request = AvatarPresignRequest.builder()
                    .fileName("my_avatar.png")
                    .contentType("image/png")
                    .sizeBytes(1024L * 1024L) // 1MB
                    .build();

            String generatedKey = "users/1/avatar/new-uuid.png";
            when(keyFactory.userAvatarKey(1L, "my_avatar.png")).thenReturn(generatedKey);

            PresignedUpload presigned = new PresignedUpload(
                    "http://localhost:8080/api/v1/dev/storage/upload?sig=abc",
                    generatedKey,
                    "PUT",
                    900L,
                    Map.of("Content-Type", "image/png")
            );

            when(storageService.presignUpload(eq(generatedKey), eq("image/png"), any(Duration.class)))
                    .thenReturn(presigned);

            AvatarPresignResponse response = userProfileService.presignAvatarUpload(1L, request);

            assertThat(response).isNotNull();
            assertThat(response.getUploadUrl()).isEqualTo("http://localhost:8080/api/v1/dev/storage/upload?sig=abc");
            assertThat(response.getStorageKey()).isEqualTo(generatedKey);
            assertThat(response.getHttpMethod()).isEqualTo("PUT");
        }

        @Test
        @DisplayName("Should throw exception for unsupported content type")
        void presignAvatarUpload_UnsupportedContentType() {
            AvatarPresignRequest request = AvatarPresignRequest.builder()
                    .fileName("document.pdf")
                    .contentType("application/pdf")
                    .sizeBytes(1024L)
                    .build();

            assertThatThrownBy(() -> userProfileService.presignAvatarUpload(1L, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.UPLOAD_UNSUPPORTED_MEDIA_TYPE));
        }

        @Test
        @DisplayName("Should throw exception for file size exceeding 5MB")
        void presignAvatarUpload_FileTooLarge() {
            AvatarPresignRequest request = AvatarPresignRequest.builder()
                    .fileName("huge.png")
                    .contentType("image/png")
                    .sizeBytes(6L * 1024 * 1024) // 6MB
                    .build();

            assertThatThrownBy(() -> userProfileService.presignAvatarUpload(1L, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.UPLOAD_FILE_TOO_LARGE));
        }
    }

    @Nested
    @DisplayName("updateProfile & completeProfile Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update user profile and delete old avatar on storage")
        void updateProfile_Success_WithNewAvatar() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(storageService.presignDownload(eq("users/1/avatar/new-uuid.webp"), any(Duration.class)))
                    .thenReturn("http://localhost:8080/signed-download-url");

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .fullName("Nguyễn Văn B")
                    .avatarKey("users/1/avatar/new-uuid.webp")
                    .bio("Tiểu sử mới cập nhật")
                    .expertise("Senior Java Engineer")
                    .interests(List.of("Cloud", "AI"))
                    .build();

            UserResponse response = userProfileService.updateProfile(1L, request);

            assertThat(response).isNotNull();
            assertThat(response.getFullName()).isEqualTo("Nguyễn Văn B");
            assertThat(response.getAvatarKey()).isEqualTo("users/1/avatar/new-uuid.webp");
            assertThat(response.getAvatarUrl()).isEqualTo("http://localhost:8080/signed-download-url");
            assertThat(response.getBio()).isEqualTo("Tiểu sử mới cập nhật");
            assertThat(response.getExpertise()).isEqualTo("Senior Java Engineer");

            // Verify old avatar was deleted from storage
            verify(storageService).delete("users/1/avatar/old-uuid.png");
            verify(userRepository).save(sampleUser);
        }

        @Test
        @DisplayName("Should throw exception if user not found")
        void updateProfile_UserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .fullName("Người dùng lạ")
                    .build();

            assertThatThrownBy(() -> userProfileService.updateProfile(99L, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("completeProfile: Should throw exception if fullName is missing")
        void completeProfile_MissingFullName() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .fullName("")
                    .build();

            assertThatThrownBy(() -> userProfileService.completeProfile(1L, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                            .isEqualTo(ErrorCode.FIELD_REQUIRED));
        }
    }
}
