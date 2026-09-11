-- Published course change sets. A lesson/assessment is independently visible
-- so a live course can be edited without changing its learner snapshot.
ALTER TABLE lessons
    ADD COLUMN publication_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN pending_storage_key VARCHAR(500),
    ADD COLUMN pending_original_file_name VARCHAR(255),
    ADD COLUMN pending_file_size_bytes BIGINT,
    ADD COLUMN pending_mime_type VARCHAR(100),
    ADD COLUMN pending_duration_seconds INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN pending_upload_status VARCHAR(20);

ALTER TABLE lessons
    ADD CONSTRAINT ck_lessons_publication_status
        CHECK (publication_status IN ('DRAFT', 'PUBLISHED')),
    ADD CONSTRAINT ck_lessons_pending_upload_status
        CHECK (pending_upload_status IS NULL OR pending_upload_status IN
               ('EMPTY', 'PENDING', 'PROCESSING', 'READY', 'FAILED')),
    ADD CONSTRAINT ck_lessons_pending_duration
        CHECK (pending_duration_seconds >= 0);

ALTER TABLE assessments
    ADD COLUMN publication_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

ALTER TABLE assessments
    ADD CONSTRAINT ck_assessments_publication_status
        CHECK (publication_status IN ('DRAFT', 'PUBLISHED'));

-- published_at is intentionally used instead of status: an UNPUBLISHED course
-- still has a published snapshot and must keep its content published on the
-- next republish.
UPDATE lessons l
SET publication_status = CASE WHEN c.published_at IS NULL THEN 'DRAFT' ELSE 'PUBLISHED' END
FROM course_sections s
JOIN courses c ON c.id = s.course_id
WHERE l.section_id = s.id;

UPDATE assessments a
SET publication_status = CASE WHEN c.published_at IS NULL THEN 'DRAFT' ELSE 'PUBLISHED' END
FROM courses c
WHERE a.course_id = c.id;

CREATE INDEX idx_lessons_publication_status
    ON lessons (section_id, publication_status, position)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_assessments_publication_status
    ON assessments (course_id, publication_status, position)
    WHERE deleted_at IS NULL;

