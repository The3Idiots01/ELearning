import { useCallback, useEffect, useState } from 'react';
import { useToast } from '../../../app/context/ToastContext';
import { formatRevenueVND } from '../../../lib/formatters';
import { revenueApi, type InstructorRevenueResponse } from '../api/revenueApi';

export function RevenuePage() {
  const { showError } = useToast();
  const [data, setData] = useState<InstructorRevenueResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadRevenue = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setData(await revenueApi.getMine());
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Không thể tải dữ liệu doanh thu.';
      setError(message);
      showError(message);
    } finally {
      setIsLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    // The initial fetch synchronizes this screen with the API on mount.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void loadRevenue();
  }, [loadRevenue]);

  return (
    <div className="flex-1 flex flex-col bg-background min-h-0 overflow-y-auto">
      <header className="bg-surface-container-lowest border-b border-outline-variant/70 px-6 sm:px-8 py-5 shrink-0">
        <h1 className="text-xl sm:text-2xl font-black text-slate-900 font-display m-0">Doanh thu</h1>
        <p className="text-xs text-slate-500 mt-1 mb-0">Tổng hợp từ các đơn hàng đã thanh toán thành công.</p>
      </header>

      <main className="p-6 sm:p-8 space-y-6 max-w-7xl mx-auto w-full">
        {isLoading ? (
          <div className="text-center py-20 bg-surface-container-lowest rounded-3xl border border-outline-variant/60">
            <span className="inline-block animate-spin w-8 h-8 border-3 border-primary border-t-transparent rounded-full" />
            <p className="text-xs text-slate-500 font-bold mt-3 mb-0">Đang tải doanh thu...</p>
          </div>
        ) : error ? (
          <div className="text-center py-16 bg-surface-container-lowest rounded-3xl border border-rose-200 p-8">
            <span className="material-symbols-outlined text-[48px] text-rose-500">error</span>
            <h2 className="text-base font-bold text-slate-900 mt-3 mb-1">Không thể tải doanh thu</h2>
            <p className="text-xs text-slate-500 mb-4">{error}</p>
            <button
              type="button"
              onClick={() => void loadRevenue()}
              className="bg-primary hover:bg-primary/90 text-white text-xs font-bold px-5 py-2.5 rounded-full cursor-pointer"
            >
              Thử lại
            </button>
          </div>
        ) : data ? (
          <>
            <section className="bg-primary text-white rounded-3xl p-6 sm:p-8 shadow-lg shadow-primary/20">
              <p className="text-xs font-bold uppercase tracking-wider text-white/75 mb-2">Tổng doanh thu</p>
              <p className="text-3xl sm:text-4xl font-black font-display m-0">
                {formatRevenueVND(data.totalRevenue)}
              </p>
              <p className="text-xs text-white/75 mt-2 mb-0">Đơn vị: {data.currency}</p>
            </section>

            {data.courses.length === 0 ? (
              <section className="text-center py-16 bg-surface-container-lowest rounded-3xl border-2 border-dashed border-outline-variant/80 p-8">
                <span className="material-symbols-outlined text-[56px] text-outline">school</span>
                <h2 className="text-base font-bold text-slate-900 mt-3 mb-1">Chưa có khóa học</h2>
                <p className="text-xs text-slate-500 m-0">Tạo khóa học đầu tiên để bắt đầu theo dõi doanh thu.</p>
              </section>
            ) : (
              <section className="bg-surface-container-lowest rounded-3xl border border-outline-variant/70 overflow-hidden">
                <div className="px-5 sm:px-6 py-4 border-b border-slate-100">
                  <h2 className="text-sm font-black text-slate-900 m-0">Doanh thu theo khóa học</h2>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse">
                    <thead>
                      <tr className="bg-surface-container-low text-[11px] uppercase tracking-wider text-slate-500">
                        <th className="px-5 sm:px-6 py-3 font-bold">Khóa học</th>
                        <th className="px-5 sm:px-6 py-3 font-bold text-right">Doanh thu</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.courses.map((course) => (
                        <tr key={course.courseId} className="border-t border-slate-100">
                          <td className="px-5 sm:px-6 py-4 text-sm font-bold text-slate-800">{course.courseTitle}</td>
                          <td className="px-5 sm:px-6 py-4 text-right text-sm font-black text-primary whitespace-nowrap">
                            {formatRevenueVND(course.revenue)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            )}
          </>
        ) : null}
      </main>
    </div>
  );
}
