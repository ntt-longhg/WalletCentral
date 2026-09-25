import { create } from 'zustand';
import {
  tenantService,
  walletService,
  walletPlanService,
  refundService,
} from '@/services/billingServices';
import type {
  TenantResponse,
  TenantCreateRequest,
  TenantUpdateRequest,
  TenantStatusRequest,
  WalletResponse,
  WalletCreateRequest,
  WalletStatusRequest,
  WalletPlanResponse,
  WalletPlanCreateRequest,
  WalletPlanApproveRequest,
  WalletPlanRejectRequest,
  RefundResponse,
  RefundApproveRequest,
  RefundRejectRequest,
} from '@/types/api';

interface BillingState {
  // Tenants
  tenants: TenantResponse[];
  tenantsLoading: boolean;
  tenantsError: string | null;

  // Wallets
  wallets: WalletResponse[];
  walletsLoading: boolean;
  walletsError: string | null;

  // Wallet Plans
  walletPlans: WalletPlanResponse[];
  pendingPlans: WalletPlanResponse[];
  pendingPlansNextCursor: string | null;
  pendingPlansHasNext: boolean;
  pendingPlansCount: number;
  plansLoading: boolean;
  plansError: string | null;

  // Refund requests
  pendingRefunds: RefundResponse[];
  refundsLoading: boolean;
  refundsError: string | null;

  // Tenant actions
  fetchTenants: () => Promise<void>;
  createTenant: (data: TenantCreateRequest) => Promise<TenantResponse>;
  updateTenant: (id: string, data: TenantUpdateRequest) => Promise<TenantResponse>;
  updateTenantStatus: (id: string, data: TenantStatusRequest) => Promise<TenantResponse>;
  deleteTenant: (id: string) => Promise<void>;

  // Wallet actions
  fetchWallets: () => Promise<void>;
  createWallet: (data: WalletCreateRequest) => Promise<WalletResponse>;
  updateWalletStatus: (id: string, data: WalletStatusRequest) => Promise<WalletResponse>;

  // Wallet Plan actions
  fetchWalletPlans: () => Promise<void>;
  fetchPendingWalletPlans: () => Promise<void>;
  createWalletPlan: (data: WalletPlanCreateRequest) => Promise<WalletPlanResponse>;
  approveWalletPlan: (id: string, data: WalletPlanApproveRequest) => Promise<WalletPlanResponse>;
  rejectWalletPlan: (id: string, data: WalletPlanRejectRequest) => Promise<WalletPlanResponse>;

  // Refund actions
  fetchPendingRefunds: () => Promise<void>;
  approveRefund: (id: string, data: RefundApproveRequest) => Promise<RefundResponse>;
  rejectRefund: (id: string, data: RefundRejectRequest) => Promise<RefundResponse>;
}

