import React, { useState, useEffect } from 'react';
import type { CourseDetail, CourseLevel } from '../../../../types/course';
import type { Category } from '../../../../types/category';
import { instructorCourseApi } from '../../api/instructorCourseApi';
import { useToast } from '../../../../app/context/ToastContext';
import { BulletsEditor } from '../../components/BulletsEditor';
import { ThumbnailUploader } from '../../components/ThumbnailUploader';
import { PublishCheckModal } from '../../components/PublishCheckModal';
import { StatusBadge } from '../../../../components/common/Badge';
import { formatCurrencyVND } from '../../../../lib/formatters';

interface CourseSettingsPageProps {
  courseId: number;
  categories: Category[];
  onBack: () => void;
  onGoToCurriculum: (courseId: number) => void;
}

export const CourseSettingsPage: React.FC<CourseSettingsPageProps> = ({
  courseId,
  categories,
  onBack,
  onGoToCurriculum
}) => {
  const { showSuccess, showError } = useToast();

  const [course, setCourse] = useState<CourseDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  // Form State: General
  const [title, setTitle] = useState('');
  const [subtitle, setSubtitle] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState<number>(1);
  const [level, setLevel] = useState<CourseLevel>('ALL');
  const [language, setLanguage] = useState('vi');

  // Form State: Pricing (BR-04: 0 - 10,000,000 VND)
  const [price, setPrice] = useState<number>(0);
  const [isFree, setIsFree] = useState<boolean>(true);

  // Form State: 3 Bullets
  const [learningObjectives, setLearningObjectives] = useState<string[]>(['', '', '', '']);
  const [requirements, setRequirements] = useState<string[]>(['']);
  const [targetAudiences, setTargetAudiences] = useState<string[]>(['']);

  // Publish Check Modal
  const [showPublishCheck, setShowPublishCheck] = useState(false);

  const fetchDetail = async () => {
    setIsLoading(true);
    try {
      const data = await instructorCourseApi.getDetail(courseId);
      setCourse(data);

      setTitle(data.title || '');
      setSubtitle(data.subtitle || '');
      setDescription(data.description || '');
      setCategoryId(data.categoryId || categories[0]?.id || 1);
      setLevel(data.level || 'ALL');
      setLanguage(data.language || 'vi');

      setPrice(data.price || 0);
      setIsFree(!data.price || data.price === 0);

      setLearningObjectives(
        data.learningObjectives && data.learningObjectives.length > 0
          ? data.learningObjectives
          : ['', '', '', '']
      );
      setRequirements(
        data.requirements && data.requirements.length > 0
          ? data.requirements
          : ['']
      );
      setTargetAudiences(
        data.targetAudiences && data.targetAudiences.length > 0
          ? data.targetAudiences
          : ['']
      );
    } catch (err: any) {
      showError(err.message || 'Lỗi khi tải thông tin khóa học.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDetail();
  }, [courseId]);

  const handleSaveAll = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    try {
      // 1. Update General Info (PATCH)
      await instructorCourseApi.updateCourse(courseId, {
        title: title.trim(),
        subtitle: subtitle.trim() || undefined,
        description: description.trim() || undefined,
        categoryId: Number(categoryId),
        level,
        language
      });

      // 2. Update Price (PUT)
      const numericPrice = isFree ? 0 : Number(price);
      if (numericPrice < 0 || numericPrice > 10000000) {
        showError('Giá khóa học phải nằm trong khoảng từ 0 đến 10.000.000 VNĐ (BR-04).');
        setIsSaving(false);
        return;
      }
      await instructorCourseApi.updatePrice(courseId, { price: numericPrice });

      // 3. Update Bullets (PUT)
      const cleanObjectives = learningObjectives.filter((o) => o.trim().length > 0);
      const cleanReqs = requirements.filter((r) => r.trim().length > 0);
      const cleanAuds = targetAudiences.filter((a) => a.trim().length > 0);

      await instructorCourseApi.updateBullets(courseId, {
        learningObjectives: cleanObjectives,
        requirements: cleanReqs,
        targetAudiences: cleanAuds
      });

      showSuccess('🎉 Đã lưu toàn bộ thông tin khóa học thành công!');
      fetchDetail();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi lưu thông tin khóa học.');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading || !course) {
    return (
      <div className="flex-1 flex items-center justify-center bg-background min-h-0">
        <div className="text-center">
          <span className="inline-block animate-spin w-8 h-8 border-3 border-primary border-t-transparent rounded-full" />
          <p className="text-xs text-slate-500 font-bold mt-3">Đang tải cài đặt khóa học...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col bg-background min-h-0 overflow-y-auto">
      {/* Top Action Header */}
      <header className="bg-surface-container-lowest border-b border-outline-variant/70 px-6 sm:px-8 py-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 sticky top-0 z-20 backdrop-blur-md shrink-0">
        <div className="flex items-center gap-3 overflow-hidden">
          <button
            type="button"
            onClick={onBack}
            className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer shrink-0"
            title="Quay lại danh sách khóa học"
          >
            <span className="material-symbols-outlined text-[20px]">arrow_back</span>
          </button>

          <div className="overflow-hidden">
            <div className="flex items-center gap-2">
              <StatusBadge status={course.status} />
              <span className="text-xs text-slate-400 font-semibold">• ID: #{course.id}</span>
            </div>
            <h1 className="text-base sm:text-lg font-black text-slate-900 truncate m-0 mt-0.5 font-display">
              {course.title}
            </h1>
          </div>
        </div>

        {/* Action Header Buttons */}
        <div className="flex items-center gap-2.5 shrink-0 self-end sm:self-center">
          <button
            type="button"
            onClick={() => onGoToCurriculum(course.id)}
            className="bg-surface-container-low hover:bg-surface-container text-primary font-bold text-xs px-4 py-2.5 rounded-xl border border-primary/30 transition-all flex items-center gap-1.5 cursor-pointer shadow-xs"
          >
            <span className="material-symbols-outlined text-[18px]">menu_book</span>
            <span>Chuyển sang Soạn giáo trình (US-06)</span>
          </button>

          <button
            type="button"
            onClick={() => setShowPublishCheck(true)}
            className="bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold text-xs px-4 py-2.5 rounded-xl transition-all shadow-md shadow-emerald-200 flex items-center gap-1.5 cursor-pointer active:scale-95"
          >
            <span className="material-symbols-outlined text-[18px]">publish</span>
            <span>Kiểm tra xuất bản</span>
          </button>
        </div>
      </header>

      {/* Main Settings Form Body */}
      <form onSubmit={handleSaveAll} className="p-6 sm:p-8 max-w-5xl mx-auto w-full space-y-8">
        {/* 1. Landing Page Information (US-05) */}
        <div className="bg-surface-container-lowest p-6 sm:p-8 rounded-3xl border border-outline-variant/70 shadow-xs space-y-6">
          <div>
            <h3 className="text-base font-extrabold text-slate-900 m-0 font-display flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[22px]">feed</span>
              <span>1. Thông tin giới thiệu khóa học (Landing Page)</span>
            </h3>
            <p className="text-xs text-slate-500 mt-1 m-0">
              Thông tin hiển thị tại trang chi tiết để học viên tìm kiếm và đăng ký.
            </p>
          </div>

          <div className="space-y-4">
            {/* Title */}
            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                Tiêu đề khóa học <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Nhập tiêu đề khóa học..."
                className="w-full px-4 py-3 bg-surface-container-low border border-outline-variant/70 rounded-2xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all font-semibold"
                required
              />
            </div>

            {/* Subtitle */}
            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                Phụ đề ngắn gọn
              </label>
              <input
                type="text"
                value={subtitle}
                onChange={(e) => setSubtitle(e.target.value)}
                placeholder="Tóm tắt 1 câu lôi cuốn về giá trị nổi bật của khóa học..."
                className="w-full px-4 py-3 bg-surface-container-low border border-outline-variant/70 rounded-2xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all"
              />
            </div>

            {/* Category, Level, Language */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Danh mục
                </label>
                <select
                  value={categoryId}
                  onChange={(e) => setCategoryId(Number(e.target.value))}
                  className="w-full px-4 py-3 bg-surface-container-low border border-outline-variant/70 rounded-2xl text-xs text-on-surface font-bold focus:bg-white focus:border-primary focus:outline-none"
                >
                  {categories.map((cat) => (
                    <option key={cat.id} value={cat.id}>
                      {cat.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Cấp độ
                </label>
                <select
                  value={level}
                  onChange={(e) => setLevel(e.target.value as CourseLevel)}
                  className="w-full px-4 py-3 bg-surface-container-low border border-outline-variant/70 rounded-2xl text-xs text-on-surface font-bold focus:bg-white focus:border-primary focus:outline-none"
                >
                  <option value="ALL">Mọi cấp độ</option>
                  <option value="BEGINNER">Cơ bản (Beginner)</option>
                  <option value="INTERMEDIATE">Trung cấp (Intermediate)</option>
                  <option value="EXPERT">Nâng cao (Expert)</option>
                </select>
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Ngôn ngữ giảng dạy
                </label>
                <select
                  value={language}
                  onChange={(e) => setLanguage(e.target.value)}
                  className="w-full px-4 py-3 bg-surface-container-low border border-outline-variant/70 rounded-2xl text-xs text-on-surface font-bold focus:bg-white focus:border-primary focus:outline-none"
                >
                  <option value="vi">Tiếng Việt</option>
                  <option value="en">English (Tiếng Anh)</option>
                </select>
              </div>
            </div>

            {/* Description with live char counter */}
            <div className="space-y-1.5">
              <div className="flex justify-between items-center">
                <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Mô tả chi tiết khóa học <span className="text-rose-500">*</span>
                </label>
                <span
                  className={`text-[11px] font-bold px-2 py-0.5 rounded-full ${
                    description.trim().length >= 200
                      ? 'bg-emerald-50 text-emerald-700'
                      : 'bg-amber-50 text-amber-700'
                  }`}
                >
                  {description.trim().length}/200 ký tự tối thiểu
                </span>
              </div>
              <textarea
                rows={6}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Mô tả chi tiết kiến thức, kỹ năng, dự án thực hành và phương pháp học trong khóa học..."
                className="w-full px-4 py-3 bg-surface-container-low border border-outline-variant/70 rounded-2xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none transition-all leading-relaxed"
                required
              />
              {description.trim().length < 200 && (
                <p className="text-[11px] text-amber-600 font-medium m-0 flex items-center gap-1">
                  <span className="material-symbols-outlined text-[14px]">info</span>
                  <span>
                    Lưu ý: Mô tả cần đạt <strong>tối thiểu 200 ký tự</strong> để vượt qua bước kiểm tra xuất bản (Publish Check).
                  </span>
                </p>
              )}
            </div>

            {/* Thumbnail Uploader */}
            <div className="pt-4 border-t border-slate-100">
              <ThumbnailUploader
                courseId={course.id}
                currentThumbnailUrl={course.thumbnailUrl}
                onThumbnailUpdated={(url) => {
                  setCourse((prev) => (prev ? { ...prev, thumbnailUrl: url } : prev));
                }}
              />
            </div>
          </div>
        </div>

        {/* 2. Pricing Settings (US-05) */}
        <div className="bg-surface-container-lowest p-6 sm:p-8 rounded-3xl border border-outline-variant/70 shadow-xs space-y-6">
          <div>
            <h3 className="text-base font-extrabold text-slate-900 m-0 font-display flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[22px]">payments</span>
              <span>2. Định giá bán khóa học (Pricing)</span>
            </h3>
            <p className="text-xs text-slate-500 mt-1 m-0">
              Quy định mức học phí khóa học theo quy định hệ thống (0 – 10.000.000 VNĐ, BR-04).
            </p>
          </div>

          <div className="space-y-4 max-w-md">
            {/* Free toggle */}
            <div className="flex items-center gap-3 bg-surface-container-low p-3.5 rounded-2xl border border-outline-variant/60">
              <input
                type="checkbox"
                id="isFreeCheckbox"
                checked={isFree}
                onChange={(e) => {
                  setIsFree(e.target.checked);
                  if (e.target.checked) setPrice(0);
                }}
                className="w-4 h-4 text-primary rounded cursor-pointer"
              />
              <label htmlFor="isFreeCheckbox" className="text-xs font-bold text-slate-800 cursor-pointer">
                Khóa học miễn phí (0 VNĐ)
              </label>
            </div>

            {/* Price Input */}
            {!isFree && (
              <div className="space-y-1.5 animate-in fade-in">
                <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Mức giá bán (VNĐ)
                </label>
                <div className="relative">
                  <input
                    type="number"
                    value={price}
                    onChange={(e) => setPrice(Number(e.target.value))}
                    min={0}
                    max={10000000}
                    step={10000}
                    placeholder="vd: 499000"
                    className="w-full px-4 py-3 bg-surface-container-low border border-outline-variant/70 rounded-2xl text-sm font-bold text-primary focus:bg-white focus:border-primary focus:outline-none"
                    required
                  />
                  <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-extrabold text-slate-400">
                    VNĐ
                  </span>
                </div>
                <p className="text-xs text-slate-500 m-0">
                  Hiển thị thực tế: <strong className="text-primary font-bold">{formatCurrencyVND(price)}</strong>
                </p>
              </div>
            )}
          </div>
        </div>

        {/* 3. Three Highlight Bullets (US-05) */}
        <div className="bg-surface-container-lowest p-6 sm:p-8 rounded-3xl border border-outline-variant/70 shadow-xs space-y-6">
          <div>
            <h3 className="text-base font-extrabold text-slate-900 m-0 font-display flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[22px]">checklist</span>
              <span>3. Khối mục tiêu, yêu cầu & đối tượng (Course Highlights)</span>
            </h3>
            <p className="text-xs text-slate-500 mt-1 m-0">
              Tạo sự rõ ràng và chuyên nghiệp cho trang khóa học của bạn.
            </p>
          </div>

          <BulletsEditor
            learningObjectives={learningObjectives}
            onChangeLearningObjectives={setLearningObjectives}
            requirements={requirements}
            onChangeRequirements={setRequirements}
            targetAudiences={targetAudiences}
            onChangeTargetAudiences={setTargetAudiences}
          />
        </div>

        {/* Save Floating / Bottom Bar */}
        <div className="sticky bottom-4 z-20 bg-slate-900 text-white p-4 rounded-2xl shadow-2xl flex items-center justify-between gap-4 border border-slate-700 backdrop-blur-md">
          <div className="text-xs text-slate-300 hidden sm:block">
            <span>Hãy lưu lại các thay đổi trước khi chuyển sang bước tiếp theo.</span>
          </div>

          <div className="flex items-center gap-3 w-full sm:w-auto justify-end">
            <button
              type="button"
              onClick={onBack}
              className="px-4 py-2.5 rounded-xl text-xs font-bold text-slate-300 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
            >
              Quay lại danh sách
            </button>

            <button
              type="submit"
              disabled={isSaving}
              className="bg-primary hover:bg-primary/90 text-white font-extrabold text-xs px-6 py-2.5 rounded-xl transition-all shadow-lg shadow-primary/30 flex items-center gap-2 cursor-pointer active:scale-95"
            >
              {isSaving && (
                <span className="inline-block animate-spin w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full" />
              )}
              <span className="material-symbols-outlined text-[18px]">save</span>
              <span>LƯU CÀI ĐẶT KHÓA HỌC</span>
            </button>
          </div>
        </div>
      </form>

      {/* Publish Check Modal */}
      {showPublishCheck && (
        <PublishCheckModal
          isOpen={showPublishCheck}
          onClose={() => setShowPublishCheck(false)}
          courseId={course.id}
          onPublishSuccess={() => fetchDetail()}
        />
      )}
    </div>
  );
};
