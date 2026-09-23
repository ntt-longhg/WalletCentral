import React, { useEffect, useMemo, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { TablePagination } from '@/components/ui/pagination';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useToast } from '@/components/ui/toast';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow, TableSkeleton } from '@/components/ui/table';
import { formatCurrency, formatDate, getStatusConfig, walletTypeConfig } from '@/lib/utils';
import { useBillingStore, useServiceStore } from '@/stores';
import {
  Wallet,
  Plus,
  RefreshCw,
  AlertCircle,
  ArrowRightLeft,
  CreditCard,
  Lock,
  Unlock,
  Search,
  WalletMinimal,
  ShoppingCart,
} from 'lucide-react';

export const WalletManagementPage: React.FC = () => {
  const { addToast } = useToast();
  const {
    wallets,
    tenants,
    walletsLoading,
    tenantsLoading,
    fetchWallets,
    fetchTenants,
    createWallet,
    updateWalletStatus,
    createWalletPlan,
  } = useBillingStore();
  const { pricingPlans, fetchPricingPlans } = useServiceStore();

  const loading = walletsLoading || tenantsLoading;
  const plans = pricingPlans.filter((p) => p.status === 'ACTIVE');
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);

  // Create wallet dialog
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [createData, setCreateData] = useState({
    tenantId: '',
    type: 'PREPAID' as 'PREPAID' | 'POSTPAID'
  });

  // Add plan to wallet dialog
  const [showAddPlanDialog, setShowAddPlanDialog] = useState(false);
  const [addPlanData, setAddPlanData] = useState({
    walletId: '',
    pricingPlanId: '',
  });

  // Switch type dialog
  const [showSwitchDialog, setShowSwitchDialog] = useState(false);
  const [switchData, setSwitchData] = useState<{
    wallet: { id: string; tenantId: string; type: string; tenantName: string; balance: number; creditLimit: number; availableBalance: number; status: string; createdAt: string };
    newType: 'PREPAID' | 'POSTPAID';
  } | null>(null);

  const fetchAllData = async () => {
    await Promise.allSettled([fetchWallets(), fetchTenants(), fetchPricingPlans()]);
  };

  useEffect(() => {
    fetchAllData();
  }, []);

  const filteredWallets = useMemo(() => {
    return wallets.filter((w) => {
      if (!searchTerm) return true;
      const term = searchTerm.toLowerCase();
      return (
        w.tenantName.toLowerCase().includes(term) ||
        w.type.toLowerCase().includes(term) ||
        w.status.toLowerCase().includes(term)
      );
    });
  }, [wallets, searchTerm]);

  const totalPages = Math.max(1, Math.ceil(filteredWallets.length / pageSize));
  const hasPrevious = currentPage > 1;
  const hasNext = currentPage < totalPages;

  const paginatedWallets = useMemo(() => {
    const startIndex = (currentPage - 1) * pageSize;
    return filteredWallets.slice(startIndex, startIndex + pageSize);
  }, [filteredWallets, currentPage, pageSize]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, pageSize]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  // Get filtered plans based on wallet type
  const getFilteredPlansForWallet = (walletType: string) => {
    if (walletType === 'PREPAID') {
      return plans.filter((p) => p.type === 'BALANCE_TOPUP');
    }
    return plans.filter((p) => p.type === 'CREDIT_INCREASE');
  };

  const handleCreateWallet = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createWallet(createData);
      addToast({ variant: 'success', message: 'Tạo ví thành công!' });
      setShowCreateDialog(false);
      setCreateData({ tenantId: '', type: 'PREPAID' });
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Không thể tạo ví.' });
    }
  };

  const handleAddPlan = async (e: React.FormEvent) => {
    e.preventDefault();
    const wallet = wallets.find((w) => w.id === addPlanData.walletId);
    if (!wallet) return;

    try {
      await createWalletPlan({
        tenantId: wallet.tenantId,
        pricingPlanId: addPlanData.pricingPlanId,
        createdBy: 'admin',
      });
      addToast({ variant: 'success', message: 'Đã thêm gói vào ví! Yêu cầu đang chờ duyệt.' });
      setShowAddPlanDialog(false);
      setAddPlanData({ walletId: '', pricingPlanId: '' });
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Không thể thêm gói.' });
    }
  };

  const handleSwitchType = async () => {
    if (!switchData) return;
    const { wallet, newType } = switchData;
    const oldType = wallet.type;

    if (oldType === 'POSTPAID' && newType === 'PREPAID') {
      try {
        const filteredPlans = getFilteredPlansForWallet('PREPAID');
        if (filteredPlans.length > 0) {
          await createWalletPlan({
            tenantId: wallet.tenantId,
            pricingPlanId: filteredPlans[0].id,
            createdBy: 'admin',
          });
        }
        addToast({ variant: 'success', message: 'Chuyển từ trả sau sang trả trước thành công! Đã lập hóa đơn và điều chỉnh credit limit.' });
      } catch (err: any) {
        addToast({ variant: 'destructive', message: err.response?.data?.message || 'Lỗi khi chuyển loại ví.' });
        setShowSwitchDialog(false);
        return;
      }
    } else {
      addToast({ variant: 'success', message: 'Chuyển loại ví thành công. Số dư được giữ nguyên.' });
    }

    setShowSwitchDialog(false);
    fetchWallets();
  };

  const handleToggleStatus = async (wallet: { id: string; status: string }) => {
    const nextStatus = wallet.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    try {
      await updateWalletStatus(wallet.id, { status: nextStatus });
      addToast({
        variant: 'success',
        message: `Đã ${nextStatus === 'ACTIVE' ? 'kích hoạt' : 'đình chỉ'} ví thành công.`,
      });
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Lỗi cập nhật trạng thái.' });
    }
  };

  const columns = [
    {
      header: 'Tenant',
      accessor: 'tenantName',
      widthClass: 'w-[140px]',
    },
    {
      header: 'Loại Ví',
      accessor: 'type',
      widthClass: 'w-[120px]',
    },
    {
      header: 'Số Dư (Balance)',
      accessor: 'balance',
      widthClass: 'w-[140px]',
    },
    {
      header: 'Hạn Mức (Credit)',
      accessor: 'creditLimit',
      widthClass: 'w-[140px]',
    },
    {
      header: 'Số Dư Khả Dụng',
      accessor: 'availableBalance',
      widthClass: 'w-[140px]',
    },
    {
      header: 'Trạng Thái',
      accessor: 'status',
      widthClass: 'w-[120px]',
    },
    {
      header: 'Ngày Tạo',
      accessor: 'createdAt',
      widthClass: 'w-[140px]',
    },
    {
      header: 'Thao Tác',
      accessor: 'actions',
      widthClass: 'w-[160px]',
    },
  ]

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Wallet className="h-5 w-5 text-blue-600" />
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Quản lý Ví Tenant</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Quản lý ví trả trước/trả sau, nạp tiền, chuyển loại ví và theo dõi số dư.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => fetchAllData()} disabled={loading} className="gap-1.5">
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </Button>
          <Button onClick={() => setShowAddPlanDialog(true)} variant="outline" className="gap-2">
            <ShoppingCart className="h-4 w-4" /> Thêm Gói vào Ví
          </Button>
          <Button onClick={() => setShowCreateDialog(true)} className="gap-2 bg-blue-600 hover:bg-blue-700">
            <Plus className="h-4 w-4" /> Tạo Ví Mới
          </Button>
        </div>
      </div>

      {/* Search */}
      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
        <Input
          placeholder="Tìm kiếm theo tên tenant, loại ví, trạng thái..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="pl-9"
        />
      </div>

      {/* Wallet Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="border-slate-200 shadow-sm">
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-blue-50 flex items-center justify-center">
                <Wallet className="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-slate-900">{wallets.length}</p>
                <p className="text-xs text-slate-500">Tổng số ví</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card className="border-slate-200 shadow-sm">
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-emerald-50 flex items-center justify-center">
                <WalletMinimal className="h-5 w-5 text-emerald-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-emerald-600">
                  {wallets.filter((w) => w.type === 'PREPAID').length}
                </p>
                <p className="text-xs text-slate-500">Ví trả trước</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card className="border-slate-200 shadow-sm">
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-purple-50 flex items-center justify-center">
                <CreditCard className="h-5 w-5 text-purple-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-purple-600">
                  {wallets.filter((w) => w.type === 'POSTPAID').length}
                </p>
                <p className="text-xs text-slate-500">Ví trả sau</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Wallet Table */}
      <Card className="border-slate-200 shadow-sm">
        <CardHeader className="pb-3">
          <CardTitle className="text-base font-semibold flex items-center gap-2">
            <Wallet className="h-4 w-4 text-blue-600" /> Danh sách Ví
          </CardTitle>
          <CardDescription>Tất cả ví trong hệ thống ({filteredWallets.length} kết quả)</CardDescription>
        </CardHeader>
        <CardContent>
          {filteredWallets.length === 0 ? (
            <div className="text-center py-10 text-slate-400 text-sm">
              {searchTerm ? 'Không tìm thấy ví phù hợp.' : 'Chưa có ví nào trong hệ thống.'}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  {
                    columns.map((col) => (
                      <TableCell key={col.accessor} className={col.widthClass}>
                        {col.header}
                      </TableCell>
                    ))
                  }
                </TableRow>
              </TableHeader>
              <TableBody>
                {
                  walletsLoading ? (
                    <TableSkeleton columns={columns.length} rows={pageSize} />
                  ) : (
                    <>
                      {paginatedWallets.map((wallet) => {
                        const typeConf = walletTypeConfig[wallet.type as keyof typeof walletTypeConfig];
                        const statusConf = getStatusConfig(wallet.status as any);
                        return (
                          <TableRow key={wallet.id}>
                            <TableCell>
                              <div className="font-medium text-slate-900 text-sm">{wallet.tenantName}</div>
                              <div className="text-xs text-slate-400 font-mono">{wallet.id.slice(0, 8)}...</div>
                            </TableCell>
                            <TableCell>
                              <Badge variant="outline" className={`text-xs ${typeConf?.textClass || ''}`}>
                                {typeConf?.label || wallet.type}
                              </Badge>
                            </TableCell>
                            <TableCell className="font-semibold text-slate-900">
                              {formatCurrency(wallet.balance)}
                            </TableCell>
                            <TableCell className="text-sm text-slate-600">
                              {formatCurrency(wallet.creditLimit)}
                            </TableCell>
                            <TableCell className="font-semibold text-blue-600">
                              {formatCurrency(wallet.availableBalance)}
                            </TableCell>
                            <TableCell>
                              <Badge variant={statusConf.variant} className="text-xs">
                                {statusConf.label}
                              </Badge>
                            </TableCell>
                            <TableCell className="text-xs text-slate-500">
                              {formatDate(wallet.createdAt)}
                            </TableCell>
                            <TableCell>
                              <div className="flex items-center gap-1.5">
                                <Button
                                  size="sm"
                                  variant="outline"
                                  className="text-xs h-7 gap-1"
                                  onClick={() => {
                                    setAddPlanData({ walletId: wallet.id, pricingPlanId: '' });
                                    setShowAddPlanDialog(true);
                                  }}
                                >
                                  <ShoppingCart className="h-3 w-3" /> Thêm gói
                                </Button>
                                <Button
                                  size="sm"
                                  variant="outline"
                                  className="text-xs h-7 gap-1"
                                  onClick={() => {
                                    setSwitchData({ wallet, newType: wallet.type === 'PREPAID' ? 'POSTPAID' : 'PREPAID' });
                                    setShowSwitchDialog(true);
                                  }}
                                >
                                  <ArrowRightLeft className="h-3 w-3" />
                                  {wallet.type === 'PREPAID' ? 'Chuyển PS' : 'Chuyển TT'}
                                </Button>
                                <Button
                                  size="sm"
                                  variant={wallet.status === 'ACTIVE' ? 'destructive' : 'success'}
                                  className="text-xs h-7 gap-1"
                                  onClick={() => handleToggleStatus(wallet)}
                                >
                                  {wallet.status === 'ACTIVE' ? (
                                    <><Lock className="h-3 w-3" /> Đình chỉ</>
                                  ) : (
                                    <><Unlock className="h-3 w-3" /> Kích hoạt</>
                                  )}
                                </Button>
                              </div>
                            </TableCell>
                          </TableRow>
                        );
                      })}
                    </>
                  )
                }
              </TableBody>
            </Table>
          )}

          {filteredWallets.length > 0 && (
            <TablePagination
              pageSize={pageSize}
              onPageSizeChange={(nextSize) => {
                setPageSize(nextSize);
                setCurrentPage(1);
              }}
              onPrevious={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
              onNext={() => setCurrentPage((prev) => Math.min(totalPages, prev + 1))}
              hasPrevious={hasPrevious}
              hasNext={hasNext}
              summaryText={`${filteredWallets.length} bản ghi trong hệ thống`}
              pageSizeOptions={[5, 10, 20, 50]}
            />
          )}
        </CardContent>
      </Card>

      {/* Create Wallet Dialog */}
      <Dialog
        open={showCreateDialog}
        onOpenChange={(open) => {
          setShowCreateDialog(open);
          if (!open) {
            setCreateData({ tenantId: '', type: 'PREPAID' });
          }
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Tạo Ví Mới</DialogTitle>
            <DialogDescription>Tạo ví cho tenant trong hệ thống billing</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleCreateWallet} className="space-y-4">
            <div className="space-y-2">
              <Label>Tenant</Label>
              <Select
                value={createData.tenantId}
                onValueChange={(value) => setCreateData({ ...createData, tenantId: value })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Chọn tenant" />
                </SelectTrigger>
                <SelectContent>
                  {tenants
                    .filter((t) => !wallets.find((w) => w.tenantId === t.id))
                    .map((tenant) => (
                      <SelectItem key={tenant.id} value={tenant.id}>
                        {tenant.name}
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Loại Ví</Label>
              <div className="rounded-xl border border-slate-200 bg-slate-50 p-1">
                <div className="grid grid-cols-2 gap-1">
                  {(['PREPAID', 'POSTPAID'] as const).map((type) => (
                    <button
                      key={type}
                      type="button"
                      onClick={() => setCreateData({ ...createData, type })}
                      className={[
                        'rounded-lg px-3 py-2 text-sm font-medium transition-all duration-200',
                        createData.type === type
                          ? 'bg-white text-blue-700 shadow-sm ring-1 ring-blue-200'
                          : 'text-slate-600 hover:text-slate-900',
                      ].join(' ')}
                    >
                      {type === 'PREPAID' ? 'Trả trước (Prepaid)' : 'Trả sau (Postpaid)'}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* {createData.type === 'POSTPAID' && (
              <div className="space-y-2">
                <Label>Hạn mức Tín dụng (Credit Limit)</Label>
                <Input
                  type="number"
                  min={0}
                  value={createData.creditLimit}
                  onChange={(e) => setCreateData({ ...createData, creditLimit: Number(e.target.value) })}
                />
              </div>
            )} */}

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setShowCreateDialog(false)}>
                Hủy
              </Button>
              <Button type="submit" disabled={!createData.tenantId}>
                Tạo Ví
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Add Plan to Wallet Dialog */}
      <Dialog
        open={showAddPlanDialog}
        onOpenChange={(open) => {
          setShowAddPlanDialog(open);
          if (!open) {
            setAddPlanData({ walletId: '', pricingPlanId: '' });
          }
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Thêm Gói vào Ví</DialogTitle>
            <DialogDescription>
              Chọn ví và gói cước để thêm. Yêu cầu sẽ được gửi đến danh sách chờ duyệt.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleAddPlan} className="space-y-4">
            <div className="space-y-2">
              <Label>Chọn Ví</Label>
              <Select
                value={addPlanData.walletId}
                onValueChange={(value) => setAddPlanData({ ...addPlanData, walletId: value, pricingPlanId: '' })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Chọn ví cần thêm gói" />
                </SelectTrigger>
                <SelectContent>
                  {wallets.map((wallet) => (
                    <SelectItem key={wallet.id} value={wallet.id}>
                      {wallet.tenantName} - {wallet.type === 'PREPAID' ? 'Trả trước' : 'Trả sau'} ({formatCurrency(wallet.balance)})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {addPlanData.walletId && (
              <div className="space-y-2">
                <Label>Chọn Gói cước</Label>
                <Select
                  value={addPlanData.pricingPlanId}
                  onValueChange={(value) => setAddPlanData({ ...addPlanData, pricingPlanId: value })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Chọn gói cước" />
                  </SelectTrigger>
                  <SelectContent>
                    {(() => {
                      const wallet = wallets.find((w) => w.id === addPlanData.walletId);
                      const filteredPlans = wallet ? getFilteredPlansForWallet(wallet.type) : [];
                      if (filteredPlans.length === 0) {
                        return (
                          <SelectItem value="none" disabled>
                            Không có gói phù hợp cho loại ví này
                          </SelectItem>
                        );
                      }
                      return filteredPlans.map((plan) => (
                        <SelectItem key={plan.id} value={plan.id}>
                          {plan.name} - {formatCurrency(plan.price)}
                          {plan.bonusType !== 'NONE' && (
                            <span className="text-emerald-600 ml-1">
                              (+{plan.bonusType === 'PERCENTAGE' ? `${plan.bonusValue}%` : formatCurrency(plan.bonusValue || 0)})
                            </span>
                          )}
                        </SelectItem>
                      ));
                    })()}
                  </SelectContent>
                </Select>
                <p className="text-[11px] text-slate-400">
                  {wallets.find((w) => w.id === addPlanData.walletId)?.type === 'PREPAID'
                    ? 'Ví trả trước chỉ hiển thị gói BALANCE_TOPUP (nạp số dư)'
                    : 'Ví trả sau chỉ hiển thị gói CREDIT_INCREASE (tăng hạn mức)'}
                </p>
              </div>
            )}

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setShowAddPlanDialog(false)}>
                Hủy
              </Button>
              <Button type="submit" disabled={!addPlanData.walletId || !addPlanData.pricingPlanId}>
                Thêm Gói
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Switch Type Dialog */}
      <Dialog
        open={showSwitchDialog}
        onOpenChange={(open) => {
          setShowSwitchDialog(open);
          if (!open) {
            setSwitchData(null);
          }
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Chuyển loại Ví</DialogTitle>
            <DialogDescription>
              {switchData && (
                <span>
                  Chuyển ví từ <strong>{switchData.wallet.type === 'PREPAID' ? 'Trả trước' : 'Trả sau'}</strong> sang{' '}
                  <strong>{switchData.newType === 'PREPAID' ? 'Trả trước' : 'Trả sau'}</strong> cho tenant{' '}
                  <strong>{switchData.wallet.tenantName}</strong>
                </span>
              )}
            </DialogDescription>
          </DialogHeader>

          {switchData && (
            <div className="space-y-3">
              {switchData.wallet.type === 'POSTPAID' && switchData.newType === 'PREPAID' && (
                <Alert variant="warning">
                  <AlertCircle className="h-4 w-4" />
                  <AlertDescription>
                    Khi chuyển từ trả sau sang trả trước, hệ thống sẽ:
                    <ul className="list-disc list-inside mt-2 space-y-1 text-xs">
                      <li>Lập hóa đơn cho số tiền hiện tại</li>
                      <li>Tạo giao dịch điều chỉnh credit_limit về 0</li>
                      <li>Số dư (balance) sẽ được giữ nguyên</li>
                    </ul>
                  </AlertDescription>
                </Alert>
              )}

              {switchData.wallet.type === 'PREPAID' && switchData.newType === 'POSTPAID' && (
                <Alert variant="info">
                  <AlertCircle className="h-4 w-4" />
                  <AlertDescription>
                    Khi chuyển từ trả trước sang trả sau, số dư (balance) sẽ được giữ nguyên.
                  </AlertDescription>
                </Alert>
              )}

              <div className="p-4 bg-slate-50 rounded-lg space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-slate-500">Số dư hiện tại:</span>
                  <span className="font-semibold">{formatCurrency(switchData.wallet.balance)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-slate-500">Hạn mức hiện tại:</span>
                  <span className="font-semibold">{formatCurrency(switchData.wallet.creditLimit)}</span>
                </div>
              </div>
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setShowSwitchDialog(false)}>
              Hủy
            </Button>
            <Button onClick={handleSwitchType}>Xác nhận chuyển</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
