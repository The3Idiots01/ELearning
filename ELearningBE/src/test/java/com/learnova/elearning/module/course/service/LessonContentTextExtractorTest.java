package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.integration.ai.AiProperties;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class LessonContentTextExtractorTest {

    private final AiProperties properties = new AiProperties();
    private final LessonContentTextExtractor extractor = new LessonContentTextExtractor(
            mock(StorageService.class), properties);

    @Test
    void articleContentIsTrimmedAndLimited() {
        properties.setMaxInputChars(8);
        Lesson lesson = Lesson.builder()
                .contentType(LessonContentType.ARTICLE)
                .contentText("  Nội dung bài học  ")
                .build();

        assertThat(extractor.extract(lesson)).isEqualTo("Nội dung");
    }

    @Test
    void videoWithoutTranscriptIsRejected() {
        Lesson lesson = Lesson.builder().contentType(LessonContentType.VIDEO).build();

        assertThatThrownBy(() -> extractor.extract(lesson))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_CONTENT_UNAVAILABLE);
    }
}
