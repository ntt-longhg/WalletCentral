import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/components/ui/toast';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Undo2, CheckCircle2, XCircle, RefreshCw, Loader2 } from 'lucide-react';
import { formatCurrency, formatDate } from '@/lib/utils';
import { useBillingStore } from '@/stores';
import { useAuth } from '@/context/AuthContext';

export const PendingRefundsPage: React.FC = () => {
  const { addToast } = useToast();
  const { adminEmail } = useAuth();
  const { pendingRefunds, refundsLoading: loading, fetchPendingRefunds, approveRefund, rejectRefund } = useBillingStore();

  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectSubmitting, setRejectSubmitting] = useState(false);

  useEffect(() => {
    fetchPendingRefunds();
  }, []);

  const handleApprove = async (id: string) => {
    try {
      await approveRefund(id, { reviewedBy: adminEmail || 'admin' });
      addToast({ variant: 'success', message: 'Đã phê duyệt hoàn tiền thành công!' });
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Không thể duyệt hoàn tiền.' });
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
      await rejectRefund(selectedId, { reviewedBy: adminEmail || 'admin', rejectReason: rejectReason.trim() });
      addToast({ variant: 'success', message: 'Đã từ chối yêu cầu hoàn tiền.' });
      setRejectDialogOpen(false);
      setSelectedId(null);
      setRejectReason('');
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Không thể từ chối hoàn tiền.' });
    } finally {
      setRejectSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Undo2 className="h-5 w-5 text-blue-600" />
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Duyệt Yêu cầu Hoàn tiền</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Phê duyệt hoặc từ chối các yêu cầu hoàn tiền giao dịch CHARGE từ các Tenant trong hệ thống.
          </p>
        </div>
        <Button variant="outline" onClick={() => fetchPendingRefunds()} disabled={loading} className="gap-2 self-start sm:self-auto">
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </Button>
      </div>

      <Card className="border-slate-200 shadow-sm">
        <CardHeader className="pb-3">
          <CardTitle className="text-base font-semibold flex items-center gap-2">
            <Undo2 className="h-4 w-4 text-amber-600" /> Yêu cầu chờ duyệt
          </CardTitle>
          <CardDescription>Danh sách các yêu cầu hoàn tiền đang chờ xử lý</CardDescription>
        </CardHeader>
        <CardContent>
          {pendingRefunds.length === 0 ? (
            <div className="text-center py-10 text-slate-400 text-sm">
              Không có yêu cầu hoàn tiền nào đang chờ duyệt.
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Tenant</TableHead>
                  <TableHead>Số tiền hoàn</TableHead>
                  <TableHead>Giao dịch gốc</TableHead>
                  <TableHead>Lý do</TableHead>
                  <TableHead>Người yêu cầu</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead>Thời gian</TableHead>
                  <TableHead>Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {pendingRefunds.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell className="font-medium text-slate-900">{r.tenantName}</TableCell>
                    <TableCell className="text-amber-700 font-semibold">
                      {formatCurrency(r.amount)}
                    </TableCell>
                    <TableCell className="text-xs font-mono text-slate-600">
                      <div>{r.transactionType}: {formatCurrency(r.transactionAmount)}</div>
                      <div className="text-slate-400 truncate max-w-40" title={r.transactionId}>{r.transactionId}</div>
                    </TableCell>
                    <TableCell className="text-xs text-slate-600 max-w-55">
                      {r.reason || <span className="text-slate-400">—</span>}
                    </TableCell>
                    <TableCell className="text-xs text-slate-500">{r.requestedBy || '—'}</TableCell>
                    <TableCell>
                      <Badge variant="warning">{r.status}</Badge>
                    </TableCell>
                    <TableCell className="text-xs text-slate-500">{formatDate(r.createdAt)}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Button
                          size="sm"
                          className="bg-emerald-600 hover:bg-emerald-700 h-7 text-xs gap-1"
                          onClick={() => handleApprove(r.id)}
                        >
                          <CheckCircle2 className="h-3.5 w-3.5" /> Duyệt
                        </Button>
                        <Button
                          size="sm"
                          variant="destructive"
                          className="h-7 text-xs gap-1"
                          onClick={() => openRejectDialog(r.id)}
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
        </CardContent>
      </Card>

      {/* Reject dialog (reason required) */}
      <Dialog open={rejectDialogOpen} onOpenChange={setRejectDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Từ chối yêu cầu hoàn tiền</DialogTitle>
            <DialogDescription>
              Vui lòng nhập lý do từ chối. Tenant sẽ nhận được thông báo này.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2 py-2">
            <Label htmlFor="reject-reason">Lý do từ chối</Label>
            <Input
              id="reject-reason"
              placeholder="VD: Giao dịch đã sử dụng dịch vụ, không đủ điều kiện hoàn..."
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
