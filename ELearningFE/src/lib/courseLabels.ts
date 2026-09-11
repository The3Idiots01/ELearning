export const contentTypeLabels: Record<string, string> = { VIDEO: 'Video', FILE: 'Tài liệu', ARTICLE: 'Bài viết', QUIZ: 'Bài kiểm tra' };
export const uploadStatusLabels: Record<string, string> = { EMPTY: 'Chưa có nội dung', PENDING: 'Đang tải lên', PROCESSING: 'Đang xử lý video', READY: 'Sẵn sàng', FAILED: 'Xử lý thất bại' };
export function videoProcessingError(code?: string | null): string {
  switch (code) {
    case 'VIDEO_OBJECT_MISSING': return 'Không tìm thấy tệp video. Vui lòng tải lên tệp khác.';
    case 'VIDEO_INVALID_METADATA': return 'Không đọc được thời lượng video. Vui lòng tải lên tệp MP4 hợp lệ.';
    case 'VIDEO_PROCESSING_TIMEOUT': return 'Xử lý video quá thời gian. Bạn có thể thử xử lý lại.';
    default: return 'Chưa xử lý được video. Bạn có thể thử lại mà không cần tải lại tệp.';
  }
}
