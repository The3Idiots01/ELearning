import { apiClient } from '../../../lib/apiClient';
import type {
  CourseQuestion,
  CourseAnswer,
  CreateQuestionPayload,
  CreateAnswerPayload
} from '../../../types/qa';

// Local storage key for fallback simulation
const QA_STORAGE_PREFIX = 'learnova_qa_';

function getLocalQuestions(courseId: number): CourseQuestion[] {
  try {
    const raw = localStorage.getItem(`${QA_STORAGE_PREFIX}${courseId}`);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveLocalQuestions(courseId: number, questions: CourseQuestion[]) {
  try {
    localStorage.setItem(`${QA_STORAGE_PREFIX}${courseId}`, JSON.stringify(questions));
  } catch {
    // Ignore storage quota
  }
}

export const qaApi = {
  /**
   * Lấy danh sách câu hỏi của một bài học
   */
  getLessonQuestions: async (courseId: number, lessonId: number): Promise<CourseQuestion[]> => {
    try {
      const data = await apiClient.get<CourseQuestion[]>(
        `/api/v1/courses/${courseId}/lessons/${lessonId}/questions`
      );
      if (Array.isArray(data)) {
        return data;
      }
    } catch {
      // Fallback
    }

    const locals = getLocalQuestions(courseId);
    return locals.filter((q) => q.lessonId === lessonId);
  },

  /**
   * Lấy toàn bộ câu hỏi của khóa học
   */
  getCourseQuestions: async (courseId: number): Promise<CourseQuestion[]> => {
    try {
      const data = await apiClient.get<CourseQuestion[]>(
        `/api/v1/courses/${courseId}/questions`
      );
      if (Array.isArray(data)) {
        return data;
      }
    } catch {
      // Fallback
    }

    return getLocalQuestions(courseId);
  },

  /**
   * Đặt câu hỏi mới cho một bài học
   */
  createQuestion: async (
    courseId: number,
    lessonId: number,
    lessonTitle: string,
    payload: CreateQuestionPayload,
    authorName: string = 'Học viên'
  ): Promise<CourseQuestion> => {
    try {
      const data = await apiClient.post<CourseQuestion>(
        `/api/v1/courses/${courseId}/lessons/${lessonId}/questions`,
        payload
      );
      if (data && data.id) {
        return data;
      }
    } catch (err: any) {
      // If error is 403 (unauthorized/not enrolled), propagate it
      if (err?.status === 403 || err?.response?.status === 403) {
        throw err;
      }
    }

    // Mock fallback creation
    const newQuestion: CourseQuestion = {
      id: Date.now(),
      courseId,
      lessonId,
      lessonTitle,
      title: payload.title,
      content: payload.content,
      author: {
        id: 999,
        fullName: authorName,
        role: 'LEARNER'
      },
      answersCount: 0,
      hasInstructorReply: false,
      answers: [],
      createdAt: new Date().toISOString()
    };

    const locals = getLocalQuestions(courseId);
    locals.unshift(newQuestion);
    saveLocalQuestions(courseId, locals);
    return newQuestion;
  },

  /**
   * Gửi phản hồi / trả lời câu hỏi
   */
  createAnswer: async (
    courseId: number,
    questionId: number,
    payload: CreateAnswerPayload,
    authorName: string = 'Bạn',
    isInstructor: boolean = false
  ): Promise<CourseAnswer> => {
    try {
      const data = await apiClient.post<CourseAnswer>(
        `/api/v1/courses/${courseId}/questions/${questionId}/answers`,
        payload
      );
      if (data && data.id) {
        return data;
      }
    } catch (err: any) {
      if (err?.status === 403 || err?.response?.status === 403) {
        throw err;
      }
    }

    // Fallback answer creation
    const newAnswer: CourseAnswer = {
      id: Date.now(),
      questionId,
      content: payload.content,
      isInstructorReply: isInstructor,
      author: {
        id: isInstructor ? 1 : 999,
        fullName: authorName,
        role: isInstructor ? 'LECTURER' : 'LEARNER'
      },
      createdAt: new Date().toISOString()
    };

    const locals = getLocalQuestions(courseId);
    const targetQ = locals.find((q) => q.id === questionId);
    if (targetQ) {
      targetQ.answers.push(newAnswer);
      targetQ.answersCount = targetQ.answers.length;
      if (isInstructor) targetQ.hasInstructorReply = true;
      saveLocalQuestions(courseId, locals);
    }

    return newAnswer;
  },

  /**
   * Xóa câu hỏi
   */
  deleteQuestion: async (courseId: number, questionId: number): Promise<void> => {
    try {
      await apiClient.delete(`/api/v1/courses/${courseId}/questions/${questionId}`);
    } catch (err: any) {
      if (err?.status === 401 || err?.status === 403) {
        throw err;
      }
    }

    const locals = getLocalQuestions(courseId);
    const updated = locals.filter((q) => q.id !== questionId);
    saveLocalQuestions(courseId, updated);
  },

  /**
   * Xóa câu trả lời / phản hồi
   */
  deleteAnswer: async (courseId: number, questionId: number, answerId: number): Promise<void> => {
    try {
      await apiClient.delete(
        `/api/v1/courses/${courseId}/questions/${questionId}/answers/${answerId}`
      );
    } catch (err: any) {
      if (err?.status === 401 || err?.status === 403) {
        throw err;
      }
    }

    const locals = getLocalQuestions(courseId);
    const targetQ = locals.find((q) => q.id === questionId);
    if (targetQ && targetQ.answers) {
      targetQ.answers = targetQ.answers.filter((a) => a.id !== answerId);
      targetQ.answersCount = targetQ.answers.length;
      targetQ.hasInstructorReply = targetQ.answers.some((a) => a.isInstructorReply);
      saveLocalQuestions(courseId, locals);
    }
  },

  /**
   * Giảng viên lấy toàn bộ câu hỏi của khóa học
   */
  getInstructorQuestions: async (
    courseId: number,
    filter: 'all' | 'unanswered' | 'answered' = 'all'
  ): Promise<CourseQuestion[]> => {
    try {
      const data = await apiClient.get<CourseQuestion[]>(
        `/api/v1/lecturer/courses/${courseId}/questions`,
        { params: { filter } }
      );
      if (Array.isArray(data)) {
        return data;
      }
    } catch {
      // Fallback
    }

    const locals = getLocalQuestions(courseId);
    if (filter === 'unanswered') {
      return locals.filter((q) => !q.hasInstructorReply);
    } else if (filter === 'answered') {
      return locals.filter((q) => q.hasInstructorReply);
    }
    return locals;
  }
};
