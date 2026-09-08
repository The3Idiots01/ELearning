package com.learnova.elearning.module.enrollment.repository;

import com.learnova.elearning.module.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudent_IdAndCourse_Id(Long studentId, Long courseId);

    Optional<Enrollment> findByStudent_IdAndCourse_Id(Long studentId, Long courseId);

    java.util.List<Enrollment> findByStudent_IdOrderByEnrolledAtDesc(Long studentId);

    boolean existsByCourse_Id(Long courseId);

    /**
     * Tính lại {@code progress} / {@code status} / {@code completed_at} của một
     * enrollment từ trạng thái hoàn thành thật của lessons và assessments — §9
     * design_us15_us17.md. {@code status} và {@code completed_at} là MỘT
     * CHIỀU: câu lệnh chỉ nâng lên {@code COMPLETED}, không bao giờ hạ xuống
     * {@code ACTIVE} dù % tụt vì course có thêm lesson mới. Chỉ gọi khi một
     * lesson vừa chuyển sang hoàn thành — không gọi ở mọi heartbeat.
     * <p>
     * {@code clearAutomatically = true}: câu lệnh là native UPDATE, không đi
     * qua persistence context — phải xoá cache Hibernate để lần đọc {@link
     * Enrollment} kế tiếp trong cùng transaction lấy giá trị mới từ DB thay vì
     * bản ghi cũ đang cache theo id.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE enrollments e
            SET progress = CASE WHEN c.total_units = 0 THEN 0
                                ELSE ROUND(100.0 * c.completed_units / c.total_units, 2) END,
                status = CASE WHEN (c.total_units > 0 AND c.completed_units = c.total_units)
                                   OR e.status = 'COMPLETED'
                              THEN 'COMPLETED'
                              WHEN e.status = 'CANCELLED' THEN 'CANCELLED'
                              ELSE 'ACTIVE' END,
                completed_at = COALESCE(e.completed_at,
                                        CASE WHEN c.total_units > 0
                                                  AND c.completed_units = c.total_units
                                             THEN now() END)
            FROM (
                SELECT
                    (SELECT COUNT(*)
                       FROM lessons l
                       JOIN course_sections s ON s.id = l.section_id
                      WHERE s.course_id = :courseId AND l.deleted_at IS NULL)
                    +
                    (SELECT COUNT(*)
                       FROM assessments a
                       JOIN course_sections s ON s.id = a.section_id
                      WHERE s.course_id = :courseId
                        AND a.section_id IS NOT NULL
                        AND a.deleted_at IS NULL) AS total_units,
                    (SELECT COUNT(*)
                       FROM lessons l
                       JOIN course_sections s ON s.id = l.section_id
                       JOIN lesson_progress lp
                         ON lp.lesson_id = l.id
                        AND lp.enrollment_id = :enrollmentId
                      WHERE s.course_id = :courseId
                        AND l.deleted_at IS NULL
                        AND lp.completed_at IS NOT NULL)
                    +
                    (SELECT COUNT(*)
                       FROM assessments a
                       JOIN course_sections s ON s.id = a.section_id
                       JOIN assessment_progress ap
                         ON ap.assessment_id = a.id
                        AND ap.enrollment_id = :enrollmentId
                      WHERE s.course_id = :courseId
                        AND a.section_id IS NOT NULL
                        AND a.deleted_at IS NULL
                        AND ap.completed_at IS NOT NULL) AS completed_units
            ) c
            WHERE e.id = :enrollmentId
            """, nativeQuery = true)
    void recalculateCourseProgress(@Param("enrollmentId") Long enrollmentId, @Param("courseId") Long courseId);
}
