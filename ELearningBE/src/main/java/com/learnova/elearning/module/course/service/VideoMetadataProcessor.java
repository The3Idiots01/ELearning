package com.learnova.elearning.module.course.service;

import com.learnova.elearning.integration.storage.VideoDurationProbe;
import com.learnova.elearning.module.course.event.LessonContentAttachedEvent;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.services.s3.model.S3Exception;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class VideoMetadataProcessor {
    private final VideoDurationProbe probe;
    private final VideoMetadataJobs jobs;
    private final LessonRepository legacyLessonRepository;
    private final ExecutorService workers = Executors.newFixedThreadPool(2);
    private final ExecutorService probes = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicInteger active = new AtomicInteger();
    public VideoMetadataProcessor(VideoDurationProbe probe, VideoMetadataJobs jobs, LessonRepository legacyLessonRepository) {
        this.probe = probe; this.jobs = jobs; this.legacyLessonRepository = legacyLessonRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onContentAttached(LessonContentAttachedEvent event) {
        // Backward-compatible path for isolated unit tests created before the
        // durable queue was introduced. Production always has VideoMetadataJobs.
        if (jobs == null) {
            try { legacyLessonRepository.markReady(event.lessonId(), probe.probeSeconds(event.storageKey())); }
            catch (Exception ignored) { legacyLessonRepository.markFailed(event.lessonId()); }
            return;
        }
        jobs.enqueue(event.lessonId(), event.storageKey());
    }

    @Scheduled(fixedDelay = 5000)
    public void poll() {
        jobs.recover();
        while (active.get() < 2) {
            VideoMetadataJobs.Job job = jobs.claim();
            if (job == null) break;
            active.incrementAndGet();
            workers.submit(() -> { try { process(job); } finally { active.decrementAndGet(); } });
        }
    }

    void process(VideoMetadataJobs.Job job) {
        long started = System.nanoTime();
        Future<Integer> result = probes.submit(() -> probe.probeSeconds(job.storageKey()));
        try {
            int seconds = result.get(60, TimeUnit.SECONDS);
            if (seconds <= 0) throw new AppException(ErrorCode.UPLOAD_METADATA_MISMATCH);
            jobs.finish(job, seconds, null, false);
            log.info("Video metadata lesson={} attempt={} duration={} elapsedMs={}", job.lessonId(), job.attempts(),
                    seconds, TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started));
        } catch (Exception failure) {
            result.cancel(true);
            Throwable cause = failure instanceof ExecutionException ? failure.getCause() : failure;
            boolean missing = cause instanceof S3Exception s3 && s3.statusCode() == 404
                    || cause instanceof AppException app && app.getErrorCode() == ErrorCode.UPLOAD_OBJECT_NOT_FOUND;
            boolean invalid = cause instanceof AppException app && app.getErrorCode() == ErrorCode.UPLOAD_METADATA_MISMATCH;
            String code = missing ? "VIDEO_OBJECT_MISSING" : invalid ? "VIDEO_INVALID_METADATA"
                    : cause instanceof TimeoutException ? "VIDEO_PROCESSING_TIMEOUT" : "VIDEO_STORAGE_UNAVAILABLE";
            jobs.finish(job, null, code, !missing && !invalid);
            log.warn("Video metadata lesson={} attempt={} error={} elapsedMs={}", job.lessonId(), job.attempts(), code,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started));
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void close() { workers.shutdownNow(); probes.shutdownNow(); }
}
