import React, { useState, useEffect } from 'react';
import type { CourseDetail, Curriculum } from '../../../../types/course';
import { curriculumApi } from '../../api/curriculumApi';
import { instructorCourseApi } from '../../api/instructorCourseApi';
import { useToast } from '../../../../app/context/ToastContext';
import { SectionItem } from '../../components/SectionItem';
import { PublishCheckModal } from '../../components/PublishCheckModal';
import { StatusBadge } from '../../../../components/common/Badge';
import { formatDuration } from '../../../../lib/formatters';

interface CurriculumEditorPageProps {
  courseId: number;
  onBack: () => void;
  onGoToSettings: (courseId: number) => void;
}

export const CurriculumEditorPage: React.FC<CurriculumEditorPageProps> = ({
  courseId,
  onBack,
  onGoToSettings
}) => {
  const { showSuccess, showError } = useToast();

  const [course, setCourse] = useState<CourseDetail | null>(null);
  const [curriculum, setCurriculum] = useState<Curriculum | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Add Section Form
  const [showAddSectionForm, setShowAddSectionForm] = useState(false);
  const [newSectionTitle, setNewSectionTitle] = useState('');
  const [newSectionDesc, setNewSectionDesc] = useState('');
  const [isCreatingSection, setIsCreatingSection] = useState(false);

  // Publish Check Modal
  const [showPublishCheck, setShowPublishCheck] = useState(false);

  // Silent update that updates state without blocking UI with full spinner
  const refreshCurriculumSilent = async () => {
    try {
      const curriculumData = await curriculumApi.getCurriculum(courseId);
      setCurriculum(curriculumData);
    } catch (err: any) {
      showError(err.message || 'Lỗi khi cập nhật dữ liệu giáo trình.');
    }
  };

  const fetchCurriculumData = async () => {
    setIsLoading(true);
    try {
      const [courseData, curriculumData] = await Promise.all([
        instructorCourseApi.getDetail(courseId),
        curriculumApi.getCurriculum(courseId)
      ]);
      setCourse(courseData);
      setCurriculum(curriculumData);
    } catch (err: any) {
      showError(err.message || 'Lỗi khi tải dữ liệu giáo trình.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchCurriculumData();
  }, [courseId]);

  const handleCreateSection = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSectionTitle.trim()) return;

    setIsCreatingSection(true);
    try {
      await curriculumApi.addSection(courseId, {
        title: newSectionTitle.trim(),
        description: newSectionDesc.trim() || undefined
      });
      setNewSectionTitle('');
      setNewSectionDesc('');
      setShowAddSectionForm(false);
      showSuccess('Đã thêm chương học mới thành công!');
      // Update data immediately without full page spinner
      await refreshCurriculumSilent();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi tạo chương học.');
    } finally {
      setIsCreatingSection(false);
    }
  };

  const handleReorderSections = async (fromIdx: number, toIdx: number) => {
    if (!curriculum) return;
    const updated = [...curriculum.sections];
    const [moved] = updated.splice(fromIdx, 1);
    updated.splice(toIdx, 0, moved);

    // Optimistic UI update
    setCurriculum({ ...curriculum, sections: updated });

    const sectionIds = updated.map((s) => s.id);
    try {
      await curriculumApi.reorderSections(courseId, sectionIds);
    } catch (err: any) {
      showError(err.message || 'Lỗi khi đổi thứ tự chương học.');
      fetchCurriculumData();
    }
  };

  // Stats
  let totalLessons = 0;
  let totalDuration = 0;
  curriculum?.sections?.forEach((s) => {
    s.lessons?.forEach((l) => {
      totalLessons++;
      totalDuration += l.durationSeconds || 0;
    });
  });

  if (isLoading || !course) {
    return (
      <div className="flex-1 flex items-center justify-center bg-background min-h-0">
        <div className="text-center">
          <span className="inline-block animate-spin w-8 h-8 border-3 border-primary border-t-transparent rounded-full" />
          <p className="text-xs text-slate-500 font-bold mt-3">Đang tải giáo trình bài giảng...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col bg-background min-h-0 overflow-y-auto">
      {/* Top Header */}
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
              Soạn Giáo Trình: {course.title}
            </h1>
          </div>
        </div>

        {/* Action Header Buttons */}
        <div className="flex items-center gap-2.5 shrink-0 self-end sm:self-center">
          <button
            type="button"
            onClick={() => onGoToSettings(course.id)}
            className="bg-surface-container-low hover:bg-surface-container text-on-surface font-bold text-xs px-4 py-2.5 rounded-xl border border-outline-variant/60 transition-all flex items-center gap-1.5 cursor-pointer shadow-xs"
          >
            <span className="material-symbols-outlined text-[18px]">tune</span>
            <span>Cài đặt thông tin & Giá (US-05)</span>
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

      {/* Main Content Body */}
      <div className="p-6 sm:p-8 max-w-5xl mx-auto w-full space-y-6">
        {/* Curriculum Stats Banner */}
        <div className="bg-surface-container-lowest p-5 rounded-3xl border border-outline-variant/70 shadow-xs flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-6">
            <div>
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                Tổng chương học
              </span>
              <strong className="text-lg font-black text-slate-900 font-display">
                {curriculum?.sections.length || 0} chương
              </strong>
            </div>

            <div className="h-8 w-px bg-slate-200" />

            <div>
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                Tổng bài giảng (US-06)
              </span>
              <strong className="text-lg font-black text-slate-900 font-display">
                {totalLessons} bài học
              </strong>
            </div>

            <div className="h-8 w-px bg-slate-200" />

            <div>
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                Thời lượng Video
              </span>
              <strong className="text-lg font-black text-primary font-display">
                {formatDuration(totalDuration)}
              </strong>
            </div>
          </div>

          <button
            type="button"
            onClick={() => setShowAddSectionForm(true)}
            className="bg-primary hover:bg-primary/90 text-white font-bold text-xs px-4 py-2.5 rounded-xl transition-all shadow-md shadow-primary/20 flex items-center gap-1.5 cursor-pointer shrink-0"
          >
            <span className="material-symbols-outlined text-[18px]">add</span>
            <span>Thêm chương học mới</span>
          </button>
        </div>

        {/* Add Section Inline Form */}
        {showAddSectionForm && (
          <form
            onSubmit={handleCreateSection}
            className="bg-surface-container-lowest p-6 rounded-3xl border-2 border-primary/40 shadow-md space-y-4 animate-in fade-in"
          >
            <div className="flex justify-between items-center">
              <h4 className="text-sm font-extrabold text-slate-900 m-0 font-display flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[20px]">
                  create_new_folder
                </span>
                <span>Thêm Chương học mới</span>
              </h4>
              <button
                type="button"
                onClick={() => setShowAddSectionForm(false)}
                className="text-slate-400 hover:text-slate-600 p-0.5 cursor-pointer"
              >
                <span className="material-symbols-outlined text-[18px]">close</span>
              </button>
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                Tiêu đề chương <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={newSectionTitle}
                onChange={(e) => setNewSectionTitle(e.target.value)}
                placeholder="vd: Chương 1: Giới thiệu & Cài đặt môi trường..."
                className="w-full px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none"
                autoFocus
                required
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                Mô tả ngắn gọn (Tùy chọn)
              </label>
              <input
                type="text"
                value={newSectionDesc}
                onChange={(e) => setNewSectionDesc(e.target.value)}
                placeholder="vd: Nắm vững các công cụ và cài đặt JDK..."
                className="w-full px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setShowAddSectionForm(false)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="submit"
                disabled={isCreatingSection}
                className="px-5 py-2 rounded-xl text-xs font-bold text-white bg-primary hover:bg-primary/90 shadow-sm flex items-center gap-1.5 cursor-pointer"
              >
                {isCreatingSection && (
                  <span className="inline-block animate-spin w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full" />
                )}
                <span>Tạo chương học</span>
              </button>
            </div>
          </form>
        )}

        {/* Sections Hierarchy List */}
        <div className="space-y-6">
          {!curriculum?.sections || curriculum.sections.length === 0 ? (
            <div className="text-center py-16 bg-surface-container-lowest rounded-3xl border-2 border-dashed border-outline-variant/80 p-8 space-y-4">
              <span className="material-symbols-outlined text-[56px] text-outline">
                folder_open
              </span>
              <h3 className="text-base font-bold text-slate-900 m-0 font-display">
                Chưa có chương học nào được tạo
              </h3>
              <p className="text-xs text-slate-500 m-0">
                Hãy tạo chương học đầu tiên để bắt đầu thêm các bài học video, bài viết và tài liệu.
              </p>
              <button
                type="button"
                onClick={() => setShowAddSectionForm(true)}
                className="bg-primary hover:bg-primary/90 text-white text-xs font-bold px-5 py-2.5 rounded-full transition-all shadow-md shadow-primary/20 cursor-pointer"
              >
                Thêm chương học ngay
              </button>
            </div>
          ) : (
            curriculum.sections.map((section, sIdx) => (
              <SectionItem
                key={section.id}
                courseId={courseId}
                section={section}
                index={sIdx}
                totalSections={curriculum.sections.length}
                allSections={curriculum.sections}
                onCurriculumChanged={refreshCurriculumSilent}
                onMoveSectionUp={() => sIdx > 0 && handleReorderSections(sIdx, sIdx - 1)}
                onMoveSectionDown={() =>
                  sIdx < curriculum.sections.length - 1 && handleReorderSections(sIdx, sIdx + 1)
                }
              />
            ))
          )}
        </div>
      </div>

      {/* Publish Check Modal */}
      {showPublishCheck && (
        <PublishCheckModal
          isOpen={showPublishCheck}
          onClose={() => setShowPublishCheck(false)}
          courseId={course.id}
          onPublishSuccess={() => refreshCurriculumSilent()}
        />
      )}
    </div>
  );
};
