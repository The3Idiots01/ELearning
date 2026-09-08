package com.learnova.elearning.module.course.dto.response;

import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
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
    private Boolean completed;

    private List<Long> outcomeIds;

    private String contentText;

    /** true nếu learner có thể xin PlaybackTicket cho lesson này ngay bây giờ — §7.1 (G1). */
    private Boolean playable;
    /** Resume — null nếu chưa từng xem hoặc không phải learner đã enroll (FR-KT-01). */
    private Integer lastPositionSeconds;
    /** Coverage — analytics, không gác completion (§5.1). Null cùng điều kiện như trên. */
    private BigDecimal coveragePercent;

    private String originalFileName;
    private Long fileSizeBytes;
    private String mimeType;

    private List<LessonResourceResponse> resources;
}
