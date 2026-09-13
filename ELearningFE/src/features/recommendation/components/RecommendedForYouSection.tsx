import React, { useEffect, useState } from 'react';
import { useAuth } from '../../../app/context/AuthContext';
import { recommendationApi } from '../api/recommendationApi';
import type { RecommendedCourse } from '../types/recommendation';
import type { CourseSummary } from '../../../types/course';
import { RecommendationCourseCard } from './RecommendationCourseCard';

interface RecommendedForYouSectionProps {
  onSelectCourse: (course: CourseSummary) => void;
  selectedCategoryId?: number | null;
}

export const RecommendedForYouSection: React.FC<RecommendedForYouSectionProps> = ({
  onSelectCourse,
  selectedCategoryId
}) => {
  const { isAuthenticated } = useAuth();
  const [items, setItems] = useState<RecommendedCourse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    if (!isAuthenticated) {
      setItems([]);
      setIsLoading(false);
      return;
    }

    let isMounted = true;
    setIsLoading(true);

    recommendationApi
      .getContinuousRecommendations({ categoryId: selectedCategoryId || undefined })
      .then((res) => {
        if (isMounted && res?.items) {
          setItems(res.items);
        }
      })
      .catch(() => {
        // Silently fail if unauthenticated or error
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, selectedCategoryId]);

  // If user is not authenticated or there are no recommendations, do not render this section
  if (!isAuthenticated || (!isLoading && items.length === 0)) {
    return null;
  }

  return (
    <section className="mb-10 p-6 rounded-3xl bg-gradient-to-br from-indigo-50/70 via-surface-container-lowest to-purple-50/50 border border-indigo-200/60 shadow-sm">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-6">
        <div className="space-y-1">
          <div className="inline-flex items-center gap-1.5 bg-primary/10 text-primary text-[11px] font-extrabold px-3 py-1 rounded-full uppercase tracking-wider">
            <span className="material-symbols-outlined text-[15px]">auto_awesome</span>
            <span>Gợi Ý Dành Riêng Cho Bạn (Top 5)</span>
          </div>
          <h2 className="text-xl sm:text-2xl font-black text-slate-900 font-display m-0">
            Khóa học đề xuất cho bạn
          </h2>
          <p className="text-xs sm:text-sm text-slate-600 m-0">
            Dựa trên chuyên mục yêu thích, tiến độ kỹ năng và đánh giá cao nhất từ cộng đồng học viên.
          </p>
        </div>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
          {[1, 2, 3, 4, 5].map((idx) => (
            <div
              key={idx}
              className="bg-white/80 rounded-2xl h-64 animate-pulse border border-indigo-100"
            />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
          {items.map((item) => (
            <RecommendationCourseCard
              key={item.course.id}
              item={item}
              onSelect={onSelectCourse}
            />
          ))}
        </div>
      )}
    </section>
  );
};
