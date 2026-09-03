import React, { useState, useEffect } from 'react';

interface DocumentViewerProps {
  url?: string | null;
  fileName?: string | null;
  mimeType?: string | null;
  title?: string | null;
}

export const DocumentViewer: React.FC<DocumentViewerProps> = ({
  url,
  fileName,
  mimeType,
  title
}) => {
  const [isFullscreen, setIsFullscreen] = useState(false);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isFullscreen) {
        setIsFullscreen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isFullscreen]);

  const getFileExtension = (name?: string | null, link?: string | null): string => {
    const target = name || link || '';
    const clean = target.split('?')[0].split('#')[0];
    const parts = clean.split('.');
    return parts.length > 1 ? parts.pop()!.toLowerCase() : '';
  };

  const ext = getFileExtension(fileName, url);
  const isPdf = ext === 'pdf' || (mimeType && mimeType.includes('pdf'));
  const isImage = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg'].includes(ext) || (mimeType && mimeType.startsWith('image/'));
  const isOffice = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes(ext) || 
    (mimeType && (mimeType.includes('word') || mimeType.includes('excel') || mimeType.includes('powerpoint') || mimeType.includes('office')));

  const isValidUrl = url && url !== '#' && url.trim().length > 0;

  const handleOpenNewTab = (e: React.MouseEvent) => {
    e.preventDefault();
    if (!isValidUrl) return;

    if (isOffice || isPdf) {
      // Force Google Docs Viewer online so browser opens reader tab instead of auto-downloading raw file
      const viewerUrl = `https://docs.google.com/gview?url=${encodeURIComponent(url)}`;
      window.open(viewerUrl, '_blank', 'noopener,noreferrer');
    } else {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  };

  const renderDocumentContent = (customHeightClass?: string) => {
    const heightClass = customHeightClass || 'min-h-[1400px] sm:min-h-[1600px]';

    if (!isValidUrl) {
      return (
        <div className="text-center p-8 space-y-4 max-w-md text-slate-400">
          <div className="w-16 h-16 bg-slate-800/80 rounded-2xl flex items-center justify-center mx-auto text-emerald-400 border border-slate-700 shadow-inner">
            <span className="material-symbols-outlined text-[36px]">description</span>
          </div>
          <div className="space-y-1">
            <h4 className="text-sm font-bold text-white m-0">Tài liệu: {title || fileName || 'Tệp đính kèm'}</h4>
            <p className="text-xs text-slate-400 m-0 leading-relaxed">
              Tài liệu chưa được khởi tạo link xem trực tuyến hoặc là tệp xem thử mô phỏng.
            </p>
          </div>
        </div>
      );
    }

    if (isPdf) {
      return (
        <iframe
          src={`${url}#toolbar=1`}
          className={`w-full border-0 bg-white ${heightClass}`}
          title={title || fileName || 'Xem trước PDF'}
        />
      );
    }

    if (isOffice) {
      return (
        <iframe
          src={`https://docs.google.com/gview?url=${encodeURIComponent(url)}&embedded=true`}
          className={`w-full border-0 bg-white ${heightClass}`}
          title={title || fileName || 'Xem trước tài liệu Office'}
        />
      );
    }

    if (isImage) {
      return (
        <div className="w-full flex items-center justify-center p-4 overflow-auto min-h-[600px]">
          <img
            src={url}
            alt={title || fileName || 'Hình ảnh tài liệu'}
            className="max-h-[90vh] w-auto object-contain rounded-lg shadow-lg border border-slate-800"
          />
        </div>
      );
    }

    return (
      <iframe
        src={url}
        className={`w-full border-0 bg-white ${heightClass}`}
        title={title || fileName || 'Xem trước tài liệu'}
      />
    );
  };

  return (
    <>
      {/* ------------------------------------------------------------------- */}
      {/* 1. INLINE VIEWER CONTAINER WITH STICKY TOOLBAR                       */}
      {/* ------------------------------------------------------------------- */}
      <div className="w-full flex flex-col bg-slate-900 rounded-2xl border border-slate-800 shadow-xl flex-1 min-h-[1400px] sm:min-h-[1600px] relative">
        {/* Sticky Top Toolbar */}
        <div className="sticky top-0 z-20 bg-slate-900/95 backdrop-blur-md px-4 py-3 border-b border-slate-800 flex items-center justify-between gap-4 shrink-0 shadow-lg rounded-t-2xl">
          <div className="flex items-center gap-2.5 overflow-hidden">
            <span className="material-symbols-outlined text-[20px] text-emerald-400 shrink-0">
              {isPdf ? 'picture_as_pdf' : isImage ? 'image' : isOffice ? 'description' : 'article'}
            </span>
            <div className="overflow-hidden">
              <span className="text-xs font-bold text-slate-200 truncate block">
                {title || fileName || 'Tài liệu xem trực tiếp'}
              </span>
              {fileName && (
                <span className="text-[10px] text-slate-400 truncate block font-mono">
                  {fileName}
                </span>
              )}
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex items-center gap-2 shrink-0">
            {isValidUrl && (
              <button
                type="button"
                onClick={() => setIsFullscreen(true)}
                className="inline-flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-500 text-white text-[11px] font-extrabold px-3 py-1.5 rounded-lg shadow-md transition-all cursor-pointer"
                title="Mở chế độ đọc toàn màn hình"
              >
                <span className="material-symbols-outlined text-[16px]">fullscreen</span>
                <span>Toàn màn hình</span>
              </button>
            )}

            {isValidUrl && (
              <button
                type="button"
                onClick={handleOpenNewTab}
                className="inline-flex items-center gap-1 bg-slate-800 hover:bg-slate-700 text-slate-200 text-[11px] font-bold px-3 py-1.5 rounded-lg border border-slate-700 transition-colors cursor-pointer"
                title="Mở tài liệu đọc trực tuyến trong tab mới"
              >
                <span className="material-symbols-outlined text-[14px]">open_in_new</span>
                <span className="hidden sm:inline">Mở tab mới xem</span>
              </button>
            )}

            {isValidUrl && (
              <a
                href={url}
                download={fileName || 'tai-lieu'}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1 bg-primary hover:bg-primary/90 text-white text-[11px] font-extrabold px-3 py-1.5 rounded-lg shadow-sm transition-all"
                title="Tải tệp về máy tính"
              >
                <span className="material-symbols-outlined text-[14px]">download</span>
                <span>Tải về</span>
              </a>
            )}
          </div>
        </div>

        {/* Main Document Canvas Area (Ultra Tall for Natural Page Scroll) */}
        <div className="flex-1 w-full min-h-[1400px] sm:min-h-[1600px] bg-slate-950 relative flex items-center justify-center">
          {renderDocumentContent()}
        </div>
      </div>

      {/* ------------------------------------------------------------------- */}
      {/* 2. FULLSCREEN READER OVERLAY MODE                                    */}
      {/* ------------------------------------------------------------------- */}
      {isFullscreen && (
        <div className="fixed inset-0 z-50 bg-slate-950 flex flex-col w-screen h-screen animate-in fade-in">
          {/* Fullscreen Header Toolbar */}
          <div className="bg-slate-900 px-6 py-3 border-b border-slate-800 flex items-center justify-between gap-4 shrink-0 shadow-lg">
            <div className="flex items-center gap-3 overflow-hidden">
              <button
                type="button"
                onClick={() => setIsFullscreen(false)}
                className="inline-flex items-center gap-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 font-extrabold text-xs px-3.5 py-2 rounded-xl border border-slate-700 transition-all cursor-pointer"
                title="Thoát chế độ toàn màn hình (Phím ESC)"
              >
                <span className="material-symbols-outlined text-[18px]">fullscreen_exit</span>
                <span>Thoát (ESC)</span>
              </button>
              <div className="overflow-hidden border-l border-slate-800 pl-3">
                <h3 className="text-sm font-extrabold text-white truncate m-0 font-display">
                  {title || fileName || 'Chế độ đọc tài liệu toàn màn hình'}
                </h3>
                {fileName && <span className="text-[11px] text-slate-400 font-mono block">{fileName}</span>}
              </div>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              {isValidUrl && (
                <button
                  type="button"
                  onClick={handleOpenNewTab}
                  className="inline-flex items-center gap-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold px-4 py-2 rounded-xl border border-slate-700 transition-colors cursor-pointer"
                >
                  <span className="material-symbols-outlined text-[16px]">open_in_new</span>
                  <span>Mở tab mới xem</span>
                </button>
              )}
              {isValidUrl && (
                <a
                  href={url}
                  download={fileName || 'tai-lieu'}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex items-center gap-1.5 bg-primary hover:bg-primary/90 text-white text-xs font-extrabold px-4 py-2 rounded-xl shadow-md transition-all"
                >
                  <span className="material-symbols-outlined text-[16px]">download</span>
                  <span>Tải về</span>
                </a>
              )}
            </div>
          </div>

          {/* Fullscreen Body Canvas */}
          <div className="flex-1 w-full h-full bg-slate-950 relative flex items-center justify-center overflow-hidden">
            {renderDocumentContent('h-full min-h-[90vh]')}
          </div>
        </div>
      )}
    </>
  );
};
