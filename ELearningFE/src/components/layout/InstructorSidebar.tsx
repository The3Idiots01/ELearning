import React from 'react';
import { useAuth } from '../../app/context/AuthContext';

interface InstructorSidebarProps {
  activeView: 'courses' | 'course-settings' | 'curriculum';
  onNavigate: (view: 'courses') => void;
  selectedCourseTitle?: string;
}

export const InstructorSidebar: React.FC<InstructorSidebarProps> = ({
  activeView,
  onNavigate,
  selectedCourseTitle
}) => {
  const { currentUser, toggleAppMode, logout } = useAuth();

  return (
    <aside className="bg-surface-container-lowest border-r border-outline-variant/70 w-64 flex flex-col py-6 px-4 shrink-0 h-full">
      {/* Studio Brand Header */}
      <div className="mb-8 px-2 flex items-center gap-3">
        <div className="bg-gradient-to-tr from-primary to-primary-container text-white w-10 h-10 rounded-xl flex items-center justify-center font-display font-black text-xl shadow-md shadow-primary/20">
          L
        </div>
        <div>
          <h2 className="font-display font-black text-lg text-primary leading-tight m-0">Learnova</h2>
          <p className="text-[10px] text-on-surface-variant font-bold uppercase tracking-wider m-0">
            Instructor Studio
          </p>
        </div>
      </div>

      {/* Main Navigation */}
      <nav className="flex-1 space-y-1.5">
        <button
          onClick={() => onNavigate('courses')}
          className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl transition-all font-bold text-xs cursor-pointer ${
            activeView === 'courses'
              ? 'bg-primary text-white shadow-sm shadow-primary/30'
              : 'text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface'
          }`}
        >
          <span className="material-symbols-outlined text-[20px]">video_library</span>
          <span>Khóa học của tôi</span>
        </button>

        {selectedCourseTitle && (
          <div className="pt-4 mt-4 border-t border-outline-variant/40 space-y-1">
            <div className="px-3 pb-1">
              <span className="text-[10px] font-extrabold uppercase tracking-wider text-outline">
                Đang biên soạn
              </span>
              <p className="text-xs font-bold text-on-surface truncate m-0 mt-0.5" title={selectedCourseTitle}>
                {selectedCourseTitle}
              </p>
            </div>

            <div
              className={`w-full flex items-center gap-3 px-3.5 py-2 rounded-xl text-xs font-semibold ${
                activeView === 'course-settings'
                  ? 'bg-primary-container/15 text-primary font-bold'
                  : 'text-on-surface-variant'
              }`}
            >
              <span className="material-symbols-outlined text-[18px]">tune</span>
              <span>1. Cài đặt & Giá (US-05)</span>
            </div>

            <div
              className={`w-full flex items-center gap-3 px-3.5 py-2 rounded-xl text-xs font-semibold ${
                activeView === 'curriculum'
                  ? 'bg-primary-container/15 text-primary font-bold'
                  : 'text-on-surface-variant'
              }`}
            >
              <span className="material-symbols-outlined text-[18px]">menu_book</span>
              <span>2. Giáo trình bài học (US-06)</span>
            </div>
          </div>
        )}
      </nav>

      {/* User info & Studio Footer */}
      <div className="pt-4 border-t border-outline-variant/60 flex flex-col gap-3">
        <button
          onClick={toggleAppMode}
          className="w-full bg-surface-container-low hover:bg-surface-container text-on-surface font-bold text-xs py-2 px-3 rounded-xl transition-colors flex items-center justify-center gap-1.5 cursor-pointer border border-outline-variant/60"
        >
          <span className="material-symbols-outlined text-[16px]">visibility</span>
          <span>Xem góc Học viên</span>
        </button>

        <div className="flex items-center justify-between pt-2">
          <div className="flex items-center gap-2.5 overflow-hidden">
            <div className="w-8 h-8 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-xs uppercase shrink-0">
              {currentUser ? currentUser.fullName.substring(0, 2) : 'GV'}
            </div>
            <div className="overflow-hidden">
              <p className="font-bold text-xs text-on-surface truncate m-0">
                {currentUser ? currentUser.fullName : 'Giảng viên'}
              </p>
              <p className="text-[10px] text-on-surface-variant m-0">Giảng viên Studio</p>
            </div>
          </div>

          <button
            onClick={logout}
            title="Đăng xuất"
            className="p-1.5 text-outline hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
          >
            <span className="material-symbols-outlined text-[18px]">logout</span>
          </button>
        </div>
      </div>
    </aside>
  );
};
