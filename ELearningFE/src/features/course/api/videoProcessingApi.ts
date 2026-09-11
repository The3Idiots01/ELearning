import { apiClient } from '../../../lib/apiClient';
import type { LessonUploadStatus } from '../../../types/course';
export interface VideoProcessingStatus {
  lessonId: number; uploadStatus: LessonUploadStatus; durationSeconds: number;
  updatedAt: string; errorCode: string | null; canRetry: boolean;
  pendingUploadStatus?: LessonUploadStatus | null;
}
export const videoProcessingApi = {
  list: (courseId: number, signal?: AbortSignal) => apiClient.get<VideoProcessingStatus[]>(
    '/api/v1/lecturer/courses/' + courseId + '/video-processing', { signal }),
  retry: (courseId: number, lessonId: number) => apiClient.post<void>(
    '/api/v1/lecturer/courses/' + courseId + '/lessons/' + lessonId + '/content/reprocess')
  ,
  cancelPending: (courseId: number, lessonId: number) => apiClient.delete<void>(
    '/api/v1/lecturer/courses/' + courseId + '/lessons/' + lessonId + '/pending-content')
};
