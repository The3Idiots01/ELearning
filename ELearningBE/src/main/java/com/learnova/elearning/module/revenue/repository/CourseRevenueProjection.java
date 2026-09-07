package com.learnova.elearning.module.revenue.repository;

import java.math.BigDecimal;

public interface CourseRevenueProjection {

    Long getCourseId();

    String getCourseTitle();

    BigDecimal getRevenue();
}
