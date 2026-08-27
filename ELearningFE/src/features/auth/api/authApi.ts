import { apiClient } from '../../../lib/apiClient';
import type { AuthResponse, LoginRequest, RegisterRequest, User } from '../../../types/auth';

export const authApi = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    return apiClient.post<AuthResponse>('/api/v1/auth/login', credentials, { skipAuth: true });
  },

  register: async (data: RegisterRequest): Promise<any> => {
    return apiClient.post('/api/v1/auth/register', data, { skipAuth: true });
  },

  getMe: async (): Promise<User> => {
    return apiClient.get<User>('/api/v1/auth/me');
  }
};
