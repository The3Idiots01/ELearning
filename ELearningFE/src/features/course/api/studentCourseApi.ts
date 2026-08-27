import { apiClient } from '../../../lib/apiClient';
import type { CourseSummary, CourseDetail, Curriculum, EnrolledCourse } from '../../../types/course';
import { MOCK_COURSES, MOCK_CURRICULUM, MOCK_ENROLLED_COURSES } from '../../../lib/mockData';

export const studentCourseApi = {
  /**
   * Get public published courses with filter & search
   */
  getPublicCourses: async (params?: {
    categoryId?: number | null;
    level?: string;
    keyword?: string;
  }): Promise<CourseSummary[]> => {
    try {
      const queryParams: Record<string, any> = {};
      if (params?.categoryId) queryParams.categoryId = params.categoryId;
      if (params?.level && params.level !== 'ALL') queryParams.level = params.level;
      if (params?.keyword) queryParams.keyword = params.keyword;

      const data = await apiClient.get<any>('/api/v1/courses', {
        params: queryParams,
        skipAuth: true
      });

      const courses = data?.content || data?.elements || data;
      if (Array.isArray(courses) && courses.length > 0) {
        return courses;
      }
    } catch {
      // Backend not yet available or empty -> fallback to filtered mock
    }

    // Apply mock filtering
    let list = [...MOCK_COURSES];
    if (params?.categoryId) {
      list = list.filter((c) => c.categoryId === params.categoryId);
    }
    if (params?.level && params.level !== 'ALL') {
      list = list.filter((c) => c.level === params.level);
    }
    if (params?.keyword && params.keyword.trim()) {
      const kw = params.keyword.toLowerCase().trim();
      list = list.filter(
        (c) =>
          c.title.toLowerCase().includes(kw) ||
          c.description?.toLowerCase().includes(kw) ||
          c.subtitle?.toLowerCase().includes(kw)
      );
    }
    return list;
  },

  /**
   * Get Course Detail
   */
  getCourseDetail: async (courseId: number): Promise<CourseDetail> => {
    try {
      const data = await apiClient.get<CourseDetail>(`/api/v1/courses/${courseId}`, {
        skipAuth: true
      });
      if (data && data.id) {
        return data;
      }
    } catch {
      // Fallback
    }

    const mock = MOCK_COURSES.find((c) => c.id === Number(courseId));
    if (mock) return mock;
    return MOCK_COURSES[0];
  },

  /**
   * Get Course Curriculum for Student
   */
  getCurriculum: async (courseId: number): Promise<Curriculum> => {
    try {
      const data = await apiClient.get<Curriculum>(`/api/v1/courses/${courseId}/curriculum`);
      if (data && Array.isArray(data.sections) && data.sections.length > 0) {
        return data;
      }
    } catch {
      // Fallback
    }

    return MOCK_CURRICULUM[courseId] || MOCK_CURRICULUM[101];
  },

  /**
   * Enroll course
   */
  enrollCourse: async (courseId: number): Promise<any> => {
    try {
      return await apiClient.post(`/api/v1/courses/${courseId}/enroll`);
    } catch {
      // If BE endpoint not implemented yet, return successful simulated mock enrollment
      return { success: true, courseId, message: 'Đăng ký thành công (Mock)' };
    }
  },

  /**
   * Get student's enrolled courses
   */
  getEnrolledCourses: async (): Promise<EnrolledCourse[]> => {
    try {
      const data = await apiClient.get<EnrolledCourse[]>('/api/v1/courses/enrolled');
      if (Array.isArray(data)) {
        return data;
      }
    } catch {
      // Fallback
    }
    return MOCK_ENROLLED_COURSES;
  },

  /**
   * Mark lesson as completed
   */
  completeLesson: async (courseId: number, lessonId: number): Promise<any> => {
    try {
      return await apiClient.post(`/api/v1/courses/${courseId}/lessons/${lessonId}/complete`);
    } catch {
      // Fallback
      return { success: true, lessonId, completed: true };
    }
  }
};
