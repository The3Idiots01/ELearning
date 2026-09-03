import React from 'react';
import type { Category } from '../../../../types/category';
import type { CourseSummary } from '../../../../types/course';
import { CourseCard } from '../../components/CourseCard';

interface StudentCatalogPageProps {
  courses: CourseSummary[];
  categories: Category[];
  selectedCategoryId: number | null;
  onSelectCategory: (id: number | null) => void;
  selectedLevel: string;
  onSelectLevel: (level: string) => void;
  searchQuery: string;
  onResetFilters: () => void;
  isLoading: boolean;
  onSelectCourse: (course: CourseSummary) => void;
  enrolledCourseIds: Set<number>;
}

export const StudentCatalogPage: React.FC<StudentCatalogPageProps> = ({
  courses,
  categories,
  selectedCategoryId,
  onSelectCategory,
  selectedLevel,
  onSelectLevel,
  onResetFilters,
  isLoading,
  onSelectCourse,
  enrolledCourseIds
}) => {
  return (
    <div className="min-h-screen bg-background pb-20 flex-1">
      {/* Hero Banner Section (Horizon Minimalist Navy Gradient) */}
      <section className="bg-gradient-to-r from-primary via-primary-container to-secondary text-white py-14 px-4 sm:px-6 lg:px-8 relative overflow-hidden shadow-inner">
        <div className="absolute inset-0 opacity-10 bg-[radial-gradient(#fff_1px,transparent_1px)] [background-size:20px_20px]" />

        <div className="max-w-7xl mx-auto relative z-10">
          <div className="max-w-3xl">
            <span className="inline-flex items-center gap-2 bg-white/15 border border-white/20 backdrop-blur-md text-white text-xs font-bold px-3.5 py-1.5 rounded-full mb-4">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              Nền tảng Học trực tuyến Chuẩn Quốc tế
            </span>
            <h1 className="text-3xl sm:text-5xl font-black tracking-tight leading-tight mb-4 font-display">
              Nâng tầm kỹ năng cùng chuyên gia hàng đầu
            </h1>
            <p className="text-white/90 text-sm sm:text-base mb-2 leading-relaxed">
              Khám phá hàng loạt khóa học lập trình, thiết kế, công nghệ thông tin và quản trị thực chiến được kiểm duyệt chất lượng cao.
            </p>
          </div>
        </div>
      </section>

      {/* Main Filter & Course Grid Container */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-8">
        
        {/* Filter Toolbar Card */}
        <div className="bg-surface-container-lowest rounded-2xl p-4 shadow-sm border border-outline-variant/70 mb-8 flex flex-col md:flex-row items-center justify-between gap-4">
          
          {/* Category Chips Scroll */}
          <div className="flex items-center gap-2 overflow-x-auto w-full md:w-auto pb-2 md:pb-0 scrollbar-none">
            <button
              onClick={() => onSelectCategory(null)}
              className={`px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
                selectedCategoryId === null
                  ? 'bg-primary text-white shadow-md shadow-primary/20'
                  : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container'
              }`}
            >
              Tất cả danh mục
            </button>

            {categories.map((cat) => (
              <button
                key={cat.id}
                onClick={() => onSelectCategory(cat.id)}
                className={`px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
                  selectedCategoryId === cat.id
                    ? 'bg-primary text-white shadow-md shadow-primary/20'
                    : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container'
                }`}
              >
                {cat.name}
              </button>
            ))}
          </div>

          {/* Level Filter Dropdown */}
          <div className="flex items-center gap-3 w-full md:w-auto justify-end border-t md:border-t-0 pt-3 md:pt-0 border-outline-variant/40 shrink-0">
            <span className="text-xs font-bold text-on-surface-variant whitespace-nowrap">
              Cấp độ:
            </span>
            <select
              value={selectedLevel}
              onChange={(e) => onSelectLevel(e.target.value)}
              className="bg-surface-container-low border border-outline-variant/70 text-on-surface text-xs font-bold rounded-xl px-3 py-2 focus:outline-none focus:border-primary"
            >
              <option value="ALL">Mọi cấp độ</option>
              <option value="BEGINNER">Cơ bản (Beginner)</option>
              <option value="INTERMEDIATE">Trung cấp (Intermediate)</option>
              <option value="ADVANCED">Nâng cao (Expert)</option>
            </select>
          </div>

        </div>

        {/* Results Header */}
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-xl font-black text-slate-900 font-display m-0">
            {selectedCategoryId ? 'Khóa học theo danh mục' : 'Tất cả khóa học nổi bật'}
          </h2>
          <span className="text-xs font-bold text-on-surface-variant bg-surface-container-low px-3 py-1 rounded-full border border-outline-variant/50">
            Tìm thấy {courses.length} khóa học
          </span>
        </div>

        {/* Course Grid */}
        {isLoading ? (
          <div className="text-center py-20 bg-surface-container-lowest border border-outline-variant/60 rounded-3xl">
            <span className="inline-block animate-spin border-4 border-primary border-t-transparent w-10 h-10 rounded-full" />
            <p className="text-on-surface-variant font-bold text-xs mt-4">
              Đang tải danh sách khóa học...
            </p>
          </div>
        ) : courses.length === 0 ? (
          <div className="text-center py-16 bg-surface-container-lowest border border-outline-variant/60 rounded-3xl p-8 max-w-lg mx-auto space-y-4">
            <span className="material-symbols-outlined text-[64px] text-outline">search_off</span>
            <h3 className="text-lg font-bold text-slate-900 m-0 font-display">
              Không tìm thấy khóa học phù hợp
            </h3>
            <p className="text-xs text-slate-500 m-0">
              Hãy thử thay đổi từ khóa tìm kiếm hoặc điều chỉnh bộ lọc danh mục.
            </p>
            <button
              onClick={onResetFilters}
              className="bg-primary hover:bg-primary/90 text-white text-xs font-bold px-5 py-2.5 rounded-full transition-all cursor-pointer"
            >
              Xóa bộ lọc tìm kiếm
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {courses.map((course) => (
              <CourseCard
                key={course.id}
                course={course}
                isEnrolled={enrolledCourseIds.has(course.id)}
                onSelect={onSelectCourse}
              />
            ))}
          </div>
        )}

      </div>
    </div>
  );
};
