// Standard Response Wrapper
/*{
    "success": true,
    "message": "Success",
    "data": {
        "items": [
            {
                "id": "01a089d2-171c-7212-ac81-15d4b6fb0ead",
                "name": "Acme Corp",
                "clientId": "acme-corp",
                "allowedDomains": "acme.com,acme.co.th",
                "status": "ACTIVE",
                "createdAt": "2026-09-10T12:37:22+07:00",
                "updatedAt": null
            }
        ],
        "nextCursor": null,
        "hasNext": false,
        "size": 20,
        "count": 1
    },
    "timestamp": "2026-09-10T15:39:52.738023278+07:00"
}*/

export interface PaginationMeta {
  nextCursor: string | null;
  hasNext: boolean;
  size: number;
  count: number;
}

export interface PaginatedResponse<T> {
  items: T[];
  meta: PaginationMeta;
}

export interface ApiResponse<T> {
  success: boolean;       // Status request (true/false)
  message: string;        // Thông điệp kết quả ("Success", "Operation completed successfully"...)
  data?: T;               // Payload dữ liệu trả về theo từng DTO (null khi lỗi hoặc không có payload)
  timestamp: string;      // Thời gian ISO-8601 OffsetDateTime (e.g. "2026-09-10T14:35:00+07:00")
}

// 2. Service Catalog Module DTOs
export interface ServiceResponse {
  id: string;             // UUID
  code: string;           // e.g. "SMS", "VKYC"
  name: string;           // e.g. "SMS Service"
  description?: string;
  createdAt: string;      // OffsetDateTime (ISO)
  updatedAt: string;      // OffsetDateTime (ISO)
}

export interface ServiceCreateRequest {
  code: string;           // Required, 2 - 100 chars
  name: string;           // Required
  description?: string;
}

export interface ServiceUpdateRequest {
  code?: string;          // Optional, 2 - 100 chars
  name?: string;
  description?: string;
}

export interface ServicePriceResponse {
  id: string;             // UUID
  serviceId: string;      // UUID
  serviceCode: string;
  initialSize: number;    // Units
  initialFee: number;     // BigDecimal (fee amount)
  subsequentSize: number; // Units
  subsequentFee: number;  // BigDecimal (fee amount)
  active: boolean;        // Status Boolean
  effectiveDate?: string; // OffsetDateTime (ISO)
  createdAt: string;      // OffsetDateTime (ISO)
}

export interface ServicePriceCreateRequest {
  initialSize: number;    // Required, positive integer (> 0)
  initialFee: number;     // Required, zero or positive (>= 0)
  subsequentSize: number; // Required, positive integer (> 0)
  subsequentFee: number;  // Required, zero or positive (>= 0)
  effectiveDate: string;  // Required, OffsetDateTime (ISO)
}

export interface ServicePriceUpdateRequest {
  initialSize?: number;   // Positive integer (> 0)
  initialFee?: number;    // Zero or positive (>= 0)
  subsequentSize?: number;// Positive integer (> 0)
  subsequentFee?: number; // Zero or positive (>= 0)
  effectiveDate?: string; // OffsetDateTime (ISO)
}

export interface PriceTierResponse {
  id: string;             // UUID
  servicePriceId: string; // UUID
  tier: string;           // e.g. "TIER_1"
  basicFee: number;       // BigDecimal (basic fee amount)
  extendedSize: number;   // Units
  extendedFee: number;    // BigDecimal (extended fee amount)
}

export interface PriceTierCreateRequest {
  tier: string;           // Required, 1 - 100 chars (e.g. "TIER_1")
  basicFee: number;       // Required, zero or positive (>= 0)
  extendedSize: number;   // Required, positive integer (> 0)
  extendedFee: number;    // Required, zero or positive (>= 0)
}

export interface PriceTierUpdateRequest {
  basicFee?: number;      // Zero or positive (>= 0)
  extendedSize?: number;  // Positive integer (> 0)
  extendedFee?: number;   // Zero or positive (>= 0)
}

// 3. Tenant Management Module DTOs
export interface TenantResponse {
  id: string;             // UUID
  name: string;           // e.g. "Acme Corp"
  clientId: string;       // e.g. "acme-corp"
  allowedDomains?: string;// Comma-separated domains (e.g. "acme.com,acme.co.th")
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;      // OffsetDateTime (ISO)
  updatedAt: string;      // OffsetDateTime (ISO)
}

export interface TenantCreateRequest {
  name: string;           // Required, 2 - 100 chars
  clientId: string;       // Required, 2 - 100 chars
  clientSecret: string;   // Required, min 8 chars
  allowedDomains: string; // Required, comma-separated domains
}

