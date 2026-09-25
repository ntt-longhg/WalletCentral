import { create } from 'zustand';
import { dashboardService } from '@/services/billingServices';
import type { DashboardSummaryResponse } from '@/types/api';

interface DashboardState {
  summary: DashboardSummaryResponse | null;
  summaryLoading: boolean;
  summaryError: string | null;

  fetchSummary: () => Promise<void>;
}

export const useDashboardStore = create<DashboardState>((set) => ({
  summary: null,
  summaryLoading: false,
  summaryError: null,

  fetchSummary: async () => {
    set({ summaryLoading: true, summaryError: null });
    try {
      const res = await dashboardService.getSummary();
      if (res.data.success && res.data.data) {
        set({ summary: res.data.data });
      } else {
        set({ summaryError: 'Không thể tải chỉ số tổng quan.' });
      }
    } catch (err: any) {
      set({ summaryError: err.response?.data?.message ?? 'Lỗi kết nối đến server.' });
    } finally {
      set({ summaryLoading: false });
    }
  },
}));
