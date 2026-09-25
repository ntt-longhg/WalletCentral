import { useCallback, useMemo, useState } from 'react';

type CursorValue = string | null;

interface FetchPageParams {
  cursor: CursorValue;
  size: number;
}

interface UseCursorPaginationOptions {
  initialPageSize?: number;
  onFetchPage: (params: FetchPageParams) => Promise<void>;
}

interface UseCursorPaginationResult {
  currentPage: number;
  pageSize: number;
  currentCursor: CursorValue;
  hasPrevious: boolean;
  pageLinks: Array<{ page: number; isActive: boolean }>;
  initialize: () => Promise<void>;
  changePageSize: (nextSize: number) => Promise<void>;
  goNext: (nextCursor: CursorValue) => Promise<void>;
  goPrevious: () => Promise<void>;
  goToPage: (targetPage: number) => Promise<void>;
  refresh: () => Promise<void>;
}

export const useCursorPagination = ({
  initialPageSize = 10,
  onFetchPage,
}: UseCursorPaginationOptions): UseCursorPaginationResult => {
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [currentCursor, setCurrentCursor] = useState<CursorValue>(null);
  const [cursorHistory, setCursorHistory] = useState<CursorValue[]>([null]);

  const hasPrevious = currentPage > 1;

  const pageLinks = useMemo(
    () =>
      cursorHistory.map((_, index) => ({
        page: index + 1,
        isActive: index + 1 === currentPage,
      })),
    [cursorHistory, currentPage]
  );

  const initialize = useCallback(async () => {
    setPageSize(initialPageSize);
    setCurrentPage(1);
    setCurrentCursor(null);
    setCursorHistory([null]);
    await onFetchPage({ cursor: null, size: initialPageSize });
  }, [initialPageSize, onFetchPage]);

  const changePageSize = useCallback(
    async (nextSize: number) => {
      setPageSize(nextSize);
      setCurrentPage(1);
      setCurrentCursor(null);
      setCursorHistory([null]);
      await onFetchPage({ cursor: null, size: nextSize });
    },
    [onFetchPage]
  );

  const goNext = useCallback(
    async (nextCursor: CursorValue) => {
      if (!nextCursor) return;

      const nextPage = currentPage + 1;
      setCurrentPage(nextPage);
      setCurrentCursor(nextCursor);
      setCursorHistory((prev) => [...prev, nextCursor]);

      await onFetchPage({ cursor: nextCursor, size: pageSize });
    },
    [currentPage, onFetchPage, pageSize]
  );

  const goPrevious = useCallback(async () => {
    if (currentPage <= 1) return;

    const previousPage = currentPage - 1;
    const previousCursor = cursorHistory[previousPage - 1] ?? null;

    setCurrentPage(previousPage);
    setCurrentCursor(previousCursor);
    setCursorHistory((prev) => prev.slice(0, previousPage));

    await onFetchPage({ cursor: previousCursor, size: pageSize });
  }, [currentPage, cursorHistory, onFetchPage, pageSize]);

  const goToPage = useCallback(
    async (targetPage: number) => {
      if (targetPage < 1 || targetPage > cursorHistory.length || targetPage === currentPage) {
        return;
      }

      const targetCursor = cursorHistory[targetPage - 1] ?? null;
      setCurrentPage(targetPage);
      setCurrentCursor(targetCursor);
      setCursorHistory((prev) => prev.slice(0, targetPage));

      await onFetchPage({ cursor: targetCursor, size: pageSize });
    },
    [currentPage, cursorHistory, onFetchPage, pageSize]
  );

  const refresh = useCallback(async () => {
    await onFetchPage({ cursor: currentCursor, size: pageSize });
  }, [currentCursor, onFetchPage, pageSize]);

  return {
    currentPage,
    pageSize,
    currentCursor,
    hasPrevious,
    pageLinks,
    initialize,
    changePageSize,
    goNext,
    goPrevious,
    goToPage,
    refresh,
  };
};
