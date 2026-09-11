-- Some environments already recorded schema version 21 before the durable
-- video queue migration was available. Keep this migration idempotent so
-- those environments receive the table without touching existing data.
CREATE TABLE IF NOT EXISTS video_metadata_jobs (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id),
    storage_key VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED','RUNNING','SUCCEEDED','FAILED','CANCELLED')),
    attempts INTEGER NOT NULL DEFAULT 0,
    next_run_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    lease_until TIMESTAMPTZ,
    lease_token UUID,
    error_code VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (lesson_id, storage_key)
);

CREATE INDEX IF NOT EXISTS ix_video_metadata_due
    ON video_metadata_jobs (next_run_at) WHERE status = 'QUEUED';
CREATE INDEX IF NOT EXISTS ix_video_metadata_lease
    ON video_metadata_jobs (lease_until) WHERE status = 'RUNNING';

INSERT INTO video_metadata_jobs (lesson_id, storage_key)
SELECT id, storage_key FROM lessons
WHERE deleted_at IS NULL AND content_type = 'VIDEO'
  AND upload_status = 'PROCESSING' AND storage_key IS NOT NULL
ON CONFLICT (lesson_id, storage_key) DO NOTHING;
