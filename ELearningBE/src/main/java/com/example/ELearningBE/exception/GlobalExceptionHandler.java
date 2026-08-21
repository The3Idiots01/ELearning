package com.example.ELearningBE.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ProblemDetail> handleAppException(AppException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("AppException occurred [code={}]: {}", errorCode.getCode(), ex.getMessage());

        ProblemDetail problemDetail = createProblemDetail(
                errorCode.getHttpStatusCode(),
                errorCode.name(),
                ex.getMessage() != null ? ex.getMessage() : errorCode.getMessage(),
                errorCode.getCode(),
                request
        );

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation failed for request: {}", request.getRequestURI());

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String message = fieldError.getDefaultMessage();
            // Check if validation message corresponds to a predefined ErrorCode enum name
            try {
                if (message != null) {
                    ErrorCode mappedErrorCode = ErrorCode.valueOf(message);
                    errorCode = mappedErrorCode;
                    message = mappedErrorCode.getMessage();
                }
            } catch (IllegalArgumentException ignored) {
                // Keep the default validation message if not an ErrorCode name
            }
            fieldErrors.put(fieldError.getField(), message);
        }

        ProblemDetail problemDetail = createProblemDetail(
                errorCode.getHttpStatusCode(),
                errorCode.name(),
                errorCode.getMessage(),
                errorCode.getCode(),
                request
        );
        problemDetail.setProperty("errors", fieldErrors);

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(problemDetail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        Map<String, String> violations = new LinkedHashMap<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            violations.put(propertyPath, violation.getMessage());
        }

        ProblemDetail problemDetail = createProblemDetail(
                errorCode.getHttpStatusCode(),
                errorCode.name(),
                errorCode.getMessage(),
                errorCode.getCode(),
                request
        );
        problemDetail.setProperty("errors", violations);

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(problemDetail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for request {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        ProblemDetail problemDetail = createProblemDetail(
                errorCode.getHttpStatusCode(),
                errorCode.name(),
                errorCode.getMessage(),
                errorCode.getCode(),
                request
        );

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(problemDetail);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failure for request {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        ProblemDetail problemDetail = createProblemDetail(
                errorCode.getHttpStatusCode(),
                errorCode.name(),
                ex.getMessage() != null ? ex.getMessage() : errorCode.getMessage(),
                errorCode.getCode(),
                request
        );

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(problemDetail);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method not allowed [{}]: {}", request.getMethod(), ex.getMessage());

        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        ProblemDetail problemDetail = createProblemDetail(
                errorCode.getHttpStatusCode(),
                errorCode.name(),
                ex.getMessage(),
                errorCode.getCode(),
                request
        );

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(problemDetail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed HTTP request: {}", ex.getMessage());

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        ProblemDetail problemDetail = createProblemDetail(
                errorCode.getHttpStatusCode(),
                errorCode.name(),
                "Malformed JSON request or invalid request body format",
                errorCode.getCode(),
                request
        );

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(problemDetail);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getResourcePath());

        ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
        ProblemDetail problemDetail = createProblemDetail(
                errorCode.getHttpStatusCode(),
                errorCode.name(),
                ex.getMessage(),
                errorCode.getCode(),
                request
        );

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUncaughtException(
            Exception ex, HttpServletRequest request) {
        log.error("Uncaught exception occurred while processing request to {}", request.getRequestURI(), ex);

        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        ProblemDetail problemDetail = createProblemDetail(
                errorCode.getHttpStatusCode(),
                errorCode.name(),
                errorCode.getMessage(),
                errorCode.getCode(),
                request
        );

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(problemDetail);
    }

    private ProblemDetail createProblemDetail(
            HttpStatusCode status,
            String title,
            String detail,
            int code,
            HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("code", code);
        problemDetail.setProperty("timestamp", Instant.now());

        if (request != null) {
            problemDetail.setInstance(URI.create(request.getRequestURI()));
        }

        return problemDetail;
    }
}
