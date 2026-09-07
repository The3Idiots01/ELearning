package com.learnova.elearning.module.delivery.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.util.Arrays;

/**
 * Cấu hình Content Delivery (US-15). §4.10 design_us15_us17.md.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "learnova.delivery")
public class DeliveryProperties {

    public static final String DEFAULT_TICKET_SECRET = "learnova-delivery-dev-secret-change-me";
    private static final Duration MAX_STORAGE_URL_TTL = Duration.ofMinutes(15);
    private static final String PROD_PROFILE = "prod";

    /** Secret ký PlaybackTicket — tách riêng khỏi jwt.secret và storage.local.signing-secret. */
    private String ticketSecret = DEFAULT_TICKET_SECRET;

    /** TTL của ticket — dài hơn buổi học để tránh swap <video src> giữa chừng (ADR-01b). */
    private Duration ticketTtl = Duration.ofHours(8);

    /** TTL của presigned URL/URL trực tiếp phục vụ nội dung — đây là "signed URL" mà BR-25 nói tới. */
    private Duration storageUrlTtl = Duration.ofMinutes(15);

    /** TTL của playback session (cookie lv_pb) — ticket không nên sống lâu hơn session. */
    private Duration playbackSessionTtl = Duration.ofHours(8);

    /** Số phiên phát đồng thời tối đa mỗi user — giảm nhẹ chia sẻ tài khoản (T5, §4.9). */
    private int maxConcurrentStreams = 3;

    /** Tên cookie ràng buộc người xem (§4.5). */
    private String cookieName = "lv_pb";

    /** SameSite của cookie playback. */
    private String cookieSameSite = "Lax";

    /** Secure flag của cookie playback — bắt buộc true ở profile prod. */
    private boolean cookieSecure = false;

    @Autowired
    private Environment environment;

    @PostConstruct
    public void validate() {
        if (storageUrlTtl.compareTo(MAX_STORAGE_URL_TTL) > 0) {
            throw new IllegalStateException(
                    "learnova.delivery.storage-url-ttl phải <= 15 phút (BR-25), hiện là " + storageUrlTtl);
        }
        if (ticketTtl.compareTo(playbackSessionTtl) > 0) {
            throw new IllegalStateException(
                    "learnova.delivery.ticket-ttl phải <= playback-session-ttl, hiện là "
                            + ticketTtl + " > " + playbackSessionTtl);
        }
        if (isProdProfile()) {
            if (DEFAULT_TICKET_SECRET.equals(ticketSecret)) {
                throw new IllegalStateException(
                        "learnova.delivery.ticket-secret không được giữ giá trị mặc định ở profile prod");
            }
            if (!cookieSecure) {
                throw new IllegalStateException(
                        "learnova.delivery.cookie-secure phải là true ở profile prod");
            }
        }
    }

    private boolean isProdProfile() {
        return environment != null && Arrays.asList(environment.getActiveProfiles()).contains(PROD_PROFILE);
    }
}
