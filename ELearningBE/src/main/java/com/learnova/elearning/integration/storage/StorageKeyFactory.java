package com.learnova.elearning.integration.storage;

import com.learnova.elearning.integration.storage.model.UploadPurpose;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

/**
 * Sinh storage_key theo quy ước cố định. Có courseId trong key để truy vết và
 * xóa theo prefix khi archive course.
 * <pre>
 * courses/{courseId}/thumbnail/{uuid}.{ext}
 * courses/{courseId}/promo/{uuid}.mp4
 * courses/{courseId}/lessons/{lessonId}/video/{uuid}.mp4
 * courses/{courseId}/lessons/{lessonId}/file/{uuid}.{ext}
 * courses/{courseId}/lessons/{lessonId}/resources/{uuid}-{slug}.{ext}
 * </pre>
 */
@Component
public class StorageKeyFactory {

    public String build(UploadPurpose purpose, Long courseId, Long lessonId, String originalFileName) {
        String uuid = UUID.randomUUID().toString();
        String ext = extractExtension(originalFileName);

        String base = "courses/" + courseId;
        if (purpose.isLessonScoped()) {
            base += "/lessons/" + lessonId;
        }
        base += "/" + purpose.keySegment() + "/";

        return switch (purpose) {
            case LESSON_RESOURCE -> base + uuid + "-" + slugFileName(originalFileName) + ext;
            default -> base + uuid + ext;
        };
    }

    public String coursePrefix(Long courseId) {
        return "courses/" + courseId + "/";
    }

    public String lessonPrefix(Long courseId, Long lessonId) {
        return coursePrefix(courseId) + "lessons/" + lessonId + "/";
    }

    private String extractExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        return ext.isEmpty() ? "" : "." + ext;
    }

    private String slugFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        String name = fileName;
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (normalized.length() > 60) {
            normalized = normalized.substring(0, 60).replaceAll("-+$", "");
        }
        return normalized.isEmpty() ? "file" : normalized;
    }
}
