package com.learnova.elearning.module.course.entity.enums;

/**
 * Vòng đời khóa học.
 * <p>
 * PENDING_REVIEW / APPROVED / REJECTED được giữ chỗ cho quy trình kiểm duyệt
 * (ngoài phạm vi hiện tại theo SRS) nhưng khai báo sẵn để CHECK constraint và
 * enum khớp nhau, tránh phải sửa DB về sau.
 */
public enum CourseStatus {
    DRAFT,
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    PUBLISHED,
    UNPUBLISHED,
    SUSPENDED,
    ARCHIVED
}
