import { api } from '@/api/api';
import {
  ApiResponse,
  PaginatedResponse,
  ServiceResponse,
  ServiceCreateRequest,
  ServiceUpdateRequest,
  ServicePriceResponse,
  ServicePriceCreateRequest,
  ServicePriceUpdateRequest,
  PriceTierResponse,
  PriceTierCreateRequest,
  PriceTierUpdateRequest,
  TenantResponse,
  TenantCreateRequest,
  TenantUpdateRequest,
  TenantStatusRequest,
  PricingPlanResponse,
  PricingPlanCreateRequest,
  PricingPlanUpdateRequest,
  PricingPlanStatusRequest,
  WalletPlanResponse,
  WalletPlanCreateRequest,
  WalletPlanApproveRequest,
  WalletPlanRejectRequest,
  RefundResponse,
  RefundCreateRequest,
  RefundApproveRequest,
  RefundRejectRequest,
  WalletResponse,
  WalletCreateRequest,
  WalletStatusRequest,
  TransactionResponse,
  TransactionCreateRequest,
  InvoiceResponse,
  InvoiceCreateRequest,
  InvoicePayRequest,
  UsageLogResponse,
  UsageLogCreateRequest,
  CreditAdjustmentResponse,
  CreditAdjustmentCreateRequest,
  AuthResponse,
  LoginConfigResponse,
  ConfigReloadResponse,
  SystemConfigResponse,
  SystemConfigUpdateRequest,
  RoleResponse,
  RoleCreateRequest,
  RoleUpdateRequest,
  PermissionResponse,
  AdminUserResponse,
  UserInfoResponse,
  NotificationResponse,
} from '@/types/api';

// 1. Service Catalog Service
export const serviceCatalogService = {
  getAll: () => api.get<ApiResponse<PaginatedResponse<ServiceResponse>>>('/services'),
  getById: (id: string) => api.get<ApiResponse<ServiceResponse>>(`/services/${id}`),
  create: (data: ServiceCreateRequest) => api.post<ApiResponse<ServiceResponse>>('/services', data),
  update: (id: string, data: ServiceUpdateRequest) => api.put<ApiResponse<ServiceResponse>>(`/services/${id}`, data),
  delete: (id: string) => api.delete<ApiResponse<void>>(`/services/${id}`),

  createPrice: (serviceId: string, data: ServicePriceCreateRequest) =>
    api.post<ApiResponse<ServicePriceResponse>>(`/services/${serviceId}/prices`, data),
  getPrices: (serviceId: string) =>
    api.get<ApiResponse<PaginatedResponse<ServicePriceResponse>>>(`/services/${serviceId}/prices`),
  getPriceDetail: (priceId: string) =>
    api.get<ApiResponse<ServicePriceResponse>>(`/services/prices/${priceId}`),
  updatePrice: (priceId: string, data: ServicePriceUpdateRequest) =>
    api.put<ApiResponse<ServicePriceResponse>>(`/services/prices/${priceId}`, data),
  activatePrice: (priceId: string) =>
    api.patch<ApiResponse<ServicePriceResponse>>(`/services/prices/${priceId}/activate`),

  addPriceTier: (priceId: string, data: PriceTierCreateRequest) =>
    api.post<ApiResponse<PriceTierResponse>>(`/services/prices/${priceId}/tiers`, data),
  getPriceTiers: (priceId: string) =>
    api.get<ApiResponse<PriceTierResponse[]>>(`/services/prices/${priceId}/tiers`),
  updatePriceTier: (priceId: string, tierId: string, data: PriceTierUpdateRequest) =>
    api.put<ApiResponse<PriceTierResponse>>(`/services/prices/${priceId}/tiers/${tierId}`, data),
  deletePriceTier: (priceId: string, tierId: string) =>
    api.delete<ApiResponse<void>>(`/services/prices/${priceId}/tiers/${tierId}`),
};

