package com.learnova.elearning.module.auth.controller;

import com.learnova.elearning.module.auth.dto.request.LoginRequest;
import com.learnova.elearning.module.auth.dto.request.RegisterRequest;
import com.learnova.elearning.module.auth.dto.response.AuthResponse;
import com.learnova.elearning.module.auth.dto.response.RegisterPendingResponse;
import com.learnova.elearning.common.dto.ApiResponse;
import com.learnova.elearning.module.auth.service.RegistrationService;
import com.learnova.elearning.module.auth.service.SessionService;
import com.learnova.elearning.module.user.dto.UserResponse;
import com.learnova.elearning.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;
    private final SessionService sessionService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterPendingResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        RegisterPendingResponse pendingResponse = registrationService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.", pendingResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = sessionService.login(request, response);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = sessionService.refreshToken(request, response);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        sessionService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserResponse userResponse = sessionService.getCurrentUser(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Get profile successful", userResponse));
    }
}
