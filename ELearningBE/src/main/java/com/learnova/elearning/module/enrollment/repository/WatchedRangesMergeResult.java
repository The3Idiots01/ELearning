package com.learnova.elearning.module.enrollment.repository;

/**
 * Projection cho kết quả {@code RETURNING} của câu merge nguyên tử watched_ranges
 * (§5.5 design_us15_us17.md). Tên getter khớp alias cột trong native query.
 */
public interface WatchedRangesMergeResult {
    Long getId();
    Integer getMaxPositionSeconds();
    Integer getWatchedSeconds();
}
