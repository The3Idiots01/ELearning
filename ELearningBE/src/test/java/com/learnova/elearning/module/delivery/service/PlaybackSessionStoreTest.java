package com.learnova.elearning.module.delivery.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.delivery.config.DeliveryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaybackSessionStoreTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private DeliveryProperties properties;
    private PlaybackSessionStore store;

    @BeforeEach
    void setUp() {
        properties = new DeliveryProperties();
        properties.setPlaybackSessionTtl(Duration.ofHours(8));
        properties.setMaxConcurrentStreams(3);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        store = new PlaybackSessionStore(redisTemplate, properties);
    }

    @Test
    void issueSession_storesDataAndReturnsNonBlankSessionId() {
        String sessionId = store.issueSession(7L, "Mozilla/5.0");

        assertThat(sessionId).isNotBlank();
        verify(valueOperations).set(any(), any(PlaybackSessionData.class), org.mockito.ArgumentMatchers.eq(Duration.ofHours(8)));
    }

    @Test
    void issueSession_redisDown_returnsNullInsteadOfThrowing() {
        doThrow(new QueryTimeoutException("redis down"))
                .when(valueOperations).set(any(), any(), any(Duration.class));

        String sessionId = store.issueSession(7L, "Mozilla/5.0");

        assertThat(sessionId).isNull();
    }

    @Test
    void matches_sameUserId_returnsTrueAndRefreshesTtl() {
        PlaybackSessionData data = new PlaybackSessionData(7L, System.currentTimeMillis(), "hash");
        when(valueOperations.get("pb:sess:abc")).thenReturn(data);

        boolean result = store.matches("abc", 7L);

        assertThat(result).isTrue();
        verify(redisTemplate).expire("pb:sess:abc", Duration.ofHours(8));
    }

    @Test
    void matches_differentUserId_returnsFalse() {
        PlaybackSessionData data = new PlaybackSessionData(7L, System.currentTimeMillis(), "hash");
        when(valueOperations.get("pb:sess:abc")).thenReturn(data);

        boolean result = store.matches("abc", 999L);

        assertThat(result).isFalse();
    }

    @Test
    void matches_unknownSession_returnsFalse() {
        when(valueOperations.get("pb:sess:missing")).thenReturn(null);

        boolean result = store.matches("missing", 7L);

        assertThat(result).isFalse();
    }

    @Test
    void matches_blankSessionId_returnsFalseWithoutTouchingRedis() {
        assertThat(store.matches(null, 7L)).isFalse();
        assertThat(store.matches("", 7L)).isFalse();
    }

    @Test
    void matches_redisDown_degradesToTrue() {
        when(valueOperations.get(anyString())).thenThrow(new QueryTimeoutException("redis down"));

        boolean result = store.matches("abc", 7L);

        assertThat(result).isTrue();
    }

    @Test
    void assertConcurrencyAllowed_underLimit_doesNotThrow() {
        when(zSetOperations.range(anyString(), anyLong(), anyLong()))
                .thenReturn(Set.of("jti-1", "jti-2"));

        assertThatCode(() -> store.assertConcurrencyAllowed(7L, "jti-2"))
                .doesNotThrowAnyException();

        verify(zSetOperations).removeRangeByScore(anyString(), anyDouble(), anyDouble());
        verify(zSetOperations).add(anyString(), any(), anyDouble());
    }

    @Test
    void assertConcurrencyAllowed_overLimit_throwsTooManyActiveStreams() {
        when(zSetOperations.range(anyString(), anyLong(), anyLong()))
                .thenReturn(Set.of("jti-1", "jti-2", "jti-3", "jti-4"));

        assertThatThrownBy(() -> store.assertConcurrencyAllowed(7L, "jti-4"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_ACTIVE_STREAMS);
    }

    @Test
    void assertConcurrencyAllowed_redisDown_doesNotThrow() {
        doThrow(new QueryTimeoutException("redis down"))
                .when(zSetOperations).removeRangeByScore(anyString(), anyDouble(), anyDouble());

        assertThatCode(() -> store.assertConcurrencyAllowed(7L, "jti-1"))
                .doesNotThrowAnyException();
    }
}
