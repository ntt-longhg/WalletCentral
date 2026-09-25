import React, { useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { formatCurrency, formatDate, getStatusConfig } from '@/lib/utils';
import {
  Users,
  CreditCard,
  Boxes,
  Clock,
  Undo2,
  ArrowUpRight,
  TrendingUp,
  Activity,
  CheckCircle,
  RefreshCw,
  Wallet,
  LayoutDashboard,
  ArrowDownLeft,
} from 'lucide-react';
import { useBillingStore, useServiceStore, useTransactionStore } from '@/stores';
import { useNavigate } from 'react-router-dom';

export const AdminDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { tenants, wallets, pendingPlansCount, pendingRefunds, fetchTenants, fetchWallets, fetchPendingWalletPlans, fetchPendingRefunds, tenantsLoading } = useBillingStore();
  const { services, fetchServices } = useServiceStore();
  const { transactions, fetchTransactions, transactionsLoading } = useTransactionStore();

  const loading = tenantsLoading || transactionsLoading;

  const fetchDashboardData = async () => {
    await Promise.allSettled([
      fetchTenants(),
      fetchServices(),
      fetchPendingWalletPlans(),
      fetchPendingRefunds(),
      fetchTransactions(),
      fetchWallets(),
    ]);
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const recentTransactions = transactions.slice(0, 5);

  return (
    <div className="space-y-6">
      {/* Header Section */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <LayoutDashboard className="h-5 w-5 text-blue-600" />
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Tổng quan Hệ thống</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Theo dõi chỉ số hoạt động, lưu lượng giao dịch và phê duyệt gói cước Billing Gateway.
          </p>
        </div>
        <Button
          onClick={fetchDashboardData}
          variant="outline"
          className="self-start sm:self-auto gap-2"
          disabled={loading}
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Làm mới dữ liệu
        </Button>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-5">
        <Card className="border-slate-200 shadow-sm hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-600">Tổng Tenant</CardTitle>
            <div className="h-8 w-8 rounded-lg bg-blue-50 flex items-center justify-center text-blue-600">
              <Users className="h-4 w-4" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-slate-900">{tenants.length}</div>
            <p className="text-xs text-slate-500 mt-1">Đối tác đang kết nối</p>
          </CardContent>
        </Card>

        <Card className="border-slate-200 shadow-sm hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-600">Dịch vụ</CardTitle>
            <div className="h-8 w-8 rounded-lg bg-emerald-50 flex items-center justify-center text-emerald-600">
              <Boxes className="h-4 w-4" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-slate-900">{services.length}</div>
            <p className="text-xs text-slate-500 mt-1">Dịch vụ đang hoạt động</p>
          </CardContent>
        </Card>

        <Card className="border-slate-200 shadow-sm hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-600">Tổng Ví</CardTitle>
            <div className="h-8 w-8 rounded-lg bg-indigo-50 flex items-center justify-center text-indigo-600">
              <Wallet className="h-4 w-4" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-slate-900">{wallets.length}</div>
            <p className="text-xs text-slate-500 mt-1">Ví trả trước & trả sau</p>
          </CardContent>
        </Card>

        <Card
          className="border-slate-200 shadow-sm hover:shadow-md transition-shadow cursor-pointer"
          onClick={() => navigate('/admin/wallet-plans/pending')}
        >
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-600">Chờ duyệt gói</CardTitle>
            <div className="h-8 w-8 rounded-lg bg-amber-50 flex items-center justify-center text-amber-600">
              <Clock className="h-4 w-4" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-amber-600">{pendingPlansCount}</div>
            <p className="text-xs text-slate-500 mt-1">
              {pendingPlansCount > 0 ? (
                <span className="text-amber-600 font-medium">Cần xử lý</span>
              ) : (
                'Không có yêu cầu chờ'
              )}
            </p>
          </CardContent>
        </Card>

        <Card
          className="border-slate-200 shadow-sm hover:shadow-md transition-shadow cursor-pointer"
          onClick={() => navigate('/admin/refunds')}
        >
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-600">Chờ duyệt hoàn tiền</CardTitle>
            <div className="h-8 w-8 rounded-lg bg-orange-50 flex items-center justify-center text-orange-600">
              <Undo2 className="h-4 w-4" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-orange-600">{pendingRefunds.length}</div>
            <p className="text-xs text-slate-500 mt-1">
              {pendingRefunds.length > 0 ? (
                <span className="text-orange-600 font-medium">Cần xử lý</span>
              ) : (
                'Không có yêu cầu chờ'
              )}
            </p>
          </CardContent>
        </Card>

        <Card className="border-slate-200 shadow-sm hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-600">Giao dịch</CardTitle>
            <div className="h-8 w-8 rounded-lg bg-purple-50 flex items-center justify-center text-purple-600">
              <CreditCard className="h-4 w-4" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-slate-900">{transactions.length}</div>
            <p className="text-xs text-slate-500 mt-1">Ghi nhận từ hệ thống</p>
          </CardContent>
        </Card>
      </div>

      {/* Main Grid: Recent Transactions */}
      <Card className="border-slate-200 shadow-sm">
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Giao dịch phát sinh gần đây</CardTitle>
            <CardDescription>Biến động nạp tiền và phí dịch vụ mới nhất</CardDescription>
          </div>
        </CardHeader>
        <CardContent>
          {recentTransactions.length === 0 ? (
            <div className="text-center py-8 text-slate-500 text-sm">
              Chưa có dữ liệu giao dịch hoặc chưa kết nối API Backend.
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Mã ví</TableHead>
                  <TableHead>Loại Giao dịch</TableHead>
                  <TableHead>Số tiền</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead>Thời gian</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {recentTransactions.map((txn) => {
                  const statusConf = getStatusConfig(txn.status as any);
                  return (
                    <TableRow key={txn.id}>
                      <TableCell className="font-mono text-xs font-medium text-slate-700">
                        {txn.walletId.slice(0, 8)}...
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          {txn.type === 'DEPOSIT' ? (
                            <div className="h-7 w-7 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center">
                              <ArrowDownLeft className="h-4 w-4" />
                            </div>
                          ) : (
                            <div className="h-7 w-7 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center">
                              <ArrowUpRight className="h-4 w-4" />
                            </div>
                          )}
                          <div>
                            <span className="font-semibold text-slate-800 text-xs block">{txn.type}</span>
                            {txn.description && <span className="text-[11px] text-slate-400">{txn.description}</span>}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="font-semibold">
                        <span className={txn.type === 'DEPOSIT' ? 'text-emerald-600' : 'text-slate-900'}>
                          {txn.type === 'DEPOSIT' ? '+' : '-'}
                          {formatCurrency(txn.amount)}
                        </span>
                      </TableCell>
                      <TableCell>
                        <Badge variant={statusConf.variant}>
                          {statusConf.label}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-xs text-slate-500">
                        {formatDate(txn.createdAt)}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
};
