import { apiClient } from '../../../lib/apiClient';
import type { Category } from '../../../types/category';
import { MOCK_CATEGORIES } from '../../../lib/mockData';

export const categoryApi = {
  getAll: async (): Promise<Category[]> => {
    try {
      const result = await apiClient.get<Category[]>('/api/v1/categories', { skipAuth: true });
      if (Array.isArray(result) && result.length > 0) {
        return result;
      }
      return MOCK_CATEGORIES;
    } catch {
      // Graceful fallback to mock categories
      return MOCK_CATEGORIES;
    }
  }
};
