import React, { useState, useEffect, useCallback } from 'react';
import { reviewApi } from '../../api/reviewApi';
import type {
  CourseReviewSummary,
  InstructorReview,
  PaginatedReviews
} from '../../../../types/review';
import { CourseRatingHistogram } from './CourseRatingHistogram';

interface InstructorReviewsListProps {
  courseId: number;
  courseTitle: string;
}

export const InstructorReviewsList: React.FC<InstructorReviewsListProps> = ({
  courseId,
  courseTitle
}) => {
  const [summary, setSummary] = useState<CourseReviewSummary | null>(null);
  const [reviewsData, setReviewsData] = useState<PaginatedReviews<InstructorReview> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedRating, setSelectedRating] = useState<number | undefined>(undefined);
  const [currentPage, setCurrentPage] = useState(0);

  const loadSummary = useCallback(async () => {
    try {
      const data = await reviewApi.getInstructorReviewSummary(courseId);
      setSummary(data);
    } catch {
      // Ignored
    }
  }, [courseId]);

  const loadReviews = useCallback(async (page: number, rating?: number) => {
    setIsLoading(true);
    try {
      const data = await reviewApi.getInstructorReviews(courseId, page, 10, rating);
      setReviewsData(data);
    } catch {
      // Ignored
    } finally {
      setIsLoading(false);
    }
  }, [courseId]);

  useEffect(() => {
    loadSummary();
  }, [loadSummary]);

  useEffect(() => {
    loadReviews(currentPage, selectedRating);
  }, [loadReviews, currentPage, selectedRating]);

  const handleRatingFilterChange = (rating?: number) => {
    setSelectedRating(rating);
    setCurrentPage(0);
  };

  const formatDate = (isoString: string) => {
    try {
      return new Intl.DateTimeFormat('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      }).format(new Date(isoString));
    } catch {
      return isoString;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header Info */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div>
          <h3 className="text-lg font-bold text-slate-900">
            Đánh giá của học viên
          </h3>
          <p className="text-xs text-slate-500">
            Khóa học: <strong className="text-slate-700">{courseTitle}</strong>
          </p>
        </div>
        <div className="text-xs font-semibold text-slate-500 bg-slate-100 px-3 py-1.5 rounded-lg self-start sm:self-auto">
          Chế độ xem quản trị (Chỉ đọc)
        </div>
      </div>

      {/* Histogram */}
      {summary && summary.totalReviews > 0 ? (
        <CourseRatingHistogram
          summary={summary}
          activeFilter={selectedRating}
          onSelectRatingFilter={handleRatingFilterChange}
        />
      ) : (
        <div className="bg-slate-50 border border-slate-200 rounded-2xl p-8 text-center text-slate-500 text-xs font-medium">
          Khóa học này hiện chưa có đánh giá nào từ học viên.
        </div>
      )}

      {/* Filter toolbar */}
      {summary && summary.totalReviews > 0 && (
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
          <button
            onClick={() => handleRatingFilterChange(undefined)}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors cursor-pointer shrink-0 ${
              selectedRating === undefined
                ? 'bg-slate-900 text-white'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            Tất cả ({summary.totalReviews})
          </button>
          {[5, 4, 3, 2, 1].map((star) => {
            const count = summary.ratingCounts?.[star] || 0;
            return (
              <button
                key={star}
                onClick={() => handleRatingFilterChange(selectedRating === star ? undefined : star)}
                className={`inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-bold transition-colors cursor-pointer shrink-0 ${
                  selectedRating === star
                    ? 'bg-amber-400 text-slate-950 font-black'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                <span>{star}</span>
                <span className="material-symbols-outlined text-[14px]" style={{ fontVariationSettings: "'FILL' 1" }}>
                  star
                </span>
                <span className="text-[11px] opacity-75">({count})</span>
              </button>
            );
          })}
        </div>
      )}

      {/* Reviews Table / List */}
      {isLoading ? (
        <div className="py-12 text-center text-slate-400">
          <span className="inline-block animate-spin border-3 border-primary border-t-transparent w-6 h-6 rounded-full mb-2" />
          <p className="text-xs font-medium">Đang tải danh sách đánh giá...</p>
        </div>
      ) : reviewsData && reviewsData.content.length > 0 ? (
        <div className="space-y-3">
          {reviewsData.content.map((review) => (
            <div
              key={review.id}
              className="bg-surface-container-lowest border border-slate-200 rounded-2xl p-5 space-y-3"
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                {/* Học viên */}
                <div className="flex items-center gap-3">
                  {review.studentAvatarUrl ? (
                    <img
                      src={review.studentAvatarUrl}
                      alt={review.studentName}
                      className="w-9 h-9 rounded-full object-cover border border-slate-200"
                    />
                  ) : (
                    <div className="w-9 h-9 rounded-full bg-primary/10 text-primary font-bold text-xs flex items-center justify-center border border-primary/20">
                      {review.studentName?.charAt(0) || 'U'}
                    </div>
                  )}
                  <div>
                    <div className="text-xs font-bold text-slate-900">{review.studentName}</div>
                    <div className="text-[11px] text-slate-400">{review.studentEmail}</div>
                  </div>
                </div>

                {/* Badge tiến độ & thời gian */}
                <div className="flex items-center gap-3 text-xs text-slate-400">
                  {review.progressPercent !== undefined && review.progressPercent !== null && (
                    <span className="bg-emerald-50 text-emerald-700 border border-emerald-200 text-[11px] font-bold px-2 py-0.5 rounded-full">
                      Đã học {Math.round(review.progressPercent)}%
                    </span>
                  )}
                  <span>{formatDate(review.createdAt)}</span>
                </div>
              </div>

              {/* Sao & Nhận xét */}
              <div className="flex items-center gap-1">
                {[1, 2, 3, 4, 5].map((star) => (
                  <span
                    key={star}
                    className={`material-symbols-outlined text-[16px] ${
                      review.rating >= star ? 'text-amber-400' : 'text-slate-200'
                    }`}
                    style={{ fontVariationSettings: "'FILL' 1" }}
                  >
                    star
                  </span>
                ))}
              </div>

              {review.comment ? (
                <p className="text-xs sm:text-sm text-slate-700 leading-relaxed whitespace-pre-line bg-slate-50/70 p-3 rounded-xl border border-slate-100">
                  {review.comment}
                </p>
              ) : (
                <p className="text-xs text-slate-400 italic">Học viên không để lại nhận xét bằng chữ</p>
              )}
            </div>
          ))}
        </div>
      ) : summary && summary.totalReviews > 0 ? (
        <div className="py-8 text-center text-slate-400 text-xs">
          Không có đánh giá nào cho mức lọc sao này.
        </div>
      ) : null}

      {/* Phân trang */}
      {reviewsData && reviewsData.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-4">
          <button
            onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
            disabled={currentPage === 0}
            className="px-3 py-1.5 rounded-lg border border-slate-200 text-xs font-semibold text-slate-700 disabled:opacity-40 hover:bg-slate-50 cursor-pointer"
          >
            Trang trước
          </button>
          <span className="text-xs text-slate-500 font-medium px-2">
            Trang {currentPage + 1} / {reviewsData.totalPages}
          </span>
          <button
            onClick={() => setCurrentPage((p) => Math.min(reviewsData.totalPages - 1, p + 1))}
            disabled={currentPage >= reviewsData.totalPages - 1}
            className="px-3 py-1.5 rounded-lg border border-slate-200 text-xs font-semibold text-slate-700 disabled:opacity-40 hover:bg-slate-50 cursor-pointer"
          >
            Trang sau
          </button>
        </div>
      )}
    </div>
  );
};
