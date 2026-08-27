import React, { useState, useRef } from 'react';
import { instructorCourseApi } from '../api/instructorCourseApi';
import { apiClient } from '../../../lib/apiClient';
import { useToast } from '../../../app/context/ToastContext';

interface ThumbnailUploaderProps {
  courseId: number;
  currentThumbnailUrl?: string;
  onThumbnailUpdated: (newThumbnailUrl: string) => void;
}

export const ThumbnailUploader: React.FC<ThumbnailUploaderProps> = ({
  courseId,
  currentThumbnailUrl,
  onThumbnailUpdated
}) => {
  const { showSuccess, showError } = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [previewUrl, setPreviewUrl] = useState<string | null>(currentThumbnailUrl || null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  const handleFileSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate type (JPG, PNG, WebP)
    const allowed = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowed.includes(file.type.toLowerCase())) {
      showError('Chỉ hỗ trợ file ảnh định dạng PNG, JPG hoặc WebP.');
      return;
    }

    // Validate size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      showError('Dung lượng ảnh tối đa là 5MB.');
      return;
    }

    setIsUploading(true);
    setUploadProgress(10);

    try {
      // 1. Presign Upload URL
      const presign = await instructorCourseApi.presignUpload({
        purpose: 'COURSE_THUMBNAIL',
        courseId,
        fileName: file.name,
        contentType: file.type,
        sizeBytes: file.size
      });

      setUploadProgress(40);

      // 2. Direct binary upload PUT to S3 / Local storage
      await apiClient.uploadDirect(presign.uploadUrl, file, file.type, (p) => {
        setUploadProgress(40 + Math.round(p * 0.4));
      });

      setUploadProgress(85);

      // 3. Attach thumbnail storageKey to course
      const updatedCourse = await instructorCourseApi.updateThumbnail(courseId, {
        storageKey: presign.storageKey
      });

      setUploadProgress(100);
      const newUrl = updatedCourse.thumbnailUrl || URL.createObjectURL(file);
      setPreviewUrl(newUrl);
      onThumbnailUpdated(newUrl);
      showSuccess('Cập nhật ảnh bìa khóa học thành công!');
    } catch (err: any) {
      showError(err.message || 'Lỗi khi tải ảnh thumbnail lên hệ thống.');
    } finally {
      setIsUploading(false);
      setUploadProgress(0);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row items-start sm:items-center gap-6">
        {/* Preview Container */}
        <div className="relative w-full sm:w-64 h-36 rounded-2xl bg-surface-container-low border-2 border-dashed border-outline-variant/80 overflow-hidden shrink-0 flex items-center justify-center group">
          {previewUrl ? (
            <img
              src={previewUrl}
              alt="Course Thumbnail"
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="text-center p-4 text-slate-400">
              <span className="material-symbols-outlined text-[36px] text-outline">image</span>
              <p className="text-[11px] font-bold mt-1 text-slate-500 m-0">Chưa có ảnh bìa</p>
            </div>
          )}

          {isUploading && (
            <div className="absolute inset-0 bg-slate-900/70 backdrop-blur-xs flex flex-col items-center justify-center p-4 text-white">
              <span className="inline-block animate-spin w-7 h-7 border-3 border-white border-t-transparent rounded-full" />
              <span className="text-xs font-bold mt-2">{uploadProgress}%</span>
            </div>
          )}
        </div>

        {/* Upload Action Area */}
        <div className="space-y-2 flex-1">
          <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider m-0">
            Tải ảnh bìa khóa học (Thumbnail)
          </h4>
          <p className="text-xs text-slate-500 leading-relaxed m-0">
            Kích thước khuyến nghị: <strong>750x422 pixels (tỉ lệ 16:9)</strong>. Hỗ trợ JPG, PNG, WebP dung lượng tối đa 5MB.
          </p>

          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileSelected}
            accept="image/jpeg,image/png,image/webp"
            className="hidden"
          />

          <div className="pt-2">
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={isUploading}
              className="bg-surface-container-lowest hover:bg-surface-container-low text-primary border border-primary/30 font-bold text-xs px-4 py-2 rounded-xl transition-all shadow-xs inline-flex items-center gap-1.5 cursor-pointer"
            >
              <span className="material-symbols-outlined text-[18px]">upload</span>
              <span>{previewUrl ? 'Thay đổi ảnh bìa' : 'Tải ảnh lên'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