export interface TenantUpdateRequest {
  name?: string;          // 2 - 100 chars
  clientId?: string;      // 2 - 100 chars
  clientSecret?: string;  // min 8 chars
  allowedDomains?: string;
}

export interface TenantStatusRequest {
  status: 'ACTIVE' | 'INACTIVE'; // Required
}

// 4. Pricing Plan Module DTOs
export interface PricingPlanResponse {
  id: string;             // UUID
  code: string;           // e.g. "TOPUP_100"
  name: string;           // e.g. "Topup 100"
  description?: string;
  price: number;          // BigDecimal
  type: 'BALANCE_TOPUP' | 'CREDIT_INCREASE' | string;
  bonusType: 'PERCENTAGE' | 'FIXED' | 'NONE';
  bonusValue?: number;    // BigDecimal
  creditLimitAction: 'NONE' | 'SET' | 'INCREASE';
  creditLimitValue: number;// BigDecimal
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;      // OffsetDateTime (ISO)
  updatedAt: string;      // OffsetDateTime (ISO)
}

export interface PricingPlanCreateRequest {
  code: string;           // Required
  name: string;           // Required
  description?: string;
  price: number;          // Required, zero or positive (>= 0)
  type: 'BALANCE_TOPUP' | 'CREDIT_INCREASE' | string; // Required
  bonusType: 'PERCENTAGE' | 'FIXED' | 'NONE'; // Required
  bonusValue?: number;    // Percentage or fixed amount
  creditLimitAction: 'NONE' | 'SET' | 'INCREASE'; // Required
  creditLimitValue: number;// Required, zero or positive (>= 0)
}

export interface PricingPlanUpdateRequest {
  name?: string;
  description?: string;
  price?: number;         // Zero or positive (>= 0)
  bonusType?: 'PERCENTAGE' | 'FIXED' | 'NONE';
  bonusValue?: number;
  creditLimitAction?: 'NONE' | 'SET' | 'INCREASE';
  creditLimitValue?: number; // Zero or positive (>= 0)
}

export interface PricingPlanStatusRequest {
  status: 'ACTIVE' | 'INACTIVE'; // Required
}

// 5. Wallet Plan Module DTOs
export interface WalletPlanResponse {
  id: string;             // UUID
  tenantId: string;       // UUID
  tenantName: string;
  pricingPlanId: string;  // UUID
  pricingPlanName: string;
  price: number;          // Plan price
  bonusAmount: number;    // Calculated bonus amount
  creditedAmount: number; // Credited amount to wallet
  balanceBefore: number;
  balanceAfter: number;
  creditLimitBefore: number;
  creditLimitAfter: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  approvedAt?: string;    // OffsetDateTime (ISO)
  approvedBy?: string;    // Identifier
  rejectReason?: string;  // Rejection reason (set when REJECTED)
  createdBy: string;      // Identifier
  createdAt: string;      // OffsetDateTime (ISO)
}

export interface WalletPlanCreateRequest {
  tenantId: string;       // Required, UUID
  pricingPlanId: string;  // Required, UUID
  createdBy: string;      // Required, NotBlank (e.g. "admin")
}

export interface WalletPlanApproveRequest {
  approvedBy: string;     // Required, NotBlank (e.g. "admin")
}

export interface WalletPlanRejectRequest {
  approvedBy: string;     // Required, NotBlank (e.g. "admin")
  rejectReason: string;   // Required, NotBlank
}

// 5b. Refund Module DTOs
export interface RefundResponse {
  id: string;                 // UUID
  transactionId: string;      // UUID of the CHARGE transaction
  transactionAmount: number;
  transactionType: string;
  transactionStatus: string;
  walletId: string;           // UUID
  tenantId: string;           // UUID
  tenantName: string;
  amount: number;             // Refund amount
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | string;
  reason?: string;
  rejectReason?: string;
  requestedBy?: string;
  reviewedBy?: string;
  reviewedAt?: string;        // LocalDateTime (ISO)
  createdAt: string;          // LocalDateTime (ISO)
  updatedAt?: string;         // LocalDateTime (ISO)
}

export interface RefundCreateRequest {
  transactionId: string;      // Required, UUID of a SUCCESS CHARGE transaction
  reason?: string;            // Refund reason
  requestedBy?: string;       // Filled by backend for embed (embed:<clientId>)
}

export interface RefundApproveRequest {
  reviewedBy: string;         // Required, NotBlank (e.g. "admin")
}

