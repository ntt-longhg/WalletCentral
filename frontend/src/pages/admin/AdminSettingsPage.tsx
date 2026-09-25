import React, { useState, useEffect, useCallback } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useToast } from '@/components/ui/toast';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Switch } from '@/components/ui/switch';
import { useConfigStore } from '@/stores';
import { useAuth } from '@/context/AuthContext';
import { authService } from '@/services/billingServices';
import { SystemConfigResponse, ConfigReloadResponse } from '@/types/api';
import { Settings, Mail, Key, ShieldCheck, Save, Loader2, AlertCircle, CheckCircle2, Lock, Bell, Receipt, FileText, Rabbit, RefreshCw, CalendarClock } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const GROUP_ICONS: Record<string, React.ReactNode> = {
  SMTP: <Mail className="h-4 w-4" />,
  OTP: <Key className="h-4 w-4" />,
  AUTH: <ShieldCheck className="h-4 w-4" />,
  ALERT: <Bell className="h-4 w-4" />,
  BILLING: <Receipt className="h-4 w-4" />,
  INVOICE: <FileText className="h-4 w-4" />,
  MQ: <Rabbit className="h-4 w-4" />,
  SCHEDULER: <CalendarClock className="h-4 w-4" />,
};

const GROUP_LABELS: Record<string, string> = {
  SMTP: 'Cấu hình Email (SMTP)',
  OTP: 'Cấu hình Mã OTP',
  AUTH: 'Cấu hình Xác thực',
  ALERT: 'Cấu hình Cảnh báo',
  BILLING: 'Cấu hình Billing',
  INVOICE: 'Cấu hình Hóa đơn',
  MQ: 'Cấu hình Hàng đợi',
  SCHEDULER: 'Cấu hình Lịch chạy',
};

const KNOWN_GROUP_ORDER = ['SMTP', 'OTP', 'AUTH', 'ALERT', 'BILLING', 'INVOICE', 'MQ', 'SCHEDULER'];

interface FieldOption {
  value: string;
  label: string;
}

const parseFieldOptions = (json?: string): FieldOption[] => {
  if (!json) return [];
  try {
    const arr = JSON.parse(json);
    if (!Array.isArray(arr)) return [];
    return arr
      .filter((o) => o && typeof o.value === 'string')
      .map((o) => ({ value: o.value, label: typeof o.label === 'string' ? o.label : o.value }));
  } catch {
    return [];
  }
};

const parseBoolValue = (value?: string) =>
  ['true', '1', 'yes', 'on', 'open', 'enabled'].includes((value || '').trim().toLowerCase());

