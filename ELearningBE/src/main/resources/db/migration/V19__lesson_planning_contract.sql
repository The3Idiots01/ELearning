-- =============================================================================
-- V19 — Lesson planning contract
--
-- Application code now creates a lesson plan without content and stores quiz
-- assessments independently. The old quiz lesson is retained only as a
-- migration bridge, so remove it after preserving assessment/progress data.
-- =============================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM quizzes WHERE assessment_id IS NULL) THEN
        RAISE EXCEPTION
            'Lesson contract migration aborted: quizzes without assessment_id remain';
    END IF;
END $$;

ALTER TABLE quizzes
    ALTER COLUMN assessment_id SET NOT NULL;

-- The legacy column is non-null and still has the old QUIZ check constraint;
-- relax both before converting quiz lesson rows into archived plan records.
ALTER TABLE lessons ALTER COLUMN content_type DROP NOT NULL;
ALTER TABLE lessons DROP CONSTRAINT IF EXISTS ck_lessons_type;

-- Keep the legacy rows for audit/history, but remove them from the active
-- curriculum. Attempts and assessment_progress are owned by the assessment.
UPDATE lessons l
SET deleted_at = COALESCE(l.deleted_at, now()),
    content_type = NULL,
    storage_key = NULL,
    original_file_name = NULL,
    file_size_bytes = NULL,
    mime_type = NULL,
    content_text = NULL,
    duration_seconds = 0,
    upload_status = 'EMPTY'
WHERE l.content_type = 'QUIZ'
;

ALTER TABLE quizzes DROP CONSTRAINT IF EXISTS fk_quizzes_lesson;
ALTER TABLE quizzes DROP CONSTRAINT IF EXISTS uq_quizzes_lesson;
DROP INDEX IF EXISTS idx_quizzes_lesson;
ALTER TABLE quizzes DROP COLUMN IF EXISTS lesson_id;

ALTER TABLE lessons ALTER COLUMN content_type DROP NOT NULL;
ALTER TABLE lessons
    ADD CONSTRAINT ck_lessons_type
        CHECK (content_type IS NULL OR content_type IN ('VIDEO', 'ARTICLE', 'FILE'));

-- Public and instructor course responses now read objectives from
-- learning_outcomes. Requirements and audiences stay in course_bullets.
DELETE FROM course_bullets
WHERE bullet_type = 'LEARNING_OBJECTIVE';
