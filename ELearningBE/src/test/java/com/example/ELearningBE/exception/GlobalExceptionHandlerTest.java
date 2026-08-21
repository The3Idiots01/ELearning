package com.example.ELearningBE.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should verify all ErrorCode codes are in range 1000 to 9999 and have valid HttpStatus")
    void shouldVerifyErrorCodeRange() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            assertThat(errorCode.getCode())
                    .as("ErrorCode %s should be between 1000 and 9999", errorCode.name())
                    .isBetween(1000, 9999);
            assertThat(errorCode.getMessage())
                    .as("ErrorCode %s message should not be blank", errorCode.name())
                    .isNotBlank();
            assertThat(errorCode.getHttpStatusCode())
                    .as("ErrorCode %s httpStatusCode should not be null", errorCode.name())
                    .isNotNull();
        }
    }

    @Test
    @DisplayName("Should handle AppException (USER_NOT_FOUND) with ProblemDetail response")
    void shouldHandleAppExceptionUserNotFound() {
        when(request.getRequestURI()).thenReturn("/api/v1/users/99");
        AppException ex = new AppException(ErrorCode.USER_NOT_FOUND);

        ResponseEntity<ProblemDetail> response = handler.handleAppException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("USER_NOT_FOUND");
        assertThat(response.getBody().getDetail()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
        assertThat(response.getBody().getProperties()).containsEntry("code", 1101);
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
        assertThat(response.getBody().getInstance()).hasToString("/api/v1/users/99");
    }

    @Test
    @DisplayName("Should handle AppException with custom message override")
    void shouldHandleAppExceptionWithCustomMessage() {
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        AppException ex = new AppException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email user@test.com is already in use");

        ResponseEntity<ProblemDetail> response = handler.handleAppException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("EMAIL_ALREADY_EXISTS");
        assertThat(response.getBody().getDetail()).isEqualTo("Email user@test.com is already in use");
        assertThat(response.getBody().getProperties()).containsEntry("code", 1103);
    }

    @Test
    @DisplayName("Should handle AccessDeniedException returning 403 Forbidden")
    void shouldHandleAccessDeniedException() {
        when(request.getRequestURI()).thenReturn("/api/v1/admin/users");
        AccessDeniedException ex = new AccessDeniedException("Forbidden access");

        ResponseEntity<ProblemDetail> response = handler.handleAccessDeniedException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().getProperties()).containsEntry("code", 1006);
    }

    @Test
    @DisplayName("Should handle AuthenticationException returning 401 Unauthorized")
    void shouldHandleAuthenticationException() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ProblemDetail> response = handler.handleAuthenticationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("UNAUTHENTICATED");
        assertThat(response.getBody().getProperties()).containsEntry("code", 1005);
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with field errors map")
    void shouldHandleMethodArgumentNotValidException() throws NoSuchMethodException {
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "userDto");
        bindingResult.addError(new FieldError("userDto", "email", "Email cannot be empty"));
        bindingResult.addError(new FieldError("userDto", "fullName", "Full name is required"));

        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("shouldHandleMethodArgumentNotValidException"),
                -1
        );
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ProblemDetail> response = handler.handleMethodArgumentNotValidException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getProperties()).containsEntry("code", 2001);
        assertThat(response.getBody().getProperties()).containsKey("errors");
    }

    @Test
    @DisplayName("Should handle generic unhandled Exception returning 500 Internal Server Error")
    void shouldHandleGenericException() {
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        RuntimeException ex = new RuntimeException("Unexpected database glitch");

        ResponseEntity<ProblemDetail> response = handler.handleUncaughtException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("UNCATEGORIZED_EXCEPTION");
        assertThat(response.getBody().getProperties()).containsEntry("code", 9999);
    }
}
