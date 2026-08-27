import React, { useState } from 'react';
import { useAuth } from '../../../app/context/AuthContext';
import { useToast } from '../../../app/context/ToastContext';
import { authApi } from '../api/authApi';
import { AvatarUploader } from '../components/AvatarUploader';

interface CompleteProfilePageProps {
  onCompleteSuccess?: () => void;
  onSkip?: () => void;
}

const PRESET_INTERESTS = [
  '💻 Lập trình Web & Backend',
  '📱 Lập trình Di động (Mobile)',
  '🎨 Thiết kế UI/UX & Đồ họa',
  '🤖 Trí tuệ nhân tạo & Machine Learning',
  '☁️ Cloud (AWS, Azure, DevOps)',
  '📊 Khoa học Dữ liệu (Data Science)',
  '📈 Quản trị Kinh doanh & Marketing',
  '🛡️ An toàn thông tin (Cybersecurity)',
  '🗣️ Ngoại ngữ & Kỹ năng mềm'
];

const EXPERTISE_SUGGESTIONS = [
  'Sinh viên CNTT',
  'Lập trình viên Frontend React',
  'Lập trình viên Java / Spring Boot',
  'Kỹ sư Fullstack Web',
  'Thiết kế viên UI/UX',
  'Chuyên viên Dữ liệu & AI'
];

