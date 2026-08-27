import React from 'react';
import type { CourseSummary } from '../../../types/course';
import { LevelBadge } from '../../../components/common/Badge';
import { formatCurrencyVND } from '../../../lib/formatters';

interface CourseCardProps {
  course: CourseSummary;
  isEnrolled?: boolean;
  progress?: number;
  onSelect: (course: CourseSummary) => void;
}

export const CourseCard: React.FC<CourseCardProps> = ({
  course,
  isEnrolled,
  progress,
  onSelect
}) => {
  const defaultThumbnail =
    'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=600&auto=format&fit=crop&q=60';

  return (
    <div
      onClick={() => onSelect(course)}
      className="bg-surface-container-lowest border border-outline-variant/70 rounded-2xl overflow-hidden hover:shadow-xl hover:-translate-y-1 transition-all duration-300 flex flex-col h-full cursor-pointer group"
    >
      {/* Thumbnail */}
      <div className="relative h-44 w-full bg-surface-container-low overflow-hidden">
        <img
          src={course.thumbnailUrl || defaultThumbnail}
          alt={course.title}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
          onError={(e) => {
            (e.target as HTMLImageElement).src = defaultThumbnail;
          }}
        />
        <div className="absolute top-3 left-3 flex items-center gap-1.5 flex-wrap">
          <LevelBadge level={course.level} />
          {(course.categoryName || course.category?.name) && (
            <span className="bg-white/95 backdrop-blur-md text-slate-800 text-[10px] font-bold px-2 py-0.5 rounded-md shadow-sm">
              {course.categoryName || course.category?.name}
            </span>
          )}
        </div>

        {isEnrolled && (
          <div className="absolute top-3 right-3 bg-emerald-600 text-white text-[10px] font-extrabold px-2.5 py-1 rounded-full shadow-md flex items-center gap-1">
            <span className="material-symbols-outlined text-[13px]">check_circle</span>
            <span>Đã đăng ký</span>
          </div>
        )}
      </div>

      {/* Body Content */}
      <div className="p-5 flex flex-col flex-1">
        <h3 className="font-bold text-sm sm:text-base text-slate-900 group-hover:text-primary transition-colors line-clamp-2 m-0 mb-2 leading-snug font-display">
          {course.title}
        </h3>

        <p className="text-xs text-slate-500 line-clamp-2 m-0 mb-4 leading-relaxed">
          {course.subtitle || course.description || 'Khóa học cung cấp kiến thức thực chiến và chuyên sâu.'}
        </p>

        {/* Progress Bar (if enrolled) */}
        {isEnrolled && progress !== undefined && (
          <div className="mb-4 pt-1">
            <div className="flex justify-between items-center text-xs font-semibold text-slate-600 mb-1">
              <span>Tiến độ học tập</span>
              <span className="text-primary font-bold">{progress}%</span>
            </div>
            <div className="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
              <div
                className="bg-primary h-full rounded-full transition-all duration-500"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>
        )}

        {/* Card Footer */}
        <div className="mt-auto pt-3 border-t border-slate-100 flex items-center justify-between">
          <div className="flex items-center gap-2 overflow-hidden">
            <div className="w-6 h-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-[10px] font-bold shrink-0">
              {course.lecturerName ? course.lecturerName.substring(0, 1) : 'L'}
            </div>
            <span className="text-xs text-slate-600 font-medium truncate max-w-[120px]">
              {course.lecturerName || 'Giảng viên Learnova'}
            </span>
          </div>

          <div className="text-right shrink-0">
            <span className="font-extrabold text-sm text-primary font-display">
              {formatCurrencyVND(course.price)}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