export const AdminSettingsPage: React.FC = () => {
  const { addToast } = useToast();
  const { configs, configsLoading: loading, fetchConfigs, updateConfigs, reloadConfigs } = useConfigStore();
  const [editedValues, setEditedValues] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [pendingSync, setPendingSync] = useState(false);
  const [reloadResult, setReloadResult] = useState<ConfigReloadResponse | null>(null);
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
      const savedCount = await updateConfigs(updates);
      setPendingSync(true);
      setReloadResult(null);
      addToast({ variant: 'success', message: `Đã lưu ${savedCount} cấu hình vào CSDL. Nhấn "Đồng bộ áp dụng" để có hiệu lực.` });
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Lưu cấu hình thất bại.' });
    } finally {
      setSaving(false);
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    try {
      const result = await reloadConfigs();
      setReloadResult(result);
      setPendingSync(false);
      if (!result) {
        addToast({ variant: 'destructive', message: 'Đồng bộ thất bại.' });
      } else if (result.totalChanged === 0) {
        addToast({ variant: 'success', message: `Backend đã cập nhật — không có gì thay đổi (${result.totalCount} keys).` });
      } else {
        addToast({
          variant: 'success',
          message: `Đã áp dụng ${result.totalChanged} thay đổi (${result.changedCount} sửa, ${result.addedCount} thêm, ${result.removedCount} xóa).`,
        });
      }
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Đồng bộ thất bại.' });
    } finally {
      setSyncing(false);
    }
  };

  const getGroupConfigs = (group: string) => configs.filter((c) => c.group === group);

  // Tabs follow whatever groups exist in DB, so new config groups appear without code changes
  const groups = React.useMemo(() => {
    const fromDb = Array.from(new Set(configs.map((c) => c.group)));
    const ordered = KNOWN_GROUP_ORDER.filter((g) => fromDb.includes(g));
    const extra = fromDb.filter((g) => !KNOWN_GROUP_ORDER.includes(g)).sort();
    return [...ordered, ...extra];
  }, [configs]);

  useEffect(() => {
    if (groups.length > 0 && !groups.includes(activeTab)) {
      setActiveTab(groups[0]);
    }
  }, [groups, activeTab]);

  const renderFieldInput = (config: SystemConfigResponse) => {
    const type = (config.fieldType || '').toLowerCase();
    const value = editedValues[config.key] ?? '';
    const options = parseFieldOptions(config.fieldOptions);
    const inputClass = 'max-w-md';

    switch (type) {
      case 'number':
        return (
          <Input
            type="number"
            value={value}
            onChange={(e) => handleChange(config.key, e.target.value)}
            placeholder={config.description || config.key}
            className={inputClass}
          />
        );
      case 'password':
        return (
          <Input
            type="password"
            value={value}
            onChange={(e) => handleChange(config.key, e.target.value)}
            placeholder={config.description || config.key}
            className={inputClass}
            autoComplete="new-password"
          />
        );
      case 'textarea':
        return (
          <Textarea
            value={value}
            onChange={(e) => handleChange(config.key, e.target.value)}
            placeholder={config.description || config.key}
            className={inputClass}
            rows={3}
          />
        );
      case 'time':
        return (
          <Input
            type="time"
            step={1}
            value={value}
            onChange={(e) => handleChange(config.key, e.target.value)}
            className={inputClass}
          />
        );
      case 'select':
        if (options.length === 0) break;
        return (
          <Select value={value} onValueChange={(v) => handleChange(config.key, v)}>
            <SelectTrigger className={inputClass}>
              <SelectValue placeholder={config.description || config.key} />
            </SelectTrigger>
            <SelectContent>
              {options.map((opt) => (
                <SelectItem key={opt.value} value={opt.value}>
                  {opt.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        );
      case 'radio':
        if (options.length === 0) break;
        return (
          <RadioGroup value={value} onValueChange={(v) => handleChange(config.key, v)} className="flex flex-col gap-2">
            {options.map((opt) => (
              <div key={opt.value} className="flex items-center gap-2">
                <RadioGroupItem value={opt.value} id={`${config.key}-${opt.value}`} />
                <Label htmlFor={`${config.key}-${opt.value}`} className="text-sm font-normal text-slate-700">
                  {opt.label}
                </Label>
              </div>
            ))}
          </RadioGroup>
        );
      case 'boolean':
        return (
          <div className="flex items-center gap-3">
            <Switch
              checked={parseBoolValue(value)}
              onCheckedChange={(checked) => handleChange(config.key, checked ? 'true' : 'false')}
            />
            <span className="text-sm text-slate-600">{parseBoolValue(value) ? 'Bật' : 'Tắt'}</span>
          </div>
        );
      default:
        break;
    }

    // Fallback for legacy rows without field_type: password heuristic, then plain text
    const isPassword = !config.fieldType && config.key.toLowerCase().includes('password');
    return (
      <Input
        type={isPassword ? 'password' : 'text'}
        value={value}
        onChange={(e) => handleChange(config.key, e.target.value)}
        placeholder={config.description || config.key}
        className={inputClass}
      />
    );
  };

  const renderConfigField = (config: SystemConfigResponse) => {
    return (
      <div key={config.key} className="grid grid-cols-1 sm:grid-cols-3 gap-4 items-start py-3 border-b border-slate-100 last:border-0">
        <div className="space-y-1">
          <Label className="text-sm font-medium text-slate-700">{config.key}</Label>
          {config.description && (
            <p className="text-xs text-slate-400">{config.description}</p>
          )}
        </div>
        <div className="sm:col-span-2">
          {renderFieldInput(config)}
        </div>
      </div>
    );
  };

  const renderChangePasswordCard = () => (
    <ChangePasswordCard />
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Settings className="h-5 w-5 text-blue-600" />
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Cấu hình Hệ thống</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">Lưu chỉ ghi vào CSDL — nhấn Đồng bộ để áp dụng ngay, không cần deploy lại</p>
        </div>
        <div className="flex gap-2">
          <Button onClick={handleSave} disabled={saving || syncing} className="gap-2 bg-blue-600 hover:bg-blue-700">
            {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
            {saving ? 'Đang lưu...' : 'Lưu vào CSDL'}
          </Button>
          <Button onClick={handleSync} disabled={saving || syncing} variant="outline" className="gap-2">
            {syncing ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
            {syncing ? 'Đang đồng bộ...' : 'Đồng bộ áp dụng'}
          </Button>
        </div>
      </div>

      {pendingSync && (
        <Alert variant="default" className="border-amber-300 bg-amber-50">
          <AlertCircle className="h-4 w-4 text-amber-600" />
          <AlertDescription className="text-amber-800">
            Đã lưu vào CSDL nhưng backend chưa áp dụng. Nhấn <strong>Đồng bộ áp dụng</strong> để có hiệu lực ngay.
          </AlertDescription>
        </Alert>
      )}

      {reloadResult && (
        <Card className="border-slate-200 shadow-sm">
          <CardContent className="pt-4">
            <div className="flex items-center gap-2 mb-2">
              <CheckCircle2 className="h-4 w-4 text-green-600" />
              <p className="text-sm font-medium text-slate-700">
                Đồng bộ lúc {reloadResult.loadedAt ? new Date(reloadResult.loadedAt).toLocaleString('vi-VN') : ''}:
                {' '}{reloadResult.totalChanged} thay đổi
                ({reloadResult.changedCount} sửa, {reloadResult.addedCount} thêm, {reloadResult.removedCount} xóa)
                / {reloadResult.totalCount} keys
              </p>
            </div>
            {reloadResult.changes.length === 0 ? (
              <p className="text-xs text-slate-400">Không có gì thay đổi — backend đã cập nhật.</p>
            ) : (
              <div className="max-h-64 overflow-y-auto rounded-md border border-slate-100">
                <table className="w-full text-xs">
                  <thead className="bg-slate-50 text-slate-500 sticky top-0">
                    <tr>
                      <th className="text-left px-3 py-2 font-medium">Key</th>
                      <th className="text-left px-3 py-2 font-medium">Loại</th>
                      <th className="text-left px-3 py-2 font-medium">Giá trị cũ</th>
                      <th className="text-left px-3 py-2 font-medium">Giá trị mới</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reloadResult.changes.map((c) => (
                      <tr key={c.key} className="border-t border-slate-100">
                        <td className="px-3 py-2 font-mono text-slate-700">{c.key}</td>
                        <td className="px-3 py-2">
                          <span className={`inline-block rounded px-1.5 py-0.5 font-medium ${
                            c.changeType === 'CHANGED' ? 'bg-blue-100 text-blue-700'
                            : c.changeType === 'ADDED' ? 'bg-green-100 text-green-700'
                            : 'bg-red-100 text-red-700'
                          }`}>
                            {c.changeType}
                          </span>
                        </td>
                        <td className="px-3 py-2 font-mono text-slate-500 break-all">
                          {c.secret ? '********' : (c.oldValue ?? <span className="italic">—</span>)}
                        </td>
                        <td className="px-3 py-2 font-mono text-slate-700 break-all">
                          {c.secret ? '********' : (c.newValue ?? <span className="italic">—</span>)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Tabs */}
      <Card className="border-slate-200 shadow-sm">
        <CardContent className="pt-6">
          <Tabs value={activeTab} onValueChange={setActiveTab}>
            <TabsList className="mb-6 flex flex-wrap h-auto gap-1">
              {groups.map((group) => (
                <TabsTrigger key={group} value={group} className="gap-2">
                  {GROUP_ICONS[group] ?? <Settings className="h-4 w-4" />}
                  {GROUP_LABELS[group] ?? group}
                </TabsTrigger>
              ))}
            </TabsList>

            {groups.map((group) => (
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
                {group === 'AUTH' && (
                  <div className="mt-6 pt-4 border-t border-slate-200">
                    {renderChangePasswordCard()}
                  </div>
                )}
              </TabsContent>
            ))}
          </Tabs>
        </CardContent>
      </Card>
    </div>
  );
};

const ChangePasswordCard: React.FC = () => {
  const { addToast } = useToast();
  const { adminLogout } = useAuth();
  const navigate = useNavigate();
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (newPassword !== confirmPassword) {
      setError('Mật khẩu nhập lại không khớp.');
      return;
    }
    if (newPassword.length < 8) {
      setError('Mật khẩu phải từ 8 ký tự trở lên.');
      return;
    }
    setSaving(true);
    try {
      await authService.changePassword(oldPassword, newPassword);
      addToast({ variant: 'success', message: 'Đổi mật khẩu thành công! Tất cả phiên đã bị đăng xuất, vui lòng đăng nhập lại.' });
      await adminLogout();
      navigate('/admin/login');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Đổi mật khẩu thất bại.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card className="border-slate-200">
      <CardHeader className="pb-2">
        <CardTitle className="text-base flex items-center gap-2">
          <Lock className="h-4 w-4 text-blue-600" />
          Đổi mật khẩu của tôi
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleChangePassword} className="space-y-3 max-w-md">
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          <div className="space-y-2">
            <Label>Mật khẩu hiện tại</Label>
            <Input type="password" value={oldPassword} onChange={(e) => setOldPassword(e.target.value)} required />
          </div>
          <div className="space-y-2">
            <Label>Mật khẩu mới (tối thiểu 8 ký tự)</Label>
            <Input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required minLength={8} />
          </div>
          <div className="space-y-2">
            <Label>Nhập lại mật khẩu mới</Label>
            <Input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required minLength={8} />
          </div>
          <Button type="submit" disabled={saving} className="gap-2">
            {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
            {saving ? 'Đang đổi...' : 'Đổi mật khẩu'}
          </Button>
          <p className="text-xs text-slate-400">Sau khi đổi, mọi phiên đăng nhập (kể cả thiết bị khác) sẽ bị đăng xuất.</p>
        </form>
      </CardContent>
    </Card>
  );
};