export const CompleteProfilePage: React.FC<CompleteProfilePageProps> = ({
  onCompleteSuccess,
  onSkip
}) => {
  const { currentUser, refreshProfile } = useAuth();
  const { showSuccess, showError } = useToast();

  const [fullName, setFullName] = useState(currentUser?.fullName || '');
  const [avatarKey, setAvatarKey] = useState(currentUser?.avatarKey || currentUser?.avatarUrl || '');
  const [avatarPreview, setAvatarPreview] = useState<string | undefined>(currentUser?.avatarUrl);
  const [expertise, setExpertise] = useState(currentUser?.expertise || '');
  const [bio, setBio] = useState(currentUser?.bio || '');

  // Parse existing interests if available
  const initialInterests = React.useMemo(() => {
    if (!currentUser?.interests) return [];
    try {
      const parsed = JSON.parse(currentUser.interests);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }, [currentUser]);

  const [selectedInterests, setSelectedInterests] = useState<string[]>(initialInterests);
  const [customInterestInput, setCustomInterestInput] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const toggleInterest = (tag: string) => {
    setSelectedInterests((prev) =>
      prev.includes(tag) ? prev.filter((i) => i !== tag) : [...prev, tag]
    );
  };

  const removeInterest = (tagToRemove: string) => {
    setSelectedInterests((prev) => prev.filter((i) => i !== tagToRemove));
  };

  const handleAddCustomInterest = (e: React.KeyboardEvent | React.MouseEvent) => {
    if (('key' in e && e.key === 'Enter') || e.type === 'click') {
      e.preventDefault();
      const trimmed = customInterestInput.trim();
      if (trimmed && !selectedInterests.includes(trimmed)) {
        setSelectedInterests((prev) => [...prev, trimmed]);
        setCustomInterestInput('');
      }
    }
  };

  const handleAvatarUploaded = (key: string, previewUrl: string) => {
    setAvatarKey(key);
    setAvatarPreview(previewUrl);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim()) {
      showError('Vui lòng nhập họ và tên của bạn.');
      return;
    }

    setIsSubmitting(true);
    try {
      await authApi.completeProfile({
        fullName: fullName.trim(),
        avatarKey: avatarKey.trim() || undefined,
        expertise: expertise.trim() || undefined,
        bio: bio.trim() || undefined,
        interests: selectedInterests
      });

      await refreshProfile();
      showSuccess('🎉 Hoàn tất thiết lập hồ sơ thành công! Chào mừng bạn đến với Learnova.');
      if (onCompleteSuccess) {
        onCompleteSuccess();
      }
    } catch (err: any) {
      showError(err.message || 'Lỗi khi lưu thông tin hồ sơ.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-3xl w-full bg-surface-container-lowest rounded-3xl shadow-xl border border-outline-variant/70 overflow-hidden">
        {/* Top Gradient Banner Header */}
        <div className="bg-gradient-to-r from-primary via-indigo-600 to-secondary p-6 sm:p-8 text-white relative">
          <div className="flex items-center gap-3.5">
            <span className="w-11 h-11 rounded-2xl bg-white/20 backdrop-blur-sm flex items-center justify-center font-bold text-white shadow-xs shrink-0">
              <span className="material-symbols-outlined text-[26px]">person_check</span>
            </span>
            <div>
              <span className="text-[11px] font-extrabold uppercase tracking-widest text-white/80 block">
                Khởi tạo trải nghiệm học tập
              </span>
              <h1 className="text-xl sm:text-2xl font-black text-white m-0 font-display">
                Hoàn thiện hồ sơ cá nhân
              </h1>
            </div>
          </div>
          <p className="text-xs text-white/90 mt-2.5 max-w-2xl leading-relaxed m-0">
            Dành 1 phút điền thông tin để hệ thống Learnova cá nhân hóa lộ trình, đề xuất các khóa học phù hợp và cấp chứng chỉ chuẩn xác cho bạn.
          </p>
        </div>

        {/* Form Body with Clear Numbered Steps */}
        <form onSubmit={handleSubmit} className="p-6 sm:p-8 space-y-8">
          {/* Section 1: Avatar */}
          <div className="space-y-4 border-b border-slate-100 pb-8">
            <div className="flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-primary/10 text-primary text-xs font-black flex items-center justify-center font-display">
                1
              </span>
              <h3 className="text-sm font-extrabold uppercase tracking-wider text-slate-900 m-0 font-display">
                Ảnh đại diện cá nhân
              </h3>
            </div>

            <AvatarUploader
              currentAvatarUrl={avatarPreview}
              fullName={fullName || currentUser?.fullName}
              onAvatarUploaded={handleAvatarUploaded}
              size="md"
            />
          </div>

          {/* Section 2: Personal Information & Expertise */}
          <div className="space-y-5 border-b border-slate-100 pb-8">
            <div className="flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-primary/10 text-primary text-xs font-black flex items-center justify-center font-display">
                2
              </span>
              <h3 className="text-sm font-extrabold uppercase tracking-wider text-slate-900 m-0 font-display">
                Thông tin cơ bản & Chuyên môn
              </h3>
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                Họ và tên hiển thị <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Ví dụ: Nguyễn Văn An"
                className="w-full px-4 py-3 bg-surface-container-low border border-outline-variant/70 rounded-2xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all font-semibold"
                required
              />
            </div>

            <div className="space-y-2">
              <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                Lĩnh vực chuyên môn / Nghề nghiệp hiện tại
              </label>
              <input
                type="text"
                value={expertise}
                onChange={(e) => setExpertise(e.target.value)}
                placeholder="Ví dụ: Sinh viên CNTT, Kỹ sư Frontend React, Marketing Specialist..."
                className="w-full px-4 py-3 bg-surface-container-low border border-outline-variant/70 rounded-2xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all font-semibold"
              />

              {/* Quick suggestion chips */}
              <div className="flex flex-wrap gap-1.5 pt-1">
                {EXPERTISE_SUGGESTIONS.map((item) => (
                  <button
                    type="button"
                    key={item}
                    onClick={() => setExpertise(item)}
                    className="text-[11px] font-semibold bg-surface-container-low hover:bg-primary/10 text-on-surface hover:text-primary px-2.5 py-1 rounded-lg border border-outline-variant/50 transition-colors cursor-pointer"
                  >
                    + {item}
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                Tiểu sử ngắn (Bio)
              </label>
              <textarea
                rows={3}
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                placeholder="Chia sẻ đôi nét về mục tiêu học tập, sở thích hoặc định hướng nghề nghiệp..."
                className="w-full px-4 py-3 bg-surface-container-low border border-outline-variant/70 rounded-2xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all resize-none leading-relaxed"
              />
            </div>
          </div>

          {/* Section 3: Interests */}
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-primary/10 text-primary text-xs font-black flex items-center justify-center font-display">
                3
              </span>
              <h3 className="text-sm font-extrabold uppercase tracking-wider text-slate-900 m-0 font-display">
                Chủ đề quan tâm yêu thích (Interests)
              </h3>
            </div>

            <p className="text-xs text-slate-500 m-0">
              Chọn các chủ đề bạn muốn ưu tiên xuất hiện trên bảng tin khám phá khóa học:
            </p>

            {/* Selected tags */}
            {selectedInterests.length > 0 && (
              <div className="flex flex-wrap gap-2 pt-1">
                {selectedInterests.map((tag) => (
                  <span
                    key={tag}
                    className="bg-primary text-white text-xs font-bold px-3 py-1.5 rounded-xl shadow-xs flex items-center gap-1.5"
                  >
                    <span>{tag}</span>
                    <button
                      type="button"
                      onClick={() => removeInterest(tag)}
                      className="hover:bg-white/20 rounded-full w-4 h-4 flex items-center justify-center cursor-pointer transition-colors"
                    >
                      <span className="material-symbols-outlined text-[12px]">close</span>
                    </button>
                  </span>
                ))}
              </div>
            )}

            {/* Preset interest pills */}
            <div className="flex flex-wrap gap-2 pt-1">
              {PRESET_INTERESTS.map((tag) => {
                const isSelected = selectedInterests.includes(tag);
                return (
                  <button
                    type="button"
                    key={tag}
                    onClick={() => toggleInterest(tag)}
                    className={`text-xs font-bold px-3 py-1.5 rounded-xl border transition-all cursor-pointer flex items-center gap-1.5 ${
                      isSelected
                        ? 'bg-primary text-white border-primary shadow-xs'
                        : 'bg-surface-container-low hover:bg-surface-container text-on-surface border-outline-variant/60'
                    }`}
                  >
                    <span>{tag}</span>
                    {isSelected && (
                      <span className="material-symbols-outlined text-[14px]">check</span>
                    )}
                  </button>
                );
              })}
            </div>

            {/* Custom Tag Input */}
            <div className="flex items-center gap-2 pt-2 border-t border-slate-100">
              <input
                type="text"
                value={customInterestInput}
                onChange={(e) => setCustomInterestInput(e.target.value)}
                onKeyDown={handleAddCustomInterest}
                placeholder="Thêm chủ đề khác (ấn Enter)..."
                className="flex-1 px-4 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none"
              />
              <button
                type="button"
                onClick={handleAddCustomInterest}
                className="bg-primary/10 hover:bg-primary text-primary hover:text-white font-bold text-xs px-4 py-2.5 rounded-xl transition-all cursor-pointer shrink-0"
              >
                Thêm
              </button>
            </div>
          </div>

          {/* Action Buttons Footer */}
          <div className="pt-6 flex flex-col sm:flex-row items-center justify-between gap-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onSkip}
              className="text-xs font-bold text-slate-500 hover:text-slate-800 transition-colors order-2 sm:order-1 cursor-pointer py-2 px-3 rounded-xl hover:bg-slate-100"
            >
              Để sau, chuyển đến Trang chủ
            </button>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full sm:w-auto bg-primary hover:bg-primary/90 text-white font-extrabold text-xs px-7 py-3 rounded-2xl transition-all shadow-lg shadow-primary/30 flex items-center justify-center gap-2 order-1 sm:order-2 cursor-pointer active:scale-95 disabled:opacity-50"
            >
              {isSubmitting ? (
                <>
                  <span className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full" />
                  <span>Đang hoàn tất...</span>
                </>
              ) : (
                <>
                  <span>HOÀN TẤT & BẮT ĐẦU TRẢI NGHIỆM</span>
                  <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

