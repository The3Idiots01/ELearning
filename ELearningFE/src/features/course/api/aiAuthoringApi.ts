import { apiClient } from '../../../lib/apiClient';
import type {
  Curriculum,
  CurriculumDraft,
  GenerateCurriculumDraftRequest,
  OutcomeSuggestionResponse
} from '../../../types/course';

export const aiAuthoringApi = {
  suggestLessonOutcomes: (courseId: number, lessonId: number) =>
    apiClient.post<OutcomeSuggestionResponse>(
      `/api/v1/lecturer/courses/${courseId}/lessons/${lessonId}/ai/outcome-suggestions`
    ),

  generateCurriculumDraft: (courseId: number, request: GenerateCurriculumDraftRequest) =>
    apiClient.post<CurriculumDraft>(
      `/api/v1/lecturer/courses/${courseId}/ai/curriculum-draft`,
      request
    ),

  applyCurriculumDraft: (courseId: number, draft: CurriculumDraft) =>
    apiClient.post<Curriculum>(
      `/api/v1/lecturer/courses/${courseId}/ai/curriculum-draft/apply`,
      draft
    )
};
