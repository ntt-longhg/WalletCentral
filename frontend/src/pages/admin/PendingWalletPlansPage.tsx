import React, { useCallback, useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/components/ui/toast';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow, TableSkeleton } from '@/components/ui/table';
import { TablePagination } from '@/components/ui/pagination';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Clock, CheckCircle2, XCircle, RefreshCw, Loader2 } from 'lucide-react';
import { formatCurrency, formatDate, getStatusConfig } from '@/lib/utils';
import { useCursorPagination } from '@/hooks/useCursorPagination';
import { useBillingStore } from '@/stores';

export const PendingWalletPlansPage: React.FC = () => {
  const { addToast } = useToast();
  const {
    pendingPlans,
    pendingPlansNextCursor,
    pendingPlansHasNext,
    pendingPlansCount,
    plansLoading: loading,
    fetchPendingWalletPlans,
    approveWalletPlan,
    rejectWalletPlan,
  } = useBillingStore();

  const handleFetchPage = useCallback(
    ({ cursor, size }: { cursor: string | null; size: number }) => fetchPendingWalletPlans({ cursor, size }),
    [fetchPendingWalletPlans]
  );

  const {
    pageSize,
    hasPrevious,
    initialize,
    changePageSize,
    goNext,
    goPrevious,
    refresh,
  } = useCursorPagination({
    initialPageSize: 5,
    onFetchPage: handleFetchPage,
  });

  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectSubmitting, setRejectSubmitting] = useState(false);

  useEffect(() => {
    initialize();
  }, [initialize]);

  const handleApprove = async (id: string) => {
    try {
      await approveWalletPlan(id, { approvedBy: 'admin' });
      await refresh();
      addToast({ variant: 'success', message: 'Đã phê duyệt gói cước thành công!' });
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Không thể duyệt gói cước.' });
    }
  };

  const openRejectDialog = (id: string) => {
    setSelectedId(id);
    setRejectReason('');
    setRejectDialogOpen(true);
  };

  const handleConfirmReject = async () => {
    if (!selectedId) return;
    if (!rejectReason.trim()) {
      addToast({ variant: 'destructive', message: 'Vui lòng nhập lý do từ chối.' });
      return;
    }
    setRejectSubmitting(true);
    try {
      await rejectWalletPlan(selectedId, { approvedBy: 'admin', rejectReason: rejectReason.trim() });
      await refresh();
      addToast({ variant: 'success', message: 'Đã từ chối đăng ký gói cước.' });
      setRejectDialogOpen(false);
      setSelectedId(null);
      setRejectReason('');
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Không thể từ chối gói cước.' });
    } finally {
      setRejectSubmitting(false);
    }
  };

    const columns = [
    {
      header: 'Tenant',
      accessor: 'tenantName',
      widthClass: 'w-[140px]',
    },
    {
      header: 'Gói cước',
      accessor: 'planName',
      widthClass: 'w-[140px]',
    },
    {
      header: 'Giá',
      accessor: 'price',
      widthClass: 'w-[100px]',
    },
    {
      header: 'Số tiền nạp',
      accessor: 'topUpAmount',
      widthClass: 'w-[120px]',
    },
    // {
    //   header: 'Dư trước &rarr; sau',
    //   accessor: 'balanceChange',
    //   widthClass: 'w-[140px]',
    // },
    // {
    //   header: 'Hạn mức trước &rarr; sau',
    //   accessor: 'creditLimitChange',
    //   widthClass: 'w-[160px]',
    // },
    {
      header: 'Trạng thái',
      accessor: 'status',
      widthClass: 'w-[100px]',
    },
    {
      header: 'Thời gian',
      accessor: 'createdAt',
      widthClass: 'w-[140px]',
    },
    {
      header: 'Thao tác',
      accessor: 'actions',
      widthClass: 'w-[140px]',
    },

  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Clock className="h-5 w-5 text-blue-600" />
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Duyệt Yêu cầu Đăng ký Gói cước</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Phê duyệt hoặc từ chối các yêu cầu nạp tiền / mua gói trả trước từ các Tenant trong hệ thống.
          </p>
        </div>
        <Button variant="outline" onClick={refresh} disabled={loading} className="gap-2 self-start sm:self-auto">
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </Button>
      </div>

      <Card className="border-slate-200 shadow-sm">
        <CardHeader className="pb-3">
          <CardTitle className="text-base font-semibold flex items-center gap-2">
            <Clock className="h-4 w-4 text-amber-600" /> Yêu cầu chờ duyệt
          </CardTitle>
          <CardDescription>
            Danh sách các yêu cầu đăng ký gói cước đang chờ xử lý ({pendingPlansCount} kết quả)
          </CardDescription>
        </CardHeader>
        <CardContent>
          {loading && pendingPlans.length === 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  {columns.map((col) => (
                    <TableCell key={col.accessor} className={col.widthClass}>
                      {col.header}
                    </TableCell>
                  ))}
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableSkeleton columns={columns.length} rows={pageSize} />
              </TableBody>
            </Table>
          ) : pendingPlans.length === 0 ? (
            <div className="text-center py-10 text-slate-400 text-sm">
              Không có yêu cầu đăng ký gói cước nào đang chờ duyệt.
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  {columns.map((col) => (
                    <TableHead key={col.accessor} className={col.widthClass}>
                      {col.header}
                    </TableHead>
                  ))}
                </TableRow>
              </TableHeader>
              <TableBody>
                {pendingPlans.map((wp) => (
                  <TableRow key={wp.id}>
                    <TableCell className="font-medium text-slate-900">{wp.tenantName}</TableCell>
                    <TableCell>
                      <div className="font-semibold text-slate-800">{wp.pricingPlanName}</div>
                    </TableCell>
                    <TableCell className="font-semibold">{formatCurrency(wp.price)}</TableCell>
                    <TableCell className="text-emerald-600 font-semibold">
                      +{formatCurrency(wp.creditedAmount)}
                    </TableCell>
                    {/* <TableCell className="text-sm">
                      {formatCurrency(wp.balanceBefore)} &rarr;{' '}
                      <span className="font-semibold text-slate-800">{formatCurrency(wp.balanceAfter)}</span>
                    </TableCell>
                    <TableCell className="text-sm">
                      {formatCurrency(wp.creditLimitBefore)} &rarr;{' '}
                      <span className="font-semibold text-slate-800">{formatCurrency(wp.creditLimitAfter)}</span>
                    </TableCell> */}
                    <TableCell>
                      <Badge variant="warning">{wp.status}</Badge>
                    </TableCell>
                    <TableCell className="text-xs text-slate-500">{formatDate(wp.createdAt)}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Button
                          size="sm"
                          className="bg-emerald-600 hover:bg-emerald-700 h-7 text-xs gap-1"
                          onClick={() => handleApprove(wp.id)}
                        >
                          <CheckCircle2 className="h-3.5 w-3.5" /> Duyệt
                        </Button>
                        <Button
                          size="sm"
                          variant="destructive"
                          className="h-7 text-xs gap-1"
                          onClick={() => openRejectDialog(wp.id)}
                        >
                          <XCircle className="h-3.5 w-3.5" /> Từ chối
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}

          {pendingPlansCount > 0 && (
            <TablePagination
              pageSize={pageSize}
              onPageSizeChange={changePageSize}
              onPrevious={goPrevious}
              onNext={() => goNext(pendingPlansNextCursor)}
              hasPrevious={hasPrevious}
              hasNext={pendingPlansHasNext}
              summaryText={`${pendingPlansCount} yêu cầu đang chờ duyệt`}
              pageSizeOptions={[5, 10, 20, 50]}
            />
          )}
        </CardContent>
      </Card>

      {/* Reject dialog (reason required) */}
      <Dialog open={rejectDialogOpen} onOpenChange={setRejectDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Từ chối yêu cầu đăng ký gói cước</DialogTitle>
            <DialogDescription>
              Vui lòng nhập lý do từ chối. Một giao dịch FAILED sẽ được ghi vào sổ cái để tenant nắm được.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2 py-2">
            <Label htmlFor="plan-reject-reason">Lý do từ chối</Label>
            <Input
              id="plan-reject-reason"
              placeholder="VD: Gói cước không còn áp dụng, tenant chưa đủ điều kiện..."
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRejectDialogOpen(false)} disabled={rejectSubmitting}>
              Hủy
            </Button>
            <Button variant="destructive" onClick={handleConfirmReject} disabled={rejectSubmitting} className="gap-1">
              {rejectSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <XCircle className="h-4 w-4" />}
              {rejectSubmitting ? 'Đang xử lý...' : 'Xác nhận từ chối'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
