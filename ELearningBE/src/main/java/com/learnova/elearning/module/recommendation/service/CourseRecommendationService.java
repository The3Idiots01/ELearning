package com.learnova.elearning.module.recommendation.service;

import com.learnova.elearning.module.recommendation.dto.request.AiRecommendationRequest;
import com.learnova.elearning.module.recommendation.dto.response.AiRecommendationResponse;
import com.learnova.elearning.module.recommendation.dto.response.ContinuousRecommendationResponse;

public interface CourseRecommendationService {

    /**
     * Đề xuất liên tục Top 5 khóa học dựa trên logic hệ thống (Category, Cấp độ, Rating, Độ phổ biến).
     *
     * @param courseId   ID khóa học đang xem (nếu có, để gợi ý khóa học liên quan)
     * @param categoryId ID danh mục tùy chọn để lọc
     * @param learnerId  ID học viên đã đăng nhập (nếu có, để loại trừ khóa đã mua và cá nhân hóa)
     * @return Top 5 khóa học phù hợp nhất
     */
    ContinuousRecommendationResponse getContinuousRecommendations(Long courseId, Long categoryId, Long learnerId);

    /**
     * Đề xuất Top 3 khóa học sử dụng AI Gemini phân tích mục tiêu và ngữ cảnh học tập.
     * Tự động fallback về logic hệ thống nếu AI chạm rate limit hoặc gặp sự cố.
     *
     * @param request   Mục tiêu, sở thích, cấp độ quan tâm của học viên
     * @param learnerId ID học viên đã đăng nhập (nếu có)
     * @return Top 3 khóa học kèm điểm match và giải thích chi tiết
     */
    AiRecommendationResponse getAiRecommendations(AiRecommendationRequest request, Long learnerId);

    /**
     * Đề xuất nhanh Top 3 bằng AI dựa trên lịch sử học tập thực tế của học viên đã đăng nhập.
     *
     * @param learnerId ID học viên đã xác thực
     * @return Top 3 khóa học gợi ý tiếp theo
     */
    AiRecommendationResponse getQuickAiRecommendations(Long learnerId);

    /**
     * Lấy kết quả đề xuất AI gần nhất đã lưu trong Database của học viên.
     *
     * @param learnerId ID học viên đã đăng nhập
     * @return Đề xuất AI gần nhất hoặc null nếu chưa có
     */
    AiRecommendationResponse getLatestAiRecommendation(Long learnerId);
}
