package com.learnova.elearning.integration.storage;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Endpoint giả lập object storage cho môi trường dev — FE PUT/GET file như thể
 * đang gọi presigned URL của S3. Chỉ tồn tại khi provider=local; production
 * (provider=s3) FE gọi thẳng bucket nên bean này không được tạo.
 */
@RestController
@RequestMapping("/api/v1/dev/storage")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "learnova.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageController {

    private final LocalStorageService storage;

    @PutMapping("/upload")
    public ResponseEntity<Void> upload(
            @RequestParam String key,
            @RequestParam long exp,
            @RequestParam String sig,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            HttpServletRequest request
    ) throws IOException {
        if (!storage.signer().verify("PUT", key, exp, sig)) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Upload URL is invalid or expired");
        }
        try (InputStream body = request.getInputStream()) {
            storage.store(key, body, contentType);
        }
        log.debug("Local storage stored object: {}", key);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(
            @RequestParam String key,
            @RequestParam long exp,
            @RequestParam String sig
    ) {
        if (!storage.signer().verify("GET", key, exp, sig)) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Download URL is invalid or expired");
        }
        Path path = storage.locate(key)
                .orElseThrow(() -> new AppException(ErrorCode.UPLOAD_OBJECT_NOT_FOUND));

        String contentType = storage.readContentType(path);
        MediaType mediaType = contentType != null
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(new FileSystemResource(path));
    }
}
