package com.learnova.elearning.integration.storage.model;

import java.util.Set;

/**
 * Mục đích upload — quyết định thư mục lưu (keySegment), loại file cho phép và
 * dung lượng tối đa. allowedContentTypes rỗng = chấp nhận mọi loại.
 * <p>
 * BR-05: video mp4, tối đa 500MB.
 */
public enum UploadPurpose {

    COURSE_THUMBNAIL(
            "thumbnail",
            false,
            Set.of("image/jpeg", "image/png", "image/webp"),
            5L * 1024 * 1024),

    COURSE_PROMO_VIDEO(
            "promo",
            false,
            Set.of("video/mp4"),
            500L * 1024 * 1024),

    LESSON_VIDEO(
            "video",
            true,
            Set.of("video/mp4"),
            500L * 1024 * 1024),

    LESSON_FILE(
            "file",
            true,
            Set.of("application/pdf", "application/zip",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            200L * 1024 * 1024),

    LESSON_RESOURCE(
            "resources",
            true,
            Set.of(),
            100L * 1024 * 1024);

    private final String keySegment;
    private final boolean lessonScoped;
    private final Set<String> allowedContentTypes;
    private final long maxSizeBytes;

    UploadPurpose(String keySegment, boolean lessonScoped,
                  Set<String> allowedContentTypes, long maxSizeBytes) {
        this.keySegment = keySegment;
        this.lessonScoped = lessonScoped;
        this.allowedContentTypes = allowedContentTypes;
        this.maxSizeBytes = maxSizeBytes;
    }

    public String keySegment() {
        return keySegment;
    }

    /** true nếu key cần lessonId (LESSON_*), false nếu chỉ cần courseId (COURSE_*). */
    public boolean isLessonScoped() {
        return lessonScoped;
    }

    public long maxSizeBytes() {
        return maxSizeBytes;
    }

    public boolean isContentTypeAllowed(String contentType) {
        return allowedContentTypes.isEmpty()
                || (contentType != null && allowedContentTypes.contains(contentType.toLowerCase()));
    }
}
