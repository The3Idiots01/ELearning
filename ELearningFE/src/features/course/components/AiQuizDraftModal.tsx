import { useMemo, useState, type ReactNode } from 'react';
import type { Assessment } from '../../../types/course';
import type {
  GenerateQuizDraftRequest,
  QuestionType,
  QuizDetail,
  QuizDifficulty,
  QuizDraft,
  UpsertQuestionRequest
} from '../../../types/quiz';
import { Modal } from '../../../components/common/Modal';
import { useToast } from '../../../app/context/ToastContext';
import { quizApi } from '../api/quizApi';
import { newQuestion, QuestionEditor } from './QuestionEditor';

interface Props {
  courseId: number;
  assessment: Assessment;
  existingQuestionCount: number;
  isOpen: boolean;
  onClose: () => void;
  onApplied: (quiz: QuizDetail) => Promise<void> | void;
}

const inputClass = 'w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-xs font-normal text-slate-900 outline-none transition focus:border-violet-500';

const difficultyLabel: Record<QuizDifficulty, string> = {
  EASY: 'Dễ',
  MEDIUM: 'Trung bình',
  HARD: 'Nâng cao',
  MIXED: 'Hỗn hợp'
};

const withOptionIds = (question: UpsertQuestionRequest): UpsertQuestionRequest => ({
  ...question,
  options: question.options.map((option) => ({
    ...option,
    id: option.id || crypto.randomUUID()
  }))
});

