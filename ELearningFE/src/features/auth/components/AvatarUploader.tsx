import React, { useState, useRef } from 'react';
import { authApi } from '../api/authApi';
import { apiClient } from '../../../lib/apiClient';
import { useToast } from '../../../app/context/ToastContext';

interface AvatarUploaderProps {
  currentAvatarUrl?: string;
  fullName?: string;
  onAvatarUploaded: (storageKey: string, previewUrl: string) => void;
  disabled?: boolean;
  size?: 'sm' | 'md' | 'lg';
}

export const AvatarUploader: React.FC<AvatarUploaderProps> = ({
  currentAvatarUrl,
  fullName = 'User',
  onAvatarUploaded,
  disabled = false,
  size = 'md'
}) => {
  const { showSuccess, showError } = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [previewUrl, setPreviewUrl] = useState<string | null>(currentAvatarUrl || null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  const sizeClasses = {
    sm: 'w-20 h-20 text-xl',
    md: 'w-28 h-28 text-2xl',
    lg: 'w-36 h-36 text-4xl'
  };

  const handleFileSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate type (JPG, PNG, WebP)
    const allowed = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowed.includes(file.type.toLowerCase())) {
      showError('Chỉ hỗ trợ file ảnh định dạng JPG, PNG hoặc WebP.');
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
      // 1. Presign Upload URL from Backend
      const presign = await authApi.presignAvatarUpload({
        fileName: file.name,
        contentType: file.type,
        sizeBytes: file.size
      });

      setUploadProgress(40);

      // 2. Direct binary upload to S3 / Local storage
      await apiClient.uploadDirect(presign.uploadUrl, file, file.type, (p) => {
        setUploadProgress(40 + Math.round(p * 0.5));
      });

      setUploadProgress(100);

      // 3. Create local preview and notify parent form
      const localPreview = URL.createObjectURL(file);
      setPreviewUrl(localPreview);
      onAvatarUploaded(presign.storageKey, localPreview);
      showSuccess('Tải ảnh đại diện lên thành công!');
    } catch (err: any) {
      showError(err.message || 'Lỗi khi tải ảnh đại diện lên hệ thống.');
    } finally {
      setIsUploading(false);
      setUploadProgress(0);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const initials = fullName
    ? fullName
        .trim()
        .split(/\s+/)
        .map((n) => n[0])
        .slice(-2)
        .join('')
        .toUpperCase()
    : 'US';

  return (
    <div className="flex flex-col sm:flex-row items-center gap-5">
      {/* Avatar Circle Container */}
      <div className="relative group cursor-pointer" onClick={() => !disabled && !isUploading && fileInputRef.current?.click()}>
        <div
          className={`${sizeClasses[size]} rounded-full bg-surface-container-low border-3 border-white shadow-md overflow-hidden shrink-0 flex items-center justify-center transition-all duration-300 group-hover:shadow-lg group-hover:scale-[1.02] ring-2 ring-primary/20`}
        >
          {previewUrl ? (
            <img
              src={previewUrl}
              alt={fullName}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full bg-gradient-to-tr from-primary to-secondary text-white font-black flex items-center justify-center">
              {initials}
            </div>
          )}

          {/* Hover Overlay */}
          {!isUploading && !disabled && (
            <div className="absolute inset-0 bg-slate-900/50 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col items-center justify-center text-white rounded-full">
              <span className="material-symbols-outlined text-[24px]">photo_camera</span>
              <span className="text-[10px] font-bold mt-0.5">Thay ảnh</span>
            </div>
          )}

          {/* Loading Spinner with Progress */}
          {isUploading && (
            <div className="absolute inset-0 bg-slate-900/80 backdrop-blur-xs flex flex-col items-center justify-center text-white rounded-full">
              <span className="inline-block animate-spin w-7 h-7 border-3 border-white border-t-transparent rounded-full" />
              <span className="text-[11px] font-bold mt-1">{uploadProgress}%</span>
            </div>
          )}
        </div>

        {/* Small Camera Badge Icon */}
        {!isUploading && !disabled && (
          <button
            type="button"
            className="absolute bottom-0 right-0 w-8 h-8 rounded-full bg-primary hover:bg-primary-container text-white flex items-center justify-center shadow-md border-2 border-white transition-transform group-hover:scale-110 cursor-pointer"
            onClick={(e) => {
              e.stopPropagation();
              fileInputRef.current?.click();
            }}
          >
            <span className="material-symbols-outlined text-[16px]">edit</span>
          </button>
        )}
      </div>

      {/* Info & Action Trigger */}
      <div className="space-y-1 text-center sm:text-left flex-1">
        <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider m-0">
          Ảnh đại diện cá nhân
        </h4>
        <p className="text-xs text-slate-500 leading-relaxed m-0">
          Định dạng hỗ trợ: <strong>JPG, PNG, WebP</strong>. Dung lượng tối đa <strong>5MB</strong>.
        </p>

        <input
          type="file"
          ref={fileInputRef}
          onChange={handleFileSelected}
          accept="image/jpeg,image/png,image/webp"
          disabled={disabled || isUploading}
          className="hidden"
        />

        <div className="pt-2">
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={disabled || isUploading}
            className="bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs px-3.5 py-1.5 rounded-xl transition-all inline-flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
          >
            <span className="material-symbols-outlined text-[16px]">upload</span>
            <span>{previewUrl ? 'Chọn ảnh khác' : 'Tải ảnh lên'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
