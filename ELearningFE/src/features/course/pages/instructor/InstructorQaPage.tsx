import { useConfirm } from '../../../../app/context/ConfirmContext';
import React, { useState, useEffect, useMemo } from 'react';
import type { CourseQuestion } from '../../../../types/qa';
import { qaApi } from '../../api/qaApi';
import { useToast } from '../../../../app/context/ToastContext';
import { useAuth } from '../../../../app/context/AuthContext';

interface InstructorQaPageProps {
  courseId: number;
  courseTitle?: string;
  onBack: () => void;
}

export const InstructorQaPage: React.FC<InstructorQaPageProps> = ({
  courseId,
  courseTitle = 'Khóa học',
  onBack
}) => {
  const { currentUser } = useAuth();
  const { showSuccess, showError } = useToast();
  const confirm = useConfirm();

  const [questions, setQuestions] = useState<CourseQuestion[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [filter, setFilter] = useState<'all' | 'unanswered' | 'answered'>('all');
  const [searchKeyword, setSearchKeyword] = useState<string>('');

  // Reply inputs
  const [replyInputs, setReplyInputs] = useState<Record<number, string>>({});
  const [submittingReplies, setSubmittingReplies] = useState<Record<number, boolean>>({});

  const loadQuestions = async () => {
    try {
      setIsLoading(true);
      const data = await qaApi.getInstructorQuestions(courseId, filter);
      setQuestions(data);
    } catch {
      showError('Không thể tải danh sách câu hỏi khóa học.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadQuestions();
  }, [courseId, filter]);

  const filteredQuestions = useMemo(() => {
    if (!searchKeyword.trim()) return questions;
    const kw = searchKeyword.toLowerCase();
    return questions.filter(
      (q) =>
        q.title.toLowerCase().includes(kw) ||
        q.content.toLowerCase().includes(kw) ||
        q.author.fullName.toLowerCase().includes(kw) ||
        q.lessonTitle.toLowerCase().includes(kw)
    );
  }, [questions, searchKeyword]);

  const stats = useMemo(() => {
    const total = questions.length;
    const unanswered = questions.filter((q) => !q.hasInstructorReply).length;
    const answered = total - unanswered;
    return { total, unanswered, answered };
  }, [questions]);

  const handleReplyAsInstructor = async (questionId: number) => {
    const content = replyInputs[questionId]?.trim();
    if (!content) return;

    if (content.length < 2) {
      showError('Nội dung phản hồi phải có ít nhất 2 ký tự.');
      return;
    }

    try {
      setSubmittingReplies((prev) => ({ ...prev, [questionId]: true }));
      const newAnswer = await qaApi.createAnswer(
        courseId,
        questionId,
        { content },
        currentUser?.fullName || 'Giảng viên',
        true
      );

      setQuestions((prev) =>
        prev.map((q) => {
          if (q.id === questionId) {
            return {
              ...q,
              answersCount: q.answersCount + 1,
              hasInstructorReply: true,
              answers: [...(q.answers || []), newAnswer]
            };
          }
          return q;
        })
      );

      setReplyInputs((prev) => ({ ...prev, [questionId]: '' }));
      showSuccess('Đã gửi phản hồi với tư cách Giảng viên!');
    } catch (err: any) {
      if (err?.code === 1324) {
        showError(err.message || 'Vui lòng kiểm tra lại ngôn từ có chứa nội dung không phù hợp và thử lại.');
      } else {
        showError(err.message || 'Lỗi khi gửi câu trả lời.');
      }
    } finally {
      setSubmittingReplies((prev) => ({ ...prev, [questionId]: false }));
    }
  };

  const handleDeleteQuestion = async (questionId: number) => {
    if (!await confirm('Bạn có chắc chắn muốn xóa câu hỏi này cùng các phản hồi?')) {
      return;
    }

    try {
      await qaApi.deleteQuestion(courseId, questionId);
      setQuestions((prev) => prev.filter((q) => q.id !== questionId));
      showSuccess('Đã xóa câu hỏi.');
    } catch (err: any) {
      showError(err.message || 'Lỗi khi xóa câu hỏi.');
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
    <div className="flex-1 flex flex-col bg-background min-h-0 overflow-y-auto">
      {/* Header Bar */}
      <header className="bg-surface-container-lowest border-b border-outline-variant/70 px-6 sm:px-8 py-5 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 shrink-0">
        <div className="flex items-center gap-3">
          <button
            onClick={onBack}
            className="p-2 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
            title="Quay lại danh sách khóa học"
          >
            <span className="material-symbols-outlined text-[20px]">arrow_back</span>
          </button>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-[11px] font-extrabold uppercase tracking-wider text-primary bg-primary/10 px-2 py-0.5 rounded-md">
                Hỏi & Đáp (US-21)
              </span>
              <span className="text-xs text-slate-400 font-medium truncate max-w-sm">
                {courseTitle}
              </span>
            </div>
            <h1 className="text-xl sm:text-2xl font-black text-slate-900 font-display m-0">
              Quản lý Thắc mắc & Trả lời Học viên
            </h1>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <div className="p-6 sm:p-8 space-y-6 max-w-7xl mx-auto w-full">
        {/* Statistics Summary Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider m-0">
                Tổng câu hỏi
              </p>
              <h3 className="text-2xl font-black text-slate-900 mt-1 m-0">{stats.total}</h3>
            </div>
            <span className="p-3 bg-blue-50 text-blue-600 rounded-2xl material-symbols-outlined text-[26px]">
              forum
            </span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-amber-200/80 bg-amber-50/20 shadow-sm flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-amber-700 uppercase tracking-wider m-0">
                Chưa trả lời
              </p>
              <h3 className="text-2xl font-black text-amber-600 mt-1 m-0">{stats.unanswered}</h3>
            </div>
            <span className="p-3 bg-amber-100 text-amber-700 rounded-2xl material-symbols-outlined text-[26px]">
              pending_actions
            </span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-emerald-200/80 bg-emerald-50/20 shadow-sm flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-emerald-700 uppercase tracking-wider m-0">
                Đã giải đáp
              </p>
              <h3 className="text-2xl font-black text-emerald-600 mt-1 m-0">{stats.answered}</h3>
            </div>
            <span className="p-3 bg-emerald-100 text-emerald-700 rounded-2xl material-symbols-outlined text-[26px]">
              task_alt
            </span>
          </div>
        </div>

        {/* Search & Filter Toolbar */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <div className="flex items-center gap-2">
            <button
              onClick={() => setFilter('all')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                filter === 'all'
                  ? 'bg-slate-900 text-white'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              Tất cả ({stats.total})
            </button>
            <button
              onClick={() => setFilter('unanswered')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
                filter === 'unanswered'
                  ? 'bg-amber-600 text-white'
                  : 'bg-amber-50 text-amber-800 hover:bg-amber-100 border border-amber-200/60'
              }`}
            >
              <span className="material-symbols-outlined text-[14px]">warning</span>
              <span>Chưa trả lời ({stats.unanswered})</span>
            </button>
            <button
              onClick={() => setFilter('answered')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                filter === 'answered'
                  ? 'bg-emerald-600 text-white'
                  : 'bg-emerald-50 text-emerald-800 hover:bg-emerald-100 border border-emerald-200/60'
              }`}
            >
              Đã trả lời ({stats.answered})
            </button>
          </div>

          <div className="relative w-full sm:w-72">
            <span className="material-symbols-outlined absolute left-3 top-2 text-slate-400 text-[18px]">
              search
            </span>
            <input
              type="text"
              placeholder="Tìm theo tiêu đề, học viên, bài học..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:border-primary transition-all"
            />
          </div>
        </div>

        {/* Questions Feed */}
        {isLoading ? (
          <div className="py-16 text-center text-slate-500">
            <span className="inline-block animate-spin border-3 border-primary border-t-transparent w-8 h-8 rounded-full mb-3" />
            <p className="text-xs font-bold">Đang tải danh sách câu hỏi...</p>
          </div>
        ) : filteredQuestions.length === 0 ? (
          <div className="py-16 bg-white border border-dashed border-slate-300 rounded-3xl text-center px-4">
            <span className="material-symbols-outlined text-slate-400 text-5xl mb-2">
              chat_bubble_outline
            </span>
            <h4 className="text-slate-800 font-bold text-base m-0">Không có câu hỏi nào</h4>
            <p className="text-slate-500 text-xs mt-1">
              {searchKeyword
                ? 'Không tìm thấy câu hỏi nào phù hợp với bộ lọc tìm kiếm.'
                : 'Khóa học chưa có thắc mắc nào cần giải đáp.'}
            </p>
          </div>
        ) : (
          <div className="space-y-5">
            {filteredQuestions.map((q) => {
              const currentReply = replyInputs[q.id] ?? '';
              const isSubmitting = submittingReplies[q.id] ?? false;

              return (
                <div
                  key={q.id}
                  className={`bg-white rounded-2xl border p-6 shadow-sm transition-all space-y-4 ${
                    !q.hasInstructorReply
                      ? 'border-amber-300 ring-2 ring-amber-100'
                      : 'border-slate-200'
                  }`}
                >
                  {/* Top Header */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-3">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-slate-100 text-slate-700 font-extrabold flex items-center justify-center text-sm border border-slate-200">
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
                          <span className="text-xs font-bold text-slate-900">
                            {q.author.fullName}
                          </span>
                          <span className="text-[11px] text-slate-400">
                            • {formatTimeAgo(q.createdAt)}
                          </span>
                        </div>
                        <div className="flex items-center gap-1.5 mt-0.5 text-xs text-primary font-semibold">
                          <span className="material-symbols-outlined text-[14px]">article</span>
                          <span className="truncate max-w-md">{q.lessonTitle}</span>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      {q.hasInstructorReply ? (
                        <span className="bg-emerald-50 text-emerald-700 border border-emerald-200 text-[10px] font-bold px-2.5 py-1 rounded-full flex items-center gap-1">
                          <span className="material-symbols-outlined text-[13px]">check_circle</span>
                          <span>Đã trả lời</span>
                        </span>
                      ) : (
                        <span className="bg-amber-50 text-amber-700 border border-amber-300 text-[10px] font-bold px-2.5 py-1 rounded-full flex items-center gap-1">
                          <span className="material-symbols-outlined text-[13px]">schedule</span>
                          <span>Đang đợi phản hồi</span>
                        </span>
                      )}

                      <button
                        onClick={() => handleDeleteQuestion(q.id)}
                        className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
                        title="Xóa câu hỏi này"
                      >
                        <span className="material-symbols-outlined text-[18px]">delete</span>
                      </button>
                    </div>
                  </div>

                  {/* Question Content */}
                  <div className="space-y-1.5">
                    <h3 className="text-sm font-bold text-slate-900 m-0">{q.title}</h3>
                    <p className="text-xs text-slate-700 whitespace-pre-wrap leading-relaxed m-0">
                      {q.content}
                    </p>
                  </div>

                  {/* Thread Replies */}
                  {q.answers && q.answers.length > 0 && (
                    <div className="space-y-2.5 pt-2 border-t border-slate-100">
                      <p className="text-[11px] font-extrabold uppercase tracking-wider text-slate-400 m-0">
                        Các phản hồi ({q.answers.length})
                      </p>
                      {q.answers.map((ans) => (
                        <div
                          key={ans.id}
                          className={`p-3.5 rounded-xl text-xs space-y-1 ${
                            ans.isInstructorReply
                              ? 'bg-indigo-50/70 border border-indigo-200 text-indigo-950'
                              : 'bg-slate-50 border border-slate-200 text-slate-800'
                          }`}
                        >
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                              <span className="font-bold">{ans.author.fullName}</span>
                              {ans.isInstructorReply && (
                                <span className="bg-indigo-600 text-white text-[9px] font-extrabold px-1.5 py-0.5 rounded flex items-center gap-0.5">
                                  <span className="material-symbols-outlined text-[10px]">
                                    school
                                  </span>
                                  <span>Giảng viên</span>
                                </span>
                              )}
                            </div>
                            <span className="text-[10px] text-slate-500">
                              {formatTimeAgo(ans.createdAt)}
                            </span>
                          </div>
                          <p className="whitespace-pre-wrap leading-relaxed m-0">{ans.content}</p>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Reply Action Box */}
                  <div className="pt-2 border-t border-slate-100 flex flex-col sm:flex-row gap-2">
                    <input
                      type="text"
                      placeholder="Nhập câu trả lời giải đáp cho học viên..."
                      value={currentReply}
                      onChange={(e) =>
                        setReplyInputs((prev) => ({ ...prev, [q.id]: e.target.value }))
                      }
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault();
                          handleReplyAsInstructor(q.id);
                        }
                      }}
                      className="flex-1 px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:border-primary transition-all"
                    />
                    <button
                      onClick={() => handleReplyAsInstructor(q.id)}
                      disabled={isSubmitting || !currentReply.trim()}
                      className="px-4 py-2 bg-primary hover:bg-primary/90 disabled:opacity-50 text-white text-xs font-bold rounded-xl shadow-sm transition-all cursor-pointer flex items-center justify-center gap-1.5 shrink-0"
                    >
                      <span className="material-symbols-outlined text-[16px]">reply</span>
                      <span>{isSubmitting ? 'Đang gửi...' : 'Trả lời'}</span>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