export interface RefundRejectRequest {
  reviewedBy: string;         // Required, NotBlank
  rejectReason: string;       // Required, NotBlank
}

// 6. Wallet Module DTOs
export interface WalletResponse {
  id: string;             // UUID
  tenantId: string;       // UUID
  tenantName: string;
  type: 'PREPAID' | 'POSTPAID';
  balance: number;        // Current balance
  creditLimit: number;    // Credit limit
  availableBalance: number; // Balance + Credit Limit
  status: 'ACTIVE' | 'LOCKED' | 'SUSPENDED';
  createdAt: string;      // OffsetDateTime (ISO)
  updatedAt: string;      // OffsetDateTime (ISO)
}

export interface WalletCreateRequest {
  tenantId: string;       // Required, UUID
  type: 'PREPAID' | 'POSTPAID'; // Required
}

export interface WalletStatusRequest {
  status: 'ACTIVE' | 'LOCKED' | 'SUSPENDED'; // Required
}

// 7. Transaction Module DTOs
export interface TransactionResponse {
  id: string;             // UUID
  walletId: string;       // UUID
  amount: number;         // Transaction amount
  type: 'TOPUP' | 'CHARGE' | 'REFUND' | 'ADJUSTMENT' | string;
  balanceBefore: number;
  balanceAfter: number;
  availableBalanceBefore: number;
  availableBalanceAfter: number;
  status: 'SUCCESS' | 'FAILED' | 'PENDING' | string;
  description?: string;
  referenceFrom?: string; // Source system (e.g. "API")
  referenceId?: string;   // Source reference ID
  createdAt: string;      // OffsetDateTime (ISO)
}

export interface TransactionCreateRequest {
  walletId: string;       // Required, UUID
  amount: number;         // Required, positive (> 0)
  type: 'TOPUP' | 'CHARGE' | 'REFUND' | 'ADJUSTMENT' | string; // Required
  referenceFrom: string;  // Required, NotBlank (e.g. "API")
  referenceId: string;    // Required, NotBlank (e.g. "TXN-12345")
  description?: string;
}

// 8. Invoice Module DTOs
export interface InvoiceResponse {
  id: string;             // UUID
  tenantId: string;       // UUID
  tenantName: string;
  walletId: string;       // UUID
  billingPeriod: string;  // Billing period format "yyyy-MM" (e.g. "2024-01")
  totalAmount: number;    // Total invoice amount
  status: 'ISSUED' | 'PAID' | 'CANCELLED' | 'OVERDUE' | string;
  dueDate: string;        // Required OffsetDateTime (ISO)
  updatedBy?: string;     // Person last updating invoice
  createdAt: string;      // OffsetDateTime (ISO)
  updatedAt: string;      // OffsetDateTime (ISO)
}

export interface InvoiceCreateRequest {
  tenantId: string;       // Required, UUID
  walletId: string;       // Required, UUID
  billingPeriod: string;  // Required, Pattern "^\\d{4}-\\d{2}$" (e.g. "2024-01")
  totalAmount: number;    // Required, zero or positive (>= 0)
  dueDate: string;        // Required, OffsetDateTime (ISO)
  updatedBy: string;      // Required, NotBlank (e.g. "admin")
}

export interface InvoicePayRequest {
  updatedBy: string;      // Required, NotBlank (e.g. "admin")
}

// 9. Usage Log Module DTOs
export interface UsageLogResponse {
  id: string;             // UUID
  tenantId: string;       // UUID
  tenantName: string;
  serviceId: string;      // UUID
  serviceCode: string;
  walletTypeSnapshot: string; // e.g. "PREPAID"
  totalUsage: number;     // Units integer
  totalCharged: number;   // Charged amount
  creditLimitSnapshot: number;
  availableBalanceSnapshot: number;
  feeBreakdown?: Record<string, any>; // JSON Map of breakdown
  referenceFrom?: string; // Source system
  referenceId?: string;   // Source reference ID
  createdAt: string;      // OffsetDateTime (ISO)
}

export interface UsageLogCreateRequest {
  tenantId: string;       // Required, UUID
  serviceId: string;      // Required, UUID
  totalUsage: number;     // Required, positive integer (> 0)
  referenceFrom: string;  // Required, NotBlank (e.g. "API")
  referenceId: string;    // Required, NotBlank (e.g. "UL-12345")
}

