import React, { useState } from 'react';
import type { LearningOutcome } from '../../../types/course';
import { outcomeApi } from '../api/outcomeApi';
import { useToast } from '../../../app/context/ToastContext';

interface LearningOutcomesEditorProps {
  courseId: number;
  outcomes: LearningOutcome[];
  onChanged: (outcomes: LearningOutcome[]) => void;
}

export const LearningOutcomesEditor: React.FC<LearningOutcomesEditorProps> = ({ courseId, outcomes, onChanged }) => {
  const { showError, showSuccess } = useToast();
  const [draft, setDraft] = useState('');
  const [saving, setSaving] = useState(false);

  const add = async () => {
    if (!draft.trim()) return;
    setSaving(true);
    try {
      const created = await outcomeApi.create(courseId, draft.trim());
      onChanged([...outcomes, created]);
      setDraft('');
      showSuccess('Đã thêm learning outcome.');
    } catch (err: any) {
      showError(err.message || 'Không thể thêm learning outcome.');
    } finally {
      setSaving(false);
    }
  };

  const edit = async (outcome: LearningOutcome) => {
    const statement = window.prompt('Cập nhật learning outcome', outcome.statement);
    if (!statement?.trim() || statement.trim() === outcome.statement) return;
    try {
      const updated = await outcomeApi.update(courseId, outcome.id, statement.trim());
      onChanged(outcomes.map((item) => (item.id === updated.id ? updated : item)));
    } catch (err: any) {
      showError(err.message || 'Không thể cập nhật learning outcome.');
    }
  };

  const remove = async (outcome: LearningOutcome) => {
    if (!window.confirm('Xóa learning outcome này?')) return;
    try {
      await outcomeApi.remove(courseId, outcome.id);
      onChanged(outcomes.filter((item) => item.id !== outcome.id));
    } catch (err: any) {
      showError(err.message || 'Không thể xóa learning outcome.');
    }
  };

  return (
    <div className="bg-surface-container-lowest p-6 sm:p-8 rounded-3xl border border-outline-variant/70 shadow-xs space-y-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h3 className="text-base font-extrabold text-slate-900 m-0 flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[22px]">account_tree</span>
            <span>Learning outcomes (kết quả đầu ra)</span>
          </h3>
          <p className="text-xs text-slate-500 mt-1 m-0">Tối thiểu 2 kết quả đầu ra. Đây là nguồn dữ liệu dùng chung cho lesson, assessment và câu hỏi.</p>
        </div>
        <span className={`text-xs font-bold px-2.5 py-1 rounded-full border ${outcomes.length >= 2 ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-amber-50 text-amber-700 border-amber-200'}`}>
          {outcomes.length}/2
        </span>
      </div>
      <div className="space-y-2">
        {outcomes.map((outcome, index) => (
          <div key={outcome.id} className="flex items-center gap-3 rounded-xl bg-slate-50 border border-slate-200 px-3 py-2.5">
            <span className="text-xs font-black text-slate-400 w-5">{index + 1}.</span>
            <span className="text-sm text-slate-800 flex-1">{outcome.statement}</span>
            <button type="button" onClick={() => void edit(outcome)} className="text-xs font-bold text-primary cursor-pointer">Sửa</button>
            <button type="button" onClick={() => void remove(outcome)} className="text-xs font-bold text-rose-600 cursor-pointer">Xóa</button>
          </div>
        ))}
      </div>
      <div className="flex gap-2">
        <input value={draft} onChange={(event) => setDraft(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); void add(); } }} placeholder="Ví dụ: Xây dựng được REST API có xác thực..." className="flex-1 px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-primary" />
        <button type="button" disabled={saving || !draft.trim()} onClick={() => void add()} className="px-4 py-3 rounded-xl bg-primary text-white text-xs font-black disabled:opacity-50 cursor-pointer">Thêm</button>
      </div>
    </div>
  );
};
