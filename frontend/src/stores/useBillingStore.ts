import { create } from 'zustand';
import {
  tenantService,
  walletService,
  walletPlanService,
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
  plansLoading: boolean;
  plansError: string | null;

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
  rejectWalletPlan: (id: string, data: WalletPlanApproveRequest) => Promise<WalletPlanResponse>;
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
  plansLoading: false,
  plansError: null,

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

  fetchPendingWalletPlans: async () => {
    set({ plansLoading: true, plansError: null });
    try {
      const res = await walletPlanService.getPending();
      set({ pendingPlans: res.data?.data?.items ?? [] });
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
      walletPlans: state.walletPlans.map((p) => (p.id === id ? updated : p)),
    }));
    return updated;
  },

  rejectWalletPlan: async (id, data) => {
    const res = await walletPlanService.reject(id, data);
    const updated = res.data.data!;
    set((state) => ({
      pendingPlans: state.pendingPlans.filter((p) => p.id !== id),
      walletPlans: state.walletPlans.map((p) => (p.id === id ? updated : p)),
    }));
    return updated;
  },
}));
