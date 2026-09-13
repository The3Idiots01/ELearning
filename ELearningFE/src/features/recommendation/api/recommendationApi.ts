import { apiClient } from '../../../lib/apiClient';
import type {
  AiRecommendationRequest,
  AiRecommendationResponse,
  ContinuousRecommendationResponse
} from '../types/recommendation';

export const recommendationApi = {
  /**
   * Lấy Top 5 khóa học đề xuất liên tục từ hệ thống (0 token)
   * Tự động lọc theo courseId (nếu đang xem bài) hoặc categoryId
   */
  getContinuousRecommendations: async (params?: {
    courseId?: number;
    categoryId?: number | null;
  }): Promise<ContinuousRecommendationResponse> => {
    const queryParams: Record<string, any> = {};
    if (params?.courseId) queryParams.courseId = params.courseId;
    if (params?.categoryId) queryParams.categoryId = params.categoryId;

    return apiClient.get<ContinuousRecommendationResponse>('/api/v1/recommendations/continuous', {
      params: queryParams
    });
  },

  /**
   * Gọi Gemini AI phân tích mục tiêu & đề xuất Top 3 khóa học
   * Chỉ kích hoạt khi người dùng bấm nút phân tích
   */
  getAiRecommendations: async (
    payload: AiRecommendationRequest
  ): Promise<AiRecommendationResponse> => {
    return apiClient.post<AiRecommendationResponse>('/api/v1/recommendations/ai', payload);
  },

  /**
   * Gợi ý nhanh Top 3 bằng AI cho học viên đang đăng nhập dựa trên lịch sử khóa học
   */
  getQuickAiRecommendations: async (): Promise<AiRecommendationResponse> => {
    return apiClient.get<AiRecommendationResponse>('/api/v1/recommendations/ai/quick');
  },

  /**
   * Lấy kết quả đề xuất AI gần nhất đã lưu trong DB của học viên
   */
  getLatestAiRecommendation: async (): Promise<AiRecommendationResponse | null> => {
    try {
      const res = await apiClient.get<AiRecommendationResponse | null>('/api/v1/recommendations/ai/latest');
      return res || null;
    } catch {
      return null;
    }
  }
};
