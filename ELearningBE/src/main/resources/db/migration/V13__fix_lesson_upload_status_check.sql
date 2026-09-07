-- =============================================================================
-- V13 — Sửa ck_lessons_upload thiếu trạng thái PROCESSING (§7.5 design_us15_us17.md)
--
-- LessonUploadStatus.PROCESSING được thêm ở Task 0b nhưng constraint gốc (V2)
-- chưa từng được cập nhật theo. LessonContentService.attachContent() set
-- upload_status = PROCESSING cho lesson VIDEO ngay khi gắn nội dung, vi phạm
-- constraint cũ -> DataIntegrityViolationException không được map -> 500
-- UNCATEGORIZED_EXCEPTION, transaction rollback (object vẫn còn trên storage
-- nhưng lesson không được gắn nội dung).
-- =============================================================================

ALTER TABLE lessons DROP CONSTRAINT ck_lessons_upload;

ALTER TABLE lessons
    ADD CONSTRAINT ck_lessons_upload
        CHECK (upload_status IN ('EMPTY', 'PENDING', 'PROCESSING', 'READY', 'FAILED'));
