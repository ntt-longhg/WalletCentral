import React, { useState, useEffect, useCallback } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/components/ui/toast';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useConfigStore } from '@/stores';
import { SystemConfigResponse } from '@/types/api';
import { Settings, Mail, Key, ShieldCheck, Save, Loader2 } from 'lucide-react';

const GROUP_ICONS: Record<string, React.ReactNode> = {
  SMTP: <Mail className="h-4 w-4" />,
  OTP: <Key className="h-4 w-4" />,
  AUTH: <ShieldCheck className="h-4 w-4" />,
};

const GROUP_LABELS: Record<string, string> = {
  SMTP: 'Cấu hình Email (SMTP)',
  OTP: 'Cấu hình Mã OTP',
  AUTH: 'Cấu hình Xác thực',
};

export const AdminSettingsPage: React.FC = () => {
  const { addToast } = useToast();
  const { configs, configsLoading: loading, fetchConfigs, updateConfigs } = useConfigStore();
  const [editedValues, setEditedValues] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState('SMTP');

  const loadData = useCallback(async () => {
    try {
      await fetchConfigs();
    } catch {
      addToast({ variant: 'destructive', message: 'Không thể tải cấu hình hệ thống.' });
    }
  }, [fetchConfigs, addToast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    if (configs.length > 0) {
      const values: Record<string, string> = {};
      configs.forEach((c) => {
        values[c.key] = c.value;
      });
      setEditedValues(values);
    }
  }, [configs]);

  const handleChange = (key: string, value: string) => {
    setEditedValues((prev) => ({ ...prev, [key]: value }));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const updates = Object.entries(editedValues).map(([key, value]) => ({ key, value }));
      await updateConfigs(updates);
      addToast({ variant: 'success', message: 'Cấu hình đã được lưu thành công!' });
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Lưu cấu hình thất bại.' });
    } finally {
      setSaving(false);
    }
  };

  const getGroupConfigs = (group: string) => configs.filter((c) => c.group === group);

  const renderConfigField = (config: SystemConfigResponse) => {
    const isPassword = config.key.toLowerCase().includes('password');
    return (
      <div key={config.key} className="grid grid-cols-1 sm:grid-cols-3 gap-4 items-start py-3 border-b border-slate-100 last:border-0">
        <div className="space-y-1">
          <Label className="text-sm font-medium text-slate-700">{config.key}</Label>
          {config.description && (
            <p className="text-xs text-slate-400">{config.description}</p>
          )}
        </div>
        <div className="sm:col-span-2">
          <Input
            type={isPassword ? 'password' : 'text'}
            value={editedValues[config.key] || ''}
            onChange={(e) => handleChange(config.key, e.target.value)}
            placeholder={config.description || config.key}
            className="max-w-md"
          />
        </div>
      </div>
    );
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Settings className="h-5 w-5 text-blue-600" />
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Cấu hình Hệ thống</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">Quản lý cài đặt SMTP, OTP và xác thực admin</p>
        </div>
        <Button onClick={handleSave} disabled={saving} className="gap-2 bg-blue-600 hover:bg-blue-700">
          {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
          {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
        </Button>
      </div>

      {/* Tabs */}
      <Card className="border-slate-200 shadow-sm">
        <CardContent className="pt-6">
          <Tabs value={activeTab} onValueChange={setActiveTab}>
            <TabsList className="mb-6">
              {['SMTP', 'OTP', 'AUTH'].map((group) => (
                <TabsTrigger key={group} value={group} className="gap-2">
                  {GROUP_ICONS[group]}
                  {GROUP_LABELS[group]}
                </TabsTrigger>
              ))}
            </TabsList>

            {['SMTP', 'OTP', 'AUTH'].map((group) => (
              <TabsContent key={group} value={group}>
                <div className="space-y-0">
                  {loading ? (
                    <div className="flex items-center justify-center py-10 text-slate-400">
                      <Loader2 className="h-6 w-6 animate-spin mr-2" />
                      <span className="text-sm">Đang tải cấu hình...</span>
                    </div>
                  ) : getGroupConfigs(group).length === 0 ? (
                    <div className="text-center py-10 text-slate-400 text-sm">
                      Không có cấu hình nào trong nhóm này.
                    </div>
                  ) : (
                    getGroupConfigs(group).map(renderConfigField)
                  )}
                </div>
              </TabsContent>
            ))}
          </Tabs>
        </CardContent>
      </Card>
    </div>
  );
};
