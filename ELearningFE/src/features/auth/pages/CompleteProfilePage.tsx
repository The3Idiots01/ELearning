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
    <div className="min-h-screen bg-slate-50 flex items-center justify-center py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-2xl w-full bg-white rounded-3xl shadow-xl shadow-slate-200/60 border border-slate-100 overflow-hidden">
        {/* Top Gradient Banner Header */}
        <div className="bg-gradient-to-r from-primary via-indigo-600 to-secondary p-8 text-white relative">
          <div className="flex items-center gap-3">
            <span className="w-10 h-10 rounded-2xl bg-white/20 backdrop-blur-sm flex items-center justify-center font-bold text-white shadow-xs">
              <span className="material-symbols-outlined text-[24px]">person_check</span>
            </span>
            <div>
              <span className="text-[11px] font-extrabold uppercase tracking-widest text-white/80">
                Bước hoàn thiện hồ sơ
              </span>
              <h1 className="text-xl sm:text-2xl font-black text-white m-0">
                Chào mừng bạn đến với Learnova!
              </h1>
            </div>
          </div>
          <p className="text-xs text-white/90 mt-2 max-w-xl leading-relaxed">
            Hãy dành vài giây hoàn thiện các thông tin còn trống để cá nhân hóa lộ trình học tập và kết nối với cộng đồng.
          </p>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 sm:p-8 space-y-6">
          {/* Section 1: Avatar Upload */}
          <div className="border-b border-slate-100 pb-6">
            <label className="block text-xs font-extrabold uppercase tracking-wider text-slate-500 mb-3">
              1. Ảnh đại diện của bạn
            </label>
            <AvatarUploader
              currentAvatarUrl={avatarPreview}
              fullName={fullName || currentUser?.fullName}
              onAvatarUploaded={handleAvatarUploaded}
              size="md"
            />
          </div>

          {/* Section 2: Personal Information */}
          <div className="space-y-4 border-b border-slate-100 pb-6">
            <label className="block text-xs font-extrabold uppercase tracking-wider text-slate-500">
              2. Thông tin cơ bản
            </label>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Họ và tên <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Ví dụ: Nguyễn Văn A"
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:bg-white focus:border-primary focus:outline-none transition-all"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Lĩnh vực chuyên môn / Nghề nghiệp hiện tại
              </label>
              <input
                type="text"
                value={expertise}
                onChange={(e) => setExpertise(e.target.value)}
                placeholder="Ví dụ: Sinh viên CNTT, Lập trình viên React, Chuyên viên Marketing..."
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:bg-white focus:border-primary focus:outline-none transition-all"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Tiểu sử ngắn (Bio)
              </label>
              <textarea
                rows={3}
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                placeholder="Chia sẻ một chút về mục tiêu học tập hoặc sở thích của bạn..."
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:bg-white focus:border-primary focus:outline-none transition-all resize-none"
              />
            </div>
          </div>

          {/* Section 3: Interests */}
          <div className="space-y-3">
            <label className="block text-xs font-extrabold uppercase tracking-wider text-slate-500">
              3. Chủ đề quan tâm yêu thích (Interests)
            </label>
            <p className="text-xs text-slate-500 m-0">
              Chọn các chủ đề bạn muốn ưu tiên khám phá:
            </p>

            <div className="flex flex-wrap gap-2 pt-1">
              {PRESET_INTERESTS.map((tag) => {
                const isSelected = selectedInterests.includes(tag);
                return (
                  <button
                    type="button"
                    key={tag}
                    onClick={() => toggleInterest(tag)}
                    className={`text-xs font-bold px-3 py-1.5 rounded-xl border transition-all cursor-pointer flex items-center gap-1 ${
                      isSelected
                        ? 'bg-primary text-white border-primary shadow-xs'
                        : 'bg-slate-50 hover:bg-slate-100 text-slate-700 border-slate-200'
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
            <div className="flex items-center gap-2 pt-2">
              <input
                type="text"
                value={customInterestInput}
                onChange={(e) => setCustomInterestInput(e.target.value)}
                onKeyDown={handleAddCustomInterest}
                placeholder="Thêm chủ đề khác (ấn Enter)..."
                className="flex-1 px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:bg-white focus:border-primary focus:outline-none"
              />
              <button
                type="button"
                onClick={handleAddCustomInterest}
                className="bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs px-3.5 py-2 rounded-xl transition-all cursor-pointer"
              >
                Thêm
              </button>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="pt-4 flex flex-col sm:flex-row items-center justify-between gap-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onSkip}
              className="text-xs font-bold text-slate-500 hover:text-slate-800 transition-colors order-2 sm:order-1 cursor-pointer py-2"
            >
              Để sau, chuyển đến Trang chủ
            </button>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full sm:w-auto bg-primary hover:bg-primary-container text-white font-extrabold text-xs px-6 py-3 rounded-2xl transition-all shadow-md flex items-center justify-center gap-2 order-1 sm:order-2 cursor-pointer disabled:opacity-50"
            >
              {isSubmitting ? (
                <>
                  <span className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full" />
                  <span>Đang lưu...</span>
                </>
              ) : (
                <>
                  <span>Hoàn tất & Bắt đầu trải nghiệm</span>
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
