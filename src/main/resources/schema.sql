-- =============================================
-- WalletCentral Schema Initialization Script
-- Generated: 2026-09-10
-- Order: Parent tables first, then child tables
-- =============================================

-- 1. Tenants (no FK dependencies)
CREATE TABLE IF NOT EXISTS tenants (
    id CHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    client_secret VARCHAR(255) NOT NULL,
    allowed_domains TEXT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenants_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tenants_status ON tenants(status);
CREATE INDEX idx_tenants_deleted_at ON tenants(deleted_at);

-- 2. Service Catalogs (no FK dependencies)
CREATE TABLE IF NOT EXISTS service_catalogs (
    id CHAR(36) NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_service_catalogs_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_service_catalogs_deleted_at ON service_catalogs(deleted_at);

-- 3. Pricing Plans (no FK dependencies)
CREATE TABLE IF NOT EXISTS pricing_plans (
    id CHAR(36) NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    type VARCHAR(20) NOT NULL DEFAULT 'BALANCE_TOPUP',
    bonus_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',
    bonus_value DECIMAL(15,2) DEFAULT 0.00,
    credit_limit_action VARCHAR(10) NOT NULL DEFAULT 'NONE',
    credit_limit_value DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pricing_plans_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pricing_plans_type ON pricing_plans(type);
CREATE INDEX idx_pricing_plans_status ON pricing_plans(status);
CREATE INDEX idx_pricing_plans_deleted_at ON pricing_plans(deleted_at);

-- 4. Wallets (FK -> tenants)
CREATE TABLE IF NOT EXISTS wallets (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    type VARCHAR(10) NOT NULL DEFAULT 'PREPAID',
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    credit_limit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallets_tenant_id (tenant_id),
    CONSTRAINT fk_wallets_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_wallets_tenant_id ON wallets(tenant_id);
CREATE INDEX idx_wallets_status ON wallets(status);
CREATE INDEX idx_wallets_type ON wallets(type);

-- 5. Service Prices (FK -> service_catalogs)
CREATE TABLE IF NOT EXISTS service_prices (
    id CHAR(36) NOT NULL,
    service_id CHAR(36) NOT NULL,
    initial_size INT NOT NULL,
    initial_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    subsequent_size INT NOT NULL,
    subsequent_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    is_active TINYINT(1) DEFAULT 1,
    effective_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_service_prices_service_catalog FOREIGN KEY (service_id) REFERENCES service_catalogs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_service_prices_service_id ON service_prices(service_id);
CREATE INDEX idx_service_prices_active ON service_prices(is_active);
CREATE INDEX idx_service_prices_effective_date ON service_prices(effective_date);
CREATE INDEX idx_service_prices_deleted_at ON service_prices(deleted_at);

-- 6. Price Tiers (FK -> service_prices)
CREATE TABLE IF NOT EXISTS price_tiers (
    id CHAR(36) NOT NULL,
    service_price_id CHAR(36) NOT NULL,
    tier VARCHAR(100) NOT NULL,
    basic_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    extended_size INT NOT NULL,
    extended_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_tiers_service_price_id_tier (service_price_id, tier),
    CONSTRAINT fk_price_tiers_service_price FOREIGN KEY (service_price_id) REFERENCES service_prices(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_price_tiers_service_price_id ON price_tiers(service_price_id);

-- 7. Credit Adjustments (FK -> wallets)
CREATE TABLE IF NOT EXISTS credit_adjustments (
    id CHAR(36) NOT NULL,
    wallet_id CHAR(36) NOT NULL,
    credit_limit_before DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    credit_limit_after DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    adjustment_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    type VARCHAR(10) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    reference_from VARCHAR(100) NOT NULL,
    reference_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_credit_adjustments_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_credit_adjustments_wallet_id ON credit_adjustments(wallet_id);
CREATE INDEX idx_credit_adjustments_type ON credit_adjustments(type);
CREATE INDEX idx_credit_adjustments_reference ON credit_adjustments(reference_from, reference_id);
CREATE INDEX idx_credit_adjustments_created_at ON credit_adjustments(created_at);

-- 8. Transactions (FK -> wallets)
CREATE TABLE IF NOT EXISTS transactions (
    id CHAR(36) NOT NULL,
    wallet_id CHAR(36) NOT NULL,
    amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    type VARCHAR(10) NOT NULL DEFAULT 'CHARGE',
    balance_before DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    balance_after DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    available_balance_before DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    available_balance_after DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(10) NOT NULL DEFAULT 'SUCCESS',
    description TEXT NULL,
    reference_from VARCHAR(100) NOT NULL,
    reference_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_transactions_wallet_id ON transactions(wallet_id);
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_reference ON transactions(reference_from, reference_id);

-- 9. Wallet Plans (FK -> tenants, FK -> pricing_plans)
CREATE TABLE IF NOT EXISTS wallet_plans (
    id CHAR(36) NOT NULL,
    pricing_plan_id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    bonus_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    credited_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_at TIMESTAMP NULL DEFAULT NULL,
    approved_by VARCHAR(100) NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(255) NULL DEFAULT NULL,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_wallet_plans_pricing_plan FOREIGN KEY (pricing_plan_id) REFERENCES pricing_plans(id),
    CONSTRAINT fk_wallet_plans_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_wallet_plans_tenant_id ON wallet_plans(tenant_id);
CREATE INDEX idx_wallet_plans_pricing_plan_id ON wallet_plans(pricing_plan_id);
CREATE INDEX idx_wallet_plans_status ON wallet_plans(status);
CREATE INDEX idx_wallet_plans_created_at ON wallet_plans(created_at);
CREATE INDEX idx_wallet_plans_deleted_at ON wallet_plans(deleted_at);

-- 10. Usage Logs (FK -> tenants, FK -> service_catalogs)
CREATE TABLE IF NOT EXISTS usage_logs (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    service_id CHAR(36) NULL DEFAULT NULL,
    wallet_type_snapshot VARCHAR(10) NOT NULL DEFAULT 'PREPAID',
    total_usage INT NOT NULL,
    total_charged DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    credit_limit_snapshot DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    available_balance_snapshot DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    fee_breakdown LONGTEXT NULL,
    reference_from VARCHAR(100) NOT NULL,
    reference_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_usage_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_usage_logs_service_catalog FOREIGN KEY (service_id) REFERENCES service_catalogs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_usage_logs_tenant_id ON usage_logs(tenant_id);
CREATE INDEX idx_usage_logs_service_id ON usage_logs(service_id);
CREATE INDEX idx_usage_logs_reference ON usage_logs(reference_from, reference_id);
CREATE INDEX idx_usage_logs_created_at ON usage_logs(created_at);

-- 11. Invoices (FK -> tenants, FK -> wallets)
CREATE TABLE IF NOT EXISTS invoices (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    wallet_id CHAR(36) NOT NULL,
    billing_period VARCHAR(7) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(10) NOT NULL DEFAULT 'ISSUED',
    due_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoices_tenant_period (tenant_id, billing_period),
    CONSTRAINT fk_invoices_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_invoices_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_invoices_tenant_id ON invoices(tenant_id);
CREATE INDEX idx_invoices_wallet_id ON invoices(wallet_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_billing_period ON invoices(billing_period);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);

-- =============================================
-- 12. System Configs (no FK dependencies)
-- Stores key-value configuration for the system (SMTP, OTP, AUTH settings)
-- =============================================
CREATE TABLE IF NOT EXISTS system_configs (
    id CHAR(36) NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT NOT NULL,
    config_group VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_system_configs_key (config_key),
    INDEX idx_system_configs_group (config_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Default system configs
INSERT IGNORE INTO system_configs (id, config_key, config_value, config_group, description) VALUES
('00000000-0000-0000-0000-000000000001', 'smtp.host', 'smtp.gmail.com', 'SMTP', 'SMTP server host'),
('00000000-0000-0000-0000-000000000002', 'smtp.port', '587', 'SMTP', 'SMTP server port'),
('00000000-0000-0000-0000-000000000003', 'smtp.username', '', 'SMTP', 'SMTP username for authentication'),
('00000000-0000-0000-0000-000000000004', 'smtp.password', '', 'SMTP', 'SMTP password for authentication'),
('00000000-0000-0000-0000-000000000005', 'smtp.from-email', '', 'SMTP', 'Sender email address'),
('00000000-0000-0000-0000-000000000006', 'otp.expiry_minutes', '5', 'OTP', 'OTP expiry time in minutes'),
('00000000-0000-0000-0000-000000000007', 'otp.length', '6', 'OTP', 'OTP code length'),
('00000000-0000-0000-0000-000000000008', 'auth.allowed_domains', 'dntg.com.vn', 'AUTH', 'Comma-separated allowed email domains'),
('00000000-0000-0000-0000-000000000009', 'auth.token_expiry_hours', '24', 'AUTH', 'Admin session token expiry in hours');

-- =============================================
-- 13. Admin OTPs (no FK dependencies)
-- Stores temporary OTP codes for admin email verification
-- =============================================
CREATE TABLE IF NOT EXISTS admin_otps (
    id CHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_admin_otps_email (email),
    INDEX idx_admin_otps_expires_at (expires_at),
    INDEX idx_admin_otps_email_used (email, used)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 14. Admin Tokens (session only - cleaned up on expiry)
-- Stores active admin session tokens (UUID) for API authentication
-- =============================================
CREATE TABLE IF NOT EXISTS admin_tokens (
    id CHAR(36) NOT NULL,
    token VARCHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_tokens_token (token),
    INDEX idx_admin_tokens_email (email),
    INDEX idx_admin_tokens_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 15. Admin Users (persistent - NEVER deleted)
-- Stores admin user accounts. RBAC data is attached here,
-- so it persists across token sessions.
-- =============================================
CREATE TABLE IF NOT EXISTS admin_users (
    id CHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 15. Roles (RBAC - dynamic roles)
-- =============================================
CREATE TABLE IF NOT EXISTS roles (
    id CHAR(36) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    is_system TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 16. Permissions (RBAC - permission catalog)
-- =============================================
CREATE TABLE IF NOT EXISTS permissions (
    id CHAR(36) NOT NULL,
    code VARCHAR(100) NOT NULL,
    module VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permissions_code (code),
    INDEX idx_permissions_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 17. Role Permissions (RBAC - role-permission mapping)
-- =============================================
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id CHAR(36) NOT NULL,
    permission_id CHAR(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 19. Admin User Roles (RBAC - user-role assignment, persists across sessions)
-- =============================================
CREATE TABLE IF NOT EXISTS admin_user_roles (
    admin_user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    PRIMARY KEY (admin_user_id),
    CONSTRAINT fk_admin_user_roles_user FOREIGN KEY (admin_user_id) REFERENCES admin_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_admin_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 20. Admin User Permissions (RBAC - user-specific permission overrides, persists across sessions)
-- =============================================
CREATE TABLE IF NOT EXISTS admin_user_permissions (
    admin_user_id CHAR(36) NOT NULL,
    permission_id CHAR(36) NOT NULL,
    is_granted TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (admin_user_id, permission_id),
    CONSTRAINT fk_admin_user_permissions_user FOREIGN KEY (admin_user_id) REFERENCES admin_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_admin_user_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 20. Notifications (real-time push + persistence)
-- =============================================
CREATE TABLE IF NOT EXISTS notifications (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id CHAR(36) NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    INDEX idx_notifications_tenant_id (tenant_id),
    INDEX idx_notifications_is_read (is_read),
    INDEX idx_notifications_created_at (created_at),
    INDEX idx_notifications_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Default RBAC Seed Data
-- =============================================

-- Default Roles
INSERT IGNORE INTO roles (id, name, description, is_system) VALUES
('00000000-0000-0000-0000-000000000001', 'SUPER_ADMIN', 'Full system access - cannot be deleted or modified', 1),
('00000000-0000-0000-0000-000000000002', 'ADMIN', 'Administrative access with most permissions', 0),
('00000000-0000-0000-0000-000000000003', 'OPERATOR', 'Operational access - manage tenants, wallets, billing', 0),
('00000000-0000-0000-0000-000000000004', 'VIEWER', 'Read-only access to all modules', 0);

-- Default Permissions
INSERT IGNORE INTO permissions (id, code, module, description) VALUES
('00000000-0000-0000-0000-000000000010', 'TENANT_VIEW', 'TENANT', 'View tenants'),
('00000000-0000-0000-0000-000000000011', 'TENANT_CREATE', 'TENANT', 'Create tenants'),
('00000000-0000-0000-0000-000000000012', 'TENANT_UPDATE', 'TENANT', 'Update tenants'),
('00000000-0000-0000-0000-000000000013', 'TENANT_DELETE', 'TENANT', 'Delete tenants'),
('00000000-0000-0000-0000-000000000020', 'WALLET_VIEW', 'WALLET', 'View wallets'),
('00000000-0000-0000-0000-000000000021', 'WALLET_UPDATE', 'WALLET', 'Update wallets'),
('00000000-0000-0000-0000-000000000022', 'WALLET_TOPUP', 'WALLET', 'Topup wallets'),
('00000000-0000-0000-0000-000000000023', 'WALLET_SWITCH_TYPE', 'WALLET', 'Switch wallet type prepaid/postpaid'),
('00000000-0000-0000-0000-000000000030', 'INVOICE_VIEW', 'INVOICE', 'View invoices'),
('00000000-0000-0000-0000-000000000031', 'INVOICE_CREATE', 'INVOICE', 'Create invoices'),
('00000000-0000-0000-0000-000000000032', 'INVOICE_UPDATE', 'INVOICE', 'Update invoices'),
('00000000-0000-0000-0000-000000000033', 'INVOICE_GENERATE', 'INVOICE', 'Generate invoices from usage logs'),
('00000000-0000-0000-0000-000000000034', 'INVOICE_PAY', 'INVOICE', 'Mark invoices as paid'),
('00000000-0000-0000-0000-000000000040', 'BILLING_VIEW', 'BILLING', 'View billing data'),
('00000000-0000-0000-0000-000000000041', 'BILLING_PROCESS', 'BILLING', 'Process billing webhooks'),
('00000000-0000-0000-0000-000000000050', 'TRANSACTION_VIEW', 'TRANSACTION', 'View transactions'),
('00000000-0000-0000-0000-000000000060', 'USAGE_LOG_VIEW', 'USAGE_LOG', 'View usage logs'),
('00000000-0000-0000-0000-000000000070', 'SERVICE_VIEW', 'SERVICE', 'View services'),
('00000000-0000-0000-0000-000000000071', 'SERVICE_CREATE', 'SERVICE', 'Create services'),
('00000000-0000-0000-0000-000000000072', 'SERVICE_UPDATE', 'SERVICE', 'Update services'),
('00000000-0000-0000-0000-000000000073', 'SERVICE_DELETE', 'SERVICE', 'Delete services'),
('00000000-0000-0000-0000-000000000080', 'PRICING_VIEW', 'PRICING', 'View pricing plans'),
('00000000-0000-0000-0000-000000000081', 'PRICING_CREATE', 'PRICING', 'Create pricing plans'),
('00000000-0000-0000-0000-000000000082', 'PRICING_UPDATE', 'PRICING', 'Update pricing plans'),
('00000000-0000-0000-0000-000000000083', 'PRICING_DELETE', 'PRICING', 'Delete pricing plans'),
('00000000-0000-0000-0000-000000000090', 'PLAN_VIEW', 'PLAN', 'View wallet plans'),
('00000000-0000-0000-0000-000000000091', 'PLAN_APPROVE', 'PLAN', 'Approve wallet plans'),
('00000000-0000-0000-0000-000000000092', 'PLAN_REJECT', 'PLAN', 'Reject wallet plans'),
('00000000-0000-0000-0000-000000000100', 'REPORT_VIEW', 'REPORT', 'View reports'),
('00000000-0000-0000-0000-000000000110', 'RBAC_VIEW', 'RBAC', 'View roles and permissions'),
('00000000-0000-0000-0000-000000000111', 'RBAC_MANAGE_ROLES', 'RBAC', 'Create/update/delete roles'),
('00000000-0000-0000-0000-000000000112', 'RBAC_MANAGE_USER_PERMISSIONS', 'RBAC', 'Manage user-specific permission overrides'),
('00000000-0000-0000-0000-000000000120', 'SETTINGS_VIEW', 'SETTINGS', 'View system settings'),
('00000000-0000-0000-0000-000000000121', 'SETTINGS_UPDATE', 'SETTINGS', 'Update system settings'),
('00000000-0000-0000-0000-000000000130', 'NOTIFICATION_VIEW', 'NOTIFICATION', 'View notifications'),
('00000000-0000-0000-0000-000000000131', 'NOTIFICATION_MANAGE', 'NOTIFICATION', 'Manage notification settings');

-- SUPER_ADMIN gets ALL permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001', id FROM permissions;

-- ADMIN gets most permissions (except RBAC management)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000002', id FROM permissions WHERE code NOT IN ('RBAC_MANAGE_ROLES', 'RBAC_MANAGE_USER_PERMISSIONS');

-- OPERATOR gets operational permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000003', id FROM permissions WHERE code IN (
    'TENANT_VIEW', 'TENANT_CREATE', 'TENANT_UPDATE',
    'WALLET_VIEW', 'WALLET_UPDATE', 'WALLET_TOPUP',
    'INVOICE_VIEW', 'INVOICE_CREATE', 'INVOICE_UPDATE', 'INVOICE_GENERATE', 'INVOICE_PAY',
    'BILLING_VIEW', 'BILLING_PROCESS',
    'TRANSACTION_VIEW', 'USAGE_LOG_VIEW',
    'SERVICE_VIEW', 'PRICING_VIEW',
    'PLAN_VIEW', 'PLAN_APPROVE', 'PLAN_REJECT',
    'REPORT_VIEW', 'NOTIFICATION_VIEW'
);

-- VIEWER gets read-only permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000004', id FROM permissions WHERE code LIKE '%_VIEW';
