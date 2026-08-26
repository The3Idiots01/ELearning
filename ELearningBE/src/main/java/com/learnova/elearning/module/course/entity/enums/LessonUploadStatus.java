package com.learnova.elearning.module.course.entity.enums;

/**
 * Trạng thái file của một lesson (áp dụng cho VIDEO/FILE):
 * <ul>
 *   <li>{@link #EMPTY} — chưa gắn file (mặc định, cả với ARTICLE/QUIZ)</li>
 *   <li>{@link #PENDING} — đã ký presigned URL, đang chờ FE upload xong</li>
 *   <li>{@link #READY} — đã xác nhận object tồn tại trên storage</li>
 *   <li>{@link #FAILED} — xác nhận thất bại (object không thấy / sai metadata)</li>
 * </ul>
 */
public enum LessonUploadStatus {
    EMPTY,
    PENDING,
    READY,
    FAILED
}
