package com.learnova.elearning.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    // General & System Errors (9999, 1001 - 1004)
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized server error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(1002, "Invalid request payload", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(1003, "Resource not found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(1004, "Method not allowed", HttpStatus.METHOD_NOT_ALLOWED),

    // Authentication & Authorization (1005 - 1012)
    UNAUTHENTICATED(1005, "Unauthenticated, please log in", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1006, "You do not have permission to access this resource", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS(1007, "Invalid email or password", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(1008, "Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(1009, "Invalid or malformed token", HttpStatus.UNAUTHORIZED),
    CONFIRMATION_TOKEN_INVALID(1010, "Confirmation token is invalid or corrupted", HttpStatus.BAD_REQUEST),
    CONFIRMATION_TOKEN_EXPIRED(1011, "Confirmation token has expired (15 minutes limit)", HttpStatus.BAD_REQUEST),
    PENDING_REGISTRATION_NOT_FOUND(1012, "No pending registration found or account already confirmed", HttpStatus.BAD_REQUEST),

    // User Domain (1101 - 1106)
    USER_NOT_FOUND(1101, "User not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(1102, "User already exists", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(1103, "Email is already registered", HttpStatus.CONFLICT),
    USER_INACTIVE(1104, "User account is inactive or disabled", HttpStatus.FORBIDDEN),
    OLD_PASSWORD_INCORRECT(1105, "Current password does not match", HttpStatus.BAD_REQUEST),
    PASSWORD_CONFIRMATION_MISMATCH(1106, "Password confirmation does not match", HttpStatus.BAD_REQUEST),

    // Validation (2001 - 2004)
    VALIDATION_ERROR(2001, "Validation failed", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_FORMAT(2002, "Invalid email format", HttpStatus.BAD_REQUEST),
    PASSWORD_TOO_WEAK(2003, "Password does not meet complexity requirements", HttpStatus.BAD_REQUEST),
    FIELD_REQUIRED(2004, "Required field is missing", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatusCode httpStatusCode;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}
