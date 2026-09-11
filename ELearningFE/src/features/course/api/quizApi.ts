import { apiClient } from '../../../lib/apiClient';
import type {
  QuizDetail,
  QuizDraft,
  QuizQuestion,
  ApplyQuizDraftRequest,
  GenerateQuizDraftRequest,
  UpsertQuestionRequest,
  UpsertQuizRequest
} from '../../../types/quiz';

export const quizApi = {
  /**
   * 1. Lấy thông tin bài Quiz và danh sách câu hỏi kèm đáp án đúng (dành cho giảng viên).
   */
  getQuiz: async (courseId: number, assessmentId: number): Promise<QuizDetail> => {
    return apiClient.get<QuizDetail>(
      `/api/v1/lecturer/courses/${courseId}/assessments/${assessmentId}/quiz`
    );
  },

  /**
   * 2. Tạo mới hoặc cập nhật cấu hình chung của bài Quiz (tiêu đề, điểm đạt, số lần làm).
   */
  upsertQuiz: async (
    courseId: number,
    assessmentId: number,
    data: UpsertQuizRequest
  ): Promise<QuizDetail> => {
    return apiClient.put<QuizDetail>(
      `/api/v1/lecturer/courses/${courseId}/assessments/${assessmentId}/quiz`,
      data
    );
  },

  /**
   * 3. Thêm một câu hỏi mới vào bài Quiz.
   */
  addQuestion: async (
    courseId: number,
    assessmentId: number,
    data: UpsertQuestionRequest
  ): Promise<QuizQuestion> => {
    return apiClient.post<QuizQuestion>(
      `/api/v1/lecturer/courses/${courseId}/assessments/${assessmentId}/quiz/questions`,
      data
    );
  },

  /**
   * 4. Cập nhật nội dung câu hỏi hoặc danh sách đáp án.
   */
  updateQuestion: async (
    courseId: number,
    assessmentId: number,
    questionId: number,
    data: UpsertQuestionRequest
  ): Promise<QuizQuestion> => {
    return apiClient.put<QuizQuestion>(
      `/api/v1/lecturer/courses/${courseId}/assessments/${assessmentId}/quiz/questions/${questionId}`,
      data
    );
  },

  /**
   * 5. Xóa câu hỏi khỏi bài Quiz.
   */
  deleteQuestion: async (
    courseId: number,
    assessmentId: number,
    questionId: number
  ): Promise<void> => {
    return apiClient.delete<void>(
      `/api/v1/lecturer/courses/${courseId}/assessments/${assessmentId}/quiz/questions/${questionId}`
    );
  },

  /**
   * 6. Sắp xếp lại thứ tự các câu hỏi trong bài Quiz.
   */
  reorderQuestions: async (
    courseId: number,
    assessmentId: number,
    questionIds: number[]
  ): Promise<void> => {
    return apiClient.patch<void>(
      `/api/v1/lecturer/courses/${courseId}/assessments/${assessmentId}/quiz/questions/reorder`,
      { questionIds }
    );
  },

  generateDraft: async (
    courseId: number,
    assessmentId: number,
    data: GenerateQuizDraftRequest
  ): Promise<QuizDraft> => {
    return apiClient.post<QuizDraft>(
      `/api/v1/lecturer/courses/${courseId}/assessments/${assessmentId}/quiz/ai/draft`,
      data
    );
  },

  applyDraft: async (
    courseId: number,
    assessmentId: number,
    data: ApplyQuizDraftRequest
  ): Promise<QuizDetail> => {
    return apiClient.post<QuizDetail>(
      `/api/v1/lecturer/courses/${courseId}/assessments/${assessmentId}/quiz/ai/draft/apply`,
      data
    );
  }
};
