import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../app/context/AuthContext';
import { recommendationApi } from '../api/recommendationApi';
import type { RecommendedCourse } from '../types/recommendation';
import type { CourseSummary } from '../../../types/course';
import { RecommendationCourseCard } from './RecommendationCourseCard';

interface RelatedCoursesSectionProps {
  courseId: number;
  onSelectCourse: (course: CourseSummary) => void;
}

export const RelatedCoursesSection: React.FC<RelatedCoursesSectionProps> = ({
  courseId,
  onSelectCourse
}) => {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [items, setItems] = useState<RecommendedCourse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    if (!courseId || !isAuthenticated) {
      setIsLoading(false);
      return;
    }
    let isMounted = true;
    setIsLoading(true);

    recommendationApi
      .getContinuousRecommendations({ courseId })
      .then((res) => {
        if (isMounted && res?.items) {
          setItems(res.items);
        }
      })
      .catch(() => {
        // Silently fail without breaking page
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [courseId, isAuthenticated]);

  if (!isAuthenticated) {
    return (
      <section className="mt-16 pt-12 border-t border-slate-200">
        <div className="bg-gradient-to-r from-slate-900 to-indigo-950 text-white rounded-3xl p-8 border border-indigo-500/30 flex flex-col md:flex-row items-center justify-between gap-6 shadow-lg">
          <div className="space-y-2 text-center md:text-left">
            <div className="inline-flex items-center gap-2 bg-indigo-500/20 text-indigo-300 text-xs font-bold px-3 py-1 rounded-full">
              <span className="material-symbols-outlined text-[16px] text-amber-400">lock</span>
              <span>Đề Xuất Cá Nhân Hóa Dành Riêng Cho Bạn</span>
            </div>
            <h3 className="text-xl font-bold font-display m-0 text-white">
              Đăng nhập để xem các khóa học đề xuất tiếp theo
            </h3>
            <p className="text-xs sm:text-sm text-slate-300 m-0">
              Hệ thống sẽ phân tích lịch sử học tập, trình độ và chuyên mục để gợi ý các bước tiến tối ưu nhất cho bạn.
            </p>
          </div>
          <button
            type="button"
            onClick={() => navigate('/login')}
            className="shrink-0 bg-primary hover:bg-primary/90 text-white font-bold text-xs sm:text-sm px-6 py-3 rounded-2xl transition-all shadow-md shadow-primary/30 cursor-pointer"
          >
            Đăng Nhập Ngay
          </button>
        </div>
      </section>
    );
  }

  if (!isLoading && items.length === 0) {
    return null;
  }

  return (
    <section className="mt-16 pt-12 border-t border-slate-200">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-8">
        <div className="space-y-1.5">
          <div className="inline-flex items-center gap-1.5 bg-primary/10 text-primary text-[11px] font-bold px-3 py-1 rounded-full uppercase tracking-wider">
            <span className="material-symbols-outlined text-[15px]">recommend</span>
            <span>Hệ Thống Đề Xuất Thông Minh</span>
          </div>
          <h2 className="text-xl sm:text-2xl font-black text-slate-900 font-display m-0">
            Khóa học liên quan & Tiếp theo
          </h2>
          <p className="text-xs sm:text-sm text-slate-500 m-0">
            Đề xuất liên tục Top 5 khóa học phù hợp nhất dựa trên chuyên mục, cấp độ và phản hồi từ học viên.
          </p>
        </div>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
          {[1, 2, 3, 4, 5].map((idx) => (
            <div
              key={idx}
              className="bg-slate-100 rounded-2xl h-64 animate-pulse border border-slate-200"
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
