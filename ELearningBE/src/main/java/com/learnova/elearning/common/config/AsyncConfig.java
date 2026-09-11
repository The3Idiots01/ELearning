package com.learnova.elearning.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Bật {@code @Async} — dùng cho job nền đo video duration (§7.5 design_us15_us17.md). */
@Configuration
@EnableAsync
@org.springframework.scheduling.annotation.EnableScheduling
public class AsyncConfig {
}
