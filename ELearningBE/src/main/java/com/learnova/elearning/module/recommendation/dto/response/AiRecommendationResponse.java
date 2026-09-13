package com.learnova.elearning.module.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationResponse {

    /** Tối đa 3 khóa học phù hợp nhất do AI đề xuất */
    private List<RecommendedCourseResponse> items;

    /** Tên mô hình AI được sử dụng (VD: gemini-3.5-flash-lite) */
    private String modelUsed;

    /** Đánh dấu có phải kết quả fallback khi AI gặp sự cố hay không */
    private boolean fallback;

    /** Lời khuyên tổng quan hoặc lộ trình học tập do AI đề xuất */
    private String summaryAdvice;

    /** Mục tiêu học tập ban đầu nếu có */
    private String goal;

    /** Thời gian đề xuất được tạo/lưu trong DB */
    private java.time.Instant createdAt;
}
