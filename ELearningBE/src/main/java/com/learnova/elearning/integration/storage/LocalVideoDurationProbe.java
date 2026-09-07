package com.learnova.elearning.integration.storage;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** provider=local — đọc trực tiếp file trên đĩa (§7.5). */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "learnova.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalVideoDurationProbe implements VideoDurationProbe {

    private final LocalStorageService storage;

    @Override
    public int probeSeconds(String storageKey) {
        Path path = storage.locate(storageKey)
                .orElseThrow(() -> new AppException(ErrorCode.UPLOAD_OBJECT_NOT_FOUND));
        try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
            return Mp4DurationReader.readDurationSeconds(channel);
        } catch (IOException e) {
            throw new AppException(ErrorCode.UPLOAD_METADATA_MISMATCH,
                    "Cannot read MP4 duration for key " + storageKey, e);
        }
    }
}
