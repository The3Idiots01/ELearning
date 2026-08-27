import { apiClient } from '../../../lib/apiClient';
import type { PageResponse } from '../../../types/common';
import type {
  CourseDetail,
  CourseStatus,
  CourseSummary,
  CreateCourseRequest,
  PresignUploadRequest,
  PresignUploadResponse,
  PublishCheckResponse,
  StatusLog,
  UpdateBulletsRequest,
  UpdateCourseRequest,
  UpdatePriceRequest,
  UpdateThumbnailRequest
} from '../../../types/course';

export const instructorCourseApi = {
  /**
   * 1. Create a new DRAFT course (US-05)
   */
  createCourse: async (data: CreateCourseRequest): Promise<CourseDetail> => {
    return apiClient.post<CourseDetail>('/api/v1/lecturer/courses', data);
  },

  /**
   * 2. List instructor's courses with pagination & filter
   */
  listMine: async (params?: {
    status?: CourseStatus | '';
    keyword?: string;
    page?: number;
    size?: number;
  }): Promise<PageResponse<CourseSummary> | CourseSummary[]> => {
    const res = await apiClient.get<any>('/api/v1/lecturer/courses', {
      params: {
        status: params?.status || undefined,
        keyword: params?.keyword || undefined,
        page: params?.page ?? 0,
        size: params?.size ?? 20
      }
    });

    if (Array.isArray(res)) return res;
    if (res?.content) return res;
    if (res?.elements) return res;
    return [];
  },

  /**
   * 3. Get Course Full Detail
   */
  getDetail: async (courseId: number): Promise<CourseDetail> => {
    return apiClient.get<CourseDetail>(`/api/v1/lecturer/courses/${courseId}`);
  },

  /**
   * 4. Update Course Landing Page (title, subtitle, description, level, language, categoryId)
   */
  updateCourse: async (courseId: number, data: UpdateCourseRequest): Promise<CourseDetail> => {
    return apiClient.patch<CourseDetail>(`/api/v1/lecturer/courses/${courseId}`, data);
  },

  /**
   * 5. Set Course Price (0 - 10,000,000 VND, BR-04)
   */
  updatePrice: async (courseId: number, data: UpdatePriceRequest): Promise<CourseDetail> => {
    return apiClient.put<CourseDetail>(`/api/v1/lecturer/courses/${courseId}/price`, data);
  },

  /**
   * 6. Set 3 Highlight Bullets (learningObjectives >= 4, requirements >= 1, targetAudiences >= 1)
   */
  updateBullets: async (courseId: number, data: UpdateBulletsRequest): Promise<CourseDetail> => {
    return apiClient.put<CourseDetail>(`/api/v1/lecturer/courses/${courseId}/bullets`, data);
  },

  /**
   * 7. Upload & Attach Thumbnail
   */
  updateThumbnail: async (courseId: number, data: UpdateThumbnailRequest): Promise<CourseDetail> => {
    return apiClient.put<CourseDetail>(`/api/v1/lecturer/courses/${courseId}/thumbnail`, data);
  },

  /**
   * 8. Delete Course
   */
  deleteCourse: async (courseId: number): Promise<void> => {
    return apiClient.delete(`/api/v1/lecturer/courses/${courseId}`);
  },

  /**
   * 9. Presign Upload URL for thumbnail/video/file/resources
   */
  presignUpload: async (data: PresignUploadRequest): Promise<PresignUploadResponse> => {
    return apiClient.post<PresignUploadResponse>('/api/v1/lecturer/uploads/presign', data);
  },

  /**
   * 10. Check Publish Requirements
   */
  publishCheck: async (courseId: number): Promise<PublishCheckResponse> => {
    return apiClient.get<PublishCheckResponse>(`/api/v1/lecturer/courses/${courseId}/publish-check`);
  },

  /**
   * 11. Publish Course
   */
  publish: async (courseId: number): Promise<CourseDetail> => {
    return apiClient.post<CourseDetail>(`/api/v1/lecturer/courses/${courseId}/publish`);
  },

  /**
   * 12. Unpublish Course
   */
  unpublish: async (courseId: number, reason?: string): Promise<CourseDetail> => {
    return apiClient.post<CourseDetail>(`/api/v1/lecturer/courses/${courseId}/unpublish`, { reason });
  },

  /**
   * 13. Get Course Status Logs
   */
  getStatusLogs: async (courseId: number): Promise<StatusLog[]> => {
    return apiClient.get<StatusLog[]>(`/api/v1/lecturer/courses/${courseId}/status-logs`);
  }
};
