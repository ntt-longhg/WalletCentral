import React, { useEffect, useState, useCallback } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { useToast } from '@/components/ui/toast';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { useEmbedStore } from '@/stores';
import { embedService } from '@/services/billingServices';
import type { InvoiceItemResponse, InvoiceResponse } from '@/types/api';
import { formatCurrency, formatDate } from '@/lib/utils';
import { FileText, CreditCard, RefreshCw, Loader2, Eye } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { EmbedAccessDeniedPage } from './EmbedAccessDeniedPage';

export const EmbedInvoicesPage: React.FC = () => {
  const { token } = useAuth();
  const { addToast } = useToast();
  const {
    invoices,
    invoicesLoading: loading,
    invoicesError: error,
    fetchInvoices,
    payInvoice,
  } = useEmbedStore();

  const [detailInvoice, setDetailInvoice] = useState<InvoiceResponse | null>(null);
  const [detailItems, setDetailItems] = useState<InvoiceItemResponse[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);

  useEffect(() => {
    if (token) {
      fetchInvoices();
    }
  }, [token, fetchInvoices]);

  useEffect(() => {
    if (error) {
      addToast({ variant: 'destructive', message: error });
    }
  }, [error, addToast]);

  const handlePay = async (invoiceId: string) => {
    try {
      await payInvoice(invoiceId);
      addToast({ variant: 'success', message: 'Thanh toán hóa đơn thành công!' });
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Thanh toán thất bại.' });
    }
  };

  const openDetail = useCallback(async (inv: InvoiceResponse) => {
    setDetailInvoice(inv);
    setDetailItems([]);
    setDetailLoading(true);
    try {
      const res = await embedService.getInvoiceItems(inv.id);
      if (res.data?.success && res.data?.data?.items) {
        setDetailItems(res.data?.data?.items);
      }
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Không thể tải chi tiết hóa đơn.' });
    } finally {
      setDetailLoading(false);
    }
  }, [addToast]);

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
      <div className="flex items-center justify-between bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-indigo-50 flex items-center justify-center text-indigo-600">
            <FileText className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900">Danh sách Hóa đơn Thanh toán</h2>
            <p className="text-xs text-slate-500">Kỳ đối soát và hóa đơn dịch vụ hàng tháng</p>
          </div>
        </div>
        <Button variant="outline" size="sm" onClick={fetchInvoices} disabled={loading} className="gap-1 text-xs">
          <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} /> Làm mới
        </Button>
      </div>

      <Card className="border-slate-200 shadow-sm">
        <CardContent className="p-0">
          {loading && invoices.length === 0 ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-5 w-5 text-blue-600 animate-spin mr-2" />
              <span className="text-sm text-slate-500">Đang tải dữ liệu...</span>
            </div>
          ) : invoices.length === 0 ? (
            <div className="text-center py-12 text-slate-400 text-sm">
              {error ? 'Lỗi tải dữ liệu.' : 'Chưa có bản ghi hóa đơn nào được phát hành.'}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Kỳ Hóa Đơn</TableHead>
                  <TableHead>Tổng Tiền</TableHead>
                  <TableHead>Hạn Thanh Toán</TableHead>
                  <TableHead>Trạng Thái</TableHead>
                  <TableHead>Ngày Tạo</TableHead>
                  <TableHead>Hành Động</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {invoices.map((inv) => (
                  <TableRow key={inv.id}>
                    <TableCell className="font-mono font-bold text-slate-800">{inv.billingPeriod}</TableCell>
                    <TableCell className="font-semibold text-slate-900">{formatCurrency(inv.totalAmount)}</TableCell>
                    <TableCell className="text-xs text-slate-600">{formatDate(inv.dueDate)}</TableCell>
                    <TableCell>
                      <Badge
                        variant={
                          inv.status === 'PAID'
                            ? 'success'
                            : inv.status === 'OVERDUE'
                              ? 'destructive'
                              : 'warning'
                        }
                      >
                        {inv.status}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-xs text-slate-500">{formatDate(inv.createdAt)}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1.5">
                        <Button
                          size="sm"
                          variant="outline"
                          className="h-7 text-xs"
                          onClick={() => openDetail(inv)}
                        >
                          <Eye className="h-3.5 w-3.5" />
                        </Button>
                        {inv.status !== 'PAID' && (
                          <Button
                            size="sm"
                            className="bg-indigo-600 hover:bg-indigo-700 h-7 text-xs gap-1"
                            onClick={() => handlePay(inv.id)}
                          >
                            <CreditCard className="h-3.5 w-3.5" /> Thanh toán
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Invoice detail dialog */}
      <Dialog open={!!detailInvoice} onOpenChange={() => setDetailInvoice(null)}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>Chi tiết Hóa đơn {detailInvoice?.billingPeriod}</DialogTitle>
          </DialogHeader>
          {detailInvoice && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-sm">
                <div>
                  <span className="text-slate-500">Tổng tiền:</span>
                  <p className="font-bold text-indigo-700">{formatCurrency(detailInvoice.totalAmount)}</p>
                </div>
                <div>
                  <span className="text-slate-500">Trạng thái:</span>
                  <p className="font-medium">{detailInvoice.status}</p>
                </div>
                <div>
                  <span className="text-slate-500">Hạn thanh toán:</span>
                  <p>{formatDate(detailInvoice.dueDate)}</p>
                </div>
              </div>
              {detailInvoice.note && (
                <div className="rounded-md bg-slate-50 border border-slate-200 px-3 py-2 text-xs text-slate-600">
                  <span className="font-medium">Ghi chú:</span> {detailInvoice.note}
                </div>
              )}
              {detailLoading ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-5 w-5 text-indigo-600 animate-spin mr-2" />
                  <span className="text-sm text-slate-500">Đang tải...</span>
                </div>
              ) : detailItems.length === 0 ? (
                <div className="text-center py-8 text-slate-400 text-sm">
                  Không có phát sinh giao dịch trong kỳ này.
                </div>
              ) : (
                <div className="max-h-80 overflow-y-auto rounded-md border border-slate-100">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Dịch vụ</TableHead>
                        <TableHead>Sản lượng</TableHead>
                        <TableHead>Cước phí</TableHead>
                        <TableHead>Hoàn tiền</TableHead>
                        <TableHead>Thời gian</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {detailItems.map((item) => (
                        <TableRow key={item.usageLogId}>
                          <TableCell>
                            <div className="font-medium text-slate-800 text-xs">{item.serviceName || '—'}</div>
                            <div className="text-[11px] font-mono text-slate-400">{item.serviceCode || ''}</div>
                          </TableCell>
                          <TableCell className="text-xs">{item.totalUsage}</TableCell>
                          <TableCell className="text-xs font-semibold">{formatCurrency(item.totalCharged)}</TableCell>
                          <TableCell>
                            {item.refunded ? (
                              <Badge variant="destructive" className="text-[11px]">Đã hoàn</Badge>
                            ) : (
                              <span className="text-xs text-slate-400">—</span>
                            )}
                          </TableCell>
                          <TableCell className="text-xs text-slate-500">{formatDate(item.createdAt)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setDetailInvoice(null)}>Đóng</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
