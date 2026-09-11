import { useCallback, useEffect, useRef, useState } from 'react';
import type { Assessment } from '../../../types/course';
import type { QuizDetail, QuizQuestion, UpsertQuestionRequest } from '../../../types/quiz';
import { quizApi } from '../api/quizApi';
import { ApiError } from '../../../lib/apiClient';
import { useConfirm } from '../../../app/context/ConfirmContext';
import { QuestionEditor, newQuestion } from './QuestionEditor';
import { AiQuizDraftModal } from './AiQuizDraftModal';

const questionTypeLabel = (type: QuizQuestion['questionType']) =>
  type === 'MULTIPLE_CHOICE' ? 'Nhiều đáp án đúng' : 'Một đáp án đúng';

export function QuizAuthoringView({ courseId, assessment, onChanged }: {
  courseId: number; assessment: Assessment; onChanged?: () => void;
}) {
  const confirm = useConfirm();
  const [quiz, setQuiz] = useState<QuizDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [aiModalOpen, setAiModalOpen] = useState(false);
  const [editor, setEditor] = useState<{ id: number | 'new'; initial: UpsertQuestionRequest } | null>(null);
  const dirty = useRef(false);
  const sequence = useRef(0);

  const load = useCallback(async () => {
    const seq = ++sequence.current;
    setLoading(true); setLoadError('');
    try {
      const data = await quizApi.getQuiz(courseId, assessment.id);
      if (seq !== sequence.current) return;
      setQuiz(data);
    } catch (e) {
      if (seq !== sequence.current) return;
      if (e instanceof ApiError && e.status === 404) setQuiz(null);
      else setLoadError(e instanceof Error ? e.message : 'Không thể tải bài kiểm tra.');
    } finally {
      if (seq === sequence.current) setLoading(false);
    }
  }, [courseId, assessment.id]);

  useEffect(() => {
    void load();
    const activeSequence = sequence;
    return () => { activeSequence.current++; };
  }, [load]);

  const open = async (question?: QuizQuestion) => {
    if (dirty.current && !await confirm('Câu hỏi đang sửa chưa được lưu.', false)) return;
    dirty.current = false;
    setError('');
    setEditor(question ? {
      id: question.id,
      initial: {
        questionText: question.questionText,
        questionType: question.questionType,
        points: Number(question.points),
        options: question.options.map(option => ({ ...option, id: option.id || crypto.randomUUID() }))
      }
    } : { id: 'new', initial: newQuestion() });
  };

  const savedQuestion = async (draft: UpsertQuestionRequest) => {
    if (!editor) return;
    if (editor.id === 'new') await quizApi.addQuestion(courseId, assessment.id, draft);
    else await quizApi.updateQuestion(courseId, assessment.id, editor.id, draft);
    dirty.current = false;
    setEditor(null);
    await load();
    onChanged?.();
  };

  const remove = async (question: QuizQuestion) => {
    if (saving || !await confirm('Xóa câu hỏi này?')) return;
    setSaving(true); setError('');
    try {
      await quizApi.deleteQuestion(courseId, assessment.id, question.id);
      if (editor?.id === question.id) setEditor(null);
      await load();
      onChanged?.();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thể xóa câu hỏi.');
    } finally { setSaving(false); }
  };

  if (loading) return <div className="rounded-3xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500">Đang tải bài kiểm tra...</div>;
  if (loadError) return <div role="alert" className="rounded-3xl border border-rose-200 bg-rose-50 p-5 text-sm text-rose-700">
    <div className="flex items-center gap-3"><span className="material-symbols-outlined">error</span><span>{loadError}</span>
      <button type="button" onClick={() => void load()} className="ml-auto rounded-xl bg-white px-3 py-2 font-bold text-rose-700 shadow-sm">Thử lại</button></div>
  </div>;

  const questions = quiz?.questions || [];
  const totalPoints = quiz?.totalPoints || 0;

  return (
    <div className="space-y-6">
      {/* Questions Section */}
      <section className="rounded-2xl border border-outline-variant/60 bg-white p-5 shadow-xs space-y-4">
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[20px]">list_alt</span>
            <div>
              <div className="flex items-center gap-2">
                <h5 className="m-0 text-sm font-black text-slate-900">Danh sách câu hỏi</h5>
                <span className="text-[11px] font-bold px-2.5 py-0.5 rounded-full bg-surface-container text-slate-600 border border-outline-variant/50">
                  {questions.length} câu · {totalPoints} điểm
                </span>
              </div>
              <p className="m-0 text-xs text-slate-500">Chọn đáp án đúng, thêm giải thích và điểm cho từng câu.</p>
            </div>
          </div>
          {!editor && (
            <div className="flex shrink-0 flex-wrap justify-end gap-2">
              <button
                type="button"
                onClick={() => setAiModalOpen(true)}
                className="flex items-center gap-1.5 rounded-xl bg-violet-600 px-4 py-2 text-xs font-bold text-white shadow-xs transition hover:bg-violet-700 active:scale-95"
              >
                <span className="material-symbols-outlined text-[18px]">auto_awesome</span>
                <span>Tạo bằng AI</span>
              </button>
              <button
                type="button"
                onClick={() => void open()}
                className="bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs px-4 py-2 rounded-xl transition shadow-xs flex items-center gap-1.5 cursor-pointer active:scale-95"
              >
                <span className="material-symbols-outlined text-[18px]">add</span>
                <span>Thêm thủ công</span>
              </button>
            </div>
          )}
        </div>

        {error && (
          <div role="alert" className="flex items-center gap-1.5 rounded-xl border border-rose-200 bg-rose-50 p-2.5 text-xs font-semibold text-rose-600">
            <span className="material-symbols-outlined text-[16px]">error</span>
            <span>{error}</span>
          </div>
        )}

        {editor?.id === 'new' && (
          <div className="animate-in fade-in">
            <QuestionEditor
              initial={editor.initial}
              onSave={savedQuestion}
              onCancel={() => setEditor(null)}
              onDirtyChange={(value) => {
                dirty.current = value;
              }}
            />
          </div>
        )}

        {questions.length === 0 && !editor ? (
          <div className="rounded-2xl border-2 border-dashed border-outline-variant/70 bg-white px-6 py-10 text-center">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
              <span className="material-symbols-outlined text-[28px]">playlist_add</span>
            </div>
            <h6 className="mt-3 text-sm font-black text-slate-800">Bài kiểm tra chưa có câu hỏi nào</h6>
            <p className="mx-auto mt-1 max-w-sm text-xs leading-relaxed text-slate-500">
              Bắt đầu bằng cách thêm câu hỏi trắc nghiệm đầu tiên cho học viên.
            </p>
            <button
              type="button"
              onClick={() => void open()}
              className="mt-3 rounded-xl bg-primary px-4 py-2 text-xs font-bold text-white hover:bg-primary/90 transition cursor-pointer"
            >
              Tạo câu hỏi đầu tiên
            </button>
          </div>
        ) : (
          <div className="space-y-3">
            {questions.map((question) => (
              <article
                key={question.id}
                className={`rounded-2xl border bg-white p-4 transition-all ${
                  editor?.id === question.id
                    ? 'border-primary ring-2 ring-primary/10 shadow-sm'
                    : 'border-outline-variant/60 hover:border-outline hover:shadow-xs'
                }`}
              >
                <div className="flex items-start gap-3">
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-xs font-black text-primary">
                    {question.position + 1}
                  </span>
                  <div className="min-w-0 flex-1 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-full bg-surface-container px-2.5 py-0.5 text-[10px] font-bold text-slate-700 border border-outline-variant/50">
                        {questionTypeLabel(question.questionType)}
                      </span>
                      <span className="rounded-full bg-amber-50 px-2.5 py-0.5 text-[10px] font-bold text-amber-700 border border-amber-200">
                        {Number(question.points)} điểm
                      </span>
                    </div>
                    <p className="m-0 text-xs sm:text-sm font-bold leading-relaxed text-slate-900">
                      {question.questionText}
                    </p>
                    <p className="m-0 text-xs text-slate-400">
                      {question.options.length} lựa chọn · {question.options.filter((o) => o.isCorrect).length} đáp án đúng
                    </p>
                  </div>

                  <div className="flex shrink-0 gap-1">
                    <button
                      type="button"
                      disabled={saving}
                      onClick={() => void open(question)}
                      aria-label="Sửa câu hỏi"
                      className="rounded-xl p-2 text-slate-400 transition hover:bg-primary/10 hover:text-primary disabled:opacity-50 cursor-pointer"
                      title="Chỉnh sửa câu hỏi"
                    >
                      <span className="material-symbols-outlined text-[18px]">edit</span>
                    </button>
                    <button
                      type="button"
                      disabled={saving}
                      onClick={() => void remove(question)}
                      aria-label="Xóa câu hỏi"
                      className="rounded-xl p-2 text-slate-400 transition hover:bg-rose-50 hover:text-rose-600 disabled:opacity-50 cursor-pointer"
                      title="Xóa câu hỏi"
                    >
                      <span className="material-symbols-outlined text-[18px]">delete</span>
                    </button>
                  </div>
                </div>

                {editor?.id === question.id && (
                  <div className="mt-4 border-t border-slate-100 pt-4 animate-in fade-in">
                    <QuestionEditor
                      key={editor.id}
                      initial={editor.initial}
                      onSave={savedQuestion}
                      onCancel={() => setEditor(null)}
                      onDirtyChange={(value) => {
                        dirty.current = value;
                      }}
                    />
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </section>

      <AiQuizDraftModal
        courseId={courseId}
        assessment={assessment}
        existingQuestionCount={questions.length}
        isOpen={aiModalOpen}
        onClose={() => setAiModalOpen(false)}
        onApplied={async () => {
          await load();
          onChanged?.();
        }}
      />
    </div>
  );
}
