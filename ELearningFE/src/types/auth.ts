export type Role = 'LEARNER' | 'LECTURER' | 'ADMIN';

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  avatarUrl?: string;
  bio?: string;
  isActive?: boolean;
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
