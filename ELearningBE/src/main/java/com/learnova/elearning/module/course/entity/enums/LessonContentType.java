package com.learnova.elearning.module.course.entity.enums;

/**
 * Loại nội dung của một curriculum item. QUIZ là item giữ chỗ trong section,
 * nội dung chi tiết nằm ở bảng quizzes (1–1 qua lesson_id).
 */
public enum LessonContentType {
    VIDEO,
    ARTICLE,
    FILE,
    QUIZ
}
