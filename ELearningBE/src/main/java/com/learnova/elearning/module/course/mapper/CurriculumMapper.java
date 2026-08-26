package com.learnova.elearning.module.course.mapper;

import com.learnova.elearning.module.course.dto.response.LessonResourceResponse;
import com.learnova.elearning.module.course.dto.response.LessonResponse;
import com.learnova.elearning.module.course.dto.response.SectionResponse;
import com.learnova.elearning.module.course.entity.CourseSection;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.LessonResource;

import java.util.List;

/**
 * Mapper thuần. downloadUrl của resource do service ký sẵn (không lộ storage_key).
 */
public final class CurriculumMapper {

    private CurriculumMapper() {}

    public static SectionResponse toSection(CourseSection section, List<LessonResponse> lessons) {
        int totalDuration = lessons.stream()
                .mapToInt(l -> l.getDurationSeconds() != null ? l.getDurationSeconds() : 0)
                .sum();
        return SectionResponse.builder()
                .id(section.getId())
                .title(section.getTitle())
                .description(section.getDescription())
                .position(section.getPosition())
                .totalLessons(lessons.size())
                .totalDurationSeconds(totalDuration)
                .lessons(lessons)
                .build();
    }

    public static LessonResponse toLesson(Lesson lesson, List<LessonResourceResponse> resources) {
        return LessonResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .contentType(lesson.getContentType())
                .uploadStatus(lesson.getUploadStatus())
                .durationSeconds(lesson.getDurationSeconds())
                .isPreview(lesson.getIsPreview())
                .position(lesson.getPosition())
                .contentText(lesson.getContentText())
                .originalFileName(lesson.getOriginalFileName())
                .fileSizeBytes(lesson.getFileSizeBytes())
                .mimeType(lesson.getMimeType())
                .resources(resources)
                .build();
    }

    public static LessonResourceResponse toResource(LessonResource resource, String downloadUrl) {
        return LessonResourceResponse.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .originalFileName(resource.getOriginalFileName())
                .fileSizeBytes(resource.getFileSizeBytes())
                .mimeType(resource.getMimeType())
                .position(resource.getPosition())
                .downloadUrl(downloadUrl)
                .build();
    }
}
