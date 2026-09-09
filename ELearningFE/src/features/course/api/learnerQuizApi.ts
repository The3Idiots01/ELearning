import { apiClient } from '../../../lib/apiClient';
import type {
  QuizAttemptResult,
  QuizTakingInfo,
  SubmitQuizAttemptPayload
} from '../../../types/quiz';

export const learnerQuizApi = {
  /**
   * 1. Lấy đề thi Quiz dành cho học viên (đã loại bỏ đáp án đúng).
   */
  getQuizForTaking: async (
    courseId: number,
    lessonId: number
  ): Promise<QuizTakingInfo> => {
    return apiClient.get<QuizTakingInfo>(
      `/api/v1/learner/courses/${courseId}/lessons/${lessonId}/quiz`
    );
  },

  /**
   * 2. Nộp bài làm trắc nghiệm để server chấm điểm tự động.
   */
  submitAttempt: async (
    courseId: number,
    lessonId: number,
    payload: SubmitQuizAttemptPayload
  ): Promise<QuizAttemptResult> => {
    return apiClient.post<QuizAttemptResult>(
      `/api/v1/learner/courses/${courseId}/lessons/${lessonId}/quiz/attempts`,
      payload
    );
  },

  /**
   * 3. Lấy lịch sử các lần làm bài của học viên đối với bài thi này.
   */
  getAttemptHistory: async (
    courseId: number,
    lessonId: number
  ): Promise<QuizAttemptResult[]> => {
    return apiClient.get<QuizAttemptResult[]>(
      `/api/v1/learner/courses/${courseId}/lessons/${lessonId}/quiz/attempts`
    );
  }
};
