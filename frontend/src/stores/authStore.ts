import { create } from 'zustand';

interface User {
  id: string;
  username: string;
  role: 'admin' | 'editor' | 'viewer';
  name: string;
}

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  checkPermission: (requiredRole: 'admin' | 'editor' | 'viewer') => boolean;
}

export const useAuthStore = create<AuthState>(() => ({
  user: { id: '1', username: 'admin', role: 'admin', name: 'Администратор' },
  isAuthenticated: true,
  
  checkPermission: (requiredRole: 'admin' | 'editor' | 'viewer') => {
    const roleHierarchy = { admin: 3, editor: 2, viewer: 1 };
    return roleHierarchy['admin'] >= roleHierarchy[requiredRole];
  },
}));
