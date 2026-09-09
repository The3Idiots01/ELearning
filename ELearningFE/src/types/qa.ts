export interface QuestionAuthor {
  id: number;
  fullName: string;
  avatarUrl?: string | null;
  role: string;
}

export interface CourseAnswer {
  id: number;
  questionId: number;
  content: string;
  isInstructorReply: boolean;
  author: QuestionAuthor;
  createdAt: string;
  updatedAt?: string;
}

export interface CourseQuestion {
  id: number;
  courseId: number;
  lessonId: number;
  lessonTitle: string;
  title: string;
  content: string;
  author: QuestionAuthor;
  answersCount: number;
  hasInstructorReply: boolean;
  answers: CourseAnswer[];
  createdAt: string;
  updatedAt?: string;
}

export interface CreateQuestionPayload {
  title: string;
  content: string;
}

export interface CreateAnswerPayload {
  content: string;
}
