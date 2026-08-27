import React from 'react';

interface RegisterPendingViewProps {
  email: string;
  onGoToLogin: () => void;
}

export const RegisterPendingView: React.FC<RegisterPendingViewProps> = ({
  email,
  onGoToLogin
}) => {
  return (
    <div className="text-center py-4 space-y-6">
      {/* Animated Mail Icon */}
      <div className="relative inline-flex items-center justify-center">
        <div className="w-20 h-20 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center shadow-inner ring-8 ring-emerald-50/50">
          <span className="material-symbols-outlined text-[44px]">mark_email_unread</span>
        </div>
        <span className="absolute top-0 right-0 flex h-4 w-4">
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
          <span className="relative inline-flex rounded-full h-4 w-4 bg-emerald-500"></span>
        </span>
      </div>

      <div className="space-y-2">
        <h3 className="text-xl font-bold text-slate-900 tracking-tight">
          Kiểm tra hộp thư kích hoạt
        </h3>
        <p className="text-xs text-slate-500 leading-relaxed max-w-sm mx-auto">
          Hệ thống đã gửi liên kết xác thực đến địa chỉ:
        </p>
        <div className="inline-block bg-slate-100 border border-slate-200 px-3.5 py-1.5 rounded-full text-xs font-bold text-slate-800 font-mono break-all">
          {email}
        </div>
      </div>

      {/* Guide Steps */}
      <div className="bg-slate-50 rounded-2xl p-4 text-left border border-slate-200/70 space-y-2.5">
        <div className="flex items-start gap-2.5 text-xs text-slate-700">
          <span className="material-symbols-outlined text-[18px] text-emerald-600 shrink-0 mt-0.5">
            schedule
          </span>
          <span>Liên kết kích hoạt tài khoản có hiệu lực trong vòng <strong>15 phút</strong>.</span>
        </div>
        <div className="flex items-start gap-2.5 text-xs text-slate-700">
          <span className="material-symbols-outlined text-[18px] text-indigo-600 shrink-0 mt-0.5">
            touch_app
          </span>
          <span>Bấm vào nút <strong>"Kích hoạt tài khoản"</strong> trong email để hoàn tất đăng ký.</span>
        </div>
        <div className="flex items-start gap-2.5 text-xs text-slate-500">
          <span className="material-symbols-outlined text-[18px] text-amber-500 shrink-0 mt-0.5">
            info
          </span>
          <span>Nếu không thấy email trong Hộp thư đến, vui lòng kiểm tra thêm mục <strong>Thư rác (Spam)</strong>.</span>
        </div>
      </div>

      {/* Action button */}
      <div className="pt-2">
        <button
          type="button"
          onClick={onGoToLogin}
          className="w-full bg-primary hover:bg-primary-container text-white font-bold text-sm py-3 rounded-2xl transition-all shadow-md shadow-primary/20 flex items-center justify-center gap-2 cursor-pointer active:scale-[0.98]"
        >
          <span className="material-symbols-outlined text-[18px]">login</span>
          <span>Đến trang Đăng nhập</span>
        </button>
      </div>
    </div>
  );
};
