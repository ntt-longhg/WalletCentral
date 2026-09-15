import { create } from 'zustand';
import {
  serviceCatalogService,
  pricingPlanService,
} from '@/services/billingServices';
import type {
  ServiceResponse,
  ServiceCreateRequest,
  ServiceUpdateRequest,
  ServicePriceResponse,
  ServicePriceCreateRequest,
  ServicePriceUpdateRequest,
  PriceTierResponse,
  PriceTierCreateRequest,
  PriceTierUpdateRequest,
  PricingPlanResponse,
  PricingPlanCreateRequest,
  PricingPlanUpdateRequest,
  PricingPlanStatusRequest,
} from '@/types/api';

interface ServiceState {
  // Services
  services: ServiceResponse[];
  servicesLoading: boolean;
  servicesError: string | null;

  // Prices per service (keyed by serviceId)
  prices: Record<string, ServicePriceResponse[]>;
  pricesLoading: boolean;

  // Tiers per price (keyed by priceId)
  tiers: Record<string, PriceTierResponse[]>;
  tiersLoading: boolean;

  // Pricing Plans
  pricingPlans: PricingPlanResponse[];
  plansLoading: boolean;
  plansError: string | null;

  // Service actions
  fetchServices: () => Promise<void>;
  createService: (data: ServiceCreateRequest) => Promise<ServiceResponse>;
  updateService: (id: string, data: ServiceUpdateRequest) => Promise<ServiceResponse>;
  deleteService: (id: string) => Promise<void>;

  // Price actions
  fetchPrices: (serviceId: string) => Promise<void>;
  createPrice: (serviceId: string, data: ServicePriceCreateRequest) => Promise<ServicePriceResponse>;
  updatePrice: (priceId: string, data: ServicePriceUpdateRequest) => Promise<ServicePriceResponse>;
  activatePrice: (priceId: string) => Promise<ServicePriceResponse>;

  // Tier actions
  fetchTiers: (priceId: string) => Promise<void>;
  addTier: (priceId: string, data: PriceTierCreateRequest) => Promise<PriceTierResponse>;
  updateTier: (priceId: string, tierId: string, data: PriceTierUpdateRequest) => Promise<PriceTierResponse>;
  deleteTier: (priceId: string, tierId: string) => Promise<void>;

  // Pricing Plan actions
  fetchPricingPlans: () => Promise<void>;
  createPricingPlan: (data: PricingPlanCreateRequest) => Promise<PricingPlanResponse>;
  updatePricingPlan: (id: string, data: PricingPlanUpdateRequest) => Promise<PricingPlanResponse>;
  updatePricingPlanStatus: (id: string, data: PricingPlanStatusRequest) => Promise<PricingPlanResponse>;
}

