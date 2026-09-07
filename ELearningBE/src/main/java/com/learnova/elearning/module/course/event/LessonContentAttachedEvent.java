package com.learnova.elearning.module.course.event;

/**
 * Phát khi một object VIDEO vừa được gắn vào lesson (đã {@code READY} trên
 * storage) và cần đo duration thật ở job nền — §7.5 design_us15_us17.md.
 */
public record LessonContentAttachedEvent(Long lessonId, String storageKey) {
}
