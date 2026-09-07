-- =============================================================================
-- V14 — Xóa dữ liệu mock course seed bởi V8 (101, 102, 103) và mọi thứ instructor
-- có thể đã thêm vào chúng trong lúc dev/test. Xóa theo thứ tự phụ thuộc vì
-- fk_sections_course / fk_lessons_section không có ON DELETE CASCADE.
-- =============================================================================

DELETE FROM lesson_resources
WHERE lesson_id IN (
    SELECT l.id FROM lessons l
    JOIN course_sections s ON s.id = l.section_id
    WHERE s.course_id IN (101, 102, 103)
);

DELETE FROM lessons
WHERE section_id IN (
    SELECT id FROM course_sections WHERE course_id IN (101, 102, 103)
);

DELETE FROM course_sections WHERE course_id IN (101, 102, 103);

-- courses -> cascade course_bullets, course_status_logs, enrollments,
-- payment_orders (và qua đó lesson_progress) nhờ ON DELETE CASCADE sẵn có.
DELETE FROM courses WHERE id IN (101, 102, 103);
