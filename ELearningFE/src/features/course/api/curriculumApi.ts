import { apiClient } from '../../../lib/apiClient';
import type {
  AddLessonResourceRequest,
  AttachLessonContentRequest,
  CreateLessonRequest,
  CreateSectionRequest,
  Curriculum,
  Lesson,
  Section,
  UpdateLessonRequest,
  UpdateSectionRequest
} from '../../../types/course';

export const curriculumApi = {
  /**
   * 1. Get Curriculum Tree
   */
  getCurriculum: async (courseId: number): Promise<Curriculum> => {
    return apiClient.get<Curriculum>(`/api/v1/lecturer/courses/${courseId}/curriculum`);
  },

  // ---------------------------------------------------------------------------
  // SECTIONS
  // ---------------------------------------------------------------------------
  addSection: async (courseId: number, data: CreateSectionRequest): Promise<Section> => {
    return apiClient.post<Section>(`/api/v1/lecturer/courses/${courseId}/sections`, data);
  },

  updateSection: async (
    courseId: number,
    sectionId: number,
    data: UpdateSectionRequest
  ): Promise<Section> => {
    return apiClient.patch<Section>(
      `/api/v1/lecturer/courses/${courseId}/sections/${sectionId}`,
      data
    );
  },

  deleteSection: async (courseId: number, sectionId: number): Promise<void> => {
    return apiClient.delete(`/api/v1/lecturer/courses/${courseId}/sections/${sectionId}`);
  },

  reorderSections: async (courseId: number, sectionIds: number[]): Promise<Curriculum> => {
    return apiClient.put<Curriculum>(`/api/v1/lecturer/courses/${courseId}/sections/order`, {
      sectionIds
    });
  },

  // ---------------------------------------------------------------------------
  // LESSONS
  // ---------------------------------------------------------------------------
  addLesson: async (
    courseId: number,
    sectionId: number,
    data: CreateLessonRequest
  ): Promise<Lesson> => {
    return apiClient.post<Lesson>(
      `/api/v1/lecturer/courses/${courseId}/sections/${sectionId}/lessons`,
      data
    );
  },

  updateLesson: async (
    courseId: number,
    lessonId: number,
    data: UpdateLessonRequest
  ): Promise<Lesson> => {
    return apiClient.patch<Lesson>(
      `/api/v1/lecturer/courses/${courseId}/lessons/${lessonId}`,
      data
    );
  },

  deleteLesson: async (courseId: number, lessonId: number): Promise<void> => {
    return apiClient.delete(`/api/v1/lecturer/courses/${courseId}/lessons/${lessonId}`);
  },

  reorderLessons: async (
    courseId: number,
    sectionId: number,
    lessonIds: number[]
  ): Promise<Curriculum> => {
    return apiClient.put<Curriculum>(
      `/api/v1/lecturer/courses/${courseId}/sections/${sectionId}/lessons/order`,
      { lessonIds }
    );
  },

  moveLesson: async (
    courseId: number,
    lessonId: number,
    targetSectionId: number,
    position: number
  ): Promise<Curriculum> => {
    return apiClient.patch<Curriculum>(
      `/api/v1/lecturer/courses/${courseId}/lessons/${lessonId}/move`,
      { targetSectionId, position }
    );
  },

  // ---------------------------------------------------------------------------
  // CONTENT & RESOURCES
  // ---------------------------------------------------------------------------
  attachContent: async (
    courseId: number,
    lessonId: number,
    data: AttachLessonContentRequest
  ): Promise<Lesson> => {
    return apiClient.put<Lesson>(
      `/api/v1/lecturer/courses/${courseId}/lessons/${lessonId}/content`,
      data
    );
  },

  removeContent: async (courseId: number, lessonId: number): Promise<Lesson> => {
    return apiClient.delete<Lesson>(
      `/api/v1/lecturer/courses/${courseId}/lessons/${lessonId}/content`
    );
  },

  addResource: async (
    courseId: number,
    lessonId: number,
    data: AddLessonResourceRequest
  ): Promise<Lesson> => {
    return apiClient.post<Lesson>(
      `/api/v1/lecturer/courses/${courseId}/lessons/${lessonId}/resources`,
      data
    );
  },

  deleteResource: async (
    courseId: number,
    lessonId: number,
    resourceId: number
  ): Promise<Lesson> => {
    return apiClient.delete<Lesson>(
      `/api/v1/lecturer/courses/${courseId}/lessons/${lessonId}/resources/${resourceId}`
    );
  }
};
