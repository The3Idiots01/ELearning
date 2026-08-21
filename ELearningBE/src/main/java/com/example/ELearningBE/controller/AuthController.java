package com.example.ELearningBE.controller;

import com.example.ELearningBE.dto.request.auth.LoginRequest;
import com.example.ELearningBE.dto.request.auth.RegisterRequest;
import com.example.ELearningBE.dto.response.auth.AuthResponse;
import com.example.ELearningBE.dto.response.auth.RegisterPendingResponse;
import com.example.ELearningBE.dto.response.common.ApiResponse;
import com.example.ELearningBE.dto.response.user.UserResponse;
import com.example.ELearningBE.security.CustomUserDetails;
import com.example.ELearningBE.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterPendingResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        RegisterPendingResponse pendingResponse = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.", pendingResponse));
    }

    @GetMapping(value = "/confirm-account", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmAccount(
            @RequestParam("token") String token,
            HttpServletRequest httpRequest
    ) {
        String htmlResult = authService.confirmAccount(token, httpRequest);
        return ResponseEntity.ok(htmlResult);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.refreshToken(request, response);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserResponse userResponse = authService.getCurrentUser(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Get profile successful", userResponse));
    }
}
