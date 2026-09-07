export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE';

export interface QuizOption {
  id?: string;
  text: string;
  isCorrect?: boolean;
  explanation?: string;
}

export interface QuizQuestion {
  id: number;
  quizId: number;
  questionText: string;
  questionType: QuestionType;
  points: number;
  position: number;
  options: QuizOption[];
  createdAt?: string;
  updatedAt?: string;
}

export interface QuizDetail {
  id: number;
  lessonId: number;
  title: string;
  passingScore: number;
  maxAttempts?: number | null;
  totalPoints: number;
  questions: QuizQuestion[];
  createdAt?: string;
  updatedAt?: string;
}

export interface UpsertQuizRequest {
  title: string;
  passingScore: number;
  maxAttempts?: number | null;
}

export interface UpsertQuestionRequest {
  questionText: string;
  questionType: QuestionType;
  points: number;
  options: QuizOption[];
}

export interface ReorderQuestionsRequest {
  questionIds: number[];
}
