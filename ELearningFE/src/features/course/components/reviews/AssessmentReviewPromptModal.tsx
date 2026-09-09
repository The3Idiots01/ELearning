import React, { useState } from 'react';

interface AssessmentReviewPromptModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectRating: (rating: number) => void;
  courseTitle: string;
  currentProgress: number;
}

const STAR_LABELS: Record<number, string> = {
  1: 'Cần cải thiện',
  2: 'Tạm ổn',
  3: 'Khá tốt',
  4: 'Rất tốt, hài lòng',
  5: 'Tuyệt vời, vượt mong đợi!'
};

export const AssessmentReviewPromptModal: React.FC<AssessmentReviewPromptModalProps> = ({
  isOpen,
  onClose,
  onSelectRating,
  courseTitle,
  currentProgress
}) => {
  const [hoverRating, setHoverRating] = useState<number>(0);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-fade-in">
      <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 max-w-md w-full text-center shadow-2xl relative overflow-hidden flex flex-col items-center">
        {/* Glow ambient background */}
        <div className="absolute -top-16 left-1/2 -translate-x-1/2 w-48 h-48 bg-amber-500/15 rounded-full blur-3xl pointer-events-none" />

        {/* Close Button */}
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 text-slate-500 hover:text-slate-300 p-1.5 rounded-full hover:bg-slate-800 transition-colors cursor-pointer"
        >
          <span className="material-symbols-outlined text-[20px]">close</span>
        </button>

        {/* Celebration Badge Icon */}
        <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-amber-500 to-amber-300 flex items-center justify-center shadow-lg shadow-amber-500/25 mb-4 text-slate-950">
          <span className="material-symbols-outlined text-[36px]" style={{ fontVariationSettings: "'FILL' 1" }}>
            workspace_premium
          </span>
        </div>

        {/* Milestone Title */}
        <span className="text-[11px] font-black uppercase tracking-widest text-amber-400 bg-amber-500/10 px-3 py-1 rounded-full border border-amber-500/20 mb-2">
          Hoàn thành bài kiểm tra!
        </span>

        <h3 className="text-lg sm:text-xl font-black text-white m-0 font-display">
          Cảm ơn bạn đã nỗ lực học tập!
        </h3>

        <p className="text-xs text-slate-300 leading-relaxed mt-2.5 mb-6 max-w-sm">
          Bạn đã đạt <strong className="text-emerald-400 font-bold">{currentProgress}%</strong> lộ trình khóa học <span className="text-white font-medium">"{courseTitle}"</span>. Bạn cảm nhận về khóa học đến thời điểm này như thế nào?
        </p>

        {/* 5 Interactive Stars */}
        <div className="bg-slate-950/60 p-4 rounded-2xl border border-slate-800/80 w-full mb-6">
          <div className="flex items-center justify-center gap-2">
            {[1, 2, 3, 4, 5].map((star) => (
              <button
                key={star}
                type="button"
                onClick={() => onSelectRating(star)}
                onMouseEnter={() => setHoverRating(star)}
                onMouseLeave={() => setHoverRating(0)}
                className="p-1 hover:scale-125 transition-transform duration-200 cursor-pointer"
                title={`${star} sao`}
              >
                <span
                  className={`material-symbols-outlined text-[32px] transition-colors ${
                    star <= (hoverRating || 0)
                      ? 'text-amber-400 drop-shadow-[0_0_8px_rgba(251,191,36,0.5)]'
                      : 'text-slate-600 hover:text-amber-400'
                  }`}
                  style={{
                    fontVariationSettings: star <= (hoverRating || 0) ? "'FILL' 1" : "'FILL' 0"
                  }}
                >
                  star
                </span>
              </button>
            ))}
          </div>
          <div className="text-[11px] font-semibold text-amber-300 min-h-[16px] mt-2">
            {hoverRating > 0 ? STAR_LABELS[hoverRating] : 'Bấm vào số sao để đánh giá nhanh'}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center justify-center gap-3 w-full">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 py-2.5 px-4 rounded-xl text-xs font-bold text-slate-400 hover:text-white bg-slate-800 hover:bg-slate-700 transition-all cursor-pointer"
          >
            Để sau
          </button>
          <button
            type="button"
            onClick={() => onSelectRating(5)}
            className="flex-1 py-2.5 px-4 rounded-xl text-xs font-bold text-white bg-primary hover:bg-primary-hover shadow-lg shadow-primary/20 transition-all cursor-pointer flex items-center justify-center gap-1.5"
          >
            <span>Đánh giá ngay</span>
            <span className="material-symbols-outlined text-[16px]">arrow_forward</span>
          </button>
        </div>
      </div>
    </div>
  );
};
