-- =============================================================================
-- V17 — Cập nhật ràng buộc ck_lessons_type cho phép kiểu bài học 'QUIZ'
-- =============================================================================

ALTER TABLE lessons DROP CONSTRAINT IF EXISTS ck_lessons_type;

ALTER TABLE lessons
    ADD CONSTRAINT ck_lessons_type
        CHECK (content_type IN ('VIDEO', 'ARTICLE', 'FILE', 'QUIZ'));
