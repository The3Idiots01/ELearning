package com.learnova.elearning.module.course.dto.response;

import com.learnova.elearning.module.course.entity.CourseStatusLog;
import com.learnova.elearning.module.course.entity.enums.CourseStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CourseStatusLogResponse {

    private Long id;
    private CourseStatus fromStatus;
    private CourseStatus toStatus;
    private String comment;
    private Long actorId;
    private String actorName;
    private Instant createdAt;

    public static CourseStatusLogResponse fromEntity(CourseStatusLog log) {
        return CourseStatusLogResponse.builder()
                .id(log.getId())
                .fromStatus(log.getFromStatus())
                .toStatus(log.getToStatus())
                .comment(log.getComment())
                .actorId(log.getActor().getId())
                .actorName(log.getActor().getFullName())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
