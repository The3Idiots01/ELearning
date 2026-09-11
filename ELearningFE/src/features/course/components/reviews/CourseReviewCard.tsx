import React from 'react';
import type { CourseReview } from '../../../../types/review';

interface CourseReviewCardProps {
  review: CourseReview;
  isMyReview?: boolean;
  onEdit?: (review: CourseReview) => void;
  onDelete?: (review: CourseReview) => void;
  isDeleting?: boolean;
}

export const CourseReviewCard: React.FC<CourseReviewCardProps> = ({
  review,
  isMyReview = false,
  onEdit,
  onDelete,
  isDeleting = false
}) => {
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

  const initials = (review.userName || 'Học viên')
    .split(' ')
    .filter(Boolean)
    .slice(-2)
    .map((n) => n[0])
    .join('')
    .toUpperCase();

  return (
    <div
      className={`rounded-2xl p-5 transition-all ${
        isMyReview
          ? 'bg-primary/5 border-2 border-primary/30 shadow-sm'
          : 'bg-surface-container-lowest border border-slate-200/80 hover:border-slate-300'
      }`}
    >
      <div className="flex items-start justify-between gap-4 mb-3">
        <div className="flex items-center gap-3">
          {/* Avatar */}
          {review.userAvatarUrl ? (
            <img
              src={review.userAvatarUrl}
              alt={review.userName}
              className="w-10 h-10 rounded-full object-cover border border-slate-200"
            />
          ) : (
            <div className="w-10 h-10 rounded-full bg-primary/10 text-primary font-bold text-xs flex items-center justify-center border border-primary/20">
              {initials}
            </div>
          )}

          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-sm text-slate-900">
                {review.userName || 'Học viên ẩn danh'}
              </span>
              {isMyReview && (
                <span className="bg-primary/15 text-primary text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider">
                  Đánh giá của bạn
                </span>
              )}
            </div>

            <div className="flex items-center gap-2 mt-0.5 text-xs text-slate-400">
              <span>{formatDate(review.createdAt)}</span>
              {review.isEdited && (
                <>
                  <span>•</span>
                  <span className="italic text-[11px]">Đã chỉnh sửa</span>
                </>
              )}
              {review.progressPercent !== undefined && review.progressPercent !== null && (
                <>
                  <span>•</span>
                  <span className="text-emerald-600 font-semibold text-[11px]">
                    Đã học {Math.round(review.progressPercent)}%
                  </span>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Action buttons if this is current user's review */}
        {isMyReview && (
          <div className="flex items-center gap-1 shrink-0">
            <button
              onClick={() => onEdit?.(review)}
              disabled={isDeleting}
              className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-slate-700 hover:text-primary hover:bg-white rounded-lg border border-slate-200 transition-colors cursor-pointer"
              title="Chỉnh sửa đánh giá"
            >
              <span className="material-symbols-outlined text-[16px]">edit</span>
              <span>Sửa</span>
            </button>
            <button
              onClick={() => onDelete?.(review)}
              disabled={isDeleting}
              className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-rose-600 hover:text-rose-700 hover:bg-rose-50 rounded-lg border border-rose-200 transition-colors cursor-pointer"
              title="Xóa đánh giá"
            >
              {isDeleting ? (
                <span className="inline-block animate-spin border-2 border-rose-600 border-t-transparent w-3.5 h-3.5 rounded-full" />
              ) : (
                <span className="material-symbols-outlined text-[16px]">delete</span>
              )}
              <span>Xóa</span>
            </button>
          </div>
        )}
      </div>

      {/* Số sao */}
      <div className="flex items-center gap-1 mb-2.5">
        {[1, 2, 3, 4, 5].map((star) => (
          <span
            key={star}
            className={`material-symbols-outlined text-[18px] ${
              review.rating >= star ? 'text-amber-400' : 'text-slate-200'
            }`}
            style={{ fontVariationSettings: "'FILL' 1" }}
          >
            star
          </span>
        ))}
      </div>

      {/* Nội dung nhận xét */}
      {review.comment ? (
        <p className="text-sm text-slate-700 leading-relaxed whitespace-pre-line">
          {review.comment}
        </p>
      ) : (
        <p className="text-xs text-slate-400 italic">Không có nhận xét bằng văn bản</p>
      )}
    </div>
  );
};
