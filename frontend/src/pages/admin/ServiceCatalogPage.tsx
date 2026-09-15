import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
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
import { Boxes, Plus, DollarSign, Layers } from 'lucide-react';
import { useServiceStore } from '@/stores';
import { ServiceResponse, ServicePriceResponse } from '@/types/api';
import { formatCurrency } from '@/lib/utils';

export const ServiceCatalogPage: React.FC = () => {
  const { addToast } = useToast();
  const {
    services,
    servicesLoading: loading,
    prices: pricesMap,
    tiers: tiersMap,
    fetchServices,
    createService,
    fetchPrices,
    createPrice,
    activatePrice,
    fetchTiers,
    addTier,
  } = useServiceStore();

  const [selectedService, setSelectedService] = useState<ServiceResponse | null>(null);
  const [selectedPrice, setSelectedPrice] = useState<ServicePriceResponse | null>(null);

  // Dialog states
  const [showCreateService, setShowCreateService] = useState(false);
  const [showCreatePrice, setShowCreatePrice] = useState(false);
  const [showCreateTier, setShowCreateTier] = useState(false);

  const [newService, setNewService] = useState({ code: '', name: '', description: '' });
  const [newPrice, setNewPrice] = useState({
    initialSize: 1,
    initialFee: 0,
    subsequentSize: 1,
    subsequentFee: 0,
    effectiveDate: new Date().toISOString(),
  });
  const [newTier, setNewTier] = useState({
    tier: 'TIER_1',
    basicFee: 0,
    extendedSize: 100,
    extendedFee: 0,
  });

  useEffect(() => {
    fetchServices();
  }, [fetchServices]);

  const prices = selectedService ? (pricesMap[selectedService.id] ?? []) : [];
  const tiers = selectedPrice ? (tiersMap[selectedPrice.id] ?? []) : [];

  const handleSelectService = async (service: ServiceResponse) => {
    setSelectedService(service);
    setSelectedPrice(null);
    try {
      await fetchPrices(service.id);
    } catch {
      // Handled by store
    }
  };

  const handleSelectPrice = async (price: ServicePriceResponse) => {
    setSelectedPrice(price);
    try {
      await fetchTiers(price.id);
    } catch {
      // Handled by store
    }
  };

  const handleCreateService = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createService(newService);
      addToast({ variant: 'success', message: 'Tạo dịch vụ thành công!' });
      setShowCreateService(false);
      setNewService({ code: '', name: '', description: '' });
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Tạo dịch vụ thất bại.' });
    }
  };

  const handleCreatePrice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedService) return;
    try {
      await createPrice(selectedService.id, newPrice);
      addToast({ variant: 'success', message: 'Tạo thiết lập giá thành công!' });
      setShowCreatePrice(false);
      handleSelectService(selectedService);
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Tạo giá thất bại.' });
    }
  };

  const handleActivatePrice = async (priceId: string) => {
    try {
      await activatePrice(priceId);
      if (selectedService) {
        addToast({ variant: 'success', message: 'Kích hoạt mức giá thành công!' });
        handleSelectService(selectedService);
      }
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Lỗi kích hoạt giá.' });
    }
  };

  const handleCreateTier = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPrice) return;
    try {
      await addTier(selectedPrice.id, newTier);
      addToast({ variant: 'success', message: 'Thêm bậc giá thành công!' });
      setShowCreateTier(false);
      handleSelectPrice(selectedPrice);
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Lỗi thêm bậc giá.' });
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Boxes className="h-5 w-5 text-blue-600" />
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Danh mục Dịch vụ & Thiết lập Giá</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Quản lý các dịch vụ cổng thanh toán (SMS, vKYC...), thiết lập mức giá cơ sở và phân bậc (Tiered Pricing).
          </p>
        </div>
        <Button onClick={() => setShowCreateService(true)} className="gap-2 bg-blue-600 hover:bg-blue-700">
          <Plus className="h-4 w-4" /> Thêm Dịch vụ Mới
        </Button>
      </div>

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Service List */}
        <Card className="lg:col-span-1 border-slate-200 shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="text-base font-semibold flex items-center gap-2">
              <Boxes className="h-4 w-4 text-blue-600" /> Danh sách Dịch vụ
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {services.length === 0 ? (
              <div className="text-center py-6 text-slate-400 text-sm">Chưa có dịch vụ nào trong hệ thống.</div>
            ) : (
              services.map((svc) => (
                <div
                  key={svc.id}
                  onClick={() => handleSelectService(svc)}
                  className={`p-3 rounded-lg border cursor-pointer transition-all ${selectedService?.id === svc.id
                      ? 'border-blue-600 bg-blue-50/50 ring-1 ring-blue-600'
                      : 'border-slate-200 hover:bg-slate-50'
                    }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-slate-900 text-sm">{svc.name}</span>
                    <Badge variant="outline" className="font-mono text-xs">
                      {svc.code}
                    </Badge>
                  </div>
                  {svc.description && <p className="text-xs text-slate-500 mt-1 line-clamp-2">{svc.description}</p>}
                </div>
              ))
            )}
          </CardContent>
        </Card>

        {/* Prices & Tiers View */}
        <Card className="lg:col-span-2 border-slate-200 shadow-sm">
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div>
              <CardTitle className="text-base font-semibold">
                {selectedService ? `Bảng giá: ${selectedService.name}` : 'Chi tiết Thiết lập Giá'}
              </CardTitle>
              <CardDescription>
                {selectedService ? `Mã dịch vụ: ${selectedService.code}` : 'Chọn dịch vụ bên trái để xem bảng giá'}
              </CardDescription>
            </div>
            {selectedService && (
              <Button size="sm" onClick={() => setShowCreatePrice(true)} className="gap-1.5 text-xs">
                <Plus className="h-3.5 w-3.5" /> Tạo mức giá mới
              </Button>
            )}
          </CardHeader>
          <CardContent className="space-y-6">
            {!selectedService ? (
              <div className="text-center py-12 text-slate-400 text-sm">
                Vui lòng chọn một dịch vụ từ danh sách bên trái.
              </div>
            ) : (
              <>
                {/* Prices Table */}
                <div>
                  <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                    <DollarSign className="h-3.5 w-3.5 text-emerald-600" /> Bảng thiết lập giá dịch vụ
                  </h4>
                  {prices.length === 0 ? (
                    <p className="text-sm text-slate-400 py-4 italic">Chưa có thiết lập giá cho dịch vụ này.</p>
                  ) : (
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Initial (Size / Fee)</TableHead>
                          <TableHead>Subsequent (Size / Fee)</TableHead>
                          <TableHead>Trạng thái</TableHead>
                          <TableHead>Thao tác</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {prices.map((pr) => (
                          <TableRow
                            key={pr.id}
                            className={selectedPrice?.id === pr.id ? 'bg-slate-50 font-medium' : ''}
                          >
                            <TableCell className="text-sm">
                              {pr.initialSize} units / {formatCurrency(pr.initialFee)}
                            </TableCell>
                            <TableCell className="text-sm">
                              {pr.subsequentSize} units / {formatCurrency(pr.subsequentFee)}
                            </TableCell>
                            <TableCell>
                              <Badge variant={pr.active ? 'success' : 'secondary'}>
                                {pr.active ? 'Kích hoạt' : 'Chưa active'}
                              </Badge>
                            </TableCell>
                            <TableCell className="flex items-center gap-2">
                              {!pr.active && (
                                <Button
                                  size="sm"
                                  variant="outline"
                                  onClick={() => handleActivatePrice(pr.id)}
                                  className="text-xs h-7"
                                >
                                  Kích hoạt
                                </Button>
                              )}
                              <Button
                                size="sm"
                                variant="ghost"
                                onClick={() => handleSelectPrice(pr)}
                                className="text-xs h-7 gap-1"
                              >
                                <Layers className="h-3.5 w-3.5" /> Bậc giá
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  )}
                </div>

                {/* Tiers View */}
                {selectedPrice && (
                  <div className="pt-4 border-t border-slate-200">
                    <div className="flex items-center justify-between mb-3">
                      <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
                        <Layers className="h-3.5 w-3.5 text-purple-600" /> Bậc giá (Tiered Pricing) cho Mức giá ID:{' '}
                        <span className="font-mono text-slate-700">{selectedPrice.id.slice(0, 8)}...</span>
                      </h4>
                      <Button size="sm" variant="outline" onClick={() => setShowCreateTier(true)} className="text-xs h-7 gap-1">
                        <Plus className="h-3 w-3" /> Thêm bậc giá
                      </Button>
                    </div>

                    {tiers.length === 0 ? (
                      <p className="text-sm text-slate-400 py-3 italic">Chưa có bậc giá mở rộng được cấu hình.</p>
                    ) : (
                      <Table>
                        <TableHeader>
                          <TableRow>
                            <TableHead>Tên Bậc (Tier)</TableHead>
                            <TableHead>Basic Fee</TableHead>
                            <TableHead>Extended (Size / Fee)</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {tiers.map((t) => (
                            <TableRow key={t.id}>
                              <TableCell className="font-semibold text-slate-800">{t.tier}</TableCell>
                              <TableCell>{formatCurrency(t.basicFee)}</TableCell>
                              <TableCell>
                                {t.extendedSize} units / {formatCurrency(t.extendedFee)}
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    )}
                  </div>
                )}
              </>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Create Service Dialog */}
      <Dialog open={showCreateService} onOpenChange={setShowCreateService}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Tạo Dịch vụ Mới</DialogTitle>
            <DialogDescription>Nhập thông tin dịch vụ cổng billing</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleCreateService} className="space-y-4">
            <div className="space-y-2">
              <Label>Mã Dịch vụ (Code)</Label>
              <Input
                required
                placeholder="e.g. SMS, VKYC"
                value={newService.code}
                onChange={(e) => setNewService({ ...newService, code: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>Tên Dịch vụ</Label>
              <Input
                required
                placeholder="e.g. SMS OTP Service"
                value={newService.name}
                onChange={(e) => setNewService({ ...newService, name: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>Mô tả</Label>
              <Input
                placeholder="Mô tả chi tiết..."
                value={newService.description}
                onChange={(e) => setNewService({ ...newService, description: e.target.value })}
              />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setShowCreateService(false)}>
                Hủy
              </Button>
              <Button type="submit">Lưu Dịch Vụ</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Create Price Dialog */}
      <Dialog open={showCreatePrice} onOpenChange={setShowCreatePrice}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Thêm Thiết lập Giá mới</DialogTitle>
            <DialogDescription>Dịch vụ: {selectedService?.name}</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleCreatePrice} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label>Initial Size</Label>
                <Input
                  type="number"
                  required
                  min={1}
                  value={newPrice.initialSize}
                  onChange={(e) => setNewPrice({ ...newPrice, initialSize: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label>Initial Fee (VNĐ)</Label>
                <Input
                  type="number"
                  required
                  min={0}
                  value={newPrice.initialFee}
                  onChange={(e) => setNewPrice({ ...newPrice, initialFee: Number(e.target.value) })}
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label>Subsequent Size</Label>
                <Input
                  type="number"
                  required
                  min={1}
                  value={newPrice.subsequentSize}
                  onChange={(e) => setNewPrice({ ...newPrice, subsequentSize: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label>Subsequent Fee (VNĐ)</Label>
                <Input
                  type="number"
                  required
                  min={0}
                  value={newPrice.subsequentFee}
                  onChange={(e) => setNewPrice({ ...newPrice, subsequentFee: Number(e.target.value) })}
                />
              </div>
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setShowCreatePrice(false)}>
                Hủy
              </Button>
              <Button type="submit">Lưu Thiết Lập Giá</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Create Tier Dialog */}
      <Dialog open={showCreateTier} onOpenChange={setShowCreateTier}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Thêm Bậc Giá (Price Tier)</DialogTitle>
            <DialogDescription>Mức giá ID: {selectedPrice?.id.slice(0, 8)}</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleCreateTier} className="space-y-4">
            <div className="space-y-2">
              <Label>Tên Bậc Tier</Label>
              <Input
                required
                placeholder="e.g. TIER_1, TIER_2"
                value={newTier.tier}
                onChange={(e) => setNewTier({ ...newTier, tier: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>Basic Fee (VNĐ)</Label>
              <Input
                type="number"
                required
                min={0}
                value={newTier.basicFee}
                onChange={(e) => setNewTier({ ...newTier, basicFee: Number(e.target.value) })}
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label>Extended Size</Label>
                <Input
                  type="number"
                  required
                  min={1}
                  value={newTier.extendedSize}
                  onChange={(e) => setNewTier({ ...newTier, extendedSize: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label>Extended Fee (VNĐ)</Label>
                <Input
                  type="number"
                  required
                  min={0}
                  value={newTier.extendedFee}
                  onChange={(e) => setNewTier({ ...newTier, extendedFee: Number(e.target.value) })}
                />
              </div>
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setShowCreateTier(false)}>
                Hủy
              </Button>
              <Button type="submit">Lưu Bậc Giá</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
};
