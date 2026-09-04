import React, { useState, useEffect } from 'react';
import type { Lesson } from '../../../types/course';
import type {
  QuizDetail,
  QuizQuestion,
  QuizOption,
  QuestionType,
  UpsertQuestionRequest
} from '../../../types/quiz';
import { quizApi } from '../api/quizApi';
import { useToast } from '../../../app/context/ToastContext';

interface QuizAuthoringViewProps {
  courseId: number;
  lesson: Lesson;
  onContentUpdated: () => void;
}

export const QuizAuthoringView: React.FC<QuizAuthoringViewProps> = ({
  courseId,
  lesson,
  onContentUpdated
}) => {
  const { showSuccess, showError } = useToast();

  const [quiz, setQuiz] = useState<QuizDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Settings State
  const [isEditingSettings, setIsEditingSettings] = useState(false);
  const [quizTitle, setQuizTitle] = useState(lesson.title || '');
  const [passingScore, setPassingScore] = useState<number>(80);
  const [unlimitedAttempts, setUnlimitedAttempts] = useState(false);
  const [maxAttempts, setMaxAttempts] = useState<number>(3);
  const [isSavingSettings, setIsSavingSettings] = useState(false);

  // Question Form State (Add / Edit)
  const [isQuestionFormOpen, setIsQuestionFormOpen] = useState(false);
  const [editingQuestionId, setEditingQuestionId] = useState<number | null>(null);
  const [questionText, setQuestionText] = useState('');
  const [questionType, setQuestionType] = useState<QuestionType>('SINGLE_CHOICE');
  const [points, setPoints] = useState<number>(1.0);
  const [options, setOptions] = useState<QuizOption[]>([
    { text: '', isCorrect: true, explanation: '' },
    { text: '', isCorrect: false, explanation: '' },
    { text: '', isCorrect: false, explanation: '' },
    { text: '', isCorrect: false, explanation: '' }
  ]);
  const [isSavingQuestion, setIsSavingQuestion] = useState(false);

  // Delete State
  const [deletingQuestionId, setDeletingQuestionId] = useState<number | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Reorder State
  const [isReordering, setIsReordering] = useState(false);

  // ---------------------------------------------------------------------------
  // FETCH QUIZ DETAIL
  // ---------------------------------------------------------------------------
  const fetchQuiz = async () => {
    setIsLoading(true);
    try {
      const data = await quizApi.getQuiz(courseId, lesson.id);
      setQuiz(data);
      setQuizTitle(data.title || lesson.title);
      setPassingScore(data.passingScore ?? 80);
      if (data.maxAttempts === null || data.maxAttempts === undefined) {
        setUnlimitedAttempts(true);
        setMaxAttempts(3);
      } else {
        setUnlimitedAttempts(false);
        setMaxAttempts(data.maxAttempts);
      }
    } catch (err: any) {
      // If 404 (Quiz not yet created), initialize default state
      if (err.status === 404) {
        setQuizTitle(lesson.title);
        setPassingScore(80);
        setMaxAttempts(3);
        setUnlimitedAttempts(false);
      } else {
        showError(err.message || 'Lỗi khi tải thông tin bài kiểm tra.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchQuiz();
  }, [courseId, lesson.id]);

  // ---------------------------------------------------------------------------
  // SAVE GENERAL SETTINGS
  // ---------------------------------------------------------------------------
  const handleSaveSettings = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!quizTitle.trim()) {
      showError('Tiêu đề bài kiểm tra không được để trống.');
      return;
    }
    if (passingScore < 0 || passingScore > 100) {
      showError('Điểm đạt phải trong khoảng từ 0% đến 100%.');
      return;
    }
    if (!unlimitedAttempts && (maxAttempts < 1 || !maxAttempts)) {
      showError('Số lần làm bài tối đa phải ít nhất là 1.');
      return;
    }

    setIsSavingSettings(true);
    try {
      const updated = await quizApi.upsertQuiz(courseId, lesson.id, {
        title: quizTitle.trim(),
        passingScore: Number(passingScore),
        maxAttempts: unlimitedAttempts ? null : Number(maxAttempts)
      });
      setQuiz(updated);
      setIsEditingSettings(false);
      showSuccess('Cập nhật cấu hình bài kiểm tra thành công!');
      onContentUpdated();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi lưu cấu hình bài kiểm tra.');
    } finally {
      setIsSavingSettings(false);
    }
  };

  // ---------------------------------------------------------------------------
  // OPEN QUESTION FORM (ADD / EDIT)
  // ---------------------------------------------------------------------------
  const openAddQuestion = () => {
    setEditingQuestionId(null);
    setQuestionText('');
    setQuestionType('SINGLE_CHOICE');
    setPoints(1.0);
    setOptions([
      { text: '', isCorrect: true, explanation: '' },
      { text: '', isCorrect: false, explanation: '' },
      { text: '', isCorrect: false, explanation: '' },
      { text: '', isCorrect: false, explanation: '' }
    ]);
    setIsQuestionFormOpen(true);
  };

  const openEditQuestion = (q: QuizQuestion) => {
    setEditingQuestionId(q.id);
    setQuestionText(q.questionText);
    setQuestionType(q.questionType);
    setPoints(q.points);
    setOptions(
      q.options && q.options.length > 0
        ? q.options.map((opt) => ({
            id: opt.id,
            text: opt.text,
            isCorrect: Boolean(opt.isCorrect),
            explanation: opt.explanation || ''
          }))
        : [
            { text: '', isCorrect: true, explanation: '' },
            { text: '', isCorrect: false, explanation: '' }
          ]
    );
    setIsQuestionFormOpen(true);
  };

  // ---------------------------------------------------------------------------
  // SAVE QUESTION (ADD OR EDIT)
  // ---------------------------------------------------------------------------
  const handleSaveQuestion = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!questionText.trim()) {
      showError('Vui lòng nhập nội dung câu hỏi.');
      return;
    }

    if (points <= 0) {
      showError('Điểm câu hỏi phải lớn hơn 0.');
      return;
    }

    // Filter out completely blank options or validate
    const filledOptions = options.map((opt) => ({
      ...opt,
      text: opt.text.trim()
    }));

    if (filledOptions.some((opt) => !opt.text)) {
      showError('Tất cả các phương án trả lời phải có nội dung chữ.');
      return;
    }

    if (filledOptions.length < 2) {
      showError('Câu hỏi phải có ít nhất 2 phương án lựa chọn.');
      return;
    }

    const correctCount = filledOptions.filter((opt) => opt.isCorrect).length;
    if (correctCount === 0) {
      showError('Vui lòng đánh dấu ít nhất 1 đáp án đúng.');
      return;
    }

    if (questionType === 'SINGLE_CHOICE' && correctCount > 1) {
      showError('Câu hỏi trắc nghiệm một đáp án (Single Choice) chỉ được chọn đúng 1 phương án.');
      return;
    }

    const payload: UpsertQuestionRequest = {
      questionText: questionText.trim(),
      questionType,
      points: Number(points),
      options: filledOptions
    };

    setIsSavingQuestion(true);
    try {
      if (editingQuestionId) {
        await quizApi.updateQuestion(courseId, lesson.id, editingQuestionId, payload);
        showSuccess('Đã cập nhật câu hỏi thành công!');
      } else {
        await quizApi.addQuestion(courseId, lesson.id, payload);
        showSuccess('Đã thêm câu hỏi mới thành công!');
      }
      setIsQuestionFormOpen(false);
      await fetchQuiz();
      onContentUpdated();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi lưu câu hỏi.');
    } finally {
      setIsSavingQuestion(false);
    }
  };

  // ---------------------------------------------------------------------------
  // DELETE QUESTION
  // ---------------------------------------------------------------------------
  const handleDeleteQuestion = async (qId: number) => {
    setIsDeleting(true);
    try {
      await quizApi.deleteQuestion(courseId, lesson.id, qId);
      showSuccess('Đã xóa câu hỏi thành công!');
      setDeletingQuestionId(null);
      await fetchQuiz();
      onContentUpdated();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi xóa câu hỏi.');
    } finally {
      setIsDeleting(false);
    }
  };

  // ---------------------------------------------------------------------------
  // REORDER QUESTIONS
  // ---------------------------------------------------------------------------
  const handleMoveQuestion = async (index: number, direction: 'UP' | 'DOWN') => {
    if (!quiz || isReordering) return;
    const questions = [...quiz.questions];
    const targetIndex = direction === 'UP' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= questions.length) return;

    // Swap
    const temp = questions[index];
    questions[index] = questions[targetIndex];
    questions[targetIndex] = temp;

    // Optimistic UI update
    setQuiz({ ...quiz, questions });

    setIsReordering(true);
    try {
      const questionIds = questions.map((q) => q.id);
      await quizApi.reorderQuestions(courseId, lesson.id, questionIds);
      showSuccess('Đã thay đổi thứ tự câu hỏi!');
    } catch (err: any) {
      showError(err.message || 'Lỗi khi sắp xếp câu hỏi.');
      await fetchQuiz();
    } finally {
      setIsReordering(false);
    }
  };

  // Helper option handlers
  const handleOptionTextChange = (idx: number, text: string) => {
    const updated = [...options];
    updated[idx].text = text;
    setOptions(updated);
  };

  const handleOptionExplanationChange = (idx: number, explanation: string) => {
    const updated = [...options];
    updated[idx].explanation = explanation;
    setOptions(updated);
  };

  const handleOptionCorrectToggle = (idx: number) => {
    const updated = [...options];
    if (questionType === 'SINGLE_CHOICE') {
      // Only 1 option is correct
      updated.forEach((opt, i) => {
        opt.isCorrect = i === idx;
      });
    } else {
      // Toggle
      updated[idx].isCorrect = !updated[idx].isCorrect;
    }
    setOptions(updated);
  };

  const handleAddOption = () => {
    if (options.length >= 6) {
      showError('Tối đa 6 phương án trả lời cho mỗi câu hỏi.');
      return;
    }
    setOptions([...options, { text: '', isCorrect: false, explanation: '' }]);
  };

  const handleRemoveOption = (idx: number) => {
    if (options.length <= 2) {
      showError('Mỗi câu hỏi phải có tối thiểu 2 phương án trả lời.');
      return;
    }
    const updated = options.filter((_, i) => i !== idx);
    // If the removed option was the only correct one, make the first one correct
    if (updated.every((opt) => !opt.isCorrect) && updated.length > 0) {
      updated[0].isCorrect = true;
    }
    setOptions(updated);
  };

  if (isLoading) {
    return (
      <div className="py-16 flex flex-col items-center justify-center gap-3 text-slate-500">
        <span className="inline-block animate-spin w-8 h-8 border-3 border-primary border-t-transparent rounded-full" />
        <span className="text-xs font-semibold">Đang tải dữ liệu bài kiểm tra...</span>
      </div>
    );
  }

  const questions = quiz?.questions || [];
  const totalPoints = quiz?.totalPoints || 0;

  return (
    <div className="space-y-6">
      {/* 1. TOP SUMMARY BANNER */}
      <div className="bg-gradient-to-r from-primary/10 via-primary/5 to-transparent p-5 rounded-2xl border border-primary/20 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[24px]">
              quiz
            </span>
            <h4 className="font-extrabold text-base text-slate-900 m-0">
              {quiz?.title || quizTitle || lesson.title}
            </h4>
          </div>
          <p className="text-xs text-slate-500 m-0">
            Điểm đạt:{' '}
            <span className="font-bold text-slate-700">
              {quiz?.passingScore ?? passingScore}%
            </span>{' '}
            • Số lần làm:{' '}
            <span className="font-bold text-slate-700">
              {unlimitedAttempts || quiz?.maxAttempts == null
                ? 'Không giới hạn'
                : `${quiz?.maxAttempts} lần`}
            </span>
          </p>
        </div>

        {/* Quick Stats Badges */}
        <div className="flex items-center gap-2.5 flex-wrap">
          <div className="px-3 py-1.5 bg-white rounded-xl border border-slate-200 shadow-2xs flex items-center gap-1.5 text-xs">
            <span className="material-symbols-outlined text-indigo-600 text-[16px]">
              help
            </span>
            <span className="font-bold text-slate-800">{questions.length}</span>
            <span className="text-slate-500">câu hỏi</span>
          </div>

          <div className="px-3 py-1.5 bg-white rounded-xl border border-slate-200 shadow-2xs flex items-center gap-1.5 text-xs">
            <span className="material-symbols-outlined text-emerald-600 text-[16px]">
              military_tech
            </span>
            <span className="font-bold text-slate-800">{totalPoints}</span>
            <span className="text-slate-500">tổng điểm</span>
          </div>

          <button
            type="button"
            onClick={() => setIsEditingSettings(!isEditingSettings)}
            className="px-3 py-1.5 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 rounded-xl text-xs font-bold transition-all flex items-center gap-1 cursor-pointer"
            title="Đổi cấu hình bài thi"
          >
            <span className="material-symbols-outlined text-[16px]">settings</span>
            <span>{isEditingSettings ? 'Đóng cấu hình' : 'Cấu hình'}</span>
          </button>
        </div>
      </div>

      {/* 2. GENERAL SETTINGS FORM (COLLAPSIBLE) */}
      {isEditingSettings && (
        <form
          onSubmit={handleSaveSettings}
          className="bg-surface-container-low p-5 rounded-2xl border border-outline-variant/60 space-y-4 animate-in fade-in duration-200"
        >
          <div className="flex items-center justify-between border-b border-slate-200/80 pb-3">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-800 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[18px] text-primary">tune</span>
              Cấu hình chung bài kiểm tra
            </span>
            <span className="text-[11px] text-slate-500">US-07: Soạn thảo bài Quiz</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
            {/* Title */}
            <div className="space-y-1.5 sm:col-span-3">
              <label className="block font-bold text-slate-700">
                Tiêu đề bài kiểm tra <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={quizTitle}
                onChange={(e) => setQuizTitle(e.target.value)}
                placeholder="Nhập tiêu đề bài trắc nghiệm..."
                className="w-full px-3.5 py-2 bg-white border border-slate-200 rounded-xl text-xs text-slate-800 focus:outline-none focus:border-primary"
              />
            </div>

            {/* Passing Score */}
            <div className="space-y-1.5">
              <label className="block font-bold text-slate-700">
                Điểm đạt tối thiểu (%):
              </label>
              <div className="relative">
                <input
                  type="number"
                  min={0}
                  max={100}
                  step={5}
                  value={passingScore}
                  onChange={(e) => setPassingScore(Number(e.target.value))}
                  className="w-full px-3.5 py-2 bg-white border border-slate-200 rounded-xl text-xs text-slate-800 focus:outline-none focus:border-primary pr-8"
                />
                <span className="absolute right-3 top-2 text-slate-400 font-bold">%</span>
              </div>
              <p className="text-[11px] text-slate-500 m-0">Mặc định: 80% để vượt qua bài học</p>
            </div>

            {/* Max Attempts */}
            <div className="space-y-1.5 sm:col-span-2">
              <label className="block font-bold text-slate-700">
                Số lần làm bài tối đa:
              </label>
              <div className="flex items-center gap-3">
                <input
                  type="number"
                  min={1}
                  max={20}
                  disabled={unlimitedAttempts}
                  value={maxAttempts}
                  onChange={(e) => setMaxAttempts(Number(e.target.value))}
                  className="w-28 px-3.5 py-2 bg-white border border-slate-200 rounded-xl text-xs text-slate-800 focus:outline-none focus:border-primary disabled:bg-slate-100 disabled:text-slate-400"
                />
                <label className="flex items-center gap-2 cursor-pointer text-xs font-semibold text-slate-700">
                  <input
                    type="checkbox"
                    checked={unlimitedAttempts}
                    onChange={(e) => setUnlimitedAttempts(e.target.checked)}
                    className="w-4 h-4 text-primary rounded cursor-pointer"
                  />
                  <span>Không giới hạn số lần thi</span>
                </label>
              </div>
              <p className="text-[11px] text-slate-500 m-0">
                {unlimitedAttempts
                  ? 'Học viên có thể làm lại bài không giới hạn lần'
                  : `Học viên chỉ được nộp bài tối đa ${maxAttempts} lần`}
              </p>
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200/80">
            <button
              type="button"
              onClick={() => setIsEditingSettings(false)}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-200/60 transition-colors cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSavingSettings}
              className="bg-primary hover:bg-primary/90 text-white text-xs font-bold px-4 py-2 rounded-xl transition-all shadow-sm flex items-center gap-1.5 cursor-pointer"
            >
              {isSavingSettings && (
                <span className="inline-block animate-spin w-3 h-3 border-2 border-white border-t-transparent rounded-full" />
              )}
              <span className="material-symbols-outlined text-[16px]">save</span>
              <span>Lưu cấu hình</span>
            </button>
          </div>
        </form>
      )}

      {/* 3. QUESTION FORM (ADD / EDIT) */}
      {isQuestionFormOpen && (
        <form
          onSubmit={handleSaveQuestion}
          className="bg-white p-5 rounded-2xl border-2 border-primary/30 shadow-md space-y-4 animate-in fade-in duration-200"
        >
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[20px]">
                {editingQuestionId ? 'edit_square' : 'add_circle'}
              </span>
              <h5 className="font-bold text-sm text-slate-900 m-0">
                {editingQuestionId ? 'Chỉnh sửa câu hỏi' : 'Thêm câu hỏi mới vào ngân hàng đề'}
              </h5>
            </div>
            <button
              type="button"
              onClick={() => setIsQuestionFormOpen(false)}
              className="text-slate-400 hover:text-slate-600 p-1 cursor-pointer"
            >
              <span className="material-symbols-outlined text-[18px]">close</span>
            </button>
          </div>

          <div className="space-y-4 text-xs">
            {/* Question Text */}
            <div className="space-y-1.5">
              <label className="block font-bold text-slate-800">
                Nội dung câu hỏi <span className="text-rose-500">*</span>
              </label>
              <textarea
                rows={3}
                value={questionText}
                onChange={(e) => setQuestionText(e.target.value)}
                placeholder="Ví dụ: Đâu là cấu hình đúng khi kết nối cơ sở dữ liệu PostgreSQL trong Spring Boot?"
                className="w-full px-3.5 py-2.5 bg-surface-container-low border border-slate-200 rounded-xl text-xs text-slate-800 focus:bg-white focus:outline-none focus:border-primary leading-relaxed"
              />
            </div>

            {/* Type & Points */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="block font-bold text-slate-800">Loại câu hỏi:</label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setQuestionType('SINGLE_CHOICE');
                      // Ensure only 1 is selected
                      const updated = [...options];
                      let found = false;
                      updated.forEach((opt) => {
                        if (opt.isCorrect && !found) {
                          found = true;
                        } else {
                          opt.isCorrect = false;
                        }
                      });
                      if (!found && updated.length > 0) updated[0].isCorrect = true;
                      setOptions(updated);
                    }}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                      questionType === 'SINGLE_CHOICE'
                        ? 'bg-primary/10 border-primary text-primary'
                        : 'bg-surface-container-low border-slate-200 text-slate-600 hover:bg-slate-100'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[16px]">radio_button_checked</span>
                    <span>1 đáp án đúng</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setQuestionType('MULTIPLE_CHOICE')}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                      questionType === 'MULTIPLE_CHOICE'
                        ? 'bg-primary/10 border-primary text-primary'
                        : 'bg-surface-container-low border-slate-200 text-slate-600 hover:bg-slate-100'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[16px]">check_box</span>
                    <span>Nhiều đáp án đúng</span>
                  </button>
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="block font-bold text-slate-800">Điểm số của câu hỏi:</label>
                <div className="relative">
                  <input
                    type="number"
                    min={0.25}
                    max={100}
                    step={0.25}
                    value={points}
                    onChange={(e) => setPoints(Number(e.target.value))}
                    className="w-full px-3.5 py-2 bg-surface-container-low border border-slate-200 rounded-xl text-xs text-slate-800 focus:bg-white focus:outline-none focus:border-primary pr-12"
                  />
                  <span className="absolute right-3 top-2 text-slate-400 font-semibold">điểm</span>
                </div>
              </div>
            </div>

            {/* Options List */}
            <div className="space-y-2.5 pt-2">
              <div className="flex items-center justify-between">
                <label className="block font-bold text-slate-800">
                  Các phương án trả lời ({options.length}/6) <span className="text-rose-500">*</span>
                </label>
                <span className="text-[11px] text-slate-500">
                  Tick chọn phương án đúng ở bên trái
                </span>
              </div>

              <div className="space-y-2.5">
                {options.map((opt, idx) => {
                  const letter = String.fromCharCode(65 + idx); // A, B, C, D...
                  return (
                    <div
                      key={idx}
                      className={`p-3 rounded-xl border transition-all space-y-2 ${
                        opt.isCorrect
                          ? 'bg-emerald-50/60 border-emerald-300'
                          : 'bg-surface-container-low/80 border-slate-200'
                      }`}
                    >
                      <div className="flex items-center gap-2.5">
                        {/* Correct indicator button */}
                        <button
                          type="button"
                          onClick={() => handleOptionCorrectToggle(idx)}
                          className={`w-7 h-7 rounded-lg flex items-center justify-center font-bold text-xs shrink-0 transition-all cursor-pointer ${
                            opt.isCorrect
                              ? 'bg-emerald-600 text-white shadow-xs'
                              : 'bg-white text-slate-500 border border-slate-300 hover:border-emerald-500'
                          }`}
                          title={
                            opt.isCorrect
                              ? 'Đáp án ĐÚNG (Click để đổi)'
                              : 'Đánh dấu là đáp án ĐÚNG'
                          }
                        >
                          {opt.isCorrect ? (
                            <span className="material-symbols-outlined text-[18px]">check</span>
                          ) : (
                            letter
                          )}
                        </button>

                        {/* Option Text */}
                        <input
                          type="text"
                          value={opt.text}
                          onChange={(e) => handleOptionTextChange(idx, e.target.value)}
                          placeholder={`Nội dung phương án ${letter}...`}
                          className="flex-1 px-3 py-1.5 bg-white border border-slate-200 rounded-lg text-xs text-slate-800 focus:outline-none focus:border-primary"
                        />

                        {/* Delete option button */}
                        {options.length > 2 && (
                          <button
                            type="button"
                            onClick={() => handleRemoveOption(idx)}
                            className="text-slate-400 hover:text-rose-600 p-1 cursor-pointer shrink-0"
                            title="Xóa phương án này"
                          >
                            <span className="material-symbols-outlined text-[18px]">delete</span>
                          </button>
                        )}
                      </div>

                      {/* Explanation input (optional) */}
                      <div className="pl-9.5">
                        <input
                          type="text"
                          value={opt.explanation || ''}
                          onChange={(e) =>
                            handleOptionExplanationChange(idx, e.target.value)
                          }
                          placeholder="Giải thích vì sao đúng / sai (tùy chọn)..."
                          className="w-full px-2.5 py-1 bg-white/70 border border-slate-200/70 rounded-md text-[11px] text-slate-600 focus:bg-white focus:outline-none"
                        />
                      </div>
                    </div>
                  );
                })}
              </div>

              {options.length < 6 && (
                <button
                  type="button"
                  onClick={handleAddOption}
                  className="text-xs text-primary hover:text-primary/80 font-bold flex items-center gap-1 pt-1 cursor-pointer"
                >
                  <span className="material-symbols-outlined text-[16px]">add</span>
                  <span>Thêm phương án lựa chọn</span>
                </button>
              )}
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={() => setIsQuestionFormOpen(false)}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isSavingQuestion}
              className="bg-primary hover:bg-primary/90 text-white text-xs font-bold px-5 py-2 rounded-xl transition-all shadow-sm flex items-center gap-1.5 cursor-pointer"
            >
              {isSavingQuestion && (
                <span className="inline-block animate-spin w-3 h-3 border-2 border-white border-t-transparent rounded-full" />
              )}
              <span className="material-symbols-outlined text-[16px]">check</span>
              <span>{editingQuestionId ? 'Cập nhật câu hỏi' : 'Lưu câu hỏi'}</span>
            </button>
          </div>
        </form>
      )}

      {/* 4. QUESTIONS LIST */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h5 className="font-bold text-xs uppercase tracking-wider text-slate-800 m-0 flex items-center gap-2">
            <span>Danh sách câu hỏi trong đề ({questions.length})</span>
          </h5>

          {!isQuestionFormOpen && (
            <button
              type="button"
              onClick={openAddQuestion}
              className="bg-primary hover:bg-primary/90 text-white text-xs font-bold px-3.5 py-1.5 rounded-xl transition-all shadow-xs flex items-center gap-1 cursor-pointer"
            >
              <span className="material-symbols-outlined text-[16px]">add</span>
              <span>Thêm câu hỏi mới</span>
            </button>
          )}
        </div>

        {questions.length === 0 ? (
          <div className="p-8 text-center bg-surface-container-low rounded-2xl border border-dashed border-slate-300 space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center mx-auto">
              <span className="material-symbols-outlined text-[28px]">post_add</span>
            </div>
            <div className="space-y-1">
              <h6 className="font-bold text-sm text-slate-800 m-0">
                Chưa có câu hỏi nào trong bài trắc nghiệm
              </h6>
              <p className="text-xs text-slate-500 m-0 max-w-md mx-auto">
                Hãy thêm các câu hỏi trắc nghiệm kèm phương án và đáp án đúng để học viên có thể thực hành ôn luyện kiến thức.
              </p>
            </div>
            {!isQuestionFormOpen && (
              <button
                type="button"
                onClick={openAddQuestion}
                className="bg-primary hover:bg-primary/90 text-white text-xs font-bold px-4 py-2 rounded-xl transition-all shadow-xs inline-flex items-center gap-1.5 cursor-pointer mt-1"
              >
                <span className="material-symbols-outlined text-[18px]">add</span>
                <span>Tạo câu hỏi đầu tiên</span>
              </button>
            )}
          </div>
        ) : (
          <div className="space-y-3">
            {questions.map((q, idx) => (
              <div
                key={q.id}
                className="bg-white p-4 rounded-2xl border border-slate-200 hover:border-slate-300 transition-all space-y-3 shadow-2xs text-xs"
              >
                {/* Question Header */}
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-2 flex-wrap">
                    {/* Reorder Buttons */}
                    <div className="flex items-center bg-slate-100 rounded-lg p-0.5">
                      <button
                        type="button"
                        disabled={idx === 0 || isReordering}
                        onClick={() => handleMoveQuestion(idx, 'UP')}
                        className="text-slate-500 hover:text-primary disabled:opacity-20 p-0.5 cursor-pointer"
                        title="Di chuyển lên"
                      >
                        <span className="material-symbols-outlined text-[14px]">
                          arrow_upward
                        </span>
                      </button>
                      <button
                        type="button"
                        disabled={idx === questions.length - 1 || isReordering}
                        onClick={() => handleMoveQuestion(idx, 'DOWN')}
                        className="text-slate-500 hover:text-primary disabled:opacity-20 p-0.5 cursor-pointer"
                        title="Di chuyển xuống"
                      >
                        <span className="material-symbols-outlined text-[14px]">
                          arrow_downward
                        </span>
                      </button>
                    </div>

                    <span className="font-extrabold text-slate-900">
                      Câu {idx + 1}:
                    </span>

                    <span className="px-2 py-0.5 bg-indigo-50 text-indigo-700 font-bold text-[10px] rounded-md uppercase">
                      {q.questionType === 'SINGLE_CHOICE'
                        ? '1 đáp án đúng'
                        : 'Nhiều đáp án đúng'}
                    </span>

                    <span className="px-2 py-0.5 bg-emerald-50 text-emerald-700 font-bold text-[10px] rounded-md">
                      {q.points} điểm
                    </span>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-1 shrink-0">
                    <button
                      type="button"
                      onClick={() => openEditQuestion(q)}
                      className="p-1 text-slate-400 hover:text-primary hover:bg-primary/5 rounded-lg transition-colors cursor-pointer"
                      title="Sửa câu hỏi"
                    >
                      <span className="material-symbols-outlined text-[16px]">edit</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => setDeletingQuestionId(q.id)}
                      className="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
                      title="Xóa câu hỏi"
                    >
                      <span className="material-symbols-outlined text-[16px]">delete</span>
                    </button>
                  </div>
                </div>

                {/* Question Text */}
                <p className="font-medium text-slate-800 leading-relaxed m-0 text-xs whitespace-pre-wrap">
                  {q.questionText}
                </p>

                {/* Options Preview */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                  {q.options?.map((opt, optIdx) => {
                    const letter = String.fromCharCode(65 + optIdx);
                    return (
                      <div
                        key={optIdx}
                        className={`p-2.5 rounded-xl border flex items-start gap-2 text-xs ${
                          opt.isCorrect
                            ? 'bg-emerald-50/80 border-emerald-300 text-emerald-950 font-medium'
                            : 'bg-slate-50 border-slate-200/80 text-slate-700'
                        }`}
                      >
                        <span
                          className={`w-5 h-5 rounded-md flex items-center justify-center font-bold text-[10px] shrink-0 ${
                            opt.isCorrect
                              ? 'bg-emerald-600 text-white'
                              : 'bg-white text-slate-500 border border-slate-200'
                          }`}
                        >
                          {opt.isCorrect ? '✓' : letter}
                        </span>
                        <div className="overflow-hidden">
                          <span className="truncate block">{opt.text}</span>
                          {opt.explanation && (
                            <span className="text-[10px] text-slate-500 italic block mt-0.5">
                              {opt.explanation}
                            </span>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 5. DELETE CONFIRMATION MODAL */}
      {deletingQuestionId && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
          <div className="bg-white rounded-2xl p-5 max-w-sm w-full space-y-4 shadow-xl animate-in fade-in zoom-in-95">
            <div className="flex items-center gap-3 text-rose-600">
              <span className="material-symbols-outlined text-[28px]">warning</span>
              <h5 className="font-bold text-base text-slate-900 m-0">Xác nhận xóa câu hỏi</h5>
            </div>
            <p className="text-xs text-slate-600 leading-relaxed m-0">
              Bạn có chắc chắn muốn xóa câu hỏi này khỏi ngân hàng đề không? Thứ tự các câu còn lại sẽ tự động được cập nhật liên tục.
            </p>
            <div className="flex justify-end gap-2 pt-1">
              <button
                type="button"
                disabled={isDeleting}
                onClick={() => setDeletingQuestionId(null)}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 cursor-pointer"
              >
                Hủy
              </button>
              <button
                type="button"
                disabled={isDeleting}
                onClick={() => handleDeleteQuestion(deletingQuestionId)}
                className="bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold px-4 py-2 rounded-xl shadow-xs cursor-pointer flex items-center gap-1.5"
              >
                {isDeleting && (
                  <span className="inline-block animate-spin w-3 h-3 border-2 border-white border-t-transparent rounded-full" />
                )}
                <span>Xác nhận xóa</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
