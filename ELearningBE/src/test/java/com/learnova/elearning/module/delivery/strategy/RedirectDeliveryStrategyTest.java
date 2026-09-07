package com.learnova.elearning.module.delivery.strategy;

import com.learnova.elearning.integration.storage.StorageService;
import com.learnova.elearning.module.course.entity.Lesson;
import com.learnova.elearning.module.delivery.config.DeliveryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectDeliveryStrategyTest {

    @Mock
    private StorageService storageService;

    private DeliveryProperties deliveryProperties;
    private RedirectDeliveryStrategy strategy;

    @BeforeEach
    void setUp() {
        deliveryProperties = new DeliveryProperties();
        deliveryProperties.setStorageUrlTtl(Duration.ofMinutes(15));
        strategy = new RedirectDeliveryStrategy(storageService, deliveryProperties);
    }

    @Test
    void deliver_redirectsToPresignedUrlWithStorageUrlTtl() {
        Lesson lesson = Lesson.builder().id(1L).storageKey("courses/1/lessons/1/video.mp4").build();
        when(storageService.presignDownload("courses/1/lessons/1/video.mp4", Duration.ofMinutes(15)))
                .thenReturn("https://r2.example.com/signed?exp=123");

        ResponseEntity<?> response = strategy.deliver(lesson, "bytes=0-100");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("https://r2.example.com/signed?exp=123");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo(CacheControl.noStore().cachePrivate().getHeaderValue());
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        verify(storageService).presignDownload("courses/1/lessons/1/video.mp4", Duration.ofMinutes(15));
    }
}
