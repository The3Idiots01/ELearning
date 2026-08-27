package com.learnova.elearning.module.user.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.user.dto.UserResponse;
import com.learnova.elearning.module.user.dto.request.AvatarPresignRequest;
import com.learnova.elearning.module.user.dto.request.UpdateProfileRequest;
import com.learnova.elearning.module.user.dto.response.AvatarPresignResponse;
import com.learnova.elearning.module.user.service.UserProfileService;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserResponse response = userProfileService.getProfile(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hồ sơ thành công", response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse response = userProfileService.updateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin hồ sơ thành công", response));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<UserResponse>> completeProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse response = userProfileService.completeProfile(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Hoàn tất thiết lập hồ sơ thành công", response));
    }

    @PostMapping("/avatar/presign")
    public ResponseEntity<ApiResponse<AvatarPresignResponse>> presignAvatar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AvatarPresignRequest request
    ) {
        AvatarPresignResponse response = userProfileService.presignAvatarUpload(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Tạo Presigned URL tải ảnh đại diện thành công", response));
    }
}
