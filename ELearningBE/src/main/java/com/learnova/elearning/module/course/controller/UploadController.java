package com.learnova.elearning.module.course.controller;

import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.course.dto.request.PresignUploadRequest;
import com.learnova.elearning.module.course.dto.response.PresignUploadResponse;
import com.learnova.elearning.module.course.service.UploadService;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lecturer/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/presign")
    public ResponseEntity<ApiResponse<PresignUploadResponse>> presign(
            @Valid @RequestBody PresignUploadRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PresignUploadResponse response = uploadService.presign(request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Presigned upload URL created", response));
    }
}
