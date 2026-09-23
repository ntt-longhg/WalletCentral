import React from 'react';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

interface TablePaginationProps {
  pageSize: number;
  onPageSizeChange: (pageSize: number) => void;
  onPrevious: () => void;
  onNext: () => void;
  hasPrevious: boolean;
  hasNext: boolean;
  summaryText?: string;
  pageSizeOptions?: number[];
  className?: string;
}

export const TablePagination: React.FC<TablePaginationProps> = ({
  pageSize,
  onPageSizeChange,
  onPrevious,
  onNext,
  hasPrevious,
  hasNext,
  summaryText,
  pageSizeOptions = [1, 2, 5, 10, 20, 50],
  className,
}) => {
  return (
    <div className={`flex flex-col gap-3 pt-4 sm:flex-row sm:items-center sm:justify-between ${className || ''}`}>
      <div className="flex items-center gap-2 text-sm text-slate-600">
        <span>Hiển thị</span>
        <Select
          value={String(pageSize)}
          onValueChange={(value) => {
            const nextSize = Number(value);
            onPageSizeChange(nextSize);
          }}
        >
          <SelectTrigger className="h-8 w-[88px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {pageSizeOptions.map((option) => (
              <SelectItem key={option} value={String(option)}>
                {option} / trang
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <span>bản ghi</span>
      </div>

      <div className="flex items-center justify-between gap-3 sm:justify-end">
        {summaryText && <span className="text-sm text-slate-500">{summaryText}</span>}
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={!hasPrevious}
            onClick={onPrevious}
          >
            Trang trước
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={!hasNext}
            onClick={onNext}
          >
            Trang sau
          </Button>
        </div>
      </div>
    </div>
  );
};
