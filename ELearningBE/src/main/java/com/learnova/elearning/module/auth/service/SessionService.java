package com.learnova.elearning.module.auth.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.common.util.CookieUtil;
import com.learnova.elearning.module.auth.dto.request.LoginRequest;
import com.learnova.elearning.module.auth.dto.response.AuthResponse;
import com.learnova.elearning.module.user.dto.UserResponse;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.repository.UserRepository;
import com.learnova.elearning.security.CustomUserDetails;
import com.learnova.elearning.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Xử lý phiên đăng nhập: login, refresh token (rotate), logout và truy vấn user hiện tại.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private static final String REDIS_REFRESH_TOKEN_PREFIX = "RT:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getIsActive() != null && !user.getIsActive()) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        // Sinh Access Token (chỉ chứa id & role) và Refresh Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Lưu Refresh Token vào Redis
        saveRefreshTokenToRedis(user.getId(), refreshToken);

        // Đính kèm Refresh Token vào HttpOnly Cookie với SameSite=Strict
        addRefreshTokenCookie(response, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .user(UserResponse.fromEntity(user))
                .build();
    }

    public AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = CookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME)
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID));

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // Kiểm tra Refresh Token trong Redis
        String redisKey = REDIS_REFRESH_TOKEN_PREFIX + userId;
        Object storedToken = redisTemplate.opsForValue().get(redisKey);

        if (storedToken == null || !refreshToken.equals(storedToken.toString())) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getIsActive() != null && !user.getIsActive()) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        // Tạo Access Token mới và rotate Refresh Token mới
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Cập nhật Refresh Token mới vào Redis
        saveRefreshTokenToRedis(user.getId(), newRefreshToken);

        // Cập nhật lại Cookie cho client
        addRefreshTokenCookie(response, newRefreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .user(UserResponse.fromEntity(user))
                .build();
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME)
                .ifPresent(refreshToken -> {
                    if (jwtTokenProvider.validateToken(refreshToken)) {
                        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
                        redisTemplate.delete(REDIS_REFRESH_TOKEN_PREFIX + userId);
                    }
                });

        // Xóa Cookie ở client
        ResponseCookie deleteCookie = CookieUtil.deleteRefreshTokenCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
    }

    public UserResponse getCurrentUser(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.fromEntity(user);
    }

    private void saveRefreshTokenToRedis(Long userId, String refreshToken) {
        String redisKey = REDIS_REFRESH_TOKEN_PREFIX + userId;
        long expirationMs = jwtTokenProvider.getRefreshTokenExpirationMs();
        redisTemplate.opsForValue().set(redisKey, refreshToken, Duration.ofMillis(expirationMs));
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        long maxAgeSeconds = jwtTokenProvider.getRefreshTokenExpirationMs() / 1000;
        ResponseCookie cookie = CookieUtil.createRefreshTokenCookie(refreshToken, maxAgeSeconds);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
