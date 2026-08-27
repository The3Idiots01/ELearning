package com.learnova.elearning.module.course.service;

import com.learnova.elearning.integration.storage.StorageProperties;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.course.dto.response.LessonResourceResponse;
import com.learnova.elearning.module.course.dto.response.LessonResponse;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.LessonResource;
import com.learnova.elearning.module.course.mapper.CurriculumMapper;
import com.learnova.elearning.module.course.repository.LessonResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Dựng LessonResponse kèm resource đã ký download URL — dùng chung cho
 * CurriculumService và LessonContentService, tránh lặp logic ký/không lộ storage_key.
 */
@Component
@RequiredArgsConstructor
public class LessonResponseAssembler {

    private final LessonResourceRepository resourceRepository;
    private final StorageService storageService;
    private final StorageProperties storageProperties;

    /** Build một lesson kèm resource (tự load resource của lesson đó). */
    public LessonResponse assembleOne(Lesson lesson) {
        List<LessonResourceResponse> resources = resourceRepository
                .findByLesson_IdOrderByPositionAsc(lesson.getId()).stream()
                .map(this::toResource)
                .toList();
        String contentUrl = null;
        if (lesson.getStorageKey() != null) {
            contentUrl = storageService.presignDownload(
                    lesson.getStorageKey(), storageProperties.getDownloadTtl());
        }
        return CurriculumMapper.toLesson(lesson, contentUrl, resources);
    }

    public LessonResourceResponse toResource(LessonResource resource) {
        String url = storageService.presignDownload(
                resource.getStorageKey(), storageProperties.getDownloadTtl());
        return CurriculumMapper.toResource(resource, url);
    }
}
