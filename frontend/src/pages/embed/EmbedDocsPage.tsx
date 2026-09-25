import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Code,
  Copy,
  Check,
  ExternalLink,
  Wallet,
  History,
  FileText,
  BarChart3,
  ClipboardList,
  Info,
  AlertTriangle,
} from 'lucide-react';

const embedRoutes = [
  {
    path: '/embed/wallet',
    name: 'Ví & Số dư',
    description: 'Hiển thị thông tin ví, số dư, và các gói cước có thể nạp',
    icon: Wallet,
    color: 'text-blue-600',
    bgColor: 'bg-blue-50',
  },
  {
    path: '/embed/transactions',
    name: 'Lịch sử Giao dịch',
    description: 'Bảng lịch sử giao dịch, biến động số dư',
    icon: History,
    color: 'text-emerald-600',
    bgColor: 'bg-emerald-50',
  },
  {
    path: '/embed/invoices',
    name: 'Hóa đơn',
    description: 'Danh sách hóa đơn và trạng thái thanh toán',
    icon: FileText,
    color: 'text-indigo-600',
    bgColor: 'bg-indigo-50',
  },
  {
    path: '/embed/reports',
    name: 'Báo cáo',
    description: 'Báo cáo sản lượng tiêu dùng và điều chỉnh credit',
    icon: BarChart3,
    color: 'text-purple-600',
    bgColor: 'bg-purple-50',
  },
  {
    path: '/embed/requests',
    name: 'Yêu cầu chờ duyệt',
    description: 'Yêu cầu hoàn tiền và nạp gói đang chờ phê duyệt',
    icon: ClipboardList,
    color: 'text-amber-600',
    bgColor: 'bg-amber-50',
  },
];

