package com.learnova.elearning.integration.storage;

import com.learnova.elearning.integration.storage.model.ObjectMetadata;
import com.learnova.elearning.integration.storage.model.PresignedUpload;

import java.time.Duration;
import java.util.Optional;

/**
 * Trừu tượng hóa object storage. Code nghiệp vụ không được biết đang chạy S3
 * (Cloudflare R2) hay đĩa local. DB chỉ lưu storage_key, mọi URL đều ký ngắn hạn
 * tại thời điểm cần (BR-12).
 */
public interface StorageService {

    /** Ký URL để FE PUT thẳng file lên storage. */
    PresignedUpload presignUpload(String key, String contentType, Duration ttl);

    /** Ký URL GET ngắn hạn để phát/tải file (dùng ở Sprint 3 — Flow 2 LLD). */
    String presignDownload(String key, Duration ttl);

    /** Đọc metadata object; empty nếu không tồn tại. Dùng để xác nhận upload. */
    Optional<ObjectMetadata> head(String key);

    void delete(String key);

    /** Xóa mọi object theo tiền tố (dùng khi archive/xóa cả course). */
    void deleteByPrefix(String prefix);
}
