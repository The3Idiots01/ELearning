-- Migration V7: Ensure missing course columns exist in PostgreSQL
ALTER TABLE courses ADD COLUMN IF NOT EXISTS thumbnail_key VARCHAR(500);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS promo_video_key VARCHAR(500);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS subtitle VARCHAR(255);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;


