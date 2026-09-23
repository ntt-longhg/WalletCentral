import React, { useCallback, useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
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
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow, TableSkeleton } from '@/components/ui/table';
import { TablePagination } from '@/components/ui/pagination';
import { FileSpreadsheet, Plus, RefreshCw } from 'lucide-react';
import { useCursorPagination } from '@/hooks/useCursorPagination';
import { useServiceStore } from '@/stores';
import {
  formatCurrency,
  getNumberInputValue,
  getStatusConfig,
  handleNumberKeyDown,
  parseNumberInput,
  sanitizePercentageInput,
  type NumericField,
} from '@/lib/utils';

export const PricingPlansPage: React.FC = () => {
  const MAX_NUMBER_DIGITS = 11;
  const MAX_FORMATTED_NUMBER_LENGTH = 14;

  const { addToast } = useToast();
  const {
    pricingPlans: plans,
    pricingPlansCount,
    pricingPlansHasNext,
    pricingPlansNextCursor,
    plansLoading: loading,
    fetchPricingPlans,
    createPricingPlan,
    updatePricingPlanStatus,
  } = useServiceStore();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [percentageBonusInput, setPercentageBonusInput] = useState('');
  const [activeNumberField, setActiveNumberField] = useState<NumericField | null>(null);

  const handleFetchPage = useCallback(
    ({ cursor, size }: { cursor: string | null; size: number }) =>
      fetchPricingPlans({ cursor, size }),
    [fetchPricingPlans]
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
    initialPageSize: 2,
    onFetchPage: handleFetchPage,
  });

  const [formData, setFormData] = useState({
    code: '',
    name: '',
    description: '',
    price: 100000,
    type: 'BALANCE_TOPUP',
    bonusType: 'NONE' as 'NONE' | 'PERCENTAGE' | 'FIXED',
    bonusValue: 0,
    creditLimitAction: 'SET' as 'SET' | 'INCREASE',
    creditLimitValue: 0,
  });

  useEffect(() => {
    initialize();
  }, [initialize]);

  const handleNextPage = async () => {
    await goNext(pricingPlansNextCursor);
  };

  const handleCreatePlan = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createPricingPlan(formData);
      addToast({ variant: 'success', message: 'Tạo bảng giá Tenant thành công!' });
      setShowCreateModal(false);
      setFormData({
        code: '',
        name: '',
        description: '',
        price: 100000,
        type: 'BALANCE_TOPUP',
        bonusType: 'NONE',
        bonusValue: 0,
        creditLimitAction: 'SET',
        creditLimitValue: 0,
      });
      setPercentageBonusInput('');
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Không thể tạo bảng giá.' });
    }
  };

  const handleToggleStatus = async (plan: { id: string; status: string }) => {
    const nextStatus = plan.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await updatePricingPlanStatus(plan.id, { status: nextStatus });
      addToast({ variant: 'success', message: `Đã chuyển trạng thái bảng giá thành ${nextStatus}` });
    } catch (err: any) {
      addToast({ variant: 'destructive', message: 'Lỗi cập nhật trạng thái bảng giá.' });
    }
  };

  const handleCancel = () => {
    setShowCreateModal(false);
    setFormData({
      code: '',
      name: '',
      description: '',
      price: 100000,
      type: 'BALANCE_TOPUP',
      bonusType: 'NONE',
      bonusValue: 0,
      creditLimitAction: 'SET',
      creditLimitValue: 0,
    });
    setPercentageBonusInput('');
  };

  const columns = [
    {
      header: 'Mã Gói',
      accessor: 'code',
      widthClass: 'w-[130px]',
    },
    {
      header: 'Tên Bảng Giá',
      accessor: 'name',
      widthClass: 'w-[220px]',
    },
    {
      header: 'Giá Gói',
      accessor: 'price',
      widthClass: 'w-[140px]',
    },
    {
      header: 'Loại Gói',
      accessor: 'type',
      widthClass: 'w-[140px]',
    },
    {
      header: 'Khuyến Mãi',
      accessor: 'bonusType',
      widthClass: 'w-[170px]',
    },
    {
      header: 'Credit Limit',
      accessor: 'creditLimitValue',
      widthClass: 'w-[170px]',
    },
    {
      header: 'Trạng Thái',
      accessor: 'status',
      widthClass: 'w-[120px]',
    },
    {
      header: 'Thao Tác',
      accessor: 'actions',
      widthClass: 'w-[120px]',
    },
  ];



  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <FileSpreadsheet className="h-5 w-5 text-blue-600" />
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Cấu hình Bảng giá cho Tenant</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Thiết lập gói cước trả trước/trả sau, tỷ lệ khuyến mãi (bonus) và cấu hình hạn mức tín dụng (credit limit).
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={refresh} disabled={loading} className="gap-1.5">
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </Button>
          <Button onClick={() => setShowCreateModal(true)} className="gap-2 bg-blue-600 hover:bg-blue-700">
            <Plus className="h-4 w-4" /> Tạo Bảng giá Mới
          </Button>
        </div>
      </div>

      {/* Pricing Plans Table Card */}
      <Card className="border-slate-200 shadow-sm">
        <CardHeader className="flex flex-row items-center justify-between pb-3">
          <div>
            <CardTitle className="text-base font-semibold flex items-center gap-2">
              <FileSpreadsheet className="h-4 w-4 text-blue-600" /> Bảng giá đã tạo
            </CardTitle>
            <CardDescription>Danh sách gói cước trả trước/trả sau dành cho Tenant</CardDescription>
          </div>
        </CardHeader>
        <CardContent>
          {plans.length === 0 ? (
            <div className="text-center py-10 text-slate-400 text-sm">Chưa có bảng giá nào trong hệ thống.</div>
          ) : (
            <Table className="table-fixed">
              <TableHeader>
                <TableRow>
                  {columns.map((column) => (
                    <TableHead key={column.accessor} className={column.widthClass}>{column.header}</TableHead>
                  ))}
                </TableRow>
              </TableHeader>
              <TableBody>
                {
                  loading ? (
                    <TableSkeleton columns={columns.length} rows={pageSize} />
                  ): (
                    plans.map((plan) => {
                      const statusConf = getStatusConfig(plan.status as any);
                      return (
                        <TableRow key={plan.id}>
                          <TableCell className="font-mono font-bold text-slate-800 truncate">{plan.code}</TableCell>
                          <TableCell>
                            <div className="font-medium text-slate-900 truncate">{plan.name}</div>
                            {plan.description && <div className="text-xs text-slate-400 truncate">{plan.description}</div>}
                          </TableCell>
                          <TableCell className="font-semibold text-slate-900">{formatCurrency(plan.price)}</TableCell>
                          <TableCell>
                            <Badge variant="outline" className="font-mono text-xs">
                              {plan.type === 'BALANCE_TOPUP' ? 'Nạp số dư' : 'Tăng hạn mức'}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-sm">
                            {plan.bonusType === 'NONE' ? (
                              <span className="text-slate-400">-</span>
                            ) : plan.bonusType === 'PERCENTAGE' ? (
                              <span className="text-emerald-600 font-medium">+{plan.bonusValue}%</span>
                            ) : (
                              <span className="text-emerald-600 font-medium">+{formatCurrency(plan.bonusValue || 0)}</span>
                            )}
                          </TableCell>
                          <TableCell className="text-xs">
                            {plan.creditLimitAction} ({formatCurrency(plan.creditLimitValue)})
                          </TableCell>
                          <TableCell>
                            <Badge variant={statusConf.variant}>
                              {statusConf.label}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => handleToggleStatus(plan)}
                              className="text-xs h-7"
                            >
                              {plan.status === 'ACTIVE' ? 'Tắt' : 'Kích hoạt'}
                            </Button>
                          </TableCell>
                        </TableRow>
                      );
                    })
                  )
                }
              </TableBody>
            </Table>
          )}

          {plans.length > 0 && (
            <TablePagination
              pageSize={pageSize}
              onPageSizeChange={changePageSize}
              onPrevious={goPrevious}
              onNext={handleNextPage}
              hasPrevious={hasPrevious}
              hasNext={pricingPlansHasNext}
              summaryText={`${pricingPlansCount} bản ghi trong trang hiện tại`}
            />
          )}
        </CardContent>
      </Card>

      {/* Create Pricing Plan Dialog */}
      <Dialog
        open={showCreateModal}
        onOpenChange={(open) => {
          setShowCreateModal(open);
          if (!open) handleCancel();
        }}
      >
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Tạo Bảng giá Tenant Mới</DialogTitle>
            <DialogDescription>Điền thông tin chi tiết bảng giá dịch vụ</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleCreatePlan} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label>Mã Bảng Giá (Code)</Label>
                <Input
                  required
                  placeholder="e.g. TOPUP_100"
                  value={formData.code}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label>Tên Bảng Giá</Label>
                <Input
                  required
                  placeholder="e.g. Topup 100K"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label>Giá gói (VNĐ)</Label>
              <Input
                type="text"
                inputMode="numeric"
                pattern="[0-9.]*"
                maxLength={MAX_FORMATTED_NUMBER_LENGTH}
                required
                value={getNumberInputValue(formData.price, undefined, {
                  activeField: activeNumberField,
                  currentField: 'price',
                })}
                onFocus={() => setActiveNumberField('price')}
                onBlur={() => setActiveNumberField(null)}
                onKeyDown={handleNumberKeyDown}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    price: parseNumberInput(e.target.value, { min: 0, maxDigits: MAX_NUMBER_DIGITS }),
                  })
                }
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label>Loại Bảng Giá</Label>
                <Select
                  value={formData.type}
                  onValueChange={(value) => setFormData({ ...formData, type: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="BALANCE_TOPUP">Nạp số dư (BALANCE_TOPUP)</SelectItem>
                    <SelectItem value="CREDIT_INCREASE">Tăng hạn mức (CREDIT_INCREASE)</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>Loại Khuyến Mãi</Label>
                <Select
                  value={formData.bonusType}
                  onValueChange={(value: 'NONE' | 'PERCENTAGE' | 'FIXED') => setFormData({ ...formData, bonusType: value, bonusValue: 0 })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="NONE">Không khuyến mãi</SelectItem>
                    <SelectItem value="PERCENTAGE">Phần trăm (%)</SelectItem>
                    <SelectItem value="FIXED">Số tiền cố định</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {formData.bonusType === 'PERCENTAGE' && (
              <div className="space-y-2">
                <Label>Giá trị Khuyến Mãi (%)</Label>
                <Input
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  maxLength={3}
                  value={getNumberInputValue(formData.bonusValue, percentageBonusInput, {
                    activeField: activeNumberField,
                    currentField: 'bonusValue',
                  })}
                  onFocus={() => setActiveNumberField('bonusValue')}
                  onBlur={() => setActiveNumberField(null)}
                  onKeyDown={(e) => handleNumberKeyDown(e, false)}
                  onChange={(e) => {
                    const nextValue = sanitizePercentageInput(e.target.value);

                    setPercentageBonusInput(nextValue);

                    setFormData({
                      ...formData,
                      bonusValue: parseNumberInput(nextValue, {
                        min: 0,
                        max: 100,
                        maxDigits: 3,
                      }),
                    });
                  }}
                />
              </div>
            )}

            {formData.bonusType === 'FIXED' && (
              <div className="space-y-2">
                <Label>Giá trị Khuyến Mãi (VNĐ)</Label>
                <Input
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  maxLength={MAX_FORMATTED_NUMBER_LENGTH}
                  value={getNumberInputValue(formData.bonusValue, undefined, {
                    activeField: activeNumberField,
                    currentField: 'bonusValue',
                  })}
                  onFocus={() => setActiveNumberField('bonusValue')}
                  onBlur={() => setActiveNumberField(null)}
                  onKeyDown={handleNumberKeyDown}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      bonusValue: parseNumberInput(e.target.value, {
                        min: 0,
                        maxDigits: MAX_NUMBER_DIGITS,
                      }),
                    })
                  }
                />
              </div>
            )}

            {
              formData.type === "CREDIT_INCREASE" && (
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-2">
                  <Label>Credit Limit Action</Label>
                  <Select
                    value={formData.creditLimitAction}
                    onValueChange={(value: 'SET' | 'INCREASE') =>
                      setFormData({
                        ...formData,
                        creditLimitAction: value,
                        creditLimitValue: 0,
                      })
                    }
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="SET">Thiết lập cố định</SelectItem>
                      <SelectItem value="INCREASE">Cộng dồn</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Giá trị Credit Limit</Label>
                  <Input
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9.]*"
                    maxLength={MAX_FORMATTED_NUMBER_LENGTH}
                    value={getNumberInputValue(formData.creditLimitValue, undefined, {
                      activeField: activeNumberField,
                      currentField: 'creditLimitValue',
                    })}
                    onFocus={() => setActiveNumberField('creditLimitValue')}
                    onBlur={() => setActiveNumberField(null)}
                    onKeyDown={handleNumberKeyDown}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        creditLimitValue: parseNumberInput(e.target.value, { min: 0, maxDigits: MAX_NUMBER_DIGITS }),
                      })
                    }
                  />
                </div>
              </div>
            )}

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => handleCancel()}>
                Hủy
              </Button>
              <Button type="submit">Tạo Bảng Giá</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
};
