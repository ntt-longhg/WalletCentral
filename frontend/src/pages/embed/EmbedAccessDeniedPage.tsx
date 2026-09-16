import React from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { useAuth } from '@/context/AuthContext';
import { ShieldAlert, ArrowLeft, ExternalLink } from 'lucide-react';

export const EmbedAccessDeniedPage: React.FC = () => {
  const { token } = useAuth();

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-red-50 via-orange-50 to-yellow-50">
      <Card className="w-full max-w-md border-red-200 shadow-lg">
        <CardHeader className="text-center pb-4">
          <div className="inline-flex items-center justify-center h-16 w-16 rounded-full bg-red-100 mx-auto mb-4">
            <ShieldAlert className="h-8 w-8 text-red-600" />
          </div>
          <CardTitle className="text-xl text-red-800">Không có quyền truy cập</CardTitle>
          <CardDescription className="text-red-600">
            Bạn không được phép truy cập nội dung này
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Alert variant="destructive">
            <AlertTitle>Lỗi xác thực</AlertTitle>
            <AlertDescription>
              {!token
                ? 'Thiếu tham số API Key. Vui lòng kiểm tra lại URL nhúng và đảm bảo đã truyền tham số api-key.'
                : 'API Key không hợp lệ hoặc đã hết hạn. Vui lòng liên hệ quản trị viên để được cấp API Key mới.'}
            </AlertDescription>
          </Alert>

          <div className="p-4 bg-slate-50 rounded-lg space-y-2">
            <p className="text-xs font-semibold text-slate-700">Hướng dẫn khắc phục:</p>
            <ol className="text-xs text-slate-600 space-y-1 list-decimal list-inside">
              <li>Kiểm tra URL nhúng có chứa tham số <code className="bg-slate-200 px-1 rounded">?api-key=YOUR_KEY</code></li>
              <li>Đảm bảo API Key là hợp lệ và được hệ thống xác thực</li>
              <li>Liên hệ quản trị viên nếu bạn chưa có API Key</li>
            </ol>
          </div>

          <div className="p-3 bg-blue-50 rounded-lg">
            <p className="text-xs text-blue-700 font-medium mb-1">Ví dụ URL nhúng đúng:</p>
            <code className="text-[11px] text-blue-800 break-all">
              https://your-domain.com/embed/wallet?api-key=YOUR_API_KEY
            </code>
          </div>

          <Button
            variant="outline"
            className="w-full"
            onClick={() => window.history.back()}
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            Quay lại
          </Button>
        </CardContent>
      </Card>
    </div>
  );
};
