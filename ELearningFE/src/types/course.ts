import type { Category } from './category';

export type CourseLevel = 'ALL' | 'BEGINNER' | 'INTERMEDIATE' | 'EXPERT';
export type CourseStatus = 'DRAFT' | 'PUBLISHED' | 'UNPUBLISHED' | 'SUSPENDED';
export type LessonContentType = 'VIDEO' | 'ARTICLE' | 'FILE' | 'QUIZ';
export type LessonUploadStatus = 'PENDING' | 'READY' | 'FAILED';

export interface LessonResource {
  id: number;
  title: string;
  originalFileName?: string;
  fileSizeBytes?: number;
  downloadUrl?: string;
  mimeType?: string;
  createdAt?: string;
}

export interface Lesson {
  id: number;
  title: string;
  contentType: LessonContentType;
  uploadStatus?: LessonUploadStatus;
  durationSeconds?: number;
  isPreview?: boolean;
  position: number;
  contentText?: string;
  contentUrl?: string;
  originalFileName?: string;
  fileSizeBytes?: number;
  mimeType?: string;
  resources?: LessonResource[];
  completed?: boolean;
}

export interface Section {
  id: number;
  title: string;
  description?: string;
  position: number;
  totalLessons?: number;
  totalDurationSeconds?: number;
  lessons: Lesson[];
}

export interface Curriculum {
  courseId: number;
  sections: Section[];
}

export interface CourseSummary {
  id: number;
  title: string;
  slug: string;
  subtitle?: string;
  description?: string;
  thumbnailUrl?: string;
  level: CourseLevel;
  price: number;
  status: CourseStatus;
  ratingAvg?: number;
  totalStudents?: number;
  categoryId?: number;
  categoryName?: string;
  category?: Category;
  lecturerName?: string;
  createdAt?: string;
  updatedAt?: string;
  publishedAt?: string;
  version?: number;
}

export interface CourseDetail extends CourseSummary {
  language?: string;
  learningObjectives?: string[];
  requirements?: string[];
  targetAudiences?: string[];
}

export interface CreateCourseRequest {
  title: string;
  categoryId: number;
}

export interface UpdateCourseRequest {
  title?: string;
  subtitle?: string;
  description?: string;
  level?: CourseLevel;
  language?: string;
  categoryId?: number;
}

export interface UpdatePriceRequest {
  price: number;
}

export interface UpdateBulletsRequest {
  learningObjectives: string[];
  requirements: string[];
  targetAudiences: string[];
}

export interface UpdateThumbnailRequest {
  storageKey: string;
}

export interface CreateSectionRequest {
  title: string;
  description?: string;
}

export interface UpdateSectionRequest {
  title?: string;
  description?: string;
}

export interface CreateLessonRequest {
  title: string;
  contentType: LessonContentType;
}

export interface UpdateLessonRequest {
  title?: string;
  isPreview?: boolean;
  contentText?: string;
}

export interface AttachLessonContentRequest {
  storageKey: string;
  originalFileName: string;
  fileSizeBytes: number;
  mimeType: string;
  durationSeconds?: number;
}

export interface AddLessonResourceRequest {
  title: string;
  storageKey: string;
  originalFileName: string;
  fileSizeBytes: number;
  mimeType: string;
}

export type UploadPurpose =
  | 'COURSE_THUMBNAIL'
  | 'COURSE_PROMO_VIDEO'
  | 'LESSON_VIDEO'
  | 'LESSON_FILE'
  | 'LESSON_RESOURCE';

export interface PresignUploadRequest {
  purpose: UploadPurpose;
  courseId: number;
  lessonId?: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
}

export interface PresignUploadResponse {
  storageKey: string;
  uploadUrl: string;
  method: string;
  headers?: Record<string, string>;
  expiresAt: string;
}

export interface PublishIssue {
  code: string;
  field: string;
  message: string;
}

export interface PublishCheckResponse {
  canPublish: boolean;
  issues: (PublishIssue | string)[];
}

export interface StatusLog {
  id: number;
  courseId: number;
  fromStatus: CourseStatus;
  toStatus: CourseStatus;
  changedBy: number;
  reason?: string;
  createdAt: string;
}

export interface EnrolledCourse {
  enrollmentId: number;
  courseId: number;
  courseTitle: string;
  courseSlug: string;
  courseThumbnailUrl?: string;
  lecturerName?: string;
  categoryName?: string;
  level?: string;
  progress: number;
  status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  enrolledAt: string;
  completedAt?: string;
}
