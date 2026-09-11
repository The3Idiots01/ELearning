import { useEffect, useState } from 'react';
import type { Assessment, LearningOutcome, Section } from '../../../types/course';
import { curriculumApi } from '../api/curriculumApi';
import { QuizAuthoringView } from './QuizAuthoringView';
import { useConfirm } from '../../../app/context/ConfirmContext';
import { useToast } from '../../../app/context/ToastContext';
import { quizApi } from '../api/quizApi';
import { ApiError } from '../../../lib/apiClient';

function AssessmentEditor({
  courseId,
  assessment,
  outcomes,
  onChanged
}: {
  courseId: number;
  assessment: Assessment;
  outcomes: LearningOutcome[];
  onChanged: () => Promise<void>;
}) {
  const [title, setTitle] = useState(assessment.title);
  const [instructions, setInstructions] = useState(assessment.instructions || '');
  const [selected, setSelected] = useState<number[]>(assessment.outcomeIds || []);
  const [passingScore, setPassingScore] = useState(80);
  const [maxAttempts, setMaxAttempts] = useState<number | null>(3);
  const [savedQuizConfig, setSavedQuizConfig] = useState({ passingScore: 80, maxAttempts: 3 as number | null });
  const [configLoading, setConfigLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [isSaved, setIsSaved] = useState(false);

  useEffect(() => {
    let active = true;
    setConfigLoading(true);
    setError('');
    void quizApi.getQuiz(courseId, assessment.id)
      .then((quiz) => {
        if (!active) return;
        const config = {
          passingScore: Number(quiz.passingScore),
          maxAttempts: quiz.maxAttempts ?? null
        };
        setPassingScore(config.passingScore);
        setMaxAttempts(config.maxAttempts);
        setSavedQuizConfig(config);
      })
      .catch((cause) => {
        if (!active || cause instanceof ApiError && cause.status === 404) return;
        setError(cause instanceof Error ? cause.message : 'Không thể tải cấu hình bài kiểm tra.');
      })
      .finally(() => {
        if (active) setConfigLoading(false);
      });
    return () => { active = false; };
  }, [assessment.id, courseId]);

  const save = async () => {
    if (saving) return;
    if (!title.trim()) {
      setError('Tên bài kiểm tra không được để trống.');
      return;
    }
    if (!Number.isFinite(passingScore) || passingScore < 0 || passingScore > 100
      || (maxAttempts !== null && (!Number.isInteger(maxAttempts) || maxAttempts < 1 || maxAttempts > 20))) {
      setError('Điểm đạt phải từ 0 đến 100; số lần làm phải là số nguyên từ 1 đến 20.');
      return;
    }
    setSaving(true);
    setError('');
    setIsSaved(false);
    try {
      await quizApi.upsertQuiz(courseId, assessment.id, {
        title: title.trim(),
        instructions: instructions.trim(),
        outcomeIds: selected,
        passingScore,
        maxAttempts
      });
      setSavedQuizConfig({ passingScore, maxAttempts });
      await onChanged();
      setIsSaved(true);
      setTimeout(() => setIsSaved(false), 2500);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thể lưu bài kiểm tra.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="rounded-2xl border border-outline-variant/60 bg-white p-5 space-y-4 shadow-xs">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="material-symbols-outlined text-primary text-[20px]">info</span>
          <h4 className="m-0 text-sm font-black text-slate-900">Thông tin bài kiểm tra</h4>
        </div>
        {isSaved && (
          <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200 flex items-center gap-1">
            <span className="material-symbols-outlined text-[14px]">check</span>Đã lưu thông tin
          </span>
        )}
      </div>

      <div className="space-y-3.5">
        <div className="space-y-1">
          <label className="block text-xs font-bold text-slate-700">
            Tên bài kiểm tra <span className="text-rose-500">*</span>
          </label>
          <input
            value={title}
            maxLength={255}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Nhập tên bài kiểm tra..."
            className="w-full px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs sm:text-sm text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all font-medium"
          />
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div className="space-y-1">
            <label className="block text-xs font-bold text-slate-700">Điểm đạt (%)</label>
            <input
              type="number"
              min={0}
              max={100}
              step="0.01"
              disabled={configLoading || saving}
              value={passingScore}
              onChange={(e) => setPassingScore(Number(e.target.value))}
              className="w-full px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs sm:text-sm font-semibold text-slate-900 focus:bg-white focus:border-primary focus:outline-none transition-all"
            />
          </div>
          <div className="space-y-1">
            <label className="block text-xs font-bold text-slate-700">Số lần làm tối đa</label>
            <input
              type="number"
              min={1}
              max={20}
              disabled={configLoading || saving}
              value={maxAttempts ?? ''}
              placeholder="Không giới hạn"
              onChange={(e) => setMaxAttempts(e.target.value ? Number(e.target.value) : null)}
              className="w-full px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs sm:text-sm font-semibold text-slate-900 focus:bg-white focus:border-primary focus:outline-none transition-all"
            />
          </div>
        </div>

        <div className="space-y-1">
          <label className="block text-xs font-bold text-slate-700">Hướng dẫn làm bài</label>
          <textarea
            value={instructions}
            onChange={(e) => setInstructions(e.target.value)}
            rows={2}
            placeholder="Hướng dẫn cho học viên trước khi bắt đầu bài kiểm tra..."
            className="w-full px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs sm:text-sm text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all leading-relaxed"
          />
        </div>

        <div className="space-y-2">
          <label className="block text-xs font-bold text-slate-700">
            Chuẩn đầu ra gắn kèm
          </label>
          {outcomes.length === 0 ? (
            <p className="text-xs text-slate-400 italic m-0">Khóa học chưa có chuẩn đầu ra nào.</p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {outcomes.map((o) => {
                const isChecked = selected.includes(o.id);
                return (
                  <button
                    key={o.id}
                    type="button"
                    disabled={saving}
                    onClick={() =>
                      setSelected((ids) =>
                        ids.includes(o.id) ? ids.filter((id) => id !== o.id) : [...ids, o.id]
                      )
                    }
                    className={`px-3 py-1.5 rounded-xl text-xs font-semibold border transition-all cursor-pointer flex items-center gap-1.5 ${
                      isChecked
                        ? 'bg-primary/10 border-primary text-primary shadow-xs'
                        : 'bg-surface-container-low border-outline-variant/60 text-slate-600 hover:border-primary/40'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[16px]">
                      {isChecked ? 'check_circle' : 'add'}
                    </span>
                    <span>{o.statement}</span>
                  </button>
                );
              })}
            </div>
          )}
          {selected.length === 0 && (
            <p className="text-xs text-amber-700 flex items-center gap-1 m-0 pt-1 font-medium">
              <span className="material-symbols-outlined text-[15px]">warning</span>
              <span>Bài kiểm tra chưa gắn kết quả đầu ra. Cần bổ sung trước khi xuất bản.</span>
            </p>
          )}
        </div>
      </div>

      {error && (
        <div
          role="alert"
          className="text-xs font-semibold text-rose-600 bg-rose-50 p-2.5 rounded-xl border border-rose-200 flex items-center gap-1.5"
        >
          <span className="material-symbols-outlined text-[16px]">error</span>
          <span>{error}</span>
        </div>
      )}

      <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-slate-100">
        <button
          type="button"
          disabled={saving}
          onClick={() => {
            setTitle(assessment.title);
            setInstructions(assessment.instructions || '');
            setSelected(assessment.outcomeIds || []);
            setPassingScore(savedQuizConfig.passingScore);
            setMaxAttempts(savedQuizConfig.maxAttempts);
            setError('');
          }}
          className="px-3.5 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100 transition cursor-pointer"
        >
          Khôi phục ban đầu
        </button>
        <button
          type="button"
          disabled={saving || configLoading}
          onClick={() => void save()}
          className="bg-primary hover:bg-primary/90 text-white font-bold text-xs px-4 py-2 rounded-xl transition shadow-sm flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
        >
          <span className="material-symbols-outlined text-[16px]">save</span>
          <span>{saving ? 'Đang lưu...' : 'Lưu tất cả thay đổi'}</span>
        </button>
      </div>
    </div>
  );
}

export function AssessmentPlanPanel({
  courseId,
  sections,
  outcomes,
  assessments,
  onChanged,
  openId,
  onOpen
}: {
  courseId: number;
  sections: Section[];
  outcomes: LearningOutcome[];
  assessments: Assessment[];
  onChanged: () => Promise<void>;
  openId: number | null;
  onOpen: (id: number | null) => void;
}) {
  const confirm = useConfirm();
  const { showError } = useToast();
  const [title, setTitle] = useState('');
  const [selected, setSelected] = useState<number[]>([]);
  const [saving, setSaving] = useState(false);

  const create = async () => {
    if (!title.trim() || saving) return;
    setSaving(true);
    try {
      const created = await curriculumApi.addAssessment(courseId, {
        type: 'QUIZ',
        title: title.trim(),
        outcomeIds: selected
      });
      setTitle('');
      setSelected([]);
      await onChanged();
      onOpen(created.id);
    } catch (e) {
      showError(e instanceof Error ? e.message : 'Không thể tạo bài kiểm tra.');
    } finally {
      setSaving(false);
    }
  };

  const place = async (assessment: Assessment, value: string) => {
    setSaving(true);
    try {
      await curriculumApi.placeAssessment(courseId, assessment.id, value ? Number(value) : null, 0);
      await onChanged();
    } catch (e) {
      showError(e instanceof Error ? e.message : 'Không thể xếp bài kiểm tra vào chương.');
    } finally {
      setSaving(false);
    }
  };

  const remove = async (assessment: Assessment) => {
    if (!(await confirm('Xóa bài kiểm tra và các câu hỏi của bài này?'))) return;
    setSaving(true);
    try {
      await curriculumApi.deleteAssessment(courseId, assessment.id, true);
      if (openId === assessment.id) onOpen(null);
      await onChanged();
    } catch (e) {
      showError(e instanceof Error ? e.message : 'Không thể xóa bài kiểm tra.');
    } finally {
      setSaving(false);
    }
  };

  const unassignedCount = assessments.filter((a) => !a.sectionId).length;

  return (
    <section className="bg-surface-container-lowest p-6 sm:p-8 rounded-3xl border border-outline-variant/70 shadow-xs space-y-6">
      {/* Section Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base sm:text-lg font-extrabold text-slate-900 m-0 font-display flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[24px]">assignment</span>
            <span>Soạn giáo trình</span>
          </h2>
          <p className="text-xs text-slate-500 mt-1 m-0">
            Tạo bài kiểm tra trắc nghiệm, liên kết chuẩn đầu ra và xếp vị trí cuối các chương.
          </p>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-xs font-bold px-3 py-1 rounded-full border bg-surface-container-low text-slate-700 border-outline-variant/60">
            {assessments.length} bài kiểm tra
          </span>
          <span
            className={`text-xs font-bold px-3 py-1 rounded-full border flex items-center gap-1.5 ${
              unassignedCount === 0
                ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                : 'bg-amber-50 text-amber-700 border-amber-200'
            }`}
          >
            <span className="material-symbols-outlined text-[15px]">
              {unassignedCount === 0 ? 'check_circle' : 'warning'}
            </span>
            <span>
              {unassignedCount === 0 ? 'Tất cả đã xếp chương' : `${unassignedCount} chưa xếp chương`}
            </span>
          </span>
        </div>
      </div>

      {/* New Assessment Form */}
      <div className="rounded-2xl bg-surface-container-low/50 border border-outline-variant/70 p-5 space-y-4">
        <div className="flex items-center gap-2 text-xs font-bold text-slate-800 uppercase tracking-wider">
          <span className="material-symbols-outlined text-primary text-[18px]">add_circle</span>
          <span>Tạo bài kiểm tra mới</span>
        </div>

        <div className="space-y-3">
          <input
            aria-label="Tên bài kiểm tra mới"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={255}
            placeholder="Nhập tên bài kiểm tra mới (ví dụ: Bài kiểm tra trắc nghiệm Chương 1)..."
            className="w-full px-4 py-2.5 bg-white border border-outline-variant/70 rounded-xl text-xs sm:text-sm text-on-surface focus:border-primary focus:outline-none transition-all font-medium"
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                void create();
              }
            }}
          />

          {outcomes.length > 0 && (
            <div className="space-y-1.5">
              <label className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider">
                Gắn kèm chuẩn đầu ra:
              </label>
              <div className="flex flex-wrap gap-2">
                {outcomes.map((o) => {
                  const isChecked = selected.includes(o.id);
                  return (
                    <button
                      key={o.id}
                      type="button"
                      onClick={() =>
                        setSelected((ids) =>
                          ids.includes(o.id) ? ids.filter((id) => id !== o.id) : [...ids, o.id]
                        )
                      }
                      className={`px-3 py-1.5 rounded-xl text-xs font-semibold border transition-all cursor-pointer flex items-center gap-1.5 ${
                        isChecked
                          ? 'bg-primary/10 border-primary text-primary shadow-xs'
                          : 'bg-white border-outline-variant/60 text-slate-600 hover:border-primary/40'
                      }`}
                    >
                      <span className="material-symbols-outlined text-[16px]">
                        {isChecked ? 'check_circle' : 'add'}
                      </span>
                      <span>{o.statement}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          <div className="flex justify-end pt-1">
            <button
              type="button"
              disabled={saving || !title.trim()}
              onClick={() => void create()}
              className="bg-primary hover:bg-primary/90 text-white font-bold text-xs px-5 py-2.5 rounded-xl transition-all shadow-md shadow-primary/20 flex items-center gap-1.5 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed active:scale-95"
            >
              <span className="material-symbols-outlined text-[18px]">add</span>
              <span>Tạo bài kiểm tra</span>
            </button>
          </div>
        </div>
      </div>

      {/* Assessments List */}
      <div className="space-y-3">
        {assessments.length === 0 ? (
          <div className="text-center py-10 bg-surface-container-low/40 rounded-2xl border border-dashed border-outline-variant/60 text-xs text-slate-400 space-y-2">
            <span className="material-symbols-outlined text-[36px] text-slate-300 block">quiz</span>
            <p className="m-0 font-medium">Chưa có bài kiểm tra nào trong khóa học.</p>
          </div>
        ) : (
          assessments.map((a) => (
            <article
              id={'assessment-' + a.id}
              key={a.id}
              className={`rounded-2xl border transition-all scroll-mt-24 overflow-hidden ${
                openId === a.id
                  ? 'border-primary/50 shadow-md bg-white ring-2 ring-primary/10'
                  : 'border-outline-variant/70 bg-white hover:border-outline hover:shadow-xs'
              }`}
            >
              <div className="p-4 sm:p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
                {/* Left: Icon, Title & Outcomes */}
                <div className="flex items-start gap-3.5 min-w-0 flex-1">
                  <div className="w-10 h-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center shrink-0">
                    <span className="material-symbols-outlined text-[22px]">quiz</span>
                  </div>
                  <div className="min-w-0 flex-1 space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <div className="flex min-w-0 items-center gap-2">
                        <h3 className="text-sm font-bold text-slate-900 m-0 truncate">{a.title}</h3>
                        {a.publicationStatus === 'DRAFT' ? (
                          <span className="shrink-0 rounded-full bg-amber-100 px-2 py-0.5 text-[9px] font-black uppercase text-amber-700">Bản nháp</span>
                        ) : a.publicationStatus === 'PUBLISHED' ? (
                          <span className="shrink-0 rounded-full bg-emerald-100 px-2 py-0.5 text-[9px] font-black uppercase text-emerald-700">Đã xuất bản</span>
                        ) : null}
                      </div>
                      {!a.sectionId && (
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-amber-50 text-amber-700 border border-amber-200">
                          Chưa xếp chương
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-2 text-xs text-slate-500 flex-wrap">
                      <span className="flex items-center gap-1">
                        <span className="material-symbols-outlined text-[15px] text-primary">
                          track_changes
                        </span>
                        <span>{a.outcomeIds?.length || 0} chuẩn đầu ra liên kết</span>
                      </span>
                    </div>
                  </div>
                </div>

                {/* Right: Section Selector & Edit Toggle & Delete */}
                <div className="flex items-center gap-2.5 shrink-0 flex-wrap justify-end">
                  <div className="relative">
                    <select
                      aria-label={'Xếp chương cho ' + a.title}
                      disabled={saving}
                      value={a.sectionId ?? ''}
                      onChange={(e) => void place(a, e.target.value)}
                      className="px-3.5 py-2 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs font-bold text-slate-700 focus:bg-white focus:border-primary focus:outline-none transition cursor-pointer pr-8"
                    >
                      <option value="">Chưa xếp vào chương nào</option>
                      {sections.map((s, idx) => (
                        <option key={s.id} value={s.id}>
                          Chương {idx + 1}: {s.title}
                        </option>
                      ))}
                    </select>
                    <span className="material-symbols-outlined text-[16px] text-slate-400 absolute right-2 top-1/2 -translate-y-1/2 pointer-events-none">
                      unfold_more
                    </span>
                  </div>

                  <button
                    type="button"
                    onClick={() => onOpen(openId === a.id ? null : a.id)}
                    className={`px-3.5 py-2 rounded-xl text-xs font-bold flex items-center gap-1.5 transition cursor-pointer ${
                      openId === a.id
                        ? 'bg-slate-200 text-slate-800'
                        : 'bg-primary/10 text-primary hover:bg-primary hover:text-white'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[16px]">
                      {openId === a.id ? 'close' : 'tune'}
                    </span>
                    <span>{openId === a.id ? 'Đóng trình soạn' : 'Chỉnh sửa'}</span>
                  </button>

                  <button
                    type="button"
                    disabled={saving}
                    onClick={() => void remove(a)}
                    className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition cursor-pointer disabled:opacity-50"
                    title="Xóa bài kiểm tra"
                  >
                    <span className="material-symbols-outlined text-[18px]">delete</span>
                  </button>
                </div>
              </div>

              {/* Expanded Editor */}
              {openId === a.id && (
                <div className="border-t border-outline-variant/60 p-5 sm:p-7 space-y-6 bg-surface-container-low/20 animate-in fade-in">
                  <AssessmentEditor
                    courseId={courseId}
                    assessment={a}
                    outcomes={outcomes}
                    onChanged={onChanged}
                  />
                  <QuizAuthoringView
                    courseId={courseId}
                    assessment={a}
                    onChanged={() => void onChanged()}
                  />
                </div>
              )}
            </article>
          ))
        )}
      </div>
    </section>
  );
}
