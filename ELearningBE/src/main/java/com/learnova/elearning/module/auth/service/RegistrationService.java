package com.learnova.elearning.module.auth.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.common.util.AppUrlUtil;
import com.learnova.elearning.integration.mail.EmailService;
import com.learnova.elearning.module.auth.dto.request.RegisterRequest;
import com.learnova.elearning.module.auth.dto.response.RegisterPendingResponse;
import com.learnova.elearning.module.auth.model.ConfirmResult;
import com.learnova.elearning.module.auth.model.ConfirmResult.RedirectTarget;
import com.learnova.elearning.module.auth.model.PendingRegisterDto;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.entity.enums.AuthProvider;
import com.learnova.elearning.module.user.entity.enums.UserRole;
import com.learnova.elearning.module.user.repository.UserRepository;
import com.learnova.elearning.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

/**
 * Xử lý đăng ký tài khoản: lưu pending vào Redis, gửi email kích hoạt, và xác nhận tài khoản.
 * Chỉ chứa logic nghiệp vụ — không sinh HTML (việc render trang do controller/view đảm nhiệm).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

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

    @Transactional
    public ConfirmResult confirmAccount(String token, HttpServletRequest httpRequest) {
        // 1. Validate Token format và Signature
        if (!jwtTokenProvider.validateConfirmationToken(token)) {
            log.warn("Invalid or expired confirmation token presented.");
            return ConfirmResult.failure(
                    "Liên kết xác nhận tài khoản không hợp lệ hoặc đã hết hạn (quá 15 phút). Vui lòng thực hiện đăng ký lại.",
                    RedirectTarget.REGISTER
            );
        }

        String email = jwtTokenProvider.getEmailFromConfirmationToken(token);
        String tokenId = jwtTokenProvider.getTokenIdFromConfirmationToken(token);

        if (email == null || tokenId == null) {
            return ConfirmResult.failure(
                    "Thông tin xác thực trong liên kết không đầy đủ.",
                    RedirectTarget.REGISTER
            );
        }

        // 2. Lấy dữ liệu tạm từ Redis
        String redisKey = REDIS_PENDING_REGISTER_PREFIX + email;
        Object storedData = redisTemplate.opsForValue().get(redisKey);

        if (storedData == null) {
            log.warn("No pending registration found in Redis for email: [{}]", email);
            return ConfirmResult.failure(
                    "Yêu cầu xác nhận tài khoản không tồn tại hoặc đã hết hạn (quá 15 phút). Vui lòng đăng ký lại.",
                    RedirectTarget.REGISTER
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
                return ConfirmResult.failure(
                        "Dữ liệu xác nhận không hợp lệ. Vui lòng đăng ký lại.",
                        RedirectTarget.REGISTER
                );
            }
        }

        // 3. Kiểm tra TokenId (UUID) trùng khớp để tránh dùng token cũ khi user đã re-register
        if (!tokenId.equals(pendingDto.getTokenId())) {
            log.warn("TokenId mismatch for email [{}]. Expected: [{}], Got: [{}]", email, pendingDto.getTokenId(), tokenId);
            return ConfirmResult.failure(
                    "Liên kết xác nhận này đã bị vô hiệu hóa do bạn đã thực hiện đăng ký lại. Vui lòng kiểm tra email mới nhất.",
                    RedirectTarget.REGISTER
            );
        }

        // 4. Kiểm tra phòng trường hợp email đã được kích hoạt trước đó
        if (userRepository.existsByEmail(email)) {
            redisTemplate.delete(redisKey);
            return ConfirmResult.failure(
                    "Tài khoản của bạn đã được kích hoạt trước đó. Bạn có thể tiến hành đăng nhập.",
                    RedirectTarget.LOGIN
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

        // 7. Trả về kết quả nghiệp vụ; controller sẽ render trang thông báo thành công
        return ConfirmResult.success(savedUser.getFullName());
    }
}
