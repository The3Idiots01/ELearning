import React, { useState } from 'react';
import { AuthLayout } from '../components/AuthLayout';
import { RegisterPendingView } from '../components/RegisterPendingView';
import { authApi } from '../api/authApi';
import { useToast } from '../../../app/context/ToastContext';

interface RegisterPageProps {
  onNavigateToLogin?: () => void;
  onNavigateToHome?: () => void;
}

export const RegisterPage: React.FC<RegisterPageProps> = ({
  onNavigateToLogin,
  onNavigateToHome
}) => {
  const { showSuccess, showError } = useToast();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState<string | null>(null);

  const validateForm = (): boolean => {
    if (!fullName.trim()) {
      setErrorMessage('Vui lòng nhập họ và tên của bạn.');
      return false;
    }
    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setErrorMessage('Vui lòng nhập địa chỉ email.');
      return false;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(trimmedEmail)) {
      setErrorMessage('Định dạng email không hợp lệ.');
      return false;
    }
    if (!password) {
      setErrorMessage('Vui lòng nhập mật khẩu.');
      return false;
    }
    if (password.length < 6) {
      setErrorMessage('Mật khẩu phải chứa ít nhất 6 ký tự.');
      return false;
    }
    if (password !== confirmPassword) {
      setErrorMessage('Mật khẩu xác nhận không khớp. Vui lòng kiểm tra lại.');
      return false;
    }
    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!validateForm()) return;

    setIsLoading(true);
    try {
      const response = await authApi.register({
        fullName: fullName.trim(),
        email: email.trim().toLowerCase(),
        password: password
      });

      const targetEmail = response?.email || email.trim();
      setRegisteredEmail(targetEmail);
      showSuccess('Đăng ký thành công! Vui lòng kiểm tra email để kích hoạt tài khoản.');
    } catch (err: any) {
      const msg = err.message || 'Đăng ký thất bại. Email có thể đã được sử dụng.';
      setErrorMessage(msg);
      showError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  if (registeredEmail) {
    return (
      <AuthLayout
        title="Đăng ký thành công!"
        subtitle="Chỉ còn một bước nữa để hoàn tất tài khoản của bạn"
        onNavigateHome={onNavigateToHome}
      >
        <RegisterPendingView
          email={registeredEmail}
          onGoToLogin={onNavigateToLogin || (() => {})}
        />
      </AuthLayout>
    );
  }

  return (
    <AuthLayout
      title="Tạo tài khoản mới"
      subtitle="Đăng ký để tham gia học tập và trải nghiệm khóa học tại Learnova"
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

        {/* Full Name */}
        <div>
          <label className="block text-xs font-bold text-slate-700 mb-1.5">
            Họ và tên <span className="text-rose-500">*</span>
          </label>
          <div className="relative">
            <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 text-[18px]">
              person
            </span>
            <input
              type="text"
              value={fullName}
              onChange={(e) => {
                setFullName(e.target.value);
                setErrorMessage(null);
              }}
              placeholder="Nguyễn Văn A"
              className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-2xl text-xs text-slate-900 placeholder:text-slate-400 focus:bg-white focus:border-primary focus:ring-4 focus:ring-primary/10 focus:outline-none transition-all"
              required
            />
          </div>
        </div>

        {/* Email */}
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

        {/* Password */}
        <div>
          <label className="block text-xs font-bold text-slate-700 mb-1.5">
            Mật khẩu <span className="text-rose-500">*</span>
          </label>
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
              placeholder="Tối thiểu 6 ký tự"
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

        {/* Confirm Password */}
        <div>
          <label className="block text-xs font-bold text-slate-700 mb-1.5">
            Xác nhận mật khẩu <span className="text-rose-500">*</span>
          </label>
          <div className="relative">
            <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 text-[18px]">
              lock_reset
            </span>
            <input
              type={showPassword ? 'text' : 'password'}
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value);
                setErrorMessage(null);
              }}
              placeholder="Nhập lại mật khẩu của bạn"
              className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-2xl text-xs text-slate-900 placeholder:text-slate-400 focus:bg-white focus:border-primary focus:ring-4 focus:ring-primary/10 focus:outline-none transition-all"
              required
            />
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
                <span>Đang gửi thông tin đăng ký...</span>
              </>
            ) : (
              <>
                <span>Đăng ký tài khoản</span>
                <span className="material-symbols-outlined text-[18px]">how_to_reg</span>
              </>
            )}
          </button>
        </div>

        {/* Switch to Login */}
        <div className="text-center pt-3 border-t border-slate-100">
          <p className="text-xs text-slate-600">
            Đã có tài khoản Learnova?{' '}
            <button
              type="button"
              onClick={onNavigateToLogin}
              className="font-bold text-primary hover:underline cursor-pointer focus:outline-none"
            >
              Đăng nhập ngay
            </button>
          </p>
        </div>
      </form>
    </AuthLayout>
  );
};