// 2. Tenant Management Service
export const tenantService = {
  create: (data: TenantCreateRequest) => api.post<ApiResponse<TenantResponse>>('/tenants', data),
  getAll: () => api.get<ApiResponse<PaginatedResponse<TenantResponse>>>('/tenants'),
  getById: (id: string) => api.get<ApiResponse<TenantResponse>>(`/tenants/${id}`),
  update: (id: string, data: TenantUpdateRequest) => api.put<ApiResponse<TenantResponse>>(`/tenants/${id}`, data),
  updateStatus: (id: string, data: TenantStatusRequest) =>
    api.patch<ApiResponse<TenantResponse>>(`/tenants/${id}/status`, data),
  delete: (id: string) => api.delete<ApiResponse<void>>(`/tenants/${id}`),
};

// 3. Pricing Plan Service
export const pricingPlanService = {
  create: (data: PricingPlanCreateRequest) => api.post<ApiResponse<PricingPlanResponse>>('/pricing-plans', data),
  getAll: () => api.get<ApiResponse<PaginatedResponse<PricingPlanResponse>>>('/pricing-plans'),
  getById: (id: string) => api.get<ApiResponse<PricingPlanResponse>>(`/pricing-plans/${id}`),
  update: (id: string, data: PricingPlanUpdateRequest) =>
    api.put<ApiResponse<PricingPlanResponse>>(`/pricing-plans/${id}`, data),
  updateStatus: (id: string, data: PricingPlanStatusRequest) =>
    api.patch<ApiResponse<PricingPlanResponse>>(`/pricing-plans/${id}/status`, data),
};

// 4. Wallet Plan Service
export const walletPlanService = {
  create: (data: WalletPlanCreateRequest) => api.post<ApiResponse<WalletPlanResponse>>('/wallet-plans', data),
  getAll: () => api.get<ApiResponse<PaginatedResponse<WalletPlanResponse>>>('/wallet-plans'),
  getById: (id: string) => api.get<ApiResponse<WalletPlanResponse>>(`/wallet-plans/${id}`),
  getPending: () => api.get<ApiResponse<PaginatedResponse<WalletPlanResponse>>>('/wallet-plans/pending'),
  approve: (id: string, data: WalletPlanApproveRequest) =>
    api.post<ApiResponse<WalletPlanResponse>>(`/wallet-plans/${id}/approve`, data),
  reject: (id: string, data: WalletPlanRejectRequest) =>
    api.post<ApiResponse<WalletPlanResponse>>(`/wallet-plans/${id}/reject`, data),
};

// 4b. Refund Service (admin)
export const refundService = {
  getPending: () => api.get<ApiResponse<PaginatedResponse<RefundResponse>>>('/refund-requests/pending'),
  approve: (id: string, data: RefundApproveRequest) =>
    api.post<ApiResponse<RefundResponse>>(`/refund-requests/${id}/approve`, data),
  reject: (id: string, data: RefundRejectRequest) =>
    api.post<ApiResponse<RefundResponse>>(`/refund-requests/${id}/reject`, data),
};

// 5. Wallet Service
export const walletService = {
  create: (data: WalletCreateRequest) => api.post<ApiResponse<WalletResponse>>('/wallets', data),
  getAll: () => api.get<ApiResponse<PaginatedResponse<WalletResponse>>>('/wallets'),
  getById: (id: string) => api.get<ApiResponse<WalletResponse>>(`/wallets/${id}`),
  getByTenantId: (tenantId: string) => api.get<ApiResponse<WalletResponse>>(`/wallets/tenant/${tenantId}`),
  updateStatus: (id: string, data: WalletStatusRequest) =>
    api.patch<ApiResponse<WalletResponse>>(`/wallets/${id}/status`, data),
};

// 6. Transaction Service
export const transactionService = {
  create: (data: TransactionCreateRequest) => api.post<ApiResponse<TransactionResponse>>('/transactions', data),
  getAll: () => api.get<ApiResponse<PaginatedResponse<TransactionResponse>>>('/transactions'),
  getById: (id: string) => api.get<ApiResponse<TransactionResponse>>(`/transactions/${id}`),
  getByWalletId: (walletId: string) =>
    api.get<ApiResponse<TransactionResponse[]>>(`/transactions/wallet/${walletId}`),
};

