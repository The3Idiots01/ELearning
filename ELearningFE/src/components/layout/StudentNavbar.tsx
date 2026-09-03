import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../app/context/AuthContext';

interface StudentNavbarProps {
  searchQuery: string;
  onSearchChange: (query: string) => void;
  onSearchSubmit: (e: React.FormEvent) => void;
}

export const StudentNavbar: React.FC<StudentNavbarProps> = ({
  searchQuery,
  onSearchChange,
  onSearchSubmit
}) => {
  const { currentUser, isAuthenticated, toggleAppMode, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const isCatalogActive = location.pathname.startsWith('/courses');
  const isMyCoursesActive = location.pathname.startsWith('/my-courses') || location.pathname.startsWith('/learning');

  return (
    <header className="sticky top-0 z-40 bg-surface-container-lowest/95 backdrop-blur-md border-b border-outline-variant/70 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 gap-4">

          {/* Logo & Navigation */}
          <div className="flex items-center gap-8">
            <button
              onClick={() => navigate('/courses')}
              className="flex items-center gap-2.5 text-left focus:outline-none cursor-pointer group"
            >
              <div className="bg-gradient-to-tr from-primary to-primary-container text-white w-9 h-9 rounded-xl flex items-center justify-center font-display font-black text-xl shadow-md shadow-primary/20 group-hover:scale-105 transition-transform">
                L
              </div>
              <div>
                <span className="font-display font-black text-xl text-primary tracking-tight block leading-none">
                  Learnova
                </span>
                <span className="text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
                  Học viên Portal
                </span>
              </div>
            </button>

            <nav className="hidden md:flex items-center gap-1">
              <button
                onClick={() => navigate('/courses')}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-2 cursor-pointer ${
                  isCatalogActive
                    ? 'bg-primary-container/10 text-primary'
                    : 'text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">grid_view</span>
                <span>Khám phá khóa học</span>
              </button>

              <button
                onClick={() => navigate('/my-courses')}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-2 cursor-pointer ${
                  isMyCoursesActive
                    ? 'bg-primary-container/10 text-primary'
                    : 'text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">school</span>
                <span>Khóa học của tôi</span>
              </button>
            </nav>
          </div>

          {/* Realtime Search Bar */}
          <div className="flex-1 max-w-md hidden sm:block">
            <form onSubmit={onSearchSubmit} className="relative">
              <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-outline text-[18px]">
                search
              </span>
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => onSearchChange(e.target.value)}
                placeholder="Tìm kiếm khóa học, kỹ năng, giảng viên..."
                className="w-full pl-10 pr-4 py-2 bg-surface-container-low border border-outline-variant/60 rounded-full text-xs text-on-surface placeholder:text-outline focus:bg-white focus:border-primary focus:outline-none transition-all"
              />
            </form>
          </div>

          {/* Right Actions & Profile */}
          <div className="flex items-center gap-3">

            {/* Role switch action */}
            <button
              onClick={() => {
                toggleAppMode();
                navigate('/instructor/courses');
              }}
              title="Chuyển sang Studio Giảng viên"
              className="bg-primary/10 hover:bg-primary/15 text-primary text-xs font-bold px-3 py-1.5 rounded-full transition-all flex items-center gap-1.5 cursor-pointer"
            >
              <span className="material-symbols-outlined text-[16px]">school</span>
              <span className="hidden lg:inline">Giảng viên Studio</span>
              <span className="lg:hidden">Studio</span>
            </button>

            {/* Profile Avatar / User info, or Login/Register when signed out */}
            {isAuthenticated ? (
              <div className="flex items-center gap-2 pl-2 border-l border-outline-variant/60">
                <button
                  type="button"
                  onClick={() => navigate('/profile')}
                  className="flex items-center gap-2 text-left hover:opacity-80 transition-opacity cursor-pointer group"
                  title="Xem và chỉnh sửa hồ sơ cá nhân"
                >
                  <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-primary to-secondary text-white font-bold text-xs flex items-center justify-center shadow-sm overflow-hidden shrink-0">
                    {currentUser?.avatarUrl ? (
                      <img src={currentUser.avatarUrl} alt={currentUser.fullName} className="w-full h-full object-cover" />
                    ) : (
                      currentUser?.fullName ? currentUser.fullName.substring(0, 2).toUpperCase() : 'HV'
                    )}
                  </div>
                  <div className="hidden xl:block text-left">
                    <p className="text-xs font-bold text-on-surface truncate max-w-[120px] m-0 group-hover:text-primary transition-colors">
                      {currentUser?.fullName || 'Học viên'}
                    </p>
                    <p className="text-[10px] text-on-surface-variant m-0">Hồ sơ cá nhân</p>
                  </div>
                </button>

                <button
                  onClick={logout}
                  title="Đăng xuất"
                  className="p-1.5 text-outline hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
                >
                  <span className="material-symbols-outlined text-[18px]">logout</span>
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-2 pl-2 border-l border-outline-variant/60">
                <button
                  type="button"
                  onClick={() => navigate('/login')}
                  className="text-primary hover:bg-primary/10 font-bold text-xs px-3.5 py-1.5 rounded-full transition-colors cursor-pointer"
                >
                  Đăng nhập
                </button>
                <button
                  type="button"
                  onClick={() => navigate('/register')}
                  className="bg-primary hover:bg-primary-container text-white font-bold text-xs px-3.5 py-1.5 rounded-full transition-colors cursor-pointer"
                >
                  Đăng ký
                </button>
              </div>
            )}

          </div>

        </div>
      </div>
    </header>
  );
};
