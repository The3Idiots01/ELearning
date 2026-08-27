import { apiClient } from '../../../lib/apiClient';
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  RegisterPendingResponse,
  User,
  AvatarPresignRequest,
  AvatarPresignResponse,
  UpdateProfileRequest
} from '../../../types/auth';

export const authApi = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    return apiClient.post<AuthResponse>('/api/v1/auth/login', credentials, { skipAuth: true });
  },

  register: async (data: RegisterRequest): Promise<RegisterPendingResponse> => {
    return apiClient.post<RegisterPendingResponse>('/api/v1/auth/register', data, { skipAuth: true });
  },

  getMe: async (): Promise<User> => {
    return apiClient.get<User>('/api/v1/auth/me');
  },

  getProfile: async (): Promise<User> => {
    return apiClient.get<User>('/api/v1/auth/profile');
  },

  updateProfile: async (data: UpdateProfileRequest): Promise<User> => {
    return apiClient.put<User>('/api/v1/auth/profile', data);
  },

  completeProfile: async (data: UpdateProfileRequest): Promise<User> => {
    return apiClient.post<User>('/api/v1/auth/profile/complete', data);
  },

  presignAvatarUpload: async (data: AvatarPresignRequest): Promise<AvatarPresignResponse> => {
    return apiClient.post<AvatarPresignResponse>('/api/v1/auth/profile/avatar/presign', data);
  },

  refreshToken: async (): Promise<AuthResponse> => {
    return apiClient.post<AuthResponse>('/api/v1/auth/refresh');
  },

  logout: async (): Promise<void> => {
    return apiClient.post<void>('/api/v1/auth/logout');
  }
};
