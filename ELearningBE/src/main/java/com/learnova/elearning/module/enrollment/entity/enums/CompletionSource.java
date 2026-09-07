package com.learnova.elearning.module.enrollment.entity.enums;

/**
 * Cơ sở đánh dấu hoàn thành một lesson — §5.7 design_us15_us17.md. Đi cùng
 * {@code completed_at} theo cặp (CHECK {@code ck_lesson_progress_completion_pair}).
 */
public enum CompletionSource {
    /** VIDEO — max_position_seconds vượt ngưỡng cấu hình (§5.7). */
    POSITION,
    /** Người dùng bấm nút "Đánh dấu hoàn thành" — áp dụng cho mọi contentType kể cả VIDEO. */
    MANUAL,
    /** QUIZ đạt điểm sàn — ngoài phạm vi Sprint 3. */
    QUIZ
}
