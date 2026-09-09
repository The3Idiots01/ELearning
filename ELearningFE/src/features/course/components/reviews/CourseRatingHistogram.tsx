import React from 'react';
import type { CourseReviewSummary } from '../../../../types/review';

interface CourseRatingHistogramProps {
  summary: CourseReviewSummary;
  activeFilter?: number;
  onSelectRatingFilter?: (rating: number | undefined) => void;
}

export const CourseRatingHistogram: React.FC<CourseRatingHistogramProps> = ({
  summary,
  activeFilter,
  onSelectRatingFilter
}) => {
  const avg = Number(summary.ratingAvg || 0).toFixed(1);
  const total = summary.totalReviews || 0;

  const renderStars = (rating: number) => {
    return (
      <div className="flex items-center justify-center gap-1 text-amber-400">
        {[1, 2, 3, 4, 5].map((star) => (
          <span
            key={star}
            className="material-symbols-outlined text-[22px]"
            style={{ fontVariationSettings: "'FILL' 1" }}
          >
            {rating >= star ? 'star' : rating >= star - 0.5 ? 'star_half' : 'star'}
          </span>
        ))}
      </div>
    );
  };

  return (
    <div className="bg-surface-container-lowest border border-slate-200/80 rounded-2xl p-6 shadow-sm w-full">
      <div className="flex flex-col sm:flex-row items-center gap-6 sm:gap-8">
        {/* Điểm tổng quan */}
        <div className="w-full sm:w-48 flex flex-col items-center justify-center text-center pb-5 sm:pb-0 sm:pr-6 border-b sm:border-b-0 sm:border-r border-slate-100 shrink-0">
          <div className="text-5xl font-black text-slate-900 tracking-tight mb-2 font-display">
            {avg}
          </div>
          {renderStars(Number(summary.ratingAvg || 0))}
          <div className="text-xs font-semibold text-slate-500 mt-2">
            {total > 0 ? (
              <>Dựa trên <strong className="text-slate-800">{total.toLocaleString()}</strong> đánh giá</>
            ) : (
              'Chưa có đánh giá'
            )}
          </div>
          {activeFilter && (
            <button
              type="button"
              onClick={() => onSelectRatingFilter?.(undefined)}
              className="mt-3 text-xs text-primary font-bold hover:underline inline-flex items-center gap-1 cursor-pointer bg-primary/5 px-2.5 py-1 rounded-full"
            >
              <span>Lọc: {activeFilter} sao</span>
              <span className="material-symbols-outlined text-[14px]">close</span>
            </button>
          )}
        </div>

        {/* Thanh phân bổ tỷ lệ các sao */}
        <div className="flex-1 w-full space-y-2">
          {[5, 4, 3, 2, 1].map((star) => {
            const count = summary.ratingCounts?.[star] || 0;
            const percentage = summary.ratingPercentages?.[star] || 0;
            const isSelected = activeFilter === star;

            return (
              <button
                key={star}
                type="button"
                onClick={() => onSelectRatingFilter?.(isSelected ? undefined : star)}
                className={`w-full flex items-center gap-3 text-xs py-1.5 px-2.5 rounded-xl transition-all text-left cursor-pointer group ${
                  isSelected
                    ? 'bg-amber-50/80 ring-1 ring-amber-300 font-bold'
                    : 'hover:bg-slate-50'
                }`}
              >
                <div className="flex items-center gap-1 w-16 shrink-0 text-slate-700 font-semibold whitespace-nowrap">
                  <span>{star} sao</span>
                  <span
                    className="material-symbols-outlined text-[14px] text-amber-400 group-hover:scale-110 transition-transform"
                    style={{ fontVariationSettings: "'FILL' 1" }}
                  >
                    star
                  </span>
                </div>

                <div className="flex-1 bg-slate-100 h-2.5 rounded-full overflow-hidden relative">
                  <div
                    className="bg-amber-400 h-full rounded-full transition-all duration-500"
                    style={{ width: `${percentage}%` }}
                  />
                </div>

                <div className="w-12 text-right text-slate-600 font-semibold shrink-0">
                  {percentage}%
                </div>
                <div className="w-14 text-right text-slate-400 text-[11px] shrink-0">
                  ({count})
                </div>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};
