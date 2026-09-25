import type { KeyboardEvent } from 'react';
import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatCurrency(amount: number, currency: string = 'VND'): string {
  if (currency === 'VND') {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount);
  }
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
  }).format(amount);
}

export function formatDate(dateString?: string): string {
  if (!dateString) return 'N/A';
  try {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }).format(date);
  } catch {
    return dateString;
  }
}

export function formatRelativeTime(dateString?: string): string {
  if (!dateString) return 'N/A';
  try {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);

    if (diffSec < 60) return 'Vừa xong';
    if (diffMin < 60) return `${diffMin} phút trước`;
    if (diffHour < 24) return `${diffHour} giờ trước`;
    if (diffDay < 30) return `${diffDay} ngày trước`;
    return formatDate(dateString);
  } catch {
    return dateString;
  }
}

export type EntityStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'SUSPENDED' | 'LOCKED' | 'CLOSED'
  | 'SUCCESS' | 'FAILED' | 'APPROVED' | 'REJECTED' | 'ISSUED' | 'PAID' | 'CANCELLED' | 'OVERDUE';

export const statusConfig: Record<EntityStatus, { label: string; variant: 'success' | 'destructive' | 'warning' | 'info' | 'secondary' | 'default' }> = {
  ACTIVE: { label: 'Hoạt động', variant: 'success' },
  INACTIVE: { label: 'Ngừng hoạt động', variant: 'secondary' },
  PENDING: { label: 'Chờ xử lý', variant: 'warning' },
  SUSPENDED: { label: 'Đình chỉ', variant: 'destructive' },
  LOCKED: { label: 'Đã khóa', variant: 'info' },
  CLOSED: { label: 'Đã đóng', variant: 'destructive' },
  SUCCESS: { label: 'Thành công', variant: 'success' },
  FAILED: { label: 'Thất bại', variant: 'destructive' },
  APPROVED: { label: 'Đã duyệt', variant: 'success' },
  REJECTED: { label: 'Từ chối', variant: 'destructive' },
  ISSUED: { label: 'Đã phát hành', variant: 'warning' },
  PAID: { label: 'Đã thanh toán', variant: 'success' },
  CANCELLED: { label: 'Đã hủy', variant: 'destructive' },
  OVERDUE: { label: 'Quá hạn', variant: 'destructive' },
};

export function getStatusConfig(status: EntityStatus) {
  return statusConfig[status] || { label: status, variant: 'secondary' as const };
}

export type NumericField = 'price' | 'bonusValue' | 'creditLimitValue' | 'initialFee' | 'subsequentFee' | 'basicFee' | 'extendedSize' | 'extendedFee';

export const sanitizePercentageInput = (value: string) => {
  const cleaned = value.replace(/\D/g, '');
  if (!cleaned) return '';

  const normalized = cleaned.slice(0, 3);
  const parsed = Number(normalized || 0);

  return parsed > 100 ? '100' : normalized;
};

export const parseNumberInput = (
  value: string,
  options: { min?: number; max?: number; maxDigits?: number; allowDecimal?: boolean } = {},
) => {
  const { min = 0, max, maxDigits = 11, allowDecimal = false } = options;

  if (value.trim() === '') return 0;

  if (allowDecimal) {
    const sanitizedValue = value
      .replace(',', '.')
      .replace(/[^\d.]/g, '')
      .replace(/\.(?=.*\.)/g, '');

    const normalizedValue = sanitizedValue === '' ? '0' : sanitizedValue;
    const parsedValue = Number(normalizedValue);

    if (!Number.isFinite(parsedValue)) return min;
    if (parsedValue < min) return min;
    if (typeof max === 'number' && parsedValue > max) return max;
    return parsedValue;
  }

  const normalizedValue = value.replace(/\D/g, '').slice(0, maxDigits);
  const parsedValue = Number(normalizedValue);

  if (!Number.isFinite(parsedValue)) return min;
  if (parsedValue < min) return min;
  if (typeof max === 'number' && parsedValue > max) return max;

  return parsedValue;
};

export const formatNumberWithDots = (value: number) => new Intl.NumberFormat('vi-VN').format(value);

export const getNumberInputValue = (
  value: number,
  rawValue?: string,
  options: {
    activeField?: NumericField | null;
    currentField?: NumericField;
    useDecimal?: boolean;
  } = {},
) => {
  const { activeField, currentField, useDecimal = false } = options;

  if (rawValue !== undefined) return rawValue;
  if (activeField && currentField && activeField === currentField && value === 0) return '';
  if (useDecimal) return value.toString();
  return formatNumberWithDots(value);
};

export const handleNumberKeyDown = (
  e: KeyboardEvent<HTMLInputElement>,
  allowDecimal = false,
) => {
  if (e.ctrlKey || e.metaKey) return;

  const allowedControlKeys = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab', 'Home', 'End', 'Enter'];
  if (allowedControlKeys.includes(e.key)) return;

  if (allowDecimal && ['.', ',', 'Decimal', 'NumpadDecimal', 'Period'].includes(e.key)) {
    if (e.currentTarget.value.includes('.') || e.currentTarget.value.includes(',')) {
      e.preventDefault();
    }
    return;
  }

  if (!/^[0-9]$/.test(e.key)) {
    e.preventDefault();
  }
};

export type WalletType = 'PREPAID' | 'POSTPAID';

export const walletTypeConfig: Record<WalletType, { label: string; color: string; bgClass: string; textClass: string }> = {
  PREPAID: { label: 'Trả trước', color: 'blue', bgClass: 'bg-blue-100', textClass: 'text-blue-700' },
  POSTPAID: { label: 'Trả sau', color: 'purple', bgClass: 'bg-purple-100', textClass: 'text-purple-700' },
};

export type TransactionType = 'TOPUP' | 'CHARGE' | 'REFUND' | 'ADJUSTMENT';

export const transactionTypeConfig: Record<TransactionType, { label: string; icon: string; colorClass: string }> = {
  TOPUP: { label: 'Nạp tiền', icon: 'arrow-down-left', colorClass: 'text-emerald-600' },
  CHARGE: { label: 'Trừ phí', icon: 'arrow-up-right', colorClass: 'text-red-600' },
  REFUND: { label: 'Hoàn tiền', icon: 'rotate-ccw', colorClass: 'text-blue-600' },
  ADJUSTMENT: { label: 'Điều chỉnh', icon: 'sliders', colorClass: 'text-amber-600' },
};

export type PlanType = 'BALANCE_TOPUP' | 'CREDIT_INCREASE';

export const planTypeConfig: Record<PlanType, { label: string; description: string }> = {
  BALANCE_TOPUP: { label: 'Nạp số dư', description: 'Tăng số dư ví (balance)' },
  CREDIT_INCREASE: { label: 'Tăng hạn mức', description: 'Tăng hạn mức tín dụng (credit limit)' },
};