export function AiQuizDraftModal({
  courseId,
  assessment,
  existingQuestionCount,
  isOpen,
  onClose,
  onApplied
}: Props) {
  const { showSuccess, showError } = useToast();
  const [questionCount, setQuestionCount] = useState('');
  const [totalPoints, setTotalPoints] = useState('');
  const [difficulty, setDifficulty] = useState<QuizDifficulty | ''>('');
  const [questionType, setQuestionType] = useState<QuestionType | ''>('');
  const [contentGuidance, setContentGuidance] = useState('');
  const [questionGuidance, setQuestionGuidance] = useState('');
  const [draft, setDraft] = useState<QuizDraft | null>(null);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isApplying, setIsApplying] = useState(false);

  const calculatedPoints = useMemo(
    () => draft?.questions.reduce((sum, question) => sum + Number(question.points), 0) || 0,
    [draft]
  );

  const generate = async () => {
    const request: GenerateQuizDraftRequest = {
      questionCount: questionCount ? Number(questionCount) : undefined,
      totalPoints: totalPoints ? Number(totalPoints) : undefined,
      difficulty: difficulty || undefined,
      questionType: questionType || undefined,
      contentGuidance: contentGuidance.trim() || undefined,
      questionGuidance: questionGuidance.trim() || undefined
    };
    if (request.questionCount !== undefined
      && (!Number.isInteger(request.questionCount) || request.questionCount < 1 || request.questionCount > 30)) {
      showError('Số câu phải là số nguyên từ 1 đến 30.');
      return;
    }
    if (request.questionCount && request.totalPoints
      && request.totalPoints < request.questionCount * 0.01) {
      showError('Tổng điểm phải đủ để mỗi câu có ít nhất 0,01 điểm.');
      return;
    }

    setIsGenerating(true);
    try {
      const result = await quizApi.generateDraft(courseId, assessment.id, request);
      setDraft({ ...result, questions: result.questions.map(withOptionIds) });
      setEditingIndex(null);
      showSuccess('AI đã tạo bản nháp. Bạn có thể sửa từng câu trước khi áp dụng.');
    } catch (error) {
      showError(error instanceof Error ? error.message : 'Không thể tạo quiz bằng AI.');
    } finally {
      setIsGenerating(false);
    }
  };

  const updateQuestion = async (index: number, question: UpsertQuestionRequest) => {
    setDraft((current) => current && ({
      ...current,
      questions: current.questions.map((item, currentIndex) =>
        currentIndex === index ? question : item)
    }));
    setEditingIndex(null);
  };

  const apply = async () => {
    if (!draft || isApplying) return;
    if (draft.questions.length === 0) {
      showError('Bản nháp cần có ít nhất một câu hỏi.');
      return;
    }

    setIsApplying(true);
    try {
      const quiz = await quizApi.applyDraft(courseId, assessment.id, {
        questions: draft.questions
      });
      showSuccess(`Đã thêm ${draft.questions.length} câu hỏi AI vào bài quiz.`);
      setDraft(null);
      setEditingIndex(null);
      await onApplied(quiz);
      onClose();
    } catch (error) {
      showError(error instanceof Error ? error.message : 'Không thể áp dụng bản nháp quiz.');
    } finally {
      setIsApplying(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="AI tạo bài quiz"
      subtitle="Điền điều bạn muốn kiểm soát; các ô để trống sẽ được AI tự đề xuất."
      icon="auto_awesome"
      maxWidth="4xl"
    >
      {!draft ? (
        <div className="space-y-5">
          <div className="rounded-xl border border-violet-200 bg-violet-50 p-3 text-xs leading-relaxed text-violet-800">
            AI sẽ dùng tên “{assessment.title}”, hướng dẫn làm bài và{' '}
            {assessment.outcomeIds?.length
              ? `${assessment.outcomeIds.length} learning outcomes đã gắn`
              : 'các learning outcomes của khóa học'}.
            {' '}Nội dung bài học cùng chương được ưu tiên; nếu bài kiểm tra
            chưa nằm trong chương, AI sẽ tham khảo toàn bộ khóa học.
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Số câu (tùy chọn)">
              <input type="number" min={1} max={30} value={questionCount}
                onChange={(event) => setQuestionCount(event.target.value)}
                placeholder="AI đề xuất 5–12 câu" className={inputClass} />
            </Field>
            <Field label="Tổng điểm (tùy chọn)">
              <input type="number" min="0.01" max={1000} step="0.01" value={totalPoints}
                onChange={(event) => setTotalPoints(event.target.value)}
                placeholder="Để AI tự phân bổ" className={inputClass} />
            </Field>
            <Field label="Mức độ (tùy chọn)">
              <select value={difficulty}
                onChange={(event) => setDifficulty(event.target.value as QuizDifficulty | '')}
                className={inputClass}>
                <option value="">Để AI tự đề xuất</option>
                <option value="EASY">Dễ</option>
                <option value="MEDIUM">Trung bình</option>
                <option value="HARD">Nâng cao</option>
                <option value="MIXED">Hỗn hợp, tăng dần</option>
              </select>
            </Field>
            <Field label="Dạng câu hỏi (tùy chọn)">
              <select value={questionType}
                onChange={(event) => setQuestionType(event.target.value as QuestionType | '')}
                className={inputClass}>
                <option value="">AI phối hợp phù hợp</option>
                <option value="SINGLE_CHOICE">Một đáp án đúng</option>
                <option value="MULTIPLE_CHOICE">Nhiều đáp án đúng</option>
              </select>
            </Field>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Nội dung / chủ đề cần kiểm tra (tùy chọn)">
              <textarea rows={4} maxLength={12000} value={contentGuidance}
                onChange={(event) => setContentGuidance(event.target.value)}
                placeholder="Ví dụ: tập trung REST API, validation và xử lý lỗi; bỏ qua deployment…"
                className={`${inputClass} leading-relaxed`} />
            </Field>
            <Field label="Định hướng cách ra câu hỏi (tùy chọn)">
              <textarea rows={4} maxLength={2000} value={questionGuidance}
                onChange={(event) => setQuestionGuidance(event.target.value)}
                placeholder="Ví dụ: ưu tiên tình huống thực tế, tránh ghi nhớ máy móc, đáp án nhiễu hợp lý…"
                className={`${inputClass} leading-relaxed`} />
            </Field>
          </div>

          <div className="flex justify-end border-t border-slate-200 pt-4">
            <button type="button" disabled={isGenerating} onClick={() => void generate()}
              className="rounded-xl bg-violet-600 px-5 py-2.5 text-xs font-extrabold text-white shadow-sm transition hover:bg-violet-700 disabled:opacity-50">
              {isGenerating ? 'Gemini đang soạn câu hỏi…' : '✨ Tạo bản nháp bằng AI'}
            </button>
          </div>
        </div>
      ) : (
        <div className="space-y-5">
          {existingQuestionCount > 0 && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs text-amber-800">
              Quiz đang có {existingQuestionCount} câu. Khi áp dụng, câu hỏi AI sẽ được thêm vào cuối;
              câu hỏi hiện tại không bị xóa.
            </div>
          )}

          <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-xs font-bold text-slate-600">
            <span className="rounded-full border border-violet-200 bg-violet-50 px-2.5 py-1 text-violet-700">
              {difficultyLabel[draft.difficulty]}
            </span>
            <span>{draft.questions.length} câu · {calculatedPoints.toFixed(2)} điểm</span>
            <span className="text-slate-400">· Dùng cấu hình chung đã lưu của “{assessment.title}”</span>
          </div>

          <div className="space-y-3">
            {draft.questions.map((question, index) => (
              <article key={index} className="rounded-2xl border border-slate-200 bg-white p-4">
                {editingIndex === index ? (
                  <QuestionEditor
                    initial={question}
                    onSave={(value) => updateQuestion(index, value)}
                    onCancel={() => setEditingIndex(null)}
                  />
                ) : (
                  <div className="flex items-start gap-3">
                    <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-violet-100 text-xs font-black text-violet-700">
                      {index + 1}
                    </span>
                    <div className="min-w-0 flex-1">
                      <div className="mb-1.5 flex flex-wrap gap-1.5 text-[10px] font-bold">
                        <span className="rounded-full bg-slate-100 px-2 py-0.5 text-slate-600">
                          {question.questionType === 'SINGLE_CHOICE' ? 'Một đáp án' : 'Nhiều đáp án'}
                        </span>
                        <span className="rounded-full bg-amber-50 px-2 py-0.5 text-amber-700">
                          {Number(question.points)} điểm
                        </span>
                      </div>
                      <p className="m-0 text-sm font-bold leading-relaxed text-slate-900">{question.questionText}</p>
                      <p className="m-0 mt-1 text-xs text-slate-400">
                        {question.options.length} lựa chọn · {question.options.filter((option) => option.isCorrect).length} đáp án đúng
                      </p>
                    </div>
                    {editingIndex === null && <div className="flex shrink-0 gap-1">
                      <button type="button" title="Sửa câu hỏi" onClick={() => setEditingIndex(index)}
                        className="rounded-lg p-2 text-slate-400 hover:bg-violet-50 hover:text-violet-700">
                        <span className="material-symbols-outlined text-[18px]">edit</span>
                      </button>
                      <button type="button" title="Bỏ câu hỏi" onClick={() => {
                        setDraft({
                          ...draft,
                          questions: draft.questions.filter((_, currentIndex) => currentIndex !== index)
                        });
                        setEditingIndex(null);
                      }} className="rounded-lg p-2 text-slate-400 hover:bg-rose-50 hover:text-rose-600">
                        <span className="material-symbols-outlined text-[18px]">delete</span>
                      </button>
                    </div>}
                  </div>
                )}
              </article>
            ))}
          </div>

          {editingIndex === draft.questions.length ? (
            <QuestionEditor
              initial={newQuestion()}
              onSave={async (value) => {
                setDraft({ ...draft, questions: [...draft.questions, value] });
                setEditingIndex(null);
              }}
              onCancel={() => setEditingIndex(null)}
            />
          ) : (
            <button type="button" onClick={() => setEditingIndex(draft.questions.length)}
              className="text-xs font-bold text-primary hover:underline">
              + Thêm câu hỏi vào bản nháp
            </button>
          )}

          <div className="flex flex-wrap justify-between gap-2 border-t border-slate-200 pt-4">
            <button type="button" onClick={() => { setDraft(null); setEditingIndex(null); }}
              className="rounded-xl bg-slate-100 px-4 py-2 text-xs font-bold text-slate-600">
              Thay đổi yêu cầu · Tạo lại
            </button>
            <button type="button" disabled={isApplying || draft.questions.length === 0}
              onClick={() => void apply()}
              className="rounded-xl bg-primary px-5 py-2.5 text-xs font-extrabold text-white disabled:opacity-50">
              {isApplying ? 'Đang áp dụng…' : `Review xong · Thêm ${draft.questions.length} câu vào quiz`}
            </button>
          </div>
        </div>
      )}
    </Modal>
  );
}

function Field({ label, className = '', children }: {
  label: string;
  className?: string;
  children: ReactNode;
}) {
  return (
    <label className={`block space-y-1 text-xs font-bold text-slate-700 ${className}`}>
      <span>{label}</span>
      {children}
    </label>
  );
}
