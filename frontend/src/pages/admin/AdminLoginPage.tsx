import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useAuth } from '@/context/AuthContext';
import { authService } from '@/services/billingServices';
import type { LoginConfigResponse } from '@/types/api';
import { ShieldCheck, Mail, KeyRound, AlertCircle, CheckCircle2, Loader2, ArrowLeft, Clock, Info } from 'lucide-react';
// import Logo from '../../assets/logo.svg';
import { Mascot } from 'page-mascot'

type OtpStep = 'email' | 'otp';
type LoginTab = 'otp' | 'password';

export const AdminLoginPage: React.FC = () => {
  const [loginConfig, setLoginConfig] = useState<LoginConfigResponse | null>(null);
  const [configLoading, setConfigLoading] = useState(true);
  const [loginTab, setLoginTab] = useState<LoginTab>('otp');
  const [step, setStep] = useState<OtpStep>('email');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [password, setPassword] = useState('');
  const [setupMode, setSetupMode] = useState(false);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const countdownRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const { adminSendOtp, adminVerifyOtp, adminLoginWithPassword, adminSetupPassword } = useAuth();
  const navigate = useNavigate();
  const otpInputRef = useRef<HTMLInputElement>(null);

  // Dynamic login parameters from backend (fall back to previous hardcodes)
  const otpLength = loginConfig?.otpLength && loginConfig.otpLength > 0 ? loginConfig.otpLength : 6;
  const otpExpiryMinutes = loginConfig?.otpExpiryMinutes && loginConfig.otpExpiryMinutes > 0 ? loginConfig.otpExpiryMinutes : 5;
  const resendCooldown = Number(loginConfig?.resendCooldownSeconds) > 0 ? Number(loginConfig?.resendCooldownSeconds) : 60;
  const passwordMinLength = loginConfig?.passwordMinLength && loginConfig.passwordMinLength > 0 ? loginConfig.passwordMinLength : 8;
  const domainHint = (loginConfig?.allowedDomains || 'dntg.com.vn').split(',')[0].trim() || 'dntg.com.vn';

  // Load public login config (which methods are enabled)
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await authService.getLoginConfig();
        if (cancelled) return;
        const cfg = res.data?.data;
        if (cfg) {
          setLoginConfig(cfg);
          if (!cfg.otpLoginEnabled && cfg.passwordLoginEnabled) setLoginTab('password');
        } else {
          // Fallback to OTP-only if backend is old/unreachable shape
          setLoginConfig({ loginMode: 'otp', passwordLoginEnabled: false, otpLoginEnabled: true, passwordSetupOpen: false });
        }
      } catch {
        if (!cancelled) {
          setLoginConfig({ loginMode: 'otp', passwordLoginEnabled: false, otpLoginEnabled: true, passwordSetupOpen: false });
        }
      } finally {
        if (!cancelled) setConfigLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    return () => {
      if (countdownRef.current) clearInterval(countdownRef.current);
    };
  }, []);

  useEffect(() => {
    if (step === 'otp' && otpInputRef.current) {
      otpInputRef.current.focus();
    }
  }, [step]);

  useEffect(() => {
    if (countdown > 0) {
      countdownRef.current = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            if (countdownRef.current) clearInterval(countdownRef.current);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
      return () => {
        if (countdownRef.current) clearInterval(countdownRef.current);
      };
    }
  }, [countdown]);

  const resetMessages = () => {
    setError('');
    setSuccess('');
  };

  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    resetMessages();
    setLoading(true);

    try {
      await adminSendOtp(email);
      const maskedEmail = email.replace(/(.{4}).*@/, '$1***@');
      setSuccess(`Mã OTP đã gửi đến ${maskedEmail}. Vui lòng kiểm tra email.`);
      setStep('otp');
      setCountdown(resendCooldown);
    } catch (err: any) {
      const code = err.response?.data?.code;
      const message = err.response?.data?.message;
      if (code === 'SMTP_SEND_FAILED' && loginConfig?.passwordLoginEnabled) {
        // Flexible fallback: SMTP unreachable -> switch to password tab
        setNotice('Không gửi được OTP (mail server không khả dụng). Vui lòng đăng nhập bằng mật khẩu.');
        setLoginTab('password');
      } else {
        setError(message || 'Không thể gửi mã OTP. Vui lòng thử lại.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const success = await adminVerifyOtp(email, otp);
      if (success) {
        navigate('/admin');
      } else {
        setError('Mã OTP không hợp lệ. Vui lòng kiểm tra lại.');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Xác thực thất bại. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  const handleResendOtp = async () => {
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      await adminSendOtp(email);
      setSuccess(`Mã OTP mới đã gửi đến ${email}.`);
      setCountdown(resendCooldown);
    } catch (err: any) {
      const code = err.response?.data?.code;
      if (code === 'SMTP_SEND_FAILED' && loginConfig?.passwordLoginEnabled) {
        setNotice('Không gửi được OTP (mail server không khả dụng). Vui lòng đăng nhập bằng mật khẩu.');
        setLoginTab('password');
      } else {
        setError(err.response?.data?.message || 'Không thể gửi lại mã OTP.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleBackToEmail = () => {
    setStep('email');
    setOtp('');
    setError('');
    setSuccess('');
    setCountdown(0);
    if (countdownRef.current) clearInterval(countdownRef.current);
  };

  const handlePasswordLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    resetMessages();
    setLoading(true);

    try {
      const result = await adminLoginWithPassword(email, password);
      if (result.ok) {
        navigate('/admin');
      } else if (result.code === 'ACCOUNT_LOCKED') {
        setError(result.message || 'Tài khoản tạm khóa do đăng nhập sai nhiều lần.');
      } else {
        setError(result.message || 'Email hoặc mật khẩu không đúng.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSetupPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    resetMessages();
    if (newPassword !== confirmPassword) {
      setError('Mật khẩu nhập lại không khớp.');
      return;
    }
    if (newPassword.length < passwordMinLength) {
      setError(`Mật khẩu phải từ ${passwordMinLength} ký tự trở lên.`);
      return;
    }
    setLoading(true);

    try {
      const result = await adminSetupPassword(email, newPassword);
      if (result.ok) {
        navigate('/admin');
      } else if (result.code === 'PASSWORD_ALREADY_SET') {
        setError('Tài khoản đã có mật khẩu. Vui lòng đăng nhập.');
        setSetupMode(false);
      } else {
        setError(result.message || 'Tạo mật khẩu thất bại. Vui lòng thử lại.');
      }
    } finally {
      setLoading(false);
    }
  };

  const showTabs = loginConfig != null
    && loginConfig.otpLoginEnabled
    && loginConfig.passwordLoginEnabled;

  const renderPasswordForm = () => (
    <>
      {notice && (
        <Alert variant="default" className="border-amber-300 bg-amber-50">
          <Info className="h-4 w-4 text-amber-600" />
          <AlertDescription className="text-amber-800">{notice}</AlertDescription>
        </Alert>
      )}

      {!setupMode ? (
        <form onSubmit={handlePasswordLogin} className="space-y-4">
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          <div className="space-y-2">
            <Label htmlFor="pw-email">Email quản trị</Label>
            <Input
              id="pw-email"
              type="email"
              placeholder={`admin@${domainHint}`}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="pw-password">Mật khẩu</Label>
            <Input
              id="pw-password"
              type="password"
              placeholder="Nhập mật khẩu"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin mr-2" />
                Đang đăng nhập...
              </>
            ) : (
              <>
                <ShieldCheck className="h-4 w-4 mr-2" />
                Đăng nhập
              </>
            )}
          </Button>

          {loginConfig?.passwordSetupOpen && (
            <div className="text-center pt-1">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => { setSetupMode(true); resetMessages(); }}
                className="gap-1 text-blue-600"
              >
                <KeyRound className="h-3 w-3" />
                Chưa có mật khẩu? Tạo mật khẩu lần đầu
              </Button>
            </div>
          )}
        </form>
      ) : (
        <form onSubmit={handleSetupPassword} className="space-y-4">
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          <Alert variant="default" className="border-blue-200 bg-blue-50">
            <Info className="h-4 w-4 text-blue-600" />
            <AlertDescription className="text-blue-800">
              Tạo mật khẩu lần đầu cho tài khoản (mỗi tài khoản chỉ tạo 1 lần).
            </AlertDescription>
          </Alert>

          <div className="space-y-2">
            <Label htmlFor="setup-email">Email quản trị</Label>
            <Input
              id="setup-email"
              type="email"
              placeholder={`admin@${domainHint}`}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="setup-password">Mật khẩu mới (tối thiểu {passwordMinLength} ký tự)</Label>
            <Input
              id="setup-password"
              type="password"
              placeholder="Nhập mật khẩu mới"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
              minLength={passwordMinLength}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="setup-confirm">Nhập lại mật khẩu</Label>
            <Input
              id="setup-confirm"
              type="password"
              placeholder="Nhập lại mật khẩu mới"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              minLength={passwordMinLength}
            />
          </div>

          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin mr-2" />
                Đang tạo...
              </>
            ) : (
              <>
                <KeyRound className="h-4 w-4 mr-2" />
                Tạo mật khẩu & đăng nhập
              </>
            )}
          </Button>

          <div className="text-center pt-1">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => { setSetupMode(false); resetMessages(); }}
              className="gap-1 text-slate-500"
            >
              <ArrowLeft className="h-3 w-3" />
              Quay lại đăng nhập
            </Button>
          </div>
        </form>
      )}
    </>
  );

  const renderOtpFlow = () => (
    <>
      {/* Step 1: Email */}
      {step === 'email' && (
        <form onSubmit={handleSendOtp} className="space-y-4">
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {success && (
            <Alert variant="success">
              <CheckCircle2 className="h-4 w-4" />
              <AlertDescription>{success}</AlertDescription>
            </Alert>
          )}

          <div className="space-y-2">
            <Label htmlFor="email">Email quản trị</Label>
            <Input
              id="email"
              type="email"
                    placeholder={`admin@${domainHint}`}
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    autoFocus
                  />
                  <p className="text-xs text-slate-400">Chỉ chấp nhận email với đuôi @{domainHint}</p>
          </div>

          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin mr-2" />
                Đang gửi...
              </>
            ) : (
              <>
                <Mail className="h-4 w-4 mr-2" />
                Gửi mã OTP
              </>
            )}
          </Button>
        </form>
      )}

      {/* Step 2: OTP */}
      {step === 'otp' && (
        <form onSubmit={handleVerifyOtp} className="space-y-4">
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {success && (
            <Alert variant="success">
              <CheckCircle2 className="h-4 w-4" />
              <AlertDescription>{success}</AlertDescription>
            </Alert>
          )}

          <div className="space-y-2">
            <Label htmlFor="otp">Mã OTP</Label>
            <Input
              id="otp"
              ref={otpInputRef}
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={otpLength}
              placeholder="Nhập mã OTP"
              value={otp}
              onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
              required
              className="text-center text-lg tracking-[0.5em] font-mono"
            />
            <p className="text-xs text-slate-400 text-center">
              Mã có hiệu lực trong {otpExpiryMinutes} phút
            </p>
          </div>

          <Button type="submit" className="w-full" disabled={loading || otp.length !== otpLength}>
            {loading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin mr-2" />
                Đang xác thực...
              </>
            ) : (
              <>
                <ShieldCheck className="h-4 w-4 mr-2" />
                Xác thực
              </>
            )}
          </Button>

          <div className="flex items-center justify-between pt-2">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={handleBackToEmail}
              className="gap-1 text-slate-500"
            >
              <ArrowLeft className="h-3 w-3" />
              Đổi email
            </Button>

            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={handleResendOtp}
              disabled={countdown > 0 || loading}
              className="gap-1 text-blue-600"
            >
              {countdown > 0 ? (
                <>
                  <Clock className="h-3 w-3" />
                  Gửi lại sau {countdown}s
                </>
              ) : (
                'Gửi lại mã'
              )}
            </Button>
          </div>
        </form>
      )}
    </>
  );

  return (
    <div className="flex items-center justify-center bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50 p-4 h-[100vh] overflow-y-auto">
      <div className="w-full max-w-md">
        {/* Brand Header */}
        <div className="text-center mb-8">
          {/* <img src={Logo} alt="WalletCentral Logo" className="h-24 mx-auto mb-3" /> */}
          <Mascot className="mx-auto mb-3" directions="/mascots/cloudpbx-directions.webp" reactions="/mascots/cloudpbx-reactions.webp" />
          <h1 className="text-2xl font-bold text-slate-900">WalletCentral</h1>
          <p className="text-sm text-slate-500 mt-1">Admin Control Panel</p>
        </div>

        <Card className="border-slate-200 shadow-lg">
          <CardHeader className="text-center pb-4">
            <div className="inline-flex items-center justify-center h-12 w-12 rounded-full bg-blue-50 mx-auto mb-3">
              {loginTab === 'otp' ? (
                <Mail className="h-6 w-6 text-blue-600" />
              ) : (
                <KeyRound className="h-6 w-6 text-blue-600" />
              )}
            </div>
            <CardTitle className="text-lg">
              {loginTab === 'otp'
                ? (step === 'email' ? 'Đăng nhập Quản trị' : 'Xác thực Mã OTP')
                : (setupMode ? 'Tạo Mật khẩu Lần đầu' : 'Đăng nhập bằng Mật khẩu')}
            </CardTitle>
            <CardDescription>
              {loginTab === 'otp'
                ? (step === 'email'
                    ? `Nhập email @${domainHint} để nhận mã xác thực`
                    : `Nhập mã OTP ${otpLength} chữ số đã gửi đến email`)
                : (setupMode
                    ? 'Tạo mật khẩu cho tài khoản quản trị của bạn'
                    : 'Nhập email và mật khẩu quản trị')}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {configLoading ? (
              <div className="flex items-center justify-center py-10 text-slate-400">
                <Loader2 className="h-6 w-6 animate-spin mr-2" />
                <span className="text-sm">Đang tải cấu hình đăng nhập...</span>
              </div>
            ) : (
              <>
                {showTabs && (
                  <Tabs value={loginTab} onValueChange={(v) => { setLoginTab(v as LoginTab); resetMessages(); setNotice(''); }} className="mb-4">
                    <TabsList className="grid w-full grid-cols-2">
                      <TabsTrigger value="otp" className="gap-2">
                        <Mail className="h-4 w-4" />
                        OTP Email
                      </TabsTrigger>
                      <TabsTrigger value="password" className="gap-2">
                        <KeyRound className="h-4 w-4" />
                        Mật khẩu
                      </TabsTrigger>
                    </TabsList>
                  </Tabs>
                )}

                {loginTab === 'otp' ? renderOtpFlow() : renderPasswordForm()}
              </>
            )}

            <div className="mt-6 pt-4 border-t border-slate-100">
              <p className="text-xs text-center text-slate-400">
                {loginTab === 'otp'
                  ? (step === 'email'
                    ? `Nhập email công ty @${domainHint} để đăng nhập.`
                    : 'Kiểm tra email để lấy mã OTP.')
                  : 'Đăng nhập bằng mật khẩu dành cho môi trường không có email.'}
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};
