package com.learnova.elearning.module.course.service;

import com.learnova.elearning.integration.storage.VideoDurationProbe;
import com.learnova.elearning.module.course.event.LessonContentAttachedEvent;
import com.learnova.elearning.module.course.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Job nền đo duration thật cho lesson VIDEO vừa upload — §7.5 design_us15_us17.md.
 * Chạy sau khi transaction gắn nội dung đã commit ({@link TransactionPhase#AFTER_COMMIT}),
 * để không đánh dấu READY cho một dòng chưa thật sự tồn tại ở transaction khác.
 * Chạy bất đồng bộ ({@link Async}) để không chặn request của instructor — file MP4
 * không "faststart" có thể mất hàng chục giây mới đọc được box mvhd.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoMetadataProcessor {

    private final VideoDurationProbe durationProbe;
    private final LessonRepository lessonRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContentAttached(LessonContentAttachedEvent event) {
        try {
            int seconds = durationProbe.probeSeconds(event.storageKey());
            lessonRepository.markReady(event.lessonId(), seconds);
            log.info("Video duration probed for lesson {}: {}s", event.lessonId(), seconds);
        } catch (Exception e) {
            log.warn("Duration probe failed for lesson {}: {}", event.lessonId(), e.getMessage());
            lessonRepository.markFailed(event.lessonId());
        }
    }
}
