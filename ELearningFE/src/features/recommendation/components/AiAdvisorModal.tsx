import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../app/context/AuthContext';
import { recommendationApi } from '../api/recommendationApi';
import type { AiRecommendationResponse } from '../types/recommendation';
import type { CourseSummary } from '../../../types/course';
import { RecommendationCourseCard } from './RecommendationCourseCard';

interface AiAdvisorModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectCourse: (course: CourseSummary) => void;
}

export const AiAdvisorModal: React.FC<AiAdvisorModalProps> = ({
  isOpen,
  onClose,
  onSelectCourse
}) => {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'GOAL' | 'QUICK'>('GOAL');
  const [goal, setGoal] = useState<string>('Tôi muốn học chuyên sâu về Backend với Java và Spring Boot');
  const [selectedLevel, setSelectedLevel] = useState<string>('INTERMEDIATE');
  const [selectedInterests, setSelectedInterests] = useState<string[]>(['Java', 'Spring Boot', 'REST API']);

  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isCheckingSaved, setIsCheckingSaved] = useState<boolean>(false);
  const [result, setResult] = useState<AiRecommendationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && isAuthenticated) {
      loadLatestSavedRecommendation();
    }
  }, [isOpen, isAuthenticated]);

  const loadLatestSavedRecommendation = async () => {
    setIsCheckingSaved(true);
    try {
      const saved = await recommendationApi.getLatestAiRecommendation();
      if (saved && saved.items && saved.items.length > 0) {
        setResult(saved);
        if (saved.goal) {
          setGoal(saved.goal);
        }
      }
    } catch {
      // ignore
    } finally {
      setIsCheckingSaved(false);
    }
  };

  if (!isOpen) return null;

  if (!isAuthenticated) {
    return (
      <div className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-md flex items-center justify-center p-3 sm:p-6 overflow-y-auto animate-in fade-in duration-200">
        <div className="bg-surface-container-lowest text-on-surface rounded-3xl w-full max-w-lg shadow-2xl border border-outline-variant/70 overflow-hidden flex flex-col p-8 text-center space-y-6">
          <div className="w-16 h-16 rounded-3xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto shadow-inner">
            <span className="material-symbols-outlined text-[36px]">lock</span>
          </div>
          <div className="space-y-2">
            <h3 className="text-xl font-black font-display text-slate-900 m-0">
              Yêu Cầu Đăng Nhập
            </h3>
            <p className="text-xs text-slate-500 leading-relaxed m-0">
              Tính năng Cố Vấn AI và Đề Xuất Khóa Học yêu cầu bạn đăng nhập tài khoản để AI có thể phân tích tiến trình, lưu trữ đề xuất và tối ưu hóa lộ trình dành riêng cho bạn.
            </p>
          </div>
          <div className="flex gap-3">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-3 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs transition-all cursor-pointer"
            >
              Để Sau
            </button>
            <button
              type="button"
              onClick={() => {
                onClose();
                navigate('/login');
              }}
              className="flex-1 py-3 px-4 rounded-xl bg-primary hover:bg-primary/90 text-white font-bold text-xs shadow-md shadow-primary/30 transition-all cursor-pointer"
            >
              Đăng Nhập Ngay
            </button>
          </div>
        </div>
      </div>
    );
  }

  const popularInterests = [
    'Java', 'Spring Boot', 'React', 'TypeScript', 'Docker',
    'Database', 'Python', 'AI / ML', 'Clean Architecture', 'DevOps'
  ];

  const toggleInterest = (tag: string) => {
    if (selectedInterests.includes(tag)) {
      setSelectedInterests(selectedInterests.filter((t) => t !== tag));
    } else {
      setSelectedInterests([...selectedInterests, tag]);
    }
  };

  const handleAnalyzeGoal = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!goal.trim()) return;

    setIsLoading(true);
    setError(null);
    try {
      const res = await recommendationApi.getAiRecommendations({
        goal: goal.trim(),
        interests: selectedInterests,
        preferredLevel: selectedLevel !== 'ALL' ? selectedLevel : undefined
      });
      setResult(res);
    } catch (err: any) {
      setError(err?.message || 'Có lỗi xảy ra khi kết nối tới AI. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickAnalyze = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await recommendationApi.getQuickAiRecommendations();
      setResult(res);
    } catch (err: any) {
      setError(err?.message || 'Có lỗi xảy ra khi kết nối tới AI. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCourseClick = (course: CourseSummary) => {
    onClose();
    onSelectCourse(course);
  };

  const handleReset = () => {
    setResult(null);
    setError(null);
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-md flex items-center justify-center p-3 sm:p-6 overflow-y-auto animate-in fade-in duration-200">
      <div className="bg-surface-container-lowest text-on-surface rounded-3xl w-full max-w-4xl shadow-2xl border border-outline-variant/70 overflow-hidden flex flex-col max-h-[92vh]">
        
        {/* Modal Header */}
        <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white p-6 sm:p-7 flex items-center justify-between shrink-0 border-b border-indigo-900/40 relative">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-indigo-500/20 border border-indigo-400/30 flex items-center justify-center text-amber-400">
              <span className="material-symbols-outlined text-[24px]">auto_awesome</span>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-lg sm:text-xl font-black font-display text-white m-0">
                  Cố Vấn Khóa Học AI
                </h3>
                <span className="bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[10px] font-bold px-2 py-0.5 rounded-full uppercase">
                  Gemini 3.5 Flash
                </span>
              </div>
              <p className="text-xs text-slate-300 m-0 mt-0.5">
                Đề xuất Top 3 khóa học chuẩn theo mục tiêu và phong cách học của bạn
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            type="button"
            className="w-9 h-9 rounded-full bg-white/10 hover:bg-white/20 text-slate-300 hover:text-white flex items-center justify-center transition-colors cursor-pointer"
          >
            <span className="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 sm:p-8 overflow-y-auto flex-1 space-y-6">

          {/* Checking Saved State */}
          {isCheckingSaved ? (
            <div className="py-16 text-center space-y-3">
              <span className="inline-block animate-spin border-4 border-indigo-200 border-t-primary w-10 h-10 rounded-full" />
              <p className="text-xs text-slate-500 font-bold">Đang tải đề xuất gần nhất của bạn...</p>
            </div>
          ) : result ? (
            <div className="space-y-6 animate-in fade-in duration-300">
              {/* Previous saved recommendation indicator */}
              {result.createdAt && (
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 p-4 bg-indigo-50/80 border border-indigo-200/80 rounded-2xl">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-xl bg-indigo-600 text-white flex items-center justify-center shrink-0">
                      <span className="material-symbols-outlined text-[18px]">history</span>
                    </div>
                    <div>
                      <div className="text-xs font-black text-indigo-950">
                        Đề xuất AI gần nhất của bạn
                      </div>
                      <div className="text-[11px] text-indigo-700/90 font-medium">
                        {result.goal ? `Mục tiêu: "${result.goal}"` : 'Đã lưu trong tài khoản của bạn'}
                      </div>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={handleReset}
                    className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-primary hover:bg-primary/90 text-white text-xs font-bold shadow-sm transition-all cursor-pointer shrink-0"
                  >
                    <span className="material-symbols-outlined text-[16px]">add_circle</span>
                    <span>Tạo Đề Xuất Mới</span>
                  </button>
                </div>
              )}

              {/* Summary Advice Banner */}
              {result.summaryAdvice && (
                <div className="bg-gradient-to-r from-indigo-50 via-purple-50 to-indigo-50 border border-indigo-200/80 rounded-2xl p-5 text-xs sm:text-sm text-slate-800 space-y-2 shadow-sm">
                  <div className="flex items-center gap-2 text-indigo-900 font-extrabold text-xs uppercase tracking-wider">
                    <span className="material-symbols-outlined text-[18px] text-indigo-600">psychology</span>
                    <span>Lời khuyên lộ trình từ AI</span>
                  </div>
                  <p className="m-0 leading-relaxed font-medium">
                    {result.summaryAdvice}
                  </p>
                </div>
              )}

              {result.fallback && (
                <div className="bg-amber-50 border border-amber-200 rounded-xl p-3 text-xs text-amber-800 flex items-center gap-2">
                  <span className="material-symbols-outlined text-[18px] text-amber-600">info</span>
                  <span>AI hiện đang bận hoặc vượt quota, hệ thống đã tự động chuyển sang chế độ gợi ý chất lượng cao theo dữ liệu đánh giá thực tế.</span>
                </div>
              )}

              {/* Course Cards Grid */}
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-extrabold text-slate-900 font-display m-0">
                    Top {result.items.length} Khóa Học Phù Hợp Nhất Dành Cho Bạn:
                  </h4>
                  <button
                    type="button"
                    onClick={handleReset}
                    className="text-xs text-primary font-bold hover:underline inline-flex items-center gap-1 cursor-pointer"
                  >
                    <span className="material-symbols-outlined text-[16px]">refresh</span>
                    <span>Tạo đề xuất mới</span>
                  </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  {result.items.map((item) => (
                    <RecommendationCourseCard
                      key={item.course.id}
                      item={item}
                      onSelect={handleCourseClick}
                    />
                  ))}
                </div>
              </div>
            </div>
          ) : isLoading ? (
            /* Loading State */
            <div className="py-16 text-center space-y-4">
              <div className="relative w-16 h-16 mx-auto">
                <div className="w-16 h-16 border-4 border-indigo-200 border-t-primary rounded-full animate-spin" />
                <span className="material-symbols-outlined absolute inset-0 m-auto text-[24px] text-primary animate-pulse flex items-center justify-center">
                  auto_awesome
                </span>
              </div>
              <div className="space-y-1">
                <h4 className="text-base font-bold text-slate-900 m-0 font-display">
                  Gemini AI đang phân tích dữ liệu...
                </h4>
                <p className="text-xs text-slate-500 m-0">
                  Đang quét nội dung và so khớp mục tiêu để chọn ra 3 khóa học tối ưu nhất cho bạn.
                </p>
              </div>
            </div>
          ) : (
            /* Input Form Mode (Token-Saving) */
            <div className="space-y-6">

              {/* Tab Selector */}
              <div className="flex p-1 bg-surface-container-low rounded-2xl max-w-md border border-outline-variant/60">
                <button
                  type="button"
                  onClick={() => setActiveTab('GOAL')}
                  className={`flex-1 py-2 text-xs font-bold rounded-xl transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                    activeTab === 'GOAL'
                      ? 'bg-surface-container-lowest text-primary shadow-sm'
                      : 'text-on-surface-variant hover:text-on-surface'
                  }`}
                >
                  <span className="material-symbols-outlined text-[16px]">track_changes</span>
                  <span>Theo Mục Tiêu Cá Nhân</span>
                </button>

                <button
                  type="button"
                  onClick={() => setActiveTab('QUICK')}
                  className={`flex-1 py-2 text-xs font-bold rounded-xl transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                    activeTab === 'QUICK'
                      ? 'bg-surface-container-lowest text-primary shadow-sm'
                      : 'text-on-surface-variant hover:text-on-surface'
                  }`}
                >
                  <span className="material-symbols-outlined text-[16px]">bolt</span>
                  <span>Gợi Ý Nhanh 1-Click</span>
                </button>
              </div>

              {error && (
                <div className="p-3.5 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-xl flex items-center gap-2">
                  <span className="material-symbols-outlined text-[18px]">error</span>
                  <span>{error}</span>
                </div>
              )}

              {/* Tab Content 1: Goal Form */}
              {activeTab === 'GOAL' ? (
                <form onSubmit={handleAnalyzeGoal} className="space-y-5">
                  <div className="space-y-2">
                    <label className="block text-xs font-bold uppercase tracking-wider text-slate-700">
                      Mục tiêu nghề nghiệp hoặc kỹ năng bạn muốn đạt được:
                    </label>
                    <textarea
                      rows={3}
                      value={goal}
                      onChange={(e) => setGoal(e.target.value)}
                      placeholder="Ví dụ: Tôi muốn học chuyên sâu về Backend với Java và Spring Boot, hiểu sâu về cấu trúc Clean Architecture và Docker..."
                      className="w-full bg-surface-container-low border border-outline-variant/80 rounded-2xl p-4 text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-all resize-none"
                      required
                    />
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {/* Level */}
                    <div className="space-y-1.5">
                      <label className="block text-xs font-bold text-slate-700">
                        Trình độ hiện tại / mong muốn:
                      </label>
                      <select
                        value={selectedLevel}
                        onChange={(e) => setSelectedLevel(e.target.value)}
                        className="w-full bg-surface-container-low border border-outline-variant/80 rounded-xl px-3.5 py-2.5 text-xs font-bold focus:outline-none focus:ring-2 focus:ring-primary/40"
                      >
                        <option value="ALL">Mọi cấp độ</option>
                        <option value="BEGINNER">Cơ bản (Beginner)</option>
                        <option value="INTERMEDIATE">Trung cấp (Intermediate)</option>
                        <option value="ADVANCED">Nâng cao (Advanced)</option>
                      </select>
                    </div>

                    {/* Hint */}
                    <div className="bg-surface-container-low p-3.5 rounded-xl border border-outline-variant/60 flex items-center gap-2.5 text-xs text-slate-600">
                      <span className="material-symbols-outlined text-primary text-[20px]">lightbulb</span>
                      <span>AI sẽ tự động loại trừ các khóa bạn đã mua và khóa do bạn tạo ra.</span>
                    </div>
                  </div>

                  {/* Popular Interest Chips */}
                  <div className="space-y-2">
                    <label className="block text-xs font-bold text-slate-700">
                      Chủ đề / Công nghệ quan tâm (Chọn nhanh):
                    </label>
                    <div className="flex flex-wrap gap-2">
                      {popularInterests.map((tag) => {
                        const isSelected = selectedInterests.includes(tag);
                        return (
                          <button
                            key={tag}
                            type="button"
                            onClick={() => toggleInterest(tag)}
                            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                              isSelected
                                ? 'bg-primary text-white shadow-sm shadow-primary/30'
                                : 'bg-surface-container-low text-slate-600 hover:bg-surface-container hover:text-slate-900'
                            }`}
                          >
                            {isSelected ? '✓ ' : '+ '}
                            {tag}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  {/* Submit Button */}
                  <div className="pt-3">
                    <button
                      type="submit"
                      disabled={isLoading || !goal.trim()}
                      className="w-full py-3.5 px-6 rounded-2xl bg-gradient-to-r from-primary via-indigo-600 to-violet-600 hover:from-primary/90 hover:to-violet-700 text-white font-extrabold text-sm shadow-lg shadow-primary/25 hover:shadow-primary/40 active:scale-[0.99] transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
                    >
                      <span className="material-symbols-outlined text-[20px]">rocket_launch</span>
                      <span>Bắt Đầu Phân Tích Với Gemini AI (Top 3)</span>
                    </button>
                  </div>
                </form>
              ) : (
                /* Tab Content 2: Quick 1-Click */
                <div className="py-8 text-center space-y-5 max-w-md mx-auto">
                  <div className="w-16 h-16 bg-amber-100 text-amber-600 rounded-3xl flex items-center justify-center mx-auto shadow-inner">
                    <span className="material-symbols-outlined text-[32px]">bolt</span>
                  </div>

                  <div className="space-y-2">
                    <h4 className="text-base font-extrabold text-slate-900 m-0 font-display">
                      Phân tích theo tiến trình học thực tế
                    </h4>
                    <p className="text-xs text-slate-500 leading-relaxed m-0">
                      Hệ thống sẽ tự động quét danh mục và cấp độ của các khóa học bạn đã tham gia để AI đề xuất các bước học nâng cao tiếp theo.
                    </p>
                  </div>

                  <button
                    type="button"
                    onClick={handleQuickAnalyze}
                    className="w-full py-3.5 px-6 rounded-2xl bg-gradient-to-r from-amber-500 to-orange-600 hover:from-amber-600 hover:to-orange-700 text-white font-extrabold text-sm shadow-lg shadow-amber-500/25 transition-all flex items-center justify-center gap-2 cursor-pointer"
                  >
                    <span className="material-symbols-outlined text-[20px]">auto_awesome</span>
                    <span>Tư Vấn Nhanh 1-Click</span>
                  </button>
                </div>
              )}
            </div>
          )}

        </div>

      </div>
    </div>
  );
};