// 10. Credit Adjustment Module DTOs
export interface CreditAdjustmentResponse {
  id: string;             // UUID
  walletId: string;       // UUID
  creditLimitBefore: number;
  creditLimitAfter: number;
  adjustmentAmount: number;
  type: 'INCREASE' | 'DECREASE' | string;
  reason?: string;
  referenceFrom?: string; // Source system
  referenceId?: string;   // Source reference ID
  createdBy: string;      // Identifier
  createdAt: string;      // OffsetDateTime (ISO)
}

export interface CreditAdjustmentCreateRequest {
  walletId: string;       // Required, UUID
  type: 'INCREASE' | 'DECREASE' | string; // Required
  adjustmentAmount: number;// Required, BigDecimal
  reason: string;         // Required, NotBlank (e.g. "Credit limit increase request")
  referenceFrom: string;  // Required, NotBlank (e.g. "API")
  referenceId: string;    // Required, NotBlank (e.g. "CA-12345")
  createdBy: string;      // Required, NotBlank (e.g. "admin")
}

// 11. Auth Module DTOs
export interface SendOtpRequest {
  email: string;          // Required, valid email format
}

export interface VerifyOtpRequest {
  email: string;          // Required, valid email format
  otp: string;            // Required, 6 digits
}

export interface AuthResponse {
  token: string;          // Admin session token (UUID)
  email: string;          // Admin email
  expiresInHours: number; // Token expiry in hours
  roleName?: string;      // User role name
  permissions?: string[]; // Effective permissions
  loginMethod?: string;   // How the user logged in: OTP | PASSWORD
}

export interface PasswordLoginRequest {
  email: string;          // Required, valid email format
  password: string;       // Required
}

export interface SetupPasswordRequest {
  email: string;          // Required, valid email format
  newPassword: string;    // Required, min 8 chars
}

export interface ChangePasswordRequest {
  oldPassword: string;    // Required
  newPassword: string;    // Required, min 8 chars
}

export interface LoginConfigResponse {
  loginMode: string;            // otp | password | auto
  passwordLoginEnabled: boolean;
  otpLoginEnabled: boolean;
  passwordSetupOpen: boolean;
  otpLength?: number;           // OTP code length
  otpExpiryMinutes?: number;    // OTP expiry in minutes
  resendCooldownSeconds?: number; // Min seconds between OTP requests
  passwordMinLength?: number;   // Min password length
  allowedDomains?: string;      // Comma-separated allowed email domains
}

// 12. System Config Module DTOs
export interface SystemConfigResponse {
  id: string;
  key: string;
  value: string;
  group: string;
  description?: string;
  fieldType?: string;   // text | number | password | textarea | select | radio | boolean | time
  fieldOptions?: string; // JSON: [{"value":"","label":""}] for select/radio
  updatedAt: string;
}

export interface SystemConfigUpdateRequest {
  key: string;
  value: string;
}

export interface ConfigChangeDto {
  key: string;
  oldValue: string | null;
  newValue: string | null;
  changeType: string; // CHANGED | ADDED | REMOVED
  secret: boolean;
}

export interface ConfigReloadResponse {
  changedCount: number;
  addedCount: number;
  removedCount: number;
  totalChanged: number;
  totalCount: number;
  loadedAt: string;
  changes: ConfigChangeDto[];
}

// 13. RBAC Module DTOs
export interface RoleResponse {
  id: string;
  name: string;
  description?: string;
  isSystem: boolean;
  permissionIds: string[];
  permissionCodes: string[];
  createdAt: string;
  updatedAt?: string;
}

export interface RoleCreateRequest {
  name: string;
  description?: string;
  permissionIds?: string[];
}

export interface RoleUpdateRequest {
  name?: string;
  description?: string;
  permissionIds?: string[];
}

export interface PermissionResponse {
  id: string;
  code: string;
  module: string;
  description?: string;
}

export interface AdminUserResponse {
  id: string;
  email: string;
  displayName?: string;
  isActive: boolean;
  roleName?: string;
  roleId?: string;
  permissions: string[];
  lastLoginAt?: string;
  createdAt: string;
}

export interface UserInfoResponse {
  email: string;
  userId?: string;
  displayName?: string;
  roleId?: string;
  roleName?: string;
  permissions: string[];
  overrides: UserPermissionOverrideResponse[];
}

export interface UserPermissionOverrideResponse {
  permissionId: string;
  permissionCode: string;
  isGranted: boolean;
}

// 14. Notification DTOs
export interface NotificationResponse {
  id: string;
  tenantId: string;
  type: string;
  title: string;
  message: string;
  referenceType?: string;
  referenceId?: string;
  isRead: boolean;
  readAt?: string;
  createdAt: string;
}
