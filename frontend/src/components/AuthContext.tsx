import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { apiService } from '../services/apiService';

interface AuthUser {
  id: string;
  name: string;
  email: string;
}

interface AuthContextType {
  isAuthenticated: boolean;
  user: AuthUser | null;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  token: string | null;
}

interface AuthProviderProps {
  children: React.ReactNode;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));

  // Initialize auth state from stored token
  useEffect(() => {
    const initializeAuth = async () => {
      const storedToken = localStorage.getItem('token');
      if (storedToken) {
        try {
          setIsAuthenticated(true);
          setToken(storedToken);
          // Fetch user profile to populate user state
          const profile = await apiService.getUserProfile();
          setUser({ id: profile.id, name: profile.name, email: '' });
        } catch (error) {
          // If token is invalid, clear authentication
          localStorage.removeItem('token');
          setIsAuthenticated(false);
          setUser(null);
          setToken(null);
        }
      }
    };

    initializeAuth();
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const response = await apiService.login({ email, password });

    localStorage.setItem('token', response.token);
    setToken(response.token);
    setIsAuthenticated(true);

    // Fetch user profile after login
    try {
      const profile = await apiService.getUserProfile();
      setUser({ id: profile.id, name: profile.name, email });
    } catch {
      // Profile fetch failed but login succeeded — still authenticated
      setUser({ id: '', name: email, email });
    }
  }, []);

  const register = useCallback(async (name: string, email: string, password: string) => {
    const response = await apiService.register({ name, email, password });

    localStorage.setItem('token', response.token);
    setToken(response.token);
    setIsAuthenticated(true);

    // Fetch user profile after registration
    try {
      const profile = await apiService.getUserProfile();
      setUser({ id: profile.id, name: profile.name, email });
    } catch {
      // Profile fetch failed but registration succeeded — still authenticated
      setUser({ id: '', name, email });
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    setIsAuthenticated(false);
    setUser(null);
    setToken(null);
    apiService.logout();
  }, []);

  // Update apiService token when it changes
  useEffect(() => {
    if (token) {
      const axiosInstance = (apiService as any)['api'];
      axiosInstance.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    }
  }, [token]);

  const value = {
    isAuthenticated,
    user,
    login,
    register,
    logout,
    token
  };

  return (
    <AuthContext.Provider value={value}>
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

export default AuthContext;