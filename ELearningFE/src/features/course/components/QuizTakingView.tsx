<<<<<<< HEAD
import React, { useEffect, useState } from 'react';
import { useToast } from '../../../app/context/ToastContext';
import { learnerQuizApi } from '../api/learnerQuizApi';
import type { Lesson } from '../../../types/course';
import type {
  QuizAttemptResult,
  QuizTakingInfo,
  QuestionTakingItem
} from '../../../types/quiz';

interface QuizTakingViewProps {
  courseId: number;
  lesson: Lesson;
  onLessonCompleted?: () => void;
}

type ViewMode = 'INTRO' | 'TAKING' | 'RESULT';

export const QuizTakingView: React.FC<QuizTakingViewProps> = ({
  courseId,
  lesson,
  onLessonCompleted
}) => {
  const { showSuccess, showError } = useToast();

  const [mode, setMode] = useState<ViewMode>('INTRO');
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [quizInfo, setQuizInfo] = useState<QuizTakingInfo | null>(null);
  const [history, setHistory] = useState<QuizAttemptResult[]>([]);

  // Taking state: questionId -> array of selected option IDs
  const [userAnswers, setUserAnswers] = useState<Record<number, string[]>>({});
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [lastResult, setLastResult] = useState<QuizAttemptResult | null>(null);

  // Load quiz info & history
  const loadQuizData = async () => {
    setIsLoading(true);
    try {
      const [info, hist] = await Promise.all([
        learnerQuizApi.getQuizForTaking(courseId, lesson.id),
        learnerQuizApi.getAttemptHistory(courseId, lesson.id)
      ]);
      setQuizInfo(info);
      setHistory(hist);
    } catch (err: any) {
      showError(err.message || 'Không thể tải thông tin bài kiểm tra.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadQuizData();
  }, [courseId, lesson.id]);

  const handleStartQuiz = () => {
    if (!quizInfo || quizInfo.questions.length === 0) {
      showError('Bài kiểm tra hiện chưa có câu hỏi nào.');
      return;
    }
    if (
      quizInfo.attemptsRemaining !== null &&
      quizInfo.attemptsRemaining !== undefined &&
      quizInfo.attemptsRemaining <= 0
    ) {
      showError('Bạn đã sử dụng hết số lần làm bài trong ngày hôm nay.');
      return;
    }
    setUserAnswers({});
    setMode('TAKING');
  };

  const handleOptionToggle = (
    question: QuestionTakingItem,
    optionId: string
  ) => {
    setUserAnswers((prev) => {
      const currentSelected = prev[question.id] || [];
      if (question.questionType === 'SINGLE_CHOICE') {
        return { ...prev, [question.id]: [optionId] };
      } else {
        // MULTIPLE_CHOICE
        if (currentSelected.includes(optionId)) {
          return {
            ...prev,
            [question.id]: currentSelected.filter((id) => id !== optionId)
          };
        } else {
          return {
            ...prev,
            [question.id]: [...currentSelected, optionId]
          };
        }
      }
    });
  };

  const answeredCount = quizInfo?.questions.filter(
    (q) => userAnswers[q.id] && userAnswers[q.id].length > 0
  ).length || 0;

  const handleSubmit = async () => {
    if (!quizInfo) return;

    if (answeredCount < quizInfo.questions.length) {
      const confirmSubmit = window.confirm(
        `Bạn mới trả lời ${answeredCount}/${quizInfo.questions.length} câu hỏi. Bạn có chắc chắn muốn nộp bài không?`
      );
      if (!confirmSubmit) return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        answers: Object.entries(userAnswers).map(([qId, opts]) => ({
          questionId: Number(qId),
          selectedOptionIds: opts
        }))
      };

      const result = await learnerQuizApi.submitAttempt(
        courseId,
        lesson.id,
        payload
      );
      setLastResult(result);
      setMode('RESULT');

      if (result.isPassed) {
        showSuccess(`Chúc mừng! Bạn đã đạt ${result.score}% và hoàn thành bài kiểm tra!`);
        onLessonCompleted?.();
      } else {
        showError(`Bạn đạt ${result.score}%. Chưa đạt điểm chuẩn (${result.passingScore}%).`);
      }

      // Refresh quiz data in background
      learnerQuizApi.getQuizForTaking(courseId, lesson.id).then(setQuizInfo);
      learnerQuizApi.getAttemptHistory(courseId, lesson.id).then(setHistory);
    } catch (err: any) {
      showError(err.message || 'Lỗi khi nộp bài thi.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="w-full min-h-[500px] flex flex-col items-center justify-center p-8 text-slate-300">
        <span className="inline-block animate-spin border-4 border-indigo-500 border-t-transparent w-10 h-10 rounded-full mb-4" />
        <p className="text-xs font-bold uppercase tracking-wider text-slate-400">
          Đang chuẩn bị đề thi bài kiểm tra...
        </p>
      </div>
    );
  }

  if (!quizInfo) {
    return (
      <div className="w-full min-h-[400px] flex flex-col items-center justify-center p-8 text-center text-slate-400">
        <span className="material-symbols-outlined text-[48px] text-rose-400 mb-2">error</span>
        <p className="text-sm font-bold text-white mb-2">Không tìm thấy thông tin bài kiểm tra</p>
        <button
          onClick={loadQuizData}
          className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-xl text-xs font-bold transition-all"
        >
          Thử tải lại
        </button>
      </div>
    );
  }

  // =========================================================================
  // VIEW MODE: INTRO / OVERVIEW
  // =========================================================================
  if (mode === 'INTRO') {
    const isOutOfAttempts =
      quizInfo.maxAttempts !== null &&
      quizInfo.maxAttempts !== undefined &&
      quizInfo.attemptsRemaining !== null &&
      quizInfo.attemptsRemaining !== undefined &&
      quizInfo.attemptsRemaining <= 0;

    return (
      <div className="w-full bg-slate-950 p-4 sm:p-8 border-b border-slate-800 shrink-0">
        <div className="max-w-4xl mx-auto space-y-6">
          {/* Header Card */}
          <div className="bg-gradient-to-br from-slate-900 via-indigo-950/40 to-slate-900 p-6 sm:p-8 rounded-3xl border border-indigo-900/40 shadow-xl relative overflow-hidden">
            <div className="absolute top-0 right-0 w-80 h-80 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />

            <div className="flex flex-wrap items-center justify-between gap-4 mb-4">
              <div className="flex items-center gap-2">
                <span className="bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 text-[11px] font-extrabold px-3 py-1 rounded-lg uppercase tracking-wider">
                  Bài Kiểm Tra Trắc Nghiệm
                </span>
                {quizInfo.hasPassed && (
                  <span className="bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[11px] font-extrabold px-3 py-1 rounded-lg uppercase tracking-wider flex items-center gap-1">
                    <span className="material-symbols-outlined text-[14px]">verified</span>
                    Đã Đạt Yêu Cầu
                  </span>
                )}
              </div>

              {quizInfo.highestScore !== null && quizInfo.highestScore !== undefined && (
                <div className="text-right">
                  <span className="text-[10px] text-slate-400 uppercase font-bold tracking-wider block">
                    Điểm cao nhất từng đạt
                  </span>
                  <span className="text-xl sm:text-2xl font-black text-emerald-400 font-mono">
                    {quizInfo.highestScore}%
                  </span>
                </div>
              )}
            </div>

            <h1 className="text-2xl sm:text-3xl font-extrabold text-white mb-3 font-display">
              {quizInfo.title}
            </h1>
            <p className="text-xs sm:text-sm text-slate-300 max-w-2xl leading-relaxed">
              Hãy kiểm tra mức độ nắm bắt kiến thức của bạn. Trả lời đúng các câu hỏi để đạt điểm sàn và đánh dấu hoàn thành bài học này.
            </p>

            {/* Quick Stats Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-6 pt-6 border-t border-slate-800/80">
              <div className="bg-slate-900/80 p-3.5 rounded-2xl border border-slate-800">
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block mb-1">
                  Điểm Chuẩn Đạt
                </span>
                <span className="text-base font-extrabold text-indigo-400 font-mono">
                  ≥ {quizInfo.passingScore}%
                </span>
              </div>

              <div className="bg-slate-900/80 p-3.5 rounded-2xl border border-slate-800">
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block mb-1">
                  Số Lượng Câu Hỏi
                </span>
                <span className="text-base font-extrabold text-white font-mono">
                  {quizInfo.questions.length} câu
                </span>
              </div>

              <div className="bg-slate-900/80 p-3.5 rounded-2xl border border-slate-800">
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block mb-1">
                  Lượt Đã Làm Hôm Nay
                </span>
                <span className="text-base font-extrabold text-white font-mono">
                  {quizInfo.attemptsUsed} lượt
                </span>
              </div>

              <div className="bg-slate-900/80 p-3.5 rounded-2xl border border-slate-800">
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block mb-1">
                  Lượt Còn Lại Hôm Nay
                </span>
                <span
                  className={`text-base font-extrabold font-mono ${
                    isOutOfAttempts ? 'text-rose-400' : 'text-emerald-400'
                  }`}
                >
                  {quizInfo.attemptsRemaining !== null
                    ? `${quizInfo.attemptsRemaining} lượt`
                    : 'Không giới hạn'}
                </span>
              </div>
            </div>

            {/* Action Bar */}
            <div className="mt-8 flex flex-col sm:flex-row items-stretch sm:items-center gap-4">
              <button
                onClick={handleStartQuiz}
                disabled={isOutOfAttempts || quizInfo.questions.length === 0}
                className={`px-8 py-3.5 rounded-2xl text-sm font-extrabold transition-all flex items-center justify-center gap-2 cursor-pointer shadow-lg active:scale-95 ${
                  isOutOfAttempts || quizInfo.questions.length === 0
                    ? 'bg-slate-800 text-slate-500 cursor-not-allowed'
                    : 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-indigo-950/50 hover:shadow-indigo-600/30'
                }`}
              >
                <span className="material-symbols-outlined text-[20px]">
                  {quizInfo.hasPassed ? 'replay' : 'play_arrow'}
                </span>
                <span>
                  {quizInfo.questions.length === 0
                    ? 'Chưa có câu hỏi'
                    : isOutOfAttempts
                    ? 'Hết lượt hôm nay'
                    : quizInfo.hasPassed
                    ? 'Làm lại bài thi (Cải thiện điểm)'
                    : quizInfo.attemptsUsed > 0
                    ? 'Thử lại bài thi'
                    : 'Bắt đầu làm bài thi'}
                </span>
              </button>

              {isOutOfAttempts && (
                <p className="text-xs text-amber-400 font-medium m-0 flex items-center gap-1.5">
                  <span className="material-symbols-outlined text-[16px]">info</span>
                  Bạn đã dùng hết số lượt làm bài hôm nay. Hệ thống sẽ khôi phục lượt làm vào 00:00 ngày mai!
                </p>
              )}
            </div>
          </div>

          {/* History Section */}
          {history.length > 0 && (
            <div className="bg-slate-900/60 p-6 rounded-3xl border border-slate-800 space-y-4">
              <h3 className="text-sm font-extrabold text-white uppercase tracking-wider flex items-center gap-2 m-0">
                <span className="material-symbols-outlined text-indigo-400 text-[18px]">history</span>
                Lịch sử các lần thi ({history.length} lần)
              </h3>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-slate-800 text-slate-400 font-bold uppercase tracking-wider text-[10px]">
                      <th className="pb-3 px-3">Lần thi</th>
                      <th className="pb-3 px-3">Thời gian nộp</th>
                      <th className="pb-3 px-3">Điểm số</th>
                      <th className="pb-3 px-3">Kết quả</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {history.map((attempt, index) => (
                      <tr key={attempt.id} className="hover:bg-slate-800/30 transition-colors">
                        <td className="py-3 px-3 font-mono text-slate-300 font-bold">
                          #{history.length - index}
                        </td>
                        <td className="py-3 px-3 text-slate-400">
                          {new Date(attempt.submittedAt).toLocaleString('vi-VN')}
                        </td>
                        <td className="py-3 px-3 font-bold font-mono text-white">
                          <span
                            className={
                              attempt.isPassed ? 'text-emerald-400' : 'text-slate-300'
                            }
                          >
                            {attempt.score}%
                          </span>
                        </td>
                        <td className="py-3 px-3">
                          <span
                            className={`px-2.5 py-0.5 rounded-md text-[10px] font-bold inline-flex items-center gap-1 ${
                              attempt.isPassed
                                ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                                : 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                            }`}
                          >
                            <span className="material-symbols-outlined text-[12px]">
                              {attempt.isPassed ? 'check' : 'close'}
                            </span>
                            {attempt.isPassed ? 'ĐẠT' : 'CHƯA ĐẠT'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>
    );
  }

  // =========================================================================
  // VIEW MODE: TAKING (EXAMINATION)
  // =========================================================================
  if (mode === 'TAKING') {
    return (
      <div className="w-full bg-slate-950 p-4 sm:p-8 border-b border-slate-800 shrink-0">
        <div className="max-w-4xl mx-auto space-y-6">
          {/* Taking Top Bar */}
          <div className="bg-slate-900/90 backdrop-blur-md p-4 sm:p-5 rounded-2xl border border-slate-800 flex flex-wrap items-center justify-between gap-4 sticky top-4 z-30 shadow-xl">
            <div>
              <span className="text-[10px] font-extrabold text-indigo-400 uppercase tracking-wider block">
                Đang Làm Bài Thi
              </span>
              <h2 className="text-base sm:text-lg font-bold text-white m-0">
                {quizInfo.title}
              </h2>
            </div>

            <div className="flex items-center gap-3">
              <div className="text-right">
                <span className="text-[10px] text-slate-400 block font-semibold">
                  Tiến độ trả lời
                </span>
                <span className="text-xs sm:text-sm font-extrabold text-white font-mono">
                  {answeredCount} / {quizInfo.questions.length} câu
                </span>
              </div>

              <button
                onClick={handleSubmit}
                disabled={isSubmitting}
                className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-extrabold transition-all shadow-lg shadow-emerald-950/30 flex items-center gap-1.5 cursor-pointer active:scale-95 disabled:opacity-50"
              >
                {isSubmitting ? (
                  <>
                    <span className="inline-block animate-spin border-2 border-white border-t-transparent w-3.5 h-3.5 rounded-full" />
                    <span>Đang nộp...</span>
                  </>
                ) : (
                  <>
                    <span className="material-symbols-outlined text-[16px]">send</span>
                    <span>Nộp bài</span>
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Questions List */}
          <div className="space-y-6">
            {quizInfo.questions.map((question, qIdx) => {
              const selectedOpts = userAnswers[question.id] || [];

              return (
                <div
                  key={question.id}
                  className="bg-slate-900/80 p-5 sm:p-7 rounded-3xl border border-slate-800 space-y-4 shadow-sm"
                >
                  {/* Question Header */}
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <span className="bg-indigo-600/30 text-indigo-300 border border-indigo-500/40 font-mono text-xs font-extrabold px-3 py-1 rounded-lg">
                        Câu {qIdx + 1}
                      </span>
                      <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                        {question.questionType === 'SINGLE_CHOICE'
                          ? 'Chọn 1 đáp án'
                          : 'Có thể chọn nhiều đáp án'}
                      </span>
                    </div>

                    <span className="text-[11px] font-bold text-slate-400 font-mono">
                      {question.points} điểm
                    </span>
                  </div>

                  {/* Question Text */}
                  <p className="text-sm sm:text-base font-semibold text-white leading-relaxed m-0 whitespace-pre-line">
                    {question.questionText}
                  </p>

                  {/* Options List */}
                  <div className="space-y-2.5 pt-2">
                    {question.options.map((opt, optIdx) => {
                      const isSelected = selectedOpts.includes(opt.id);
                      const optionLetter = String.fromCharCode(65 + optIdx);

                      return (
                        <div
                          key={opt.id}
                          onClick={() => handleOptionToggle(question, opt.id)}
                          className={`p-3.5 sm:p-4 rounded-2xl border transition-all cursor-pointer flex items-start gap-3 select-none ${
                            isSelected
                              ? 'bg-indigo-600/15 border-indigo-500/80 shadow-sm text-white'
                              : 'bg-slate-950/60 border-slate-800 hover:border-slate-700 hover:bg-slate-900/60 text-slate-300'
                          }`}
                        >
                          {/* Radio / Checkbox Indicator */}
                          <div
                            className={`w-6 h-6 shrink-0 mt-0.5 flex items-center justify-center font-mono text-xs font-extrabold transition-colors ${
                              question.questionType === 'SINGLE_CHOICE'
                                ? 'rounded-full'
                                : 'rounded-lg'
                            } ${
                              isSelected
                                ? 'bg-indigo-600 text-white shadow-sm'
                                : 'bg-slate-800 text-slate-400 border border-slate-700'
                            }`}
                          >
                            {isSelected ? (
                              <span className="material-symbols-outlined text-[14px]">
                                check
                              </span>
                            ) : (
                              optionLetter
                            )}
                          </div>

                          <span className="text-xs sm:text-sm leading-relaxed flex-1">
                            {opt.text}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Bottom Submit Action */}
          <div className="bg-slate-900/90 p-5 rounded-2xl border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
            <span className="text-xs text-slate-400 font-medium">
              Bạn đã hoàn thành{' '}
              <strong className="text-white font-mono">{answeredCount}</strong> /{' '}
              <strong className="text-white font-mono">{quizInfo.questions.length}</strong> câu
            </span>

            <div className="flex items-center gap-3">
              <button
                onClick={() => setMode('INTRO')}
                className="px-4 py-2.5 text-slate-400 hover:text-white text-xs font-bold transition-colors cursor-pointer"
              >
                Hủy bài thi
              </button>

              <button
                onClick={handleSubmit}
                disabled={isSubmitting}
                className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-extrabold transition-all shadow-lg flex items-center gap-2 cursor-pointer active:scale-95 disabled:opacity-50"
              >
                {isSubmitting ? 'Đang nộp...' : 'Nộp bài thi & Xem điểm'}
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // =========================================================================
  // VIEW MODE: RESULT (SCORE & REVIEW)
  // =========================================================================
  if (mode === 'RESULT' && lastResult) {
    const isPassed = lastResult.isPassed;

    return (
      <div className="w-full bg-slate-950 p-4 sm:p-8 border-b border-slate-800 shrink-0">
        <div className="max-w-4xl mx-auto space-y-6">
          {/* Result Hero Banner */}
          <div
            className={`p-6 sm:p-8 rounded-3xl border shadow-2xl relative overflow-hidden ${
              isPassed
                ? 'bg-gradient-to-br from-emerald-950/40 via-slate-900 to-slate-900 border-emerald-600/40'
                : 'bg-gradient-to-br from-amber-950/40 via-slate-900 to-slate-900 border-amber-600/40'
            }`}
          >
            <div className="flex flex-col sm:flex-row items-center justify-between gap-6 text-center sm:text-left">
              <div>
                <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-extrabold uppercase tracking-wider mb-2 bg-black/40">
                  <span
                    className={`material-symbols-outlined text-[16px] ${
                      isPassed ? 'text-emerald-400' : 'text-amber-400'
                    }`}
                  >
                    {isPassed ? 'check_circle' : 'cancel'}
                  </span>
                  <span className={isPassed ? 'text-emerald-400' : 'text-amber-400'}>
                    {isPassed ? 'ĐÃ ĐẠT YÊU CẦU' : 'CHƯA ĐẠT ĐIỂM CHUẨN'}
                  </span>
                </div>

                <h1 className="text-2xl sm:text-3xl font-black text-white m-0 font-display">
                  {isPassed
                    ? 'Xuất sắc! Bạn đã vượt qua bài kiểm tra'
                    : 'Cố gắng lên! Hãy ôn tập và làm lại nhé'}
                </h1>
                <p className="text-xs sm:text-sm text-slate-300 mt-2 m-0 max-w-lg">
                  {isPassed
                    ? 'Tiến độ học tập của bài học này đã được ghi nhận hoàn thành. Bạn có thể làm lại để cải thiện điểm hoặc tiếp tục bài tiếp theo.'
                    : `Bạn cần đạt tối thiểu ${lastResult.passingScore}% để hoàn thành bài học. Hãy xem lại giải thích chi tiết bên dưới.`}
                </p>
              </div>

              {/* Score Display Ring / Badge */}
              <div className="flex flex-col items-center justify-center p-6 bg-slate-950/60 rounded-3xl border border-slate-800 min-w-[160px]">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                  Điểm số đạt được
                </span>
                <span
                  className={`text-4xl sm:text-5xl font-black font-mono my-1 ${
                    isPassed ? 'text-emerald-400' : 'text-amber-400'
                  }`}
                >
                  {lastResult.score}%
                </span>
                <span className="text-xs font-bold text-slate-400">
                  Đúng {lastResult.correctQuestions} / {lastResult.totalQuestions} câu
                </span>
              </div>
            </div>

            {/* Actions */}
            <div className="mt-8 pt-6 border-t border-slate-800 flex flex-wrap items-center gap-3 justify-center sm:justify-start">
              <button
                onClick={() => setMode('INTRO')}
                className="px-6 py-3 bg-slate-800 hover:bg-slate-700 text-white rounded-xl text-xs font-extrabold transition-all cursor-pointer flex items-center gap-2"
              >
                <span className="material-symbols-outlined text-[18px]">list</span>
                <span>Về trang tổng quan & Lịch sử</span>
              </button>

              <button
                onClick={handleStartQuiz}
                className="px-6 py-3 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-extrabold transition-all cursor-pointer flex items-center gap-2 shadow-lg shadow-indigo-950/40"
              >
                <span className="material-symbols-outlined text-[18px]">replay</span>
                <span>Làm lại bài thi ngay</span>
              </button>
            </div>
          </div>

          {/* Question Review Section */}
          <div className="space-y-4">
            <h3 className="text-base font-extrabold text-white uppercase tracking-wider flex items-center gap-2 m-0">
              <span className="material-symbols-outlined text-indigo-400 text-[20px]">
                fact_check
              </span>
              Chi tiết bài nộp & Giải thích đáp án
            </h3>

            <div className="space-y-6">
              {quizInfo.questions.map((question, qIdx) => {
                const qResult = lastResult.questionResults?.find(
                  (r) => r.questionId === question.id
                );
                const isCorrect = qResult?.isCorrect ?? false;

                return (
                  <div
                    key={question.id}
                    className={`p-6 rounded-3xl border space-y-4 shadow-sm ${
                      isCorrect
                        ? 'bg-slate-900/60 border-emerald-900/40'
                        : 'bg-slate-900/60 border-rose-900/40'
                    }`}
                  >
                    {/* Review Question Header */}
                    <div className="flex items-center justify-between gap-3">
                      <div className="flex items-center gap-2">
                        <span
                          className={`font-mono text-xs font-extrabold px-3 py-1 rounded-lg ${
                            isCorrect
                              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                              : 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                          }`}
                        >
                          Câu {qIdx + 1}
                        </span>
                        <span
                          className={`text-xs font-bold flex items-center gap-1 ${
                            isCorrect ? 'text-emerald-400' : 'text-rose-400'
                          }`}
                        >
                          <span className="material-symbols-outlined text-[16px]">
                            {isCorrect ? 'check_circle' : 'cancel'}
                          </span>
                          {isCorrect ? 'Chính xác' : 'Chưa đúng'}
                        </span>
                      </div>

                      <span className="text-xs font-bold font-mono text-slate-400">
                        {qResult?.earnedPoints ?? 0} / {question.points} điểm
                      </span>
                    </div>

                    {/* Question Text */}
                    <p className="text-sm sm:text-base font-semibold text-white leading-relaxed m-0 whitespace-pre-line">
                      {question.questionText}
                    </p>

                    {/* Options Review */}
                    <div className="space-y-2 pt-2">
                      {question.options.map((opt, optIdx) => {
                        const optionLetter = String.fromCharCode(65 + optIdx);
                        const isStudentChoice =
                          qResult?.selectedOptionIds?.includes(opt.id);
                        const isActualCorrect =
                          qResult?.correctOptionIds?.includes(opt.id);

                        let cardStyle = 'bg-slate-950/40 border-slate-800 text-slate-400';
                        if (isActualCorrect) {
                          cardStyle = 'bg-emerald-950/30 border-emerald-600/70 text-emerald-200';
                        } else if (isStudentChoice && !isActualCorrect) {
                          cardStyle = 'bg-rose-950/30 border-rose-600/70 text-rose-200';
                        }

                        return (
                          <div
                            key={opt.id}
                            className={`p-3.5 rounded-2xl border flex items-start justify-between gap-3 text-xs sm:text-sm ${cardStyle}`}
                          >
                            <div className="flex items-start gap-3">
                              <span className="w-5 h-5 rounded-md bg-black/40 flex items-center justify-center font-mono text-xs font-bold shrink-0 mt-0.5">
                                {optionLetter}
                              </span>
                              <span>{opt.text}</span>
                            </div>

                            <div className="flex items-center gap-2 shrink-0">
                              {isStudentChoice && (
                                <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                                  Bạn chọn
                                </span>
                              )}
                              {isActualCorrect && (
                                <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 flex items-center gap-1">
                                  <span className="material-symbols-outlined text-[12px]">check</span>
                                  Đáp án đúng
                                </span>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>

                    {/* Explanation Box */}
                    {qResult?.explanation && (
                      <div className="p-4 bg-indigo-950/20 border border-indigo-900/30 rounded-2xl text-xs space-y-1">
                        <span className="text-indigo-400 font-bold uppercase tracking-wider flex items-center gap-1 text-[10px]">
                          <span className="material-symbols-outlined text-[14px]">lightbulb</span>
                          Giải thích đáp án:
                        </span>
                        <p className="text-slate-300 m-0 leading-relaxed whitespace-pre-line">
                          {qResult.explanation}
                        </p>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    );
  }

  return null;
=======
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
>>>>>>> 6f14ee806c34ad2a8955f6783458598d6afa214a
};
