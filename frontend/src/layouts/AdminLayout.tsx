import React from 'react';
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Boxes,
  FileSpreadsheet,
  Clock,
  Undo2,
  Building2,
  Wallet,
  Bell,
  UserCheck,
  ChevronRight,
  ShieldAlert,
  LogOut,
  Code,
  Play,
  Settings,
  FileText,
  Shield,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/context/AuthContext';
import { useNotifications } from '@/context/NotificationContext';
import { NotificationCenter } from '@/components/NotificationCenter';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';
import { useTranslation } from 'react-i18next';
// import Logo from '@/assets/logo.svg';
import { Mascot } from 'page-mascot'


export const AdminLayout: React.FC = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const { adminLogout, adminEmail } = useAuth();
  const { pendingWalletPlanCount, pendingRefundCount } = useNotifications();

  const menuItems = [
    { path: '/admin', label: 'Tổng quan', icon: LayoutDashboard },
    { path: '/admin/services', label: 'Danh mục dịch vụ', icon: Boxes },
    { path: '/admin/pricing-plans', label: 'Bảng giá Tenant', icon: FileSpreadsheet },
    { path: '/admin/wallets', label: 'Quản lý ví', icon: Wallet },
    { path: '/admin/wallet-plans/pending', label: 'Duyệt gói cước', icon: Clock, badge: pendingWalletPlanCount },
    { path: '/admin/refunds', label: 'Duyệt hoàn tiền', icon: Undo2, badge: pendingRefundCount },
    { path: '/admin/tenants', label: 'Quản lý Tenant', icon: Building2 },
    { path: '/admin/invoices', label: 'Hóa đơn', icon: FileText },
    { path: '/admin/rbac', label: 'Phân quyền (RBAC)', icon: Shield },
  ];

  const toolItems = [
    { path: '/admin/docs/embed', label: 'Hướng dẫn nhúng', icon: Code },
    { path: '/admin/demo', label: 'Thử nghiệm nhúng', icon: Play },
    { path: '/admin/settings', label: 'Cấu hình hệ thống', icon: Settings }
  ];

  const handleLogout = () => {
    adminLogout();
    navigate('/admin/login');
  };

  return (
    <div className="flex min-h-screen bg-slate-50 font-sans text-slate-900">
      {/* Sidebar */}
      <aside className="w-64 bg-slate-900 text-white flex flex-col shrink-0 border-r border-slate-800 shadow-lg h-[100vh]">
        {/* Brand Header */}
        <div className="h-16 px-6 flex items-center justify-between border-b border-slate-800">
          <div className="flex items-center gap-3">
            {/* <img src={Logo} alt="Logo" className="h-9 w-9" /> */}
            <Mascot size={50} directions="/mascots/cloudpbx-directions.webp" reactions="/mascots/cloudpbx-reactions.webp" />
            <div>
              <h2 className="font-bold text-base tracking-wide text-white">WalletCentral</h2>
              <p className="text-xs text-slate-400">Admin Control Panel</p>
            </div>
          </div>
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 p-4 space-y-1 overflow-y-auto">
          <p className="px-3 text-[11px] font-semibold tracking-wider text-slate-400 uppercase mb-2">
            Quản trị hệ thống
          </p>
          {menuItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                className={cn(
                  'flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150',
                  isActive
                    ? 'bg-blue-600 text-white shadow-md shadow-blue-900/30'
                    : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                )}
              >
                <div className="flex items-center gap-3">
                  <Icon className={cn('h-4 w-4', isActive ? 'text-white' : 'text-slate-400')} />
                  <span>{item.label}</span>
                </div>
                {item.badge && item.badge > 0 ? (
                  <Badge variant="destructive" className="text-[10px] px-1.5 py-0">
                    {item.badge}
                  </Badge>
                ) : (
                  isActive && <ChevronRight className="h-4 w-4 text-white/70" />
                )}
              </Link>
            );
          })}

          <div className="pt-4 mt-4 border-t border-slate-800">
            <p className="px-3 text-[11px] font-semibold tracking-wider text-slate-400 uppercase mb-2">
              Công cụ
            </p>
            {toolItems.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.path;
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={cn(
                    'flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150',
                    isActive
                      ? 'bg-blue-600 text-white shadow-md shadow-blue-900/30'
                      : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                  )}
                >
                  <div className="flex items-center gap-3">
                    <Icon className={cn('h-4 w-4', isActive ? 'text-white' : 'text-slate-400')} />
                    <span>{item.label}</span>
                  </div>
                </Link>
              );
            })}
          </div>
        </nav>

        {/* Logout Button */}
        <div className="p-4 border-t border-slate-800">
          <Button
            variant="ghost"
            className="w-full justify-start text-slate-300 hover:text-white hover:bg-slate-800 gap-3"
            onClick={handleLogout}
          >
            <LogOut className="h-4 w-4" />
            <span className="text-sm">Đăng xuất</span>
          </Button>
        </div>

        {/* System Footer Info */}
        <div className="p-4 border-t border-slate-800 bg-slate-950/50">
          <div className="flex items-center gap-3">
            <div className="h-8 w-8 rounded-full bg-slate-800 flex items-center justify-center text-slate-300">
              <ShieldAlert className="h-4 w-4" />
            </div>
            <div className="text-xs">
              <p className="font-medium text-slate-200">Phiên bản UI 1.0.0</p>
              <p className="text-slate-400">Spring Boot REST API v1</p>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Container */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Header */}
        <header className="h-16 bg-white border-b border-slate-200 px-6 flex items-center justify-between shadow-xs sticky top-0 z-10">
          <div className="flex items-center gap-2 text-sm text-slate-500">
            <span className="font-medium text-slate-700">WalletCentral Platform</span>
            <span>/</span>
            <span className="capitalize text-slate-900 font-semibold">
              {location.pathname === '/admin'
                ? 'Tổng quan'
                : location.pathname.split('/').pop()?.replace('-', ' ')}
            </span>
          </div>

          <div className="flex items-center gap-4">
            <NotificationCenter />
            <LanguageSwitcher />
            <div className="h-6 w-[1px] bg-slate-200" />
            <div className="flex items-center gap-3">
              <div className="h-8 w-8 rounded-full bg-slate-200 flex items-center justify-center text-slate-700 font-semibold">
                <UserCheck className="h-4 w-4" />
              </div>
              <div className="text-sm">
                <p className="font-medium text-slate-800 leading-tight">Administrator</p>
                <p className="text-xs text-slate-500">{adminEmail || 'admin@dntg.com.vn'}</p>
              </div>
            </div>
          </div>
        </header>

        {/* Content Outlet */}
        <main className="flex-1 overflow-hidden max-h-[calc(100vh-64px-49px)]">
          <div className="overflow-y-auto h-full p-4 md:p-4">
            <Outlet />
          </div>
        </main>

        {/* Footer */}
        <footer className="py-4 px-6 bg-white border-t border-slate-200 text-center text-xs text-slate-500">
          © {new Date().getFullYear()} WalletCentral Platform. Tất cả các quyền được bảo lưu.
        </footer>
      </div>
    </div>
  );
};
