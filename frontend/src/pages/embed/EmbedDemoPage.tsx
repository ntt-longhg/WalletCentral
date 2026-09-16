import React, { useState, useCallback } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  Wallet,
  History,
  FileText,
  BarChart3,
  ExternalLink,
  RefreshCw,
  Loader2,
  ShieldAlert,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  KeyRound,
  Play,
} from 'lucide-react';
import axios from 'axios';

const demoPages = [
  {
    path: '/embed/wallet',
    name: 'Ví & Số dư',
    icon: Wallet,
    color: 'text-blue-600',
    bgColor: 'bg-blue-50',
    height: 600,
  },
  {
    path: '/embed/transactions',
    name: 'Lịch sử Giao dịch',
    icon: History,
    color: 'text-emerald-600',
    bgColor: 'bg-emerald-50',
    height: 600,
  },
  {
    path: '/embed/invoices',
    name: 'Hóa đơn',
    icon: FileText,
    color: 'text-indigo-600',
    bgColor: 'bg-indigo-50',
    height: 600,
  },
  {
    path: '/embed/reports',
    name: 'Báo cáo',
    icon: BarChart3,
    color: 'text-purple-600',
    bgColor: 'bg-purple-50',
    height: 600,
  },
];

type VerifyStatus = 'idle' | 'verifying' | 'success' | 'error';

