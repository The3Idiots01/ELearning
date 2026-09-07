package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.integration.storage.VideoDurationProbe;
import com.learnova.elearning.module.course.event.LessonContentAttachedEvent;
import com.learnova.elearning.module.course.repository.LessonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoMetadataProcessorTest {

    @Mock
    private VideoDurationProbe durationProbe;
    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private VideoMetadataProcessor processor;

    @Test
    void onContentAttached_probeSucceeds_marksReadyWithDuration() {
        when(durationProbe.probeSeconds("courses/1/lessons/42/video/x.mp4")).thenReturn(612);

        processor.onContentAttached(new LessonContentAttachedEvent(42L, "courses/1/lessons/42/video/x.mp4"));

        verify(lessonRepository).markReady(42L, 612);
        verify(lessonRepository, never()).markFailed(42L);
    }

    @Test
    void onContentAttached_probeFails_marksFailed() {
        when(durationProbe.probeSeconds("courses/1/lessons/42/video/bad.mp4"))
                .thenThrow(new AppException(ErrorCode.UPLOAD_METADATA_MISMATCH));

        processor.onContentAttached(new LessonContentAttachedEvent(42L, "courses/1/lessons/42/video/bad.mp4"));

        verify(lessonRepository).markFailed(42L);
        verify(lessonRepository, never()).markReady(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }
}
