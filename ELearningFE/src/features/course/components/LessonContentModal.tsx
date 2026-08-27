import React, { useState, useRef } from 'react';
import { Modal } from '../../../components/common/Modal';
import type { Lesson, UploadPurpose } from '../../../types/course';
import { instructorCourseApi } from '../api/instructorCourseApi';
import { curriculumApi } from '../api/curriculumApi';
import { apiClient } from '../../../lib/apiClient';
import { useToast } from '../../../app/context/ToastContext';
import { formatDuration, formatFileSize } from '../../../lib/formatters';

interface LessonContentModalProps {
  isOpen: boolean;
  onClose: () => void;
  courseId: number;
  lesson: Lesson;
  onContentUpdated: () => void;
}

export const LessonContentModal: React.FC<LessonContentModalProps> = ({
  isOpen,
  onClose,
  courseId,
  lesson,
  onContentUpdated
}) => {
  const { showSuccess, showError } = useToast();

  // Active sub-tab: 'content' | 'resources'
  const [activeTab, setActiveTab] = useState<'content' | 'resources'>('content');

  // Article text state
  const [contentText, setContentText] = useState(lesson.contentText || '');
  const [isSavingText, setIsSavingText] = useState(false);

  // Video duration manual input state (if not auto-detected)
  const [videoDuration, setVideoDuration] = useState<number>(lesson.durationSeconds || 300);

  // Upload states
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatusText, setUploadStatusText] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Resource upload states
  const [resourceTitle, setResourceTitle] = useState('');
  const resourceFileInputRef = useRef<HTMLInputElement>(null);
  const [isUploadingResource, setIsUploadingResource] = useState(false);

  // ---------------------------------------------------------------------------
  // 1. SAVE ARTICLE CONTENT (TEXT)
  // ---------------------------------------------------------------------------
  const handleSaveArticleText = async () => {
    setIsSavingText(true);
    try {
      await curriculumApi.updateLesson(courseId, lesson.id, {
        contentText: contentText.trim()
      });
      showSuccess('Đã lưu nội dung bài viết thành công!');
      onContentUpdated();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi lưu bài viết.');
    } finally {
      setIsSavingText(false);
    }
  };

  // ---------------------------------------------------------------------------
  // 2. UPLOAD VIDEO OR FILE CONTENT VIA PRESIGNED URL
  // ---------------------------------------------------------------------------
  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    let purpose: UploadPurpose = 'LESSON_VIDEO';
    if (lesson.contentType === 'FILE') {
      purpose = 'LESSON_FILE';
    }

    // Validate video limits
    if (lesson.contentType === 'VIDEO') {
      if (!file.type.includes('mp4')) {
        showError('Video bài giảng phải có định dạng MP4 (BR-05).');
        return;
      }
      if (file.size > 500 * 1024 * 1024) {
        showError('Dung lượng video tối đa là 500MB (BR-05).');
        return;
      }
    }

    setIsUploading(true);
    setUploadProgress(10);
    setUploadStatusText('Đang khởi tạo Presigned URL upload...');

    try {
      // 1. Request Presigned URL from Backend
      const presign = await instructorCourseApi.presignUpload({
        purpose,
        courseId,
        lessonId: lesson.id,
        fileName: file.name,
        contentType: file.type || (lesson.contentType === 'VIDEO' ? 'video/mp4' : 'application/pdf'),
        sizeBytes: file.size
      });

      setUploadProgress(30);
      setUploadStatusText(`Đang tải tệp lên Storage (${Math.round(file.size / 1024 / 1024)} MB)...`);

      // 2. Direct binary PUT upload
      await apiClient.uploadDirect(
        presign.uploadUrl,
        file,
        file.type || (lesson.contentType === 'VIDEO' ? 'video/mp4' : 'application/pdf'),
        (percent) => {
          setUploadProgress(30 + Math.round(percent * 0.5));
        }
      );

      setUploadProgress(85);
      setUploadStatusText('Đang gắn nội dung và xác thực metadata...');

      // 3. Attach content metadata to lesson
      await curriculumApi.attachContent(courseId, lesson.id, {
        storageKey: presign.storageKey,
        originalFileName: file.name,
        fileSizeBytes: file.size,
        mimeType: file.type || (lesson.contentType === 'VIDEO' ? 'video/mp4' : 'application/pdf'),
        durationSeconds: lesson.contentType === 'VIDEO' ? videoDuration : undefined
      });

      setUploadProgress(100);
      setUploadStatusText('Hoàn tất tải lên thành công!');
      showSuccess(`Đã tải lên và gắn nội dung ${lesson.contentType} thành công!`);
      onContentUpdated();
    } catch (err: any) {
      showError(err.message || 'Lỗi trong quá trình upload file.');
    } finally {
      setIsUploading(false);
      setUploadProgress(0);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  // ---------------------------------------------------------------------------
  // 3. ADD SUPPLEMENTARY RESOURCE
  // ---------------------------------------------------------------------------
  const handleAddResource = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!resourceTitle.trim()) {
      showError('Vui lòng nhập tên tài liệu đính kèm trước khi chọn file.');
      return;
    }

    if (file.size > 100 * 1024 * 1024) {
      showError('Tài liệu đính kèm tối đa 100MB.');
      return;
    }

    setIsUploadingResource(true);
    try {
      // 1. Presign Upload URL for Resource
      const presign = await instructorCourseApi.presignUpload({
        purpose: 'LESSON_RESOURCE',
        courseId,
        lessonId: lesson.id,
        fileName: file.name,
        contentType: file.type || 'application/octet-stream',
        sizeBytes: file.size
      });

      // 2. Direct binary upload
      await apiClient.uploadDirect(
        presign.uploadUrl,
        file,
        file.type || 'application/octet-stream'
      );

      // 3. Attach Resource to Lesson
      await curriculumApi.addResource(courseId, lesson.id, {
        title: resourceTitle.trim(),
        storageKey: presign.storageKey,
        originalFileName: file.name,
        fileSizeBytes: file.size,
        mimeType: file.type || 'application/octet-stream'
      });

      showSuccess('Đã thêm tài liệu đính kèm thành công!');
      setResourceTitle('');
      onContentUpdated();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi tải tài liệu đính kèm.');
    } finally {
      setIsUploadingResource(false);
      if (resourceFileInputRef.current) resourceFileInputRef.current.value = '';
    }
  };

  const handleDeleteResource = async (resourceId: number) => {
    try {
      await curriculumApi.deleteResource(courseId, lesson.id, resourceId);
      showSuccess('Đã xóa tài liệu đính kèm.');
      onContentUpdated();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi xóa tài liệu.');
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Soạn nội dung: ${lesson.title}`}
      subtitle={`Định dạng bài học: ${lesson.contentType} • Trạng thái: ${lesson.uploadStatus || 'READY'}`}
      maxWidth="2xl"
      icon={
        lesson.contentType === 'VIDEO'
          ? 'play_circle'
          : lesson.contentType === 'ARTICLE'
          ? 'article'
          : 'description'
      }
    >
      <div className="space-y-6">
        {/* Tab Headers */}
        <div className="flex border-b border-slate-200 gap-6 text-xs font-bold">
          <button
            type="button"
            onClick={() => setActiveTab('content')}
            className={`pb-3 flex items-center gap-2 cursor-pointer transition-colors ${
              activeTab === 'content'
                ? 'border-b-2 border-primary text-primary'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <span className="material-symbols-outlined text-[18px]">edit_note</span>
            <span>Nội dung chính ({lesson.contentType})</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('resources')}
            className={`pb-3 flex items-center gap-2 cursor-pointer transition-colors ${
              activeTab === 'resources'
                ? 'border-b-2 border-primary text-primary'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <span className="material-symbols-outlined text-[18px]">attach_file</span>
            <span>Tài liệu bổ trợ ({lesson.resources?.length || 0})</span>
          </button>
        </div>

        {/* TAB 1: MAIN CONTENT */}
        {activeTab === 'content' && (
          <div className="space-y-5">
            {/* FORMAT: VIDEO */}
            {lesson.contentType === 'VIDEO' && (
              <div className="space-y-4">
                <div className="bg-surface-container-low p-4 rounded-2xl border border-outline-variant/60 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-800">
                      Tệp Video hiện tại
                    </span>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                        lesson.uploadStatus === 'READY'
                          ? 'bg-emerald-100 text-emerald-800'
                          : 'bg-amber-100 text-amber-800'
                      }`}
                    >
                      {lesson.uploadStatus === 'READY' ? 'Đã sẵn sàng' : 'Chưa có video'}
                    </span>
                  </div>

                  {lesson.originalFileName ? (
                    <div className="text-xs text-slate-700 flex items-center justify-between bg-white p-3 rounded-xl border border-slate-200">
                      <div className="flex items-center gap-2 overflow-hidden">
                        <span className="material-symbols-outlined text-primary text-[20px]">
                          movie
                        </span>
                        <span className="font-semibold truncate">{lesson.originalFileName}</span>
                      </div>
                      <span className="text-slate-500 shrink-0">
                        {formatFileSize(lesson.fileSizeBytes)} • {formatDuration(lesson.durationSeconds)}
                      </span>
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500 m-0">
                      Chưa tải lên file video nào cho bài học này.
                    </p>
                  )}
                </div>

                {/* Duration Setting */}
                <div className="flex items-center gap-3">
                  <label className="text-xs font-bold text-slate-700 whitespace-nowrap">
                    Thời lượng ước tính (giây):
                  </label>
                  <input
                    type="number"
                    value={videoDuration}
                    onChange={(e) => setVideoDuration(Number(e.target.value))}
                    min={1}
                    className="w-32 px-3 py-1.5 bg-surface-container-low border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none"
                  />
                  <span className="text-xs text-slate-500 font-medium">
                    (~{formatDuration(videoDuration)})
                  </span>
                </div>

                {/* Upload Button & Progress */}
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleFileUpload}
                  accept="video/mp4"
                  className="hidden"
                />

                {isUploading && (
                  <div className="space-y-2 bg-slate-900 text-white p-4 rounded-2xl">
                    <div className="flex justify-between text-xs font-bold">
                      <span>{uploadStatusText}</span>
                      <span className="text-indigo-400">{uploadProgress}%</span>
                    </div>
                    <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
                      <div
                        className="bg-primary-container h-full rounded-full transition-all duration-300"
                        style={{ width: `${uploadProgress}%` }}
                      />
                    </div>
                  </div>
                )}

                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={isUploading}
                  className="w-full bg-primary hover:bg-primary/90 text-white font-bold text-xs py-3 rounded-xl transition-all flex items-center justify-center gap-2 cursor-pointer shadow-md shadow-primary/20"
                >
                  <span className="material-symbols-outlined text-[18px]">upload_file</span>
                  <span>{lesson.originalFileName ? 'Tải lên video thay thế' : 'Chọn video MP4 để tải lên'}</span>
                </button>
              </div>
            )}

            {/* FORMAT: ARTICLE (TEXT) */}
            {lesson.contentType === 'ARTICLE' && (
              <div className="space-y-4">
                <div className="space-y-1.5">
                  <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                    Soạn thảo nội dung bài viết lý thuyết
                  </label>
                  <textarea
                    rows={12}
                    value={contentText}
                    onChange={(e) => setContentText(e.target.value)}
                    placeholder="Viết nội dung bài giảng, lý thuyết, đoạn code mẫu, hoặc các ghi chú kiến thức cho học viên..."
                    className="w-full px-4 py-3 bg-surface-container-low border border-outline-variant/70 rounded-2xl text-xs text-on-surface focus:bg-white focus:border-primary focus:outline-none leading-relaxed transition-all font-mono"
                  />
                  <div className="flex justify-between text-[11px] text-slate-500">
                    <span>Hỗ trợ định dạng Markdown và văn bản thuần túy</span>
                    <span>{contentText.length} ký tự</span>
                  </div>
                </div>

                <div className="flex justify-end">
                  <button
                    type="button"
                    onClick={handleSaveArticleText}
                    disabled={isSavingText}
                    className="bg-primary hover:bg-primary/90 text-white font-bold text-xs px-5 py-2.5 rounded-xl transition-all shadow-md shadow-primary/20 flex items-center gap-1.5 cursor-pointer"
                  >
                    {isSavingText && (
                      <span className="inline-block animate-spin w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full" />
                    )}
                    <span className="material-symbols-outlined text-[18px]">save</span>
                    <span>Lưu nội dung bài viết</span>
                  </button>
                </div>
              </div>
            )}

            {/* FORMAT: FILE (DOCUMENT) */}
            {lesson.contentType === 'FILE' && (
              <div className="space-y-4">
                <div className="bg-surface-container-low p-4 rounded-2xl border border-outline-variant/60 space-y-3">
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-800">
                    Tài liệu bài giảng hiện tại
                  </span>
                  {lesson.originalFileName ? (
                    <div className="text-xs text-slate-700 flex items-center justify-between bg-white p-3 rounded-xl border border-slate-200">
                      <div className="flex items-center gap-2 overflow-hidden">
                        <span className="material-symbols-outlined text-primary text-[20px]">
                          description
                        </span>
                        <span className="font-semibold truncate">{lesson.originalFileName}</span>
                      </div>
                      <span className="text-slate-500 shrink-0">
                        {formatFileSize(lesson.fileSizeBytes)}
                      </span>
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500 m-0">
                      Chưa tải lên tệp tài liệu nào.
                    </p>
                  )}
                </div>

                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleFileUpload}
                  accept=".pdf,.docx,.doc,.pptx,.ppt,.zip"
                  className="hidden"
                />

                {isUploading && (
                  <div className="space-y-2 bg-slate-900 text-white p-4 rounded-2xl">
                    <div className="flex justify-between text-xs font-bold">
                      <span>{uploadStatusText}</span>
                      <span className="text-indigo-400">{uploadProgress}%</span>
                    </div>
                    <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
                      <div
                        className="bg-primary-container h-full rounded-full transition-all duration-300"
                        style={{ width: `${uploadProgress}%` }}
                      />
                    </div>
                  </div>
                )}

                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={isUploading}
                  className="w-full bg-primary hover:bg-primary/90 text-white font-bold text-xs py-3 rounded-xl transition-all flex items-center justify-center gap-2 cursor-pointer shadow-md shadow-primary/20"
                >
                  <span className="material-symbols-outlined text-[18px]">upload</span>
                  <span>Tải lên tệp PDF / DOCX / ZIP (&lt;= 200MB)</span>
                </button>
              </div>
            )}
          </div>
        )}

        {/* TAB 2: SUPPLEMENTARY RESOURCES */}
        {activeTab === 'resources' && (
          <div className="space-y-5">
            {/* Add Resource Form */}
            <div className="bg-surface-container-low p-4 rounded-2xl border border-outline-variant/60 space-y-3">
              <h5 className="text-xs font-bold uppercase tracking-wider text-slate-800 m-0">
                Thêm tài liệu đính kèm mới
              </h5>
              <div className="flex flex-col sm:flex-row items-center gap-3">
                <input
                  type="text"
                  value={resourceTitle}
                  onChange={(e) => setResourceTitle(e.target.value)}
                  placeholder="Tên tài liệu (vd: File mã nguồn bài 1, Slides tóm tắt)..."
                  className="flex-1 px-3.5 py-2 bg-white border border-outline-variant/70 rounded-xl text-xs text-on-surface focus:border-primary focus:outline-none w-full"
                />

                <input
                  type="file"
                  ref={resourceFileInputRef}
                  onChange={handleAddResource}
                  className="hidden"
                />

                <button
                  type="button"
                  onClick={() => {
                    if (!resourceTitle.trim()) {
                      showError('Vui lòng nhập tên tài liệu trước khi chọn tệp.');
                      return;
                    }
                    resourceFileInputRef.current?.click();
                  }}
                  disabled={isUploadingResource}
                  className="bg-primary hover:bg-primary/90 text-white font-bold text-xs px-4 py-2 rounded-xl transition-all flex items-center gap-1.5 shrink-0 cursor-pointer shadow-sm"
                >
                  {isUploadingResource ? (
                    <span className="inline-block animate-spin w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full" />
                  ) : (
                    <span className="material-symbols-outlined text-[16px]">add</span>
                  )}
                  <span>Chọn tệp & Tải lên</span>
                </button>
              </div>
            </div>

            {/* List of Current Resources */}
            <div className="space-y-2">
              <h5 className="text-xs font-bold uppercase tracking-wider text-slate-800 m-0">
                Tài liệu đã đính kèm ({lesson.resources?.length || 0})
              </h5>

              {!lesson.resources || lesson.resources.length === 0 ? (
                <p className="text-xs text-slate-400 italic py-3 m-0">
                  Chưa có tài liệu đính kèm nào cho bài học này.
                </p>
              ) : (
                <div className="divide-y divide-slate-100 border border-slate-200 rounded-2xl overflow-hidden bg-white">
                  {lesson.resources.map((res) => (
                    <div key={res.id} className="p-3 flex items-center justify-between text-xs">
                      <div className="flex items-center gap-2.5 overflow-hidden">
                        <span className="material-symbols-outlined text-primary text-[18px]">
                          attach_file
                        </span>
                        <div className="overflow-hidden">
                          <p className="font-bold text-slate-900 m-0 truncate">{res.title}</p>
                          <p className="text-[10px] text-slate-500 m-0">
                            {res.originalFileName} • {formatFileSize(res.fileSizeBytes)}
                          </p>
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={() => handleDeleteResource(res.id)}
                        className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
                        title="Xóa tài nguyên"
                      >
                        <span className="material-symbols-outlined text-[18px]">delete</span>
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Footer Actions */}
        <div className="flex justify-end pt-4 border-t border-slate-100">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2.5 rounded-xl text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-colors cursor-pointer"
          >
            Đóng
          </button>
        </div>
      </div>
    </Modal>
  );
};
