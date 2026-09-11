import { useEffect, useState } from 'react';
import type { LearningOutcome } from '../../../types/course';
import { outcomeApi } from '../api/outcomeApi';
import { instructorCourseApi } from '../api/instructorCourseApi';
import { useConfirm } from '../../../app/context/ConfirmContext';
import { useToast } from '../../../app/context/ToastContext';

interface Props {
  courseId: number;
  outcomes: LearningOutcome[];
  onChanged: (outcomes: LearningOutcome[]) => void;
}

export function LearningOutcomesEditor({ courseId, outcomes, onChanged }: Props) {
  const confirm = useConfirm();
  const { showError } = useToast();
  const [draft, setDraft] = useState('');
  const [editing, setEditing] = useState<number | null>(null);
  const [statement, setStatement] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [minimum, setMinimum] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    instructorCourseApi
      .publishCheck(courseId)
      .then((r) => {
        if (!cancelled) setMinimum(r.requirements?.minOutcomes ?? null);
      })
      .catch(() => {
        if (!cancelled) setMinimum(null);
      });
    return () => {
      cancelled = true;
    };
  }, [courseId]);

  const begin = async (outcome: LearningOutcome) => {
    const original = outcomes.find((o) => o.id === editing)?.statement;
    if (editing !== null && statement !== original && !(await confirm('Thay đổi kết quả đầu ra chưa được lưu.', false))) {
      return;
    }
    setEditing(outcome.id);
    setStatement(outcome.statement);
    setError('');
  };

  const save = async () => {
    if (saving) return;
    const value = (editing === null ? draft : statement).trim();
    if (!value) {
      setError('Kết quả đầu ra không được để trống.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const changed =
        editing === null
          ? await outcomeApi.create(courseId, value)
          : await outcomeApi.update(courseId, editing, value);
      onChanged(editing === null ? [...outcomes, changed] : outcomes.map((o) => (o.id === changed.id ? changed : o)));
      if (editing === null) setDraft('');
      else setEditing(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thể lưu kết quả đầu ra.');
    } finally {
      setSaving(false);
    }
  };

  const remove = async (outcome: LearningOutcome) => {
    if (!(await confirm('Xóa kết quả đầu ra này và các liên kết liên quan?'))) return;
    setSaving(true);
    try {
      await outcomeApi.remove(courseId, outcome.id);
      onChanged(outcomes.filter((o) => o.id !== outcome.id));
    } catch (e) {
      showError(e instanceof Error ? e.message : 'Không thể xóa kết quả đầu ra.');
    } finally {
      setSaving(false);
    }
  };

  const targetMin = minimum ?? 2;
  const isMet = outcomes.length >= targetMin;

  return (
    <section className="bg-surface-container-lowest p-6 sm:p-8 rounded-3xl border border-outline-variant/70 shadow-xs space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h3 className="text-base font-extrabold text-slate-900 m-0 font-display flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[22px]">track_changes</span>
            <span>3. Chuẩn đầu ra khóa học</span>
          </h3>
          <p className="text-xs text-slate-500 mt-1 m-0">
            Xác định năng lực và kiến thức cốt lõi học viên sẽ đạt được sau khi hoàn thành khóa học.
          </p>
        </div>

        <span
          className={`text-xs font-bold px-3 py-1 rounded-full border w-fit flex items-center gap-1.5 shrink-0 ${
            isMet
              ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
              : 'bg-amber-50 text-amber-700 border-amber-200'
          }`}
        >
          <span className="material-symbols-outlined text-[16px]">
            {isMet ? 'check_circle' : 'pending'}
          </span>
          <span>
            {outcomes.length}/{targetMin} mục
          </span>
        </span>
      </div>

      {/* Info notice if outcomes are below requirement */}
      {!isMet && (
        <div className="flex items-center gap-2 p-3 bg-amber-50/70 border border-amber-200/80 rounded-2xl text-xs text-amber-800">
          <span className="material-symbols-outlined text-[18px] text-amber-600 shrink-0">info</span>
          <span>
            Yêu cầu tối thiểu <strong>{targetMin} chuẩn đầu ra</strong> để đủ điều kiện xuất bản khóa học.
          </span>
        </div>
      )}

      {/* Outcomes List */}
      <div className="space-y-2.5">
        {outcomes.length === 0 ? (
          <div className="text-center py-8 bg-surface-container-low/50 rounded-2xl border border-dashed border-outline-variant/60 text-xs text-slate-400 space-y-2">
            <span className="material-symbols-outlined text-[32px] text-slate-300 block">playlist_add</span>
            <p className="m-0">Chưa có chuẩn đầu ra nào. Hãy thêm chuẩn đầu ra đầu tiên bên dưới.</p>
          </div>
        ) : (
          outcomes.map((outcome, index) => (
            <div
              key={outcome.id}
              className="flex items-center gap-3 rounded-2xl border border-outline-variant/60 bg-surface-container-low/40 px-4 py-3 transition-all hover:bg-white hover:border-primary/40 group"
            >
              <span className="text-xs font-bold text-slate-400 w-6 text-right shrink-0">{index + 1}.</span>

              {editing === outcome.id ? (
                <div className="flex-1 flex items-center gap-2">
                  <input
                    aria-label="Sửa kết quả đầu ra"
                    autoFocus
                    value={statement}
                    disabled={saving}
                    className="flex-1 px-3.5 py-2 bg-white border border-primary rounded-xl text-xs sm:text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-primary/20 font-medium"
                    onChange={(e) => setStatement(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        void save();
                      }
                      if (e.key === 'Escape') {
                        e.preventDefault();
                        setEditing(null);
                      }
                    }}
                  />
                  <button
                    type="button"
                    disabled={saving}
                    onClick={() => void save()}
                    className="p-2 rounded-xl bg-primary text-white hover:bg-primary/90 transition shadow-xs cursor-pointer flex items-center justify-center disabled:opacity-50"
                    title="Lưu thay đổi"
                  >
                    <span className="material-symbols-outlined text-[18px]">check</span>
                  </button>
                  <button
                    type="button"
                    disabled={saving}
                    onClick={() => setEditing(null)}
                    className="p-2 rounded-xl bg-slate-100 text-slate-600 hover:bg-slate-200 transition cursor-pointer flex items-center justify-center disabled:opacity-50"
                    title="Hủy bỏ"
                  >
                    <span className="material-symbols-outlined text-[18px]">close</span>
                  </button>
                </div>
              ) : (
                <>
                  <span className="flex-1 text-xs sm:text-sm font-semibold text-slate-800 leading-relaxed break-words">
                    {outcome.statement}
                  </span>
                  <div className="flex items-center gap-1 shrink-0">
                    <button
                      type="button"
                      disabled={saving}
                      onClick={() => void begin(outcome)}
                      className="p-2 text-slate-400 hover:text-primary hover:bg-primary/10 rounded-xl transition cursor-pointer disabled:opacity-50"
                      title="Chỉnh sửa"
                    >
                      <span className="material-symbols-outlined text-[18px]">edit</span>
                    </button>
                    <button
                      type="button"
                      disabled={saving}
                      onClick={() => void remove(outcome)}
                      className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition cursor-pointer disabled:opacity-50"
                      title="Xóa"
                    >
                      <span className="material-symbols-outlined text-[18px]">delete</span>
                    </button>
                  </div>
                </>
              )}
            </div>
          ))
        )}
      </div>

      {error && (
        <div role="alert" className="flex items-center gap-2 text-xs font-semibold text-rose-600 bg-rose-50 p-3 rounded-xl border border-rose-200">
          <span className="material-symbols-outlined text-[18px]">error</span>
          <span>{error}</span>
        </div>
      )}

      {/* Add New Outcome Input */}
      {editing === null && (
        <div className="flex flex-col sm:flex-row gap-2.5 pt-2">
          <div className="relative flex-1">
            <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-[20px] text-slate-400 pointer-events-none">
              edit_note
            </span>
            <input
              aria-label="Kết quả đầu ra mới"
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  void save();
                }
              }}
              placeholder="Nhập chuẩn đầu ra mới (ví dụ: Nắm vững các cấu trúc ngữ pháp JLPT N3)..."
              className="w-full pl-11 pr-4 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs sm:text-sm text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all"
            />
          </div>
          <button
            type="button"
            disabled={saving || !draft.trim()}
            onClick={() => void save()}
            className="bg-primary hover:bg-primary/90 text-white font-bold text-xs sm:text-sm px-5 py-2.5 rounded-xl transition-all shadow-md shadow-primary/20 flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed shrink-0 active:scale-95"
          >
            <span className="material-symbols-outlined text-[18px]">add</span>
            <span>Thêm chuẩn đầu ra</span>
          </button>
        </div>
      )}
    </section>
  );
}
