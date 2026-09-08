-- Recalculate the denormalized enrollment progress after assessment_progress
-- was backfilled by V17. A course unit is an active lesson or a placed,
-- non-deleted assessment.
WITH rollup AS (
    SELECT
        e.id AS enrollment_id,
        (
            (SELECT COUNT(*)
               FROM lessons l
              JOIN course_sections s ON s.id = l.section_id
              WHERE s.course_id = e.course_id AND l.deleted_at IS NULL)
            +
            (SELECT COUNT(*)
               FROM assessments a
              WHERE a.course_id = e.course_id
                AND a.section_id IS NOT NULL
                AND a.deleted_at IS NULL)
        ) AS total_units,
        (
            (SELECT COUNT(*)
               FROM lessons l
              JOIN course_sections s ON s.id = l.section_id
              JOIN lesson_progress lp
                ON lp.lesson_id = l.id
               AND lp.enrollment_id = e.id
              WHERE s.course_id = e.course_id
                AND l.deleted_at IS NULL
                AND lp.completed_at IS NOT NULL)
            +
            (SELECT COUNT(*)
               FROM assessments a
              JOIN assessment_progress ap
                ON ap.assessment_id = a.id
               AND ap.enrollment_id = e.id
              WHERE a.course_id = e.course_id
                AND a.section_id IS NOT NULL
                AND a.deleted_at IS NULL
                AND ap.completed_at IS NOT NULL)
        ) AS completed_units
    FROM enrollments e
)
UPDATE enrollments e
SET progress = CASE WHEN r.total_units = 0 THEN 0
                    ELSE ROUND(100.0 * r.completed_units / r.total_units, 2) END,
    status = CASE WHEN r.total_units > 0 AND r.completed_units = r.total_units
                       OR e.status = 'COMPLETED'
                  THEN 'COMPLETED'
                  WHEN e.status = 'CANCELLED' THEN 'CANCELLED'
                  ELSE 'ACTIVE' END,
    completed_at = COALESCE(e.completed_at,
                            CASE WHEN r.total_units > 0
                                      AND r.completed_units = r.total_units
                                 THEN now() END)
FROM rollup r
WHERE e.id = r.enrollment_id;
