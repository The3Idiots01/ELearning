package com.learnova.elearning.integration.storage;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.integration.storage.model.ObjectMetadata;
import com.learnova.elearning.integration.storage.model.PresignedUpload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Object storage tương thích S3 (Cloudflare R2). Bật khi provider=s3. Presigned
 * PUT/GET ký phía server, TTL ngắn (BR-12). Không lộ storage_key ra ngoài.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "learnova.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private static final int DELETE_BATCH_SIZE = 1000;

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3StorageService(S3Client s3Client, S3Presigner presigner, StorageProperties properties) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.bucket = properties.getS3().getBucket();
        log.info("S3StorageService active — bucket: {}", bucket);
    }

    @Override
    public PresignedUpload presignUpload(String key, String contentType, Duration ttl) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(putRequest)
                .build();
        String url = presigner.presignPutObject(presignRequest).url().toString();

        return PresignedUpload.builder()
                .uploadUrl(url)
                .storageKey(key)
                .httpMethod("PUT")
                .expiresInSeconds(ttl.toSeconds())
                .requiredHeaders(Map.of("Content-Type", contentType))
                .build();
    }

    @Override
    public String presignDownload(String key, Duration ttl) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(b -> b.bucket(bucket).key(key))
                .build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public Optional<ObjectMetadata> head(String key) {
        try {
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return Optional.of(new ObjectMetadata(key, response.contentLength(), response.contentType()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            log.error("S3 headObject failed for key {}: {}", key, e.getMessage());
            throw new AppException(ErrorCode.STORAGE_UNAVAILABLE, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            log.warn("S3 delete failed for key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public void deleteByPrefix(String prefix) {
        try {
            List<ObjectIdentifier> batch = new ArrayList<>(DELETE_BATCH_SIZE);
            for (var page : s3Client.listObjectsV2Paginator(
                    ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build())) {
                for (var obj : page.contents()) {
                    batch.add(ObjectIdentifier.builder().key(obj.key()).build());
                    if (batch.size() == DELETE_BATCH_SIZE) {
                        flushDelete(batch);
                        batch.clear();
                    }
                }
            }
            if (!batch.isEmpty()) {
                flushDelete(batch);
            }
        } catch (S3Exception e) {
            log.warn("S3 deleteByPrefix failed for prefix {}: {}", prefix, e.getMessage());
        }
    }

    private void flushDelete(List<ObjectIdentifier> keys) {
        s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(keys).build())
                .build());
    }
}
