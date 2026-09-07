package com.learnova.elearning.module.delivery.dto;

import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** §4.6 design_us15_us17.md — response của GET .../playback. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackResponse {

    private Long lessonId;
    private LessonContentType contentType;
    private String streamUrl;
    private Instant expiresAt;
    private long ttlSeconds;
    private Integer durationSeconds;
    private String mimeType;
}
