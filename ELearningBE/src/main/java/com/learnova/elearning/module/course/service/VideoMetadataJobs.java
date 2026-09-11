package com.learnova.elearning.module.course.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** SQL queue shares the content transaction; all network I/O stays outside transactions. */
@Service
public class VideoMetadataJobs {
    public record Job(long id, long lessonId, String storageKey, int attempts, UUID token) {}
    public record Status(long lessonId, String uploadStatus, int durationSeconds, Instant updatedAt,
                         String errorCode, boolean canRetry, String pendingUploadStatus) {}
    private record Slot(String liveKey, String pendingKey, String liveStatus, String pendingStatus) {}
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    public VideoMetadataJobs(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(manager);
    }

    public void enqueue(long lessonId, String key) {
        jdbc.update("""
            INSERT INTO video_metadata_jobs (lesson_id, storage_key) VALUES (?, ?)
            ON CONFLICT (lesson_id, storage_key) DO UPDATE SET status='QUEUED', attempts=0,
                next_run_at=now(), lease_token=NULL, lease_until=NULL, error_code=NULL, updated_at=now()
            WHERE video_metadata_jobs.status NOT IN ('QUEUED','RUNNING')
            """, lessonId, key);
    }

    public void cancel(long lessonId, String key) {
        jdbc.update("UPDATE video_metadata_jobs SET status='CANCELLED', lease_token=NULL, lease_until=NULL, updated_at=now() "
                + "WHERE lesson_id=? AND storage_key=? AND status IN ('QUEUED','RUNNING')", lessonId, key);
    }

    public Job claim() {
        return tx.execute(ignored -> {
            List<Long> ids = jdbc.queryForList("""
                SELECT id FROM video_metadata_jobs WHERE status='QUEUED' AND next_run_at <= now()
                ORDER BY next_run_at, id LIMIT 1 FOR UPDATE SKIP LOCKED
                """, Long.class);
            if (ids.isEmpty()) return null;
            UUID token = UUID.randomUUID();
            return jdbc.queryForObject("""
                UPDATE video_metadata_jobs SET status='RUNNING', attempts=attempts+1,
                    lease_token=?, lease_until=now()+interval '90 seconds', updated_at=now()
                WHERE id=? RETURNING id, lesson_id, storage_key, attempts
                """, (rs, row) -> new Job(rs.getLong("id"), rs.getLong("lesson_id"),
                    rs.getString("storage_key"), rs.getInt("attempts"), token), token, ids.getFirst());
        });
    }

    public void finish(Job job, Integer duration, String error, boolean retryable) {
        tx.executeWithoutResult(ignored -> {
            // Same lock order as attach/reprocess: lesson then job. Old file results cannot win.
            Slot slot = jdbc.queryForObject("""
                SELECT storage_key, pending_storage_key, upload_status, pending_upload_status
                FROM lessons WHERE id=? FOR UPDATE
                """, (rs, row) -> new Slot(rs.getString("storage_key"),
                    rs.getString("pending_storage_key"), rs.getString("upload_status"),
                    rs.getString("pending_upload_status")), job.lessonId());
            List<Long> owned = jdbc.queryForList("""
                SELECT id FROM video_metadata_jobs WHERE id=? AND status='RUNNING' AND lease_token=? FOR UPDATE
                """, Long.class, job.id(), job.token());
            if (owned.isEmpty()) return;
            boolean live = slot != null && job.storageKey().equals(slot.liveKey())
                    && "PROCESSING".equals(slot.liveStatus());
            boolean pending = slot != null && job.storageKey().equals(slot.pendingKey())
                    && "PROCESSING".equals(slot.pendingStatus());
            boolean current = (live || pending) && Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT deleted_at IS NULL AND content_type='VIDEO' FROM lessons WHERE id=?
                    """, Boolean.class, job.lessonId()));
            String state = !current ? "CANCELLED" : duration != null ? "SUCCEEDED"
                    : retryable && job.attempts() < 3 ? "QUEUED" : "FAILED";
            int delay = job.attempts() == 1 ? 5 : 20;
            jdbc.update("""
                UPDATE video_metadata_jobs SET status=?, error_code=?, lease_token=NULL, lease_until=NULL,
                    next_run_at=now() + (? * interval '1 second'), updated_at=now() WHERE id=?
                """, state, error, delay, job.id());
            if (state.equals("SUCCEEDED") || state.equals("FAILED")) {
                if (pending) {
                    jdbc.update("""
                        UPDATE lessons SET pending_upload_status=?, pending_duration_seconds=?, updated_at=now()
                        WHERE id=? AND pending_storage_key=? AND deleted_at IS NULL AND content_type='VIDEO'
                        """, duration != null ? "READY" : "FAILED", duration != null ? duration : 0,
                            job.lessonId(), job.storageKey());
                } else if (live) {
                    jdbc.update("""
                        UPDATE lessons SET upload_status=?, duration_seconds=?, updated_at=now()
                        WHERE id=? AND storage_key=? AND deleted_at IS NULL AND content_type='VIDEO'
                        """, duration != null ? "READY" : "FAILED", duration != null ? duration : 0,
                            job.lessonId(), job.storageKey());
                }
            }
        });
    }

    public void recover() {
        List<Job> expired = jdbc.query("""
            SELECT id, lesson_id, storage_key, attempts, lease_token FROM video_metadata_jobs
            WHERE status='RUNNING' AND lease_until < now() LIMIT 100
            """, (rs, row) -> new Job(rs.getLong("id"), rs.getLong("lesson_id"), rs.getString("storage_key"),
                rs.getInt("attempts"), rs.getObject("lease_token", UUID.class)));
        for (Job job : expired) finish(job, null, "VIDEO_PROCESSING_TIMEOUT", true);
        jdbc.update("""
            INSERT INTO video_metadata_jobs (lesson_id, storage_key)
            SELECT id, storage_key FROM lessons WHERE deleted_at IS NULL AND content_type='VIDEO'
                AND upload_status='PROCESSING' AND storage_key IS NOT NULL
            UNION ALL
            SELECT id, pending_storage_key FROM lessons WHERE deleted_at IS NULL AND content_type='VIDEO'
                AND pending_upload_status='PROCESSING' AND pending_storage_key IS NOT NULL
            ON CONFLICT (lesson_id, storage_key) DO NOTHING
            """);
    }

    public List<Status> statuses(long courseId) {
        return jdbc.query("""
            SELECT l.id, l.upload_status, l.duration_seconds, COALESCE(j.updated_at,l.updated_at,l.created_at) AS updated_at,
                j.error_code, l.pending_upload_status FROM lessons l JOIN course_sections s ON s.id=l.section_id
                LEFT JOIN video_metadata_jobs j ON j.lesson_id=l.id AND j.storage_key=l.storage_key
            WHERE s.course_id=? AND s.deleted_at IS NULL AND l.deleted_at IS NULL AND l.content_type='VIDEO'
            ORDER BY l.id
            """, (rs, row) -> new Status(rs.getLong("id"), rs.getString("upload_status"), rs.getInt("duration_seconds"),
                rs.getTimestamp("updated_at").toInstant(), rs.getString("error_code"),
                "FAILED".equals(rs.getString("upload_status")), rs.getString("pending_upload_status")), courseId);
    }
}
