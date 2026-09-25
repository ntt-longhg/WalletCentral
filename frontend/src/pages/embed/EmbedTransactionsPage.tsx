import React, { useEffect, useState, useCallback } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { useToast } from '@/components/ui/toast';
import { useEmbedStore } from '@/stores';
import { formatCurrency, formatDate } from '@/lib/utils';
import { ArrowDownLeft, ArrowUpRight, History, RefreshCw, Loader2, Undo2 } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { EmbedAccessDeniedPage } from './EmbedAccessDeniedPage';
import type { TransactionResponse } from '@/types/api';

export const EmbedTransactionsPage: React.FC = () => {
  const { token } = useAuth();
  const { addToast } = useToast();
  const {
    transactions,
    transactionsLoading: loading,
    transactionsError: error,
    fetchTransactions,
    createRefundRequest,
    refunds,
    fetchRefundRequests,
  } = useEmbedStore();

  const [filterType, setFilterType] = useState<string>('ALL');
  const [refundDialogOpen, setRefundDialogOpen] = useState(false);
  const [selectedTxn, setSelectedTxn] = useState<TransactionResponse | null>(null);
  const [refundReason, setRefundReason] = useState('');
  const [refundSubmitting, setRefundSubmitting] = useState(false);

  useEffect(() => {
    if (token) {
      fetchTransactions();
      fetchRefundRequests();
    }
  }, [token, fetchTransactions, fetchRefundRequests]);

  useEffect(() => {
    if (error) {
      addToast({ variant: 'destructive', message: error });
    }
  }, [error]);

  const filteredTxns = transactions.filter((t) => {
    if (filterType === 'ALL') return true;
    return t.type === filterType;
  });

  // Transaction IDs that already have a PENDING or APPROVED refund (backend also guards)
  const refundedTxnIds = React.useMemo(() => {
    const ids = new Set<string>();
    for (const r of refunds) {
      if ((r.status === 'PENDING' || r.status === 'APPROVED') && r.transactionId) {
        ids.add(r.transactionId);
      }
    }
    return ids;
  }, [refunds]);

  const canRefund = useCallback((t: TransactionResponse) => {
    return t.type === 'CHARGE' && t.status === 'SUCCESS' && !refundedTxnIds.has(t.id);
  }, [refundedTxnIds]);

  const openRefundDialog = (txn: TransactionResponse) => {
    setSelectedTxn(txn);
    setRefundReason('');
    setRefundDialogOpen(true);
  };

  const handleConfirmRefund = async () => {
    if (!selectedTxn) return;
    setRefundSubmitting(true);
    try {
      await createRefundRequest({
        transactionId: selectedTxn.id,
        reason: refundReason.trim() || undefined,
      });
      addToast({ variant: 'success', message: 'Đã gửi yêu cầu hoàn tiền, chờ phê duyệt.' });
      setRefundDialogOpen(false);
      setSelectedTxn(null);
      setRefundReason('');
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Gửi yêu cầu hoàn tiền thất bại.' });
    } finally {
      setRefundSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[400px] flex items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-blue-600" />
      </div>
    );
  }

  if (error) {
    return (
      <EmbedAccessDeniedPage />
    )
  }

  return (
    <div className="p-4 space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-blue-50 flex items-center justify-center text-blue-600">
            <History className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900">Lịch sử Giao dịch & Biến động Dư</h2>
            <p className="text-xs text-slate-500">Nhật ký trừ phí dịch vụ và nạp tiền tài khoản</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-lg">
            {['ALL', 'DEPOSIT', 'CHARGE', 'REFUND'].map((type) => (
              <button
                key={type}
                onClick={() => setFilterType(type)}
                className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors ${filterType === type
                  ? 'bg-white text-blue-600 shadow-2xs'
                  : 'text-slate-600 hover:text-slate-900'
                  }`}
              >
                {type}
              </button>
            ))}
          </div>
          <Button variant="outline" size="sm" onClick={fetchTransactions} disabled={loading} className="gap-1 text-xs">
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
          </Button>
        </div>
      </div>

      <Card className="border-slate-200 shadow-sm">
        <CardContent className="p-0">
          {loading && transactions.length === 0 ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-5 w-5 text-blue-600 animate-spin mr-2" />
              <span className="text-sm text-slate-500">Đang tải dữ liệu...</span>
            </div>
          ) : filteredTxns.length === 0 ? (
            <div className="text-center py-12 text-slate-400 text-sm">
              {error ? 'Lỗi tải dữ liệu.' : 'Không tìm thấy biến động giao dịch nào.'}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Loại Giao Dịch</TableHead>
                  <TableHead>Số Tiền</TableHead>
                  <TableHead>Dư Trước &rarr; Sau</TableHead>
                  <TableHead>Khả dụng trước &rarr; sau</TableHead>
                  <TableHead>Trạng Thái</TableHead>
                  <TableHead>Nguồn / Reference</TableHead>
                  <TableHead>Thời Gian</TableHead>
                  <TableHead className="text-right">Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredTxns.map((txn) => (
                  <TableRow key={txn.id}>
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
                    <TableCell className="text-xs text-slate-500">
                      {formatCurrency(txn.balanceBefore)} &rarr;{' '}
                      <span className="font-medium text-slate-800">{formatCurrency(txn.balanceAfter)}</span>
                    </TableCell>
                    <TableCell className="text-sm">
                      {formatCurrency(txn.availableBalanceBefore)} &rarr;{' '}
                      <span className="font-semibold text-slate-800">{formatCurrency(txn.availableBalanceAfter)}</span>
                    </TableCell>
                    <TableCell>
                      <Badge variant={txn.status === 'SUCCESS' ? 'success' : 'destructive'} className="text-[11px]">
                        {txn.status}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-xs font-mono text-slate-600">
                      {txn.referenceFrom}: {txn.referenceId}
                    </TableCell>
                    <TableCell className="text-xs text-slate-500">{formatDate(txn.createdAt)}</TableCell>
                    <TableCell className="text-right">
                      {canRefund(txn) && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openRefundDialog(txn)}
                          className="gap-1 text-xs text-amber-700 border-amber-200 hover:bg-amber-50"
                        >
                          <Undo2 className="h-3.5 w-3.5" />
                          Hoàn tiền
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Refund request dialog */}
      <Dialog open={refundDialogOpen} onOpenChange={setRefundDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Yêu cầu hoàn tiền</DialogTitle>
            <DialogDescription>
              {selectedTxn && (
                <>
                  Hoàn tiền giao dịch <span className="font-mono">{selectedTxn.referenceFrom}: {selectedTxn.referenceId}</span>
                  {' '}với số tiền <strong>{formatCurrency(selectedTxn.amount)}</strong>.
                  Yêu cầu sẽ được gửi đi và chờ quản trị viên phê duyệt.
                </>
              )}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2 py-2">
            <Label htmlFor="refund-reason">Lý do hoàn tiền</Label>
            <Input
              id="refund-reason"
              placeholder="VD: Khách hàng khiếu nại trừ phí sai..."
              value={refundReason}
              onChange={(e) => setRefundReason(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRefundDialogOpen(false)} disabled={refundSubmitting}>
              Hủy
            </Button>
            <Button onClick={handleConfirmRefund} disabled={refundSubmitting} className="gap-1">
              {refundSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Undo2 className="h-4 w-4" />}
              {refundSubmitting ? 'Đang gửi...' : 'Gửi yêu cầu'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
