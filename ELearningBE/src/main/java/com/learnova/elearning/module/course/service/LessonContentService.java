package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.integration.storage.StorageKeyFactory;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.integration.storage.model.ObjectMetadata;
import com.learnova.elearning.integration.storage.model.UploadPurpose;
import com.learnova.elearning.module.course.dto.request.AddLessonResourceRequest;
import com.learnova.elearning.module.course.dto.request.AttachLessonContentRequest;
import com.learnova.elearning.module.course.dto.response.LessonResponse;
import com.learnova.elearning.module.course.entity.Course;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.course.entity.LessonResource;
import com.learnova.elearning.module.course.entity.enums.LessonContentType;
import com.learnova.elearning.module.course.entity.enums.LessonUploadStatus;
import com.learnova.elearning.module.course.repository.LessonRepository;
import com.learnova.elearning.module.course.repository.LessonResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Gắn/gỡ file cho lesson (sau khi FE upload xong qua presigned URL) và CRUD tài
 * liệu đính kèm. Luôn HEAD lại object để đối chiếu size/type thực tế với khai báo
 * và re-validate giới hạn ở server (NFR-03) — không tin client. Không lộ storage_key.
 */
@Service
@RequiredArgsConstructor
public class LessonContentService {

    private final CourseOwnershipGuard ownershipGuard;
    private final LessonRepository lessonRepository;
    private final LessonResourceRepository resourceRepository;
    private final StorageService storageService;
    private final StorageKeyFactory keyFactory;
    private final LessonResponseAssembler lessonAssembler;

    // ---- Lesson content (VIDEO / FILE) ------------------------------------

    @Transactional
    public LessonResponse attachContent(Long courseId, Long lessonId,
                                        AttachLessonContentRequest request, Long userId) {
        ownershipGuard.requireEditableCourse(courseId, userId);
        Lesson lesson = ownershipGuard.requireLessonInCourse(lessonId, courseId);
        UploadPurpose purpose = purposeForContent(lesson.getContentType());

        String key = request.getStorageKey();
        // Key phải thuộc đúng lesson này
        if (!key.startsWith(keyFactory.lessonPrefix(courseId, lessonId))) {
            throw new AppException(ErrorCode.UPLOAD_METADATA_MISMATCH,
                    "storageKey does not belong to this lesson");
        }

        ObjectMetadata meta = storageService.head(key)
                .orElseThrow(() -> new AppException(ErrorCode.UPLOAD_OBJECT_NOT_FOUND));

        verifyMetadata(meta, request.getFileSizeBytes(), request.getMimeType(), purpose);

        String effectiveType = meta.contentType() != null ? meta.contentType() : request.getMimeType();
        String oldKey = lesson.getStorageKey();

        lesson.setStorageKey(key);
        lesson.setOriginalFileName(request.getOriginalFileName());
        lesson.setFileSizeBytes(meta.sizeBytes());
        lesson.setMimeType(effectiveType);
        lesson.setUploadStatus(LessonUploadStatus.READY);
        lesson.setDurationSeconds(
                purpose == UploadPurpose.LESSON_VIDEO && request.getDurationSeconds() != null
                        ? request.getDurationSeconds()
                        : 0);

        Lesson saved = lessonRepository.save(lesson);

        if (oldKey != null && !oldKey.equals(key)) {
            storageService.delete(oldKey);
        }
        return lessonAssembler.assembleOne(saved);
    }

    @Transactional
    public LessonResponse removeContent(Long courseId, Long lessonId, Long userId) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        Lesson lesson = ownershipGuard.requireLessonInCourse(lessonId, courseId);
        purposeForContent(lesson.getContentType()); // chặn ARTICLE/QUIZ

        String key = lesson.getStorageKey();
        lesson.setStorageKey(null);
        lesson.setOriginalFileName(null);
        lesson.setFileSizeBytes(null);
        lesson.setMimeType(null);
        lesson.setDurationSeconds(0);
        lesson.setUploadStatus(LessonUploadStatus.EMPTY);
        Lesson saved = lessonRepository.save(lesson);

