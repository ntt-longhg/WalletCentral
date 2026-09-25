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
  RefundResponse,
  RefundCreateRequest,
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
  usageLogsError: string | null;

  // Credit Adjustments
  creditAdjustments: CreditAdjustmentResponse[];
  creditAdjustmentsLoading: boolean;
  creditAdjustmentsError: string | null;

  // Pricing Plans
  pricingPlans: PricingPlanResponse[];
  pricingPlansLoading: boolean;
  pricingPlansError: string | null;

  // Pending wallet plans (topup requests awaiting approval)
  pendingWalletPlans: WalletPlanResponse[];
  pendingWalletPlansLoading: boolean;
  pendingWalletPlansError: string | null;

  // Refund requests
  refunds: RefundResponse[];
  pendingRefunds: RefundResponse[];
  refundsLoading: boolean;
  refundsError: string | null;

  // Actions
  fetchWallet: () => Promise<void>;
  fetchTransactions: () => Promise<void>;
  fetchInvoices: () => Promise<void>;
  fetchUsageLogs: () => Promise<void>;
  fetchCreditAdjustments: () => Promise<void>;
  fetchPricingPlans: () => Promise<void>;
  fetchPendingWalletPlans: () => Promise<void>;
  fetchRefundRequests: () => Promise<void>;
  fetchPendingRefundRequests: () => Promise<void>;
  createWalletPlan: (pricingPlanId: string) => Promise<WalletPlanResponse>;
  createRefundRequest: (data: RefundCreateRequest) => Promise<RefundResponse>;
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
  usageLogsError: null,

  creditAdjustments: [],
  creditAdjustmentsLoading: false,
  creditAdjustmentsError: null,

  pricingPlans: [],
  pricingPlansLoading: false,
  pricingPlansError: null,

  pendingWalletPlans: [],
  pendingWalletPlansLoading: false,
  pendingWalletPlansError: null,

  refunds: [],
  pendingRefunds: [],
  refundsLoading: false,
  refundsError: null,

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
    } catch (err: any) {
      set({ usageLogsError: err.response?.data?.message ?? 'Lỗi kết nối đến server.' });
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
    } catch (err: any) {
      set({ creditAdjustmentsError: err.response?.data?.message ?? 'Lỗi kết nối đến server.' });
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
    } catch (err: any) {
      set({ pricingPlansError: err.response?.data?.message ?? 'Lỗi kết nối đến server.' });
    } finally {
      set({ pricingPlansLoading: false });
    }
  },

  createWalletPlan: async (pricingPlanId) => {
    const res = await embedService.createWalletPlan(pricingPlanId);
    return res.data.data!;
  },

  fetchPendingWalletPlans: async () => {
    set({ pendingWalletPlansLoading: true, pendingWalletPlansError: null });
    try {
      const res = await embedService.getPendingWalletPlans();
      if (res.data.success && res.data.data?.items) {
        set({ pendingWalletPlans: res.data.data.items });
      } else {
        set({ pendingWalletPlansError: 'Không thể tải yêu cầu nạp gói đang chờ.' });
      }
    } catch (err: any) {
      set({ pendingWalletPlansError: err.response?.data?.message ?? 'Lỗi kết nối đến server.' });
    } finally {
      set({ pendingWalletPlansLoading: false });
    }
  },

  fetchRefundRequests: async () => {
    set({ refundsLoading: true, refundsError: null });
    try {
      const res = await embedService.getRefundRequests();
      if (res.data.success && res.data.data?.items) {
        set({ refunds: res.data.data.items });
      } else {
        set({ refundsError: 'Không thể tải danh sách yêu cầu hoàn tiền.' });
      }
    } catch (err: any) {
      set({ refundsError: err.response?.data?.message ?? 'Lỗi kết nối đến server.' });
    } finally {
      set({ refundsLoading: false });
    }
  },

  fetchPendingRefundRequests: async () => {
    set({ refundsLoading: true, refundsError: null });
    try {
      const res = await embedService.getPendingRefundRequests();
      if (res.data.success && res.data.data?.items) {
        set({ pendingRefunds: res.data.data.items });
      } else {
        set({ refundsError: 'Không thể tải yêu cầu hoàn tiền đang chờ.' });
      }
    } catch (err: any) {
      set({ refundsError: err.response?.data?.message ?? 'Lỗi kết nối đến server.' });
    } finally {
      set({ refundsLoading: false });
    }
  },

  createRefundRequest: async (data) => {
    const res = await embedService.createRefundRequest(data);
    const created = res.data.data!;
    set((state) => ({
      refunds: [created, ...state.refunds],
      pendingRefunds: created.status === 'PENDING' ? [created, ...state.pendingRefunds] : state.pendingRefunds,
    }));
    return created;
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
