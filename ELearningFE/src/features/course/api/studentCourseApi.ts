import { apiClient } from '../../../lib/apiClient';
import type { CourseSummary, CourseDetail, Curriculum, EnrolledCourse } from '../../../types/course';

export const studentCourseApi = {
  /**
   * Get public published courses with filter & search
   */
  getPublicCourses: async (params?: {
    categoryId?: number | null;
    level?: string;
    keyword?: string;
  }): Promise<CourseSummary[]> => {
    const queryParams: Record<string, any> = {};
    if (params?.categoryId) queryParams.categoryId = params.categoryId;
    if (params?.level && params.level !== 'ALL') queryParams.level = params.level;
    if (params?.keyword) queryParams.keyword = params.keyword;

    const data = await apiClient.get<any>('/api/v1/courses', {
      params: queryParams
    });

    const courses = data?.content || data?.elements || data;
    return Array.isArray(courses) ? courses : [];
  },

  /**
   * Get Course Detail
   */
  getCourseDetail: async (courseId: number): Promise<CourseDetail> => {
    return apiClient.get<CourseDetail>(`/api/v1/courses/${courseId}`, {
      skipAuth: true
    });
  },

  /**
   * Get Course Curriculum for Student
   */
  getCurriculum: async (courseId: number): Promise<Curriculum> => {
    return apiClient.get<Curriculum>(`/api/v1/courses/${courseId}/curriculum`);
  },

  /**
   * Enroll course
   */
  enrollCourse: async (courseId: number): Promise<any> => {
    return apiClient.post(`/api/v1/courses/${courseId}/enroll`);
  },

  /**
   * Get student's enrolled courses
   */
  getEnrolledCourses: async (): Promise<EnrolledCourse[]> => {
    return apiClient.get<EnrolledCourse[]>('/api/v1/courses/enrolled');
  },

  /**
   * Mark lesson as completed
   */
  completeLesson: async (courseId: number, lessonId: number): Promise<any> => {
    return apiClient.post(`/api/v1/courses/${courseId}/lessons/${lessonId}/complete`);
  }
};
