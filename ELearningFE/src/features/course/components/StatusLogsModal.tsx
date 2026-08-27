import React, { useState, useEffect } from 'react';
import { Modal } from '../../../components/common/Modal';
import { instructorCourseApi } from '../api/instructorCourseApi';
import type { StatusLog } from '../../../types/course';
import { StatusBadge } from '../../../components/common/Badge';
import { formatDateTime } from '../../../lib/formatters';
import { useToast } from '../../../app/context/ToastContext';

interface StatusLogsModalProps {
  isOpen: boolean;
  onClose: () => void;
  courseId: number;
}

export const StatusLogsModal: React.FC<StatusLogsModalProps> = ({
  isOpen,
  onClose,
  courseId
}) => {
  const { showError } = useToast();
  const [logs, setLogs] = useState<StatusLog[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setIsLoading(true);
      instructorCourseApi
        .getStatusLogs(courseId)
        .then((res) => {
          setLogs(Array.isArray(res) ? res : []);
        })
        .catch((err) => {
          showError(err.message || 'Lỗi khi tải lịch sử trạng thái.');
        })
        .finally(() => {
          setIsLoading(false);
        });
    }
  }, [isOpen, courseId]);

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Nhật ký chuyển đổi trạng thái"
      subtitle="Theo dõi lịch sử xuất bản, gỡ bài và khóa khóa học theo thời gian thực."
      maxWidth="lg"
      icon="history"
    >
      <div className="space-y-4">
        {isLoading ? (
          <div className="text-center py-10">
            <span className="inline-block animate-spin w-7 h-7 border-3 border-primary border-t-transparent rounded-full" />
            <p className="text-xs text-slate-500 font-bold mt-2">Đang tải nhật ký...</p>
          </div>
        ) : logs.length === 0 ? (
          <div className="text-center py-8 bg-surface-container-low rounded-2xl border border-outline-variant/60">
            <p className="text-xs text-slate-500 m-0">Chưa có bản ghi nhật ký trạng thái nào.</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100 max-h-96 overflow-y-auto pr-1">
            {logs.map((log) => (
              <div key={log.id} className="py-3 flex flex-col gap-1.5 text-xs">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <StatusBadge status={log.fromStatus} size="sm" />
                    <span className="material-symbols-outlined text-[14px] text-slate-400">
                      arrow_forward
                    </span>
                    <StatusBadge status={log.toStatus} size="sm" />
                  </div>
                  <span className="text-[11px] text-slate-400 font-mono">
                    {formatDateTime(log.createdAt)}
                  </span>
                </div>
                {log.reason && (
                  <p className="text-slate-600 italic bg-surface-container-low p-2 rounded-lg border border-outline-variant/40 m-0">
                    Lý do: &quot;{log.reason}&quot;
                  </p>
                )}
              </div>
            ))}
          </div>
        )}

        <div className="flex justify-end pt-3 border-t border-slate-100">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 transition-colors cursor-pointer"
          >
            Đóng
          </button>
        </div>
      </div>
    </Modal>
  );
};
