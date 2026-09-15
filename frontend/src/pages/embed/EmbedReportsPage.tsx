import React, { useEffect, useState, useCallback } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { useToast } from '@/components/ui/toast';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { useEmbedStore } from '@/stores';
import { formatCurrency, formatDate } from '@/lib/utils';
import { BarChart3, Activity, ShieldCheck, RefreshCw, Layers, Loader2 } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';

export const EmbedReportsPage: React.FC = () => {
  const { token } = useAuth();
  const { addToast } = useToast();
  const {
    usageLogs,
    creditAdjustments: adjustments,
    usageLogsLoading,
    creditAdjustmentsLoading,
    fetchUsageLogs,
    fetchCreditAdjustments,
  } = useEmbedStore();

  const loading = usageLogsLoading || creditAdjustmentsLoading;
  const [error, setError] = useState<string | null>(null);

  const fetchReportsData = useCallback(async () => {
    setError(null);
    try {
      await Promise.all([fetchUsageLogs(), fetchCreditAdjustments()]);
    } catch (err: any) {
      setError('Lỗi kết nối đến server.');
    }
  }, [fetchUsageLogs, fetchCreditAdjustments]);

  useEffect(() => {
    if (token) {
      fetchReportsData();
    }
  }, [token, fetchReportsData]);

  useEffect(() => {
    if (error) {
      addToast({ variant: 'destructive', message: error });
    }
  }, [error, addToast]);

  const totalUsageUnits = usageLogs.reduce((acc, curr) => acc + (curr.totalUsage || 0), 0);
  const totalChargedAmount = usageLogs.reduce((acc, curr) => acc + (curr.totalCharged || 0), 0);

  if (!token) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-6 w-6 text-blue-600 animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-emerald-50 flex items-center justify-center text-emerald-600">
            <BarChart3 className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900">Báo cáo Sản lượng & Tiêu dùng</h2>
            <p className="text-xs text-slate-500">Thống kê log tiêu dùng dịch vụ và điều chỉnh hạn mức</p>
          </div>
        </div>
        <Button variant="outline" size="sm" onClick={fetchReportsData} disabled={loading} className="gap-1 text-xs">
          <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} /> Làm mới
        </Button>
      </div>

      {loading && usageLogs.length === 0 && adjustments.length === 0 ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="h-5 w-5 text-blue-600 animate-spin mr-2" />
          <span className="text-sm text-slate-500">Đang tải dữ liệu...</span>
        </div>
      ) : (
        <>
          {/* Overview Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Card className="border-slate-200 shadow-sm">
              <CardHeader className="pb-2">
                <CardTitle className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Tổng Sản lượng Tiêu dùng (Units)
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-slate-900">{totalUsageUnits.toLocaleString('vi-VN')}</div>
                <p className="text-xs text-slate-500 mt-1 flex items-center gap-1">
                  <Activity className="h-3.5 w-3.5 text-emerald-500" /> Tính trên toàn bộ API dịch vụ
                </p>
              </CardContent>
            </Card>

            <Card className="border-slate-200 shadow-sm">
              <CardHeader className="pb-2">
                <CardTitle className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Tổng Phí Phát sinh (Total Charged)
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-blue-600">{formatCurrency(totalChargedAmount)}</div>
                <p className="text-xs text-slate-500 mt-1 flex items-center gap-1">
                  <ShieldCheck className="h-3.5 w-3.5 text-blue-500" /> Trừ vào số dư ví / hạn mức tín dụng
                </p>
              </CardContent>
            </Card>
          </div>

          {/* Usage Logs Table */}
          <Card className="border-slate-200 shadow-sm">
            <CardHeader className="pb-3">
              <CardTitle className="text-base font-semibold flex items-center gap-2">
                <Layers className="h-4 w-4 text-emerald-600" /> Nhật ký Log Tiêu Dùng
              </CardTitle>
            </CardHeader>
            <CardContent>
              {usageLogs.length === 0 ? (
                <div className="text-center py-8 text-slate-400 text-sm">
                  {error ? 'Lỗi tải dữ liệu.' : 'Chưa có dữ liệu tiêu dùng.'}
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Mã Dịch Vụ</TableHead>
                      <TableHead>Sản Lượng</TableHead>
                      <TableHead>Số Tiền Tính Phí</TableHead>
                      <TableHead>Ví Snapshot</TableHead>
                      <TableHead>Thời Gian</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {usageLogs.map((log) => (
                      <TableRow key={log.id}>
                        <TableCell>
                          <Badge variant="outline" className="font-mono">
                            {log.serviceCode || 'N/A'}
                          </Badge>
                        </TableCell>
                        <TableCell className="font-semibold text-slate-900">
                          {log.totalUsage.toLocaleString('vi-VN')} units
                        </TableCell>
                        <TableCell className="font-semibold text-blue-600">
                          {formatCurrency(log.totalCharged)}
                        </TableCell>
                        <TableCell className="text-xs text-slate-500">
                          {log.walletTypeSnapshot} (Avail: {formatCurrency(log.availableBalanceSnapshot)})
                        </TableCell>
                        <TableCell className="text-xs text-slate-500">{formatDate(log.createdAt)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>

          {/* Credit Adjustments Table */}
          {adjustments.length > 0 && (
            <Card className="border-slate-200 shadow-sm">
              <CardHeader className="pb-3">
                <CardTitle className="text-base font-semibold flex items-center gap-2">
                  <ShieldCheck className="h-4 w-4 text-blue-600" /> Lịch sử Điều chỉnh Credit
                </CardTitle>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Loại điều chỉnh</TableHead>
                      <TableHead>Số tiền</TableHead>
                      <TableHead>Trước &rarr; Sau</TableHead>
                      <TableHead>Lý do</TableHead>
                      <TableHead>Thời Gian</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {adjustments.map((adj) => (
                      <TableRow key={adj.id}>
                        <TableCell>
                          <Badge variant={adj.type === 'INCREASE' ? 'success' : 'destructive'}>
                            {adj.type === 'INCREASE' ? 'Tăng' : 'Giảm'}
                          </Badge>
                        </TableCell>
                        <TableCell className="font-semibold">{formatCurrency(adj.adjustmentAmount)}</TableCell>
                        <TableCell className="text-xs text-slate-500">
                          {formatCurrency(adj.creditLimitBefore)} &rarr;{' '}
                          <span className="font-medium text-slate-800">{formatCurrency(adj.creditLimitAfter)}</span>
                        </TableCell>
                        <TableCell className="text-xs text-slate-600">{adj.reason || '-'}</TableCell>
                        <TableCell className="text-xs text-slate-500">{formatDate(adj.createdAt)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          )}
        </>
      )}
    </div>
  );
};
