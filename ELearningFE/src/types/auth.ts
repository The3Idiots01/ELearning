export type Role = 'LEARNER' | 'LECTURER' | 'ADMIN';
export type AuthProvider = 'LOCAL' | 'GOOGLE' | 'GITHUB';

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  authProvider?: AuthProvider;
  authProviderId?: string;
  avatarUrl?: string;
  avatarKey?: string;
  bio?: string;
  interests?: string;
  expertise?: string;
  isActive?: boolean;
  isProfileCompleted?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType?: string;
  expiresIn?: number;
  user?: User;
}

export interface LoginRequest {
  email: string;
  password?: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password?: string;
}

export interface RegisterPendingResponse {
  email: string;
  message: string;
  expiresInMinutes: number;
}

export interface AvatarPresignRequest {
  fileName: string;
  contentType: string;
  sizeBytes: number;
}

export interface AvatarPresignResponse {
  uploadUrl: string;
  storageKey: string;
  httpMethod: string;
  expiresInSeconds: number;
  requiredHeaders?: Record<string, string>;
}

export interface UpdateProfileRequest {
  fullName?: string;
  avatarKey?: string;
  bio?: string;
  expertise?: string;
  interests?: string[];
}
