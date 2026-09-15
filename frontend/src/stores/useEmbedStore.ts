import { create } from 'zustand';
import { embedService } from '@/services/billingServices';
import type {
  WalletResponse,
  TransactionResponse,
  InvoiceResponse,
  UsageLogResponse,
  CreditAdjustmentResponse,
  PricingPlanResponse,
  WalletPlanResponse,
  PaginatedResponse,
} from '@/types/api';

interface EmbedState {
  // Wallet
  wallet: WalletResponse | null;
  walletLoading: boolean;
  walletError: string | null;

  // Transactions
  transactions: TransactionResponse[];
  transactionsLoading: boolean;
  transactionsError: string | null;

  // Invoices
  invoices: InvoiceResponse[];
  invoicesLoading: boolean;
  invoicesError: string | null;

  // Usage Logs
  usageLogs: UsageLogResponse[];
  usageLogsLoading: boolean;

  // Credit Adjustments
  creditAdjustments: CreditAdjustmentResponse[];
  creditAdjustmentsLoading: boolean;

  // Pricing Plans
  pricingPlans: PricingPlanResponse[];
  pricingPlansLoading: boolean;

  // Actions
  fetchWallet: () => Promise<void>;
  fetchTransactions: () => Promise<void>;
  fetchInvoices: () => Promise<void>;
  fetchUsageLogs: () => Promise<void>;
  fetchCreditAdjustments: () => Promise<void>;
  fetchPricingPlans: () => Promise<void>;
  createWalletPlan: (pricingPlanId: string) => Promise<WalletPlanResponse>;
  payInvoice: (invoiceId: string) => Promise<void>;
}

export const useEmbedStore = create<EmbedState>((set) => ({
  // Initial state
  wallet: null,
  walletLoading: false,
  walletError: null,

  transactions: [],
  transactionsLoading: false,
  transactionsError: null,

  invoices: [],
  invoicesLoading: false,
  invoicesError: null,

  usageLogs: [],
  usageLogsLoading: false,

  creditAdjustments: [],
  creditAdjustmentsLoading: false,

  pricingPlans: [],
  pricingPlansLoading: false,

  // ─── Actions ──────────────────────────────────────────────────────────────────

  fetchWallet: async () => {
    set({ walletLoading: true, walletError: null });
    try {
      const res = await embedService.getWallet();
      if (res.data.success && res.data.data) {
        set({ wallet: res.data.data });
      } else {
        set({ walletError: 'Không thể tải thông tin ví. Vui lòng kiểm tra API Key.' });
      }
    } catch (err: any) {
      set({ walletError: err.response?.data?.message ?? 'Lỗi kết nối đến server.' });
    } finally {
      set({ walletLoading: false });
    }
  },

  fetchTransactions: async () => {
    set({ transactionsLoading: true, transactionsError: null });
    try {
      const res = await embedService.getTransactions();
      if (res.data.success && res.data.data?.items) {
        set({ transactions: res.data.data.items });
      } else {
        set({ transactionsError: 'Không thể tải lịch sử giao dịch.' });
      }
    } catch (err: any) {
      set({ transactionsError: err.response?.data?.message ?? 'Lỗi kết nối đến server.' });
    } finally {
      set({ transactionsLoading: false });
    }
  },

  fetchInvoices: async () => {
    set({ invoicesLoading: true, invoicesError: null });
    try {
      const res = await embedService.getInvoices();
      if (res.data.success && res.data.data?.items) {
        set({ invoices: res.data.data.items });
      } else {
        set({ invoicesError: 'Không thể tải danh sách hóa đơn.' });
      }
    } catch (err: any) {
      set({ invoicesError: err.response?.data?.message ?? 'Lỗi kết nối đến server.' });
    } finally {
      set({ invoicesLoading: false });
    }
  },

  fetchUsageLogs: async () => {
    set({ usageLogsLoading: true });
    try {
      const res = await embedService.getUsageLogs();
      if (res.data.success && res.data.data) {
        const data = res.data.data;
        const items = Array.isArray(data)
          ? data
          : (data as PaginatedResponse<UsageLogResponse>).items ?? [];
        set({ usageLogs: items });
      }
    } finally {
      set({ usageLogsLoading: false });
    }
  },

  fetchCreditAdjustments: async () => {
    set({ creditAdjustmentsLoading: true });
    try {
      const res = await embedService.getCreditAdjustments();
      if (res.data.success && res.data.data) {
        const data = res.data.data;
        const items = Array.isArray(data)
          ? data
          : (data as PaginatedResponse<CreditAdjustmentResponse>).items ?? [];
        set({ creditAdjustments: items });
      }
    } finally {
      set({ creditAdjustmentsLoading: false });
    }
  },

  fetchPricingPlans: async () => {
    set({ pricingPlansLoading: true });
    try {
      const res = await embedService.getPricingPlans();
      if (res.data.success && res.data.data) {
        set({ pricingPlans: res.data.data });
      }
    } finally {
      set({ pricingPlansLoading: false });
    }
  },

  createWalletPlan: async (pricingPlanId) => {
    const res = await embedService.createWalletPlan(pricingPlanId);
    return res.data.data!;
  },

  payInvoice: async (invoiceId) => {
    const { api } = await import('@/api/api');
    const res = await api.patch(`/invoices/${invoiceId}/pay`, { updatedBy: 'client' });
    if (res.data.success) {
      set((state) => ({
        invoices: state.invoices.map((inv) =>
          inv.id === invoiceId ? { ...inv, status: 'PAID' } : inv
        ),
      }));
    }
  },
}));
