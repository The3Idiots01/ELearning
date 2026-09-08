import React, { useEffect, useMemo, useState } from 'react';
import { ApiError } from '../../../lib/apiClient';
import type { QuizAnswer, QuizAttempt, StudentQuiz } from '../../../types/quiz';
import { studentQuizApi } from '../api/studentQuizApi';

interface QuizTakingViewProps {
  courseId: number;
  assessmentId: number;
  onSubmitted?: () => void;
}

export const QuizTakingView: React.FC<QuizTakingViewProps> = ({
  courseId,
  assessmentId,
  onSubmitted
}) => {
  const [quiz, setQuiz] = useState<StudentQuiz | null>(null);
  const [history, setHistory] = useState<QuizAttempt[]>([]);
  const [answers, setAnswers] = useState<Record<number, string[]>>({});
  const [result, setResult] = useState<QuizAttempt | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    Promise.all([
      studentQuizApi.getQuiz(courseId, assessmentId),
      studentQuizApi.getHistory(courseId, assessmentId)
    ])
      .then(([loadedQuiz, loadedHistory]) => {
        if (cancelled) return;
        setQuiz(loadedQuiz);
        setHistory(loadedHistory);
        setAnswers({});
        setResult(null);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : 'Không thể tải bài kiểm tra.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [assessmentId, courseId]);

  const answeredCount = useMemo(() => Object.keys(answers).filter((id) => answers[Number(id)]?.length).length, [answers]);

  const toggleOption = (questionId: number, optionId: string, multiple: boolean) => {
    setAnswers((current) => {
      const selected = current[questionId] || [];
      if (multiple) {
        return {
          ...current,
          [questionId]: selected.includes(optionId)
            ? selected.filter((id) => id !== optionId)
            : [...selected, optionId]
        };
      }
      return { ...current, [questionId]: [optionId] };
    });
  };

  const submit = async () => {
    if (!quiz || submitting || quiz.attemptsRemaining === 0) return;
    if (answeredCount !== quiz.questions.length) {
      setError('Vui lòng trả lời tất cả câu hỏi trước khi nộp bài.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const payload: QuizAnswer[] = quiz.questions.map((question) => ({
        questionId: question.id,
        selectedOptionIds: answers[question.id] || []
      }));
      const submitted = await studentQuizApi.submitAttempt(courseId, assessmentId, payload);
      setResult(submitted);
      const refreshed = await studentQuizApi.getQuiz(courseId, assessmentId);
      setQuiz(refreshed);
      setHistory(await studentQuizApi.getHistory(courseId, assessmentId));
      onSubmitted?.();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không thể nộp bài.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="p-10 text-slate-400">Đang tải bài kiểm tra...</div>;
  }
  if (error && !quiz) {
    return <div className="m-6 rounded-xl border border-rose-500/30 bg-rose-500/10 p-5 text-sm text-rose-200">{error}</div>;
  }
  if (!quiz) return null;

  return (
    <div className="max-w-4xl mx-auto w-full p-6 sm:p-10 space-y-6">
      <div className="border-b border-slate-800 pb-5">
        <div className="flex items-center justify-between gap-4">
          <div>
            <span className="text-[10px] uppercase tracking-widest text-amber-400 font-black">Bài kiểm tra</span>
            <h2 className="text-2xl font-black text-white mt-2">{quiz.title}</h2>
          </div>
          <div className="text-right text-xs text-slate-400">
            <div>Đạt: <strong className="text-emerald-400">{quiz.passingScore}%</strong></div>
            <div>Còn lại: <strong className="text-white">{quiz.attemptsRemaining ?? 'Không giới hạn'}</strong></div>
          </div>
        </div>
      </div>

      {quiz.hasPassed && !result && (
        <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-4 text-sm text-emerald-200">Bạn đã vượt qua bài kiểm tra này.</div>
      )}
      {error && <div className="rounded-xl border border-rose-500/30 bg-rose-500/10 p-4 text-sm text-rose-200">{error}</div>}
      {result && (
        <div className={`rounded-2xl border p-5 ${result.isPassed ? 'border-emerald-500/30 bg-emerald-500/10' : 'border-amber-500/30 bg-amber-500/10'}`}>
          <div className="text-lg font-black text-white">Kết quả: {Number(result.score).toFixed(2)}%</div>
          <p className="text-sm text-slate-200 mt-1">{result.isPassed ? 'Chúc mừng, bạn đã đạt!' : 'Bạn chưa đạt. Hãy xem lại bài học và thử lại.'}</p>
          {result.questionResults && result.questionResults.length > 0 && (
            <div className="mt-4 space-y-2">
              {result.questionResults.map((item, index) => (
                <div key={item.questionId} className="flex items-center justify-between rounded-lg bg-black/10 px-3 py-2 text-xs text-slate-200">
                  <span>Câu {index + 1}</span>
                  <span className={item.isCorrect ? 'text-emerald-300 font-bold' : 'text-rose-200 font-bold'}>{item.isCorrect ? 'Đúng' : 'Chưa đúng'} · {Number(item.earnedPoints).toFixed(2)}/{Number(item.totalPoints).toFixed(2)}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      <div className="space-y-4">
        {quiz.questions.map((question, index) => {
          const multiple = question.questionType === 'MULTIPLE_CHOICE';
          const selected = answers[question.id] || [];
          return (
            <section key={question.id} className="rounded-2xl border border-slate-800 bg-slate-900 p-5">
              <h3 className="text-sm font-bold text-white leading-relaxed">{index + 1}. {question.questionText}</h3>
              <div className="mt-4 space-y-2">
                {question.options.map((option) => (
                  <label key={option.id} className={`flex items-center gap-3 rounded-xl border px-4 py-3 text-sm cursor-pointer transition-colors ${selected.includes(option.id) ? 'border-indigo-400 bg-indigo-500/10 text-white' : 'border-slate-700 text-slate-300 hover:border-slate-500'}`}>
                    <input
                      type={multiple ? 'checkbox' : 'radio'}
                      name={`question-${question.id}`}
                      checked={selected.includes(option.id)}
                      onChange={() => toggleOption(question.id, option.id, multiple)}
                    />
                    <span>{option.text}</span>
                  </label>
                ))}
              </div>
            </section>
          );
        })}
      </div>

      <div className="flex items-center justify-between gap-4">
        <span className="text-xs text-slate-400">Đã trả lời {answeredCount}/{quiz.questions.length} câu</span>
        <button
          onClick={submit}
          disabled={submitting || quiz.attemptsRemaining === 0 || quiz.hasPassed}
          className="rounded-xl bg-indigo-600 px-6 py-3 text-sm font-black text-white hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {submitting ? 'Đang chấm...' : 'Nộp bài'}
        </button>
      </div>

      {history.length > 0 && (
        <div className="border-t border-slate-800 pt-5">
          <h3 className="text-xs font-black uppercase tracking-wider text-slate-400">Lịch sử làm bài</h3>
          <div className="mt-3 space-y-2">
            {history.map((attempt) => (
              <div key={attempt.id} className="flex justify-between rounded-xl bg-slate-900 px-4 py-3 text-xs text-slate-300">
                <span>{new Date(attempt.submittedAt).toLocaleString('vi-VN')}</span>
                <span className={attempt.isPassed ? 'text-emerald-400 font-bold' : 'text-rose-300 font-bold'}>{Number(attempt.score).toFixed(2)}%</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
