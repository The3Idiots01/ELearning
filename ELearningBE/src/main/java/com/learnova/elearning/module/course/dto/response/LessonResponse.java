package com.learnova.elearning.module.course.dto.response;

import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Một curriculum item. Không lộ storage_key; chỉ trả metadata file (BR-12). */
@Data
@Builder
public class LessonResponse {

    private Long id;
    private String title;
    private LessonContentType contentType;
    private LessonUploadStatus uploadStatus;
    private Integer durationSeconds;
    private Boolean isPreview;
    private Integer position;

    private String contentText;
    private String originalFileName;
    private Long fileSizeBytes;
    private String mimeType;

    private List<LessonResourceResponse> resources;
}
