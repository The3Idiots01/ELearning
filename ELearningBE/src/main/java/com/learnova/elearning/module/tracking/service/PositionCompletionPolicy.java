package com.learnova.elearning.module.tracking.service;

import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.enrollment.entity.LessonProgress;
import com.learnova.elearning.module.tracking.config.TrackingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * VIDEO hoàn thành theo vị trí xa nhất đã tới, KHÔNG theo coverage (§5.1,
 * §5.7). Các contentType khác (ARTICLE, FILE) chỉ hoàn thành thủ công /
 * qua module khác — policy này luôn trả {@code false} cho chúng.
 */
@Component
@RequiredArgsConstructor
public class PositionCompletionPolicy implements CompletionPolicy {

    private final TrackingProperties trackingProperties;

    @Override
    public boolean isComplete(Lesson lesson, LessonProgress progress) {
        if (lesson.getContentType() != LessonContentType.VIDEO) {
            return false;
        }
        int durationSeconds = lesson.getDurationSeconds() != null ? lesson.getDurationSeconds() : 0;
        if (durationSeconds <= 0) {
            return false;
        }
        int maxPosition = progress.getMaxPositionSeconds() != null ? progress.getMaxPositionSeconds() : 0;
        double threshold = durationSeconds * trackingProperties.getVideoCompletionRatio();
        return maxPosition >= threshold;
    }
}