export const EmbedDemoPage: React.FC = () => {
  const [selectedPage, setSelectedPage] = useState(demoPages[0]);
  const [apiKey, setApiKey] = useState('');
  const [verifiedKey, setVerifiedKey] = useState('');
  const [verifyStatus, setVerifyStatus] = useState<VerifyStatus>('idle');
  const [verifyError, setVerifyError] = useState('');
  const [iframeKey, setIframeKey] = useState(0);
  const [iframeLoading, setIframeLoading] = useState(true);

  const isVerified = verifyStatus === 'success' && verifiedKey === apiKey;

  const handleVerify = useCallback(async () => {
    const key = apiKey.trim();
    if (!key) return;

    setVerifyStatus('verifying');
    setVerifyError('');

    try {
      const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1';
      await axios.get(`${baseURL}/embed/tenant-info`, {
        headers: { 'X-API-Key': key },
        timeout: 10000,
      });
      setVerifiedKey(key);
      setVerifyStatus('success');
      setIframeLoading(true);
      setIframeKey((prev) => prev + 1);
    } catch (err: any) {
      setVerifiedKey('');
      setVerifyStatus('error');
      if (err.response?.status === 401) {
        setVerifyError('API Key không hợp lệ hoặc chưa được xác thực.');
      } else if (err.code === 'ECONNABORTED') {
        setVerifyError('Hết thời gian kết nối. Vui lòng thử lại.');
      } else {
        setVerifyError(err.response?.data?.message || 'Lỗi xác thực. Vui lòng thử lại.');
      }
    }
  }, [apiKey]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleVerify();
    }
  };

  const getIframeUrl = () => {
    const base = window.location.origin;
    const params = new URLSearchParams();
    if (verifiedKey) params.set('api-key', verifiedKey);
    const queryString = params.toString();
    return `${base}${selectedPage.path}${queryString ? '?' + queryString : ''}`;
  };

  const refreshIframe = () => {
    setIframeLoading(true);
    setIframeKey((prev) => prev + 1);
  };

  const handleApiKeyChange = (value: string) => {
    setApiKey(value);
    // Reset verification when key changes
    if (value !== verifiedKey) {
      setVerifyStatus('idle');
      setVerifiedKey('');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <div className="flex items-center gap-2">
          <Play className="h-5 w-5 text-blue-600" />
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Demo iFrame nhúng</h1>
        </div>
        <p className="text-sm text-slate-500 mt-1">
          Xác thực API Key và trải nghiệm các giao diện iFrame nhúng với dữ liệu thực từ backend.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Sidebar Controls */}
        <div className="lg:col-span-1 space-y-4">
          <Card className="border-slate-200 shadow-sm">
            <CardHeader className="pb-3">
              <CardTitle className="text-sm font-semibold">Xác thực & Cấu hình</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* API Key Input + Verify */}
              <div className="space-y-2">
                <Label className="text-xs">API Key</Label>
                <div className="flex gap-2">
                  <Input
                    value={apiKey}
                    onChange={(e) => handleApiKeyChange(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="Nhập API Key"
                    className="text-xs"
                    disabled={verifyStatus === 'verifying'}
                  />
                  <Button
                    size="sm"
                    onClick={handleVerify}
                    disabled={!apiKey.trim() || verifyStatus === 'verifying'}
                    className="shrink-0 gap-1"
                  >
                    {verifyStatus === 'verifying' ? (
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    ) : (
                      <KeyRound className="h-3.5 w-3.5" />
                    )}
                    Xác thực
                  </Button>
                </div>

                {/* Verify Status */}
                {verifyStatus === 'success' && (
                  <div className="flex items-center gap-1.5 text-emerald-600">
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    <span className="text-xs font-medium">API Key hợp lệ</span>
                  </div>
                )}
                {verifyStatus === 'error' && (
                  <div className="flex items-center gap-1.5 text-red-600">
                    <AlertCircle className="h-3.5 w-3.5" />
                    <span className="text-xs font-medium">{verifyError}</span>
                  </div>
                )}
                {verifyStatus === 'idle' && (
                  <p className="text-[11px] text-slate-400">
                    Nhập API Key rồi nhấn Xác thực để kiểm tra
                  </p>
                )}
              </div>

              <Separator />

              {/* Page Selection - only enabled after verification */}
              <div className="space-y-2">
                <Label className="text-xs">Chọn trang nhúng</Label>
                <div className="space-y-1.5">
                  {demoPages.map((page) => {
                    const Icon = page.icon;
                    return (
                      <button
                        key={page.path}
                        onClick={() => {
                          setSelectedPage(page);
                          if (isVerified) {
                            setIframeLoading(true);
                            setIframeKey((prev) => prev + 1);
                          }
                        }}
                        disabled={!isVerified}
                        className={`w-full flex items-center gap-2.5 p-2.5 rounded-lg text-left transition-colors ${selectedPage.path === page.path
                          ? 'bg-blue-50 border border-blue-200 text-blue-700'
                          : isVerified
                            ? 'hover:bg-slate-50 text-slate-600 border border-transparent'
                            : 'text-slate-400 border border-transparent cursor-not-allowed opacity-50'
                          }`}
                      >
                        <div className={`h-8 w-8 rounded-md ${page.bgColor} flex items-center justify-center`}>
                          <Icon className={`h-4 w-4 ${page.color}`} />
                        </div>
                        <span className="text-sm font-medium">{page.name}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              <Separator />

              <Button
                variant="outline"
                size="sm"
                className="w-full gap-1.5"
                onClick={refreshIframe}
                disabled={!isVerified}
              >
                <RefreshCw className="h-3.5 w-3.5" /> Làm mới iFrame
              </Button>
            </CardContent>
          </Card>

          <Card className="border-slate-200 shadow-sm">
            <CardContent className="pt-4">
              <p className="text-xs text-slate-500 font-medium mb-2">URL nhúng:</p>
              <code className="text-[11px] text-slate-600 break-all bg-slate-50 p-2 rounded block">
                {isVerified ? getIframeUrl() : '/embed/wallet?api-key=YOUR_KEY'}
              </code>
            </CardContent>
          </Card>
        </div>

        {/* iframe Preview */}
        <div className="lg:col-span-3">
          <Card className="border-slate-200 shadow-sm overflow-hidden">
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <div>
                <CardTitle className="text-sm font-semibold flex items-center gap-2">
                  {React.createElement(selectedPage.icon, { className: `h-4 w-4 ${selectedPage.color}` })}
                  {selectedPage.name}
                </CardTitle>
                <CardDescription className="text-xs">
                  Route: {selectedPage.path}
                </CardDescription>
              </div>
              {isVerified && (
                <Button
                  size="sm"
                  variant="ghost"
                  className="gap-1 text-xs"
                  onClick={() => window.open(getIframeUrl(), '_blank')}
                >
                  <ExternalLink className="h-3.5 w-3.5" /> Mở tab mới
                </Button>
              )}
            </CardHeader>
            <CardContent className="p-0">
              <div className="border-t border-slate-200 relative overflow-y-auto" style={{ maxHeight: '80vh' }}>
                {!isVerified ? (
                  <div className="flex flex-col items-center justify-center py-20 text-center bg-slate-50">
                    <div className="h-16 w-16 rounded-full bg-amber-100 flex items-center justify-center mb-4">
                      <ShieldAlert className="h-8 w-8 text-amber-500" />
                    </div>
                    <h3 className="text-lg font-semibold text-slate-900 mb-2">Cần xác thực API Key</h3>
                    <p className="text-sm text-slate-500 max-w-md px-4">
                      Nhập API Key của tenant vào ô bên trái rồi nhấn <strong>Xác thực</strong> để kiểm tra và xem dữ liệu trong iFrame.
                    </p>
                  </div>
                ) : (
                  <>
                    {iframeLoading && (
                      <div className="absolute inset-0 flex items-center justify-center bg-white/80 z-10">
                        <div className="flex items-center gap-2 text-slate-500">
                          <Loader2 className="h-5 w-5 animate-spin" />
                          <span className="text-sm">Đang tải iFrame...</span>
                        </div>
                      </div>
                    )}
                    <iframe
                      key={iframeKey}
                      src={getIframeUrl()}
                      width="100%"
                      height={selectedPage.height}
                      style={{ border: 'none' }}
                      title={selectedPage.name}
                      onLoad={() => setIframeLoading(false)}
                    />
                  </>
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};
