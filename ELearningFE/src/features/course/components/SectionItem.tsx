import React, { useState, useRef } from 'react';
import type { LessonContentType, Section } from '../../../types/course';
import { curriculumApi } from '../api/curriculumApi';
import { instructorCourseApi } from '../api/instructorCourseApi';
import { apiClient } from '../../../lib/apiClient';
import { useToast } from '../../../app/context/ToastContext';
import { LessonItem } from './LessonItem';
import { ConfirmDialog } from '../../../components/common/ConfirmDialog';
import { Modal } from '../../../components/common/Modal';

interface SectionItemProps {
  courseId: number;
  section: Section;
  index: number;
  totalSections: number;
  allSections: Section[];
  onCurriculumChanged: () => void;
  onMoveSectionUp?: () => void;
  onMoveSectionDown?: () => void;
}

export const SectionItem: React.FC<SectionItemProps> = ({
  courseId,
  section,
  index,
  totalSections,
  allSections,
  onCurriculumChanged,
  onMoveSectionUp,
  onMoveSectionDown
}) => {
  const { showSuccess, showError } = useToast();

  // Section Edit State
  const [isEditingSection, setIsEditingSection] = useState(false);
  const [sectionTitle, setSectionTitle] = useState(section.title);
  const [sectionDesc, setSectionDesc] = useState(section.description || '');

  // Delete section dialog
  const [isDeleting, setIsDeleting] = useState(false);

  // Add Lesson State
  const [showAddLessonForm, setShowAddLessonForm] = useState(false);
  const [newLessonTitle, setNewLessonTitle] = useState('');
  const [newLessonType, setNewLessonType] = useState<LessonContentType>('VIDEO');
  const [isCreatingLesson, setIsCreatingLesson] = useState(false);

  // Lesson Content Inline Form States
  // 1. Video
  const [videoFile, setVideoFile] = useState<File | null>(null);
  const [videoDuration, setVideoDuration] = useState<number>(300);
  const videoInputRef = useRef<HTMLInputElement>(null);

  // 2. Article
  const [articleContent, setArticleContent] = useState<string>('');

  // 3. File (Document)
  const [docFile, setDocFile] = useState<File | null>(null);
  const docInputRef = useRef<HTMLInputElement>(null);

  // 4. Preview Flag
  const [isPreview, setIsPreview] = useState<boolean>(false);

  // Upload progress
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatusText, setUploadStatusText] = useState('');

  const resetForm = () => {
    setNewLessonTitle('');
    setNewLessonType('VIDEO');
    setVideoFile(null);
    setVideoDuration(300);
    setArticleContent('');
    setDocFile(null);
    setIsPreview(false);
    setUploadProgress(0);
    setUploadStatusText('');
    setShowAddLessonForm(false);
  };

  const handleUpdateSection = async () => {
    if (!sectionTitle.trim()) return;
    try {
      await curriculumApi.updateSection(courseId, section.id, {
        title: sectionTitle.trim(),
        description: sectionDesc.trim() || undefined
      });
      setIsEditingSection(false);
      showSuccess('Đã cập nhật thông tin chương học!');
      onCurriculumChanged();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi cập nhật chương học.');
    }
  };

  const handleDeleteSection = async () => {
    try {
      await curriculumApi.deleteSection(courseId, section.id);
      setIsDeleting(false);
      showSuccess('Đã xóa chương học.');
      onCurriculumChanged();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi xóa chương học.');
    }
  };

  const handleCreateLessonWithContent = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newLessonTitle.trim()) {
      showError('Vui lòng nhập tiêu đề bài học.');
      return;
    }

    setIsCreatingLesson(true);
    setUploadStatusText('Đang khởi tạo bài học...');

    try {
      // 1. Create Lesson record
      const createdLesson = await curriculumApi.addLesson(courseId, section.id, {
        title: newLessonTitle.trim(),
        contentType: newLessonType
      });

      // Update preview flag if checked
      if (isPreview) {
        await curriculumApi.updateLesson(courseId, createdLesson.id, {
          isPreview: true
        });
      }

      // 2. Attach Content directly based on type
      if (newLessonType === 'VIDEO' && videoFile) {
        if (!videoFile.type.includes('mp4')) {
          showError('Video bài giảng phải có định dạng MP4 (BR-05).');
          setIsCreatingLesson(false);
          return;
        }
        if (videoFile.size > 500 * 1024 * 1024) {
          showError('Dung lượng video tối đa là 500MB (BR-05).');
          setIsCreatingLesson(false);
          return;
        }

        setUploadStatusText('Đang lấy URL tải lên trực tiếp (Presigned URL)...');
        const presign = await instructorCourseApi.presignUpload({
          purpose: 'LESSON_VIDEO',
          courseId,
          lessonId: createdLesson.id,
          fileName: videoFile.name,
          contentType: videoFile.type || 'video/mp4',
          sizeBytes: videoFile.size
        });

        setUploadStatusText(`Đang tải video lên (${Math.round(videoFile.size / 1024 / 1024)} MB)...`);
        await apiClient.uploadDirect(
          presign.uploadUrl,
          videoFile,
          videoFile.type || 'video/mp4',
          (percent) => {
            setUploadProgress(percent);
          }
        );

        setUploadStatusText('Đang gắn video vào bài giảng...');
        await curriculumApi.attachContent(courseId, createdLesson.id, {
          storageKey: presign.storageKey,
          originalFileName: videoFile.name,
          fileSizeBytes: videoFile.size,
          mimeType: videoFile.type || 'video/mp4',
          durationSeconds: videoDuration
        });
      } else if (newLessonType === 'ARTICLE' && articleContent.trim()) {
        setUploadStatusText('Đang lưu nội dung bài viết...');
        await curriculumApi.updateLesson(courseId, createdLesson.id, {
          contentText: articleContent.trim()
        });
      } else if (newLessonType === 'FILE' && docFile) {
        if (docFile.size > 200 * 1024 * 1024) {
          showError('Dung lượng tài liệu tối đa là 200MB.');
          setIsCreatingLesson(false);
          return;
        }

        setUploadStatusText('Đang lấy URL tải tài liệu...');
        const presign = await instructorCourseApi.presignUpload({
          purpose: 'LESSON_FILE',
          courseId,
          lessonId: createdLesson.id,
          fileName: docFile.name,
          contentType: docFile.type || 'application/pdf',
          sizeBytes: docFile.size
        });

        setUploadStatusText(`Đang tải tệp lên (${Math.round(docFile.size / 1024 / 1024)} MB)...`);
        await apiClient.uploadDirect(
          presign.uploadUrl,
          docFile,
          docFile.type || 'application/pdf',
          (percent) => {
            setUploadProgress(percent);
          }
        );

        setUploadStatusText('Đang gắn tệp vào bài giảng...');
        await curriculumApi.attachContent(courseId, createdLesson.id, {
          storageKey: presign.storageKey,
          originalFileName: docFile.name,
          fileSizeBytes: docFile.size,
          mimeType: docFile.type || 'application/pdf'
        });
      }

      showSuccess(`🎉 Đã tạo và lưu nội dung bài học "${newLessonTitle.trim()}" thành công!`);
      resetForm();
      onCurriculumChanged();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi tạo và lưu bài học.');
    } finally {
      setIsCreatingLesson(false);
      setUploadProgress(0);
      setUploadStatusText('');
    }
  };

  const handleReorderLessons = async (fromIdx: number, toIdx: number) => {
    const updated = [...section.lessons];
    const [moved] = updated.splice(fromIdx, 1);
    updated.splice(toIdx, 0, moved);

    const lessonIds = updated.map((l) => l.id);
    try {
      await curriculumApi.reorderLessons(courseId, section.id, lessonIds);
      onCurriculumChanged();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi sắp xếp lại bài học.');
    }
  };

  return (
    <>
      <div className="bg-surface-container-lowest border border-outline-variant/70 rounded-2xl overflow-hidden shadow-xs space-y-4 p-5">
        {/* Section Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
          <div className="flex items-center gap-3 overflow-hidden">
            {/* Move Section Buttons */}
            <div className="flex flex-col shrink-0">
              <button
                type="button"
                disabled={index === 0}
                onClick={onMoveSectionUp}
                className="text-slate-400 hover:text-primary disabled:opacity-20 p-0.5 cursor-pointer"
                title="Di chuyển chương lên"
              >
                <span className="material-symbols-outlined text-[16px]">arrow_upward</span>
              </button>
              <button
                type="button"
                disabled={index === totalSections - 1}
                onClick={onMoveSectionDown}
                className="text-slate-400 hover:text-primary disabled:opacity-20 p-0.5 cursor-pointer"
                title="Di chuyển chương xuống"
              >
                <span className="material-symbols-outlined text-[16px]">arrow_downward</span>
              </button>
            </div>

            <div className="overflow-hidden space-y-0.5">
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-black uppercase tracking-wider bg-primary/10 text-primary px-2 py-0.5 rounded">
                  Chương {index + 1}
                </span>
                <h3 className="text-sm font-extrabold text-slate-900 m-0 truncate font-display">
                  {section.title}
                </h3>
              </div>
              {section.description && (
                <p className="text-xs text-slate-500 line-clamp-1 m-0">{section.description}</p>
              )}
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
            <span className="text-xs text-slate-400 font-semibold mr-1">
              {section.lessons.length} bài học
            </span>

            <button
              type="button"
              onClick={() => {
                setSectionTitle(section.title);
                setSectionDesc(section.description || '');
                setIsEditingSection(true);
              }}
              className="p-1.5 text-slate-400 hover:text-primary hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
              title="Chỉnh sửa thông tin chương"
            >
              <span className="material-symbols-outlined text-[18px]">edit</span>
            </button>

            <button
              type="button"
              onClick={() => setIsDeleting(true)}
              className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
              title="Xóa chương học"
            >
              <span className="material-symbols-outlined text-[18px]">delete</span>
            </button>
          </div>
        </div>

        {/* Section Lessons List */}
        <div className="space-y-2.5">
          {section.lessons.length === 0 ? (
            <div className="text-center py-6 bg-surface-container-low/60 rounded-xl border border-dashed border-outline-variant/60 text-xs text-slate-400">
              Chương này chưa có bài học nào. Hãy bấm &quot;Thêm bài học mới&quot; bên dưới.
            </div>
          ) : (
            section.lessons.map((lesson, lIdx) => (
              <LessonItem
                key={lesson.id}
                courseId={courseId}
                sectionId={section.id}
                lesson={lesson}
                index={lIdx}
                totalLessons={section.lessons.length}
                allSections={allSections}
                onCurriculumChanged={onCurriculumChanged}
                onMoveUp={() => lIdx > 0 && handleReorderLessons(lIdx, lIdx - 1)}
                onMoveDown={() =>
                  lIdx < section.lessons.length - 1 && handleReorderLessons(lIdx, lIdx + 1)
                }
              />
            ))
          )}
        </div>

        {/* Add Lesson Action or Integrated All-in-One Form */}
        {!showAddLessonForm ? (
          <div className="pt-2">
            <button
              type="button"
              onClick={() => setShowAddLessonForm(true)}
              className="w-full py-2.5 bg-surface-container-low hover:bg-surface-container text-primary font-bold text-xs rounded-xl border border-dashed border-primary/40 transition-all flex items-center justify-center gap-1.5 cursor-pointer shadow-xs"
            >
              <span className="material-symbols-outlined text-[18px]">add_circle</span>
              <span>Thêm bài học mới vào chương này</span>
            </button>
          </div>
        ) : (
          <form
            onSubmit={handleCreateLessonWithContent}
            className="p-5 bg-surface-container-low border-2 border-primary/40 rounded-2xl space-y-4 animate-in fade-in shadow-sm"
          >
            <div className="flex items-center justify-between border-b border-outline-variant/50 pb-3">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[20px]">
                  post_add
                </span>
                <span className="text-xs font-black text-slate-900 uppercase tracking-wider font-display">
                  Thêm & Soạn Bài Học Mới (All-In-One Form)
                </span>
              </div>
              <button
                type="button"
                onClick={resetForm}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg hover:bg-slate-200/60 cursor-pointer"
              >
                <span className="material-symbols-outlined text-[18px]">close</span>
              </button>
            </div>

            {/* Basic Info: Title & Format */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="sm:col-span-2 space-y-1">
                <label className="block text-[11px] font-bold text-slate-700 uppercase">
                  Tiêu đề bài học <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  value={newLessonTitle}
                  onChange={(e) => setNewLessonTitle(e.target.value)}
                  placeholder="vd: 1.1 Tổng quan về kiến trúc MVC và Dependency Injection..."
                  className="w-full px-3.5 py-2.5 bg-white border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:border-primary focus:outline-none"
                  autoFocus
                  required
                />
              </div>

              <div className="space-y-1">
                <label className="block text-[11px] font-bold text-slate-700 uppercase">
                  Định dạng bài học
                </label>
                <select
                  value={newLessonType}
                  onChange={(e) => setNewLessonType(e.target.value as LessonContentType)}
                  className="w-full px-3.5 py-2.5 bg-white border border-outline-variant/70 rounded-xl text-xs text-on-surface font-bold focus:border-primary focus:outline-none"
                >
                  <option value="VIDEO">📹 Video bài giảng (MP4)</option>
                  <option value="ARTICLE">📝 Bài viết lý thuyết (Text)</option>
                  <option value="FILE">📄 Tệp tài liệu (PDF, ZIP, DOCX)</option>
                  <option value="QUIZ">❓ Bài trắc nghiệm (Quiz)</option>
                </select>
              </div>
            </div>

            {/* Dynamic Content Sub-form Based on Format */}
            <div className="bg-white p-4 rounded-xl border border-outline-variant/60 space-y-3">
              
              {/* VIDEO SUBFORM (Default) */}
              {newLessonType === 'VIDEO' && (
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                      <span className="material-symbols-outlined text-primary text-[18px]">movie</span>
                      <span>Chọn file Video MP4 (Tối đa 500MB, BR-05)</span>
                    </span>
                    {videoFile && (
                      <span className="text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded">
                        Đã chọn: {videoFile.name} ({Math.round(videoFile.size / 1024 / 1024)} MB)
                      </span>
                    )}
                  </div>

                  <input
                    type="file"
                    ref={videoInputRef}
                    onChange={(e) => {
                      if (e.target.files?.[0]) setVideoFile(e.target.files[0]);
                    }}
                    accept="video/mp4"
                    className="hidden"
                  />

                  <div className="flex flex-col sm:flex-row items-center gap-3">
                    <button
                      type="button"
                      onClick={() => videoInputRef.current?.click()}
                      className="w-full sm:w-auto bg-primary/10 hover:bg-primary text-primary hover:text-white font-bold text-xs px-4 py-2.5 rounded-xl transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                    >
                      <span className="material-symbols-outlined text-[18px]">upload_file</span>
                      <span>{videoFile ? 'Đổi tệp Video' : 'Chọn tệp Video MP4'}</span>
                    </button>

                    <div className="flex items-center gap-2 w-full sm:w-auto">
                      <label className="text-xs font-semibold text-slate-600 whitespace-nowrap">
                        Thời lượng (giây):
                      </label>
                      <input
                        type="number"
                        value={videoDuration}
                        onChange={(e) => setVideoDuration(Number(e.target.value))}
                        min={1}
                        className="w-28 px-3 py-1.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs"
                      />
                    </div>
                  </div>
                </div>
              )}

              {/* ARTICLE SUBFORM */}
              {newLessonType === 'ARTICLE' && (
                <div className="space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                      <span className="material-symbols-outlined text-primary text-[18px]">article</span>
                      <span>Soạn thảo nội dung bài viết lý thuyết</span>
                    </span>
                    <span className="text-[11px] text-slate-400">{articleContent.length} ký tự</span>
                  </div>
                  <textarea
                    rows={6}
                    value={articleContent}
                    onChange={(e) => setArticleContent(e.target.value)}
                    placeholder="Viết nội dung bài giảng, lý thuyết, giải thích mã nguồn hoặc các ghi chú kiến thức cho học viên..."
                    className="w-full px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none leading-relaxed font-mono"
                  />
                </div>
              )}

              {/* FILE SUBFORM */}
              {newLessonType === 'FILE' && (
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                      <span className="material-symbols-outlined text-primary text-[18px]">description</span>
                      <span>Tải lên tệp tài liệu PDF, DOCX, ZIP (Tối đa 200MB)</span>
                    </span>
                    {docFile && (
                      <span className="text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded">
                        Đã chọn: {docFile.name} ({Math.round(docFile.size / 1024 / 1024)} MB)
                      </span>
                    )}
                  </div>

                  <input
                    type="file"
                    ref={docInputRef}
                    onChange={(e) => {
                      if (e.target.files?.[0]) setDocFile(e.target.files[0]);
                    }}
                    accept=".pdf,.docx,.doc,.pptx,.ppt,.zip"
                    className="hidden"
                  />

                  <button
                    type="button"
                    onClick={() => docInputRef.current?.click()}
                    className="w-full sm:w-auto bg-primary/10 hover:bg-primary text-primary hover:text-white font-bold text-xs px-4 py-2.5 rounded-xl transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <span className="material-symbols-outlined text-[18px]">upload</span>
                    <span>{docFile ? 'Đổi tệp tài liệu' : 'Chọn tệp tài liệu để tải lên'}</span>
                  </button>
                </div>
              )}

              {/* QUIZ SUBFORM */}
              {newLessonType === 'QUIZ' && (
                <div className="p-3 text-xs text-slate-500 bg-surface-container-low rounded-xl">
                  Bài học dạng trắc nghiệm (Quiz) sẽ được khởi tạo và sẵn sàng để bạn thêm ngân hàng câu hỏi.
                </div>
              )}

              {/* Preview Toggle Checkbox */}
              <div className="pt-2 border-t border-slate-100 flex items-center gap-2">
                <input
                  type="checkbox"
                  id={`preview-check-${section.id}`}
                  checked={isPreview}
                  onChange={(e) => setIsPreview(e.target.checked)}
                  className="w-4 h-4 text-primary rounded cursor-pointer"
                />
                <label
                  htmlFor={`preview-check-${section.id}`}
                  className="text-xs font-bold text-slate-700 cursor-pointer"
                >
                  Cho phép học viên Học thử miễn phí bài này (Preview)
                </label>
              </div>
            </div>

            {/* Upload Progress Bar if active */}
            {isCreatingLesson && (
              <div className="space-y-2 bg-slate-900 text-white p-3.5 rounded-xl animate-in fade-in">
                <div className="flex justify-between text-xs font-bold">
                  <span>{uploadStatusText}</span>
                  {uploadProgress > 0 && <span className="text-indigo-400">{uploadProgress}%</span>}
                </div>
                {uploadProgress > 0 && (
                  <div className="w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
                    <div
                      className="bg-primary-container h-full rounded-full transition-all duration-300"
                      style={{ width: `${uploadProgress}%` }}
                    />
                  </div>
                )}
              </div>
            )}

            {/* Action Buttons */}
            <div className="flex justify-end gap-2 pt-1">
              <button
                type="button"
                onClick={resetForm}
                disabled={isCreatingLesson}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 bg-slate-200/70 hover:bg-slate-200 cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="submit"
                disabled={isCreatingLesson}
                className="px-5 py-2 rounded-xl text-xs font-extrabold text-white bg-primary hover:bg-primary/90 shadow-md shadow-primary/20 flex items-center gap-1.5 cursor-pointer active:scale-95"
              >
                {isCreatingLesson && (
                  <span className="inline-block animate-spin w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full" />
                )}
                <span>Hoàn tất & Thêm vào giáo trình</span>
              </button>
            </div>
          </form>
        )}
      </div>

      {/* Edit Section Modal */}
      {isEditingSection && (
        <Modal
          isOpen={isEditingSection}
          onClose={() => setIsEditingSection(false)}
          title="Chỉnh sửa thông tin chương học"
          maxWidth="sm"
          icon="edit"
        >
          <div className="space-y-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                Tiêu đề chương
              </label>
              <input
                type="text"
                value={sectionTitle}
                onChange={(e) => setSectionTitle(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none"
                placeholder="Nhập tiêu đề chương..."
                autoFocus
                required
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                Mô tả chương (Tùy chọn)
              </label>
              <textarea
                rows={3}
                value={sectionDesc}
                onChange={(e) => setSectionDesc(e.target.value)}
                className="w-full px-3.5 py-2 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none"
                placeholder="Tóm tắt nội dung học tập của chương này..."
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setIsEditingSection(false)}
                className="px-3.5 py-2 rounded-xl text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 cursor-pointer"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={handleUpdateSection}
                className="px-4 py-2 rounded-xl text-xs font-bold text-white bg-primary hover:bg-primary/90 cursor-pointer"
              >
                Lưu thay đổi
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Delete Confirmation */}
      {isDeleting && (
        <ConfirmDialog
          isOpen={isDeleting}
          onClose={() => setIsDeleting(false)}
          onConfirm={handleDeleteSection}
          title="Xóa chương học"
          message={`Bạn có chắc chắn muốn xóa chương "${section.title}" cùng toàn bộ các bài học bên trong không?`}
          confirmText="Xóa chương học"
          isDestructive
        />
      )}
    </>
  );
};