// 7. Invoice Service
export const invoiceService = {
  create: (data: InvoiceCreateRequest) => api.post<ApiResponse<InvoiceResponse>>('/invoices', data),
  getAll: () => api.get<ApiResponse<PaginatedResponse<InvoiceResponse>>>('/invoices'),
  getById: (id: string) => api.get<ApiResponse<InvoiceResponse>>(`/invoices/${id}`),
  pay: (id: string, data: InvoicePayRequest) =>
    api.patch<ApiResponse<InvoiceResponse>>(`/invoices/${id}/pay`, data),
  generate: (data: { tenantId: string; billingPeriod?: string; updatedBy: string }) =>
    api.post<ApiResponse<InvoiceResponse>>('/invoices/generate', data),
  generateAll: (billingPeriod?: string, updatedBy?: string) =>
    api.post<ApiResponse<{ generatedCount: number; billingPeriod: string }>>('/invoices/generate-all', null, {
      params: { billingPeriod, updatedBy },
    }),
};

// 8. Usage Log Service
export const usageLogService = {
  create: (data: UsageLogCreateRequest) => api.post<ApiResponse<UsageLogResponse>>('/usage-logs', data),
  getAll: () => api.get<ApiResponse<PaginatedResponse<UsageLogResponse>>>('/usage-logs'),
  getById: (id: string) => api.get<ApiResponse<UsageLogResponse>>(`/usage-logs/${id}`),
};

// 9. Credit Adjustment Service
export const creditAdjustmentService = {
  create: (data: CreditAdjustmentCreateRequest) =>
    api.post<ApiResponse<CreditAdjustmentResponse>>('/credit-adjustments', data),
  getAll: () => api.get<ApiResponse<PaginatedResponse<CreditAdjustmentResponse>>>('/credit-adjustments'),
  getById: (id: string) => api.get<ApiResponse<CreditAdjustmentResponse>>(`/credit-adjustments/${id}`),
};

// 10. Auth Service (OTP + password admin login)
export const authService = {
  sendOtp: (email: string) =>
    api.post<ApiResponse<{ email: string; message: string }>>('/auth/otp/send', { email }),
  verifyOtp: (email: string, otp: string) =>
    api.post<ApiResponse<AuthResponse>>('/auth/otp/verify', { email, otp }),
  getLoginConfig: () =>
    api.get<ApiResponse<LoginConfigResponse>>('/auth/login-config'),
  loginWithPassword: (email: string, password: string) =>
    api.post<ApiResponse<AuthResponse>>('/auth/password/login', { email, password }),
  setupPassword: (email: string, newPassword: string) =>
    api.post<ApiResponse<AuthResponse>>('/auth/password/setup', { email, newPassword }),
  changePassword: (oldPassword: string, newPassword: string) =>
    api.post<ApiResponse<{ message: string }>>('/auth/password/change', { oldPassword, newPassword }),
  logout: () =>
    api.post<ApiResponse<null>>('/auth/logout'),
};

// 11. System Config Service
export const systemConfigService = {
  getAll: (group?: string) =>
    api.get<ApiResponse<SystemConfigResponse[]>>('/system-configs', { params: group ? { group } : {} }),
  update: (configs: SystemConfigUpdateRequest[]) =>
    api.put<ApiResponse<{ savedCount: number }>>('/system-configs', { configs }),
  reload: () =>
    api.post<ApiResponse<ConfigReloadResponse>>('/system-configs/reload'),
};

