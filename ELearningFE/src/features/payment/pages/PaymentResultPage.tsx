import React, { useEffect, useState } from 'react';
import { paymentApi } from '../api/paymentApi';
import type { PaymentStatusResponse } from '../api/paymentApi';
import { formatCurrencyVND } from '../../../lib/formatters';

interface PaymentResultPageProps {
  orderCode: number | null;
  initialStatus?: string | null;
  initialCourseId?: number | null;
  onGoToLearning: (courseId: number) => void;
  onBackToCatalog: () => void;
}

export const PaymentResultPage: React.FC<PaymentResultPageProps> = ({
  orderCode,
  initialStatus,
  initialCourseId,
  onGoToLearning,
  onBackToCatalog
}) => {
  const [loading, setLoading] = useState<boolean>(true);
  const [paymentData, setPaymentData] = useState<PaymentStatusResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!orderCode) {
      setLoading(false);
      setError('Không tìm thấy mã đơn hàng thanh toán.');
      return;
    }

    const checkStatus = async () => {
      try {
        setLoading(true);
        const data = await paymentApi.getPaymentStatus(orderCode);
        setPaymentData(data);
      } catch (err: any) {
        if (initialStatus === 'PAID') {
          setPaymentData({
            orderCode: orderCode,
            courseId: initialCourseId || 0,
            courseTitle: 'Khóa học vừa thanh toán',
            amount: 0,
            status: 'PAID',
            enrolled: true
          });
        } else {
          setError(err.message || 'Không thể kiểm tra trạng thái thanh toán.');
        }
      } finally {
        setLoading(false);
      }
    };

    checkStatus();
  }, [orderCode]);

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6 flex-1">
        <div className="bg-surface-container-lowest border border-outline-variant/60 rounded-3xl p-8 max-w-md w-full text-center space-y-6 shadow-xl">
          <div className="w-16 h-16 bg-primary/10 rounded-2xl flex items-center justify-center mx-auto">
            <span className="inline-block animate-spin border-4 border-primary border-t-transparent w-8 h-8 rounded-full" />
          </div>
          <div className="space-y-2">
            <h3 className="text-lg font-black text-slate-900 font-display m-0">
              Đang xác minh giao dịch...
            </h3>
            <p className="text-xs text-slate-500 m-0">
              Hệ thống đang đồng bộ kết quả thanh toán từ cổng PayOS. Vui lòng chờ trong giây lát.
            </p>
          </div>
        </div>
      </div>
    );
  }

  const isSuccess = paymentData?.status === 'PAID' || initialStatus === 'PAID' || paymentData?.enrolled;

  if (error || !paymentData) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6 flex-1">
        <div className="bg-surface-container-lowest border border-outline-variant/60 rounded-3xl p-8 max-w-md w-full text-center space-y-6 shadow-xl">
          <div className="w-16 h-16 bg-red-100 text-red-600 rounded-2xl flex items-center justify-center mx-auto">
            <span className="material-symbols-outlined text-[32px]">error</span>
          </div>
          <div className="space-y-2">
            <h3 className="text-lg font-black text-slate-900 font-display m-0">
              Không thể tải thông tin giao dịch
            </h3>
            <p className="text-xs text-slate-500 m-0 leading-relaxed">
              {error || 'Đơn hàng không tồn tại hoặc đã bị hủy.'}
            </p>
          </div>
          <button
            type="button"
            onClick={onBackToCatalog}
            className="w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-3.5 px-6 rounded-2xl text-xs transition-all shadow-md cursor-pointer"
          >
            Quay lại danh mục khóa học
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center p-4 sm:p-6 flex-1">
      <div className="bg-surface-container-lowest border border-outline-variant/60 rounded-3xl p-8 max-w-lg w-full shadow-2xl space-y-6">
        {isSuccess ? (
          <>
            <div className="text-center space-y-3">
              <div className="w-20 h-20 bg-emerald-100 text-emerald-600 rounded-3xl flex items-center justify-center mx-auto shadow-inner animate-bounce">
                <span className="material-symbols-outlined text-[44px]">verified</span>
              </div>
              <span className="bg-emerald-500/10 text-emerald-700 border border-emerald-500/20 text-[11px] font-extrabold px-3 py-1 rounded-full uppercase tracking-wider inline-block">
                Thanh toán thành công
              </span>
              <h2 className="text-2xl font-black text-slate-900 font-display m-0">
                Chúc mừng bạn đã ghi danh!
              </h2>
              <p className="text-xs text-slate-500 leading-relaxed m-0">
                Giao dịch của bạn qua PayOS đã hoàn tất. Khóa học đã được mở khóa tự động trong tài khoản của bạn.
              </p>
            </div>

            <div className="bg-surface-container-low p-5 rounded-2xl border border-outline-variant/60 space-y-3 text-xs">
              <div className="flex justify-between items-center pb-2 border-b border-slate-200">
                <span className="text-slate-500">Mã đơn hàng PayOS:</span>
                <span className="font-mono font-bold text-slate-800">#{paymentData.orderCode}</span>
              </div>
              <div className="flex justify-between items-center pb-2 border-b border-slate-200">
                <span className="text-slate-500">Khóa học:</span>
                <span className="font-bold text-slate-900 truncate max-w-[220px]">
                  {paymentData.courseTitle}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500">Số tiền đã thanh toán:</span>
                <span className="font-black text-primary text-sm font-display">
                  {formatCurrencyVND(paymentData.amount)}
                </span>
              </div>
            </div>

            <div className="space-y-3 pt-2">
              <button
                type="button"
                onClick={() => onGoToLearning(paymentData.courseId)}
                className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-black py-4 px-6 rounded-2xl text-sm transition-all shadow-xl shadow-emerald-200 flex items-center justify-center gap-2 cursor-pointer active:scale-95"
              >
                <span className="material-symbols-outlined text-[24px]">play_circle</span>
                <span>VÀO HỌC NGAY BÂY GIỜ</span>
              </button>
              <button
                type="button"
                onClick={onBackToCatalog}
                className="w-full bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold py-3 px-6 rounded-2xl text-xs transition-colors cursor-pointer"
              >
                Về danh mục khóa học
              </button>
            </div>
          </>
        ) : (
          <>
            <div className="text-center space-y-3">
              <div className="w-20 h-20 bg-amber-100 text-amber-600 rounded-3xl flex items-center justify-center mx-auto shadow-inner">
                <span className="material-symbols-outlined text-[44px]">pending_actions</span>
              </div>
              <span className="bg-amber-500/10 text-amber-700 border border-amber-500/20 text-[11px] font-extrabold px-3 py-1 rounded-full uppercase tracking-wider inline-block">
                Giao dịch chưa hoàn tất
              </span>
              <h2 className="text-2xl font-black text-slate-900 font-display m-0">
                Thanh toán đã bị hủy hoặc dừng lại
              </h2>
              <p className="text-xs text-slate-500 leading-relaxed m-0">
                Bạn chưa hoàn tất thanh toán cho khóa học <strong className="text-slate-800">&quot;{paymentData.courseTitle}&quot;</strong>. 
                Bạn có thể thử lại bất kỳ lúc nào.
              </p>
            </div>

            <div className="bg-surface-container-low p-5 rounded-2xl border border-outline-variant/60 space-y-3 text-xs">
              <div className="flex justify-between items-center pb-2 border-b border-slate-200">
                <span className="text-slate-500">Mã đơn hàng:</span>
                <span className="font-mono font-bold text-slate-800">#{paymentData.orderCode}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500">Số tiền:</span>
                <span className="font-bold text-slate-900 font-display">
                  {formatCurrencyVND(paymentData.amount)}
                </span>
              </div>
            </div>

            <div className="space-y-3 pt-2">
              <button
                type="button"
                onClick={onBackToCatalog}
                className="w-full bg-primary hover:bg-primary/90 text-white font-extrabold py-3.5 px-6 rounded-2xl text-xs transition-all shadow-lg shadow-primary/20 cursor-pointer"
              >
                Quay lại danh mục & Thử lại
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};
