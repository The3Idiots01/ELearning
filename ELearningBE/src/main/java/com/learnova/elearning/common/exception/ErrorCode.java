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
    FIELD_REQUIRED(2004, "Required field is missing", HttpStatus.BAD_REQUEST),

    // Course Domain (1201 - 1210)
    COURSE_NOT_FOUND(1201, "Course not found", HttpStatus.NOT_FOUND),
    COURSE_ACCESS_DENIED(1202, "You are not the owner of this course", HttpStatus.FORBIDDEN),
    COURSE_INVALID_STATUS_TRANSITION(1203, "Invalid course status transition", HttpStatus.CONFLICT),
    COURSE_NOT_READY_TO_PUBLISH(1204, "Course does not meet publishing requirements", HttpStatus.UNPROCESSABLE_ENTITY),
    COURSE_PRICE_OUT_OF_RANGE(1205, "Price must be between 0 and 10,000,000 VND", HttpStatus.BAD_REQUEST),
    COURSE_SLUG_ALREADY_EXISTS(1206, "Course slug already exists", HttpStatus.CONFLICT),
    COURSE_HAS_ENROLLMENTS(1207, "Course already has learners, use unpublish instead", HttpStatus.CONFLICT),
    COURSE_MODIFIED_CONCURRENTLY(1208, "Course was modified elsewhere, please reload", HttpStatus.CONFLICT),
    COURSE_LOCKED_BY_ADMIN(1209, "Course is suspended and cannot be edited", HttpStatus.FORBIDDEN),
    CATEGORY_NOT_FOUND(1210, "Category not found", HttpStatus.NOT_FOUND),
    COURSE_NOT_PUBLISHED(1211, "Course is not published", HttpStatus.BAD_REQUEST),
    LECTURER_CANNOT_ENROLL_OWN_COURSE(1212, "Lecturer cannot enroll in their own course", HttpStatus.BAD_REQUEST),
    ALREADY_ENROLLED(1213, "Already enrolled in this course", HttpStatus.CONFLICT),
    ENROLLMENT_NOT_FOUND(1214, "Enrollment not found", HttpStatus.NOT_FOUND),

    // Curriculum (1221 - 1235)
    SECTION_NOT_FOUND(1221, "Section not found", HttpStatus.NOT_FOUND),
    SECTION_NOT_IN_COURSE(1222, "Section does not belong to this course", HttpStatus.BAD_REQUEST),
    LESSON_NOT_FOUND(1231, "Lesson not found", HttpStatus.NOT_FOUND),
    LESSON_NOT_IN_COURSE(1232, "Lesson does not belong to this course", HttpStatus.BAD_REQUEST),
    LESSON_CONTENT_TYPE_MISMATCH(1233, "Operation not allowed for this lesson content type", HttpStatus.BAD_REQUEST),
    ORDER_PAYLOAD_MISMATCH(1234, "Reorder payload does not match current items", HttpStatus.BAD_REQUEST),
    LESSON_RESOURCE_NOT_FOUND(1235, "Lesson resource not found", HttpStatus.NOT_FOUND),

    // Storage & Upload (1241 - 1245)
    UPLOAD_FILE_TOO_LARGE(1241, "File exceeds the maximum allowed size", HttpStatus.PAYLOAD_TOO_LARGE),
    UPLOAD_UNSUPPORTED_MEDIA_TYPE(1242, "Unsupported file type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    UPLOAD_OBJECT_NOT_FOUND(1243, "Uploaded object not found in storage", HttpStatus.BAD_REQUEST),
    UPLOAD_METADATA_MISMATCH(1244, "Uploaded file does not match declared metadata", HttpStatus.BAD_REQUEST),
    STORAGE_UNAVAILABLE(1245, "Storage service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatusCode httpStatusCode;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}
