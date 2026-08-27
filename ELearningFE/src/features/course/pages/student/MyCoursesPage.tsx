import React, { useState } from 'react';
import type { EnrolledCourse } from '../../../../types/course';

interface MyCoursesPageProps {
  enrolledCourses: EnrolledCourse[];
  isLoading: boolean;
  onGoToLearning: (courseId: number) => void;
  onExploreMore: () => void;
}

export const MyCoursesPage: React.FC<MyCoursesPageProps> = ({
  enrolledCourses,
  isLoading,
  onGoToLearning,
  onExploreMore
}) => {
  const [filterTab, setFilterTab] = useState<'ALL' | 'ACTIVE' | 'COMPLETED'>('ALL');

  const filtered = enrolledCourses.filter((c) => {
    if (filterTab === 'ACTIVE') return c.status === 'ACTIVE';
    if (filterTab === 'COMPLETED') return c.status === 'COMPLETED';
    return true;
  });

  const defaultThumbnail =
    'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=600&auto=format&fit=crop&q=60';

  return (
    <div className="min-h-screen bg-background pb-20 flex-1">
      {/* Top Header */}
      <div className="bg-gradient-to-r from-primary via-primary-container to-secondary text-white py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <span className="text-white/80 text-xs font-bold uppercase tracking-wider block mb-1">
              Góc học tập cá nhân
            </span>
            <h1 className="text-3xl font-black tracking-tight m-0 font-display">
              Khóa học của tôi
            </h1>
            <p className="text-white/90 text-xs sm:text-sm mt-1 m-0">
              Quản lý tiến trình học tập và tiếp tục các bài giảng chưa hoàn thành.
            </p>
          </div>

          <button
            onClick={onExploreMore}
            className="bg-white/15 hover:bg-white/25 text-white text-xs font-bold px-4 py-2.5 rounded-full border border-white/20 transition-all backdrop-blur-md self-start md:self-auto flex items-center gap-1.5 cursor-pointer"
          >
            <span className="material-symbols-outlined text-[18px]">add_circle</span>
            <span>Khám phá khóa học mới</span>
          </button>
        </div>
      </div>

      {/* Main Container */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 -mt-6">
        
        {/* Filter Tabs */}
        <div className="bg-surface-container-lowest rounded-2xl p-1.5 shadow-sm border border-outline-variant/70 mb-8 inline-flex items-center gap-1">
          <button
            onClick={() => setFilterTab('ALL')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              filterTab === 'ALL'
                ? 'bg-primary text-white shadow-md shadow-primary/20'
                : 'text-on-surface-variant hover:bg-surface-container-low'
            }`}
          >
            Tất cả ({enrolledCourses.length})
          </button>

          <button
            onClick={() => setFilterTab('ACTIVE')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              filterTab === 'ACTIVE'
                ? 'bg-primary text-white shadow-md shadow-primary/20'
                : 'text-on-surface-variant hover:bg-surface-container-low'
            }`}
          >
            Đang học ({enrolledCourses.filter((c) => c.status === 'ACTIVE').length})
          </button>

          <button
            onClick={() => setFilterTab('COMPLETED')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              filterTab === 'COMPLETED'
                ? 'bg-primary text-white shadow-md shadow-primary/20'
                : 'text-on-surface-variant hover:bg-surface-container-low'
            }`}
          >
            Đã hoàn thành ({enrolledCourses.filter((c) => c.status === 'COMPLETED').length})
          </button>
        </div>

        {/* Content Area */}
        {isLoading ? (
          <div className="text-center py-20 bg-surface-container-lowest border border-outline-variant/60 rounded-3xl">
            <span className="inline-block animate-spin border-4 border-primary border-t-transparent w-10 h-10 rounded-full" />
            <p className="text-on-surface-variant font-bold text-xs mt-4">
              Đang tải danh sách khóa học của bạn...
            </p>
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-16 bg-surface-container-lowest border border-outline-variant/60 rounded-3xl p-8 max-w-lg mx-auto space-y-4">
            <span className="material-symbols-outlined text-[64px] text-outline">menu_book</span>
            <h3 className="text-lg font-bold text-slate-900 m-0 font-display">
              Bạn chưa có khóa học nào trong mục này
            </h3>
            <p className="text-xs text-slate-500 m-0">
              Hãy truy cập danh mục để tìm kiếm và đăng ký khóa học mong muốn.
            </p>
            <button
              onClick={onExploreMore}
              className="bg-primary hover:bg-primary/90 text-white text-xs font-bold px-5 py-2.5 rounded-full transition-all cursor-pointer"
            >
              Khám phá danh mục khóa học
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filtered.map((item) => (
              <div
                key={item.enrollmentId}
                className="bg-surface-container-lowest border border-outline-variant/70 rounded-2xl overflow-hidden hover:shadow-xl transition-all duration-300 flex flex-col h-full group"
              >
                {/* Thumbnail Header */}
                <div className="relative h-44 w-full bg-surface-container-low overflow-hidden">
                  <img
                    src={item.courseThumbnailUrl || defaultThumbnail}
                    alt={item.courseTitle}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    onError={(e) => {
                      (e.target as HTMLImageElement).src = defaultThumbnail;
                    }}
                  />
                  <div className="absolute top-3 right-3">
                    {item.status === 'COMPLETED' ? (
                      <span className="bg-emerald-600 text-white text-[10px] font-extrabold px-3 py-1 rounded-full shadow-md flex items-center gap-1">
                        <span className="material-symbols-outlined text-[14px]">workspace_premium</span>
                        <span>Đã hoàn thành</span>
                      </span>
                    ) : (
                      <span className="bg-primary text-white text-[10px] font-extrabold px-3 py-1 rounded-full shadow-md flex items-center gap-1">
                        <span className="material-symbols-outlined text-[14px]">play_circle</span>
                        <span>Đang học</span>
                      </span>
                    )}
                  </div>
                </div>

                {/* Content */}
                <div className="p-5 flex flex-col flex-1 space-y-4">
                  <div>
                    <span className="text-[10px] font-bold text-primary uppercase tracking-wider block mb-1">
                      {item.categoryName || 'Khóa học'}
                    </span>
                    <h3 className="font-bold text-sm sm:text-base text-slate-900 line-clamp-2 leading-snug m-0 font-display">
                      {item.courseTitle}
                    </h3>
                  </div>

                  {/* Progress Bar */}
                  <div className="space-y-1.5 pt-2 border-t border-slate-100 mt-auto">
                    <div className="flex justify-between items-center text-xs">
                      <span className="font-semibold text-slate-500">Tiến độ</span>
                      <strong className="text-primary font-bold">{item.progress}%</strong>
                    </div>
                    <div className="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all duration-500 ${
                          item.status === 'COMPLETED' ? 'bg-emerald-500' : 'bg-primary'
                        }`}
                        style={{ width: `${item.progress}%` }}
                      />
                    </div>
                  </div>

                  {/* Action Button */}
                  <button
                    onClick={() => onGoToLearning(item.courseId)}
                    className="w-full bg-primary/10 hover:bg-primary text-primary hover:text-white font-extrabold py-2.5 rounded-xl text-xs transition-all flex items-center justify-center gap-2 cursor-pointer active:scale-95"
                  >
                    <span className="material-symbols-outlined text-[18px]">play_arrow</span>
                    <span>{item.status === 'COMPLETED' ? 'Ôn lại bài học' : 'Tiếp tục bài học'}</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

      </div>
    </div>
  );
};
