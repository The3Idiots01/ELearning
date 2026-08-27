const TOKEN_KEY = 'learnova_access_token';
const USER_KEY = 'learnova_user_profile';
const APP_MODE_KEY = 'learnova_app_mode';

export const storage = {
  getToken: (): string | null => {
    return localStorage.getItem(TOKEN_KEY);
  },
  setToken: (token: string): void => {
    localStorage.setItem(TOKEN_KEY, token);
  },
  removeToken: (): void => {
    localStorage.removeItem(TOKEN_KEY);
  },

  getUser: <T>(): T | null => {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as T;
    } catch {
      return null;
    }
  },
  setUser: <T>(user: T): void => {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  removeUser: (): void => {
    localStorage.removeItem(USER_KEY);
  },

  getAppMode: (): 'STUDENT' | 'LECTURER' => {
    const mode = localStorage.getItem(APP_MODE_KEY);
    return mode === 'LECTURER' ? 'LECTURER' : 'STUDENT';
  },
  setAppMode: (mode: 'STUDENT' | 'LECTURER'): void => {
    localStorage.setItem(APP_MODE_KEY, mode);
  },

  clearAll: (): void => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
};
