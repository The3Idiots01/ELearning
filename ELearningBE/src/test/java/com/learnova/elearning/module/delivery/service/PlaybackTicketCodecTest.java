package com.learnova.elearning.module.delivery.service;

import com.learnova.elearning.common.exception.AppException;
import com.learnova.elearning.common.exception.ErrorCode;
import com.learnova.elearning.module.delivery.config.DeliveryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaybackTicketCodecTest {

    private PlaybackTicketCodec codec;

    @BeforeEach
    void setUp() {
        DeliveryProperties properties = new DeliveryProperties();
        properties.setTicketSecret("unit-test-secret");
        codec = new PlaybackTicketCodec(properties);
    }

    @Test
    void encodeThenDecode_roundTripsAllClaims() {
        long exp = Instant.now().plusSeconds(900).getEpochSecond();

        String ticket = codec.encode(7L, 42L, AccessScope.ENROLLED, exp);
        PlaybackTicketClaims claims = codec.decode(ticket);

        assertThat(claims.userId()).isEqualTo(7L);
        assertThat(claims.lessonId()).isEqualTo(42L);
        assertThat(claims.scope()).isEqualTo(AccessScope.ENROLLED);
        assertThat(claims.exp()).isEqualTo(exp);
        assertThat(claims.jti()).isNotBlank();
        assertThat(claims.isExpired()).isFalse();
        assertThat(claims.isGuest()).isFalse();
    }

    @Test
    void encode_guestPreviewTicket_userIdZeroIsGuest() {
        long exp = Instant.now().plusSeconds(900).getEpochSecond();

        String ticket = codec.encode(0L, 42L, AccessScope.PREVIEW, exp);
        PlaybackTicketClaims claims = codec.decode(ticket);

        assertThat(claims.isGuest()).isTrue();
    }

    @Test
    void encode_twoCallsSameArgs_produceDifferentJti() {
        long exp = Instant.now().plusSeconds(900).getEpochSecond();

        String first = codec.encode(7L, 42L, AccessScope.ENROLLED, exp);
        String second = codec.encode(7L, 42L, AccessScope.ENROLLED, exp);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void decode_tamperedSignature_throwsInvalid() {
        long exp = Instant.now().plusSeconds(900).getEpochSecond();
        String ticket = codec.encode(7L, 42L, AccessScope.ENROLLED, exp);
        String tampered = ticket.substring(0, ticket.length() - 1)
                + (ticket.charAt(ticket.length() - 1) == 'a' ? 'b' : 'a');

        assertThatThrownBy(() -> codec.decode(tampered))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAYBACK_TICKET_INVALID);
    }

    @Test
    void decode_tamperedLessonIdInPayload_signatureNoLongerMatches() {
        long exp = Instant.now().plusSeconds(900).getEpochSecond();
        String ticket = codec.encode(7L, 42L, AccessScope.ENROLLED, exp);
        int dot = ticket.lastIndexOf('.');
        String encodedCanonical = ticket.substring(0, dot);
        String signature = ticket.substring(dot + 1);

        String canonical = new String(Base64.getUrlDecoder().decode(encodedCanonical), StandardCharsets.UTF_8);
        String forgedCanonical = canonical.replaceFirst("\\|42\\|", "|999|");
        String forgedTicket = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(forgedCanonical.getBytes(StandardCharsets.UTF_8)) + "." + signature;

        assertThatThrownBy(() -> codec.decode(forgedTicket))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAYBACK_TICKET_INVALID);
    }

    @Test
    void decode_expiredTicket_throwsExpired() {
        long exp = Instant.now().minusSeconds(1).getEpochSecond();
        String ticket = codec.encode(7L, 42L, AccessScope.ENROLLED, exp);

        assertThatThrownBy(() -> codec.decode(ticket))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAYBACK_TICKET_EXPIRED);
    }

    @Test
    void decode_blankOrNull_throwsInvalid() {
        assertThatThrownBy(() -> codec.decode(""))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAYBACK_TICKET_INVALID);

        assertThatThrownBy(() -> codec.decode(null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAYBACK_TICKET_INVALID);
    }

    @Test
    void decode_missingDotSeparator_throwsInvalid() {
        assertThatThrownBy(() -> codec.decode("not-a-valid-ticket"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAYBACK_TICKET_INVALID);
    }

    @Test
    void decode_malformedBase64Canonical_throwsInvalid() {
        assertThatThrownBy(() -> codec.decode("not base64!!.sig"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAYBACK_TICKET_INVALID);
    }

    @Test
    void decode_signedWithDifferentSecret_throwsInvalid() {
        long exp = Instant.now().plusSeconds(900).getEpochSecond();
        DeliveryProperties otherProperties = new DeliveryProperties();
        otherProperties.setTicketSecret("a-completely-different-secret");
        PlaybackTicketCodec otherCodec = new PlaybackTicketCodec(otherProperties);

        String ticket = otherCodec.encode(7L, 42L, AccessScope.ENROLLED, exp);

        assertThatThrownBy(() -> codec.decode(ticket))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAYBACK_TICKET_INVALID);
    }
}
