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
    PAYMENT_REQUIRED(1215, "Payment is required for this course before enrollment", HttpStatus.PAYMENT_REQUIRED),
    PAYMENT_GATEWAY_ERROR(1216, "Error connecting to payment gateway", HttpStatus.BAD_GATEWAY),
    INVALID_PAYMENT_SIGNATURE(1217, "Invalid payment webhook signature", HttpStatus.BAD_REQUEST),

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
    STORAGE_UNAVAILABLE(1245, "Storage service is unavailable", HttpStatus.SERVICE_UNAVAILABLE),

    // Content Delivery (1251 - 1257)
    PLAYBACK_TICKET_INVALID(1251, "Playback ticket is invalid or malformed", HttpStatus.UNAUTHORIZED),
    PLAYBACK_TICKET_EXPIRED(1252, "Playback ticket has expired", HttpStatus.UNAUTHORIZED),
    PLAYBACK_TICKET_MISMATCH(1253, "Playback ticket does not belong to this session", HttpStatus.FORBIDDEN),
    CONTENT_ACCESS_DENIED(1254, "You must be enrolled to access this content", HttpStatus.FORBIDDEN),
    LESSON_CONTENT_NOT_READY(1255, "Lesson content is not ready for playback", HttpStatus.CONFLICT),
    TOO_MANY_ACTIVE_STREAMS(1256, "Too many concurrent playback sessions", HttpStatus.TOO_MANY_REQUESTS),
    CONTENT_RANGE_NOT_SATISFIABLE(1257, "Requested range not satisfiable", HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE),

    // Knowledge Tracking (1261 - 1265)
    PROGRESS_RATE_LIMITED(1261, "Progress updates are being sent too fast", HttpStatus.TOO_MANY_REQUESTS),

    // Backward Design - Learning Outcomes (1271 - 1273)
    LEARNING_OUTCOME_NOT_FOUND(1271, "Learning outcome not found", HttpStatus.NOT_FOUND),
    OUTCOME_NOT_IN_COURSE(1272, "Learning outcome does not belong to this course", HttpStatus.BAD_REQUEST),
    LEARNING_OUTCOME_IN_USE(1273, "Learning outcome is still used by a lesson or assessment", HttpStatus.CONFLICT),

    // Backward Design - Assessments (1281 - 1284)
    ASSESSMENT_NOT_FOUND(1281, "Assessment not found", HttpStatus.NOT_FOUND),
    ASSESSMENT_NOT_IN_COURSE(1282, "Assessment does not belong to this course", HttpStatus.BAD_REQUEST),
    ASSESSMENT_TYPE_UNSUPPORTED(1283, "Assessment type is not supported", HttpStatus.BAD_REQUEST),
    ASSESSMENT_OUTCOME_MISMATCH(1284, "Assessment outcomes must belong to this course", HttpStatus.BAD_REQUEST),

    // Quiz Domain (1301 - 1310)
    QUIZ_NOT_FOUND(1301, "Quiz not found", HttpStatus.NOT_FOUND),
    QUIZ_QUESTION_NOT_FOUND(1302, "Quiz question not found", HttpStatus.NOT_FOUND),
    QUIZ_MAX_ATTEMPTS_REACHED(1303, "Maximum quiz attempts reached", HttpStatus.BAD_REQUEST),
    QUESTION_NOT_IN_QUIZ(1304, "Question does not belong to this quiz", HttpStatus.BAD_REQUEST),
    QUIZ_ALREADY_EXISTS(1305, "Quiz already exists for this assessment", HttpStatus.CONFLICT),
    LESSON_NOT_A_QUIZ(1306, "Lesson is not configured as a quiz", HttpStatus.BAD_REQUEST),
    QUIZ_QUESTION_INVALID_OPTIONS(1307, "Question must have at least one correct option", HttpStatus.BAD_REQUEST),
    QUIZ_SINGLE_CHOICE_MULTIPLE_CORRECT(1308, "Single choice question cannot have more than one correct option", HttpStatus.BAD_REQUEST),
    QUIZ_WITHOUT_QUESTION(1310, "Quiz must contain at least one question", HttpStatus.BAD_REQUEST),

    // Q&A Domain (1321 - 1325)
    QUESTION_NOT_FOUND(1321, "Question not found", HttpStatus.NOT_FOUND),
    NOT_ENROLLED(1322, "Only enrolled students or course lecturer can participate in Q&A", HttpStatus.FORBIDDEN),
    QUESTION_ACCESS_DENIED(1323, "You do not have permission to modify this thread", HttpStatus.FORBIDDEN),
    QA_CONTENT_VIOLATION(1324, "Nội dung câu hỏi hoặc câu trả lời chứa từ ngữ không phù hợp hoặc vi phạm tiêu chuẩn cộng đồng. Vui lòng chỉnh sửa lại.", HttpStatus.BAD_REQUEST),

    // Course Review Domain (1331 - 1336)
    REVIEW_NOT_FOUND(1331, "Review not found", HttpStatus.NOT_FOUND),
    REVIEW_ACCESS_DENIED(1332, "You do not have permission to modify this review", HttpStatus.FORBIDDEN),
    REVIEW_NOT_ENROLLED(1333, "Chỉ học viên đã đăng ký khóa học mới có thể đánh giá", HttpStatus.FORBIDDEN),
    REVIEW_CONTENT_VIOLATION(1334, "Nội dung đánh giá chứa từ ngữ chưa phù hợp hoặc vi phạm tiêu chuẩn cộng đồng. Vui lòng chỉnh sửa lại.", HttpStatus.BAD_REQUEST),
    REVIEW_INVALID_RATING(1335, "Số sao đánh giá phải từ 1 đến 5", HttpStatus.BAD_REQUEST),
    REVIEW_PROGRESS_INSUFFICIENT(1336, "Bạn cần hoàn thành ít nhất 20% khóa học để có thể gửi đánh giá", HttpStatus.FORBIDDEN);

    private final int code;
    private final String message;
    private final HttpStatusCode httpStatusCode;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}