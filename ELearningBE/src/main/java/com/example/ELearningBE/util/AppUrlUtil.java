package com.example.ELearningBE.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public final class AppUrlUtil {

    private AppUrlUtil() {}

    /**
     * Xác định Base URL linh hoạt:
     * 1. Ưu tiên cấu hình (APP_BASE_URL) nếu được thiết lập khi hosting / production
     * 2. Tự động nhận diện domain name, giao thức (HTTP/HTTPS), cổng từ HTTP Request (hỗ trợ qua Nginx / Cloudflare / Load Balancer)
     * 3. Fallback mặc định về localhost nếu không có request context
     */
    public static String resolveBaseUrl(String configuredBaseUrl, HttpServletRequest request) {
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
            return configuredBaseUrl.replaceAll("/+$", "");
        }

        if (request != null) {
            try {
                String uri = ServletUriComponentsBuilder.fromContextPath(request).build().toUriString();
                if (uri != null && !uri.isBlank()) {
                    return uri.replaceAll("/+$", "");
                }
            } catch (Exception ignored) {
                // Fallback to manual header inspection
            }

            String scheme = request.getHeader("X-Forwarded-Proto");
            if (scheme == null || scheme.isBlank()) {
                scheme = request.getScheme();
            }

            String host = request.getHeader("X-Forwarded-Host");
            if (host == null || host.isBlank()) {
                host = request.getHeader("Host");
                if (host == null || host.isBlank()) {
                    host = request.getServerName();
                    int port = request.getServerPort();
                    if ((scheme.equalsIgnoreCase("http") && port != 80) || (scheme.equalsIgnoreCase("https") && port != 443)) {
                        host += ":" + port;
                    }
                }
            }

            String contextPath = request.getContextPath();
            return (scheme + "://" + host + (contextPath != null ? contextPath : "")).replaceAll("/+$", "");
        }

        return "http://localhost:8080";
    }
}
