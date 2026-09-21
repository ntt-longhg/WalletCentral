import React, { Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AdminLayout } from '../layouts/AdminLayout';
import { EmbedLayout } from '../layouts/EmbedLayout';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { NotificationProvider } from '../context/NotificationContext';
import { ToastProvider } from '../components/ui/toast';
import { Loader2 } from 'lucide-react';

import { AdminLoginPage } from '../pages/admin/AdminLoginPage';
import { AdminDashboard } from '../pages/admin/AdminDashboard';
import { ServiceCatalogPage } from '../pages/admin/ServiceCatalogPage';
import { PricingPlansPage } from '../pages/admin/PricingPlansPage';
import { PendingWalletPlansPage } from '../pages/admin/PendingWalletPlansPage';
import { TenantManagementPage } from '../pages/admin/TenantManagementPage';
import { WalletManagementPage } from '../pages/admin/WalletManagementPage';
import { AdminSettingsPage } from '../pages/admin/AdminSettingsPage';
import { AdminInvoicePage } from '../pages/admin/AdminInvoicePage';
import { AdminRbacPage } from '../pages/admin/AdminRbacPage';

import { EmbedWalletPage } from '../pages/embed/EmbedWalletPage';
import { EmbedTransactionsPage } from '../pages/embed/EmbedTransactionsPage';
import { EmbedInvoicesPage } from '../pages/embed/EmbedInvoicesPage';
import { EmbedReportsPage } from '../pages/embed/EmbedReportsPage';
import { EmbedDocsPage } from '../pages/embed/EmbedDocsPage';
import { EmbedDemoPage } from '../pages/embed/EmbedDemoPage';

const AdminRouteGuard: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAdmin, authReady } = useAuth();

  if (!authReady) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="flex items-center gap-3 text-slate-500">
          <Loader2 className="h-6 w-6 animate-spin" />
          <span className="text-sm">Đang tải...</span>
        </div>
      </div>
    );
  }

  if (!isAdmin) {
    return <AdminLoginPage />;
  }
  return <>{children}</>;
};

const ProtectedRoute: React.FC<{ children: React.ReactNode; permission?: string }> = ({ children, permission }) => {
  const { hasPermission } = useAuth();

  if (permission && !hasPermission(permission)) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-slate-800">403 - Access Denied</h2>
          <p className="text-slate-500 mt-2">You don't have permission to access this page.</p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
};

export const AppRoutes: React.FC = () => {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <BrowserRouter>
        <AuthProvider>
          <ToastProvider>
            <Routes>
              {/* Default Redirect */}
              <Route path="/" element={<Navigate to="/admin" replace />} />

              {/* Admin Login (public) */}
              <Route path="/admin/login" element={<AdminLoginPage />} />

              {/* ------------------------------------------------------------- */}
              {/* 1. NHÁNH ROUTER /admin (Giao diện Quản trị Nội bộ)          */}
              {/* ------------------------------------------------------------- */}
              <Route
                path="/admin"
                element={
                  <AdminRouteGuard>
                    <NotificationProvider admin>
                      <AdminLayout />
                    </NotificationProvider>
                  </AdminRouteGuard>
                }
              >
                <Route index element={<AdminDashboard />} />
                <Route path="services" element={
                  <ProtectedRoute permission="SERVICE_VIEW">
                    <ServiceCatalogPage />
                  </ProtectedRoute>
                } />
                <Route path="pricing-plans" element={
                  <ProtectedRoute permission="PRICING_VIEW">
                    <PricingPlansPage />
                  </ProtectedRoute>
                } />
                <Route path="wallet-plans/pending" element={
                  <ProtectedRoute permission="PLAN_VIEW">
                    <PendingWalletPlansPage />
                  </ProtectedRoute>
                } />
                <Route path="tenants" element={
                  <ProtectedRoute permission="TENANT_VIEW">
                    <TenantManagementPage />
                  </ProtectedRoute>
                } />
                <Route path="wallets" element={
                  <ProtectedRoute permission="WALLET_VIEW">
                    <WalletManagementPage />
                  </ProtectedRoute>
                } />
                <Route path="invoices" element={
                  <ProtectedRoute permission="INVOICE_VIEW">
                    <AdminInvoicePage />
                  </ProtectedRoute>
                } />
                <Route path="rbac" element={
                  <ProtectedRoute permission="RBAC_VIEW">
                    <AdminRbacPage />
                  </ProtectedRoute>
                } />
                <Route path="docs/embed" element={<EmbedDocsPage />} />
                <Route path="demo" element={<EmbedDemoPage />} />
                <Route path="settings" element={
                  <ProtectedRoute permission="SETTINGS_VIEW">
                    <AdminSettingsPage />
                  </ProtectedRoute>
                } />
              </Route>

              {/* ------------------------------------------------------------- */}
              {/* 2. NHÁNH ROUTER /embed (Giao diện Nhúng iFrame cho Client)    */}
              {/* ------------------------------------------------------------- */}
              <Route path="/embed" element={<EmbedLayout />}>
                <Route index element={<Navigate to="/embed/wallet" replace />} />
                <Route path="wallet" element={<EmbedWalletPage />} />
                <Route path="transactions" element={<EmbedTransactionsPage />} />
                <Route path="invoices" element={<EmbedInvoicesPage />} />
                <Route path="reports" element={<EmbedReportsPage />} />
              </Route>

              {/* Fallback 404 Route */}
              <Route
                path="*"
                element={
                  <div className="p-10 text-center font-sans">
                    <h2 className="text-2xl font-bold text-slate-800">404 - Trang không tồn tại</h2>
                    <p className="text-slate-500 mt-2">Đường dẫn bạn yêu cầu không nằm trong hệ thống.</p>
                  </div>
                }
              />
            </Routes>
          </ToastProvider>
        </AuthProvider>
      </BrowserRouter>
    </Suspense>
  );
};
