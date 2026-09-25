import { create } from 'zustand';
import { systemConfigService, notificationService } from '@/services/billingServices';
import type {
  SystemConfigResponse,
  SystemConfigUpdateRequest,
  ConfigReloadResponse,
  NotificationResponse,
} from '@/types/api';

interface ConfigState {
  // System Configs
  configs: SystemConfigResponse[];
  configsLoading: boolean;
  configsError: string | null;

  // Notifications
  notifications: NotificationResponse[];
  unreadCount: number;
  notificationsLoading: boolean;

  // Config actions
  fetchConfigs: (group?: string) => Promise<void>;
  updateConfigs: (updates: SystemConfigUpdateRequest[]) => Promise<number>;
  reloadConfigs: () => Promise<ConfigReloadResponse | null>;

  // Notification actions
  fetchNotifications: (params?: { tenantId?: string; isRead?: boolean; cursor?: string; size?: number }) => Promise<void>;
  fetchUnreadCount: (tenantId: string) => Promise<void>;
  markAsRead: (id: string) => Promise<void>;
  markAllAsRead: (tenantId: string) => Promise<void>;
}

export const useConfigStore = create<ConfigState>((set) => ({
  // Initial state
  configs: [],
  configsLoading: false,
  configsError: null,

  notifications: [],
  unreadCount: 0,
  notificationsLoading: false,

  // ─── Config Actions ───────────────────────────────────────────────────────────

  fetchConfigs: async (group) => {
    set({ configsLoading: true, configsError: null });
    try {
      const res = await systemConfigService.getAll(group);
      set({ configs: res.data?.data ?? [] });
    } catch (err: any) {
      set({ configsError: err.response?.data?.message ?? 'Không thể tải cấu hình hệ thống.' });
    } finally {
      set({ configsLoading: false });
    }
  },

  updateConfigs: async (updates) => {
    const res = await systemConfigService.update(updates);
    // Refetch to show saved DB values (they take effect after Sync)
    const all = await systemConfigService.getAll();
    set({ configs: all.data?.data ?? [] });
    return res.data?.data?.savedCount ?? updates.length;
  },

  reloadConfigs: async () => {
    const res = await systemConfigService.reload();
    return res.data?.data ?? null;
  },

  // ─── Notification Actions ─────────────────────────────────────────────────────

  fetchNotifications: async (params) => {
    set({ notificationsLoading: true });
    try {
      const res = await notificationService.getAll(params);
      set({ notifications: res.data?.data?.items ?? [] });
    } finally {
      set({ notificationsLoading: false });
    }
  },

  fetchUnreadCount: async (tenantId) => {
    try {
      const res = await notificationService.getUnreadCount(tenantId);
      set({ unreadCount: res.data?.data?.count ?? 0 });
    } catch {
      // Silent fail for badge count
    }
  },

  markAsRead: async (id) => {
    await notificationService.markAsRead(id);
    set((state) => ({
      notifications: state.notifications.map((n) =>
        n.id === id ? { ...n, isRead: true } : n
      ),
      unreadCount: Math.max(0, state.unreadCount - 1),
    }));
  },

  markAllAsRead: async (tenantId) => {
    await notificationService.markAllAsRead(tenantId);
    set((state) => ({
      notifications: state.notifications.map((n) => ({ ...n, isRead: true })),
      unreadCount: 0,
    }));
  },
}));
