import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
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
import { Building2, Plus, RefreshCw, Trash2, ToggleLeft, ToggleRight, Search } from 'lucide-react';
import { TenantResponse } from '@/types/api';
import { formatDate, getStatusConfig } from '@/lib/utils';
import { useBillingStore } from '@/stores';

export const TenantManagementPage: React.FC = () => {
  const { addToast } = useToast();
  const { tenants, tenantsLoading: loading, fetchTenants, createTenant, updateTenantStatus, deleteTenant } = useBillingStore();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<TenantResponse | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  const [formData, setFormData] = useState({
    name: '',
    clientId: '',
    clientSecret: '',
    allowedDomains: '',
  });

  useEffect(() => {
    fetchTenants();
  }, []);

  const filteredTenants = tenants.filter((t) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      t.name.toLowerCase().includes(term) ||
      t.clientId.toLowerCase().includes(term) ||
      t.status.toLowerCase().includes(term)
    );
  });

  const handleCreateTenant = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createTenant(formData);
      addToast({ variant: 'success', message: 'Tạo Tenant thành công!' });
      setShowCreateModal(false);
      setFormData({ name: '', clientId: '', clientSecret: '', allowedDomains: '' });
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Không thể tạo Tenant.' });
    }
  };

  const handleToggleStatus = async (tenant: TenantResponse) => {
    const nextStatus = tenant.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await updateTenantStatus(tenant.id, { status: nextStatus });
      addToast({ variant: 'success', message: `Đã chuyển trạng thái Tenant thành ${nextStatus}` });
    } catch (err: any) {
      addToast({ variant: 'destructive', message: 'Lỗi cập nhật trạng thái Tenant.' });
    }
  };

  const handleDelete = async () => {
    if (!showDeleteConfirm) return;
    try {
      await deleteTenant(showDeleteConfirm.id);
      addToast({ variant: 'success', message: 'Đã xóa Tenant thành công.' });
      setShowDeleteConfirm(null);
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Không thể xóa Tenant.' });
    }
  };

  const handleCancelCreate = () => {
    setShowCreateModal(false);
    setFormData({ name: '', clientId: '', clientSecret: '', allowedDomains: '' });
  };

  const handleCancelDelete = () => {
    setShowDeleteConfirm(null);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Building2 className="h-5 w-5 text-blue-600" />
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Quản lý Tenant</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Quản lý các Tenant (đối tác) trong hệ thống, bao gồm thông tin xác thực và trạng thái hoạt động.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => fetchTenants()} disabled={loading} className="gap-1.5">
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </Button>
          <Button onClick={() => setShowCreateModal(true)} className="gap-2 bg-blue-600 hover:bg-blue-700">
            <Plus className="h-4 w-4" /> Tạo Tenant Mới
          </Button>
        </div>
      </div>

      {/* Search */}
      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
        <Input
          placeholder="Tìm kiếm tenant..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="pl-9"
        />
      </div>

      {/* Tenants Table */}
      <Card className="border-slate-200 shadow-sm">
        <CardHeader className="pb-3">
          <CardTitle className="text-base font-semibold flex items-center gap-2">
            <Building2 className="h-4 w-4 text-blue-600" /> Danh sách Tenant
          </CardTitle>
          <CardDescription>Tất cả tenant trong hệ thống ({filteredTenants.length} kết quả)</CardDescription>
        </CardHeader>
        <CardContent>
          {filteredTenants.length === 0 ? (
            <div className="text-center py-10 text-slate-400 text-sm">
              {searchTerm ? 'Không tìm thấy tenant phù hợp.' : 'Chưa có tenant nào trong hệ thống.'}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Tên Tenant</TableHead>
                  <TableHead>Client ID</TableHead>
                  <TableHead>Domains</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead>Ngày tạo</TableHead>
                  <TableHead>Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredTenants.map((tenant) => {
                  const statusConf = getStatusConfig(tenant.status as any);
                  return (
                    <TableRow key={tenant.id}>
                      <TableCell>
                        <div className="font-medium text-slate-900">{tenant.name}</div>
                        <div className="text-xs text-slate-400 font-mono">{tenant.id.slice(0, 8)}...</div>
                      </TableCell>
                      <TableCell className="font-mono text-sm">{tenant.clientId}</TableCell>
                      <TableCell className="text-xs text-slate-600 max-w-[200px] truncate">
                        {tenant.allowedDomains || 'N/A'}
                      </TableCell>
                      <TableCell>
                        <Badge variant={statusConf.variant}>
                          {statusConf.label}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-xs text-slate-500">
                        {formatDate(tenant.createdAt)}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-1.5">
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-xs h-7"
                            onClick={() => handleToggleStatus(tenant)}
                          >
                            {tenant.status === 'ACTIVE' ? (
                              <><ToggleLeft className="h-3.5 w-3.5 mr-1" /> Tắt</>
                            ) : (
                              <><ToggleRight className="h-3.5 w-3.5 mr-1" /> Bật</>
                            )}
                          </Button>
                          <Button
                            size="sm"
                            variant="destructive"
                            className="text-xs h-7"
                            onClick={() => setShowDeleteConfirm(tenant)}
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Create Tenant Dialog */}
      <Dialog
          open={showCreateModal}
          onOpenChange={(open) => {
            setShowCreateModal(open);
            if (!open) {
              handleCancelCreate();
            }
          }}
        >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Tạo Tenant Mới</DialogTitle>
            <DialogDescription>Điền thông tin để tạo Tenant mới trong hệ thống</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleCreateTenant} className="space-y-4">
            <div className="space-y-2">
              <Label>Tên Tenant</Label>
              <Input
                required
                placeholder="e.g. Acme Corp"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>Client ID</Label>
              <Input
                required
                placeholder="e.g. acme-corp"
                value={formData.clientId}
                onChange={(e) => setFormData({ ...formData, clientId: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>Client Secret</Label>
              <Input
                required
                type="password"
                placeholder="Tối thiểu 8 ký tự"
                minLength={8}
                value={formData.clientSecret}
                onChange={(e) => setFormData({ ...formData, clientSecret: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>Allowed Domains</Label>
              <Input
                required
                placeholder="e.g. acme.com,acme.co.th"
                value={formData.allowedDomains}
                onChange={(e) => setFormData({ ...formData, allowedDomains: e.target.value })}
              />
              <p className="text-xs text-slate-400">Phân tách bằng dấu phẩy</p>
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={handleCancelCreate}>
                Hủy
              </Button>
              <Button type="submit">Tạo Tenant</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={!!showDeleteConfirm} onOpenChange={(open) => {
        if (!open) {
          handleCancelDelete();
        }
      }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Xác nhận xóa Tenant</DialogTitle>
            <DialogDescription>
              Bạn có chắc chắn muốn xóa Tenant <strong>{showDeleteConfirm?.name}</strong>?
              Hành động này không thể hoàn tác.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={handleCancelDelete}>
              Hủy
            </Button>
            <Button variant="destructive" onClick={handleDelete}>
              Xóa Tenant
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
