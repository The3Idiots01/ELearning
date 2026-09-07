package com.learnova.elearning.module.enrollment.repository;

import com.learnova.elearning.module.enrollment.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    boolean existsByEnrollment_IdAndLesson_Id(Long enrollmentId, Long lessonId);

    long countByEnrollment_Id(Long enrollmentId);

    List<LessonProgress> findByEnrollment_Id(Long enrollmentId);

    Optional<LessonProgress> findByEnrollment_IdAndLesson_Id(Long enrollmentId, Long lessonId);

    /**
     * Merge nguyên tử một heartbeat — §5.5 design_us15_us17.md. {@code +} trên
     * multirange là phép hợp (giao hoán, luỹ đẳng, đơn điệu): gộp khoảng chồng
     * lấn/liền kề, không đếm trùng, không cần khoá hay optimistic lock.
     * {@code GREATEST} giữ {@code max_position_seconds} đơn điệu tăng — tua
     * ngược không làm tụt điều kiện hoàn thành. Không có mệnh đề {@code WHERE}
     * bảo vệ độ dài như phương án bitmap vì multirange không quan tâm độ dài.
     */
    @Query(value = """
            INSERT INTO lesson_progress (
                enrollment_id, lesson_id, watched_ranges,
                last_position_seconds, max_position_seconds, first_started_at, updated_at)
            VALUES (
                :enrollmentId, :lessonId, CAST(:ranges AS int4multirange),
                :position, :position, now(), now())
            ON CONFLICT (enrollment_id, lesson_id) DO UPDATE SET
                watched_ranges        = lesson_progress.watched_ranges + EXCLUDED.watched_ranges,
                last_position_seconds = EXCLUDED.last_position_seconds,
                max_position_seconds  = GREATEST(lesson_progress.max_position_seconds,
                                                 EXCLUDED.max_position_seconds),
                updated_at            = now()
            RETURNING id AS id,
                      max_position_seconds AS maxPositionSeconds,
                      (SELECT COALESCE(SUM(upper(r) - lower(r)), 0)::int
                         FROM unnest(watched_ranges) r) AS watchedSeconds
            """, nativeQuery = true)
    WatchedRangesMergeResult mergeWatchedRanges(
            @Param("enrollmentId") Long enrollmentId,
            @Param("lessonId") Long lessonId,
            @Param("ranges") String ranges,
            @Param("position") int position);

    /**
     * Xem trước số giây coverage MỚI mà {@code ranges} sẽ thêm vào nếu merge —
     * hiệu (phép {@code -}) giữa ranges đề nghị và {@code watched_ranges} hiện
     * có, KHÔNG ghi gì (§5.6). Dùng để quyết định có cần cắt bớt trước khi merge
     * thật, vì phép hợp {@code +} một khi đã ghi là không lùi lại được.
     */
    @Query(value = """
            SELECT COALESCE(SUM(upper(r) - lower(r)), 0)::int
            FROM unnest(
                CAST(:ranges AS int4multirange) - COALESCE(
                    (SELECT watched_ranges FROM lesson_progress
                      WHERE enrollment_id = :enrollmentId AND lesson_id = :lessonId),
                    '{}'::int4multirange)
            ) r
            """, nativeQuery = true)
    int previewNewWatchedSeconds(
            @Param("enrollmentId") Long enrollmentId,
            @Param("lessonId") Long lessonId,
            @Param("ranges") String ranges);

    /** Vật chất hoá coverage_percent từ watched_seconds vừa RETURNING (cùng transaction) — §5.5. */
    @Modifying
    @Query(value = """
            UPDATE lesson_progress
            SET watched_seconds  = :watchedSeconds,
                coverage_percent = LEAST(100.00, ROUND(:watchedSeconds * 100.0 / NULLIF(:durationSeconds, 0), 2))
            WHERE id = :id
            """, nativeQuery = true)
    void materializeCoverage(
            @Param("id") Long id,
            @Param("watchedSeconds") int watchedSeconds,
            @Param("durationSeconds") int durationSeconds);
}
