import { create } from 'zustand';
import { rbacService } from '@/services/billingServices';
import type {
  AdminUserResponse,
  UserInfoResponse,
  RoleResponse,
  RoleCreateRequest,
  RoleUpdateRequest,
  PermissionResponse,
} from '@/types/api';

interface AuthStoreState {
  // Users
  users: AdminUserResponse[];
  usersLoading: boolean;
  usersError: string | null;

  // Roles
  roles: RoleResponse[];
  rolesLoading: boolean;
  rolesError: string | null;

  // Permissions
  permissions: PermissionResponse[];
  permissionsLoading: boolean;

  // User info (keyed by email)
  userInfoCache: Record<string, UserInfoResponse>;

  // User actions
  fetchUsers: () => Promise<void>;
  getUserInfo: (email: string) => Promise<UserInfoResponse>;
  assignRole: (email: string, roleId: string) => Promise<void>;
  removeRole: (email: string) => Promise<void>;
  grantPermission: (email: string, permissionId: string) => Promise<void>;
  revokePermission: (email: string, permissionId: string) => Promise<void>;

  // Role actions
  fetchRoles: (cursor?: string) => Promise<void>;
  createRole: (data: RoleCreateRequest) => Promise<RoleResponse>;
  updateRole: (id: string, data: RoleUpdateRequest) => Promise<RoleResponse>;
  deleteRole: (id: string) => Promise<void>;

  // Permission actions
  fetchPermissions: (module?: string) => Promise<void>;
}

export const useAuthStore = create<AuthStoreState>((set, get) => ({
  // Initial state
  users: [],
  usersLoading: false,
  usersError: null,

  roles: [],
  rolesLoading: false,
  rolesError: null,

  permissions: [],
  permissionsLoading: false,

  userInfoCache: {},

  // ─── User Actions ─────────────────────────────────────────────────────────────

  fetchUsers: async () => {
    set({ usersLoading: true, usersError: null });
    try {
      const res = await rbacService.getUsers();
      set({ users: res.data?.data ?? [] });
    } catch (err: any) {
      set({ usersError: err.response?.data?.message ?? 'Không thể tải danh sách người dùng.' });
    } finally {
      set({ usersLoading: false });
    }
  },

  getUserInfo: async (email) => {
    const cached = get().userInfoCache[email];
    if (cached) return cached;

    const res = await rbacService.getUserInfo(email);
    const info = res.data.data!;
    set((state) => ({
      userInfoCache: { ...state.userInfoCache, [email]: info },
    }));
    return info;
  },

  assignRole: async (email, roleId) => {
    await rbacService.assignRole(email, roleId);
    // Invalidate user info cache for this email
    set((state) => {
      const cache = { ...state.userInfoCache };
      delete cache[email];
      return { userInfoCache: cache };
    });
  },

  removeRole: async (email) => {
    await rbacService.removeRole(email);
    set((state) => {
      const cache = { ...state.userInfoCache };
      delete cache[email];
      return { userInfoCache: cache };
    });
  },

  grantPermission: async (email, permissionId) => {
    await rbacService.grantPermission(email, permissionId);
    set((state) => {
      const cache = { ...state.userInfoCache };
      delete cache[email];
      return { userInfoCache: cache };
    });
  },

  revokePermission: async (email, permissionId) => {
    await rbacService.revokePermission(email, permissionId);
    set((state) => {
      const cache = { ...state.userInfoCache };
      delete cache[email];
      return { userInfoCache: cache };
    });
  },

  // ─── Role Actions ─────────────────────────────────────────────────────────────

  fetchRoles: async (cursor) => {
    set({ rolesLoading: true, rolesError: null });
    try {
      const res = await rbacService.getRoles(cursor);
      set({ roles: res.data?.data?.items ?? [] });
    } catch (err: any) {
      set({ rolesError: err.response?.data?.message ?? 'Không thể tải danh sách role.' });
    } finally {
      set({ rolesLoading: false });
    }
  },

  createRole: async (data) => {
    const res = await rbacService.createRole(data);
    const role = res.data.data!;
    set((state) => ({ roles: [...state.roles, role] }));
    return role;
  },

  updateRole: async (id, data) => {
    const res = await rbacService.updateRole(id, data);
    const updated = res.data.data!;
    set((state) => ({
      roles: state.roles.map((r) => (r.id === id ? updated : r)),
    }));
    return updated;
  },

  deleteRole: async (id) => {
    await rbacService.deleteRole(id);
    set((state) => ({ roles: state.roles.filter((r) => r.id !== id) }));
  },

  // ─── Permission Actions ───────────────────────────────────────────────────────

  fetchPermissions: async (module) => {
    set({ permissionsLoading: true });
    try {
      const res = await rbacService.getPermissions(module);
      set({ permissions: res.data?.data ?? [] });
    } finally {
      set({ permissionsLoading: false });
    }
  },
}));
