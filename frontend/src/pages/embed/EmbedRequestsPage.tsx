import React, { useEffect, useState } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useToast } from '@/components/ui/toast';
import { useEmbedStore } from '@/stores';
import { formatCurrency, formatDate } from '@/lib/utils';
import { ClipboardList, Undo2, Wallet, RefreshCw, Loader2 } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { EmbedAccessDeniedPage } from './EmbedAccessDeniedPage';

export const EmbedRequestsPage: React.FC = () => {
  const { token } = useAuth();
  const { addToast } = useToast();
  const {
    pendingRefunds,
    pendingWalletPlans,
    refundsLoading: loading,
    pendingWalletPlansLoading: plansLoading,
    refundsError: error,
    pendingWalletPlansError: plansError,
    fetchPendingRefundRequests,
    fetchPendingWalletPlans,
  } = useEmbedStore();

  const [tab, setTab] = useState<'refunds' | 'wallet-plans'>('refunds');

  useEffect(() => {
    if (token) {
      fetchPendingRefundRequests();
      fetchPendingWalletPlans();
    }
  }, [token, fetchPendingRefundRequests, fetchPendingWalletPlans]);

  useEffect(() => {
    if (error) addToast({ variant: 'destructive', message: error });
  }, [error, addToast]);

  useEffect(() => {
    if (plansError) addToast({ variant: 'destructive', message: plansError });
  }, [plansError, addToast]);

  const handleRefresh = () => {
    fetchPendingRefundRequests();
    fetchPendingWalletPlans();
  };

  if (error && pendingRefunds.length === 0 && plansError && pendingWalletPlans.length === 0) {
    return <EmbedAccessDeniedPage />;
  }

  return (
    <div className="p-4 space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-amber-50 flex items-center justify-center text-amber-600">
            <ClipboardList className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900">Yêu cầu đang chờ duyệt</h2>
            <p className="text-xs text-slate-500">Theo dõi yêu cầu hoàn tiền và nạp gói đã gửi</p>
          </div>
        </div>
        <Button variant="outline" size="sm" onClick={handleRefresh} disabled={loading || plansLoading} className="gap-1 text-xs">
          <RefreshCw className={`h-3.5 w-3.5 ${(loading || plansLoading) ? 'animate-spin' : ''}`} />
        </Button>
      </div>

      <Tabs value={tab} onValueChange={(v) => setTab(v as 'refunds' | 'wallet-plans')}>
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="refunds" className="gap-2">
            <Undo2 className="h-4 w-4" />
            Hoàn tiền ({pendingRefunds.length})
          </TabsTrigger>
          <TabsTrigger value="wallet-plans" className="gap-2">
            <Wallet className="h-4 w-4" />
            Nạp gói ({pendingWalletPlans.length})
          </TabsTrigger>
        </TabsList>

        <TabsContent value="refunds">
          <Card className="border-slate-200 shadow-sm">
            <CardContent className="p-0">
              {loading && pendingRefunds.length === 0 ? (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="h-5 w-5 text-blue-600 animate-spin mr-2" />
                  <span className="text-sm text-slate-500">Đang tải dữ liệu...</span>
                </div>
              ) : pendingRefunds.length === 0 ? (
                <div className="text-center py-12 text-slate-400 text-sm">
                  Không có yêu cầu hoàn tiền nào đang chờ duyệt.
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Số tiền</TableHead>
                      <TableHead>Lý do</TableHead>
                      <TableHead>Người yêu cầu</TableHead>
                      <TableHead>Trạng thái</TableHead>
                      <TableHead>Thời gian</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {pendingRefunds.map((r) => (
                      <TableRow key={r.id}>
                        <TableCell className="font-semibold text-amber-700">
                          {formatCurrency(r.amount)}
                        </TableCell>
                        <TableCell className="text-xs text-slate-600 max-w-55">
                          {r.reason || <span className="text-slate-400">—</span>}
                        </TableCell>
                        <TableCell className="text-xs text-slate-500">{r.requestedBy || '—'}</TableCell>
                        <TableCell>
                          <Badge variant="warning" className="text-[11px]">{r.status}</Badge>
                        </TableCell>
                        <TableCell className="text-xs text-slate-500">{formatDate(r.createdAt)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="wallet-plans">
          <Card className="border-slate-200 shadow-sm">
            <CardContent className="p-0">
              {plansLoading && pendingWalletPlans.length === 0 ? (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="h-5 w-5 text-blue-600 animate-spin mr-2" />
                  <span className="text-sm text-slate-500">Đang tải dữ liệu...</span>
                </div>
              ) : pendingWalletPlans.length === 0 ? (
                <div className="text-center py-12 text-slate-400 text-sm">
                  Không có yêu cầu nạp gói nào đang chờ duyệt.
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Gói cước</TableHead>
                      <TableHead>Giá</TableHead>
                      <TableHead>Số tiền nạp</TableHead>
                      <TableHead>Trạng thái</TableHead>
                      <TableHead>Thời gian</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {pendingWalletPlans.map((wp) => (
                      <TableRow key={wp.id}>
                        <TableCell className="font-semibold text-slate-800">{wp.pricingPlanName}</TableCell>
                        <TableCell>{formatCurrency(wp.price)}</TableCell>
                        <TableCell className="text-emerald-600 font-semibold">
                          +{formatCurrency(wp.creditedAmount)}
                        </TableCell>
                        <TableCell>
                          <Badge variant="warning" className="text-[11px]">{wp.status}</Badge>
                        </TableCell>
                        <TableCell className="text-xs text-slate-500">{formatDate(wp.createdAt)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};
