import { apiClient } from '../../../lib/apiClient';
import type {
  CourseReview,
  CourseReviewSummary,
  InstructorReview,
  CreateOrUpdateReviewPayload,
  PaginatedReviews,
  CourseReviewEligibility
} from '../../../types/review';

export const reviewApi = {
  /**
   * Lấy danh sách đánh giá của khóa học (công khai, có phân trang, lọc theo số sao và sắp xếp)
   */
  getCourseReviews: async (
    courseId: number,
    page: number = 0,
    size: number = 10,
    rating?: number,
    sort: 'newest' | 'highest' | 'lowest' = 'newest'
  ): Promise<PaginatedReviews<CourseReview>> => {
    return apiClient.get<PaginatedReviews<CourseReview>>(
      `/api/v1/courses/${courseId}/reviews`,
      {
        params: {
          page,
          size,
          rating: rating && rating > 0 ? rating : undefined,
          sort
        }
      }
    );
  },

  /**
   * Lấy thống kê phân bổ đánh giá (điểm trung bình, số lượng từng mức 1-5 sao, % từng mức)
   */
  getCourseReviewSummary: async (courseId: number): Promise<CourseReviewSummary> => {
    return apiClient.get<CourseReviewSummary>(`/api/v1/courses/${courseId}/reviews/summary`);
  },

  /**
   * Lấy trạng thái và điều kiện đánh giá của học viên hiện tại (tiến độ hiện tại, tiến độ yêu cầu 20%, canReview, review đã có)
   */
  getMyReviewStatus: async (courseId: number): Promise<CourseReviewEligibility | null> => {
    try {
      return await apiClient.get<CourseReviewEligibility>(`/api/v1/courses/${courseId}/reviews/my-status`);
    } catch {
      return null;
    }
  },

  /**
   * Lấy đánh giá của chính học viên hiện tại (nếu có)
   */
  getMyReview: async (courseId: number): Promise<CourseReview | null> => {
    try {
      const data = await apiClient.get<CourseReviewEligibility>(`/api/v1/courses/${courseId}/reviews/my-status`);
      return data?.review || null;
    } catch {
      return null;
    }
  },

  /**
   * Gửi hoặc cập nhật đánh giá của học viên
   */
  submitReview: async (
    courseId: number,
    payload: CreateOrUpdateReviewPayload
  ): Promise<CourseReview> => {
    return apiClient.post<CourseReview>(
      `/api/v1/courses/${courseId}/reviews/my-review`,
      payload
    );
  },

  /**
   * Xóa đánh giá của chính học viên (Soft delete & tính lại điểm)
   */
  deleteMyReview: async (courseId: number): Promise<void> => {
    await apiClient.delete<void>(`/api/v1/courses/${courseId}/reviews/my-review`);
  },

  /**
   * Dành cho Giảng viên: Xem danh sách đánh giá của học viên (Chế độ Read-only, phân trang & lọc sao)
   */
  getInstructorReviews: async (
    courseId: number,
    page: number = 0,
    size: number = 10,
    rating?: number
  ): Promise<PaginatedReviews<InstructorReview>> => {
    return apiClient.get<PaginatedReviews<InstructorReview>>(
      `/api/v1/lecturer/courses/${courseId}/reviews`,
      {
        params: {
          page,
          size,
          rating: rating && rating > 0 ? rating : undefined
        }
      }
    );
  },

  /**
   * Dành cho Giảng viên: Xem thống kê đánh giá của khóa học
   */
  getInstructorReviewSummary: async (courseId: number): Promise<CourseReviewSummary> => {
    return apiClient.get<CourseReviewSummary>(
      `/api/v1/lecturer/courses/${courseId}/reviews/summary`
    );
  }
};