        // Chỉ xóa object khi course chưa từng publish (BR-07)
        if (key != null && course.getPublishedAt() == null) {
            storageService.delete(key);
        }
        return lessonAssembler.assembleOne(saved);
    }

    // ---- Lesson resources -------------------------------------------------

    @Transactional
    public LessonResponse addResource(Long courseId, Long lessonId,
                                      AddLessonResourceRequest request, Long userId) {
        ownershipGuard.requireEditableCourse(courseId, userId);
        Lesson lesson = ownershipGuard.requireLessonInCourse(lessonId, courseId);

        String key = request.getStorageKey();
        if (!key.startsWith(keyFactory.lessonPrefix(courseId, lessonId) + "resources/")) {
            throw new AppException(ErrorCode.UPLOAD_METADATA_MISMATCH,
                    "storageKey does not belong to this lesson resources");
        }

        ObjectMetadata meta = storageService.head(key)
                .orElseThrow(() -> new AppException(ErrorCode.UPLOAD_OBJECT_NOT_FOUND));
        verifyMetadata(meta, request.getFileSizeBytes(), null, UploadPurpose.LESSON_RESOURCE);

        int position = (int) resourceRepository.countByLesson_Id(lessonId);
        LessonResource resource = LessonResource.builder()
                .lesson(lesson)
                .title(request.getTitle().trim())
                .storageKey(key)
                .originalFileName(request.getOriginalFileName())
                .fileSizeBytes(meta.sizeBytes())
                .mimeType(request.getMimeType() != null ? request.getMimeType() : meta.contentType())
                .position(position)
                .build();
        resourceRepository.save(resource);

        return lessonAssembler.assembleOne(lesson);
    }

    @Transactional
    public LessonResponse deleteResource(Long courseId, Long lessonId, Long resourceId, Long userId) {
        Course course = ownershipGuard.requireEditableCourse(courseId, userId);
        Lesson lesson = ownershipGuard.requireLessonInCourse(lessonId, courseId);

        LessonResource resource = resourceRepository.findByIdAndLesson_Id(resourceId, lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_RESOURCE_NOT_FOUND));

        resource.setDeletedAt(Instant.now());
        resourceRepository.save(resource);

        if (course.getPublishedAt() == null) {
            storageService.delete(resource.getStorageKey());
        }
        normalizeResourcePositions(lessonId);

        return lessonAssembler.assembleOne(lesson);
    }

    // ---- Helpers ----------------------------------------------------------

    private UploadPurpose purposeForContent(LessonContentType contentType) {
        return switch (contentType) {
            case VIDEO -> UploadPurpose.LESSON_VIDEO;
            case FILE -> UploadPurpose.LESSON_FILE;
            default -> throw new AppException(ErrorCode.LESSON_CONTENT_TYPE_MISMATCH,
                    "Chỉ lesson VIDEO/FILE mới gắn được file");
        };
    }

    /** Đối chiếu metadata thực tế với khai báo + re-validate giới hạn theo purpose. */
    private void verifyMetadata(ObjectMetadata meta, Long declaredSize,
                                String declaredType, UploadPurpose purpose) {
        if (declaredSize != null && meta.sizeBytes() != declaredSize) {
            throw new AppException(ErrorCode.UPLOAD_METADATA_MISMATCH, "Kích thước file không khớp khai báo");
        }
        String actualType = meta.contentType() != null ? meta.contentType() : declaredType;
        if (declaredType != null && meta.contentType() != null
                && !declaredType.equalsIgnoreCase(meta.contentType())) {
            throw new AppException(ErrorCode.UPLOAD_METADATA_MISMATCH, "Loại file không khớp khai báo");
        }
        if (!purpose.isContentTypeAllowed(actualType)) {
            throw new AppException(ErrorCode.UPLOAD_UNSUPPORTED_MEDIA_TYPE);
        }
        if (meta.sizeBytes() > purpose.maxSizeBytes()) {
            throw new AppException(ErrorCode.UPLOAD_FILE_TOO_LARGE);
        }
    }

    private void normalizeResourcePositions(Long lessonId) {
        List<LessonResource> resources = resourceRepository.findByLesson_IdOrderByPositionAsc(lessonId);
        for (int i = 0; i < resources.size(); i++) {
            resources.get(i).setPosition(i);
        }
        resourceRepository.saveAll(resources);
    }
}
