package com.learnova.elearning.integration.storage;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.integration.storage.model.ObjectMetadata;
import com.learnova.elearning.integration.storage.model.PresignedUpload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Storage chạy trên đĩa local cho môi trường dev — giả lập presigned PUT/GET qua
 * endpoint {@code /api/v1/dev/storage/**}. Bật khi learnova.storage.provider=local
 * (mặc định). Production dùng {@code S3StorageService} (Task 6).
 * <p>
 * Content-Type được lưu vào sidecar {key}.contenttype để {@link #head} đối chiếu
 * chính xác lúc confirm (Files.probeContentType không ổn định với mp4).
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "learnova.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final String UPLOAD_PATH = "/api/v1/dev/storage/upload";
    private static final String DOWNLOAD_PATH = "/api/v1/dev/storage/download";
    private static final String META_SUFFIX = ".contenttype";

    private final StorageProperties properties;
    private final LocalStorageSigner signer;
    private final Path rootDir;

    public LocalStorageService(StorageProperties properties, LocalStorageSigner signer) {
        this.properties = properties;
        this.signer = signer;
        this.rootDir = Path.of(properties.getLocal().getRootDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
            log.info("LocalStorageService active — root dir: {}", rootDir);
        } catch (IOException e) {
            throw new AppException(ErrorCode.STORAGE_UNAVAILABLE, "Cannot create local storage root dir", e);
        }
    }

    @Override
    public PresignedUpload presignUpload(String key, String contentType, Duration ttl) {
        long exp = Instant.now().plus(ttl).getEpochSecond();
        String sig = signer.sign("PUT", key, exp);
        String url = UriComponentsBuilder.fromUriString(properties.getLocal().getPublicBaseUrl())
                .path(UPLOAD_PATH)
                .queryParam("key", key)
                .queryParam("exp", exp)
                .queryParam("sig", sig)
                .build()
                .toUriString();
        return PresignedUpload.builder()
                .uploadUrl(url)
                .storageKey(key)
                .httpMethod("PUT")
                .expiresInSeconds(ttl.toSeconds())
                .requiredHeaders(java.util.Map.of("Content-Type", contentType))
                .build();
    }

    @Override
    public String presignDownload(String key, Duration ttl) {
        long exp = Instant.now().plus(ttl).getEpochSecond();
        String sig = signer.sign("GET", key, exp);
        return UriComponentsBuilder.fromUriString(properties.getLocal().getPublicBaseUrl())
                .path(DOWNLOAD_PATH)
                .queryParam("key", key)
                .queryParam("exp", exp)
                .queryParam("sig", sig)
                .build()
                .toUriString();
    }

    @Override
    public Optional<ObjectMetadata> head(String key) {
        Path path = resolve(key);
        if (!Files.exists(path) || Files.isDirectory(path)) {
            return Optional.empty();
        }
        try {
            long size = Files.size(path);
            String contentType = readContentType(path);
            return Optional.of(new ObjectMetadata(key, size, contentType));
        } catch (IOException e) {
            log.warn("Failed to read metadata for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void delete(String key) {
        try {
            Path path = resolve(key);
            Files.deleteIfExists(path);
            Files.deleteIfExists(metaPath(path));
        } catch (IOException e) {
            log.warn("Failed to delete key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public void deleteByPrefix(String prefix) {
        Path dir = resolve(prefix);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            log.warn("Failed to delete {}: {}", p, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to walk prefix {}: {}", prefix, e.getMessage());
        }
    }

    // ---- internal (gọi từ LocalStorageController) --------------------------

    void store(String key, InputStream body, String contentType) {
        Path path = resolve(key);
        try {
            Files.createDirectories(path.getParent());
            Files.copy(body, path, StandardCopyOption.REPLACE_EXISTING);
            if (contentType != null && !contentType.isBlank()) {
                Files.writeString(metaPath(path), contentType, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new AppException(ErrorCode.STORAGE_UNAVAILABLE, "Cannot write object to local storage", e);
        }
    }

    Optional<Path> locate(String key) {
        Path path = resolve(key);
        if (Files.exists(path) && !Files.isDirectory(path)) {
            return Optional.of(path);
        }
        return Optional.empty();
    }

    String readContentType(Path path) {
        try {
            Path meta = metaPath(path);
            if (Files.exists(meta)) {
                return Files.readString(meta, StandardCharsets.UTF_8).trim();
            }
            return Files.probeContentType(path);
        } catch (IOException e) {
            return null;
        }
    }

    LocalStorageSigner signer() {
        return signer;
    }

    /** Chặn path traversal: key không được thoát khỏi rootDir. */
    private Path resolve(String key) {
        Path resolved = rootDir.resolve(key).normalize();
        if (!resolved.startsWith(rootDir)) {
            throw new AppException(ErrorCode.UPLOAD_OBJECT_NOT_FOUND, "Invalid storage key");
        }
        return resolved;
    }

    private Path metaPath(Path path) {
        return path.resolveSibling(path.getFileName().toString() + META_SUFFIX);
    }
}
