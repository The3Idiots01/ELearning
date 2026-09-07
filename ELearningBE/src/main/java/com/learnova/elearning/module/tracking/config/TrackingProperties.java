package com.learnova.elearning.module.tracking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình Knowledge Tracking (US-17). §5.7 design_us15_us17.md.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "learnova.tracking")
public class TrackingProperties {

    /** Ngưỡng max_position_seconds / duration_seconds để tự hoàn thành lesson VIDEO — cấu hình, không hard-code. */
    private double videoCompletionRatio = 0.9;
}
