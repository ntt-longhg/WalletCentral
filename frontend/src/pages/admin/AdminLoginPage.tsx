import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useAuth } from '@/context/AuthContext';
import { ShieldCheck, Mail, KeyRound, AlertCircle, CheckCircle2, Loader2, ArrowLeft, Clock } from 'lucide-react';
// import Logo from '../../assets/logo.svg';
import { Mascot } from 'page-mascot'

export const AdminLoginPage: React.FC = () => {
  const [step, setStep] = useState<'email' | 'otp'>('email');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const countdownRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const { adminSendOtp, adminVerifyOtp } = useAuth();
  const navigate = useNavigate();
  const otpInputRef = useRef<HTMLInputElement>(null);

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

  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      await adminSendOtp(email);
      const maskedEmail = email.replace(/(.{4}).*@/, '$1***@');
      setSuccess(`Mã OTP đã gửi đến ${maskedEmail}. Vui lòng kiểm tra email.`);
      setStep('otp');
      setCountdown(60);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Không thể gửi mã OTP. Vui lòng thử lại.');
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
      setCountdown(60);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Không thể gửi lại mã OTP.');
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
              {step === 'email' ? (
                <Mail className="h-6 w-6 text-blue-600" />
              ) : (
                <KeyRound className="h-6 w-6 text-blue-600" />
              )}
            </div>
            <CardTitle className="text-lg">
              {step === 'email' ? 'Đăng nhập Quản trị' : 'Xác thực Mã OTP'}
            </CardTitle>
            <CardDescription>
              {step === 'email'
                ? 'Nhập email @dntg.com.vn để nhận mã xác thực'
                : `Nhập mã OTP 6 chữ số đã gửi đến email`}
            </CardDescription>
          </CardHeader>
          <CardContent>
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
                    placeholder="admin@dntg.com.vn"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    autoFocus
                  />
                  <p className="text-xs text-slate-400">Chỉ chấp nhận email với đuôi @dntg.com.vn</p>
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
                    maxLength={6}
                    placeholder="Nhập mã OTP"
                    value={otp}
                    onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                    required
                    className="text-center text-lg tracking-[0.5em] font-mono"
                  />
                  <p className="text-xs text-slate-400 text-center">
                    Mã có hiệu lực trong 5 phút
                  </p>
                </div>

                <Button type="submit" className="w-full" disabled={loading || otp.length !== 6}>
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

            <div className="mt-6 pt-4 border-t border-slate-100">
              <p className="text-xs text-center text-slate-400">
                {step === 'email'
                  ? 'Nhập email công ty @dntg.com.vn để đăng nhập.'
                  : 'Kiểm tra email để lấy mã OTP.'}
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};
