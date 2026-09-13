import React from 'react';
import type { RecommendedCourse } from '../types/recommendation';
import type { CourseSummary } from '../../../types/course';

interface RecommendationCourseCardProps {
  item: RecommendedCourse;
  onSelect: (course: CourseSummary) => void;
}

export const RecommendationCourseCard: React.FC<RecommendationCourseCardProps> = ({
  item,
  onSelect
}) => {
  const { course, matchScore, recommendationReason, recommendationType } = item;

  const defaultThumbnail =
    'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800&auto=format&fit=crop&q=60';

  const formatPrice = (price?: number) => {
    if (!price || price <= 0) return 'Miễn phí';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const matchPercent = Math.round((matchScore || 0.8) * 100);

  const isAi = recommendationType === 'GEMINI_AI';

  return (
    <div
      onClick={() => onSelect(course)}
      className="group bg-surface-container-lowest rounded-2xl border border-outline-variant/70 hover:border-primary/50 shadow-sm hover:shadow-xl transition-all duration-300 flex flex-col overflow-hidden cursor-pointer transform hover:-translate-y-1"
    >
      {/* Thumbnail & Match Score Floating Badge */}
      <div className="relative aspect-video w-full overflow-hidden bg-slate-100">
        <img
          src={course.thumbnailUrl || defaultThumbnail}
          alt={course.title}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
          loading="lazy"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-black/20 opacity-80 group-hover:opacity-90 transition-opacity" />

        {/* Top Badges */}
        <div className="absolute top-2.5 left-2.5 right-2.5 flex items-center justify-between gap-1.5">
          {/* Recommendation Type Badge */}
          {isAi ? (
            <span className="inline-flex items-center gap-1 bg-gradient-to-r from-violet-600 to-indigo-600 text-white text-[10px] font-black px-2.5 py-1 rounded-full shadow-md backdrop-blur-md">
              <span className="material-symbols-outlined text-[12px] animate-pulse">auto_awesome</span>
              <span>AI Đề Xuất</span>
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 bg-emerald-600/90 text-white text-[10px] font-bold px-2 py-0.5 rounded-full shadow-md backdrop-blur-md">
              <span className="material-symbols-outlined text-[12px]">trending_up</span>
              <span>Phổ biến</span>
            </span>
          )}

          {/* Match Score */}
          <span className="inline-flex items-center gap-1 bg-white/95 text-primary text-[11px] font-black px-2.5 py-1 rounded-full shadow-md">
            <span className="text-emerald-600">●</span>
            <span>{matchPercent}% Match</span>
          </span>
        </div>

        {/* Category at bottom of thumbnail */}
        <div className="absolute bottom-2 left-2.5">
          <span className="text-white text-[11px] font-bold drop-shadow-md">
            {course.categoryName || 'Khóa học'}
          </span>
        </div>
      </div>

      {/* Content */}
      <div className="p-4 flex-1 flex flex-col justify-between space-y-3">
        <div>
          <h4 className="text-sm font-bold text-on-surface line-clamp-2 leading-snug group-hover:text-primary transition-colors font-display m-0">
            {course.title}
          </h4>

          {course.subtitle && (
            <p className="text-xs text-on-surface-variant line-clamp-1 mt-1 m-0">
              {course.subtitle}
            </p>
          )}

          {/* Recommendation Reason Box */}
          {recommendationReason && (
            <div className="mt-2.5 bg-primary-container/10 border border-primary/20 rounded-xl p-2.5 text-xs text-on-surface-variant flex items-start gap-1.5">
              <span className="material-symbols-outlined text-primary text-[16px] shrink-0 mt-0.5">
                psychology
              </span>
              <p className="m-0 text-[11px] leading-relaxed text-slate-700 italic">
                &ldquo;{recommendationReason}&rdquo;
              </p>
            </div>
          )}
        </div>

        {/* Footer Meta */}
        <div className="pt-2 border-t border-outline-variant/40 flex items-center justify-between text-xs">
          <div className="flex items-center gap-1 text-amber-500 font-bold">
            <span className="material-symbols-outlined text-[15px]" style={{ fontVariationSettings: "'FILL' 1" }}>
              star
            </span>
            <span>{course.ratingAvg && course.ratingAvg > 0 ? course.ratingAvg : '4.8'}</span>
            <span className="text-on-surface-variant font-normal text-[11px]">
              ({course.ratingCount || 10})
            </span>
          </div>

          <div className="text-right font-extrabold text-primary font-display text-sm">
            {formatPrice(course.price)}
          </div>
        </div>
      </div>
    </div>
  );
};
