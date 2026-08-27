import React, { useState } from 'react';
import type { Lesson, Section } from '../../../types/course';
import { curriculumApi } from '../api/curriculumApi';
import { useToast } from '../../../app/context/ToastContext';
import { formatDuration, formatFileSize } from '../../../lib/formatters';
import { LessonContentModal } from './LessonContentModal';
import { ConfirmDialog } from '../../../components/common/ConfirmDialog';
import { Modal } from '../../../components/common/Modal';

interface LessonItemProps {
  courseId: number;
  sectionId: number;
  lesson: Lesson;
  index: number;
  totalLessons: number;
  allSections: Section[];
  onCurriculumChanged: () => void;
  onMoveUp?: () => void;
  onMoveDown?: () => void;
}

export const LessonItem: React.FC<LessonItemProps> = ({
  courseId,
  sectionId,
  lesson,
  index,
  totalLessons,
  allSections,
  onCurriculumChanged,
  onMoveUp,
  onMoveDown
}) => {
  const { showSuccess, showError } = useToast();

  // Edit title modal
  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const [editTitle, setEditTitle] = useState(lesson.title);

  // Content modal
  const [isContentModalOpen, setIsContentModalOpen] = useState(false);

  // Delete dialog
  const [isDeleting, setIsDeleting] = useState(false);

  // Move to section modal
  const [isMoveModalOpen, setIsMoveModalOpen] = useState(false);
  const [targetSectionId, setTargetSectionId] = useState<number>(
    allSections.find((s) => s.id !== sectionId)?.id || sectionId
  );

  const handleUpdateTitle = async () => {
    if (!editTitle.trim()) return;
    try {
      await curriculumApi.updateLesson(courseId, lesson.id, {
        title: editTitle.trim()
      });
      setIsEditingTitle(false);
      showSuccess('Đã đổi tiêu đề bài học!');
      onCurriculumChanged();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi cập nhật tiêu đề.');
    }
  };

  const handleTogglePreview = async () => {
    try {
      await curriculumApi.updateLesson(courseId, lesson.id, {
        isPreview: !lesson.isPreview
      });
      showSuccess(
        !lesson.isPreview
          ? 'Đã đặt bài học ở chế độ Học thử miễn phí.'
          : 'Đã hủy chế độ Học thử miễn phí.'
      );
      onCurriculumChanged();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi thay đổi chế độ xem thử.');
    }
  };

  const handleDelete = async () => {
    try {
      await curriculumApi.deleteLesson(courseId, lesson.id);
      setIsDeleting(false);
      showSuccess('Đã xóa bài học.');
      onCurriculumChanged();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi xóa bài học.');
    }
  };

  const handleMoveLesson = async () => {
    if (targetSectionId === sectionId) {
      setIsMoveModalOpen(false);
      return;
    }
    try {
      await curriculumApi.moveLesson(courseId, lesson.id, targetSectionId, 1);
      setIsMoveModalOpen(false);
      showSuccess('Đã di chuyển bài học sang chương mới!');
      onCurriculumChanged();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi di chuyển bài học.');
    }
  };

  const getContentTypeIcon = () => {
    switch (lesson.contentType) {
      case 'VIDEO':
        return 'play_circle';
      case 'ARTICLE':
        return 'article';
      case 'FILE':
        return 'description';
      case 'QUIZ':
        return 'quiz';
      default:
        return 'description';
    }
  };

  return (
    <>
      <div className="bg-white border border-slate-200 rounded-xl p-3.5 hover:shadow-xs transition-all flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs">
        {/* Left: Icon, Number, Title & Badges */}
        <div className="flex items-center gap-3 overflow-hidden flex-1">
          {/* Move Reorder Buttons */}
          <div className="flex flex-col shrink-0">
            <button
              type="button"
              disabled={index === 0}
              onClick={onMoveUp}
              className="text-slate-400 hover:text-primary disabled:opacity-20 disabled:hover:text-slate-400 p-0.5 cursor-pointer"
              title="Di chuyển lên"
            >
              <span className="material-symbols-outlined text-[14px]">arrow_upward</span>
            </button>
            <button
              type="button"
              disabled={index === totalLessons - 1}
              onClick={onMoveDown}
              className="text-slate-400 hover:text-primary disabled:opacity-20 disabled:hover:text-slate-400 p-0.5 cursor-pointer"
              title="Di chuyển xuống"
            >
              <span className="material-symbols-outlined text-[14px]">arrow_downward</span>
            </button>
          </div>

          <div className="w-8 h-8 rounded-lg bg-surface-container-low text-primary flex items-center justify-center shrink-0">
            <span className="material-symbols-outlined text-[18px]">
              {getContentTypeIcon()}
            </span>
          </div>

          <div className="overflow-hidden space-y-0.5 flex-1">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="font-bold text-slate-900 truncate">
                Bài {index + 1}: {lesson.title}
              </span>
              <button
                type="button"
                onClick={() => {
                  setEditTitle(lesson.title);
                  setIsEditingTitle(true);
                }}
                className="text-slate-400 hover:text-primary p-0.5 cursor-pointer"
                title="Đổi tiêu đề bài học"
              >
                <span className="material-symbols-outlined text-[14px]">edit</span>
              </button>
            </div>

            <div className="flex items-center gap-2 text-[11px] text-slate-500 flex-wrap">
              <span className="font-semibold uppercase tracking-wider text-[10px] text-primary">
                {lesson.contentType}
              </span>

              {lesson.contentType === 'VIDEO' && (
                <span>• {formatDuration(lesson.durationSeconds)}</span>
              )}
              {lesson.contentType === 'FILE' && lesson.fileSizeBytes && (
                <span>• {formatFileSize(lesson.fileSizeBytes)}</span>
              )}
              {lesson.resources && lesson.resources.length > 0 && (
                <span className="text-indigo-600 font-semibold">
                  • {lesson.resources.length} tài liệu đính kèm
                </span>
              )}

              {/* Ready status */}
              {lesson.uploadStatus === 'READY' ? (
                <span className="text-emerald-700 font-semibold">• Đã có nội dung</span>
              ) : (
                <span className="text-amber-600 font-semibold">• Chưa có nội dung</span>
              )}
            </div>
          </div>
        </div>

        {/* Right: Actions */}
        <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
          {/* Preview Toggle */}
          <button
            type="button"
            onClick={handleTogglePreview}
            className={`px-2.5 py-1 rounded-lg text-[10px] font-extrabold uppercase tracking-wider transition-colors cursor-pointer border ${
              lesson.isPreview
                ? 'bg-primary/10 text-primary border-primary/20 hover:bg-primary/15'
                : 'bg-slate-100 text-slate-500 border-slate-200 hover:bg-slate-200'
            }`}
            title="Cho phép học viên học thử bài này mà không cần mua"
          >
            {lesson.isPreview ? 'Học thử: BẬT' : 'Học thử: TẮT'}
          </button>

          {/* Edit Content Button */}
          <button
            type="button"
            onClick={() => setIsContentModalOpen(true)}
            className="bg-primary/10 hover:bg-primary text-primary hover:text-white font-bold text-xs px-3 py-1.5 rounded-xl transition-all flex items-center gap-1 cursor-pointer"
          >
            <span className="material-symbols-outlined text-[16px]">edit_note</span>
            <span>Soạn nội dung</span>
          </button>

          {/* Move to another section */}
          {allSections.length > 1 && (
            <button
              type="button"
              onClick={() => setIsMoveModalOpen(true)}
              className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
              title="Di chuyển sang chương khác"
            >
              <span className="material-symbols-outlined text-[18px]">drive_file_move</span>
            </button>
          )}

          {/* Delete Lesson */}
          <button
            type="button"
            onClick={() => setIsDeleting(true)}
            className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
            title="Xóa bài học"
          >
            <span className="material-symbols-outlined text-[18px]">delete</span>
          </button>
        </div>
      </div>

      {/* Edit Title Modal */}
      {isEditingTitle && (
        <Modal
          isOpen={isEditingTitle}
          onClose={() => setIsEditingTitle(false)}
          title="Đổi tiêu đề bài học"
          maxWidth="sm"
          icon="edit"
        >
          <div className="space-y-4">
            <input
              type="text"
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              className="w-full px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none"
              placeholder="Nhập tiêu đề mới..."
              autoFocus
            />
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setIsEditingTitle(false)}
                className="px-3.5 py-2 rounded-xl text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 cursor-pointer"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={handleUpdateTitle}
                className="px-4 py-2 rounded-xl text-xs font-bold text-white bg-primary hover:bg-primary/90 cursor-pointer"
              >
                Lưu thay đổi
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Move Lesson Modal */}
      {isMoveModalOpen && (
        <Modal
          isOpen={isMoveModalOpen}
          onClose={() => setIsMoveModalOpen(false)}
          title="Di chuyển bài học sang chương khác"
          maxWidth="sm"
          icon="drive_file_move"
        >
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                Chọn chương đích
              </label>
              <select
                value={targetSectionId}
                onChange={(e) => setTargetSectionId(Number(e.target.value))}
                className="w-full px-3.5 py-2.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none"
              >
                {allSections.map((s) => (
                  <option key={s.id} value={s.id} disabled={s.id === sectionId}>
                    {s.title} {s.id === sectionId ? '(Hiện tại)' : ''}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setIsMoveModalOpen(false)}
                className="px-3.5 py-2 rounded-xl text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 cursor-pointer"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={handleMoveLesson}
                className="px-4 py-2 rounded-xl text-xs font-bold text-white bg-primary hover:bg-primary/90 cursor-pointer"
              >
                Di chuyển ngay
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Content Modal */}
      {isContentModalOpen && (
        <LessonContentModal
          isOpen={isContentModalOpen}
          onClose={() => setIsContentModalOpen(false)}
          courseId={courseId}
          lesson={lesson}
          onContentUpdated={onCurriculumChanged}
        />
      )}

      {/* Delete Confirmation */}
      {isDeleting && (
        <ConfirmDialog
          isOpen={isDeleting}
          onClose={() => setIsDeleting(false)}
          onConfirm={handleDelete}
          title="Xóa bài học"
          message={`Bạn có chắc muốn xóa bài học "${lesson.title}" không? Hành động này không thể hoàn tác.`}
          confirmText="Xóa bài học"
          isDestructive
        />
      )}
    </>
  );
};
