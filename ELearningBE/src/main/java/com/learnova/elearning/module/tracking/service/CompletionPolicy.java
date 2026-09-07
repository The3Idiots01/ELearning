package com.learnova.elearning.module.tracking.service;

import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.enrollment.entity.LessonProgress;

/**
 * Điều kiện tự động hoàn thành một lesson — §5.7 design_us15_us17.md. Chỉ xét
 * hoàn thành theo tín hiệu tự động (vị trí xem); hoàn thành thủ công (nút tick)
 * đi thẳng qua {@code EnrollmentService.completeLesson}, không qua policy này.
 */
public interface CompletionPolicy {

    boolean isComplete(Lesson lesson, LessonProgress progress);
}
