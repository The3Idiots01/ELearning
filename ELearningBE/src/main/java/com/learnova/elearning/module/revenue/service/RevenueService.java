package com.learnova.elearning.module.revenue.service;

import com.learnova.elearning.module.revenue.dto.CourseRevenueItem;
import com.learnova.elearning.module.revenue.dto.InstructorRevenueResponse;
import com.learnova.elearning.module.revenue.repository.CourseRevenueProjection;
import com.learnova.elearning.module.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final RevenueRepository revenueRepository;

    @Transactional(readOnly = true)
    public InstructorRevenueResponse getRevenue(Long lecturerId) {
        List<CourseRevenueItem> courses = revenueRepository.findCourseRevenue(lecturerId)
                .stream()
                .map(this::toItem)
                .toList();

        BigDecimal totalRevenue = courses.stream()
                .map(CourseRevenueItem::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return InstructorRevenueResponse.builder()
                .currency("VND")
                .totalRevenue(totalRevenue)
                .courses(courses)
                .build();
    }

    private CourseRevenueItem toItem(CourseRevenueProjection projection) {
        BigDecimal revenue = projection.getRevenue() == null
                ? BigDecimal.ZERO
                : projection.getRevenue();

        return CourseRevenueItem.builder()
                .courseId(projection.getCourseId())
                .courseTitle(projection.getCourseTitle())
                .revenue(revenue)
                .build();
    }
}
