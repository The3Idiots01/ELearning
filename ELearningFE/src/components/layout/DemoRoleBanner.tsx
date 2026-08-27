import React, { useState } from 'react';
import { useAuth } from '../../app/context/AuthContext';
import { API_BASE_URL, apiClient } from '../../lib/apiClient';
import { Modal } from '../common/Modal';
import { useToast } from '../../app/context/ToastContext';

export const DemoRoleBanner: React.FC = () => {
  const { appMode, toggleAppMode, currentUser, token, login, logout, refreshProfile } = useAuth();
  const { showSuccess, showError } = useToast();

  const [showAuthModal, setShowAuthModal] = useState(false);
  const [email, setEmail] = useState('quangtienhoihop@gmail.com');
  const [password, setPassword] = useState('123456');
  const [customToken, setCustomToken] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      const response = await apiClient.post<any>(
        '/api/v1/auth/login',
        { email: email.trim(), password: password.trim() },
        { skipAuth: true }
      );

      const accessToken = response?.accessToken || response?.result?.accessToken;
      if (accessToken) {
        login(accessToken);
        await refreshProfile();
        showSuccess('Đăng nhập Giảng viên thành công!');
        setShowAuthModal(false);
      } else {
        showError('Không tìm thấy token trong phản hồi từ máy chủ.');
      }
    } catch (err: any) {
      showError(err.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại Email & Mật khẩu.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleApplyToken = async () => {
    if (!customToken.trim()) return;
    login(customToken.trim());
    await refreshProfile();
    showSuccess('Đã áp dụng Access Token JWT!');
    setShowAuthModal(false);
  };

  return (
    <>
      <div className="bg-slate-900 text-slate-200 px-4 sm:px-6 py-2 flex flex-col sm:flex-row justify-between items-center text-xs gap-2 z-50 border-b border-slate-800 shrink-0">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="font-extrabold uppercase tracking-wider bg-primary-container/80 text-white px-2.5 py-0.5 rounded-full text-[10px]">
            Learnova Platform
          </span>
          <span className="text-slate-400">
            API: <code className="text-indigo-300 font-mono text-[11px]">{API_BASE_URL}</code>
          </span>
          <span className="text-slate-500">|</span>
          <span>
            Vai trò: <strong className="text-emerald-400 font-bold uppercase">{appMode === 'STUDENT' ? 'Học viên (Portal)' : 'Giảng viên (Studio)'}</strong>
          </span>

          {token ? (
            <span className="text-emerald-400 bg-emerald-950/60 border border-emerald-800 px-2 py-0.5 rounded-md text-[10px] font-bold flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              <span>{currentUser?.fullName || currentUser?.email || 'Đã xác thực JWT'}</span>
            </span>
          ) : (
            <span className="text-amber-400 bg-amber-950/60 border border-amber-800 px-2 py-0.5 rounded-md text-[10px] font-bold">
              Chưa đăng nhập
            </span>
          )}
        </div>

        <div className="flex items-center gap-2">
          {token ? (
            <button
              onClick={logout}
              className="text-slate-400 hover:text-rose-400 text-xs font-semibold px-2 py-1 transition-colors cursor-pointer"
              title="Đăng xuất"
            >
              Đăng xuất
            </button>
          ) : (
            <button
              onClick={() => setShowAuthModal(true)}
              className="bg-emerald-600 hover:bg-emerald-700 text-white font-bold px-3 py-1 rounded-full transition-all text-xs flex items-center gap-1 shadow-sm cursor-pointer"
            >
              <span className="material-symbols-outlined text-[14px]">login</span>
              <span>Đăng nhập Giảng viên</span>
            </button>
          )}

          <button
            onClick={() => setShowAuthModal(true)}
            className="text-indigo-300 hover:text-white text-xs font-semibold px-2 py-1 transition-colors cursor-pointer"
            title="Cài đặt JWT Token"
          >
            <span className="material-symbols-outlined text-[16px]">key</span>
          </button>

          <button
            onClick={toggleAppMode}
            className="bg-primary hover:bg-primary-container text-white font-bold px-3.5 py-1 rounded-full transition-all text-xs flex items-center gap-1.5 shadow-sm active:scale-95 cursor-pointer"
          >
            <span className="material-symbols-outlined text-[16px]">swap_horiz</span>
            <span>Chuyển sang {appMode === 'STUDENT' ? 'Studio Giảng viên' : 'Portal Học viên'}</span>
          </button>
        </div>
      </div>

      {/* Quick Auth & Token Modal */}
      {showAuthModal && (
        <Modal
          isOpen={showAuthModal}
          onClose={() => setShowAuthModal(false)}
          title="Xác thực Tài khoản & JWT Token"
          subtitle="Đăng nhập để nhận quyền LECTURER gọi các API tạo và xuất bản khóa học."
          maxWidth="md"
          icon="lock"
        >
          <div className="space-y-6">
            {/* Form Login */}
            <form onSubmit={handleLoginSubmit} className="space-y-3 bg-surface-container-low p-4 rounded-2xl border border-outline-variant/60">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-800 m-0">
                1. Đăng nhập trực tiếp (Email / Mật khẩu)
              </h4>

              <div className="space-y-1">
                <label className="block text-[11px] font-semibold text-slate-600">Email Giảng viên</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="quangtienhoihop@gmail.com"
                  className="w-full px-3 py-2 bg-white border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:border-primary focus:outline-none"
                  required
                />
              </div>

              <div className="space-y-1">
                <label className="block text-[11px] font-semibold text-slate-600">Mật khẩu</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="123456"
                  className="w-full px-3 py-2 bg-white border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:border-primary focus:outline-none"
                  required
                />
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full bg-primary hover:bg-primary/90 text-white font-bold text-xs py-2 rounded-xl transition-all flex items-center justify-center gap-1.5 cursor-pointer shadow-xs"
              >
                {isSubmitting && (
                  <span className="inline-block animate-spin w-3 h-3 border-2 border-white border-t-transparent rounded-full" />
                )}
                <span>Đăng nhập hệ thống</span>
              </button>
            </form>

            {/* Token Paste Direct */}
            <div className="space-y-3 bg-surface-container-low p-4 rounded-2xl border border-outline-variant/60">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-800 m-0">
                2. Hoặc Dán Token JWT Trực Tiếp
              </h4>
              <p className="text-[11px] text-slate-500 m-0">
                Dán access token lấy từ Swagger hoặc file <code className="bg-slate-200 px-1 py-0.5 rounded">.http</code> nếu bạn đã login bên ngoài.
              </p>

              <textarea
                rows={3}
                value={customToken}
                onChange={(e) => setCustomToken(e.target.value)}
                placeholder="eyJhbGciOiJIUzI1NiIsInR5cCI6..."
                className="w-full px-3 py-2 bg-white border border-outline-variant/70 rounded-xl text-xs text-on-surface font-mono focus:border-primary focus:outline-none"
              />

              <button
                type="button"
                onClick={handleApplyToken}
                disabled={!customToken.trim()}
                className="w-full bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs py-2 rounded-xl transition-all cursor-pointer"
              >
                Áp dụng Token này
              </button>
            </div>
          </div>
        </Modal>
      )}
    </>
  );
};
