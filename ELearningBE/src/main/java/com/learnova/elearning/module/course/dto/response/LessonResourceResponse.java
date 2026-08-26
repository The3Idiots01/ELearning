package com.learnova.elearning.module.course.dto.response;

import lombok.Builder;
import lombok.Data;

/** Tài liệu đính kèm. downloadUrl đã ký ngắn hạn — không lộ storage_key (BR-12). */
@Data
@Builder
public class LessonResourceResponse {

    private Long id;
    private String title;
    private String originalFileName;
    private Long fileSizeBytes;
    private String mimeType;
    private Integer position;
    private String downloadUrl;
}