export const useBillingStore = create<BillingState>((set) => ({
  // Initial state
  tenants: [],
  tenantsLoading: false,
  tenantsError: null,

  wallets: [],
  walletsLoading: false,
  walletsError: null,

  walletPlans: [],
  pendingPlans: [],
  pendingPlansNextCursor: null,
  pendingPlansHasNext: false,
  pendingPlansCount: 0,
  plansLoading: false,
  plansError: null,

  pendingRefunds: [],
  refundsLoading: false,
  refundsError: null,

  // ─── Tenant Actions ──────────────────────────────────────────────────────────

  fetchTenants: async () => {
    set({ tenantsLoading: true, tenantsError: null });
    try {
      const res = await tenantService.getAll();
      set({ tenants: res.data?.data?.items ?? [] });
    } catch (err: any) {
      set({ tenantsError: err.response?.data?.message ?? 'Không thể tải danh sách tenant.' });
    } finally {
      set({ tenantsLoading: false });
    }
  },

  createTenant: async (data) => {
    const res = await tenantService.create(data);
    const tenant = res.data.data!;
    set((state) => ({ tenants: [...state.tenants, tenant] }));
    return tenant;
  },

  updateTenant: async (id, data) => {
    const res = await tenantService.update(id, data);
    const updated = res.data.data!;
    set((state) => ({
      tenants: state.tenants.map((t) => (t.id === id ? updated : t)),
    }));
    return updated;
  },

  updateTenantStatus: async (id, data) => {
    const res = await tenantService.updateStatus(id, data);
    const updated = res.data.data!;
    set((state) => ({
      tenants: state.tenants.map((t) => (t.id === id ? updated : t)),
    }));
    return updated;
  },

  deleteTenant: async (id) => {
    await tenantService.delete(id);
    set((state) => ({ tenants: state.tenants.filter((t) => t.id !== id) }));
  },

  // ─── Wallet Actions ───────────────────────────────────────────────────────────

  fetchWallets: async () => {
    set({ walletsLoading: true, walletsError: null });
    try {
      const res = await walletService.getAll();
      set({ wallets: res.data?.data?.items ?? [] });
    } catch (err: any) {
      set({ walletsError: err.response?.data?.message ?? 'Không thể tải danh sách ví.' });
    } finally {
      set({ walletsLoading: false });
    }
  },

  createWallet: async (data) => {
    const res = await walletService.create(data);
    const wallet = res.data.data!;
    set((state) => ({ wallets: [...state.wallets, wallet] }));
    return wallet;
  },

  updateWalletStatus: async (id, data) => {
    const res = await walletService.updateStatus(id, data);
    const updated = res.data.data!;
    set((state) => ({
      wallets: state.wallets.map((w) => (w.id === id ? updated : w)),
    }));
    return updated;
  },

  // ─── Wallet Plan Actions ──────────────────────────────────────────────────────

  fetchWalletPlans: async () => {
    set({ plansLoading: true, plansError: null });
    try {
      const res = await walletPlanService.getAll();
      set({ walletPlans: res.data?.data?.items ?? [] });
    } catch (err: any) {
      set({ plansError: err.response?.data?.message ?? 'Không thể tải wallet plans.' });
    } finally {
      set({ plansLoading: false });
    }
  },

  fetchPendingWalletPlans: async (params) => {
    set({ plansLoading: true, plansError: null });
    try {
      const size = params?.size;
      const cursor = params?.cursor ?? undefined;
      const res = await walletPlanService.getPending(cursor, size);
      const data: any = res.data?.data ?? {};
      const meta = data.meta ?? {};
      const items = data.items ?? [];

      set((state) => {
        const nextState = {
          pendingPlansNextCursor: data.nextCursor ?? meta.nextCursor ?? null,
          pendingPlansHasNext: data.hasNext ?? meta.hasNext ?? false,
          pendingPlansCount: data.count ?? meta.count ?? items.length,
        };

        if (params) {
          return {
            ...nextState,
            pendingPlans: items,
          };
        }

        return nextState;
      });
    } catch (err: any) {
      set({ plansError: err.response?.data?.message ?? 'Không thể tải pending plans.' });
    } finally {
      set({ plansLoading: false });
    }
  },

  createWalletPlan: async (data) => {
    const res = await walletPlanService.create(data);
    return res.data.data!;
  },

  approveWalletPlan: async (id, data) => {
    const res = await walletPlanService.approve(id, data);
    const updated = res.data.data!;
    set((state) => ({
      pendingPlans: state.pendingPlans.filter((p) => p.id !== id),
      pendingPlansCount: Math.max(0, state.pendingPlansCount - 1),
      walletPlans: state.walletPlans.map((p) => (p.id === id ? updated : p)),
    }));
    return updated;
  },

  rejectWalletPlan: async (id, data) => {
    const res = await walletPlanService.reject(id, data);
    const updated = res.data.data!;
    set((state) => ({
      pendingPlans: state.pendingPlans.filter((p) => p.id !== id),
      pendingPlansCount: Math.max(0, state.pendingPlansCount - 1),
      walletPlans: state.walletPlans.map((p) => (p.id === id ? updated : p)),
    }));
    return updated;
  },

  fetchPendingRefunds: async () => {
    set({ refundsLoading: true, refundsError: null });
    try {
      const res = await refundService.getPending();
      set({ pendingRefunds: res.data?.data?.items ?? [] });
    } catch (err: any) {
      set({ refundsError: err.response?.data?.message ?? 'Không thể tải yêu cầu hoàn tiền.' });
    } finally {
      set({ refundsLoading: false });
    }
  },

  approveRefund: async (id, data) => {
    const res = await refundService.approve(id, data);
    const updated = res.data.data!;
    set((state) => ({
      pendingRefunds: state.pendingRefunds.filter((r) => r.id !== id),
    }));
    return updated;
  },

  rejectRefund: async (id, data) => {
    const res = await refundService.reject(id, data);
    const updated = res.data.data!;
    set((state) => ({
      pendingRefunds: state.pendingRefunds.filter((r) => r.id !== id),
    }));
    return updated;
  },
}));
