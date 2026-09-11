package com.learnova.elearning.integration.storage;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/** provider=s3 — đọc qua Range GET, không tải hết file (§7.5). */
@Service
@Slf4j
@ConditionalOnProperty(name = "learnova.storage.provider", havingValue = "s3")
public class S3VideoDurationProbe implements VideoDurationProbe {

    private final S3Client s3Client;
    private final String bucket;

    public S3VideoDurationProbe(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.bucket = properties.getS3().getBucket();
    }

    @Override
    public int probeSeconds(String storageKey) {
        try (SeekableByteChannel channel = new S3RangeSeekableChannel(s3Client, bucket, storageKey)) {
            return Mp4DurationReader.readDurationSeconds(channel);
        } catch (IOException e) {
            throw new AppException(ErrorCode.UPLOAD_METADATA_MISMATCH,
                    "Cannot read MP4 duration for key " + storageKey, e);
        } catch (SdkException e) {
            // Preserve transport errors so the durable worker can retry transient failures.
            throw e;
        }
    }
}
