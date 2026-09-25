import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { setApiToken } from '../api/api';
import { authService } from '../services/billingServices';
import type { AuthResponse } from '../types/api';

interface AuthContextType {
  token: string | null;
  isAdmin: boolean;
  isEmbed: boolean;
  authReady: boolean;
  adminEmail: string | null;
  permissions: Set<string>;
  roleName: string | null;
  setToken: (token: string | null) => void;
  adminSendOtp: (email: string) => Promise<void>;
  adminVerifyOtp: (email: string, otp: string) => Promise<boolean>;
  adminLoginWithPassword: (email: string, password: string) => Promise<AuthResult>;
  adminSetupPassword: (email: string, newPassword: string) => Promise<AuthResult>;
  adminLogout: () => void;
  validateEmbedToken: () => boolean;
  hasPermission: (code: string) => boolean;
  hasAnyPermission: (...codes: string[]) => boolean;
}

interface AuthResult {
  ok: boolean;
  code?: string;
  message?: string;
}

const AuthContext = createContext<AuthContextType>({
  token: null,
  isAdmin: false,
  isEmbed: false,
  authReady: false,
  adminEmail: null,
  permissions: new Set(),
  roleName: null,
  setToken: () => {},
  adminSendOtp: async () => {},
  adminVerifyOtp: async () => false,
  adminLoginWithPassword: async () => ({ ok: false }),
  adminSetupPassword: async () => ({ ok: false }),
  adminLogout: () => {},
  validateEmbedToken: () => false,
  hasPermission: () => false,
  hasAnyPermission: () => false,
});

const ADMIN_TOKEN_KEY = 'ADMIN_SESSION_TOKEN';
const ADMIN_EMAIL_KEY = 'ADMIN_EMAIL';
const ADMIN_PERMISSIONS_KEY = 'ADMIN_PERMISSIONS';
const ADMIN_ROLE_KEY = 'ADMIN_ROLE';

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [searchParams] = useSearchParams();
  const [token, setTokenState] = useState<string | null>(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [adminEmail, setAdminEmail] = useState<string | null>(null);
  const [authReady, setAuthReady] = useState(false);
  const [permissions, setPermissions] = useState<Set<string>>(new Set());
  const [roleName, setRoleName] = useState<string | null>(null);

  const isEmbed = window.location.pathname.startsWith('/embed');

  useEffect(() => {
    const urlToken = searchParams.get('token') || searchParams.get('api-key');
    if (urlToken) {
      setTokenState(urlToken);
      localStorage.setItem('X_API_KEY', urlToken);
      setApiToken(urlToken);
    } else {
      const storedAdminToken = localStorage.getItem(ADMIN_TOKEN_KEY);
      const storedAdminEmail = localStorage.getItem(ADMIN_EMAIL_KEY);
      const storedPermissions = localStorage.getItem(ADMIN_PERMISSIONS_KEY);
      const storedRole = localStorage.getItem(ADMIN_ROLE_KEY);
      if (storedAdminToken) {
        setTokenState(storedAdminToken);
        setApiToken(storedAdminToken);
        setIsAdmin(true);
      }
      if (storedAdminEmail) setAdminEmail(storedAdminEmail);
      if (storedPermissions) {
        try {
          setPermissions(new Set(JSON.parse(storedPermissions)));
        } catch {}
      }
      if (storedRole) setRoleName(storedRole);
    }
    setAuthReady(true);
  }, [searchParams]);

  const setToken = useCallback((newToken: string | null) => {
    setTokenState(newToken);
    if (newToken) {
      localStorage.setItem(ADMIN_TOKEN_KEY, newToken);
      setApiToken(newToken);
      setIsAdmin(true);
    } else {
      localStorage.removeItem(ADMIN_TOKEN_KEY);
      localStorage.removeItem(ADMIN_EMAIL_KEY);
      localStorage.removeItem(ADMIN_PERMISSIONS_KEY);
      localStorage.removeItem(ADMIN_ROLE_KEY);
      setApiToken(null);
      setIsAdmin(false);
      setAdminEmail(null);
      setPermissions(new Set());
      setRoleName(null);
    }
  }, []);

  const adminSendOtp = useCallback(async (email: string): Promise<void> => {
    await authService.sendOtp(email);
  }, []);

  const saveSession = useCallback((data: AuthResponse) => {
    const { token: newToken, email: userEmail, permissions: perms, roleName: role } = data;
    setToken(newToken);
    setAdminEmail(userEmail);
    localStorage.setItem(ADMIN_EMAIL_KEY, userEmail);
    if (perms) {
      const permSet = new Set(perms);
      setPermissions(permSet);
      localStorage.setItem(ADMIN_PERMISSIONS_KEY, JSON.stringify([...permSet]));
    }
    if (role) {
      setRoleName(role);
      localStorage.setItem(ADMIN_ROLE_KEY, role);
    }
  }, [setToken]);

  const adminVerifyOtp = useCallback(async (email: string, otp: string): Promise<boolean> => {
    try {
      const res = await authService.verifyOtp(email, otp);
      if (res.data.success && res.data.data) {
        saveSession(res.data.data);
        return true;
      }
      return false;
    } catch {
      return false;
    }
  }, [saveSession]);

  const adminLoginWithPassword = useCallback(async (email: string, password: string): Promise<AuthResult> => {
    try {
      const res = await authService.loginWithPassword(email, password);
      if (res.data.success && res.data.data) {
        saveSession(res.data.data);
        return { ok: true };
      }
      return { ok: false, message: res.data?.message };
    } catch (err: any) {
      return { ok: false, code: err.response?.data?.code, message: err.response?.data?.message };
    }
  }, [saveSession]);

  const adminSetupPassword = useCallback(async (email: string, newPassword: string): Promise<AuthResult> => {
    try {
      const res = await authService.setupPassword(email, newPassword);
      if (res.data.success && res.data.data) {
        saveSession(res.data.data);
        return { ok: true };
      }
      return { ok: false, message: res.data?.message };
    } catch (err: any) {
      return { ok: false, code: err.response?.data?.code, message: err.response?.data?.message };
    }
  }, [saveSession]);

  const adminLogout = useCallback(async () => {
    try {
      await authService.logout();
    } catch {}
    setToken(null);
  }, [setToken]);

  const validateEmbedToken = useCallback((): boolean => {
    if (!token) return false;
    return token.length > 0;
  }, [token]);

  const hasPermission = useCallback((code: string): boolean => {
    return permissions.has(code);
  }, [permissions]);

  const hasAnyPermission = useCallback((...codes: string[]): boolean => {
    return codes.some(code => permissions.has(code));
  }, [permissions]);

  return (
    <AuthContext.Provider value={{ token, isAdmin, isEmbed, authReady, adminEmail, permissions, roleName, setToken, adminSendOtp, adminVerifyOtp, adminLoginWithPassword, adminSetupPassword, adminLogout, validateEmbedToken, hasPermission, hasAnyPermission }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
