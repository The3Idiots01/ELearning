package com.learnova.elearning.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
@Slf4j
public class PayOSConfig {

    @Value("${payos.client-id:}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {
        String effectiveClientId = clientId != null && !clientId.isBlank() ? clientId.trim() : System.getenv("PAYOS_CLIENT_ID");
        String effectiveApiKey = apiKey != null && !apiKey.isBlank() ? apiKey.trim() : System.getenv("PAYOS_API_KEY");
        String effectiveChecksumKey = checksumKey != null && !checksumKey.isBlank() ? checksumKey.trim() : System.getenv("PAYOS_CHECKSUM_KEY");

        effectiveClientId = effectiveClientId != null ? effectiveClientId.trim() : "";
        effectiveApiKey = effectiveApiKey != null ? effectiveApiKey.trim() : "";
        effectiveChecksumKey = effectiveChecksumKey != null ? effectiveChecksumKey.trim() : "";

        if (effectiveClientId.isBlank() || effectiveChecksumKey.isBlank()) {
            log.warn("PayOS credentials are missing or empty! Check application.properties or environment variables.");
        } else {
            log.info("Initialized PayOS bean with Client ID: {}...", effectiveClientId.substring(0, Math.min(8, effectiveClientId.length())));
        }

        return new PayOS(effectiveClientId, effectiveApiKey, effectiveChecksumKey);
    }
}
