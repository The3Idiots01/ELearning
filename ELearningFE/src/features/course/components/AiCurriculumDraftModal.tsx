import React, { useState } from 'react';
import type {
  CurriculumDraft,
  CurriculumLessonDraft,
  LearningOutcome,
  LessonContentType
} from '../../../types/course';
import { Modal } from '../../../components/common/Modal';
import { useToast } from '../../../app/context/ToastContext';
import { aiAuthoringApi } from '../api/aiAuthoringApi';

interface Props {
  courseId: number;
  outcomes: LearningOutcome[];
  isOpen: boolean;
  hasExistingCurriculum: boolean;
  onClose: () => void;
  onApplied: () => Promise<void> | void;
}

const emptyLesson = (): CurriculumLessonDraft => ({
  title: 'Bài học mới',
  recommendedContentType: 'ARTICLE',
  outcomeIds: []
});

export const AiCurriculumDraftModal: React.FC<Props> = ({
  courseId,
  outcomes,
  isOpen,
  hasExistingCurriculum,
  onClose,
  onApplied
}) => {
  const { showSuccess, showError } = useToast();
  const [maxSections, setMaxSections] = useState('');
  const [lessonsPerSection, setLessonsPerSection] = useState('');
  const [guidance, setGuidance] = useState('');
  const [draft, setDraft] = useState<CurriculumDraft | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isApplying, setIsApplying] = useState(false);

  const generate = async () => {
    setIsGenerating(true);
    try {
      const result = await aiAuthoringApi.generateCurriculumDraft(courseId, {
        maxSections: maxSections ? Number(maxSections) : undefined,
        lessonsPerSection: lessonsPerSection ? Number(lessonsPerSection) : undefined,
        guidance: guidance.trim() || undefined
      });
      setDraft(result);
      showSuccess('AI đã tạo bản nháp. Bạn có thể chỉnh sửa trước khi áp dụng.');
    } catch (err: any) {
      showError(err.message || 'Không thể tạo cấu trúc bằng AI.');
    } finally {
      setIsGenerating(false);
    }
  };

  const updateSection = (sectionIndex: number, field: 'title' | 'description', value: string) => {
    setDraft((current) => current && ({
      sections: current.sections.map((section, index) =>
        index === sectionIndex ? { ...section, [field]: value } : section)
    }));
  };

  const updateLesson = (
    sectionIndex: number,
    lessonIndex: number,
    changes: Partial<CurriculumLessonDraft>
  ) => {
    setDraft((current) => current && ({
      sections: current.sections.map((section, index) => index === sectionIndex
        ? {
            ...section,
            lessons: section.lessons.map((lesson, childIndex) =>
              childIndex === lessonIndex ? { ...lesson, ...changes } : lesson)
          }
        : section)
    }));
  };

  const toggleOutcome = (sectionIndex: number, lessonIndex: number, outcomeId: number) => {
    const lesson = draft?.sections[sectionIndex]?.lessons[lessonIndex];
    if (!lesson) return;
    const outcomeIds = lesson.outcomeIds.includes(outcomeId)
      ? lesson.outcomeIds.filter((id) => id !== outcomeId)
      : [...lesson.outcomeIds, outcomeId];
    updateLesson(sectionIndex, lessonIndex, { outcomeIds });
  };

  const apply = async () => {
    if (!draft || draft.sections.length === 0) return;
    setIsApplying(true);
    try {
      await aiAuthoringApi.applyCurriculumDraft(courseId, draft);
      showSuccess('Đã thêm cấu trúc AI và lưu định dạng cho từng bài học.');
      setDraft(null);
      await onApplied();
      onClose();
    } catch (err: any) {
      showError(err.message || 'Không thể áp dụng bản nháp curriculum.');
    } finally {
      setIsApplying(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="AI tạo cấu trúc khóa học"
      subtitle="Gemini dựa trên thông tin khóa học và learning outcomes; bạn là người quyết định kết quả cuối."
      icon="auto_awesome"
      maxWidth="4xl"
    >
      <div className="space-y-5">
        {!draft && (
          <>
            {outcomes.length < 2 && (
              <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs text-amber-800">
                Hãy tạo tối thiểu 2 learning outcomes trong phần Cài đặt khóa học trước khi sinh giáo trình.
              </div>
            )}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <label className="space-y-1 text-xs font-bold text-slate-700">
                <span>Số chương tối đa (tùy chọn)</span>
                <input
                  type="number"
                  min={1}
                  max={12}
                  value={maxSections}
                  onChange={(event) => setMaxSections(event.target.value)}
                  placeholder="Để AI tự đề xuất"
                  className="w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal"
                />
              </label>
              <label className="space-y-1 text-xs font-bold text-slate-700">
                <span>Số bài tối đa mỗi chương (tùy chọn)</span>
                <input
                  type="number"
                  min={1}
                  max={10}
                  value={lessonsPerSection}
                  onChange={(event) => setLessonsPerSection(event.target.value)}
                  placeholder="Để AI tự đề xuất"
                  className="w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal"
                />
              </label>
            </div>
            <label className="block space-y-1 text-xs font-bold text-slate-700">
              <span>Định hướng thêm (tùy chọn)</span>
              <textarea
                rows={3}
                maxLength={1000}
                value={guidance}
                onChange={(event) => setGuidance(event.target.value)}
                placeholder="Ví dụ: đi từ cơ bản đến nâng cao, ưu tiên ví dụ thực tế và bài học dạng video…"
                className="w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal leading-relaxed"
              />
            </label>
            <div className="flex justify-end">
              <button
                type="button"
                disabled={isGenerating || outcomes.length < 2}
                onClick={() => void generate()}
                className="rounded-xl bg-violet-600 px-5 py-2.5 text-xs font-extrabold text-white disabled:opacity-50"
              >
                {isGenerating ? 'Gemini đang tạo cấu trúc…' : '✨ Tạo bản nháp bằng AI'}
              </button>
            </div>
          </>
        )}

        {draft && (
          <>
            <div className="rounded-xl border border-indigo-200 bg-indigo-50 p-3 text-xs text-indigo-800">
              {hasExistingCurriculum
                ? 'Khi áp dụng, các chương bên dưới sẽ được thêm vào cuối giáo trình hiện tại; nội dung cũ không bị xóa.'
                : 'Kiểm tra tiêu đề, định dạng và outcome trước khi tạo bài học. Định dạng đã chọn sẽ được lưu cùng bài.'}
            </div>
            <div className="space-y-4">
              {draft.sections.map((section, sectionIndex) => (
                <div key={sectionIndex} className="rounded-2xl border border-slate-200 bg-slate-50 p-4 space-y-3">
                  <div className="flex items-start gap-2">
                    <div className="grid flex-1 grid-cols-1 gap-2 sm:grid-cols-2">
                      <input
                        value={section.title}
                        maxLength={255}
                        onChange={(event) => updateSection(sectionIndex, 'title', event.target.value)}
                        className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-bold"
                      />
                      <input
                        value={section.description || ''}
                        maxLength={500}
                        onChange={(event) => updateSection(sectionIndex, 'description', event.target.value)}
                        placeholder="Mô tả chương"
                        className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs"
                      />
                    </div>
                    <button
                      type="button"
                      title="Bỏ chương này"
                      onClick={() => setDraft({ sections: draft.sections.filter((_, index) => index !== sectionIndex) })}
                      className="p-2 text-rose-600"
                    >
                      <span className="material-symbols-outlined text-[18px]">delete</span>
                    </button>
                  </div>

                  {section.lessons.map((lesson, lessonIndex) => (
                    <div key={lessonIndex} className="rounded-xl border border-slate-200 bg-white p-3 space-y-2">
                      <div className="flex gap-2">
                        <input
                          value={lesson.title}
                          maxLength={255}
                          onChange={(event) => updateLesson(sectionIndex, lessonIndex, { title: event.target.value })}
                          className="min-w-0 flex-1 rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold"
                        />
                        <select
                          value={lesson.recommendedContentType}
                          onChange={(event) => updateLesson(sectionIndex, lessonIndex, {
                            recommendedContentType: event.target.value as LessonContentType
                          })}
                          className="rounded-lg border border-slate-200 px-2 py-2 text-xs font-bold"
                        >
                          <option value="ARTICLE">Article</option>
                          <option value="VIDEO">Video</option>
                          <option value="FILE">Tài liệu</option>
                        </select>
                        <button
                          type="button"
                          title="Bỏ bài học"
                          onClick={() => setDraft({
                            sections: draft.sections.map((item, index) => index === sectionIndex
                              ? { ...item, lessons: item.lessons.filter((_, childIndex) => childIndex !== lessonIndex) }
                              : item)
                          })}
                          className="p-2 text-rose-500"
                        >
                          <span className="material-symbols-outlined text-[17px]">close</span>
                        </button>
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {outcomes.map((outcome) => (
                          <button
                            key={outcome.id}
                            type="button"
                            onClick={() => toggleOutcome(sectionIndex, lessonIndex, outcome.id)}
                            title={outcome.statement}
                            className={`rounded-full border px-2 py-1 text-[10px] font-bold ${lesson.outcomeIds.includes(outcome.id)
                              ? 'border-indigo-300 bg-indigo-50 text-indigo-700'
                              : 'border-slate-200 text-slate-500'}`}
                          >
                            LO{outcome.position + 1}
                          </button>
                        ))}
                      </div>
                    </div>
                  ))}
                  <button
                    type="button"
                    onClick={() => setDraft({
                      sections: draft.sections.map((item, index) => index === sectionIndex
                        ? { ...item, lessons: [...item.lessons, emptyLesson()] }
                        : item)
                    })}
                    className="text-xs font-bold text-primary"
                  >
                    + Thêm bài học
                  </button>
                </div>
              ))}
            </div>
            <div className="flex flex-wrap justify-between gap-2 border-t border-slate-200 pt-4">
              <button
                type="button"
                onClick={() => setDraft(null)}
                className="rounded-xl bg-slate-100 px-4 py-2 text-xs font-bold text-slate-600"
              >
                Tạo lại
              </button>
              <button
                type="button"
                disabled={isApplying || draft.sections.length === 0}
                onClick={() => void apply()}
                className="rounded-xl bg-primary px-5 py-2.5 text-xs font-extrabold text-white disabled:opacity-50"
              >
                {isApplying ? 'Đang áp dụng…' : 'Review xong · Áp dụng vào giáo trình'}
              </button>
            </div>
          </>
        )}
      </div>
    </Modal>
  );
};
