package com.learnova.elearning.module.course.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ngưỡng điều kiện publish (kiểu Udemy) — để ngoài code cho dễ chỉnh khi demo.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "learnova.course.publish")
public class CoursePublishProperties {

    private int minDescriptionLength = 200;
    private int minObjectives = 4;
    private int minRequirements = 1;
    private int minAudiences = 1;
}
