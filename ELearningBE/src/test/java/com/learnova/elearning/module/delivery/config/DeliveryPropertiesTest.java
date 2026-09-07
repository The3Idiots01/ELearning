package com.learnova.elearning.module.delivery.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryPropertiesTest {

    private DeliveryProperties withEnvironment(String... activeProfiles) {
        DeliveryProperties properties = new DeliveryProperties();
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(activeProfiles);
        properties.setEnvironment(environment);
        return properties;
    }

    @Test
    void validate_defaults_passInNonProdProfile() {
        DeliveryProperties properties = withEnvironment("dev");

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void validate_storageUrlTtlAboveFifteenMinutes_throws() {
        DeliveryProperties properties = withEnvironment("dev");
        properties.setStorageUrlTtl(Duration.ofMinutes(16));

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("storage-url-ttl");
    }

    @Test
    void validate_storageUrlTtlExactlyFifteenMinutes_passes() {
        DeliveryProperties properties = withEnvironment("dev");
        properties.setStorageUrlTtl(Duration.ofMinutes(15));

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void validate_ticketTtlLongerThanPlaybackSessionTtl_throws() {
        DeliveryProperties properties = withEnvironment("dev");
        properties.setTicketTtl(Duration.ofHours(9));
        properties.setPlaybackSessionTtl(Duration.ofHours(8));

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ticket-ttl");
    }

    @Test
    void validate_prodProfileWithDefaultSecret_throws() {
        DeliveryProperties properties = withEnvironment("prod");
        properties.setCookieSecure(true);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ticket-secret");
    }

    @Test
    void validate_prodProfileWithCookieSecureFalse_throws() {
        DeliveryProperties properties = withEnvironment("prod");
        properties.setTicketSecret("a-real-production-secret");
        properties.setCookieSecure(false);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cookie-secure");
    }

    @Test
    void validate_prodProfileWithProperSecretAndSecureCookie_passes() {
        DeliveryProperties properties = withEnvironment("prod");
        properties.setTicketSecret("a-real-production-secret");
        properties.setCookieSecure(true);

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }
}