export const EmbedDocsPage: React.FC = () => {
  const [baseUrl, setBaseUrl] = useState(window.location.origin);
  const [apiKey, setApiKey] = useState('');
  const [copied, setCopied] = useState<string | null>(null);

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopied(id);
    setTimeout(() => setCopied(null), 2000);
  };

  const generateEmbedUrl = (path: string) => {
    const params = new URLSearchParams();
    if (apiKey) params.set('api-key', apiKey);
    const queryString = params.toString();
    return `${baseUrl}${path}${queryString ? '?' + queryString : ''}`;
  };

  const generateIframeCode = (path: string) => {
    const url = generateEmbedUrl(path);
    return `<iframe src="${url}" width="100%" height="600" frameborder="0" allowfullscreen></iframe>`;
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <div className="flex items-center gap-2">
          <Code className="h-5 w-5 text-blue-600" />
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Hướng dẫn nhúng iFrame</h1>
        </div>
        <p className="text-sm text-slate-500 mt-1">
          Tài liệu hướng dẫn cách nhúng giao diện BillingGateway vào ứng dụng của bạn thông qua iFrame.
        </p>
      </div>

      {/* Quick Start */}
      <Card className="border-slate-200 shadow-sm">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Info className="h-5 w-5 text-blue-600" />
            Bắt đầu nhanh
          </CardTitle>
          <CardDescription>Các bước cơ bản để nhúng giao diện BillingGateway</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label>Cấu hình</Label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="baseUrl" className="text-xs">Base URL</Label>
                <Input
                  id="baseUrl"
                  value={baseUrl}
                  onChange={(e) => setBaseUrl(e.target.value)}
                  placeholder="https://your-domain.com"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="apiKey" className="text-xs">API Key (tùy chọn)</Label>
                <Input
                  id="apiKey"
                  value={apiKey}
                  onChange={(e) => setApiKey(e.target.value)}
                  placeholder="Nhập API Key của tenant"
                />
              </div>
            </div>
          </div>

          <Separator />

          <div className="space-y-3">
            <h4 className="text-sm font-semibold text-slate-700">Cách 1: Sử dụng URL trực tiếp</h4>
            <p className="text-xs text-slate-500">
              Truy cập trực tiếp URL bên dưới trong trình duyệt hoặc nhúng vào iframe.
            </p>
            <div className="flex items-center gap-2">
              <code className="flex-1 p-3 bg-slate-100 rounded-lg text-xs text-slate-700 break-all font-mono">
                {generateEmbedUrl('/embed/wallet')}
              </code>
              <Button
                size="sm"
                variant="outline"
                onClick={() => copyToClipboard(generateEmbedUrl('/embed/wallet'), 'url')}
              >
                {copied === 'url' ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
              </Button>
            </div>
          </div>

          <div className="space-y-3">
            <h4 className="text-sm font-semibold text-slate-700">Cách 2: Nhúng iFrame</h4>
            <p className="text-xs text-slate-500">
              Copy đoạn code HTML bên dưới và dán vào trang web của bạn.
            </p>
            <div className="relative">
              <pre className="p-4 bg-slate-900 text-slate-100 rounded-lg text-xs overflow-x-auto">
                {generateIframeCode('/embed/wallet')}
              </pre>
              <Button
                size="sm"
                variant="ghost"
                className="absolute top-2 right-2 text-slate-400 hover:text-white"
                onClick={() => copyToClipboard(generateIframeCode('/embed/wallet'), 'iframe')}
              >
                {copied === 'iframe' ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Available Routes */}
      <Card className="border-slate-200 shadow-sm">
        <CardHeader>
          <CardTitle className="text-lg">Các Route có sẵn</CardTitle>
          <CardDescription>Danh sách các trang có thể nhúng vào ứng dụng</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {embedRoutes.map((route) => {
              const Icon = route.icon;
              return (
                <div key={route.path} className="p-4 border border-slate-200 rounded-lg space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className={`h-10 w-10 rounded-lg ${route.bgColor} flex items-center justify-center`}>
                        <Icon className={`h-5 w-5 ${route.color}`} />
                      </div>
                      <div>
                        <h4 className="font-semibold text-slate-900 text-sm">{route.name}</h4>
                        <p className="text-xs text-slate-500">{route.description}</p>
                      </div>
                    </div>
                    <Badge variant="outline" className="font-mono text-xs">
                      {route.path}
                    </Badge>
                  </div>

                  <div className="flex items-center gap-2">
                    <code className="flex-1 p-2 bg-slate-100 rounded text-xs text-slate-600 font-mono truncate">
                      {generateEmbedUrl(route.path)}
                    </code>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => copyToClipboard(generateEmbedUrl(route.path), route.path)}
                    >
                      {copied === route.path ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => window.open(generateEmbedUrl(route.path), '_blank')}
                    >
                      <ExternalLink className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>

      {/* API Key Info */}
      <Card className="border-amber-200 bg-amber-50 shadow-sm">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2 text-amber-800">
            <AlertTriangle className="h-5 w-5" />
            Lưu ý về API Key
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm text-amber-700">
          <ul className="list-disc list-inside space-y-1">
            <li>Mỗi tenant có một API Key duy nhất được cấp bởi quản trị viên</li>
            <li>API Key được truyền qua tham số URL: <code className="bg-amber-100 px-1 rounded">?api-key=YOUR_KEY</code></li>
            <li>Nếu không truyền API Key hoặc API Key không hợp lệ, giao diện sẽ hiển thị thông báo lỗi quyền truy cập</li>
            <li>API Key cần được xác thực với backend trước khi hiển thị dữ liệu</li>
            <li>Không nhúng API Key trực tiếp vào mã nguồn công khai</li>
          </ul>
        </CardContent>
      </Card>

      {/* PostMessage API */}
      <Card className="border-slate-200 shadow-sm">
        <CardHeader>
          <CardTitle className="text-lg">PostMessage API</CardTitle>
          <CardDescription>Giao tiếp giữa trang cha và iFrame</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <h4 className="text-sm font-semibold text-slate-700">Nhận tín hiệu RESIZE từ iFrame</h4>
            <pre className="p-3 bg-slate-100 rounded-lg text-xs text-slate-700 overflow-x-auto">
              {`window.addEventListener('message', (event) => {
  if (event.data.action === 'RESIZE') {
    // event.data.height: chiều cao hiện tại của nội dung
    // event.data.path: đường dẫn route hiện tại
    iframe.style.height = event.data.height + 'px';
  }
});`}
            </pre>
          </div>

          <div className="space-y-2">
            <h4 className="text-sm font-semibold text-slate-700">Điều hướng iFrame từ trang cha</h4>
            <pre className="p-3 bg-slate-100 rounded-lg text-xs text-slate-700 overflow-x-auto">
              {`iframe.contentWindow.postMessage({
  action: 'NAVIGATE',
  to: '/embed/transactions'
}, '*');`}
            </pre>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
