import React, { useMemo, useState } from 'react';
import type { Assessment, LearningOutcome, Section } from '../../../types/course';
import { curriculumApi } from '../api/curriculumApi';
import { QuizAuthoringView } from './QuizAuthoringView';
import { useToast } from '../../../app/context/ToastContext';

interface AssessmentPlanPanelProps {
  courseId: number;
  sections: Section[];
  outcomes: LearningOutcome[];
  assessments: Assessment[];
  onChanged: () => Promise<void>;
}

export const AssessmentPlanPanel: React.FC<AssessmentPlanPanelProps> = ({ courseId, sections, outcomes, assessments, onChanged }) => {
  const { showError, showSuccess } = useToast();
  const [title, setTitle] = useState('');
  const [selectedOutcomes, setSelectedOutcomes] = useState<number[]>([]);
  const [openQuiz, setOpenQuiz] = useState<number | null>(null);

  const toggleOutcome = (id: number) => setSelectedOutcomes((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
  const create = async () => {
    if (!title.trim()) return;
    try {
      await curriculumApi.addAssessment(courseId, { type: 'QUIZ', title: title.trim(), outcomeIds: selectedOutcomes });
      setTitle('');
      setSelectedOutcomes([]);
      await onChanged();
      showSuccess('Đã tạo assessment plan.');
    } catch (err: any) {
      showError(err.message || 'Không thể tạo assessment.');
    }
  };
  const place = async (assessment: Assessment, value: string) => {
    try {
      await curriculumApi.placeAssessment(courseId, assessment.id, value ? Number(value) : null, 0);
      await onChanged();
    } catch (err: any) {
      showError(err.message || 'Không thể đặt assessment vào chương.');
    }
  };
  const remove = async (assessment: Assessment) => {
    if (!window.confirm('Xóa assessment này và cấu hình quiz?')) return;
    try {
      await curriculumApi.deleteAssessment(courseId, assessment.id);
      await onChanged();
    } catch (err: any) {
      showError(err.message || 'Không thể xóa assessment.');
    }
  };
  const sortedAssessments = useMemo(() => [...assessments].sort((a, b) => a.position - b.position || a.id - b.id), [assessments]);
  const unplacedCount = assessments.filter((assessment) => !assessment.sectionId).length;

  return (
    <section className="bg-surface-container-lowest p-6 rounded-3xl border border-amber-200 shadow-xs space-y-5">
      <div>
        <h2 className="text-base font-black text-slate-900 m-0 flex items-center gap-2"><span className="material-symbols-outlined text-amber-500">quiz</span>Assessment plan</h2>
        <p className="text-xs text-slate-500 mt-1 m-0">Tạo đánh giá trước, gắn learning outcome, sau đó đặt vào chương. Quiz luôn hiển thị sau các lesson của chương.</p>
        <div className="mt-2 inline-flex items-center gap-2 rounded-lg bg-slate-100 px-3 py-1.5 text-[11px] font-bold text-slate-600"><span className="material-symbols-outlined text-[15px]">inventory_2</span>Pool chưa placement: {unplacedCount}</div>
      </div>
      <div className="rounded-2xl bg-amber-50/50 border border-amber-100 p-4 space-y-3">
        <input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Tên bài kiểm tra (ví dụ: Quiz chương 1)" className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-xs focus:outline-none focus:border-amber-400" />
        <div className="flex flex-wrap gap-2">
          {outcomes.map((outcome) => <label key={outcome.id} className="inline-flex items-center gap-2 rounded-lg bg-white border border-slate-200 px-3 py-2 text-xs text-slate-700"><input type="checkbox" checked={selectedOutcomes.includes(outcome.id)} onChange={() => toggleOutcome(outcome.id)} />{outcome.statement}</label>)}
        </div>
        <button type="button" onClick={() => void create()} disabled={!title.trim()} className="rounded-xl bg-amber-500 px-4 py-2.5 text-xs font-black text-white disabled:opacity-50">Tạo assessment</button>
      </div>
      <div className="space-y-3">
        {sortedAssessments.length === 0 && <p className="text-xs text-slate-500 m-0">Chưa có assessment nào.</p>}
        {sortedAssessments.map((assessment) => (
          <div key={assessment.id} className="rounded-2xl border border-slate-200 bg-white p-4 space-y-3">
            <div className="flex items-center gap-3">
              <span className="material-symbols-outlined text-amber-500">quiz</span>
              <div className="flex-1"><h3 className="text-sm font-bold text-slate-900 m-0">{assessment.title}</h3><p className="text-[11px] text-slate-500 m-0 mt-1">Outcome: {assessment.outcomeIds?.length || 0}</p></div>
              <select value={assessment.sectionId ?? ''} onChange={(event) => void place(assessment, event.target.value)} className="rounded-lg border border-slate-200 px-2 py-2 text-xs"><option value="">Chưa đặt chương</option>{sections.map((section) => <option key={section.id} value={section.id}>{section.title}</option>)}</select>
              <button type="button" onClick={() => setOpenQuiz(openQuiz === assessment.id ? null : assessment.id)} className="rounded-lg bg-indigo-50 px-3 py-2 text-xs font-bold text-indigo-700">{openQuiz === assessment.id ? 'Ẩn quiz' : 'Cấu hình quiz'}</button>
              <button type="button" onClick={() => void remove(assessment)} className="text-xs font-bold text-rose-600">Xóa</button>
            </div>
            {openQuiz === assessment.id && <QuizAuthoringView courseId={courseId} assessment={assessment} onChanged={onChanged} />}
          </div>
        ))}
      </div>
    </section>
  );
};
