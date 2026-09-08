import { apiClient } from '../../../lib/apiClient';
import type { QuizAnswer, QuizAttempt, StudentQuiz } from '../../../types/quiz';

export const studentQuizApi = {
  getQuiz: (courseId: number, assessmentId: number) =>
    apiClient.get<StudentQuiz>(
      `/api/v1/learner/courses/${courseId}/assessments/${assessmentId}/quiz`
    ),
  submitAttempt: (courseId: number, assessmentId: number, answers: QuizAnswer[]) =>
    apiClient.post<QuizAttempt>(
      `/api/v1/learner/courses/${courseId}/assessments/${assessmentId}/quiz/attempts`,
      { answers }
    ),
  getHistory: (courseId: number, assessmentId: number) =>
    apiClient.get<QuizAttempt[]>(
      `/api/v1/learner/courses/${courseId}/assessments/${assessmentId}/quiz/attempts`
    )
};
