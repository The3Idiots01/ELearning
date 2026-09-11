import React, { useState, useEffect } from 'react';
import { reviewApi } from '../../api/reviewApi';
import type { CourseReview } from '../../../../types/review';
import { ApiError } from '../../../../lib/apiClient';

interface CourseReviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  courseId: number;
  courseTitle: string;
  initialRating?: number;
  existingReview?: CourseReview | null;
  onReviewSubmitted: (review: CourseReview) => void;
  onReviewDeleted?: () => void;
}

const RATING_LABELS: Record<number, string> = {
  1: 'Rất không hài lòng',
  2: 'Chưa đạt kỳ vọng',
  3: 'Bình thường, tạm ổn',
  4: 'Tốt, đáng học',
  5: 'Xuất sắc, vượt mong đợi!'
};

export const CourseReviewModal: React.FC<CourseReviewModalProps> = ({
  isOpen,
  onClose,
  courseId,
  courseTitle,
  initialRating,
  existingReview,
  onReviewSubmitted,
  onReviewDeleted
}) => {
  const [rating, setRating] = useState<number>(5);
  const [hoverRating, setHoverRating] = useState<number>(0);
  const [comment, setComment] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  useEffect(() => {
    if (isOpen) {
      if (existingReview) {
        setRating(existingReview.rating);
        setComment(existingReview.comment || '');
      } else {
        setRating(initialRating && initialRating >= 1 && initialRating <= 5 ? initialRating : 5);
        setComment('');
      }
      setErrorMessage(null);
      setShowDeleteConfirm(false);
    }
  }, [isOpen, existingReview, initialRating]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (rating < 1 || rating > 5) {
      setErrorMessage('Vui lòng chọn số sao từ 1 đến 5.');
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      const result = await reviewApi.submitReview(courseId, {
        rating,
        comment: comment.trim() || undefined
      });
      onReviewSubmitted(result);
      onClose();
    } catch (err) {
      if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        setErrorMessage(err.message || 'Bạn không có quyền thực hiện thao tác này.');
      } else {
        setErrorMessage('Vui lòng kiểm tra lại ngôn từ có chứa nội dung không phù hợp và thử lại');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    setIsDeleting(true);
    setErrorMessage(null);
    try {
      await reviewApi.deleteMyReview(courseId);
      onReviewDeleted?.();
      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMessage(err.message || 'Không thể xóa đánh giá.');
      } else {
        setErrorMessage('Lỗi khi xóa đánh giá.');
      }
    } finally {
      setIsDeleting(false);
      setShowDeleteConfirm(false);
    }
  };

  const currentDisplayRating = hoverRating || rating;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
      <div className="bg-surface-container-lowest w-full max-w-lg rounded-3xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col">
        {/* Header */}
        <div className="p-6 border-b border-slate-100 flex items-start justify-between gap-4">
          <div>
            <h3 className="text-lg font-bold text-slate-900">
              {existingReview ? 'Chỉnh sửa đánh giá của bạn' : 'Đánh giá khóa học'}
            </h3>
            <p className="text-xs text-slate-500 line-clamp-1 mt-0.5">{courseTitle}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 p-1.5 rounded-full hover:bg-slate-100 transition-colors cursor-pointer"
          >
            <span className="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-6 overflow-y-auto max-h-[80vh]">
          {/* Thông báo lỗi nếu có */}
          {errorMessage && (
            <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-start gap-2.5">
              <span className="material-symbols-outlined text-[18px] shrink-0 text-rose-500">
                error
              </span>
              <div className="flex-1 font-medium leading-relaxed">{errorMessage}</div>
            </div>
          )}

          {/* Chọn số sao */}
          <div className="text-center py-2">
            <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-2">
              Bạn đánh giá khóa học này thế nào?
            </label>
            <div className="flex items-center justify-center gap-2 mb-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  onClick={() => setRating(star)}
                  onMouseEnter={() => setHoverRating(star)}
                  onMouseLeave={() => setHoverRating(0)}
                  className="p-1 transition-transform hover:scale-110 cursor-pointer focus:outline-none"
                >
                  <span
                    className={`material-symbols-outlined text-[36px] transition-colors ${
                      currentDisplayRating >= star
                        ? 'text-amber-400'
                        : 'text-slate-200 hover:text-amber-200'
                    }`}
                    style={{ fontVariationSettings: "'FILL' 1" }}
                  >
                    star
                  </span>
                </button>
              ))}
            </div>
            <div className="text-sm font-bold text-amber-600 h-5">
              {RATING_LABELS[currentDisplayRating] || ''}
            </div>
          </div>

          {/* Ô nhập nhận xét */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label htmlFor="review-comment" className="text-xs font-bold text-slate-700">
                Nhận xét chi tiết (tùy chọn)
              </label>
              <span className="text-[11px] text-slate-400">{comment.length}/2000</span>
            </div>
            <textarea
              id="review-comment"
              rows={4}
              maxLength={2000}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="Chia sẻ trải nghiệm học tập, chất lượng bài giảng, cách truyền đạt của giảng viên để giúp các học viên khác..."
              className="w-full text-xs sm:text-sm p-3.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none transition-all resize-none text-slate-900 placeholder:text-slate-400"
            />
            <div className="flex items-center gap-1.5 mt-2 text-[11px] text-slate-400">
              <span className="material-symbols-outlined text-[14px]">info</span>
              <span>
                Hệ thống tự động kiểm duyệt ngôn từ: không dùng từ ngữ thô tục, 18+ hoặc chèn liên kết ngoài.
              </span>
            </div>
          </div>

          {/* Xác nhận xóa nếu bấm nút Xóa */}
          {showDeleteConfirm && (
            <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 space-y-3 animate-fade-in">
              <div className="text-xs font-bold text-amber-900 flex items-center gap-1.5">
                <span className="material-symbols-outlined text-[16px] text-amber-600">warning</span>
                <span>Xác nhận xóa đánh giá?</span>
              </div>
              <p className="text-xs text-amber-800 leading-relaxed">
                Đánh giá của bạn sẽ bị gỡ bỏ và điểm trung bình của khóa học sẽ được cập nhật lại. Bạn có chắc chắn muốn xóa không?
              </p>
              <div className="flex items-center gap-2 justify-end">
                <button
                  type="button"
                  onClick={() => setShowDeleteConfirm(false)}
                  disabled={isDeleting}
                  className="px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-200/60 rounded-lg transition-colors cursor-pointer"
                >
                  Hủy
                </button>
                <button
                  type="button"
                  onClick={handleDelete}
                  disabled={isDeleting}
                  className="px-3 py-1.5 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-lg transition-colors inline-flex items-center gap-1.5 cursor-pointer"
                >
                  {isDeleting && (
                    <span className="inline-block animate-spin border-2 border-white border-t-transparent w-3 h-3 rounded-full" />
                  )}
                  <span>Xóa vĩnh viễn</span>
                </button>
              </div>
            </div>
          )}

          {/* Action Buttons Footer */}
          <div className="flex items-center justify-between pt-3 border-t border-slate-100">
            {existingReview && !showDeleteConfirm ? (
              <button
                type="button"
                onClick={() => setShowDeleteConfirm(true)}
                disabled={isSubmitting || isDeleting}
                className="text-xs font-bold text-rose-600 hover:text-rose-700 hover:underline inline-flex items-center gap-1 cursor-pointer"
              >
                <span className="material-symbols-outlined text-[16px]">delete</span>
                <span>Xóa đánh giá này</span>
              </button>
            ) : (
              <div />
            )}

            <div className="flex items-center gap-2.5">
              <button
                type="button"
                onClick={onClose}
                disabled={isSubmitting || isDeleting}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
              >
                Đóng
              </button>
              <button
                type="submit"
                disabled={isSubmitting || isDeleting}
                className="px-5 py-2 text-xs font-bold text-white bg-primary hover:bg-primary-hover active:scale-[0.98] rounded-xl shadow-md transition-all inline-flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
              >
                {isSubmitting && (
                  <span className="inline-block animate-spin border-2 border-white border-t-transparent w-3 h-3 rounded-full" />
                )}
                <span>{existingReview ? 'Cập nhật đánh giá' : 'Gửi đánh giá'}</span>
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};
