import React from 'react';

interface AiAdvisorBannerProps {
  onOpen: () => void;
}

export const AiAdvisorBanner: React.FC<AiAdvisorBannerProps> = ({ onOpen }) => {
  return (
    <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white p-6 sm:p-8 shadow-xl border border-indigo-500/30 mb-8">
      {/* Background Decorative Glow */}
      <div className="absolute -top-16 -right-16 w-64 h-64 bg-indigo-500/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-16 -left-16 w-64 h-64 bg-violet-500/20 rounded-full blur-3xl pointer-events-none" />

      <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div className="space-y-2 max-w-2xl">
          <div className="inline-flex items-center gap-2 bg-indigo-500/20 border border-indigo-400/30 backdrop-blur-md text-indigo-300 text-[11px] font-bold px-3 py-1 rounded-full uppercase tracking-wider">
            <span className="material-symbols-outlined text-[14px] text-amber-400 animate-spin">
              auto_awesome
            </span>
            <span>Trợ Lý Lộ Trình AI Cá Nhân Hóa</span>
          </div>

          <h3 className="text-xl sm:text-2xl font-black text-white font-display tracking-tight m-0">
            Bạn chưa biết nên bắt đầu hoặc học tiếp khóa nào?
          </h3>

          <p className="text-xs sm:text-sm text-slate-300 leading-relaxed m-0">
            Chỉ cần 1 click, AI Gemini 3.5 sẽ phân tích mục tiêu nghề nghiệp, kỹ năng quan tâm và các môn bạn đã học để chọn ra <strong>Top 3 khóa học phù hợp nhất</strong>.
          </p>
        </div>

        <button
          type="button"
          onClick={onOpen}
          className="shrink-0 inline-flex items-center gap-2.5 bg-gradient-to-r from-primary via-indigo-600 to-violet-600 hover:from-primary/90 hover:to-violet-700 text-white font-black text-xs sm:text-sm px-6 py-3.5 rounded-2xl shadow-lg shadow-indigo-600/30 hover:shadow-indigo-600/50 hover:scale-[1.02] active:scale-[0.98] transition-all cursor-pointer"
        >
          <span className="material-symbols-outlined text-[20px]">psychology</span>
          <span>Nhận Tư Vấn Với AI</span>
          <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
        </button>
      </div>
    </div>
  );
};
