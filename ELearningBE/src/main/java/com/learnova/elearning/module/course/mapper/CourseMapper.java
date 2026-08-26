package com.learnova.elearning.module.course.mapper;

import com.learnova.elearning.module.category.entity.Category;
import com.learnova.elearning.module.course.dto.response.CourseResponse;
import com.learnova.elearning.module.course.dto.response.CourseSummaryResponse;
import com.learnova.elearning.module.course.entity.Course;

import java.util.List;

/**
 * Mapper thuần (viết tay). thumbnailUrl và các nhóm bullet do service truyền vào
 * vì cần ký URL / gom từ bảng khác — mapper không phụ thuộc service/storage.
 */
public final class CourseMapper {

    private CourseMapper() {}

    public static CourseSummaryResponse toSummary(Course course, String thumbnailUrl) {
        Category category = course.getCategory();
        return CourseSummaryResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .subtitle(course.getSubtitle())
                .slug(course.getSlug())
                .status(course.getStatus())
                .price(course.getPrice())
                .thumbnailUrl(thumbnailUrl)
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getName() : null)
                .totalStudents(course.getTotalStudents())
                .ratingAvg(course.getRatingAvg())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    public static CourseResponse toDetail(Course course,
                                          String thumbnailUrl,
                                          List<String> learningObjectives,
                                          List<String> requirements,
                                          List<String> targetAudiences) {
        Category category = course.getCategory();
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .subtitle(course.getSubtitle())
                .slug(course.getSlug())
                .description(course.getDescription())
                .thumbnailUrl(thumbnailUrl)
                .language(course.getLanguage())
                .level(course.getLevel())
                .price(course.getPrice())
                .status(course.getStatus())
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getName() : null)
                .ratingAvg(course.getRatingAvg())
                .totalStudents(course.getTotalStudents())
                .version(course.getVersion())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .publishedAt(course.getPublishedAt())
                .learningObjectives(learningObjectives)
                .requirements(requirements)
                .targetAudiences(targetAudiences)
                .build();
    }
}
