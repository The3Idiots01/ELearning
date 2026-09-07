package com.learnova.elearning.module.course.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Kết quả job backfill duration cho lesson VIDEO đã publish từ trước khi có
 * {@code VideoDurationProbe} (§7.5, Task 0c) — kể cả seed
 * {@code V8__seed_published_courses.sql}.
 */
@Data
@Builder
public class VideoDurationBackfillResponse {

    private int scanned;
    private int updated;
    private int unchanged;
    private int failed;
    private List<Long> failedLessonIds;
}
