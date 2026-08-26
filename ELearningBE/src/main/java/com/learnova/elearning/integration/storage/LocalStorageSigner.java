package com.learnova.elearning.integration.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Ký/kiểm token cho endpoint dev storage — mô phỏng chữ ký + thời hạn của
 * presigned URL. Chỉ dùng cho môi trường dev (provider=local).
 */
@Component
@ConditionalOnProperty(name = "learnova.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageSigner {

    private static final String HMAC_ALGO = "HmacSHA256";

    private final byte[] secret;

    public LocalStorageSigner(StorageProperties properties) {
        this.secret = properties.getLocal().getSigningSecret().getBytes(StandardCharsets.UTF_8);
    }

    public String sign(String action, String key, long expEpochSeconds) {
        String payload = action + ":" + key + ":" + expEpochSeconds;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret, HMAC_ALGO));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign local storage token", e);
        }
    }

    public boolean verify(String action, String key, long expEpochSeconds, String signature) {
        if (signature == null || Instant.now().getEpochSecond() > expEpochSeconds) {
            return false;
        }
        String expected = sign(action, key, expEpochSeconds);
        return constantTimeEquals(expected, signature);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
