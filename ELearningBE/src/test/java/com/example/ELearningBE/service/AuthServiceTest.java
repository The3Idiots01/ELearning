package com.example.ELearningBE.service;

import com.example.ELearningBE.dto.request.auth.PendingRegisterDto;
import com.example.ELearningBE.dto.request.auth.RegisterRequest;
import com.example.ELearningBE.dto.response.auth.RegisterPendingResponse;
import com.example.ELearningBE.entity.User;
import com.example.ELearningBE.entity.enums.AuthProvider;
import com.example.ELearningBE.entity.enums.UserRole;
import com.example.ELearningBE.exception.AppException;
import com.example.ELearningBE.integration.mail.EmailService;
import com.example.ELearningBE.repository.UserRepository;
import com.example.ELearningBE.security.JwtTokenProvider;
import com.example.ELearningBE.service.impl.AuthServiceImpl;
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
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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
    private AuthServiceImpl authService;

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

        ReflectionTestUtils.setField(authService, "emailTemplateService", emailTemplateService);
        ReflectionTestUtils.setField(authService, "configuredBaseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:5173");
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
        RegisterPendingResponse response = authService.register(request, httpRequest);

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

        assertThatThrownBy(() -> authService.register(request, null))
                .isInstanceOf(AppException.class);

        verify(redisTemplate, never()).opsForValue();
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("ConfirmAccount: Should save user to DB, delete Redis key, and return success HTML")
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
        String htmlResult = authService.confirmAccount(token, null);

        // Assert
        assertThat(htmlResult).contains("Kích Hoạt Thành Công!");
        assertThat(htmlResult).contains("Nguyen Van A");
        assertThat(htmlResult).contains("http://localhost:5173/login");

        verify(userRepository).save(any(User.class));
        verify(redisTemplate).delete("PENDING_REG:" + email);
    }

    @Test
    @DisplayName("ConfirmAccount: Should return failure HTML if token is expired or invalid")
    void confirmAccount_InvalidToken() {
        String token = "invalid_or_expired_token";
        when(jwtTokenProvider.validateConfirmationToken(token)).thenReturn(false);

        String htmlResult = authService.confirmAccount(token, null);

        assertThat(htmlResult).contains("Xác Nhận Không Thành Công");
        assertThat(htmlResult).contains("http://localhost:5173/register");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("ConfirmAccount: Should return failure HTML if pending registration not found in Redis")
    void confirmAccount_PendingRegistrationNotFoundInRedis() {
        String token = "valid_token";
        String email = "test@example.com";
        String tokenId = "some_token_id";

        when(jwtTokenProvider.validateConfirmationToken(token)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromConfirmationToken(token)).thenReturn(email);
        when(jwtTokenProvider.getTokenIdFromConfirmationToken(token)).thenReturn(tokenId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("PENDING_REG:" + email)).thenReturn(null);

        String htmlResult = authService.confirmAccount(token, null);

        assertThat(htmlResult).contains("Xác Nhận Không Thành Công");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("ConfirmAccount: Should return failure HTML if tokenId does not match (re-registered)")
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

        String htmlResult = authService.confirmAccount(oldToken, null);

        assertThat(htmlResult).contains("Xác Nhận Không Thành Công");
        assertThat(htmlResult).contains("Liên kết xác nhận này đã bị vô hiệu hóa");
        verify(userRepository, never()).save(any(User.class));
    }
}
