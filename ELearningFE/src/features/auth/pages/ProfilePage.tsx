import React, { useEffect, useState } from 'react';
import { authApi } from '../api/authApi';
import { useAuth } from '../../../app/context/AuthContext';
import { useToast } from '../../../app/context/ToastContext';
import { AvatarUploader } from '../components/AvatarUploader';
import type { User } from '../../../types/auth';

interface ProfilePageProps {
  onNavigateHome?: () => void;
  onNavigateToLogin?: () => void;
  onNavigateToCompleteProfile?: () => void;
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

export const ProfilePage: React.FC<ProfilePageProps> = ({
  onNavigateHome,
  onNavigateToLogin,
  onNavigateToCompleteProfile
}) => {
  const { token, currentUser, logout, refreshProfile } = useAuth();
  const { showSuccess, showError } = useToast();

  const [profile, setProfile] = useState<User | null>(currentUser);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  // Form states for editing
  const [fullName, setFullName] = useState('');
  const [avatarKey, setAvatarKey] = useState('');
  const [avatarPreview, setAvatarPreview] = useState<string | undefined>('');
  const [bio, setBio] = useState('');
  const [expertise, setExpertise] = useState('');
  const [selectedInterests, setSelectedInterests] = useState<string[]>([]);
  const [customInterestInput, setCustomInterestInput] = useState('');

  const parseInterests = (interestsStr?: string): string[] => {
    if (!interestsStr) return [];
    try {
      const parsed = JSON.parse(interestsStr);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  };

  const populateForm = (user: User) => {
    setProfile(user);
    setFullName(user.fullName || '');
    setAvatarKey(user.avatarKey || user.avatarUrl || '');
    setAvatarPreview(user.avatarUrl || undefined);
    setBio(user.bio || '');
    setExpertise(user.expertise || '');
    setSelectedInterests(parseInterests(user.interests));
  };

  const fetchUserProfile = async () => {
    if (!token) return;
    setIsLoading(true);
    try {
      const user = await authApi.getProfile();
      populateForm(user);
    } catch (err: any) {
      // Fallback to getMe if profile endpoint has issue
      try {
        const user = await authApi.getMe();
        populateForm(user);
      } catch {
        showError(err.message || 'Không thể tải thông tin hồ sơ.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (token) {
      fetchUserProfile();
    }
  }, [token]);

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

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim()) {
      showError('Họ và tên không được để trống.');
      return;
    }

    setIsSaving(true);
    try {
      const updatedUser = await authApi.updateProfile({
        fullName: fullName.trim(),
        avatarKey: avatarKey.trim() || undefined,
        bio: bio.trim() || undefined,
        expertise: expertise.trim() || undefined,
        interests: selectedInterests
      });

      populateForm(updatedUser);
      setIsEditing(false);
      showSuccess('Cập nhật thông tin tài khoản thành công!');
      await refreshProfile();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi lưu thông tin.');
    } finally {
      setIsSaving(false);
    }
  };

  if (!token) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="bg-white max-w-md w-full rounded-3xl p-8 text-center shadow-xl border border-slate-100 space-y-4">
          <div className="w-16 h-16 rounded-full bg-amber-50 text-amber-500 flex items-center justify-center mx-auto">
            <span className="material-symbols-outlined text-[32px]">lock</span>
          </div>
          <h3 className="text-xl font-bold text-slate-900">Yêu cầu đăng nhập</h3>
          <p className="text-xs text-slate-600">
            Bạn cần đăng nhập tài khoản để xem và quản lý thông tin hồ sơ cá nhân.
          </p>
          <div className="pt-2 flex flex-col gap-2">
            <button
              onClick={onNavigateToLogin}
              className="w-full bg-primary hover:bg-primary-container text-white font-bold text-xs py-3 rounded-2xl transition-all cursor-pointer"
            >
              Đến trang Đăng nhập
            </button>
            <button
              onClick={onNavigateHome}
              className="w-full bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs py-2.5 rounded-2xl transition-all cursor-pointer"
            >
              Về trang chủ
            </button>
          </div>
        </div>
      </div>
    );
  }

  const roleBadgeColor = {
    LEARNER: 'bg-blue-50 text-blue-700 border-blue-200',
    LECTURER: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    ADMIN: 'bg-purple-50 text-purple-700 border-purple-200'
  };

  const displayInterests = parseInterests(profile?.interests);

  return (
    <div className="min-h-screen bg-slate-50 py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Top Breadcrumb & Actions */}
        <div className="flex items-center justify-between">
          <button
            onClick={onNavigateHome}
            className="flex items-center gap-2 text-xs font-bold text-slate-600 hover:text-primary transition-colors cursor-pointer"
          >
            <span className="material-symbols-outlined text-[18px]">arrow_back</span>
            <span>Quay lại Khám phá khóa học</span>
          </button>

          <div className="flex items-center gap-3">
            {onNavigateToCompleteProfile && (
              <button
                onClick={onNavigateToCompleteProfile}
                className="flex items-center gap-1.5 text-xs font-bold text-primary hover:bg-primary/10 px-3 py-1.5 rounded-xl transition-colors cursor-pointer border border-primary/20"
              >
                <span className="material-symbols-outlined text-[16px]">how_to_reg</span>
                <span>Trang điền hồ sơ (Onboarding)</span>
              </button>
            )}

            <button
              onClick={() => {
                logout();
                if (onNavigateToLogin) onNavigateToLogin();
              }}
              className="flex items-center gap-1.5 text-xs font-bold text-rose-600 hover:bg-rose-50 px-3 py-1.5 rounded-xl transition-colors cursor-pointer border border-rose-200"
            >
              <span className="material-symbols-outlined text-[16px]">logout</span>
              <span>Đăng xuất</span>
            </button>
          </div>
        </div>

        {/* Profile Header Banner Card */}
        <div className="bg-white rounded-3xl shadow-xl shadow-slate-200/50 border border-slate-100 overflow-hidden">
          <div className="h-32 bg-gradient-to-r from-primary via-indigo-600 to-secondary relative">
            <div className="absolute inset-0 bg-black/10 backdrop-blur-[1px]" />
          </div>

          <div className="px-6 pb-6 relative flex flex-col sm:flex-row items-start sm:items-end justify-between gap-4 -mt-14">
            <div className="flex flex-col sm:flex-row items-start sm:items-end gap-4">
              {/* Avatar Circle Container */}
              <div className="w-24 h-24 rounded-2xl bg-white p-1 shadow-lg ring-4 ring-white shrink-0 overflow-hidden">
                {profile?.avatarUrl ? (
                  <img
                    src={profile.avatarUrl}
                    alt={profile.fullName}
                    className="w-full h-full object-cover rounded-xl"
                  />
                ) : (
                  <div className="w-full h-full rounded-xl bg-gradient-to-tr from-primary to-secondary text-white font-black text-2xl flex items-center justify-center">
                    {profile?.fullName ? profile.fullName.substring(0, 2).toUpperCase() : 'US'}
                  </div>
                )}
              </div>

              <div>
                <div className="flex items-center gap-2 flex-wrap">
                  <h1 className="text-xl sm:text-2xl font-black text-slate-900">
                    {profile?.fullName || 'Người dùng'}
                  </h1>
                  <span
                    className={`text-[10px] font-extrabold uppercase px-2.5 py-0.5 rounded-full border ${
                      roleBadgeColor[profile?.role || 'LEARNER']
                    }`}
                  >
                    {profile?.role || 'LEARNER'}
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-0.5 flex items-center gap-1.5">
                  <span className="material-symbols-outlined text-[14px]">mail</span>
                  <span>{profile?.email}</span>
                </p>
              </div>
            </div>

            {/* Edit Button */}
            <button
              onClick={() => {
                if (!isEditing && profile) {
                  populateForm(profile);
                }
                setIsEditing(!isEditing);
              }}
              className="bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold px-4 py-2 rounded-xl transition-all flex items-center gap-1.5 shadow-sm cursor-pointer"
            >
              <span className="material-symbols-outlined text-[16px]">
                {isEditing ? 'close' : 'edit'}
              </span>
              <span>{isEditing ? 'Hủy chỉnh sửa' : 'Chỉnh sửa hồ sơ'}</span>
            </button>
          </div>
        </div>

        {/* Profile Content Body */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Left Column: Account Meta */}
          <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 space-y-4 h-fit">
            <h3 className="text-xs font-extrabold uppercase tracking-wider text-slate-400">
              Thông tin tài khoản
            </h3>

            <div className="space-y-3 text-xs">
              <div>
                <span className="text-slate-500 block text-[11px]">Mã định danh (User ID)</span>
                <span className="font-mono font-bold text-slate-800">#{profile?.id || '—'}</span>
              </div>

              <div>
                <span className="text-slate-500 block text-[11px]">Nguồn xác thực (Provider)</span>
                <span className="font-bold text-slate-800 flex items-center gap-1">
                  <span className="material-symbols-outlined text-[14px] text-primary">verified_user</span>
                  {profile?.authProvider || 'LOCAL'}
                </span>
              </div>

              <div>
                <span className="text-slate-500 block text-[11px]">Trạng thái tài khoản</span>
                <span className="inline-flex items-center gap-1 font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-md text-[11px]">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                  Đang hoạt động
                </span>
              </div>

              {profile?.createdAt && (
                <div>
                  <span className="text-slate-500 block text-[11px]">Ngày tham gia</span>
                  <span className="font-semibold text-slate-800">
                    {new Date(profile.createdAt).toLocaleDateString('vi-VN')}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Right Column: Profile Details / Form */}
          <div className="md:col-span-2 bg-white rounded-3xl p-6 shadow-sm border border-slate-100 space-y-6">
            <div className="flex items-center justify-between border-b border-slate-100 pb-4">
              <h3 className="text-sm font-extrabold uppercase tracking-wider text-slate-900">
                {isEditing ? 'Chỉnh sửa thông tin cá nhân' : 'Hồ sơ chi tiết'}
              </h3>
              {isLoading && (
                <span className="text-xs text-primary font-bold flex items-center gap-1">
                  <span className="animate-spin w-3 h-3 border-2 border-primary border-t-transparent rounded-full" />
                  Đang tải...
                </span>
              )}
            </div>

            {isEditing ? (
              <form onSubmit={handleSaveProfile} className="space-y-6">
                {/* 1. Avatar Uploader Component */}
                <div className="bg-slate-50/80 p-4 rounded-2xl border border-slate-100">
                  <AvatarUploader
                    currentAvatarUrl={avatarPreview}
                    fullName={fullName || profile?.fullName}
                    onAvatarUploaded={handleAvatarUploaded}
                    size="md"
                  />
                </div>

                {/* 2. Full Name Input */}
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Họ và tên <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:bg-white focus:border-primary focus:outline-none"
                    required
                  />
                </div>

                {/* 3. Expertise Input */}
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Lĩnh vực chuyên môn (Expertise)
                  </label>
                  <input
                    type="text"
                    value={expertise}
                    onChange={(e) => setExpertise(e.target.value)}
                    placeholder="Ví dụ: Giảng viên Lập trình Java, Thiết kế UI/UX"
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:bg-white focus:border-primary focus:outline-none"
                  />
                </div>

                {/* 4. Bio Textarea */}
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Tiểu sử & Giới thiệu bản thân (Bio)
                  </label>
                  <textarea
                    rows={4}
                    value={bio}
                    onChange={(e) => setBio(e.target.value)}
                    placeholder="Chia sẻ đôi nét về kinh nghiệm, mục tiêu học tập hoặc giảng dạy của bạn..."
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:bg-white focus:border-primary focus:outline-none resize-none"
                  />
                </div>

                {/* 5. Interests Selection */}
                <div className="space-y-2">
                  <label className="block text-xs font-bold text-slate-700">
                    Lĩnh vực quan tâm (Interests)
                  </label>
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

                  {/* Custom interest tag input */}
                  <div className="flex items-center gap-2 pt-1">
                    <input
                      type="text"
                      value={customInterestInput}
                      onChange={(e) => setCustomInterestInput(e.target.value)}
                      onKeyDown={handleAddCustomInterest}
                      placeholder="Thêm tag khác (Enter)..."
                      className="flex-1 px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 focus:bg-white focus:border-primary focus:outline-none"
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
                <div className="flex items-center gap-3 pt-3 border-t border-slate-100">
                  <button
                    type="submit"
                    disabled={isSaving}
                    className="bg-primary hover:bg-primary-container text-white font-bold text-xs px-5 py-2.5 rounded-xl transition-all shadow-sm flex items-center gap-2 cursor-pointer disabled:opacity-50"
                  >
                    {isSaving ? (
                      <>
                        <span className="animate-spin w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full" />
                        <span>Đang lưu...</span>
                      </>
                    ) : (
                      <span>Lưu thay đổi</span>
                    )}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      if (profile) populateForm(profile);
                      setIsEditing(false);
                    }}
                    disabled={isSaving}
                    className="bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs px-4 py-2.5 rounded-xl transition-all cursor-pointer"
                  >
                    Hủy bỏ
                  </button>
                </div>
              </form>
            ) : (
              <div className="space-y-5 text-xs">
                <div>
                  <span className="text-slate-500 font-medium block mb-1">Chuyên môn</span>
                  <p className="font-bold text-slate-800 bg-slate-50 p-3 rounded-xl border border-slate-100 m-0">
                    {profile?.expertise || 'Chưa cập nhật chuyên môn'}
                  </p>
                </div>

                <div>
                  <span className="text-slate-500 font-medium block mb-1">Tiểu sử & Giới thiệu</span>
                  <p className="text-slate-700 bg-slate-50 p-3 rounded-xl border border-slate-100 leading-relaxed whitespace-pre-line m-0">
                    {profile?.bio || 'Chưa có thông tin giới thiệu bản thân.'}
                  </p>
                </div>

                <div>
                  <span className="text-slate-500 font-medium block mb-1">Lĩnh vực quan tâm (Interests)</span>
                  {displayInterests.length > 0 ? (
                    <div className="flex flex-wrap gap-2 pt-1">
                      {displayInterests.map((interest, idx) => (
                        <span
                          key={idx}
                          className="bg-primary/10 text-primary font-bold px-3 py-1 rounded-lg text-[11px]"
                        >
                          {interest}
                        </span>
                      ))}
                    </div>
                  ) : (
                    <p className="text-slate-400 italic bg-slate-50 p-3 rounded-xl border border-slate-100 m-0">
                      Chưa chọn lĩnh vực quan tâm.
                    </p>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
