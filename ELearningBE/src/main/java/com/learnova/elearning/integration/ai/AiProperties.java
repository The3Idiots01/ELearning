package com.learnova.elearning.integration.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "learnova.ai")
public class AiProperties {
    private boolean enabled = true;
    private String apiKey = "";
    private String model = "gemini-3.8-flash";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private int maxInputChars = 60_000;
    private long maxDocumentBytes = 20L * 1024 * 1024;
}
