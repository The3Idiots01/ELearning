package com.learnova.elearning.module.recommendation.dto.request;

import com.learnova.elearning.module.course.entity.enums.CourseLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationRequest {

    /** Mục tiêu học tập của người học (VD: "Trở thành lập trình viên Spring Boot backend") */
    private String goal;

    /** Danh sách chủ đề hoặc công nghệ quan tâm (VD: ["Java", "Docker", "Database"]) */
    private List<String> interests;

    /** Trình độ mong muốn hoặc hiện tại của người học (BEGINNER, INTERMEDIATE, EXPERT) */
    private CourseLevel preferredLevel;

    /** Tùy chọn giới hạn trong một danh mục cụ thể */
    private Long targetCategoryId;
}
