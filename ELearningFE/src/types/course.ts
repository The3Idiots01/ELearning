import type { Category } from './category';

export type CourseLevel = 'ALL' | 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
export type CourseStatus = 'DRAFT' | 'PUBLISHED' | 'UNPUBLISHED' | 'SUSPENDED';
export type LessonContentType = 'VIDEO' | 'ARTICLE' | 'FILE';
export type LessonUploadStatus = 'EMPTY' | 'PENDING' | 'PROCESSING' | 'READY' | 'FAILED';

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
  contentType?: LessonContentType;
  uploadStatus?: LessonUploadStatus;
  durationSeconds?: number;
  isPreview?: boolean;
  position: number;
  contentText?: string;
  /** true nếu có thể xin PlaybackTicket cho lesson này ngay bây giờ — thay cho contentUrl (§7.1). */
  playable?: boolean;
  lastPositionSeconds?: number;
  coveragePercent?: number;
  originalFileName?: string;
  fileSizeBytes?: number;
  mimeType?: string;
  resources?: LessonResource[];
  completed?: boolean;
  outcomeIds?: number[];
}

export interface Assessment {
  id: number;
  type: 'QUIZ';
  title: string;
  instructions?: string;
  sectionId?: number | null;
  position: number;
  outcomeIds?: number[];
  completed?: boolean;
}

export interface LearningOutcome {
  id: number;
  statement: string;
  position: number;
}

export interface Section {
  id: number;
  title: string;
  description?: string;
  position: number;
  totalLessons?: number;
  completedLessons?: number;
  totalAssessments?: number;
  completedAssessments?: number;
  totalDurationSeconds?: number;
  lessons: Lesson[];
  assessments: Assessment[];
}

export interface Curriculum {
  courseId: number;
  sections: Section[];
  unplacedAssessments?: Assessment[];
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
  requirements: string[];
  targetAudiences: string[];
  version?: number;
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
  outcomeIds?: number[];
}

export interface UpdateLessonRequest {
  title?: string;
  isPreview?: boolean;
  outcomeIds?: number[];
}

export interface AttachLessonContentRequest {
  contentType: LessonContentType;
  storageKey?: string;
  originalFileName?: string;
  fileSizeBytes?: number;
  mimeType?: string;
  contentText?: string;
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
  recordId?: number | null;
  path?: string | null;
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

export interface PlaybackTicket {
  lessonId: number;
  contentType: LessonContentType;
  streamUrl: string;
  expiresAt: string;
  ttlSeconds: number;
  durationSeconds?: number;
  mimeType?: string;
}

/** Payload heartbeat — §5.4 design_us15_us17.md. playedRanges là nguyên văn video.played (tích luỹ). */
export interface HeartbeatPayload {
  positionSeconds: number;
  playedRanges: [number, number][];
  playbackRate: number;
  clientSessionId: string;
  durationSeconds: number;
}

export type CompletionSource = 'POSITION' | 'MANUAL' | 'QUIZ';

/** Response của heartbeat — §5.4 design_us15_us17.md. */
export interface ProgressSnapshot {
  lessonId: number;
  lastPositionSeconds: number;
  maxPositionSeconds: number;
  coveragePercent: number;
  watchedSeconds: number;
  newWatchedSeconds: number;
  lessonCompleted: boolean;
  completionSource?: CompletionSource | null;
  courseProgressPercent?: number | null;
  enrollmentStatus?: string;
}

export interface AssessmentProgress {
  assessmentId: number;
  completed: boolean;
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
