package com.learnova.elearning.module.course.entity.enums;

/**
 * Trạng thái file của một lesson (áp dụng cho VIDEO/FILE):
 * <ul>
 *   <li>{@link #EMPTY} — chưa gắn file (mặc định, cả với ARTICLE/QUIZ)</li>
 *   <li>{@link #PENDING} — đã ký presigned URL, đang chờ FE upload xong</li>
 *   <li>{@link #PROCESSING} — object VIDEO đã có trên storage, đang đo duration
 *       thật ở job nền ({@code VideoMetadataProcessor}) — §7.5</li>
 *   <li>{@link #READY} — đã xác nhận object tồn tại trên storage (và với VIDEO,
 *       đã biết duration thật)</li>
 *   <li>{@link #FAILED} — xác nhận thất bại (object không thấy / sai metadata /
 *       không đọc được duration)</li>
 * </ul>
 */
public enum LessonUploadStatus {
    EMPTY,
    PENDING,
    PROCESSING,
    READY,
    FAILED
}
