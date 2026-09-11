package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.integration.ai.AiProperties;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class LessonContentTextExtractor {

    private final StorageService storageService;
    private final AiProperties properties;

    public String extract(Lesson lesson) {
        if (lesson.getContentType() == LessonContentType.ARTICLE) {
            return requireText(lesson.getContentText());
        }
        if (lesson.getContentType() != LessonContentType.FILE
                || lesson.getStorageKey() == null || lesson.getStorageKey().isBlank()) {
            throw new AppException(ErrorCode.AI_CONTENT_UNAVAILABLE,
                    "AI outcome analysis currently supports ARTICLE, PDF and DOCX lessons");
        }
        if (!isSupportedDocument(lesson)) {
            throw new AppException(ErrorCode.AI_CONTENT_UNAVAILABLE,
                    "Only PDF and DOCX lesson files can be analyzed in this version");
        }
        if (lesson.getFileSizeBytes() != null
                && lesson.getFileSizeBytes() > properties.getMaxDocumentBytes()) {
            throw new AppException(ErrorCode.AI_CONTENT_UNAVAILABLE,
                    "Document is too large for AI analysis");
        }

        try (InputStream input = storageService.openReadable(lesson.getStorageKey()).getInputStream()) {
            String text = new Tika().parseToString(
                    input, new Metadata(), properties.getMaxInputChars());
            return requireText(text);
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppException(ErrorCode.AI_CONTENT_UNAVAILABLE,
                    "Could not extract text from this document", ex);
        }
    }

    private String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.AI_CONTENT_UNAVAILABLE);
        }
        String normalized = value.trim();
        return normalized.length() <= properties.getMaxInputChars()
                ? normalized : normalized.substring(0, properties.getMaxInputChars());
    }

    private boolean isSupportedDocument(Lesson lesson) {
        String name = lesson.getOriginalFileName() == null
                ? "" : lesson.getOriginalFileName().toLowerCase(Locale.ROOT);
        String mime = lesson.getMimeType() == null
                ? "" : lesson.getMimeType().toLowerCase(Locale.ROOT);
        return name.endsWith(".pdf") || name.endsWith(".docx")
                || mime.equals("application/pdf")
                || mime.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }
}
