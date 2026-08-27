import React, { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import type { User } from '../../types/auth';
import { storage } from '../../lib/storage';
import { apiClient } from '../../lib/apiClient';

interface AuthContextType {
  token: string | null;
  currentUser: User | null;
  isAuthenticated: boolean;
  appMode: 'STUDENT' | 'LECTURER';
  setAppMode: (mode: 'STUDENT' | 'LECTURER') => void;
  toggleAppMode: () => void;
  login: (token: string, user?: User) => void;
  logout: () => void;
  ensureInstructorToken: () => Promise<string | null>;
  refreshProfile: () => Promise<void>;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(storage.getToken());
  const [currentUser, setCurrentUser] = useState<User | null>(storage.getUser<User>());
  const [appMode, setAppModeState] = useState<'STUDENT' | 'LECTURER'>(storage.getAppMode());
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const setAppMode = (mode: 'STUDENT' | 'LECTURER') => {
    setAppModeState(mode);
    storage.setAppMode(mode);
  };

  const toggleAppMode = () => {
    const newMode = appMode === 'STUDENT' ? 'LECTURER' : 'STUDENT';
    setAppMode(newMode);
  };

  const login = (newToken: string, user?: User) => {
    setToken(newToken);
    storage.setToken(newToken);
    if (user) {
      setCurrentUser(user);
      storage.setUser(user);
    }
  };

  const logout = () => {
    setToken(null);
    setCurrentUser(null);
    storage.clearAll();
  };

  const refreshProfile = async () => {
    const activeToken = storage.getToken();
    if (!activeToken) return;

    try {
      const user = await apiClient.get<User>('/api/v1/auth/me');
      setCurrentUser(user);
      storage.setUser(user);
    } catch (error) {
      console.warn('Could not fetch user profile:', error);
    }
  };

  /**
   * Helper to ensure an authenticated session exists for Lecturer APIs.
   * If not logged in, attempts default test login or provides active token.
   */
  const ensureInstructorToken = async (): Promise<string | null> => {
    const currentToken = storage.getToken();
    if (currentToken) return currentToken;

    setIsLoading(true);

    const candidateCredentials = [
      { email: 'quangtienhoihop@gmail.com', password: 'password' },
      { email: 'quangtienhoihop@gmail.com', password: 'password' },
      { email: 'lecturer@learnova.com', password: 'password' },
      { email: 'lecturer@learnova.com', password: 'password' }
    ];

    for (const cred of candidateCredentials) {
      try {
        const response = await apiClient.post<any>('/api/v1/auth/login', cred, { skipAuth: true });
        const accessToken = response?.accessToken || response?.result?.accessToken;
        if (accessToken) {
          login(accessToken);
          await refreshProfile();
          setIsLoading(false);
          return accessToken;
        }
      } catch {
        // Continue to next candidate
      }
    }

    setIsLoading(false);
    return null;
  };

  useEffect(() => {
    if (token) {
      refreshProfile();
    }
  }, [token]);

  return (
    <AuthContext.Provider
      value={{
        token,
        currentUser,
        isAuthenticated: !!token,
        appMode,
        setAppMode,
        toggleAppMode,
        login,
        logout,
        ensureInstructorToken,
        refreshProfile,
        isLoading
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
