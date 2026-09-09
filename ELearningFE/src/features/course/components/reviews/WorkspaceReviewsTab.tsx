import React, { useState, useEffect, useCallback } from 'react';
import { reviewApi } from '../../api/reviewApi';
import type {
  CourseReview,
  CourseReviewSummary,
  PaginatedReviews
} from '../../../../types/review';

interface WorkspaceReviewsTabProps {
  courseId: number;
  courseTitle: string;
  currentProgress: number;
  myReview: CourseReview | null;
  summary: CourseReviewSummary | null;
  onOpenReviewModal: (rating?: number) => void;
  onDeleteReview?: () => void;
}

export const WorkspaceReviewsTab: React.FC<WorkspaceReviewsTabProps> = ({
  courseId,
  currentProgress,
  myReview,
  summary,
  onOpenReviewModal,
  onDeleteReview
}) => {
  const [reviewsData, setReviewsData] = useState<PaginatedReviews<CourseReview> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedRating, setSelectedRating] = useState<number | undefined>(undefined);
  const [sortBy, setSortBy] = useState<'newest' | 'highest' | 'lowest'>('newest');
  const [currentPage, setCurrentPage] = useState(0);

  const loadReviews = useCallback(async (page: number, rating?: number, sort?: 'newest' | 'highest' | 'lowest') => {
    setIsLoading(true);
    try {
      const data = await reviewApi.getCourseReviews(courseId, page, 6, rating, sort);
      setReviewsData(data);
    } catch {
      // Ignored
    } finally {
      setIsLoading(false);
    }
  }, [courseId]);

  useEffect(() => {
    loadReviews(currentPage, selectedRating, sortBy);
  }, [loadReviews, currentPage, selectedRating, sortBy]);

  const handleRatingFilter = (rating?: number) => {
    setSelectedRating(rating);
    setCurrentPage(0);
  };

  const handleSortChange = (newSort: 'newest' | 'highest' | 'lowest') => {
    setSortBy(newSort);
    setCurrentPage(0);
  };

  const formatDate = (isoString: string) => {
    try {
      const date = new Date(isoString);
      return new Intl.DateTimeFormat('vi-VN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      }).format(date);
    } catch {
      return isoString;
    }
  };

  const getInitials = (name: string) => {
    return (name || 'Học viên')
      .split(' ')
      .filter(Boolean)
      .slice(-2)
      .map((n) => n[0])
      .join('')
      .toUpperCase();
  };

  return (
    <div className="space-y-8 animate-fade-in text-slate-200">
      {/* 1. Header / Rating Breakdown Summary */}
      <div className="bg-slate-900 rounded-3xl p-6 border border-slate-800 shadow-lg">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-center">
          {/* Average Score Box */}
          <div className="md:col-span-4 flex flex-col items-center justify-center text-center p-4 bg-slate-950/60 rounded-2xl border border-slate-800/80">
            <span className="text-5xl font-black text-amber-400 font-display">
              {summary ? summary.ratingAvg.toFixed(1) : '0.0'}
            </span>
            <div className="flex items-center gap-1 my-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <span
                  key={star}
                  className="material-symbols-outlined text-[20px] text-amber-400"
                  style={{
                    fontVariationSettings: star <= Math.round(summary?.ratingAvg || 0) ? "'FILL' 1" : "'FILL' 0"
                  }}
                >
                  star
                </span>
              ))}
            </div>
            <span className="text-xs font-bold text-slate-400">
              {summary?.totalReviews || 0} đánh giá từ học viên
            </span>
          </div>

          {/* Histogram distribution bars */}
          <div className="md:col-span-8 space-y-2">
            {[5, 4, 3, 2, 1].map((stars) => {
              const count = summary?.ratingCounts?.[stars] || 0;
              const percent = summary?.ratingPercentages?.[stars] || 0;
              const isSelected = selectedRating === stars;

              return (
                <button
                  key={stars}
                  type="button"
                  onClick={() => handleRatingFilter(isSelected ? undefined : stars)}
                  className={`w-full flex items-center gap-3 group py-1 px-2 rounded-xl transition-all cursor-pointer ${
                    isSelected ? 'bg-amber-500/15 border border-amber-500/30' : 'hover:bg-slate-800/60'
                  }`}
                >
                  <div className="flex items-center gap-1 w-14 shrink-0 justify-end">
                    <span className="text-xs font-bold text-slate-300 group-hover:text-amber-400 transition-colors">
                      {stars}
                    </span>
                    <span className="material-symbols-outlined text-[14px] text-amber-400" style={{ fontVariationSettings: "'FILL' 1" }}>
                      star
                    </span>
                  </div>

                  <div className="flex-1 bg-slate-800 rounded-full h-2.5 overflow-hidden">
                    <div
                      className="bg-amber-400 h-full rounded-full transition-all duration-500 group-hover:bg-amber-300"
                      style={{ width: `${percent}%` }}
                    />
                  </div>

                  <span className="text-xs text-slate-400 w-12 text-right font-medium">
                    {percent}%
                  </span>
                  <span className="text-[11px] text-slate-500 w-8 text-right">
                    ({count})
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* 2. Student's Current Review Status Action Box */}
      <div className="bg-slate-900 rounded-3xl p-6 border border-slate-800 shadow-lg">
        {myReview ? (
          <div className="space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-800 pb-3">
              <div className="flex items-center gap-2">
                <span className="text-xs font-black uppercase tracking-wider text-amber-400 bg-amber-400/10 px-3 py-1 rounded-full border border-amber-400/20">
                  Đánh giá của bạn
                </span>
                <span className="text-[11px] text-slate-400">
                  Đăng ngày {formatDate(myReview.createdAt)}
                </span>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => onOpenReviewModal(myReview.rating)}
                  className="px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 hover:text-white font-bold text-xs transition-all flex items-center gap-1.5 cursor-pointer border border-slate-700"
                >
                  <span className="material-symbols-outlined text-[16px]">edit_note</span>
                  <span>Chỉnh sửa</span>
                </button>
                {onDeleteReview && (
                  <button
                    type="button"
                    onClick={onDeleteReview}
                    className="px-3 py-1.5 rounded-xl bg-rose-950/40 hover:bg-rose-900/60 text-rose-300 hover:text-rose-100 font-bold text-xs transition-all flex items-center gap-1 cursor-pointer border border-rose-800/40"
                  >
                    <span className="material-symbols-outlined text-[16px]">delete</span>
                    <span>Xóa</span>
                  </button>
                )}
              </div>
            </div>

            <div className="flex items-center gap-1">
              {[1, 2, 3, 4, 5].map((star) => (
                <span
                  key={star}
                  className="material-symbols-outlined text-[18px] text-amber-400"
                  style={{
                    fontVariationSettings: star <= myReview.rating ? "'FILL' 1" : "'FILL' 0"
                  }}
                >
                  star
                </span>
              ))}
              <span className="text-xs font-bold text-slate-300 ml-2">
                {myReview.rating}/5 sao
              </span>
            </div>

            {myReview.comment ? (
              <p className="text-xs sm:text-sm text-slate-200 leading-relaxed bg-slate-950/50 p-4 rounded-2xl border border-slate-800/80 m-0">
                "{myReview.comment}"
              </p>
            ) : (
              <p className="text-xs text-slate-500 italic m-0">
                (Bạn đã chấm {myReview.rating} sao và không để lại lời nhận xét)
              </p>
            )}
          </div>
        ) : currentProgress >= 20 ? (
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-amber-400 text-[22px]">
                  rate_review
                </span>
                <h4 className="text-sm font-bold text-white m-0">
                  Chia sẻ trải nghiệm học tập của bạn
                </h4>
              </div>
              <p className="text-xs text-slate-400 mt-1 max-w-md">
                Bạn đã hoàn thành <strong className="text-emerald-400">{currentProgress}%</strong> lộ trình khóa học! Hãy dành 1 phút để đánh giá chất lượng bài giảng và hỗ trợ cộng đồng nhé.
              </p>
            </div>
            <button
              type="button"
              onClick={() => onOpenReviewModal()}
              className="px-5 py-2.5 rounded-xl bg-primary hover:bg-primary-hover text-white font-bold text-xs transition-all shadow-lg shadow-primary/20 flex items-center gap-2 cursor-pointer shrink-0 self-start sm:self-auto"
            >
              <span className="material-symbols-outlined text-[18px]">star</span>
              <span>Viết đánh giá ngay</span>
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-3 p-2 text-slate-400">
            <span className="material-symbols-outlined text-[24px] text-amber-500 shrink-0">
              lock_clock
            </span>
            <div className="text-xs leading-relaxed">
              <strong className="text-slate-300">Cần đạt tối thiểu 20% tiến độ để mở đánh giá.</strong> Hiện tại bạn đã hoàn thành <strong className="text-amber-400">{currentProgress}%</strong>. Tiếp tục học thêm các bài giảng để chia sẻ cảm nhận nhé!
            </div>
          </div>
        )}
      </div>

      {/* 3. Community Reviews Section */}
      <div className="space-y-4">
        {/* Filter Pills and Sort Toolbar */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-4">
          {/* Star Filter Pills */}
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => handleRatingFilter(undefined)}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                selectedRating === undefined
                  ? 'bg-amber-400 text-slate-950 shadow-md shadow-amber-400/20'
                  : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
              }`}
            >
              Tất cả
            </button>
            {[5, 4, 3, 2, 1].map((s) => (
              <button
                key={s}
                type="button"
                onClick={() => handleRatingFilter(s)}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1 cursor-pointer ${
                  selectedRating === s
                    ? 'bg-amber-400 text-slate-950 shadow-md shadow-amber-400/20'
                    : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                }`}
              >
                <span>{s}</span>
                <span className="material-symbols-outlined text-[14px]" style={{ fontVariationSettings: "'FILL' 1" }}>
                  star
                </span>
              </button>
            ))}
          </div>

          {/* Sort Selector */}
          <div className="flex items-center gap-2 self-end sm:self-auto">
            <span className="text-xs text-slate-400">Sắp xếp:</span>
            <select
              value={sortBy}
              onChange={(e) => handleSortChange(e.target.value as any)}
              className="bg-slate-900 border border-slate-700 rounded-xl px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-primary cursor-pointer"
            >
              <option value="newest">Mới nhất</option>
              <option value="highest">Đánh giá cao nhất</option>
              <option value="lowest">Đánh giá thấp nhất</option>
            </select>
          </div>
        </div>

        {/* Reviews List */}
        {isLoading ? (
          <div className="py-12 text-center text-slate-400">
            <span className="material-symbols-outlined text-[32px] animate-spin text-primary">
              progress_activity
            </span>
            <p className="text-xs font-bold mt-2">Đang tải danh sách đánh giá...</p>
          </div>
        ) : !reviewsData || reviewsData.content.length === 0 ? (
          <div className="bg-slate-900/60 rounded-3xl p-10 border border-slate-800 text-center space-y-2">
            <span className="material-symbols-outlined text-[40px] text-slate-600">
              rate_review
            </span>
            <p className="text-sm font-bold text-slate-300">Không tìm thấy đánh giá nào</p>
            <p className="text-xs text-slate-500">
              {selectedRating ? `Chưa có đánh giá nào ${selectedRating} sao.` : 'Hãy là người đầu tiên trải nghiệm và để lại đánh giá!'}
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {reviewsData.content.map((rev) => (
              <div
                key={rev.id}
                className="bg-slate-900/70 rounded-2xl p-5 border border-slate-800/90 space-y-3 hover:border-slate-700 transition-colors"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex items-center gap-3">
                    {rev.userAvatarUrl ? (
                      <img
                        src={rev.userAvatarUrl}
                        alt={rev.userName}
                        className="w-9 h-9 rounded-full object-cover border border-slate-700"
                      />
                    ) : (
                      <div className="w-9 h-9 rounded-full bg-slate-800 text-primary-container font-bold text-xs flex items-center justify-center border border-slate-700">
                        {getInitials(rev.userName)}
                      </div>
                    )}
                    <div>
                      <div className="text-xs font-extrabold text-white">
                        {rev.userName}
                      </div>
                      <div className="flex items-center gap-2 mt-0.5">
                        <div className="flex items-center">
                          {[1, 2, 3, 4, 5].map((star) => (
                            <span
                              key={star}
                              className="material-symbols-outlined text-[14px] text-amber-400"
                              style={{
                                fontVariationSettings: star <= rev.rating ? "'FILL' 1" : "'FILL' 0"
                              }}
                            >
                              star
                            </span>
                          ))}
                        </div>
                        {rev.progressPercent !== undefined && rev.progressPercent > 0 && (
                          <span className="text-[10px] font-semibold text-emerald-400 bg-emerald-950/60 border border-emerald-800/40 px-2 py-0.5 rounded-full">
                            Đã học {Math.round(rev.progressPercent)}%
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  <span className="text-[11px] text-slate-500">
                    {formatDate(rev.createdAt)}
                  </span>
                </div>

                {rev.comment && (
                  <p className="text-xs sm:text-sm text-slate-300 leading-relaxed m-0 whitespace-pre-line pl-12">
                    {rev.comment}
                  </p>
                )}
              </div>
            ))}

            {/* Pagination Controls */}
            {reviewsData.totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 pt-4">
                <button
                  type="button"
                  onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                  disabled={currentPage === 0}
                  className="px-3 py-1.5 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-xs font-bold transition-all cursor-pointer"
                >
                  Trang trước
                </button>
                <span className="text-xs text-slate-400 px-2 font-medium">
                  Trang {currentPage + 1} / {reviewsData.totalPages}
                </span>
                <button
                  type="button"
                  onClick={() => setCurrentPage((p) => Math.min(reviewsData.totalPages - 1, p + 1))}
                  disabled={currentPage >= reviewsData.totalPages - 1}
                  className="px-3 py-1.5 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-xs font-bold transition-all cursor-pointer"
                >
                  Trang sau
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
