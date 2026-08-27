import React, { useEffect, useState, useMemo } from 'react';
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

const EXPERTISE_SUGGESTIONS = [
  'Lập trình viên Web / Backend',
  'Lập trình viên Frontend React',
  'Kỹ sư Fullstack',
  'Thiết kế viên UI/UX',
  'Kỹ sư AI & Machine Learning',
  'Chuyên viên Khoa học Dữ liệu',
  'Kỹ sư DevOps / Cloud',
  'Giảng viên Công nghệ thông tin',
  'Sinh viên CNTT'
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
  const [copiedId, setCopiedId] = useState(false);

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

  const handleCopyUserId = (id: number) => {
    navigator.clipboard.writeText(String(id));
    setCopiedId(true);
    setTimeout(() => setCopiedId(false), 2000);
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
      showSuccess('🎉 Cập nhật thông tin tài khoản thành công!');
      await refreshProfile();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi lưu thông tin.');
    } finally {
      setIsSaving(false);
    }
  };

  // Calculate Profile Completeness Score
  const completenessDetails = useMemo(() => {
    const currentData = isEditing
      ? { fullName, avatarUrl: avatarPreview, bio, expertise, interests: selectedInterests }
      : {
          fullName: profile?.fullName,
          avatarUrl: profile?.avatarUrl,
          bio: profile?.bio,
          expertise: profile?.expertise,
          interests: parseInterests(profile?.interests)
        };

    const checks = [
      { id: 'name', label: 'Họ và tên', completed: !!currentData.fullName?.trim(), weight: 20 },
      { id: 'avatar', label: 'Ảnh đại diện', completed: !!currentData.avatarUrl, weight: 20 },
      { id: 'expertise', label: 'Lĩnh vực chuyên môn', completed: !!currentData.expertise?.trim(), weight: 20 },
      { id: 'bio', label: 'Tiểu sử giới thiệu', completed: !!currentData.bio?.trim(), weight: 20 },
      { id: 'interests', label: 'Lĩnh vực quan tâm', completed: currentData.interests.length > 0, weight: 20 }
    ];

    const total = checks.reduce((acc, c) => acc + (c.completed ? c.weight : 0), 0);
    return { checks, total };
  }, [isEditing, fullName, avatarPreview, bio, expertise, selectedInterests, profile]);

  if (!token) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center p-4">
        <div className="bg-surface-container-lowest max-w-md w-full rounded-3xl p-8 text-center shadow-xl border border-outline-variant/70 space-y-4">
          <div className="w-16 h-16 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center mx-auto shadow-xs">
            <span className="material-symbols-outlined text-[32px]">lock</span>
          </div>
          <h3 className="text-xl font-bold text-slate-900 font-display">Yêu cầu đăng nhập</h3>
          <p className="text-xs text-slate-600 leading-relaxed">
            Bạn cần đăng nhập tài khoản để xem và quản lý thông tin hồ sơ cá nhân trên Learnova.
          </p>
          <div className="pt-2 flex flex-col gap-2.5">
            <button
              onClick={onNavigateToLogin}
              className="w-full bg-primary hover:bg-primary/90 text-white font-bold text-xs py-3 rounded-xl transition-all cursor-pointer shadow-md shadow-primary/20"
            >
              Đến trang Đăng nhập
            </button>
            <button
              onClick={onNavigateHome}
              className="w-full bg-surface-container-low hover:bg-surface-container text-on-surface font-bold text-xs py-2.5 rounded-xl transition-all cursor-pointer border border-outline-variant/60"
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

  const roleLabel = {
    LEARNER: 'Học viên (Learner)',
    LECTURER: 'Giảng viên (Lecturer)',
    ADMIN: 'Quản trị viên (Admin)'
  };

  const displayInterests = parseInterests(profile?.interests);

  return (
    <div className="min-h-screen bg-background py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto space-y-6">
        {/* Top Header & Breadcrumb Actions */}
        <header className="bg-surface-container-lowest border border-outline-variant/70 rounded-2xl px-5 py-3.5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-xs">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={onNavigateHome}
              className="p-2 text-slate-400 hover:text-slate-700 hover:bg-surface-container-low rounded-xl transition-colors cursor-pointer"
              title="Quay lại khám phá khóa học"
            >
              <span className="material-symbols-outlined text-[20px]">arrow_back</span>
            </button>
            <div>
              <span className="text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
                Quản lý tài khoản
              </span>
              <h1 className="text-base font-extrabold text-slate-900 m-0 font-display flex items-center gap-2">
                <span>Hồ sơ người dùng</span>
                {isLoading && (
                  <span className="animate-spin w-3.5 h-3.5 border-2 border-primary border-t-transparent rounded-full" />
                )}
              </h1>
            </div>
          </div>

          <div className="flex items-center gap-2.5 self-end sm:self-center">
            {onNavigateToCompleteProfile && (
              <button
                type="button"
                onClick={onNavigateToCompleteProfile}
                className="bg-surface-container-low hover:bg-surface-container text-primary font-bold text-xs px-3.5 py-2 rounded-xl transition-all cursor-pointer border border-primary/20 flex items-center gap-1.5 shadow-xs"
              >
                <span className="material-symbols-outlined text-[16px]">how_to_reg</span>
                <span className="hidden sm:inline">Trang hoàn thiện hồ sơ</span>
                <span className="sm:hidden">Onboarding</span>
              </button>
            )}

            <button
              type="button"
              onClick={() => {
                if (!isEditing && profile) {
                  populateForm(profile);
                }
                setIsEditing(!isEditing);
              }}
              className={`text-xs font-bold px-4 py-2 rounded-xl transition-all flex items-center gap-1.5 cursor-pointer shadow-xs ${
                isEditing
                  ? 'bg-slate-200 hover:bg-slate-300 text-slate-800'
                  : 'bg-primary hover:bg-primary/90 text-white shadow-primary/20'
              }`}
            >
              <span className="material-symbols-outlined text-[16px]">
                {isEditing ? 'close' : 'edit'}
              </span>
              <span>{isEditing ? 'Hủy chỉnh sửa' : 'Chỉnh sửa hồ sơ'}</span>
            </button>

            <button
              type="button"
              onClick={() => {
                logout();
                if (onNavigateToLogin) onNavigateToLogin();
              }}
              className="p-2 text-rose-600 hover:bg-rose-50 rounded-xl transition-colors cursor-pointer border border-rose-200 shrink-0"
              title="Đăng xuất khỏi hệ thống"
            >
              <span className="material-symbols-outlined text-[18px]">logout</span>
            </button>
          </div>
        </header>

        {/* Profile Hero Card */}
        <div className="bg-surface-container-lowest rounded-3xl shadow-sm border border-outline-variant/70 overflow-hidden">
          <div className="h-32 sm:h-36 bg-gradient-to-r from-primary via-indigo-600 to-secondary relative">
            <div className="absolute inset-0 bg-black/10 backdrop-blur-[1px]" />
            <div className="absolute top-4 right-4 flex items-center gap-2">
              <span className="inline-flex items-center gap-1.5 bg-black/30 backdrop-blur-md text-white px-3 py-1 rounded-full text-xs font-semibold border border-white/20">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                <span>Trực tuyến</span>
              </span>
            </div>
          </div>

          <div className="px-6 sm:px-8 pb-6 relative flex flex-col sm:flex-row items-start sm:items-end justify-between gap-6 -mt-12 sm:-mt-14">
            <div className="flex flex-col sm:flex-row items-start sm:items-end gap-5">
              {/* Avatar Container */}
              <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-2xl bg-white p-1 shadow-lg ring-4 ring-white shrink-0 overflow-hidden">
                {profile?.avatarUrl ? (
                  <img
                    src={profile.avatarUrl}
                    alt={profile.fullName}
                    className="w-full h-full object-cover rounded-xl"
                  />
                ) : (
                  <div className="w-full h-full rounded-xl bg-gradient-to-tr from-primary via-indigo-600 to-secondary text-white font-black text-2xl sm:text-3xl flex items-center justify-center font-display">
                    {profile?.fullName ? profile.fullName.substring(0, 2).toUpperCase() : 'US'}
                  </div>
                )}
              </div>

              {/* Identity Details */}
              <div className="space-y-1">
                <div className="flex items-center gap-2.5 flex-wrap">
                  <h2 className="text-xl sm:text-2xl font-black text-slate-900 font-display m-0">
                    {profile?.fullName || 'Người dùng Learnova'}
                  </h2>
                  <span
                    className={`text-[10px] font-extrabold uppercase px-2.5 py-0.5 rounded-full border ${
                      roleBadgeColor[profile?.role || 'LEARNER']
                    }`}
                  >
                    {roleLabel[profile?.role || 'LEARNER']}
                  </span>
                </div>

                <div className="flex items-center gap-4 text-xs text-slate-500 flex-wrap">
                  <span className="flex items-center gap-1">
                    <span className="material-symbols-outlined text-[15px] text-primary">mail</span>
                    <span>{profile?.email}</span>
                  </span>

                  <span className="flex items-center gap-1 font-mono text-[11px] bg-slate-100 px-2 py-0.5 rounded-md text-slate-700">
                    <span>ID: #{profile?.id || '—'}</span>
                  </span>
                </div>
              </div>
            </div>

            {/* Quick Status / Edit Trigger in Banner */}
            <div className="sm:self-end">
              <span className="text-xs font-semibold text-slate-500 block text-left sm:text-right">
                {isEditing ? (
                  <span className="inline-flex items-center gap-1 text-amber-600 bg-amber-50 px-2.5 py-1 rounded-lg border border-amber-200">
                    <span className="material-symbols-outlined text-[14px]">edit_note</span>
                    <span>Đang ở chế độ chỉnh sửa</span>
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-lg border border-emerald-200">
                    <span className="material-symbols-outlined text-[14px]">check_circle</span>
                    <span>Hồ sơ đã đồng bộ</span>
                  </span>
                )}
              </span>
            </div>
          </div>
        </div>

        {/* Main Grid: Left Sidebar (Meta & Completeness) + Right Content (View/Edit) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Left Column: Account Meta & Completeness (4 cols) */}
          <div className="lg:col-span-4 space-y-6">
            {/* 1. Profile Completeness Widget */}
            <div className="bg-surface-container-lowest p-6 rounded-3xl border border-outline-variant/70 shadow-xs space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-xs font-extrabold uppercase tracking-wider text-slate-900 m-0 flex items-center gap-1.5 font-display">
                  <span className="material-symbols-outlined text-primary text-[18px]">verified</span>
                  <span>Độ hoàn thiện hồ sơ</span>
                </h3>
                <span className="text-xs font-black text-primary font-display">
                  {completenessDetails.total}%
                </span>
              </div>

              {/* Progress Bar */}
              <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
                <div
                  className="bg-gradient-to-r from-primary to-secondary h-full rounded-full transition-all duration-500"
                  style={{ width: `${completenessDetails.total}%` }}
                />
              </div>

              {/* Checklist Items */}
              <div className="space-y-2 pt-1">
                {completenessDetails.checks.map((item) => (
                  <div key={item.id} className="flex items-center justify-between text-xs">
                    <span className="flex items-center gap-1.5 text-slate-700">
                      <span
                        className={`material-symbols-outlined text-[16px] ${
                          item.completed ? 'text-emerald-500' : 'text-slate-300'
                        }`}
                      >
                        {item.completed ? 'check_circle' : 'radio_button_unchecked'}
                      </span>
                      <span className={item.completed ? 'font-medium' : 'text-slate-400'}>
                        {item.label}
                      </span>
                    </span>
                    <span
                      className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${
                        item.completed
                          ? 'bg-emerald-50 text-emerald-700'
                          : 'bg-slate-100 text-slate-400'
                      }`}
                    >
                      {item.completed ? '+20%' : 'Chưa có'}
                    </span>
                  </div>
                ))}
              </div>

              {completenessDetails.total < 100 && !isEditing && (
                <div className="pt-2">
                  <button
                    type="button"
                    onClick={() => {
                      if (profile) populateForm(profile);
                      setIsEditing(true);
                    }}
                    className="w-full bg-primary/10 hover:bg-primary/20 text-primary font-bold text-xs py-2 rounded-xl transition-colors flex items-center justify-center gap-1 cursor-pointer"
                  >
                    <span>Hoàn thiện ngay</span>
                    <span className="material-symbols-outlined text-[14px]">arrow_forward</span>
                  </button>
                </div>
              )}
            </div>

            {/* 2. Account Security & Metadata Widget */}
            <div className="bg-surface-container-lowest p-6 rounded-3xl border border-outline-variant/70 shadow-xs space-y-4">
              <h3 className="text-xs font-extrabold uppercase tracking-wider text-slate-900 m-0 flex items-center gap-1.5 font-display">
                <span className="material-symbols-outlined text-primary text-[18px]">shield</span>
                <span>Thông tin tài khoản & Bảo mật</span>
              </h3>

              <div className="space-y-3.5 text-xs">
                <div>
                  <span className="text-slate-500 block text-[11px] font-medium">Mã định danh hệ thống (User ID)</span>
                  <div className="flex items-center justify-between bg-surface-container-low p-2 rounded-xl mt-1 border border-outline-variant/60">
                    <span className="font-mono font-bold text-slate-800">#{profile?.id || '—'}</span>
                    {profile?.id && (
                      <button
                        type="button"
                        onClick={() => handleCopyUserId(profile.id)}
                        className="text-slate-500 hover:text-primary p-1 rounded transition-colors cursor-pointer flex items-center gap-1 text-[11px] font-bold"
                        title="Sao chép ID"
                      >
                        <span className="material-symbols-outlined text-[14px]">
                          {copiedId ? 'done' : 'content_copy'}
                        </span>
                        <span>{copiedId ? 'Đã chép' : 'Sao chép'}</span>
                      </button>
                    )}
                  </div>
                </div>

                <div>
                  <span className="text-slate-500 block text-[11px] font-medium">Nguồn xác thực (Provider)</span>
                  <span className="font-bold text-slate-800 flex items-center gap-1.5 mt-0.5">
                    <span className="material-symbols-outlined text-[16px] text-primary">verified_user</span>
                    <span>{profile?.authProvider || 'LOCAL'} Account</span>
                  </span>
                </div>

                <div>
                  <span className="text-slate-500 block text-[11px] font-medium">Trạng thái kích hoạt</span>
                  <span className="inline-flex items-center gap-1.5 font-bold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-lg text-[11px] border border-emerald-200 mt-0.5">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                    Đang hoạt động bình thường
                  </span>
                </div>

                {profile?.createdAt && (
                  <div>
                    <span className="text-slate-500 block text-[11px] font-medium">Ngày khởi tạo tài khoản</span>
                    <span className="font-semibold text-slate-800 mt-0.5 block">
                      {new Date(profile.createdAt).toLocaleDateString('vi-VN', {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric'
                      })}
                    </span>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Right Column: View Mode OR Edit Form (8 cols) */}
          <div className="lg:col-span-8 space-y-6">
            {isEditing ? (
              /* ------------------------------------------------------------- */
              /* EDIT MODE FORM (Course Settings Style)                        */
              /* ------------------------------------------------------------- */
              <form onSubmit={handleSaveProfile} className="space-y-6">
                {/* 1. Section 1: Avatar & Personal Info */}
                <div className="bg-surface-container-lowest p-6 sm:p-8 rounded-3xl border border-outline-variant/70 shadow-xs space-y-6">
                  <div>
                    <h3 className="text-base font-extrabold text-slate-900 m-0 font-display flex items-center gap-2">
                      <span className="material-symbols-outlined text-primary text-[22px]">badge</span>
                      <span>1. Ảnh đại diện & Thông tin cơ bản</span>
                    </h3>
                    <p className="text-xs text-slate-500 mt-1 m-0">
                      Cập nhật ảnh đại diện và tên hiển thị công khai trên hồ sơ và chứng chỉ khóa học.
                    </p>
                  </div>

                  {/* Avatar Uploader */}
                  <AvatarUploader
                    currentAvatarUrl={avatarPreview}
                    fullName={fullName || profile?.fullName}
                    onAvatarUploaded={handleAvatarUploaded}
                    size="md"
                  />

                  {/* Full Name Input */}
                  <div className="space-y-1.5">
                    <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                      Họ và tên hiển thị <span className="text-rose-500">*</span>
                    </label>
                    <input
                      type="text"
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                      placeholder="Nhập họ và tên đầy đủ của bạn..."
                      className="w-full px-4 py-3 bg-surface-container-low border border-outline-variant/70 rounded-2xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all font-semibold"
                      required
                    />
                    <p className="text-[11px] text-slate-500 m-0">
                      Tên này sẽ hiển thị trên tất cả các khóa học, chứng chỉ và bài đánh giá của bạn.
                    </p>
                  </div>

                  {/* Email Read-only */}
                  <div className="space-y-1.5">
                    <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                      Địa chỉ Email tài khoản
                    </label>
                    <input
                      type="email"
                      value={profile?.email || ''}
                      disabled
                      className="w-full px-4 py-3 bg-slate-100 border border-slate-200 rounded-2xl text-xs text-slate-500 font-mono cursor-not-allowed"
                    />
                    <p className="text-[11px] text-slate-400 m-0">
                      Email đăng ký được bảo mật và không thể tự ý thay đổi trực tiếp.
                    </p>
                  </div>
                </div>

                {/* 2. Section 2: Expertise & Bio */}
                <div className="bg-surface-container-lowest p-6 sm:p-8 rounded-3xl border border-outline-variant/70 shadow-xs space-y-6">
                  <div>
                    <h3 className="text-base font-extrabold text-slate-900 m-0 font-display flex items-center gap-2">
                      <span className="material-symbols-outlined text-primary text-[22px]">psychology</span>
                      <span>2. Lĩnh vực chuyên môn & Giới thiệu bản thân</span>
                    </h3>
                    <p className="text-xs text-slate-500 mt-1 m-0">
                      Giúp giảng viên và học viên khác hiểu rõ hơn về thế mạnh và định hướng của bạn.
                    </p>
                  </div>

                  {/* Expertise Input + Suggestions */}
                  <div className="space-y-2">
                    <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                      Lĩnh vực chuyên môn / Vị trí hiện tại
                    </label>
                    <input
                      type="text"
                      value={expertise}
                      onChange={(e) => setExpertise(e.target.value)}
                      placeholder="Ví dụ: Kỹ sư Fullstack, Giảng viên Lập trình Java, Thiết kế UI/UX..."
                      className="w-full px-4 py-3 bg-surface-container-low border border-outline-variant/70 rounded-2xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all font-semibold"
                    />

                    {/* Quick suggestion chips */}
                    <div className="space-y-1.5 pt-1">
                      <span className="text-[11px] text-slate-500 font-medium block">
                        Gợi ý nhanh (nhấn để chọn):
                      </span>
                      <div className="flex flex-wrap gap-1.5">
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
                  </div>

                  {/* Bio Textarea with Live Counter */}
                  <div className="space-y-1.5">
                    <div className="flex items-center justify-between">
                      <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                        Tiểu sử & Đôi nét về bản thân (Bio)
                      </label>
                      <span className="text-[11px] font-mono text-slate-400">
                        {bio.length} ký tự
                      </span>
                    </div>
                    <textarea
                      rows={5}
                      value={bio}
                      onChange={(e) => setBio(e.target.value)}
                      placeholder="Chia sẻ về kinh nghiệm làm việc, phong cách học tập, mục tiêu phát triển nghề nghiệp hoặc câu chuyện truyền cảm hứng của bạn..."
                      className="w-full px-4 py-3 bg-surface-container-low border border-outline-variant/70 rounded-2xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all leading-relaxed resize-none"
                    />
                    <p className="text-[11px] text-slate-500 m-0">
                      Mẹo: Một đoạn giới thiệu chân thành và cụ thể sẽ tăng khả năng kết nối trong cộng đồng học tập.
                    </p>
                  </div>
                </div>

                {/* 3. Section 3: Interests & Skills */}
                <div className="bg-surface-container-lowest p-6 sm:p-8 rounded-3xl border border-outline-variant/70 shadow-xs space-y-6">
                  <div>
                    <h3 className="text-base font-extrabold text-slate-900 m-0 font-display flex items-center gap-2">
                      <span className="material-symbols-outlined text-primary text-[22px]">interests</span>
                      <span>3. Lĩnh vực quan tâm & Kỹ năng yêu thích</span>
                    </h3>
                    <p className="text-xs text-slate-500 mt-1 m-0">
                      Hệ thống sẽ gợi ý các khóa học và chủ đề phù hợp nhất với sở thích của bạn.
                    </p>
                  </div>

                  {/* Selected Tags list */}
                  {selectedInterests.length > 0 && (
                    <div className="space-y-1.5">
                      <span className="text-xs font-bold text-slate-800 block">
                        Các chủ đề đã chọn ({selectedInterests.length}):
                      </span>
                      <div className="flex flex-wrap gap-2">
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
                              title="Gỡ chủ đề này"
                            >
                              <span className="material-symbols-outlined text-[12px]">close</span>
                            </button>
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Preset Pills */}
                  <div className="space-y-2">
                    <span className="text-xs font-bold text-slate-800 block">
                      Chọn nhanh từ danh sách phổ biến:
                    </span>
                    <div className="flex flex-wrap gap-2">
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
                  </div>

                  {/* Custom interest tag input */}
                  <div className="space-y-1.5 pt-2 border-t border-slate-100">
                    <label className="block text-xs font-bold text-slate-700">
                      Thêm chủ đề / kỹ năng khác
                    </label>
                    <div className="flex items-center gap-2">
                      <input
                        type="text"
                        value={customInterestInput}
                        onChange={(e) => setCustomInterestInput(e.target.value)}
                        onKeyDown={handleAddCustomInterest}
                        placeholder="Nhập tên chủ đề khác rồi nhấn Enter hoặc bấm Thêm..."
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
                </div>

                {/* Sticky Bottom Save Action Bar (Matching CourseSettingsPage) */}
                <div className="sticky bottom-4 z-20 bg-slate-900 text-white p-4 rounded-2xl shadow-2xl flex items-center justify-between gap-4 border border-slate-700 backdrop-blur-md">
                  <div className="text-xs text-slate-300 hidden sm:block">
                    <span>Bạn đang ở chế độ chỉnh sửa hồ sơ. Nhớ bấm Lưu để cập nhật.</span>
                  </div>

                  <div className="flex items-center gap-3 w-full sm:w-auto justify-end">
                    <button
                      type="button"
                      onClick={() => {
                        if (profile) populateForm(profile);
                        setIsEditing(false);
                      }}
                      disabled={isSaving}
                      className="px-4 py-2.5 rounded-xl text-xs font-bold text-slate-300 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
                    >
                      Hủy bỏ
                    </button>

                    <button
                      type="submit"
                      disabled={isSaving}
                      className="bg-primary hover:bg-primary/90 text-white font-extrabold text-xs px-6 py-2.5 rounded-xl transition-all shadow-lg shadow-primary/30 flex items-center gap-2 cursor-pointer active:scale-95 disabled:opacity-50"
                    >
                      {isSaving && (
                        <span className="inline-block animate-spin w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full" />
                      )}
                      <span className="material-symbols-outlined text-[18px]">save</span>
                      <span>LƯU THAY ĐỔI HỒ SƠ</span>
                    </button>
                  </div>
                </div>
              </form>
            ) : (
              /* ------------------------------------------------------------- */
              /* VIEW MODE (Clean, Structured Display Cards)                   */
              /* ------------------------------------------------------------- */
              <div className="space-y-6">
                {/* 1. Personal & Contact Card */}
                <div className="bg-surface-container-lowest p-6 sm:p-8 rounded-3xl border border-outline-variant/70 shadow-xs space-y-6">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                    <div>
                      <h3 className="text-base font-extrabold text-slate-900 m-0 font-display flex items-center gap-2">
                        <span className="material-symbols-outlined text-primary text-[22px]">badge</span>
                        <span>Thông tin định danh</span>
                      </h3>
                      <p className="text-xs text-slate-500 mt-0.5 m-0">
                        Thông tin tài khoản chính thức được ghi nhận trên Learnova.
                      </p>
                    </div>

                    <button
                      type="button"
                      onClick={() => {
                        if (profile) populateForm(profile);
                        setIsEditing(true);
                      }}
                      className="text-primary hover:bg-primary/10 text-xs font-bold px-3 py-1.5 rounded-xl transition-colors cursor-pointer flex items-center gap-1 border border-primary/20"
                    >
                      <span className="material-symbols-outlined text-[16px]">edit</span>
                      <span>Chỉnh sửa</span>
                    </button>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="bg-surface-container-low p-4 rounded-2xl border border-outline-variant/60">
                      <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500 block mb-1">
                        Họ và tên đầy đủ
                      </span>
                      <p className="text-sm font-extrabold text-slate-900 m-0">
                        {profile?.fullName || 'Chưa cập nhật'}
                      </p>
                    </div>

                    <div className="bg-surface-container-low p-4 rounded-2xl border border-outline-variant/60">
                      <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500 block mb-1">
                        Địa chỉ Email
                      </span>
                      <p className="text-sm font-bold text-slate-800 m-0 truncate">
                        {profile?.email || '—'}
                      </p>
                    </div>

                    <div className="bg-surface-container-low p-4 rounded-2xl border border-outline-variant/60">
                      <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500 block mb-1">
                        Vai trò hệ thống
                      </span>
                      <p className="text-xs font-bold text-slate-800 m-0 flex items-center gap-1.5">
                        <span className="material-symbols-outlined text-[16px] text-primary">school</span>
                        <span>{roleLabel[profile?.role || 'LEARNER']}</span>
                      </p>
                    </div>

                    <div className="bg-surface-container-low p-4 rounded-2xl border border-outline-variant/60">
                      <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500 block mb-1">
                        Cổng đăng nhập
                      </span>
                      <p className="text-xs font-bold text-slate-800 m-0 flex items-center gap-1.5">
                        <span className="material-symbols-outlined text-[16px] text-primary">login</span>
                        <span>{profile?.authProvider || 'LOCAL'} Authenticator</span>
                      </p>
                    </div>
                  </div>
                </div>

                {/* 2. Expertise & Bio Card */}
                <div className="bg-surface-container-lowest p-6 sm:p-8 rounded-3xl border border-outline-variant/70 shadow-xs space-y-6">
                  <div className="border-b border-slate-100 pb-4">
                    <h3 className="text-base font-extrabold text-slate-900 m-0 font-display flex items-center gap-2">
                      <span className="material-symbols-outlined text-primary text-[22px]">psychology</span>
                      <span>Lĩnh vực chuyên môn & Giới thiệu bản thân</span>
                    </h3>
                    <p className="text-xs text-slate-500 mt-0.5 m-0">
                      Hồ sơ năng lực và câu chuyện cá nhân của bạn.
                    </p>
                  </div>

                  <div className="space-y-4">
                    <div>
                      <span className="text-xs font-bold text-slate-700 block mb-1.5">
                        Lĩnh vực chuyên môn / Nghề nghiệp:
                      </span>
                      {profile?.expertise ? (
                        <div className="inline-flex items-center gap-2 bg-primary/10 text-primary font-bold px-4 py-2 rounded-xl text-xs border border-primary/20">
                          <span className="material-symbols-outlined text-[18px]">work</span>
                          <span>{profile.expertise}</span>
                        </div>
                      ) : (
                        <p className="text-xs text-slate-400 italic bg-surface-container-low p-3.5 rounded-2xl border border-outline-variant/60 m-0">
                          Chưa cập nhật lĩnh vực chuyên môn.
                        </p>
                      )}
                    </div>

                    <div>
                      <span className="text-xs font-bold text-slate-700 block mb-1.5">
                        Tiểu sử & Giới thiệu (Bio):
                      </span>
                      {profile?.bio ? (
                        <div className="bg-surface-container-low p-5 rounded-2xl border border-outline-variant/60 text-xs text-slate-800 leading-relaxed whitespace-pre-line relative font-normal">
                          <span className="material-symbols-outlined text-primary/30 text-[32px] absolute top-3 right-3 select-none">
                            format_quote
                          </span>
                          {profile.bio}
                        </div>
                      ) : (
                        <p className="text-xs text-slate-400 italic bg-surface-container-low p-3.5 rounded-2xl border border-outline-variant/60 m-0">
                          Chưa có thông tin giới thiệu bản thân.
                        </p>
                      )}
                    </div>
                  </div>
                </div>

                {/* 3. Interests & Skills Card */}
                <div className="bg-surface-container-lowest p-6 sm:p-8 rounded-3xl border border-outline-variant/70 shadow-xs space-y-6">
                  <div className="border-b border-slate-100 pb-4">
                    <h3 className="text-base font-extrabold text-slate-900 m-0 font-display flex items-center gap-2">
                      <span className="material-symbols-outlined text-primary text-[22px]">interests</span>
                      <span>Lĩnh vực quan tâm & Kỹ năng theo đuổi</span>
                    </h3>
                    <p className="text-xs text-slate-500 mt-0.5 m-0">
                      Các chủ đề học tập được ưu tiên đề xuất cho bạn trên trang Khám phá khóa học.
                    </p>
                  </div>

                  {displayInterests.length > 0 ? (
                    <div className="flex flex-wrap gap-2.5">
                      {displayInterests.map((interest, idx) => (
                        <span
                          key={idx}
                          className="bg-surface-container-low hover:bg-surface-container text-on-surface font-bold text-xs px-3.5 py-2 rounded-xl border border-outline-variant/70 shadow-xs transition-colors"
                        >
                          {interest}
                        </span>
                      ))}
                    </div>
                  ) : (
                    <div className="text-center py-6 bg-surface-container-low rounded-2xl border border-outline-variant/60 space-y-2">
                      <span className="material-symbols-outlined text-[32px] text-slate-400">category</span>
                      <p className="text-xs text-slate-500 m-0">
                        Bạn chưa chọn chủ đề hoặc kỹ năng quan tâm nào.
                      </p>
                      <button
                        type="button"
                        onClick={() => {
                          if (profile) populateForm(profile);
                          setIsEditing(true);
                        }}
                        className="text-primary font-bold text-xs hover:underline cursor-pointer"
                      >
                        + Bổ sung sở thích ngay
                      </button>
                    </div>
                  )}
                </div>

                {/* Quick Edit CTA Footer */}
                <div className="p-6 bg-gradient-to-r from-surface-container-low via-surface-container to-surface-container-high rounded-3xl border border-outline-variant/70 flex flex-col sm:flex-row items-center justify-between gap-4">
                  <div className="space-y-0.5 text-center sm:text-left">
                    <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider m-0">
                      Bạn muốn cập nhật thêm thông tin?
                    </h4>
                    <p className="text-xs text-slate-500 m-0">
                      Chỉnh sửa lại ảnh đại diện, chức danh, mô tả hoặc các chủ đề yêu thích bất kỳ lúc nào.
                    </p>
                  </div>

                  <button
                    type="button"
                    onClick={() => {
                      if (profile) populateForm(profile);
                      setIsEditing(true);
                    }}
                    className="bg-primary hover:bg-primary/90 text-white font-extrabold text-xs px-5 py-2.5 rounded-xl transition-all shadow-md shadow-primary/20 flex items-center gap-1.5 cursor-pointer shrink-0"
                  >
                    <span className="material-symbols-outlined text-[16px]">edit</span>
                    <span>Chỉnh sửa hồ sơ</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

