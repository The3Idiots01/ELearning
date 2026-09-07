package com.learnova.elearning.module.delivery.strategy;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.course.entity.Lesson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamDeliveryStrategyTest {

    private static final byte[] CONTENT = "0123456789".repeat(20).getBytes(StandardCharsets.UTF_8); // 200 bytes

    @Mock
    private StorageService storageService;

    private StreamDeliveryStrategy strategy;
    private Path tempFile;
    private Lesson lesson;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("stream-strategy-test", ".mp4");
        Files.write(tempFile, CONTENT);
        strategy = new StreamDeliveryStrategy(storageService);
        lesson = Lesson.builder().id(1L).storageKey("courses/1/lessons/1/video.mp4").mimeType("video/mp4").build();
        lenient().when(storageService.openReadable(lesson.getStorageKey())).thenReturn(new FileSystemResource(tempFile));
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    @Test
    void deliver_noRangeHeader_returns200WithFullLength() {
        ResponseEntity<?> response = strategy.deliver(lesson, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.valueOf("video/mp4"));
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(CONTENT.length);
    }

    @Test
    void deliver_validRange_returns206WithResourceRegion() {
        ResponseEntity<?> response = strategy.deliver(lesson, "bytes=10-19");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getBody()).isInstanceOf(ResourceRegion.class);
        ResourceRegion region = (ResourceRegion) response.getBody();
        assertThat(region.getPosition()).isEqualTo(10);
        assertThat(region.getCount()).isEqualTo(10);
    }

    @Test
    void deliver_openEndedRange_clampsToChunkSizeAtMost() {
        ResponseEntity<?> response = strategy.deliver(lesson, "bytes=0-");

        ResourceRegion region = (ResourceRegion) response.getBody();
        assertThat(region.getPosition()).isEqualTo(0);
        assertThat(region.getCount()).isEqualTo(CONTENT.length); // nhỏ hơn 1 MiB nên lấy hết phần còn lại
    }

    @Test
    void deliver_suffixRange_returnsLastNBytes() {
        ResponseEntity<?> response = strategy.deliver(lesson, "bytes=-10");

        ResourceRegion region = (ResourceRegion) response.getBody();
        assertThat(region.getPosition()).isEqualTo(CONTENT.length - 10);
        assertThat(region.getCount()).isEqualTo(10);
    }

    @Test
    void deliver_malformedRangeHeader_throwsRangeNotSatisfiable() {
        assertThatThrownBy(() -> strategy.deliver(lesson, "not-a-range"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_RANGE_NOT_SATISFIABLE);
    }

    @Test
    void deliver_rangeStartBeyondLength_throwsRangeNotSatisfiable() {
        assertThatThrownBy(() -> strategy.deliver(lesson, "bytes=99999999-"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_RANGE_NOT_SATISFIABLE);
    }

    @Test
    void deliver_missingMimeType_fallsBackToOctetStream() {
        Lesson noMime = Lesson.builder().id(2L).storageKey("k").build();
        when(storageService.openReadable("k")).thenReturn(new FileSystemResource(tempFile));

        ResponseEntity<?> response = strategy.deliver(noMime, null);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }
}
