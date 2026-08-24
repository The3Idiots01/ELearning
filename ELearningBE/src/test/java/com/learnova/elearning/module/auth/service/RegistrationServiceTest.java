package com.learnova.elearning.module.auth.service;

import com.learnova.elearning.module.auth.dto.request.RegisterRequest;
import com.learnova.elearning.module.auth.dto.response.RegisterPendingResponse;
import com.learnova.elearning.module.auth.model.ConfirmResult;
import com.learnova.elearning.module.auth.model.PendingRegisterDto;
import com.learnova.elearning.module.user.entity.User;
import com.learnova.elearning.module.user.entity.enums.AuthProvider;
import com.learnova.elearning.module.user.entity.enums.UserRole;
import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.integration.mail.EmailService;
import com.learnova.elearning.module.user.repository.UserRepository;
import com.learnova.elearning.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private EmailService emailService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private RegistrationService registrationService;

    private EmailTemplateService emailTemplateService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        emailTemplateService = new EmailTemplateService(templateEngine);

        ReflectionTestUtils.setField(registrationService, "emailTemplateService", emailTemplateService);
        ReflectionTestUtils.setField(registrationService, "configuredBaseUrl", "http://localhost:8080");
    }

    @Test
    @DisplayName("Register: Should save pending data to Redis and send confirmation email")
    void register_Success() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyen Van A")
                .email("test@example.com")
                .password("Password123!")
                .build();

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setScheme("http");
        httpRequest.setServerName("localhost");
        httpRequest.setServerPort(8080);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtTokenProvider.generateAccountConfirmationToken(eq("test@example.com"), anyString())).thenReturn("mock_token");
        when(emailService.sendEmail(eq("test@example.com"), anyString(), anyString(), eq(true))).thenReturn(true);

        // Act
        RegisterPendingResponse response = registrationService.register(request, httpRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getExpiresInMinutes()).isEqualTo(15);

        // Verify Redis set called with 15 min duration
        verify(valueOperations).set(eq("PENDING_REG:test@example.com"), any(PendingRegisterDto.class), eq(Duration.ofMinutes(15)));
        verify(emailService).sendEmail(eq("test@example.com"), anyString(), contains("http://localhost:8080/api/v1/auth/confirm-account?token=mock_token"), eq(true));
    }

    @Test
    @DisplayName("Register: Should throw exception if email already exists in DB")
    void register_EmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyen Van A")
                .email("existing@example.com")
                .password("Password123!")
                .build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request, null))
                .isInstanceOf(AppException.class);

        verify(redisTemplate, never()).opsForValue();
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("ConfirmAccount: Should save user to DB, delete Redis key, and return success result")
    void confirmAccount_Success() {
        String token = "valid_token";
        String email = "test@example.com";
        String tokenId = UUID.randomUUID().toString();

        PendingRegisterDto pendingDto = PendingRegisterDto.builder()
                .fullName("Nguyen Van A")
                .email(email)
                .passwordHash("hashed_password")
                .tokenId(tokenId)
                .build();

        User savedUser = User.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .email(email)
                .passwordHash("hashed_password")
                .role(UserRole.LEARNER)
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .build();

        when(jwtTokenProvider.validateConfirmationToken(token)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromConfirmationToken(token)).thenReturn(email);
        when(jwtTokenProvider.getTokenIdFromConfirmationToken(token)).thenReturn(tokenId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("PENDING_REG:" + email)).thenReturn(pendingDto);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        ConfirmResult result = registrationService.confirmAccount(token, null);

        // Assert
        assertThat(result.success()).isTrue();
        assertThat(result.displayName()).isEqualTo("Nguyen Van A");
        assertThat(result.target()).isEqualTo(ConfirmResult.RedirectTarget.LOGIN);

        verify(userRepository).save(any(User.class));
        verify(redisTemplate).delete("PENDING_REG:" + email);
    }

    @Test
    @DisplayName("ConfirmAccount: Should return failure result if token is expired or invalid")
    void confirmAccount_InvalidToken() {
        String token = "invalid_or_expired_token";
        when(jwtTokenProvider.validateConfirmationToken(token)).thenReturn(false);

        ConfirmResult result = registrationService.confirmAccount(token, null);

        assertThat(result.success()).isFalse();
        assertThat(result.target()).isEqualTo(ConfirmResult.RedirectTarget.REGISTER);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("ConfirmAccount: Should return failure result if pending registration not found in Redis")
    void confirmAccount_PendingRegistrationNotFoundInRedis() {
        String token = "valid_token";
        String email = "test@example.com";
        String tokenId = "some_token_id";

        when(jwtTokenProvider.validateConfirmationToken(token)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromConfirmationToken(token)).thenReturn(email);
        when(jwtTokenProvider.getTokenIdFromConfirmationToken(token)).thenReturn(tokenId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("PENDING_REG:" + email)).thenReturn(null);

        ConfirmResult result = registrationService.confirmAccount(token, null);

        assertThat(result.success()).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("ConfirmAccount: Should return failure result if tokenId does not match (re-registered)")
    void confirmAccount_TokenIdMismatch() {
        String oldToken = "old_token";
        String email = "test@example.com";
        String oldTokenId = "old_uuid";
        String newTokenId = "new_uuid";

        PendingRegisterDto pendingDto = PendingRegisterDto.builder()
                .fullName("Nguyen Van A")
                .email(email)
                .passwordHash("hashed_password")
                .tokenId(newTokenId) // Redis has new UUID from re-registration
                .build();

        when(jwtTokenProvider.validateConfirmationToken(oldToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromConfirmationToken(oldToken)).thenReturn(email);
        when(jwtTokenProvider.getTokenIdFromConfirmationToken(oldToken)).thenReturn(oldTokenId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("PENDING_REG:" + email)).thenReturn(pendingDto);

        ConfirmResult result = registrationService.confirmAccount(oldToken, null);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Liên kết xác nhận này đã bị vô hiệu hóa");
        assertThat(result.target()).isEqualTo(ConfirmResult.RedirectTarget.REGISTER);
        verify(userRepository, never()).save(any(User.class));
    }
}
