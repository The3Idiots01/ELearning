import { apiClient } from '../../../lib/apiClient';
import type { LearningOutcome } from '../../../types/course';

export const outcomeApi = {
  list: (courseId: number) =>
    apiClient.get<LearningOutcome[]>(`/api/v1/lecturer/courses/${courseId}/outcomes`),
  create: (courseId: number, statement: string) =>
    apiClient.post<LearningOutcome>(`/api/v1/lecturer/courses/${courseId}/outcomes`, { statement }),
  update: (courseId: number, outcomeId: number, statement: string) =>
    apiClient.patch<LearningOutcome>(
      `/api/v1/lecturer/courses/${courseId}/outcomes/${outcomeId}`,
      { statement }
    ),
  remove: (courseId: number, outcomeId: number) =>
    apiClient.delete<void>(`/api/v1/lecturer/courses/${courseId}/outcomes/${outcomeId}`),
  reorder: (courseId: number, outcomeIds: number[]) =>
    apiClient.put<LearningOutcome[]>(`/api/v1/lecturer/courses/${courseId}/outcomes/order`, {
      outcomeIds
    })
};
