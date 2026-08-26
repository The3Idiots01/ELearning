package com.learnova.elearning.integration.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Cấu hình storage. provider=local (mặc định, dev) | s3 (Cloudflare R2, Task 6).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "learnova.storage")
public class StorageProperties {

    /** local | s3 */
    private String provider = "local";

    /** TTL cho presigned upload URL. */
    private Duration uploadTtl = Duration.ofMinutes(15);

    /** TTL cho presigned download URL (BR-12: <= 15 phút). */
    private Duration downloadTtl = Duration.ofMinutes(15);

    private final Local local = new Local();
    private final S3 s3 = new S3();

    @Getter
    @Setter
    public static class Local {
        /** Thư mục gốc lưu file trên đĩa khi chạy dev. */
        private String rootDir = "./storage-data";

        /** Base URL public để dựng uploadUrl/downloadUrl (điểm FE gọi tới). */
        private String publicBaseUrl = "http://localhost:8080";

        /** Secret ký token cho endpoint dev storage (chỉ dùng cho môi trường dev). */
        private String signingSecret = "learnova-local-storage-dev-secret-change-me";
    }

    @Getter
    @Setter
    public static class S3 {
        private String endpoint;
        private String region = "auto";
        private String bucket;
        private String accessKey;
        private String secretKey;
        /** Base URL public/CDN nếu có, để dựng download URL. */
        private String publicBaseUrl;
    }
}
