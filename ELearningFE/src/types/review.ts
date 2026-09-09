export interface CourseReview {
  id: number;
  courseId: number;
  userId: number;
  userName: string;
  userAvatarUrl?: string;
  rating: number;
  comment?: string;
  progressPercent?: number;
  createdAt: string;
  updatedAt: string;
  isEdited?: boolean;
}

export interface CourseReviewSummary {
  courseId: number;
  ratingAvg: number;
  totalReviews: number;
  ratingCounts: Record<number, number>;
  ratingPercentages: Record<number, number>;
}

export interface InstructorReview {
  id: number;
  courseId: number;
  userId: number;
  studentName: string;
  studentEmail: string;
  studentAvatarUrl?: string;
  rating: number;
  comment?: string;
  progressPercent?: number;
  createdAt: string;
  updatedAt: string;
  isEdited?: boolean;
}

export interface CreateOrUpdateReviewPayload {
  rating: number;
  comment?: string;
}

export interface PaginatedReviews<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  empty: boolean;
}

export interface CourseReviewEligibility {
  isEnrolled: boolean;
  currentProgress: number;
  requiredProgress: number;
  canReview: boolean;
  review: CourseReview | null;
}

