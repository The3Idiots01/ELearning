package com.example.ELearningBE.service.impl;

import com.example.ELearningBE.dto.request.auth.LoginRequest;
import com.example.ELearningBE.dto.request.auth.PendingRegisterDto;
import com.example.ELearningBE.dto.request.auth.RegisterRequest;
import com.example.ELearningBE.dto.response.auth.AuthResponse;
import com.example.ELearningBE.dto.response.auth.RegisterPendingResponse;
import com.example.ELearningBE.dto.response.user.UserResponse;
import com.example.ELearningBE.entity.User;
import com.example.ELearningBE.entity.enums.AuthProvider;
import com.example.ELearningBE.entity.enums.UserRole;
import com.example.ELearningBE.exception.AppException;
import com.example.ELearningBE.exception.ErrorCode;
import com.example.ELearningBE.integration.mail.EmailService;
import com.example.ELearningBE.repository.UserRepository;
import com.example.ELearningBE.security.CustomUserDetails;
import com.example.ELearningBE.security.JwtTokenProvider;
import com.example.ELearningBE.service.AuthService;
import com.example.ELearningBE.service.EmailTemplateService;
import com.example.ELearningBE.util.AppUrlUtil;
import com.example.ELearningBE.util.CookieUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String REDIS_REFRESH_TOKEN_PREFIX = "RT:";
    private static final String REDIS_PENDING_REGISTER_PREFIX = "PENDING_REG:";
    private static final String CONFIRM_ENDPOINT_PATH = "/api/v1/auth/confirm-account";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final ObjectMapper objectMapper;

    @Value("${app.base-url:}")
    private String configuredBaseUrl;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public RegisterPendingResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. Kiểm tra tài khoản đã tồn tại trong DB chưa
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 2. Tạo mã UUID ngẫu nhiên cho confirmation token
        String tokenId = UUID.randomUUID().toString();

        // 3. Hash password và chuẩn bị PendingRegisterDto
        PendingRegisterDto pendingDto = PendingRegisterDto.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .tokenId(tokenId)
                .build();

        // 4. Lưu vào Redis với Key là email (prefixed), TTL 15 phút
        // Nếu user đăng ký lại với cùng email, lệnh set này sẽ ghi đè giá trị và làm mới TTL 15 phút
        String redisKey = REDIS_PENDING_REGISTER_PREFIX + email;
        redisTemplate.opsForValue().set(redisKey, pendingDto, Duration.ofMinutes(15));
        log.info("Saved pending registration to Redis for email: [{}], tokenId: [{}]", email, tokenId);

        // 5. Sinh Confirmation Token (JWT chứa email và UUID ngẫu nhiên)
        String confirmationToken = jwtTokenProvider.generateAccountConfirmationToken(email, tokenId);

        // 6. Tự động xác định Base URL động theo domain/host của request thực tế hoặc cấu hình APP_BASE_URL
        String baseUrl = AppUrlUtil.resolveBaseUrl(configuredBaseUrl, httpRequest);
        String activationLink = baseUrl + CONFIRM_ENDPOINT_PATH + "?token=" + confirmationToken;

        // In đường dẫn xác thực ra console để tiện test
        log.info("\n===================================================================================="
                + "\n📧 [ACCOUNT CONFIRMATION LINK] Email: [{}]"
                + "\n👉 Link: {}"
                + "\n====================================================================================",
                email, activationLink);

        // 7. Gửi email kích hoạt tài khoản bằng HTML template đọc từ resources/templates
        String emailContent = emailTemplateService.buildAccountActivationEmail(pendingDto.getFullName(), email, activationLink);
        boolean emailSent = emailService.sendEmail(email, "Xác nhận đăng ký tài khoản E-Learning", emailContent, true);
        if (!emailSent) {
            log.warn("Failed to send activation email to: [{}]", email);
        }

        return RegisterPendingResponse.builder()
                .email(email)
                .message("Đăng ký thành công! Vui lòng kiểm tra email của bạn để kích hoạt tài khoản trong vòng 15 phút.")
                .expiresInMinutes(15)
                .build();
    }

    @Override
    @Transactional
    public String confirmAccount(String token, HttpServletRequest httpRequest) {
        String registerUrl = frontendUrl + "/register";
        String loginUrl = frontendUrl + "/login";

        // 1. Validate Token format và Signature
        if (!jwtTokenProvider.validateConfirmationToken(token)) {
            log.warn("Invalid or expired confirmation token presented.");
            return emailTemplateService.buildConfirmationFailurePage(
                    "Liên kết xác nhận tài khoản không hợp lệ hoặc đã hết hạn (quá 15 phút). Vui lòng thực hiện đăng ký lại.",
                    registerUrl
            );
        }

        String email = jwtTokenProvider.getEmailFromConfirmationToken(token);
        String tokenId = jwtTokenProvider.getTokenIdFromConfirmationToken(token);

        if (email == null || tokenId == null) {
            return emailTemplateService.buildConfirmationFailurePage(
                    "Thông tin xác thực trong liên kết không đầy đủ.",
                    registerUrl
            );
        }

        // 2. Lấy dữ liệu tạm từ Redis
        String redisKey = REDIS_PENDING_REGISTER_PREFIX + email;
        Object storedData = redisTemplate.opsForValue().get(redisKey);

        if (storedData == null) {
            log.warn("No pending registration found in Redis for email: [{}]", email);
            return emailTemplateService.buildConfirmationFailurePage(
                    "Yêu cầu xác nhận tài khoản không tồn tại hoặc đã hết hạn (quá 15 phút). Vui lòng đăng ký lại.",
                    registerUrl
            );
        }

        PendingRegisterDto pendingDto;
        if (storedData instanceof PendingRegisterDto p) {
            pendingDto = p;
        } else {
            try {
                pendingDto = objectMapper.convertValue(storedData, PendingRegisterDto.class);
            } catch (Exception e) {
                log.error("Failed to parse pending register data from Redis for email: {}", email, e);
                return emailTemplateService.buildConfirmationFailurePage(
                        "Dữ liệu xác nhận không hợp lệ. Vui lòng đăng ký lại.",
                        registerUrl
                );
            }
        }

        // 3. Kiểm tra TokenId (UUID) trùng khớp để tránh dùng token cũ khi user đã re-register
        if (!tokenId.equals(pendingDto.getTokenId())) {
            log.warn("TokenId mismatch for email [{}]. Expected: [{}], Got: [{}]", email, pendingDto.getTokenId(), tokenId);
            return emailTemplateService.buildConfirmationFailurePage(
                    "Liên kết xác nhận này đã bị vô hiệu hóa do bạn đã thực hiện đăng ký lại. Vui lòng kiểm tra email mới nhất.",
                    registerUrl
            );
        }

        // 4. Kiểm tra phòng trường hợp email đã được kích hoạt trước đó
        if (userRepository.existsByEmail(email)) {
            redisTemplate.delete(redisKey);
            return emailTemplateService.buildConfirmationFailurePage(
                    "Tài khoản của bạn đã được kích hoạt trước đó. Bạn có thể tiến hành đăng nhập.",
                    loginUrl
            );
        }

        // 5. Tạo mới User vào Database (mặc định role là LEARNER)
        User user = User.builder()
                .fullName(pendingDto.getFullName())
                .email(pendingDto.getEmail())
                .passwordHash(pendingDto.getPasswordHash())
                .role(UserRole.LEARNER)
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully confirmed and created account for user id: [{}], email: [{}]", savedUser.getId(), savedUser.getEmail());

        // 6. Xóa dữ liệu tạm trong Redis sau khi đã tạo account thành công
        redisTemplate.delete(redisKey);

        // 7. Trả về trang HTML thông báo kích hoạt thành công
        return emailTemplateService.buildConfirmationSuccessPage(savedUser.getFullName(), loginUrl);
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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
