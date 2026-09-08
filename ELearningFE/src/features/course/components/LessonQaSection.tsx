import React, { useState, useEffect, useMemo } from 'react';
import type { CourseQuestion, CourseAnswer } from '../../../types/qa';
import { qaApi } from '../api/qaApi';
import { useAuth } from '../../../app/context/AuthContext';
import { useToast } from '../../../app/context/ToastContext';

interface LessonQaSectionProps {
  courseId: number;
  lessonId: number;
  lessonTitle: string;
}

export const LessonQaSection: React.FC<LessonQaSectionProps> = ({
  courseId,
  lessonId,
  lessonTitle
}) => {
  const { currentUser, appMode } = useAuth();
  const { showSuccess, showError } = useToast();

  const [questions, setQuestions] = useState<CourseQuestion[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [scope, setScope] = useState<'current_lesson' | 'all_course'>('current_lesson');
  const [searchKeyword, setSearchKeyword] = useState<string>('');

  // Ask Question Form State
  const [isAsking, setIsAsking] = useState<boolean>(false);
  const [newTitle, setNewTitle] = useState<string>('');
  const [newContent, setNewContent] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // Thread Replies State
  const [expandedQuestions, setExpandedQuestions] = useState<Record<number, boolean>>({});
  const [replyInputs, setReplyInputs] = useState<Record<number, string>>({});
  const [submittingReplies, setSubmittingReplies] = useState<Record<number, boolean>>({});

  const loadQuestions = async () => {
    try {
      setIsLoading(true);
      if (scope === 'current_lesson') {
        const data = await qaApi.getLessonQuestions(courseId, lessonId);
        setQuestions(data);
      } else {
        const data = await qaApi.getCourseQuestions(courseId);
        setQuestions(data);
      }
    } catch {
      showError('Không thể tải danh sách hỏi đáp.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadQuestions();
  }, [courseId, lessonId, scope]);

  const filteredQuestions = useMemo(() => {
    if (!searchKeyword.trim()) return questions;
    const kw = searchKeyword.toLowerCase();
    return questions.filter(
      (q) =>
        q.title.toLowerCase().includes(kw) ||
        q.content.toLowerCase().includes(kw) ||
        q.author.fullName.toLowerCase().includes(kw)
    );
  }, [questions, searchKeyword]);

  const handleCreateQuestion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim() || !newContent.trim()) {
      showError('Vui lòng điền đầy đủ tiêu đề và nội dung câu hỏi.');
      return;
    }

    try {
      setIsSubmitting(true);
      const created = await qaApi.createQuestion(
        courseId,
        lessonId,
        lessonTitle,
        {
          title: newTitle.trim(),
          content: newContent.trim()
        },
        currentUser?.fullName || 'Học viên'
      );

      setQuestions((prev) => [created, ...prev]);
      setNewTitle('');
      setNewContent('');
      setIsAsking(false);
      showSuccess('Câu hỏi của bạn đã được gửi thành công!');
    } catch (err: any) {
      if (err?.status === 403 || err?.response?.status === 403) {
        showError('Chỉ học viên đã đăng ký khóa học mới có thể đặt câu hỏi.');
      } else {
        showError(err.message || 'Lỗi khi gửi câu hỏi.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const toggleExpandThread = (questionId: number) => {
    setExpandedQuestions((prev) => ({
      ...prev,
      [questionId]: !prev[questionId]
    }));
  };

  const handleCreateAnswer = async (questionId: number) => {
    const text = replyInputs[questionId]?.trim();
    if (!text) return;

    try {
      setSubmittingReplies((prev) => ({ ...prev, [questionId]: true }));
      const isInstructor = appMode === 'LECTURER';
      const createdAnswer: CourseAnswer = await qaApi.createAnswer(
        courseId,
        questionId,
        { content: text },
        currentUser?.fullName || (isInstructor ? 'Giảng viên' : 'Học viên'),
        isInstructor
      );

      setQuestions((prev) =>
        prev.map((q) => {
          if (q.id === questionId) {
            return {
              ...q,
              answersCount: q.answersCount + 1,
              hasInstructorReply: q.hasInstructorReply || isInstructor,
              answers: [...(q.answers || []), createdAnswer]
            };
          }
          return q;
        })
      );

      setReplyInputs((prev) => ({ ...prev, [questionId]: '' }));
      setExpandedQuestions((prev) => ({ ...prev, [questionId]: true }));
      showSuccess('Đã gửi phản hồi!');
    } catch (err: any) {
      if (err?.status === 403 || err?.response?.status === 403) {
        showError('Bạn không có quyền phản hồi trên khóa học này.');
      } else {
        showError(err.message || 'Lỗi khi gửi phản hồi.');
      }
    } finally {
      setSubmittingReplies((prev) => ({ ...prev, [questionId]: false }));
    }
  };

  const handleDeleteQuestion = async (questionId: number) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa câu hỏi này cùng tất cả câu trả lời?')) {
      return;
    }

    try {
      await qaApi.deleteQuestion(courseId, questionId);
      setQuestions((prev) => prev.filter((q) => q.id !== questionId));
      showSuccess('Đã xóa câu hỏi thành công.');
    } catch (err: any) {
      if (err?.status === 403 || err?.response?.status === 403) {
        showError('Bạn không có quyền xóa câu hỏi này.');
      } else {
        showError(err.message || 'Lỗi khi xóa câu hỏi.');
      }
    }
  };

  const handleDeleteAnswer = async (questionId: number, answerId: number) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa câu trả lời này?')) {
      return;
    }

    try {
      await qaApi.deleteAnswer(courseId, questionId, answerId);
      setQuestions((prev) =>
        prev.map((q) => {
          if (q.id === questionId) {
            const updatedAnswers = (q.answers || []).filter((a) => a.id !== answerId);
            return {
              ...q,
              answersCount: updatedAnswers.length,
              hasInstructorReply: updatedAnswers.some((a) => a.isInstructorReply),
              answers: updatedAnswers
            };
          }
          return q;
        })
      );
      showSuccess('Đã xóa câu trả lời.');
    } catch (err: any) {
      if (err?.status === 403 || err?.response?.status === 403) {
        showError('Bạn không có quyền xóa câu trả lời này.');
      } else {
        showError(err.message || 'Lỗi khi xóa câu trả lời.');
      }
    }
  };

  const formatTimeAgo = (isoDate: string) => {
    try {
      const date = new Date(isoDate);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffMins = Math.floor(diffMs / (1000 * 60));
      const diffHours = Math.floor(diffMins / 60);
      const diffDays = Math.floor(diffHours / 24);

      if (diffMins < 1) return 'Vừa xong';
      if (diffMins < 60) return `${diffMins} phút trước`;
      if (diffHours < 24) return `${diffHours} giờ trước`;
      if (diffDays < 7) return `${diffDays} ngày trước`;
      return date.toLocaleDateString('vi-VN');
    } catch {
      return isoDate;
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Controls: Filter Scope, Search, Ask Button */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900/60 p-4 rounded-2xl border border-slate-800">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setScope('current_lesson')}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer ${
              scope === 'current_lesson'
                ? 'bg-primary text-white shadow-md shadow-primary/20'
                : 'bg-slate-800 text-slate-400 hover:text-white hover:bg-slate-700'
            }`}
          >
            <span className="material-symbols-outlined text-[16px]">topic</span>
            <span>Bài học này</span>
          </button>

          <button
            onClick={() => setScope('all_course')}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer ${
              scope === 'all_course'
                ? 'bg-primary text-white shadow-md shadow-primary/20'
                : 'bg-slate-800 text-slate-400 hover:text-white hover:bg-slate-700'
            }`}
          >
            <span className="material-symbols-outlined text-[16px]">forum</span>
            <span>Toàn bộ khóa học</span>
          </button>
        </div>

        <div className="flex items-center gap-2.5">
          <div className="relative flex-1 sm:w-64">
            <span className="material-symbols-outlined absolute left-3 top-2.5 text-slate-500 text-[18px]">
              search
            </span>
            <input
              type="text"
              placeholder="Tìm câu hỏi..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 bg-slate-950 border border-slate-700/80 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-primary transition-all"
            />
          </div>

          <button
            onClick={() => setIsAsking(!isAsking)}
            className="px-4 py-2 bg-gradient-to-r from-primary to-indigo-600 hover:from-primary/90 hover:to-indigo-500 text-white font-bold text-xs rounded-xl shadow-lg shadow-primary/20 flex items-center gap-1.5 transition-all cursor-pointer whitespace-nowrap active:scale-95"
          >
            <span className="material-symbols-outlined text-[18px]">
              {isAsking ? 'close' : 'add_comment'}
            </span>
            <span>{isAsking ? 'Đóng form' : 'Đặt câu hỏi'}</span>
          </button>
        </div>
      </div>

      {/* Form: Ask New Question */}
      {isAsking && (
        <form
          onSubmit={handleCreateQuestion}
          className="bg-slate-900 border border-primary/40 rounded-2xl p-5 shadow-2xl space-y-4 animate-in fade-in slide-in-from-top-2 duration-200"
        >
          <div className="flex items-center justify-between border-b border-slate-800 pb-3">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[20px]">
                help_outline
              </span>
              <h4 className="text-sm font-bold text-white m-0 font-display">
                Đặt câu hỏi cho bài: <span className="text-primary">{lessonTitle}</span>
              </h4>
            </div>
            <span className="text-[11px] text-slate-400 font-medium">
              Chỉ học viên và giảng viên thấy thread này
            </span>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-300">Tiêu đề câu hỏi</label>
            <input
              type="text"
              required
              placeholder="Ví dụ: Lỗi Uncaught TypeError khi render component ở phút 04:20"
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-primary transition-all"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-300">Chi tiết thắc mắc</label>
            <textarea
              required
              rows={4}
              placeholder="Mô tả cụ thể bạn đang gặp phải vấn đề gì, đã thử những cách nào..."
              value={newContent}
              onChange={(e) => setNewContent(e.target.value)}
              className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-primary transition-all resize-none"
            />
          </div>

          <div className="flex items-center justify-end gap-2.5 pt-2">
            <button
              type="button"
              onClick={() => setIsAsking(false)}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-bold rounded-xl transition-all cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 bg-primary hover:bg-primary/90 disabled:opacity-50 text-white text-xs font-bold rounded-xl shadow-lg shadow-primary/25 transition-all flex items-center gap-1.5 cursor-pointer active:scale-95"
            >
              {isSubmitting ? (
                <span>Đang gửi...</span>
              ) : (
                <>
                  <span className="material-symbols-outlined text-[16px]">send</span>
                  <span>Đăng câu hỏi</span>
                </>
              )}
            </button>
          </div>
        </form>
      )}

      {/* Questions Feed */}
      {isLoading ? (
        <div className="py-12 text-center text-slate-400">
          <span className="inline-block animate-spin border-3 border-primary border-t-transparent w-7 h-7 rounded-full mb-3" />
          <p className="text-xs font-bold">Đang tải danh sách câu hỏi...</p>
        </div>
      ) : filteredQuestions.length === 0 ? (
        <div className="py-12 bg-slate-900/40 border border-dashed border-slate-800 rounded-2xl text-center px-4">
          <span className="material-symbols-outlined text-slate-600 text-4xl mb-2">
            question_answer
          </span>
          <p className="text-slate-300 font-bold text-sm">Chưa có câu hỏi nào</p>
          <p className="text-slate-500 text-xs mt-1 max-w-sm mx-auto">
            {searchKeyword
              ? 'Không tìm thấy câu hỏi phù hợp với từ khóa.'
              : 'Hãy đặt câu hỏi đầu tiên nếu bạn gặp khó khăn trong bài học này!'}
          </p>
          {!searchKeyword && (
            <button
              onClick={() => setIsAsking(true)}
              className="mt-4 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-primary-container text-xs font-bold rounded-xl border border-slate-700 transition-all cursor-pointer"
            >
              Đặt câu hỏi ngay
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          {filteredQuestions.map((q) => {
            const isExpanded = expandedQuestions[q.id] ?? false;
            const currentReplyText = replyInputs[q.id] ?? '';
            const isSubmittingThisReply = submittingReplies[q.id] ?? false;

            return (
              <div
                key={q.id}
                className="bg-slate-900 border border-slate-800/80 rounded-2xl p-5 hover:border-slate-700/80 transition-all shadow-sm space-y-4"
              >
                {/* Question Header */}
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center font-extrabold text-white text-xs shadow-md">
                      {q.author.avatarUrl ? (
                        <img
                          src={q.author.avatarUrl}
                          alt={q.author.fullName}
                          className="w-full h-full rounded-full object-cover"
                        />
                      ) : (
                        q.author.fullName.charAt(0).toUpperCase()
                      )}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-white">{q.author.fullName}</span>
                        {q.author.role === 'LECTURER' && (
                          <span className="bg-purple-500/20 text-purple-300 border border-purple-500/30 text-[9px] font-extrabold px-2 py-0.5 rounded-full">
                            Giảng viên
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-2 text-[11px] text-slate-400 mt-0.5">
                        <span>{formatTimeAgo(q.createdAt)}</span>
                        {scope === 'all_course' && q.lessonTitle && (
                          <>
                            <span>•</span>
                            <span className="text-indigo-400 font-semibold truncate max-w-xs">
                              {q.lessonTitle}
                            </span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Right Header Actions */}
                  <div className="flex items-center gap-2 shrink-0">
                    {/* Instructor Answered Badge */}
                    {q.hasInstructorReply && (
                      <span className="bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 text-[10px] font-bold px-2.5 py-1 rounded-full flex items-center gap-1 shrink-0">
                        <span className="material-symbols-outlined text-[14px]">verified</span>
                        <span>Giảng viên đã trả lời</span>
                      </span>
                    )}

                    {/* Delete Question button for author or lecturer/admin */}
                    {(currentUser?.id === q.author.id ||
                      currentUser?.role === 'ADMIN' ||
                      appMode === 'LECTURER') && (
                      <button
                        onClick={() => handleDeleteQuestion(q.id)}
                        className="p-1 text-slate-500 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors cursor-pointer shrink-0"
                        title="Xóa câu hỏi này"
                      >
                        <span className="material-symbols-outlined text-[16px]">delete</span>
                      </button>
                    )}
                  </div>
                </div>

                {/* Question Body */}
                <div className="space-y-1.5 pl-12">
                  <h4 className="text-sm font-bold text-slate-100 m-0 leading-snug break-words">{q.title}</h4>
                  <p className="text-xs text-slate-300 whitespace-pre-wrap leading-relaxed break-words m-0">
                    {q.content}
                  </p>
                </div>

                {/* Footer / Toggle replies */}
                <div className="pl-12 flex items-center justify-between pt-2 border-t border-slate-800/60">
                  <button
                    onClick={() => toggleExpandThread(q.id)}
                    className="text-xs font-bold text-primary-container hover:text-white flex items-center gap-1.5 transition-colors cursor-pointer"
                  >
                    <span className="material-symbols-outlined text-[16px]">
                      {isExpanded ? 'chat_bubble' : 'chat_bubble_outline'}
                    </span>
                    <span>
                      {q.answersCount > 0
                        ? `${q.answersCount} câu trả lời`
                        : 'Viết câu trả lời đầu tiên'}
                    </span>
                    <span className="material-symbols-outlined text-[16px]">
                      {isExpanded ? 'expand_less' : 'expand_more'}
                    </span>
                  </button>

                  <button
                    onClick={() => toggleExpandThread(q.id)}
                    className="text-[11px] text-slate-400 hover:text-slate-200 transition-colors cursor-pointer"
                  >
                    {isExpanded ? 'Thu gọn' : 'Mở rộng'}
                  </button>
                </div>

                {/* Thread: Answers List & Quick Reply */}
                {isExpanded && (
                  <div className="pl-12 space-y-3 pt-3 animate-in fade-in duration-150">
                    {/* List of answers */}
                    {q.answers && q.answers.length > 0 && (
                      <div className="space-y-2.5">
                        {q.answers.map((ans) => (
                          <div
                            key={ans.id}
                            className={`p-3.5 rounded-xl border text-xs space-y-1.5 ${
                              ans.isInstructorReply
                                ? 'bg-indigo-950/40 border-indigo-500/30'
                                : 'bg-slate-950/80 border-slate-800'
                            }`}
                          >
                            <div className="flex items-center justify-between">
                              <div className="flex items-center gap-2">
                                <span className="font-bold text-white">{ans.author.fullName}</span>
                                {ans.isInstructorReply && (
                                  <span className="bg-primary/20 text-indigo-300 border border-primary/30 text-[9px] font-extrabold px-2 py-0.5 rounded-md flex items-center gap-1">
                                    <span className="material-symbols-outlined text-[11px]">
                                      school
                                    </span>
                                    <span>Giảng viên</span>
                                  </span>
                                )}
                              </div>
                              <div className="flex items-center gap-2">
                                <span className="text-[10px] text-slate-500">
                                  {formatTimeAgo(ans.createdAt)}
                                </span>
                                {(currentUser?.id === ans.author.id ||
                                  currentUser?.role === 'ADMIN' ||
                                  appMode === 'LECTURER') && (
                                  <button
                                    onClick={() => handleDeleteAnswer(q.id, ans.id)}
                                    className="p-1 text-slate-500 hover:text-red-400 hover:bg-red-500/10 rounded transition-colors cursor-pointer"
                                    title="Xóa câu trả lời này"
                                  >
                                    <span className="material-symbols-outlined text-[14px]">delete</span>
                                  </button>
                                )}
                              </div>
                            </div>

                            <p className="text-slate-200 whitespace-pre-wrap leading-relaxed break-words m-0">
                              {ans.content}
                            </p>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Fast Reply Box */}
                    <div className="flex items-center gap-2 pt-1">
                      <input
                        type="text"
                        placeholder="Viết phản hồi của bạn..."
                        value={currentReplyText}
                        onChange={(e) =>
                          setReplyInputs((prev) => ({ ...prev, [q.id]: e.target.value }))
                        }
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') {
                            e.preventDefault();
                            handleCreateAnswer(q.id);
                          }
                        }}
                        className="flex-1 px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-primary transition-all"
                      />
                      <button
                        onClick={() => handleCreateAnswer(q.id)}
                        disabled={isSubmittingThisReply || !currentReplyText.trim()}
                        className="px-3.5 py-2 bg-primary hover:bg-primary/90 disabled:opacity-40 text-white font-bold text-xs rounded-xl transition-all cursor-pointer flex items-center gap-1 active:scale-95"
                      >
                        <span className="material-symbols-outlined text-[16px]">send</span>
                        <span>Gửi</span>
                      </button>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
