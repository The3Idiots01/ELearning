import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Modal } from '../../../components/common/Modal';
import { instructorCourseApi } from '../api/instructorCourseApi';
import type { PublishCheckResponse, PublishIssue } from '../../../types/course';
import { useToast } from '../../../app/context/ToastContext';
import { useAuth } from '../../../app/context/AuthContext';

interface PublishCheckModalProps {
  isOpen: boolean;
  onClose: () => void;
  courseId: number;
  onPublishSuccess: () => void;
}

export const PublishCheckModal: React.FC<PublishCheckModalProps> = ({
  isOpen,
  onClose,
  courseId,
  onPublishSuccess
}) => {
  const { showSuccess, showError } = useToast();
  const { currentUser } = useAuth();
  const navigate = useNavigate();

  const isProfileIncomplete = !currentUser?.isProfileCompleted;

  const [checkResult, setCheckResult] = useState<PublishCheckResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);

  const fetchCheck = async () => {
    setIsLoading(true);
    try {
      const res = await instructorCourseApi.publishCheck(courseId);
      setCheckResult(res);
    } catch (err: any) {
      showError(err.message || 'Lỗi khi kiểm tra điều kiện xuất bản.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen && !isProfileIncomplete) {
      fetchCheck();
    }
  }, [isOpen, courseId, isProfileIncomplete]);

  const handleGoToProfile = () => {
    onClose();
    navigate('/profile');
  };

  const handlePublish = async () => {
    if (!checkResult?.canPublish) return;
    setIsPublishing(true);
    try {
      await instructorCourseApi.publish(courseId);
      showSuccess('🎉 Chúc mừng! Khóa học đã được xuất bản công khai thành công.');
      onPublishSuccess();
      onClose();
    } catch (err: any) {
      showError(err.message || 'Lỗi khi xuất bản khóa học.');
    } finally {
      setIsPublishing(false);
    }
  };

  const getIssueMessage = (issue: PublishIssue | string): string => {
    if (typeof issue === 'string') return issue;
    if (issue && typeof issue === 'object') {
      return issue.message || issue.code || JSON.stringify(issue);
    }
    return 'Tiêu chí chưa đạt';
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Kiểm tra điều kiện xuất bản khóa học"
      subtitle="Hệ thống xác thực các tiêu chí chất lượng trước khi khóa học lên sàn thương mại."
      maxWidth="lg"
      icon="fact_check"
    >
      <div className="space-y-6">
        {isProfileIncomplete ? (
          <div className="space-y-5">
            <div className="bg-amber-50 border border-amber-200 rounded-2xl p-4 flex items-start gap-3">
              <span className="material-symbols-outlined text-amber-600 text-[24px] shrink-0 mt-0.5">
                person_alert
              </span>
              <div>
                <h4 className="text-xs font-black text-amber-900 uppercase tracking-wider m-0">
                  Cần hoàn thiện hồ sơ cá nhân
                </h4>
                <p className="text-xs text-amber-700 m-0 mt-0.5">
                  Bạn cần hoàn thiện hồ sơ giảng viên (họ tên và ít nhất một trong: giới thiệu bản thân, chuyên
                  môn, sở thích hoặc ảnh đại diện) trước khi có thể xuất bản khóa học.
                </p>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 transition-colors cursor-pointer"
              >
                Đóng
              </button>
              <button
                type="button"
                onClick={handleGoToProfile}
                className="px-5 py-2 rounded-xl text-xs font-black uppercase tracking-wider text-white bg-primary hover:bg-primary-container transition-all shadow-md flex items-center gap-1.5 cursor-pointer active:scale-95"
              >
                <span className="material-symbols-outlined text-[18px]">person</span>
                <span>Hoàn thiện hồ sơ ngay</span>
              </button>
            </div>
          </div>
        ) : isLoading ? (
          <div className="text-center py-10">
            <span className="inline-block animate-spin w-8 h-8 border-3 border-primary border-t-transparent rounded-full" />
            <p className="text-xs text-slate-500 font-bold mt-3">Đang phân tích dữ liệu khóa học...</p>
          </div>
        ) : checkResult ? (
          <div className="space-y-5">
            {/* Status Summary Banner */}
            {checkResult.canPublish ? (
              <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-4 flex items-center gap-3">
                <span className="material-symbols-outlined text-emerald-600 text-[24px]">
                  check_circle
                </span>
                <div>
                  <h4 className="text-xs font-black text-emerald-900 uppercase tracking-wider m-0">
                    Đủ điều kiện xuất bản
                  </h4>
                  <p className="text-xs text-emerald-700 m-0 mt-0.5">
                    Khóa học đã thỏa mãn 100% tiêu chí chuẩn và sẵn sàng mở bán cho học viên.
                  </p>
                </div>
              </div>
            ) : (
              <div className="bg-amber-50 border border-amber-200 rounded-2xl p-4 flex items-start gap-3">
                <span className="material-symbols-outlined text-amber-600 text-[24px] shrink-0 mt-0.5">
                  warning
                </span>
                <div>
                  <h4 className="text-xs font-black text-amber-900 uppercase tracking-wider m-0">
                    Chưa đủ điều kiện xuất bản
                  </h4>
                  <p className="text-xs text-amber-700 m-0 mt-0.5">
                    Vui lòng hoàn thiện các tiêu chí còn thiếu dưới đây để kích hoạt tính năng xuất bản.
                  </p>
                </div>
              </div>
            )}

            {/* Checklist of Issues */}
            <div className="space-y-3">
              <h5 className="text-xs font-bold uppercase tracking-wider text-slate-800 m-0">
                Danh sách kiểm tra chi tiết
              </h5>

              {checkResult.issues && checkResult.issues.length > 0 ? (
                <div className="space-y-2 bg-surface-container-low p-4 rounded-2xl border border-outline-variant/60">
                  {checkResult.issues.map((issue, idx) => (
                    <div key={idx} className="flex items-start gap-2.5 text-xs text-slate-700">
                      <span className="material-symbols-outlined text-rose-500 text-[16px] shrink-0 mt-0.5">
                        cancel
                      </span>
                      <span className="leading-snug font-medium text-rose-900">
                        {getIssueMessage(issue)}
                      </span>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="bg-surface-container-low p-4 rounded-2xl border border-outline-variant/60 space-y-2 text-xs text-slate-700">
                  <div className="flex items-center gap-2 text-emerald-700 font-semibold">
                    <span className="material-symbols-outlined text-[16px]">done</span>
                    <span>Mô tả khóa học chi tiết và đầy đủ (≥ 200 ký tự)</span>
                  </div>
                  <div className="flex items-center gap-2 text-emerald-700 font-semibold">
                    <span className="material-symbols-outlined text-[16px]">done</span>
                    <span>Ảnh bìa thumbnail chất lượng cao</span>
                  </div>
                  <div className="flex items-center gap-2 text-emerald-700 font-semibold">
                    <span className="material-symbols-outlined text-[16px]">done</span>
                    <span>Đủ mục tiêu (≥4), yêu cầu (≥1) và đối tượng học viên (≥1)</span>
                  </div>
                  <div className="flex items-center gap-2 text-emerald-700 font-semibold">
                    <span className="material-symbols-outlined text-[16px]">done</span>
                    <span>Giá bán hợp lệ (0 - 10.000.000 VNĐ)</span>
                  </div>
                  <div className="flex items-center gap-2 text-emerald-700 font-semibold">
                    <span className="material-symbols-outlined text-[16px]">done</span>
                    <span>Giáo trình có ít nhất 1 chương và các bài học đều có nội dung</span>
                  </div>
                </div>
              )}
            </div>

            {/* Actions */}
            <div className="flex items-center justify-between pt-4 border-t border-slate-100">
              <button
                type="button"
                onClick={fetchCheck}
                disabled={isLoading}
                className="text-primary hover:text-primary-container font-bold text-xs inline-flex items-center gap-1 cursor-pointer"
              >
                <span className="material-symbols-outlined text-[16px]">refresh</span>
                <span>Kiểm tra lại</span>
              </button>

              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={onClose}
                  className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 transition-colors cursor-pointer"
                >
                  Đóng
                </button>
                <button
                  type="button"
                  onClick={handlePublish}
                  disabled={!checkResult.canPublish || isPublishing}
                  className={`px-5 py-2 rounded-xl text-xs font-black uppercase tracking-wider text-white transition-all shadow-md flex items-center gap-1.5 cursor-pointer ${
                    checkResult.canPublish
                      ? 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-200 active:scale-95'
                      : 'bg-slate-300 text-slate-500 cursor-not-allowed shadow-none'
                  }`}
                >
                  {isPublishing && (
                    <span className="inline-block animate-spin w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full mr-1" />
                  )}
                  <span className="material-symbols-outlined text-[18px]">publish</span>
                  <span>Xuất bản khóa học</span>
                </button>
              </div>
            </div>
          </div>
        ) : null}
      </div>
    </Modal>
  );
};
