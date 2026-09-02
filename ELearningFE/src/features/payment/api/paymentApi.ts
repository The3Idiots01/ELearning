import { apiClient } from '../../../lib/apiClient';

export interface CheckoutResponse {
  orderCode?: number;
  courseId: number;
  checkoutUrl?: string;
  amount: number;
  status: 'PENDING' | 'PAID' | 'CANCELLED' | 'FAILED';
  isFree?: boolean;
  isEnrolled?: boolean;
}

export interface PaymentStatusResponse {
  orderCode: number;
  courseId: number;
  courseTitle: string;
  amount: number;
  status: 'PENDING' | 'PAID' | 'CANCELLED' | 'FAILED';
  enrolled: boolean;
}

export const paymentApi = {
  /**
   * Create payment checkout link or perform instant enrollment if free
   */
  createCheckout: async (courseId: number): Promise<CheckoutResponse> => {
    return await apiClient.post<CheckoutResponse>('/api/v1/payments/checkout', { courseId });
  },

  /**
   * Get payment status by order code and check enrollment state
   */
  getPaymentStatus: async (orderCode: number): Promise<PaymentStatusResponse> => {
    return await apiClient.get<PaymentStatusResponse>(`/api/v1/payments/${orderCode}/status`);
  }
};
