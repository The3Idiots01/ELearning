import React, { useState, useEffect, useCallback } from 'react';
import { reviewApi } from '../../api/reviewApi';
import type {
  CourseReview,
  CourseReviewSummary,
  PaginatedReviews,
  CourseReviewEligibility
} from '../../../../types/review';
import { CourseRatingHistogram } from './CourseRatingHistogram';
import { CourseReviewCard } from './CourseReviewCard';
import { CourseReviewModal } from './CourseReviewModal';

interface CourseReviewsSectionProps {
  courseId: number;
  courseTitle: string;
  isEnrolled: boolean;
  onEnrollRequired?: () => void;
}

export const CourseReviewsSection: React.FC<CourseReviewsSectionProps> = ({
  courseId,
  courseTitle,
  isEnrolled,
  onEnrollRequired
}) => {
  const [summary, setSummary] = useState<CourseReviewSummary | null>(null);
  const [reviewsData, setReviewsData] = useState<PaginatedReviews<CourseReview> | null>(null);
  const [myReview, setMyReview] = useState<CourseReview | null>(null);
  const [eligibility, setEligibility] = useState<CourseReviewEligibility | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedRating, setSelectedRating] = useState<number | undefined>(undefined);
  const [sortBy, setSortBy] = useState<'newest' | 'highest' | 'lowest'>('newest');
  const [currentPage, setCurrentPage] = useState(0);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeletingMyReview, setIsDeletingMyReview] = useState(false);

  // Load summary and my review status
  const loadSummaryAndMyReview = useCallback(async () => {
    try {
      const [summaryRes, eligibilityRes] = await Promise.all([
        reviewApi.getCourseReviewSummary(courseId),
        isEnrolled ? reviewApi.getMyReviewStatus(courseId) : Promise.resolve(null)
      ]);
      setSummary(summaryRes);
      setEligibility(eligibilityRes);
      setMyReview(eligibilityRes?.review || null);
    } catch {
      // Ignored
    }
  }, [courseId, isEnrolled]);

  // Load paginated reviews
  const loadReviews = useCallback(async (page: number, rating?: number, sort?: 'newest' | 'highest' | 'lowest') => {
    setIsLoading(true);
    try {
      const data = await reviewApi.getCourseReviews(courseId, page, 8, rating, sort);
      setReviewsData(data);
    } catch {
      // Ignored
    } finally {
      setIsLoading(false);
    }
  }, [courseId]);

  useEffect(() => {
    loadSummaryAndMyReview();
  }, [loadSummaryAndMyReview]);

  useEffect(() => {
    loadReviews(currentPage, selectedRating, sortBy);
  }, [loadReviews, currentPage, selectedRating, sortBy]);

  const handleRatingFilterChange = (rating?: number) => {
    setSelectedRating(rating);
    setCurrentPage(0);
  };

  const handleSortChange = (newSort: 'newest' | 'highest' | 'lowest') => {
    setSortBy(newSort);
    setCurrentPage(0);
  };

  const handleReviewSubmitted = (review: CourseReview) => {
    setMyReview(review);
    loadSummaryAndMyReview();
    loadReviews(0, selectedRating, sortBy);
  };

  const handleReviewDeleted = () => {
    setMyReview(null);
    loadSummaryAndMyReview();
    loadReviews(0, selectedRating, sortBy);
  };

  const handleDeleteCard = async (_review: CourseReview) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa đánh giá của mình không?')) return;
    setIsDeletingMyReview(true);
    try {
      await reviewApi.deleteMyReview(courseId);
      handleReviewDeleted();
    } catch {
      alert('Không thể xóa đánh giá. Vui lòng thử lại sau.');
    } finally {
      setIsDeletingMyReview(false);
    }
  };

  // Lọc ra các review khác ngoài myReview để tránh trùng lặp hiển thị nếu myReview đã ghim
  const otherReviews = (reviewsData?.content || []).filter(
    (r) => !myReview || r.id !== myReview.id
  );

  return (
    <section className="pt-8 border-t border-slate-200/80 space-y-6 w-full">
      {/* Tiêu đề & Nút hành động đánh giá */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight font-display">
              Đánh giá từ học viên
            </h2>
            <p className="text-xs sm:text-sm text-slate-500 mt-1">
              Phản hồi thực tế từ những học viên đã tham gia khóa học này.
            </p>
          </div>

          {isEnrolled ? (
            myReview ? (
              <button
                onClick={() => setIsModalOpen(true)}
                className="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-primary text-white font-bold text-xs shadow-sm hover:bg-primary-hover active:scale-95 transition-all cursor-pointer self-start sm:self-auto"
              >
                <span className="material-symbols-outlined text-[18px]">edit_note</span>
                <span>Chỉnh sửa đánh giá của bạn</span>
              </button>
            ) : eligibility && !eligibility.canReview ? (
              <div className="relative group self-start sm:self-auto">
                <button
                  type="button"
                  className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-slate-100 border border-slate-200 text-slate-500 font-semibold text-xs cursor-not-allowed opacity-90 transition-all"
                >
                  <span className="material-symbols-outlined text-[18px] text-amber-500">lock_clock</span>
                  <span>Cần học 20% để đánh giá ({Math.round(eligibility.currentProgress)}%/20%)</span>
                </button>
                <div className="absolute right-0 bottom-full mb-2 hidden group-hover:flex flex-col items-center z-20 w-64 p-3 rounded-2xl bg-slate-900 text-white text-[11px] font-medium shadow-2xl pointer-events-none text-center leading-relaxed">
                  <span>Bạn cần hoàn thành tối thiểu <strong>20%</strong> khóa học để có thể gửi đánh giá.</span>
                  <span className="text-amber-300 font-bold mt-1">Tiến độ hiện tại: {Math.round(eligibility.currentProgress)}%</span>
                  <div className="w-2.5 h-2.5 bg-slate-900 rotate-45 -mb-4 mt-1" />
                </div>
              </div>
            ) : (
              <button
                onClick={() => setIsModalOpen(true)}
                className="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-primary text-white font-bold text-xs shadow-sm hover:bg-primary-hover active:scale-95 transition-all cursor-pointer self-start sm:self-auto"
              >
                <span className="material-symbols-outlined text-[18px]">rate_review</span>
                <span>Viết đánh giá</span>
              </button>
            )
          ) : (
            <button
              onClick={onEnrollRequired}
              className="inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs transition-colors cursor-pointer self-start sm:self-auto"
            >
              <span className="material-symbols-outlined text-[16px] text-slate-500">lock</span>
              <span>Đăng ký học để đánh giá</span>
            </button>
          )}
        </div>

        {/* Biểu đồ phân bổ tỷ lệ sao */}
        {summary && summary.totalReviews > 0 ? (
          <CourseRatingHistogram
            summary={summary}
            activeFilter={selectedRating}
            onSelectRatingFilter={handleRatingFilterChange}
          />
        ) : (
          <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-8 text-center">
            <span className="material-symbols-outlined text-[40px] text-slate-400 mb-2">
              reviews
            </span>
            <p className="text-sm font-bold text-slate-700">Chưa có đánh giá nào</p>
            <p className="text-xs text-slate-400 mt-1">
              Hãy là người đầu tiên trải nghiệm và chia sẻ cảm nhận về khóa học này!
            </p>
          </div>
        )}

        {/* Khối đánh giá của chính học viên (nếu có) */}
        {myReview && (
          <div className="space-y-2">
            <div className="text-xs font-bold text-slate-600 uppercase tracking-wider flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[16px] text-primary">person_check</span>
              <span>Đánh giá của bạn</span>
            </div>
            <CourseReviewCard
              review={myReview}
              isMyReview={true}
              onEdit={() => setIsModalOpen(true)}
              onDelete={handleDeleteCard}
              isDeleting={isDeletingMyReview}
            />
          </div>
        )}

        {/* Thanh công cụ lọc & sắp xếp */}
        {summary && summary.totalReviews > 0 && (
          <div className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t border-slate-100">
            {/* Bộ lọc theo số sao */}
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1 max-w-full">
              <button
                onClick={() => handleRatingFilterChange(undefined)}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors cursor-pointer shrink-0 ${
                  selectedRating === undefined
                    ? 'bg-slate-900 text-white'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                Tất cả
              </button>
              {[5, 4, 3, 2, 1].map((star) => (
                <button
                  key={star}
                  onClick={() => handleRatingFilterChange(selectedRating === star ? undefined : star)}
                  className={`inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-bold transition-colors cursor-pointer shrink-0 ${
                    selectedRating === star
                      ? 'bg-amber-400 text-slate-950 font-black'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  <span>{star}</span>
                  <span className="material-symbols-outlined text-[14px]" style={{ fontVariationSettings: "'FILL' 1" }}>
                    star
                  </span>
                </button>
              ))}
            </div>

            {/* Sắp xếp */}
            <div className="flex items-center gap-2 text-xs">
              <span className="text-slate-500 font-medium">Sắp xếp theo:</span>
              <select
                value={sortBy}
                onChange={(e) => handleSortChange(e.target.value as any)}
                className="bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 text-xs font-semibold text-slate-700 outline-none focus:border-primary cursor-pointer"
              >
                <option value="newest">Mới nhất</option>
                <option value="highest">Đánh giá cao nhất</option>
                <option value="lowest">Đánh giá thấp nhất</option>
              </select>
            </div>
          </div>
        )}

        {/* Danh sách các review khác */}
        {isLoading ? (
          <div className="py-12 text-center text-slate-400">
            <span className="inline-block animate-spin border-3 border-primary border-t-transparent w-7 h-7 rounded-full mb-2" />
            <p className="text-xs font-medium">Đang tải danh sách đánh giá...</p>
          </div>
        ) : otherReviews.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {otherReviews.map((review) => (
              <CourseReviewCard key={review.id} review={review} />
            ))}
          </div>
        ) : !myReview && summary && summary.totalReviews > 0 ? (
          <div className="py-8 text-center text-slate-500 text-xs">
            Không tìm thấy đánh giá nào phù hợp với bộ lọc đã chọn.
          </div>
        ) : null}

        {/* Phân trang */}
        {reviewsData && reviewsData.totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 pt-6">
            <button
              onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
              disabled={currentPage === 0}
              className="px-3 py-1.5 rounded-lg border border-slate-200 text-xs font-semibold text-slate-700 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50 cursor-pointer"
            >
              Trang trước
            </button>
            <span className="text-xs text-slate-500 font-medium px-2">
              Trang {currentPage + 1} / {reviewsData.totalPages}
            </span>
            <button
              onClick={() => setCurrentPage((p) => Math.min(reviewsData.totalPages - 1, p + 1))}
              disabled={currentPage >= reviewsData.totalPages - 1}
              className="px-3 py-1.5 rounded-lg border border-slate-200 text-xs font-semibold text-slate-700 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50 cursor-pointer"
            >
              Trang sau
            </button>
          </div>
        )}

      {/* Review Modal */}
      <CourseReviewModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        courseId={courseId}
        courseTitle={courseTitle}
        existingReview={myReview}
        onReviewSubmitted={handleReviewSubmitted}
        onReviewDeleted={handleReviewDeleted}
      />
    </section>
  );
};
