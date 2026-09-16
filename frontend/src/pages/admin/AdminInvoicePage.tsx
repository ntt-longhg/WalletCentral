import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { MonthPicker } from '@/components/ui/month-picker';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { useToast } from '@/components/ui/toast';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  FileText,
  Plus,
  RefreshCw,
  Search,
  CreditCard,
  Send,
  Loader2,
  Eye,
  Clock,
  CheckCircle,
  XCircle,
} from 'lucide-react';
import { useBillingStore, useTransactionStore } from '@/stores';
import { formatCurrency, formatDate, getStatusConfig } from '@/lib/utils';

export const AdminInvoicePage: React.FC = () => {
  const { addToast } = useToast();
  const { tenants, wallets, fetchTenants, fetchWallets } = useBillingStore();
  const { invoices, invoicesLoading: loading, fetchInvoices, payInvoice, generateAllInvoices } = useTransactionStore();
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState('all');
  const [showGenerateDialog, setShowGenerateDialog] = useState(false);
  const [showDetailDialog, setShowDetailDialog] = useState<{ id: string; billingPeriod: string; status: string; totalAmount: number; dueDate?: string; tenantId: string; createdAt: string } | null>(null);
  const [generating, setGenerating] = useState(false);

  const [generateForm, setGenerateForm] = useState({
    tenantId: '',
    billingPeriod: '',
    updatedBy: 'admin',
  });

  const fetchAllData = async () => {
    await Promise.allSettled([fetchInvoices(), fetchTenants(), fetchWallets()]);
  };

  useEffect(() => {
    fetchAllData();
  }, []);

  const filteredInvoices = invoices.filter((inv) => {
    const matchSearch = !searchTerm ||
      inv.tenantId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      inv.billingPeriod.includes(searchTerm) ||
      inv.status.toLowerCase().includes(searchTerm.toLowerCase());
    const matchTab = activeTab === 'all' || inv.status === activeTab;
    return matchSearch && matchTab;
  });

  const handleGenerateAll = async () => {
    setGenerating(true);
    try {
      const result = await generateAllInvoices(generateForm.billingPeriod || undefined, generateForm.updatedBy);
      addToast({
        variant: 'success',
        message: `Đã tạo ${result.generatedCount || 0} hóa đơn cho kỳ ${result.billingPeriod}`,
      });
      setShowGenerateDialog(false);
      fetchInvoices();
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Không thể tạo hóa đơn.' });
    } finally {
      setGenerating(false);
    }
  };

  const handleMarkAsPaid = async (invoice: { id: string; billingPeriod: string }) => {
    try {
      await payInvoice(invoice.id, { updatedBy: 'admin' });
      addToast({ variant: 'success', message: `Hóa đơn ${invoice.billingPeriod} đã được đánh dấu thanh toán.` });
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Không thể cập nhật.' });
    }
  };

  const getStatusBadge = (status: string) => {
    const config = getStatusConfig(status as any);
    return <Badge variant={config.variant}>{config.label}</Badge>;
  };

  const getTenantName = (tenantId: string) => {
    const tenant = tenants.find(t => t.id === tenantId);
    return tenant?.name || tenantId.slice(0, 8) + '...';
  };

  const summaryStats = {
    total: invoices.length,
    issued: invoices.filter(i => i.status === 'ISSUED').length,
    paid: invoices.filter(i => i.status === 'PAID').length,
    overdue: invoices.filter(i => i.status === 'OVERDUE').length,
    totalAmount: invoices.reduce((sum, i) => sum + i.totalAmount, 0),
    unpaidAmount: invoices.filter(i => i.status !== 'PAID').reduce((sum, i) => sum + i.totalAmount, 0),
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <FileText className="h-5 w-5 text-blue-600" />
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Quản lý Hóa đơn</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Theo dõi, tạo và quản lý hóa đơn thanh toán cho các Tenant.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={fetchInvoices} disabled={loading} className="gap-1.5">
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </Button>
          <Button onClick={() => setShowGenerateDialog(true)} className="gap-2 bg-blue-600 hover:bg-blue-700">
            <Plus className="h-4 w-4" /> Tạo Hóa đơn Hàng loạt
          </Button>
        </div>
      </div>

      {/* Summary Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-4">
        <Card className="border-slate-200 shadow-sm">
          <CardContent className="pt-4 pb-3">
            <div className="text-sm text-slate-500">Tổng cộng</div>
            <div className="text-2xl font-bold text-slate-900">{summaryStats.total}</div>
          </CardContent>
        </Card>
        <Card className="border-slate-200 shadow-sm">
          <CardContent className="pt-4 pb-3">
            <div className="text-sm text-slate-500 flex items-center gap-1"><Clock className="h-3.5 w-3.5" /> Chờ thanh toán</div>
            <div className="text-2xl font-bold text-amber-600">{summaryStats.issued}</div>
          </CardContent>
        </Card>
        <Card className="border-slate-200 shadow-sm">
          <CardContent className="pt-4 pb-3">
            <div className="text-sm text-slate-500 flex items-center gap-1"><CheckCircle className="h-3.5 w-3.5" /> Đã thanh toán</div>
            <div className="text-2xl font-bold text-emerald-600">{summaryStats.paid}</div>
          </CardContent>
        </Card>
        <Card className="border-slate-200 shadow-sm">
          <CardContent className="pt-4 pb-3">
            <div className="text-sm text-slate-500 flex items-center gap-1"><XCircle className="h-3.5 w-3.5" /> Quá hạn</div>
            <div className="text-2xl font-bold text-red-600">{summaryStats.overdue}</div>
          </CardContent>
        </Card>
        <Card className="border-slate-200 shadow-sm">
          <CardContent className="pt-4 pb-3">
            <div className="text-sm text-slate-500">Tổng tiền chưa trả</div>
            <div className="text-2xl font-bold text-blue-600">{formatCurrency(summaryStats.unpaidAmount)}</div>
          </CardContent>
        </Card>
      </div>

      {/* Search + Tabs */}
      <div className="flex flex-col sm:flex-row sm:items-center gap-4">
        <div className="relative max-w-sm flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <Input
            placeholder="Tìm kiếm hóa đơn..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9"
          />
        </div>
        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <TabsList>
            <TabsTrigger value="all">Tất cả</TabsTrigger>
            <TabsTrigger value="ISSUED">Chờ TT</TabsTrigger>
            <TabsTrigger value="PAID">Đã TT</TabsTrigger>
            <TabsTrigger value="OVERDUE">Quá hạn</TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      {/* Invoices Table */}
      <Card className="border-slate-200 shadow-sm">
        <CardHeader className="pb-3">
          <CardTitle className="text-base font-semibold flex items-center gap-2">
            <FileText className="h-4 w-4 text-blue-600" /> Danh sách Hóa đơn
          </CardTitle>
          <CardDescription>{filteredInvoices.length} kết quả</CardDescription>
        </CardHeader>
        <CardContent>
          {filteredInvoices.length === 0 ? (
            <div className="text-center py-10 text-slate-400 text-sm">
              {searchTerm || activeTab !== 'all' ? 'Không tìm thấy hóa đơn phù hợp.' : 'Chưa có hóa đơn nào.'}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Tenant</TableHead>
                  <TableHead>Kỳ hóa đơn</TableHead>
                  <TableHead>Tổng tiền</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead>Hạn thanh toán</TableHead>
                  <TableHead>Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredInvoices.map((invoice) => (
                  <TableRow key={invoice.id}>
                    <TableCell>
                      <div className="font-medium text-slate-900">{getTenantName(invoice.tenantId)}</div>
                    </TableCell>
                    <TableCell className="font-mono text-sm font-semibold">{invoice.billingPeriod}</TableCell>
                    <TableCell className="font-semibold">{formatCurrency(invoice.totalAmount)}</TableCell>
                    <TableCell>{getStatusBadge(invoice.status)}</TableCell>
                    <TableCell className="text-xs text-slate-500">{formatDate(invoice.dueDate)}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1.5">
                        <Button
                          size="sm"
                          variant="outline"
                          className="text-xs h-7"
                          onClick={() => setShowDetailDialog(invoice)}
                        >
                          <Eye className="h-3.5 w-3.5" />
                        </Button>
                        {invoice.status === 'ISSUED' && (
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-xs h-7 text-emerald-600 hover:text-emerald-700"
                            onClick={() => handleMarkAsPaid(invoice)}
                          >
                            <CreditCard className="h-3.5 w-3.5 mr-1" /> Đã TT
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

      {/* Generate Dialog */}
      <Dialog open={showGenerateDialog} onOpenChange={setShowGenerateDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Tạo Hóa đơn Hàng loạt</DialogTitle>
            <DialogDescription>Tạo hóa đơn cho các tenant sử dụng trả sau (POSTPAID) trong kỳ</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Kỳ hóa đơn</Label>
              <MonthPicker
                value={generateForm.billingPeriod}
                onChange={(val) => setGenerateForm({ ...generateForm, billingPeriod: val })}
                placeholder="Để trống = kỳ hiện tại"
              />
            </div>
            <div className="space-y-2">
              <Label>Người tạo</Label>
              <Input
                value={generateForm.updatedBy}
                onChange={(e) => setGenerateForm({ ...generateForm, updatedBy: e.target.value })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowGenerateDialog(false)}>Hủy</Button>
            <Button onClick={handleGenerateAll} disabled={generating} className="gap-2">
              {generating ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
              {generating ? 'Đang tạo...' : 'Tạo Hóa đơn'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Detail Dialog */}
      <Dialog open={!!showDetailDialog} onOpenChange={() => setShowDetailDialog(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Chi tiết Hóa đơn</DialogTitle>
          </DialogHeader>
          {showDetailDialog && (
            <div className="space-y-3">
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <span className="text-slate-500">Tenant:</span>
                  <p className="font-medium">{getTenantName(showDetailDialog.tenantId)}</p>
                </div>
                <div>
                  <span className="text-slate-500">Kỳ:</span>
                  <p className="font-mono font-semibold">{showDetailDialog.billingPeriod}</p>
                </div>
                <div>
                  <span className="text-slate-500">Tổng tiền:</span>
                  <p className="font-bold text-blue-600">{formatCurrency(showDetailDialog.totalAmount)}</p>
                </div>
                <div>
                  <span className="text-slate-500">Trạng thái:</span>
                  <p>{getStatusBadge(showDetailDialog.status)}</p>
                </div>
                <div>
                  <span className="text-slate-500">Hạn thanh toán:</span>
                  <p>{formatDate(showDetailDialog.dueDate)}</p>
                </div>
                <div>
                  <span className="text-slate-500">Cập nhật bởi:</span>
                  <p>{showDetailDialog.updatedBy || 'N/A'}</p>
                </div>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowDetailDialog(null)}>Đóng</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
