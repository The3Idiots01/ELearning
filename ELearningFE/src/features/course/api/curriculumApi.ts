import { apiClient } from '../../../lib/apiClient';
import type {
  AddLessonResourceRequest,
  Assessment,
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

  deleteSection: async (courseId: number, sectionId: number, confirm = false): Promise<void> => {
    return apiClient.delete(`/api/v1/lecturer/courses/${courseId}/sections/${sectionId}`, { params: { confirm } });
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

  deleteLesson: async (courseId: number, lessonId: number, confirm = false): Promise<void> => {
    return apiClient.delete(`/api/v1/lecturer/courses/${courseId}/lessons/${lessonId}`, { params: { confirm } });
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

  listAssessments: async (courseId: number): Promise<Assessment[]> => {
    return apiClient.get<Assessment[]>(`/api/v1/lecturer/courses/${courseId}/assessments`);
  },

  addAssessment: async (
    courseId: number,
    data: { type: 'QUIZ'; title: string; instructions?: string; outcomeIds: number[] }
  ): Promise<Assessment> => {
    return apiClient.post<Assessment>(`/api/v1/lecturer/courses/${courseId}/assessments`, data);
  },

  updateAssessment: async (
    courseId: number,
    assessmentId: number,
    data: { title?: string; instructions?: string; outcomeIds?: number[] }
  ): Promise<Assessment> => {
    return apiClient.patch<Assessment>(
      `/api/v1/lecturer/courses/${courseId}/assessments/${assessmentId}`,
      data
    );
  },

  deleteAssessment: async (courseId: number, assessmentId: number, confirm = false): Promise<void> => {
    return apiClient.delete(`/api/v1/lecturer/courses/${courseId}/assessments/${assessmentId}`, { params: { confirm } });
  },

  placeAssessment: async (
    courseId: number,
    assessmentId: number,
    sectionId: number | null,
    position = 0
  ): Promise<Assessment> => {
    return apiClient.patch<Assessment>(
      `/api/v1/lecturer/courses/${courseId}/assessments/${assessmentId}/placement`,
      { sectionId, position }
    );
  },

  reorderAssessments: async (
    courseId: number,
    sectionId: number,
    assessmentIds: number[]
  ): Promise<Assessment[]> => {
    return apiClient.put<Assessment[]>(
      `/api/v1/lecturer/courses/${courseId}/sections/${sectionId}/assessments/order`,
      { assessmentIds }
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
