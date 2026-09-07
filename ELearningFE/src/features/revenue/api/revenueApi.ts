import { apiClient } from '../../../lib/apiClient';

export interface CourseRevenueItem {
  courseId: number;
  courseTitle: string;
  revenue: number;
}

export interface InstructorRevenueResponse {
  currency: 'VND';
  totalRevenue: number;
  courses: CourseRevenueItem[];
}

export const revenueApi = {
  getMine: async (): Promise<InstructorRevenueResponse> => {
    return apiClient.get<InstructorRevenueResponse>('/api/v1/lecturer/revenue');
  }
};
