import React, { useEffect, useState } from 'react';
import type { Assessment } from '../../../types/course';
import type { QuizDetail, QuizOption, QuizQuestion, QuestionType } from '../../../types/quiz';
import { quizApi } from '../api/quizApi';
import { useToast } from '../../../app/context/ToastContext';

interface QuizAuthoringViewProps {
  courseId: number;
  assessment: Assessment;
  onChanged?: () => void;
}

const newOptions = (): QuizOption[] => [
  { id: 'opt_1', text: '', isCorrect: true },
  { id: 'opt_2', text: '', isCorrect: false }
];

export const QuizAuthoringView: React.FC<QuizAuthoringViewProps> = ({ courseId, assessment, onChanged }) => {
  const { showError, showSuccess } = useToast();
  const [quiz, setQuiz] = useState<QuizDetail | null>(null);
  const [passingScore, setPassingScore] = useState(80);
  const [maxAttempts, setMaxAttempts] = useState<number | null>(3);
  const [questionText, setQuestionText] = useState('');
  const [questionType, setQuestionType] = useState<QuestionType>('SINGLE_CHOICE');
  const [points, setPoints] = useState(1);
  const [options, setOptions] = useState<QuizOption[]>(newOptions());
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const data = await quizApi.getQuiz(courseId, assessment.id);
      setQuiz(data);
      setPassingScore(Number(data.passingScore));
      setMaxAttempts(data.maxAttempts ?? null);
    } catch {
      setQuiz(null);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { void load(); }, [courseId, assessment.id]);

  const saveConfig = async () => {
    setSaving(true);
    try {
      const data = await quizApi.upsertQuiz(courseId, assessment.id, { passingScore, maxAttempts });
      setQuiz(data);
      showSuccess('Đã lưu cấu hình quiz.');
      onChanged?.();
    } catch (err: any) {
      showError(err.message || 'Không thể lưu cấu hình quiz.');
    } finally {
      setSaving(false);
    }
  };

  const addQuestion = async () => {
    if (!questionText.trim() || options.some((option) => !option.text?.trim())) return;
    try {
      await quizApi.addQuestion(courseId, assessment.id, {
        questionText: questionText.trim(),
        questionType,
        points,
        options
      });
      setQuestionText('');
      setOptions(newOptions());
      await load();
      showSuccess('Đã thêm câu hỏi.');
      onChanged?.();
    } catch (err: any) {
      showError(err.message || 'Không thể thêm câu hỏi.');
    }
  };

  const removeQuestion = async (question: QuizQuestion) => {
    if (!window.confirm('Xóa câu hỏi này?')) return;
    try {
      await quizApi.deleteQuestion(courseId, assessment.id, question.id);
      await load();
    } catch (err: any) {
      showError(err.message || 'Không thể xóa câu hỏi.');
    }
  };

  const editQuestion = async (question: QuizQuestion) => {
    const text = window.prompt('Nội dung câu hỏi', question.questionText);
    if (!text?.trim() || text.trim() === question.questionText) return;
    try {
      await quizApi.updateQuestion(courseId, assessment.id, question.id, {
        questionText: text.trim(),
        questionType: question.questionType,
        points: Number(question.points),
        options: question.options
      });
      await load();
    } catch (err: any) {
      showError(err.message || 'Không thể cập nhật câu hỏi.');
    }
  };

  const updateOption = (index: number, value: Partial<QuizOption>) => setOptions((current) => current.map((option, optionIndex) => optionIndex === index ? { ...option, ...value } : option));

  if (loading) return <div className="p-4 text-xs text-slate-400">Đang tải cấu hình quiz...</div>;

  return (
    <div className="mt-4 rounded-2xl border border-indigo-200 bg-indigo-50/50 p-4 space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h4 className="text-sm font-black text-slate-900 m-0">Cấu hình câu hỏi: {assessment.title}</h4>
        <span className="text-xs font-bold text-slate-500">{quiz?.questions.length || 0} câu</span>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <label className="text-xs font-bold text-slate-700">Điểm đạt (%)<input type="number" min={0} max={100} value={passingScore} onChange={(event) => setPassingScore(Number(event.target.value))} className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2" /></label>
        <label className="text-xs font-bold text-slate-700">Số lần làm tối đa<input type="number" min={1} value={maxAttempts ?? ''} onChange={(event) => setMaxAttempts(event.target.value ? Number(event.target.value) : null)} className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2" placeholder="Không giới hạn" /></label>
      </div>
      <button type="button" onClick={() => void saveConfig()} disabled={saving} className="rounded-lg bg-indigo-600 px-3 py-2 text-xs font-bold text-white disabled:opacity-50">Lưu cấu hình</button>

      <div className="border-t border-indigo-100 pt-4 space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
          <input value={questionText} onChange={(event) => setQuestionText(event.target.value)} placeholder="Nội dung câu hỏi" className="rounded-lg border border-slate-200 px-3 py-2 text-xs sm:col-span-2" />
          <select value={questionType} onChange={(event) => setQuestionType(event.target.value as QuestionType)} className="rounded-lg border border-slate-200 px-3 py-2 text-xs"><option value="SINGLE_CHOICE">Một đáp án đúng</option><option value="MULTIPLE_CHOICE">Nhiều đáp án đúng</option></select>
          <input type="number" min={0.01} step={0.01} value={points} onChange={(event) => setPoints(Number(event.target.value))} placeholder="Điểm câu hỏi" className="rounded-lg border border-slate-200 px-3 py-2 text-xs" />
        </div>
        <div className="space-y-2">
          {options.map((option, index) => (
            <div key={option.id || index} className="flex gap-2 items-center">
              <input type={questionType === 'MULTIPLE_CHOICE' ? 'checkbox' : 'radio'} checked={Boolean(option.isCorrect)} onChange={() => setOptions((current) => current.map((item, itemIndex) => questionType === 'SINGLE_CHOICE' ? { ...item, isCorrect: itemIndex === index } : itemIndex === index ? { ...item, isCorrect: !item.isCorrect } : item))} />
              <input value={option.text} onChange={(event) => updateOption(index, { text: event.target.value })} placeholder={`Đáp án ${index + 1}`} className="flex-1 rounded-lg border border-slate-200 px-3 py-2 text-xs" />
              {options.length > 2 && <button type="button" onClick={() => setOptions((current) => current.filter((_, itemIndex) => itemIndex !== index))} className="text-rose-600 text-xs">Xóa</button>}
            </div>
          ))}
        </div>
        <div className="flex gap-2">
          <button type="button" onClick={() => setOptions((current) => [...current, { id: `opt_${current.length + 1}`, text: '', isCorrect: false }])} className="rounded-lg border border-slate-300 px-3 py-2 text-xs font-bold text-slate-700">Thêm đáp án</button>
          <button type="button" onClick={() => void addQuestion()} className="rounded-lg bg-emerald-600 px-3 py-2 text-xs font-bold text-white">Thêm câu hỏi</button>
        </div>
      </div>

      <div className="space-y-2">
        {(quiz?.questions || []).map((question) => (
          <div key={question.id} className="flex items-center gap-3 rounded-xl bg-white border border-slate-200 px-3 py-2 text-xs">
            <span className="font-bold text-slate-500">#{question.position + 1}</span><span className="flex-1 text-slate-800">{question.questionText}</span>
            <button type="button" onClick={() => void editQuestion(question)} className="font-bold text-primary">Sửa</button>
            <button type="button" onClick={() => void removeQuestion(question)} className="font-bold text-rose-600">Xóa</button>
          </div>
        ))}
      </div>
    </div>
  );
};