// 12. Embed Service (tenant-scoped endpoints for embedded views)
export const embedService = {
  getTenantInfo: () => api.get<ApiResponse<{ tenantId: string; name: string }>>('/embed/tenant-info'),
  getWallet: () => api.get<ApiResponse<WalletResponse>>('/embed/wallet'),
  getTransactions: () =>
    api.get<ApiResponse<PaginatedResponse<TransactionResponse>>>('/embed/transactions'),
  getInvoices: () =>
    api.get<ApiResponse<PaginatedResponse<InvoiceResponse>>>('/embed/invoices'),
  getUsageLogs: () =>
    api.get<ApiResponse<PaginatedResponse<UsageLogResponse>>>('/embed/usage-logs'),
  getCreditAdjustments: () =>
    api.get<ApiResponse<PaginatedResponse<CreditAdjustmentResponse>>>('/embed/credit-adjustments'),
  getPricingPlans: () => api.get<ApiResponse<PricingPlanResponse[]>>('/embed/pricing-plans'),
  createWalletPlan: (pricingPlanId: string) =>
    api.post<ApiResponse<WalletPlanResponse>>('/embed/wallet-plans', pricingPlanId),
  getPendingWalletPlans: () =>
    api.get<ApiResponse<PaginatedResponse<WalletPlanResponse>>>('/embed/wallet-plans/pending'),
  createRefundRequest: (data: RefundCreateRequest) =>
    api.post<ApiResponse<RefundResponse>>('/embed/refund-requests', data),
  getRefundRequests: () =>
    api.get<ApiResponse<PaginatedResponse<RefundResponse>>>('/embed/refund-requests'),
  getPendingRefundRequests: () =>
    api.get<ApiResponse<PaginatedResponse<RefundResponse>>>('/embed/refund-requests/pending'),
};

// 13. RBAC Service (admin only)
export const rbacService = {
  // Users
  getUsers: () =>
    api.get<ApiResponse<AdminUserResponse[]>>('/rbac/users'),
  getUserInfo: (email: string) =>
    api.get<ApiResponse<UserInfoResponse>>(`/rbac/users/${encodeURIComponent(email)}/info`),

  // Roles
  getRoles: (cursor?: string) =>
    api.get<ApiResponse<PaginatedResponse<RoleResponse>>>('/rbac/roles', { params: { cursor } }),
  getRoleById: (id: string) => api.get<ApiResponse<RoleResponse>>(`/rbac/roles/${id}`),
  createRole: (data: RoleCreateRequest) => api.post<ApiResponse<RoleResponse>>('/rbac/roles', data),
  updateRole: (id: string, data: RoleUpdateRequest) =>
    api.put<ApiResponse<RoleResponse>>(`/rbac/roles/${id}`, data),
  deleteRole: (id: string) => api.delete<ApiResponse<void>>(`/rbac/roles/${id}`),

  // Permissions
  getPermissions: (module?: string) =>
    api.get<ApiResponse<PermissionResponse[]>>('/rbac/permissions', { params: { module } }),

  // User role assignment (by email)
  assignRole: (email: string, roleId: string) =>
    api.post<ApiResponse<void>>(`/rbac/users/${encodeURIComponent(email)}/role`, { roleId }),
  removeRole: (email: string) =>
    api.delete<ApiResponse<void>>(`/rbac/users/${encodeURIComponent(email)}/role`),

  // User permission overrides (by email)
  grantPermission: (email: string, permissionId: string) =>
    api.post<ApiResponse<void>>(`/rbac/users/${encodeURIComponent(email)}/permissions`, { permissionId }),
  revokePermission: (email: string, permissionId: string) =>
    api.delete<ApiResponse<void>>(`/rbac/users/${encodeURIComponent(email)}/permissions/${permissionId}`),
};

// 14. Notification Service (admin only)
export const notificationService = {
  getAll: (params?: { tenantId?: string; isRead?: boolean; cursor?: string; size?: number }) =>
    api.get<ApiResponse<PaginatedResponse<NotificationResponse>>>('/notifications', { params }),
  getUnreadCount: (tenantId: string) =>
    api.get<ApiResponse<{ count: number }>>('/notifications/unread-count', { params: { tenantId } }),
  markAsRead: (id: string) => api.patch<ApiResponse<NotificationResponse>>(`/notifications/${id}/read`),
  markAllAsRead: (tenantId: string) =>
    api.patch<ApiResponse<void>>('/notifications/read-all', null, { params: { tenantId } }),
};

// 15. Auth Me endpoint
export const authMeService = {
  getMe: () => api.get<ApiResponse<UserInfoResponse>>('/auth/me'),
};
