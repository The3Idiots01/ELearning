package com.learnova.elearning.module.course.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.integration.storage.StorageKeyFactory;
import com.learnova.elearning.integration.storage.StorageProperties;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.integration.storage.model.PresignedUpload;
import com.learnova.elearning.integration.storage.model.UploadPurpose;
import com.learnova.elearning.module.course.dto.request.PresignUploadRequest;
import com.learnova.elearning.module.course.dto.response.PresignUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Cấp presigned URL cho FE upload thẳng lên storage. Xác thực quyền sở hữu course
 * (và lesson với purpose LESSON_*), loại file và dung lượng TRƯỚC khi ký (BR-05).
 * File thật được đối chiếu lại ở bước confirm (Task 9) qua StorageService.head.
 */
@Service
@RequiredArgsConstructor
public class UploadService {

    private final CourseOwnershipGuard ownershipGuard;
    private final StorageService storageService;
    private final StorageKeyFactory keyFactory;
    private final StorageProperties storageProperties;

    public PresignUploadResponse presign(PresignUploadRequest request, Long userId) {
        UploadPurpose purpose = request.getPurpose();

        // 1. Quyền sở hữu course (chặn cả khi course bị admin SUSPENDED)
        ownershipGuard.requireEditableCourse(request.getCourseId(), userId);

        // 2. Với purpose thuộc lesson: lessonId bắt buộc và phải thuộc đúng course
        if (purpose.isLessonScoped()) {
            if (request.getLessonId() == null) {
                throw new AppException(ErrorCode.FIELD_REQUIRED, "lessonId is required for " + purpose);
            }
            ownershipGuard.requireLessonInCourse(request.getLessonId(), request.getCourseId());
        }

        // 3. Loại file cho phép (BR-05: video mp4)
        if (!purpose.isContentTypeAllowed(request.getContentType())) {
            throw new AppException(ErrorCode.UPLOAD_UNSUPPORTED_MEDIA_TYPE,
                    "Content type " + request.getContentType() + " not allowed for " + purpose);
        }

        // 4. Dung lượng tối đa (BR-05: <= 500MB)
        if (request.getSizeBytes() > purpose.maxSizeBytes()) {
            throw new AppException(ErrorCode.UPLOAD_FILE_TOO_LARGE,
                    "File exceeds max size " + purpose.maxSizeBytes() + " bytes for " + purpose);
        }

        // 5. Sinh key và ký URL
        String key = keyFactory.build(purpose, request.getCourseId(), request.getLessonId(), request.getFileName());
        PresignedUpload presigned = storageService.presignUpload(
                key, request.getContentType(), storageProperties.getUploadTtl());

        return PresignUploadResponse.from(presigned);
    }
}
