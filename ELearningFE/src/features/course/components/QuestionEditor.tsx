import { useState } from 'react';
import type { QuestionType, UpsertQuestionRequest } from '../../../types/quiz';
import { useConfirm } from '../../../app/context/ConfirmContext';

export function newQuestion(): UpsertQuestionRequest {
  return {
    questionText: '',
    questionType: 'SINGLE_CHOICE',
    points: 1,
    options: [true, false].map((isCorrect) => ({
      id: crypto.randomUUID(),
      text: '',
      isCorrect,
      explanation: ''
    }))
  };
}

export function QuestionEditor({
  initial,
  onSave,
  onCancel,
  onDirtyChange
}: {
  initial: UpsertQuestionRequest;
  onSave: (draft: UpsertQuestionRequest) => Promise<void>;
  onCancel: () => void;
  onDirtyChange?: (dirty: boolean) => void;
}) {
  const [draft, setDraft] = useState<UpsertQuestionRequest>(() => structuredClone(initial));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const confirm = useConfirm();

  const change = (next: UpsertQuestionRequest) => {
    setDraft(next);
    onDirtyChange?.(JSON.stringify(next) !== JSON.stringify(initial));
  };

  const cancel = async () => {
    if (
      JSON.stringify(draft) !== JSON.stringify(initial) &&
      !(await confirm('Nội dung câu hỏi chưa được lưu.', false))
    ) {
      return;
    }
    onDirtyChange?.(false);
    onCancel();
  };

  const save = async () => {
    if (saving) return;
    const correct = draft.options.filter((option) => option.isCorrect).length;
    if (!draft.questionText.trim()) {
      setError('Nhập nội dung câu hỏi.');
      return;
    }
    if (!Number.isFinite(draft.points) || draft.points < 0.01) {
      setError('Điểm phải từ 0,01 trở lên.');
      return;
    }
    if (draft.options.length < 2 || draft.options.some((option) => !option.text.trim())) {
      setError('Cần ít nhất hai lựa chọn có nội dung.');
      return;
    }
    if (!correct || (draft.questionType === 'SINGLE_CHOICE' && correct !== 1)) {
      setError(
        draft.questionType === 'SINGLE_CHOICE'
          ? 'Vui lòng chọn đúng một đáp án đúng.'
          : 'Vui lòng chọn ít nhất một đáp án đúng.'
      );
      return;
    }
    setSaving(true);
    setError('');
    try {
      await onSave({
        ...draft,
        questionText: draft.questionText.trim(),
        options: draft.options.map((option) => ({
          ...option,
          text: option.text.trim(),
          explanation: option.explanation?.trim()
        }))
      });
      onDirtyChange?.(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thể lưu câu hỏi.');
    } finally {
      setSaving(false);
    }
  };

  const correctCount = draft.options.filter((option) => option.isCorrect).length;

  return (
    <div
      className="rounded-2xl border border-primary/40 bg-surface-container-low/40 p-5 space-y-4"
      onKeyDown={(e) => {
        if (e.key === 'Escape' && !saving) {
          e.preventDefault();
          e.stopPropagation();
          void cancel();
        }
        if (e.key === 'Enter' && e.ctrlKey) {
          e.preventDefault();
          void save();
        }
      }}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="rounded-xl bg-primary/10 text-primary p-2 flex items-center justify-center">
            <span className="material-symbols-outlined text-[20px]">edit_note</span>
          </div>
          <div>
            <h6 className="m-0 text-sm font-black text-slate-900">Soạn nội dung câu hỏi</h6>
            <p className="m-0 text-[11px] text-slate-500">Ctrl + Enter để lưu nhanh · Esc để hủy</p>
          </div>
        </div>
      </div>

      <fieldset disabled={saving} className="space-y-4">
        <div className="space-y-1">
          <label className="block text-xs font-bold text-slate-700">
            Nội dung câu hỏi <span className="text-rose-500">*</span>
          </label>
          <textarea
            autoFocus
            value={draft.questionText}
            onChange={(e) => change({ ...draft, questionText: e.target.value })}
            placeholder="Nhập câu hỏi rõ ràng, dễ hiểu..."
            className="w-full px-3.5 py-2.5 bg-white border border-outline-variant/70 rounded-xl text-xs sm:text-sm text-slate-900 leading-relaxed outline-none transition focus:border-primary font-medium"
            rows={3}
          />
        </div>

        <div className="grid gap-3 sm:grid-cols-[1fr_160px]">
          <div className="space-y-1">
            <label className="block text-xs font-bold text-slate-700">Loại câu hỏi</label>
            <select
              value={draft.questionType}
              onChange={(e) =>
                change({ ...draft, questionType: e.target.value as QuestionType })
              }
              className="w-full px-3.5 py-2 bg-white border border-outline-variant/70 rounded-xl text-xs sm:text-sm font-semibold text-slate-900 outline-none transition focus:border-primary"
            >
              <option value="SINGLE_CHOICE">Một đáp án đúng (Single choice)</option>
              <option value="MULTIPLE_CHOICE">Nhiều đáp án đúng (Multiple choice)</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="block text-xs font-bold text-slate-700">Điểm</label>
            <input
              type="number"
              min="0.01"
              step="0.01"
              value={draft.points}
              onChange={(e) => change({ ...draft, points: Number(e.target.value) })}
              className="w-full px-3.5 py-2 bg-white border border-outline-variant/70 rounded-xl text-xs sm:text-sm font-semibold text-slate-900 outline-none transition focus:border-primary"
            />
          </div>
        </div>

        {/* Options */}
        <div className="rounded-2xl border border-outline-variant/60 bg-white p-4 space-y-3">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="m-0 text-xs font-black text-slate-800">Các lựa chọn đáp án</p>
              <p className="m-0 text-[11px] text-slate-400">
                Đánh dấu đáp án đúng bằng nút chọn tương ứng.
              </p>
            </div>
            <span
              className={`rounded-full px-2.5 py-0.5 text-[10px] font-bold border ${
                correctCount > 0
                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                  : 'bg-amber-50 text-amber-700 border-amber-200'
              }`}
            >
              {correctCount} đáp án đúng
            </span>
          </div>

          <div className="space-y-2.5">
            {draft.options.map((option, index) => (
              <div
                key={option.id}
                className={`rounded-xl border p-3 transition ${
                  option.isCorrect
                    ? 'border-emerald-300 bg-emerald-50/40'
                    : 'border-outline-variant/60 bg-surface-container-low/30'
                }`}
              >
                <div className="flex items-center gap-2.5">
                  <input
                    aria-label={'Đáp án đúng ' + (index + 1)}
                    type={draft.questionType === 'SINGLE_CHOICE' ? 'radio' : 'checkbox'}
                    checked={Boolean(option.isCorrect)}
                    onChange={() =>
                      change({
                        ...draft,
                        options: draft.options.map((current, currentIndex) => ({
                          ...current,
                          isCorrect:
                            draft.questionType === 'SINGLE_CHOICE'
                              ? currentIndex === index
                              : currentIndex === index
                              ? !current.isCorrect
                              : current.isCorrect
                        }))
                      })
                    }
                    className="h-4 w-4 accent-emerald-600 cursor-pointer"
                  />

                  <span
                    className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-lg text-xs font-black ${
                      option.isCorrect
                        ? 'bg-emerald-600 text-white'
                        : 'bg-surface-container text-slate-600'
                    }`}
                  >
                    {String.fromCharCode(65 + index)}
                  </span>

                  <input
                    aria-label={'Đáp án ' + (index + 1)}
                    value={option.text}
                    placeholder={'Nhập nội dung lựa chọn ' + String.fromCharCode(65 + index)}
                    className="min-w-0 flex-1 rounded-xl border border-outline-variant/70 bg-white px-3 py-2 text-xs sm:text-sm text-slate-900 outline-none focus:border-primary font-medium"
                    onChange={(e) =>
                      change({
                        ...draft,
                        options: draft.options.map((current, currentIndex) =>
                          currentIndex === index ? { ...current, text: e.target.value } : current
                        )
                      })
                    }
                  />

                  <button
                    type="button"
                    disabled={draft.options.length <= 2}
                    onClick={() =>
                      change({
                        ...draft,
                        options: draft.options.filter(
                          (_, currentIndex) => currentIndex !== index
                        )
                      })
                    }
                    aria-label="Xóa lựa chọn"
                    className="rounded-lg p-1.5 text-slate-400 transition hover:bg-rose-50 hover:text-rose-600 disabled:cursor-not-allowed disabled:opacity-20 cursor-pointer"
                    title="Xóa lựa chọn này"
                  >
                    <span className="material-symbols-outlined text-[18px]">delete</span>
                  </button>
                </div>

                <textarea
                  aria-label={'Giải thích đáp án ' + (index + 1)}
                  value={option.explanation || ''}
                  placeholder="Giải thích vì sao đúng hoặc sai (không bắt buộc)..."
                  rows={1}
                  className="mt-2 block w-full rounded-xl border border-outline-variant/50 bg-white px-3 py-1.5 text-xs text-slate-700 outline-none focus:border-primary transition-all"
                  onChange={(e) =>
                    change({
                      ...draft,
                      options: draft.options.map((current, currentIndex) =>
                        currentIndex === index
                          ? { ...current, explanation: e.target.value }
                          : current
                      )
                    })
                  }
                />
              </div>
            ))}
          </div>

          <button
            type="button"
            onClick={() =>
              change({
                ...draft,
                options: [
                  ...draft.options,
                  { id: crypto.randomUUID(), text: '', isCorrect: false, explanation: '' }
                ]
              })
            }
            className="mt-2 inline-flex items-center gap-1.5 text-xs font-bold text-primary hover:text-primary-container transition cursor-pointer"
          >
            <span className="material-symbols-outlined text-[17px]">add_circle</span>
            <span>Thêm lựa chọn đáp án</span>
          </button>
        </div>
      </fieldset>

      {error && (
        <div
          role="alert"
          className="flex items-center gap-2 rounded-xl bg-rose-50 px-3 py-2 text-xs font-semibold text-rose-700 border border-rose-200"
        >
          <span className="material-symbols-outlined text-[17px]">error</span>
          <span>{error}</span>
        </div>
      )}

      <div className="flex justify-end gap-2.5 border-t border-outline-variant/60 pt-3">
        <button
          type="button"
          disabled={saving}
          onClick={() => void cancel()}
          className="rounded-xl px-4 py-2 text-xs font-bold text-slate-600 transition hover:bg-slate-200/70 disabled:opacity-50 cursor-pointer"
        >
          Hủy bỏ
        </button>
        <button
          type="button"
          disabled={saving}
          onClick={() => void save()}
          className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-5 py-2 text-xs font-bold text-white shadow-sm transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60 cursor-pointer"
        >
          <span className="material-symbols-outlined text-[17px]">save</span>
          <span>{saving ? 'Đang lưu...' : 'Lưu câu hỏi'}</span>
        </button>
      </div>
    </div>
  );
}

