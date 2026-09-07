package com.learnova.elearning.module.course.service;

import com.learnova.elearning.integration.storage.VideoDurationProbe;
import com.learnova.elearning.module.course.dto.response.VideoDurationBackfillResponse;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import com.learnova.elearning.module.course.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Job chạy một lần, do admin kích hoạt qua endpoint — đo lại duration thật cho
 * lesson VIDEO đã {@code READY} từ trước khi có {@link VideoDurationProbe}
 * (kể cả seed {@code V8__seed_published_courses.sql}). §7.5, Task 0c.
 * <p>
 * Bắt buộc chạy trước migration V9 (Task 1) — backfill coverage của V9 dựa vào
 * {@code duration_seconds} đáng tin.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoDurationBackfillService {

    private final LessonRepository lessonRepository;
    private final VideoDurationProbe durationProbe;

    @Transactional
    public VideoDurationBackfillResponse backfill() {
        List<Lesson> lessons = lessonRepository.findByContentTypeAndUploadStatus(
                LessonContentType.VIDEO, LessonUploadStatus.READY);

        int updated = 0;
        int unchanged = 0;
        List<Long> failedIds = new ArrayList<>();

        for (Lesson lesson : lessons) {
            if (lesson.getStorageKey() == null) {
                continue;
            }
            try {
                int probedSeconds = durationProbe.probeSeconds(lesson.getStorageKey());
                int previousSeconds = lesson.getDurationSeconds() != null ? lesson.getDurationSeconds() : 0;
                if (previousSeconds != probedSeconds) {
                    lesson.setDurationSeconds(probedSeconds);
                    lessonRepository.save(lesson);
                    updated++;
                } else {
                    unchanged++;
                }
            } catch (Exception e) {
                log.warn("Duration backfill failed for lesson {}: {}", lesson.getId(), e.getMessage());
                failedIds.add(lesson.getId());
            }
        }

        return VideoDurationBackfillResponse.builder()
                .scanned(lessons.size())
                .updated(updated)
                .unchanged(unchanged)
                .failed(failedIds.size())
                .failedLessonIds(failedIds)
                .build();
    }
}
