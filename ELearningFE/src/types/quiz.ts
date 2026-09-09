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
  assessmentId: number;
  title?: string;
  passingScore: number;
  maxAttempts?: number | null;
  totalPoints: number;
  questions: QuizQuestion[];
  createdAt?: string;
  updatedAt?: string;
}

export interface UpsertQuizRequest {
  title?: string;
  passingScore: number;
  maxAttempts?: number | null;
}

export interface UpsertQuestionRequest {
  questionText: string;
  questionType: QuestionType;
  points: number;
  options: QuizOption[];
}

export interface StudentQuizOption {
  id: string;
  text: string;
}

export interface StudentQuizQuestion {
  id: number;
  questionText: string;
  questionType: QuestionType;
  points: number;
  position: number;
  options: StudentQuizOption[];
}

export interface StudentQuiz {
  id: number;
  assessmentId: number;
  title: string;
  passingScore: number;
  maxAttempts?: number | null;
  attemptsUsed: number;
  attemptsRemaining?: number | null;
  hasPassed: boolean;
  questions: StudentQuizQuestion[];
}

export interface QuizAnswer {
  questionId: number;
  selectedOptionIds: string[];
}

export interface QuizAttempt {
  id: number;
  quizId: number;
  assessmentId: number;
  score: number;
  passingScore: number;
  isPassed: boolean;
  totalQuestions: number;
  correctQuestions: number;
  submittedAt: string;
  questionResults?: Array<{
    questionId: number;
    isCorrect: boolean;
    earnedPoints: number;
    totalPoints: number;
    selectedOptionIds?: string[];
    correctOptionIds?: string[];
    explanation?: string;
  }>;
}

export interface ReorderQuestionsRequest {
  questionIds: number[];
}

// ==========================================
// Learner Quiz Taking Types (US-20)
// ==========================================

export interface StudentOptionItem {
  id: string;
  text: string;
}

export interface QuestionTakingItem {
  id: number;
  questionText: string;
  questionType: QuestionType;
  points: number;
  position: number;
  options: StudentOptionItem[];
}

export interface QuizTakingInfo {
  id: number;
  assessmentId?: number;
  lessonId?: number;
  title: string;
  passingScore: number;
  maxAttempts?: number | null;
  attemptsUsed: number;
  attemptsRemaining?: number | null;
  highestScore?: number | null;
  hasPassed: boolean;
  questions: QuestionTakingItem[];
}

export interface QuestionAnswerItem {
  questionId: number;
  selectedOptionIds: string[];
}

export interface SubmitQuizAttemptPayload {
  answers: QuestionAnswerItem[];
}

export interface QuestionResultItem {
  questionId: number;
  isCorrect: boolean;
  earnedPoints: number;
  totalPoints: number;
  selectedOptionIds: string[];
  correctOptionIds: string[];
  explanation?: string | null;
}

export interface QuizAttemptResult {
  id: number;
  quizId: number;
  score: number;
  passingScore: number;
  isPassed: boolean;
  totalQuestions: number;
  correctQuestions: number;
  submittedAt: string;
  questionResults?: QuestionResultItem[];
}
