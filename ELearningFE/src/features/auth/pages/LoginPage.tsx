import React, { useState } from 'react';
import { AuthLayout } from '../components/AuthLayout';
import { authApi } from '../api/authApi';
import { useAuth } from '../../../app/context/AuthContext';
import { useToast } from '../../../app/context/ToastContext';

interface LoginPageProps {
  onNavigateToRegister?: () => void;
  onNavigateToHome?: () => void;
  onLoginSuccess?: () => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({
  onNavigateToRegister,
  onNavigateToHome,
  onLoginSuccess
}) => {
  const { login, refreshProfile } = useAuth();
  const { showSuccess, showError } = useToast();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setErrorMessage('Vui lòng nhập địa chỉ email.');
      return;
    }
    if (!password) {
      setErrorMessage('Vui lòng nhập mật khẩu.');
      return;
    }

    setIsLoading(true);
    try {
      const response = await authApi.login({
        email: trimmedEmail,
        password: password
      });

      const token = response.accessToken;
      if (token) {
        login(token, response.user);
        await refreshProfile();
        showSuccess('Đăng nhập thành công! Chào mừng bạn trở lại.');
        if (onLoginSuccess) {
          onLoginSuccess();
        } else if (onNavigateToHome) {
          onNavigateToHome();
        }
      } else {
        throw new Error('Không nhận được Access Token từ máy chủ.');
      }
    } catch (err: any) {
      const msg = err.message || 'Đăng nhập thất bại. Vui lòng kiểm tra email và mật khẩu.';
      setErrorMessage(msg);
      showError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Đăng nhập tài khoản"
      subtitle="Truy cập vào hệ thống học tập và giảng dạy Learnova"
      onNavigateHome={onNavigateToHome}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Error Alert */}
        {errorMessage && (
          <div className="bg-rose-50 border border-rose-200 text-rose-700 px-4 py-3 rounded-2xl text-xs flex items-start gap-2 animate-shake">
            <span className="material-symbols-outlined text-[18px] shrink-0 mt-0.5 text-rose-500">
              error
            </span>
            <div className="flex-1 font-medium">{errorMessage}</div>
          </div>
        )}

        {/* Email Field */}
        <div>
          <label className="block text-xs font-bold text-slate-700 mb-1.5">
            Địa chỉ Email <span className="text-rose-500">*</span>
          </label>
          <div className="relative">
            <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 text-[18px]">
              mail
            </span>
            <input
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setErrorMessage(null);
              }}
              placeholder="name@example.com"
              className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-2xl text-xs text-slate-900 placeholder:text-slate-400 focus:bg-white focus:border-primary focus:ring-4 focus:ring-primary/10 focus:outline-none transition-all"
              required
            />
          </div>
        </div>

        {/* Password Field */}
        <div>
          <div className="flex items-center justify-between mb-1.5">
            <label className="block text-xs font-bold text-slate-700">
              Mật khẩu <span className="text-rose-500">*</span>
            </label>
          </div>
          <div className="relative">
            <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 text-[18px]">
              lock
            </span>
            <input
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                setErrorMessage(null);
              }}
              placeholder="Nhập mật khẩu của bạn"
              className="w-full pl-10 pr-10 py-2.5 bg-slate-50 border border-slate-200 rounded-2xl text-xs text-slate-900 placeholder:text-slate-400 focus:bg-white focus:border-primary focus:ring-4 focus:ring-primary/10 focus:outline-none transition-all"
              required
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer p-1"
            >
              <span className="material-symbols-outlined text-[18px]">
                {showPassword ? 'visibility_off' : 'visibility'}
              </span>
            </button>
          </div>
        </div>

        {/* Submit Button */}
        <div className="pt-2">
          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-primary hover:bg-primary-container text-white font-bold text-sm py-3 rounded-2xl transition-all shadow-md shadow-primary/25 flex items-center justify-center gap-2 cursor-pointer active:scale-[0.98] disabled:opacity-60"
          >
            {isLoading ? (
              <>
                <span className="inline-block animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full" />
                <span>Đang xác thực...</span>
              </>
            ) : (
              <>
                <span>Đăng nhập</span>
                <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
              </>
            )}
          </button>
        </div>

        {/* Switch to Register */}
        <div className="text-center pt-3 border-t border-slate-100">
          <p className="text-xs text-slate-600">
            Chưa có tài khoản Learnova?{' '}
            <button
              type="button"
              onClick={onNavigateToRegister}
              className="font-bold text-primary hover:underline cursor-pointer focus:outline-none"
            >
              Đăng ký ngay
            </button>
          </p>
        </div>
      </form>
    </AuthLayout>
  );
};