export const useServiceStore = create<ServiceState>((set) => ({
  // Initial state
  services: [],
  servicesLoading: false,
  servicesError: null,

  prices: {},
  pricesLoading: false,

  tiers: {},
  tiersLoading: false,

  pricingPlans: [],
  plansLoading: false,
  plansError: null,

  // ─── Service Actions ──────────────────────────────────────────────────────────

  fetchServices: async () => {
    set({ servicesLoading: true, servicesError: null });
    try {
      const res = await serviceCatalogService.getAll();
      set({ services: res.data?.data?.items ?? [] });
    } catch (err: any) {
      set({ servicesError: err.response?.data?.message ?? 'Không thể tải danh sách dịch vụ.' });
    } finally {
      set({ servicesLoading: false });
    }
  },

  createService: async (data) => {
    const res = await serviceCatalogService.create(data);
    const service = res.data.data!;
    set((state) => ({ services: [...state.services, service] }));
    return service;
  },

  updateService: async (id, data) => {
    const res = await serviceCatalogService.update(id, data);
    const updated = res.data.data!;
    set((state) => ({
      services: state.services.map((s) => (s.id === id ? updated : s)),
    }));
    return updated;
  },

  deleteService: async (id) => {
    await serviceCatalogService.delete(id);
    set((state) => ({ services: state.services.filter((s) => s.id !== id) }));
  },

  // ─── Price Actions ────────────────────────────────────────────────────────────

  fetchPrices: async (serviceId) => {
    set({ pricesLoading: true });
    try {
      const res = await serviceCatalogService.getPrices(serviceId);
      const items = res.data?.data?.items ?? [];
      set((state) => ({ prices: { ...state.prices, [serviceId]: items } }));
    } finally {
      set({ pricesLoading: false });
    }
  },

  createPrice: async (serviceId, data) => {
    const res = await serviceCatalogService.createPrice(serviceId, data);
    const price = res.data.data!;
    set((state) => ({
      prices: {
        ...state.prices,
        [serviceId]: [...(state.prices[serviceId] ?? []), price],
      },
    }));
    return price;
  },

  updatePrice: async (priceId, data) => {
    const res = await serviceCatalogService.updatePrice(priceId, data);
    const updated = res.data.data!;
    // Update within all services' price lists
    set((state) => {
      const newPrices = { ...state.prices };
      for (const sid in newPrices) {
        newPrices[sid] = newPrices[sid].map((p) => (p.id === priceId ? updated : p));
      }
      return { prices: newPrices };
    });
    return updated;
  },

  activatePrice: async (priceId) => {
    const res = await serviceCatalogService.activatePrice(priceId);
    const updated = res.data.data!;
    set((state) => {
      const newPrices = { ...state.prices };
      for (const sid in newPrices) {
        newPrices[sid] = newPrices[sid].map((p) => (p.id === priceId ? updated : p));
      }
      return { prices: newPrices };
    });
    return updated;
  },

  // ─── Tier Actions ─────────────────────────────────────────────────────────────

  fetchTiers: async (priceId) => {
    set({ tiersLoading: true });
    try {
      const res = await serviceCatalogService.getPriceTiers(priceId);
      const items = res.data?.data ?? [];
      set((state) => ({ tiers: { ...state.tiers, [priceId]: items } }));
    } finally {
      set({ tiersLoading: false });
    }
  },

  addTier: async (priceId, data) => {
    const res = await serviceCatalogService.addPriceTier(priceId, data);
    const tier = res.data.data!;
    set((state) => ({
      tiers: {
        ...state.tiers,
        [priceId]: [...(state.tiers[priceId] ?? []), tier],
      },
    }));
    return tier;
  },

  updateTier: async (priceId, tierId, data) => {
    const res = await serviceCatalogService.updatePriceTier(priceId, tierId, data);
    const updated = res.data.data!;
    set((state) => ({
      tiers: {
        ...state.tiers,
        [priceId]: (state.tiers[priceId] ?? []).map((t) => (t.id === tierId ? updated : t)),
      },
    }));
    return updated;
  },

  deleteTier: async (priceId, tierId) => {
    await serviceCatalogService.deletePriceTier(priceId, tierId);
    set((state) => ({
      tiers: {
        ...state.tiers,
        [priceId]: (state.tiers[priceId] ?? []).filter((t) => t.id !== tierId),
      },
    }));
  },

  // ─── Pricing Plan Actions ─────────────────────────────────────────────────────

  fetchPricingPlans: async () => {
    set({ plansLoading: true, plansError: null });
    try {
      const res = await pricingPlanService.getAll();
      set({ pricingPlans: res.data?.data?.items ?? [] });
    } catch (err: any) {
      set({ plansError: err.response?.data?.message ?? 'Không thể tải danh sách gói cước.' });
    } finally {
      set({ plansLoading: false });
    }
  },

  createPricingPlan: async (data) => {
    const res = await pricingPlanService.create(data);
    const plan = res.data.data!;
    set((state) => ({ pricingPlans: [...state.pricingPlans, plan] }));
    return plan;
  },

  updatePricingPlan: async (id, data) => {
    const res = await pricingPlanService.update(id, data);
    const updated = res.data.data!;
    set((state) => ({
      pricingPlans: state.pricingPlans.map((p) => (p.id === id ? updated : p)),
    }));
    return updated;
  },

  updatePricingPlanStatus: async (id, data) => {
    const res = await pricingPlanService.updateStatus(id, data);
    const updated = res.data.data!;
    set((state) => ({
      pricingPlans: state.pricingPlans.map((p) => (p.id === id ? updated : p)),
    }));
    return updated;
  },
}));
