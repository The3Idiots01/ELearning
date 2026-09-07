package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.integration.storage.VideoDurationProbe;
import com.learnova.elearning.module.course.dto.response.VideoDurationBackfillResponse;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import com.learnova.elearning.module.course.repository.LessonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoDurationBackfillServiceTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private VideoDurationProbe durationProbe;

    @InjectMocks
    private VideoDurationBackfillService backfillService;

    private Lesson video(long id, String key, int currentDuration) {
        return Lesson.builder()
                .id(id)
                .title("Lesson " + id)
                .contentType(LessonContentType.VIDEO)
                .uploadStatus(LessonUploadStatus.READY)
                .storageKey(key)
                .durationSeconds(currentDuration)
                .build();
    }

    @Test
    void backfill_updatesWrongDuration_leavesCorrectOneAlone_andCountsFailures() {
        Lesson stale = video(1L, "courses/1/lessons/1/video/a.mp4", 0);
        Lesson correct = video(2L, "courses/1/lessons/2/video/b.mp4", 300);
        Lesson broken = video(3L, "courses/1/lessons/3/video/c.mp4", 120);

        when(lessonRepository.findByContentTypeAndUploadStatus(LessonContentType.VIDEO, LessonUploadStatus.READY))
                .thenReturn(List.of(stale, correct, broken));
        when(durationProbe.probeSeconds("courses/1/lessons/1/video/a.mp4")).thenReturn(612);
        when(durationProbe.probeSeconds("courses/1/lessons/2/video/b.mp4")).thenReturn(300);
        when(durationProbe.probeSeconds("courses/1/lessons/3/video/c.mp4"))
                .thenThrow(new AppException(ErrorCode.UPLOAD_OBJECT_NOT_FOUND));

        VideoDurationBackfillResponse result = backfillService.backfill();

        assertThat(result.getScanned()).isEqualTo(3);
        assertThat(result.getUpdated()).isEqualTo(1);
        assertThat(result.getUnchanged()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getFailedLessonIds()).containsExactly(3L);
        assertThat(stale.getDurationSeconds()).isEqualTo(612);
        assertThat(correct.getDurationSeconds()).isEqualTo(300);
        // lesson lỗi probe vẫn giữ nguyên trạng thái/duration cũ — không bị FAILED
        assertThat(broken.getUploadStatus()).isEqualTo(LessonUploadStatus.READY);
        assertThat(broken.getDurationSeconds()).isEqualTo(120);
    }

    @Test
    void backfill_noLessons_returnsZeroedSummary() {
        when(lessonRepository.findByContentTypeAndUploadStatus(LessonContentType.VIDEO, LessonUploadStatus.READY))
                .thenReturn(List.of());

        VideoDurationBackfillResponse result = backfillService.backfill();

        assertThat(result.getScanned()).isZero();
        assertThat(result.getUpdated()).isZero();
        assertThat(result.getFailed()).isZero();
    }
}
