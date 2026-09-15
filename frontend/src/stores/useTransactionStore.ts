import { create } from 'zustand';
import {
  transactionService,
  invoiceService,
} from '@/services/billingServices';
import type {
  TransactionResponse,
  TransactionCreateRequest,
  InvoiceResponse,
  InvoiceCreateRequest,
  InvoicePayRequest,
} from '@/types/api';

interface TransactionState {
  // Transactions
  transactions: TransactionResponse[];
  transactionsLoading: boolean;
  transactionsError: string | null;

  // Invoices
  invoices: InvoiceResponse[];
  invoicesLoading: boolean;
  invoicesError: string | null;

  // Transaction actions
  fetchTransactions: () => Promise<void>;
  createTransaction: (data: TransactionCreateRequest) => Promise<TransactionResponse>;

  // Invoice actions
  fetchInvoices: () => Promise<void>;
  createInvoice: (data: InvoiceCreateRequest) => Promise<InvoiceResponse>;
  payInvoice: (id: string, data: InvoicePayRequest) => Promise<InvoiceResponse>;
  generateInvoice: (data: { tenantId: string; billingPeriod?: string; updatedBy: string }) => Promise<InvoiceResponse>;
  generateAllInvoices: (billingPeriod?: string, updatedBy?: string) => Promise<{ generatedCount: number; billingPeriod: string }>;
}

export const useTransactionStore = create<TransactionState>((set) => ({
  // Initial state
  transactions: [],
  transactionsLoading: false,
  transactionsError: null,

  invoices: [],
  invoicesLoading: false,
  invoicesError: null,

  // ─── Transaction Actions ──────────────────────────────────────────────────────

  fetchTransactions: async () => {
    set({ transactionsLoading: true, transactionsError: null });
    try {
      const res = await transactionService.getAll();
      set({ transactions: res.data?.data?.items ?? [] });
    } catch (err: any) {
      set({ transactionsError: err.response?.data?.message ?? 'Không thể tải danh sách giao dịch.' });
    } finally {
      set({ transactionsLoading: false });
    }
  },

  createTransaction: async (data) => {
    const res = await transactionService.create(data);
    const tx = res.data.data!;
    set((state) => ({ transactions: [tx, ...state.transactions] }));
    return tx;
  },

  // ─── Invoice Actions ──────────────────────────────────────────────────────────

  fetchInvoices: async () => {
    set({ invoicesLoading: true, invoicesError: null });
    try {
      const res = await invoiceService.getAll();
      set({ invoices: res.data?.data?.items ?? [] });
    } catch (err: any) {
      set({ invoicesError: err.response?.data?.message ?? 'Không thể tải danh sách hóa đơn.' });
    } finally {
      set({ invoicesLoading: false });
    }
  },

  createInvoice: async (data) => {
    const res = await invoiceService.create(data);
    const invoice = res.data.data!;
    set((state) => ({ invoices: [invoice, ...state.invoices] }));
    return invoice;
  },

  payInvoice: async (id, data) => {
    const res = await invoiceService.pay(id, data);
    const updated = res.data.data!;
    set((state) => ({
      invoices: state.invoices.map((inv) => (inv.id === id ? updated : inv)),
    }));
    return updated;
  },

  generateInvoice: async (data) => {
    const res = await invoiceService.generate(data);
    const invoice = res.data.data!;
    set((state) => ({ invoices: [invoice, ...state.invoices] }));
    return invoice;
  },

  generateAllInvoices: async (billingPeriod, updatedBy) => {
    const res = await invoiceService.generateAll(billingPeriod, updatedBy);
    return res.data.data!;
  },
}));
