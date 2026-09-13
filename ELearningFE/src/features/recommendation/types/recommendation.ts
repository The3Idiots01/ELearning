import type { CourseSummary } from '../../../types/course';

export interface RecommendedCourse {
  course: CourseSummary;
  matchScore: number;
  recommendationReason: string;
  recommendationType: 'SYSTEM_RULE' | 'GEMINI_AI' | 'SYSTEM_FALLBACK' | string;
}

export interface ContinuousRecommendationResponse {
  items: RecommendedCourse[];
  strategy: string;
  total: number;
}

export interface AiRecommendationResponse {
  items: RecommendedCourse[];
  modelUsed: string;
  fallback: boolean;
  summaryAdvice: string;
  goal?: string;
  createdAt?: string;
}

export interface AiRecommendationRequest {
  goal?: string;
  interests?: string[];
  preferredLevel?: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | string;
  targetCategoryId?: number;
}
