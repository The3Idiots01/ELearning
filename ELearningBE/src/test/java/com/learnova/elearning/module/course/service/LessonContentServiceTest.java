package com.learnova.elearning.module.course.service;

import com.learnova.elearning.integration.storage.StorageKeyFactory;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.integration.storage.model.ObjectMetadata;
import com.learnova.elearning.module.course.dto.request.AttachLessonContentRequest;
import com.learnova.elearning.module.course.dto.response.LessonResponse;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.CourseSection;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import com.learnova.elearning.module.course.event.LessonContentAttachedEvent;
import com.learnova.elearning.module.course.repository.LessonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * §7.5 design_us15_us17.md — attachContent() không còn tin duration client
 * khai cho VIDEO: chuyển PROCESSING, phát sự kiện cho job nền đo lại. FILE thì
 * không liên quan tới duration nên vẫn READY ngay, không phát sự kiện.
 */
@ExtendWith(MockitoExtension.class)
class LessonContentServiceTest {

    @Mock
    private CourseOwnershipGuard ownershipGuard;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private com.learnova.elearning.module.course.repository.LessonResourceRepository resourceRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private StorageKeyFactory keyFactory;
    @Mock
    private LessonResponseAssembler lessonAssembler;
    @Mock
    private ApplicationEventPublisher events;

    private LessonContentService service() {
        return new LessonContentService(
                ownershipGuard, lessonRepository, resourceRepository,
                storageService, keyFactory, lessonAssembler, events);
    }

    private Course course(long courseId) {
        return Course.builder().id(courseId).build();
    }

    private CourseSection section(Course c) {
        return CourseSection.builder().id(1L).course(c).build();
    }

    private Lesson lesson(long lessonId, LessonContentType type, CourseSection sec) {
        return Lesson.builder()
                .id(lessonId)
                .title("L" + lessonId)
                .contentType(type)
                .section(sec)
                .uploadStatus(LessonUploadStatus.EMPTY)
                .durationSeconds(0)
                .build();
    }

    @Test
    void attachContent_video_setsProcessingAndPublishesEvent_ignoringClientDuration() {
        long courseId = 1L, lessonId = 42L;
        Course c = course(courseId);
        CourseSection sec = section(c);
        Lesson l = lesson(lessonId, LessonContentType.VIDEO, sec);

        when(ownershipGuard.requireEditableCourse(courseId, 10L)).thenReturn(c);
        when(ownershipGuard.requireLessonInCourse(lessonId, courseId)).thenReturn(l);
        when(keyFactory.lessonPrefix(courseId, lessonId)).thenReturn("courses/1/lessons/42/");
        String key = "courses/1/lessons/42/video/x.mp4";
        when(storageService.head(key)).thenReturn(Optional.of(new ObjectMetadata(key, 1000L, "video/mp4")));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lessonAssembler.assembleOne(any(Lesson.class))).thenReturn(LessonResponse.builder().build());

        AttachLessonContentRequest req = new AttachLessonContentRequest();
        req.setStorageKey(key);
        req.setOriginalFileName("x.mp4");
        req.setFileSizeBytes(1000L);
        req.setMimeType("video/mp4");
        req.setDurationSeconds(99999); // client khai gian — phải bị bỏ qua

        service().attachContent(courseId, lessonId, req, 10L);

        assertThat(l.getUploadStatus()).isEqualTo(LessonUploadStatus.PROCESSING);
        assertThat(l.getDurationSeconds()).isZero();

        ArgumentCaptor<LessonContentAttachedEvent> captor = ArgumentCaptor.forClass(LessonContentAttachedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().lessonId()).isEqualTo(lessonId);
        assertThat(captor.getValue().storageKey()).isEqualTo(key);
    }

    @Test
    void attachContent_file_readyImmediately_noEvent() {
        long courseId = 1L, lessonId = 43L;
        Course c = course(courseId);
        CourseSection sec = section(c);
        Lesson l = lesson(lessonId, LessonContentType.FILE, sec);

        when(ownershipGuard.requireEditableCourse(courseId, 10L)).thenReturn(c);
        when(ownershipGuard.requireLessonInCourse(lessonId, courseId)).thenReturn(l);
        when(keyFactory.lessonPrefix(courseId, lessonId)).thenReturn("courses/1/lessons/43/");
        String key = "courses/1/lessons/43/file/doc.pdf";
        when(storageService.head(key)).thenReturn(Optional.of(new ObjectMetadata(key, 2000L, "application/pdf")));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lessonAssembler.assembleOne(any(Lesson.class))).thenReturn(LessonResponse.builder().build());

        AttachLessonContentRequest req = new AttachLessonContentRequest();
        req.setStorageKey(key);
        req.setOriginalFileName("doc.pdf");
        req.setFileSizeBytes(2000L);
        req.setMimeType("application/pdf");

        service().attachContent(courseId, lessonId, req, 10L);

        assertThat(l.getUploadStatus()).isEqualTo(LessonUploadStatus.READY);
        verify(events, never()).publishEvent(any());
    }
}
