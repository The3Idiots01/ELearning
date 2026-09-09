package com.learnova.elearning.module.course.mapper;

import com.learnova.elearning.module.course.dto.response.LessonResourceResponse;
import com.learnova.elearning.module.course.dto.response.LessonResponse;
import com.learnova.elearning.module.course.dto.response.SectionResponse;
import com.learnova.elearning.module.course.entity.CourseSection;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.LessonResource;
import com.learnova.elearning.module.course.entity.LearningOutcome;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mapper thuần. downloadUrl của resource do service ký sẵn (không lộ storage_key).
 */
public final class CurriculumMapper {

    private CurriculumMapper() {}

    public static SectionResponse toSection(CourseSection section, List<LessonResponse> lessons) {
        return toSection(section, lessons, List.of());
    }

    public static SectionResponse toSection(CourseSection section, List<LessonResponse> lessons,
                                            List<com.learnova.elearning.module.course.dto.response.AssessmentResponse> assessments) {
        int totalDuration = lessons.stream()
                .mapToInt(l -> l.getDurationSeconds() != null ? l.getDurationSeconds() : 0)
                .sum();
        return SectionResponse.builder()
                .id(section.getId())
                .title(section.getTitle())
                .description(section.getDescription())
                .position(section.getPosition())
                .totalLessons(lessons.size())
                .completedLessons((int) lessons.stream().filter(l -> Boolean.TRUE.equals(l.getCompleted())).count())
                .totalAssessments(assessments.size())
                .completedAssessments((int) assessments.stream().filter(a -> Boolean.TRUE.equals(a.getCompleted())).count())
                .totalDurationSeconds(totalDuration)
                .lessons(lessons)
                .assessments(assessments)
                .build();
    }

    public static LessonResponse toLesson(Lesson lesson, Boolean playable, Integer lastPositionSeconds,
                                           BigDecimal coveragePercent, List<LessonResourceResponse> resources) {
        return LessonResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .contentType(lesson.getContentType())
                .uploadStatus(lesson.getUploadStatus())
                .durationSeconds(lesson.getDurationSeconds())
                .isPreview(lesson.getIsPreview())
                .position(lesson.getPosition())
                .contentText(lesson.getContentText())
                .playable(playable)
                .lastPositionSeconds(lastPositionSeconds)
                .coveragePercent(coveragePercent)
                .outcomeIds((lesson.getOutcomes() == null ? java.util.Set.<LearningOutcome>of() : lesson.getOutcomes()).stream()
                        .sorted(java.util.Comparator.comparing(LearningOutcome::getPosition)
                                .thenComparing(LearningOutcome::getId))
                        .map(LearningOutcome::getId)
                        .toList())
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
