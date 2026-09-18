import React, { useState, useEffect, useRef } from 'react';
import {
  Send,
  Phone,
  KeyRound,
  QrCode,
  ShieldCheck,
  Loader2,
  AlertCircle,
  RefreshCw,
  CheckCircle2,
} from 'lucide-react';
import { TelegramUser } from '../types';

interface LoginViewProps {
  onLoginSuccess: (user: TelegramUser, authenticatedSessionToken?: string) => void;
  lang?: 'ar' | 'en';
}

export const LoginView: React.FC<LoginViewProps> = ({ onLoginSuccess, lang = 'ar' }) => {
  const isAr = lang === 'ar';
  const [method, setMethod] = useState<'phone' | 'qr'>('phone');
  const [step, setStep] = useState<'phone' | 'code' | 'password'>('phone');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [sessionToken, setSessionToken] = useState<string>('');
  const [phoneCodeHash, setPhoneCodeHash] = useState('');
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // QR Login states
  const [qrUrl, setQrUrl] = useState<string>('');
  const [qrLoading, setQrLoading] = useState(false);
  const [qrExpired, setQrExpired] = useState(false);
  const pollIntervalRef = useRef<any>(null);

  // Initialize a fresh MTProto session token on mount
  useEffect(() => {
    let isMounted = true;
    const initSession = async () => {
      try {
        const res = await fetch('/api/telegram/accounts/new', { method: 'POST' });
        if (res.ok) {
          const data = await res.json();
          if (isMounted && data.sessionToken) {
            setSessionToken(data.sessionToken);
          }
        }
      } catch (err) {
        console.warn('Failed to pre-allocate session slot:', err);
      }
    };
    initSession();
    return () => {
      isMounted = false;
    };
  }, []);

  // Fetch or refresh real Telegram QR Login Token
  const loadQrToken = async (activeToken?: string) => {
    const tokenToUse = activeToken || sessionToken;
    setQrLoading(true);
    setError(null);
    setQrExpired(false);

    try {
      const res = await fetch('/api/telegram/qr/export', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionToken: tokenToUse }),
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.error || (isAr ? 'فشل توليد رمز QR' : 'Failed to export QR code'));
      }

      if (data.loggedIn && data.user) {
        onLoginSuccess(data.user, data.sessionToken || tokenToUse);
        return;
      }

      setQrUrl(data.qrUrl);
      if (data.sessionToken) {
        setSessionToken(data.sessionToken);
      }

      // Start polling for scan completion
      startQrPolling(data.sessionToken || tokenToUse);
    } catch (err: any) {
      setError(err.message || (isAr ? 'تعذر جلب رمز QR من خوادم تيليجرام' : 'Failed to fetch QR from Telegram'));
    } finally {
      setQrLoading(false);
    }
  };

  const startQrPolling = (token: string) => {
    if (pollIntervalRef.current) {
      clearInterval(pollIntervalRef.current);
    }

    let checkCount = 0;
    pollIntervalRef.current = setInterval(async () => {
      checkCount++;
      if (checkCount > 60) {
        // Expired after ~150 seconds
        clearInterval(pollIntervalRef.current);
        setQrExpired(true);
        return;
      }

      try {
        const res = await fetch('/api/telegram/qr/check', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ sessionToken: token }),
        });
        if (res.ok) {
          const data = await res.json();
          if (data.loggedIn && data.user) {
            clearInterval(pollIntervalRef.current);
            onLoginSuccess(data.user, data.sessionToken || token);
          }
        }
      } catch {}
    }, 2500);
  };

  useEffect(() => {
    if (method === 'qr') {
      loadQrToken();
    } else {
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current);
      }
    }
    return () => {
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current);
      }
    };
  }, [method]);

  // 1. Send Code to Real Telegram Phone Number
  const handleSendCode = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanPhone = phoneNumber.replace(/[\s\-\(\)]/g, '');
    if (!cleanPhone || cleanPhone.length < 5) {
      setError(isAr ? 'يرجى إدخال رقم هاتف صحيح مع رمز الدولة الدولي (مثال: +966...)' : 'Please enter a valid international phone number');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const res = await fetch('/api/telegram/send-code', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phoneNumber: cleanPhone, sessionToken }),
      });
      const data = await res.json();
      if (!res.ok) {
        let msg = data.error || (isAr ? 'فشل إرسال رمز التحقق' : 'Failed to send verification code');
        if (data.details?.includes('PHONE_NUMBER_INVALID')) {
          msg = isAr ? 'رقم الهاتف غير مسجل أو غير صالح في تليجرام' : 'Phone number is invalid in Telegram';
        } else if (data.details?.includes('FLOOD_WAIT')) {
          msg = isAr ? 'تم تجاوز عدد المحاولات، يرجى الانتظار قليلاً والمحاولة لاحقاً' : 'Too many attempts. Please try again later.';
        }
        throw new Error(msg);
      }

      setPhoneCodeHash(data.phoneCodeHash);
      if (data.sessionToken) {
        setSessionToken(data.sessionToken);
      }
      setStep('code');
    } catch (err: any) {
      setError(err.message || (isAr ? 'حدث خطأ أثناء التواصل مع سيرفرات تليجرام' : 'An error occurred connecting to Telegram'));
    } finally {
      setLoading(false);
    }
  };

  // 2. Sign In with Telegram Code
  const handleSignIn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!code.trim()) {
      setError(isAr ? 'يرجى إدخال رمز التحقق' : 'Please enter the verification code');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const res = await fetch('/api/telegram/sign-in', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          phoneNumber: phoneNumber.replace(/[\s\-\(\)]/g, ''),
          phoneCodeHash,
          phoneCode: code.trim(),
          sessionToken,
        }),
      });
      const data = await res.json();

      if (!res.ok) {
        let msg = data.error || (isAr ? 'رمز التحقق غير صحيح' : 'Invalid verification code');
        if (data.details?.includes('PHONE_CODE_INVALID')) {
          msg = isAr ? 'رمز التحقق غير صحيح. يرجى التأكد من الرمز المرسل' : 'Verification code is invalid';
        } else if (data.details?.includes('PHONE_CODE_EXPIRED')) {
          msg = isAr ? 'انتهت صلاحية رمز التحقق. يرجى طلب رمز جديد' : 'Code has expired. Please request a new code.';
        }
        throw new Error(msg);
      }

      if (data.requiresPassword) {
        setStep('password');
        setLoading(false);
        return;
      }

      if (!data.user) {
        throw new Error(isAr ? 'فشل استرجاع بيانات الحساب من تليجرام' : 'Failed to retrieve account data');
      }

      onLoginSuccess(data.user, data.sessionToken || sessionToken);
    } catch (err: any) {
      setError(err.message || (isAr ? 'فشل التحقق من الرمز' : 'Verification failed'));
    } finally {
      setLoading(false);
    }
  };

  // 3. Sign In with 2FA Cloud Password
  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!password) {
      setError(isAr ? 'يرجى إدخال كلمة المرور' : 'Please enter your password');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const res = await fetch('/api/telegram/sign-in-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          password,
          sessionToken,
        }),
      });
      const data = await res.json();

      if (!res.ok) {
        let msg = data.error || (isAr ? 'كلمة المرور غير صحيحة' : 'Password incorrect');
        if (data.details?.includes('PASSWORD_HASH_INVALID')) {
          msg = isAr ? 'كلمة المرور غير صحيحة. يرجى المحاولة مرة أخرى' : 'Incorrect 2FA password. Please try again.';
        }
        throw new Error(msg);
      }

      if (!data.user) {
        throw new Error(isAr ? 'فشل تسجيل الدخول' : 'Sign in failed');
      }

      onLoginSuccess(data.user, data.sessionToken || sessionToken);
    } catch (err: any) {
      setError(err.message || (isAr ? 'فشل تسجيل الدخول بكلمة المرور' : 'Password verification failed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="min-h-screen w-full flex items-center justify-center p-4 bg-[#0e1621] text-white font-sans select-none"
      dir={isAr ? 'rtl' : 'ltr'}
    >
      <div className="w-full max-w-md bg-[#17212b] border border-[#242f3d] rounded-3xl p-8 shadow-2xl relative overflow-hidden flex flex-col items-center">
        {/* Telegram Icon Logo */}
        <div className="w-24 h-24 rounded-full bg-linear-to-tr from-[#2aabee] to-[#229ed9] flex items-center justify-center shadow-xl shadow-[#2aabee]/20 mb-6 group transition hover:scale-105">
          <Send className="w-12 h-12 text-white -rotate-12 translate-x-1" />
        </div>

        <h1 className="text-2xl font-bold text-white mb-2">Telegram Web</h1>
        <p className="text-sm text-gray-400 text-center mb-6">
          {isAr
            ? 'سجل الدخول بحساب تليجرام الحقيقي للوصول إلى محادثاتك ورسائلك السحابية'
            : 'Log in with your authentic Telegram account to access your cloud messages'}
        </p>

        {/* Method Switcher: Phone vs QR */}
        <div className="w-full flex p-1 rounded-2xl bg-[#0e1621] border border-gray-800/80 mb-6">
          <button
            type="button"
            onClick={() => {
              setMethod('phone');
              setError(null);
            }}
            className={`flex-1 py-2.5 rounded-xl text-xs font-bold transition flex items-center justify-center gap-2 ${
              method === 'phone'
                ? 'bg-[#3390ec] text-white shadow-md'
                : 'text-gray-400 hover:text-white'
            }`}
          >
            <Phone className="w-4 h-4" />
            <span>{isAr ? 'رقم الهاتف' : 'Phone Number'}</span>
          </button>
          <button
            type="button"
            onClick={() => {
              setMethod('qr');
              setError(null);
            }}
            className={`flex-1 py-2.5 rounded-xl text-xs font-bold transition flex items-center justify-center gap-2 ${
              method === 'qr'
                ? 'bg-[#3390ec] text-white shadow-md'
                : 'text-gray-400 hover:text-white'
            }`}
          >
            <QrCode className="w-4 h-4" />
            <span>{isAr ? 'رمز الاستجابة (QR)' : 'QR Code'}</span>
          </button>
        </div>

        {/* Error Notification Banner */}
        {error && (
          <div className="w-full mb-4 p-3.5 rounded-2xl bg-rose-500/15 border border-rose-500/30 text-rose-300 text-xs flex items-center gap-2.5 leading-relaxed">
            <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
            <span className="flex-1">{error}</span>
          </div>
        )}

        {method === 'phone' ? (
          <div className="w-full">
            {step === 'phone' && (
              <form onSubmit={handleSendCode} className="space-y-4">
                <div>
                  <label className="block text-xs font-medium text-gray-300 mb-1.5">
                    {isAr ? 'رقم الهاتف الدولي' : 'Country & Phone Number'}
                  </label>
                  <div className="relative">
                    <input
                      type="tel"
                      value={phoneNumber}
                      onChange={(e) => setPhoneNumber(e.target.value)}
                      placeholder="+966 50 000 0000"
                      autoFocus
                      required
                      dir="ltr"
                      className="w-full px-4 py-3 rounded-2xl bg-[#242f3d] border border-gray-700/60 text-white font-mono text-sm focus:border-[#3390ec] outline-hidden transition"
                    />
                  </div>
                  <p className="text-[11px] text-gray-400 mt-1.5 leading-relaxed">
                    {isAr
                      ? 'ستصلك رسالة تحتوي على كود التحقق في تطبيق تليجرام على أجهزتك الأخرى أو عبر SMS.'
                      : 'You will receive a code in your Telegram app on your other devices or via SMS.'}
                  </p>
                </div>

                <button
                  type="submit"
                  disabled={loading || !phoneNumber.trim()}
                  className="w-full py-3.5 rounded-2xl bg-[#3390ec] hover:bg-[#2b7ec9] text-white font-bold text-sm shadow-lg shadow-[#3390ec]/25 flex items-center justify-center gap-2 transition active:scale-98 disabled:opacity-50"
                >
                  {loading ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <span>{isAr ? 'إرسال الرمز' : 'Next'}</span>
                  )}
                </button>
              </form>
            )}

            {step === 'code' && (
              <form onSubmit={handleSignIn} className="space-y-4">
                <div className="text-center mb-2">
                  <span className="text-xs text-gray-400">
                    {isAr ? 'أدخل الرمز المرسل إلى' : 'Enter the code sent to'}{' '}
                    <strong className="text-white font-mono">{phoneNumber}</strong>
                  </span>
                </div>

                <div>
                  <input
                    type="text"
                    value={code}
                    onChange={(e) => setCode(e.target.value.replace(/[^0-9]/g, ''))}
                    placeholder="•••••"
                    autoFocus
                    required
                    maxLength={6}
                    dir="ltr"
                    className="w-full px-4 py-3 rounded-2xl bg-[#242f3d] border border-gray-700/60 text-white font-mono text-center tracking-widest text-lg focus:border-[#3390ec] outline-hidden transition"
                  />
                </div>

                <button
                  type="submit"
                  disabled={loading || !code.trim()}
                  className="w-full py-3.5 rounded-2xl bg-[#3390ec] hover:bg-[#2b7ec9] text-white font-bold text-sm shadow-lg shadow-[#3390ec]/25 flex items-center justify-center gap-2 transition active:scale-98 disabled:opacity-50"
                >
                  {loading ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <span>{isAr ? 'تأكيد الرمز' : 'Confirm'}</span>
                  )}
                </button>

                <button
                  type="button"
                  onClick={() => {
                    setStep('phone');
                    setError(null);
                  }}
                  className="w-full text-center text-xs text-gray-400 hover:text-white transition py-1"
                >
                  {isAr ? 'تغيير رقم الهاتف' : 'Change Phone Number'}
                </button>
              </form>
            )}

            {step === 'password' && (
              <form onSubmit={handlePasswordSubmit} className="space-y-4">
                <div className="text-center mb-2">
                  <KeyRound className="w-8 h-8 text-[#3390ec] mx-auto mb-2" />
                  <span className="text-xs text-gray-400">
                    {isAr
                      ? 'الحساب محمي بكلمة مرور التحقق بخطوتين (2FA)'
                      : 'Enter your 2-Step Verification Password'}
                  </span>
                </div>

                <div>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder={isAr ? 'كلمة المرور' : 'Password'}
                    autoFocus
                    required
                    className="w-full px-4 py-3 rounded-2xl bg-[#242f3d] border border-gray-700/60 text-white text-center text-sm focus:border-[#3390ec] outline-hidden transition"
                  />
                </div>

                <button
                  type="submit"
                  disabled={loading || !password}
                  className="w-full py-3.5 rounded-2xl bg-[#3390ec] hover:bg-[#2b7ec9] text-white font-bold text-sm shadow-lg shadow-[#3390ec]/25 flex items-center justify-center gap-2 transition active:scale-98 disabled:opacity-50"
                >
                  {loading ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <span>{isAr ? 'تسجيل الدخول' : 'Sign In'}</span>
                  )}
                </button>

                <button
                  type="button"
                  onClick={() => {
                    setStep('code');
                    setError(null);
                  }}
                  className="w-full text-center text-xs text-gray-400 hover:text-white transition py-1"
                >
                  {isAr ? 'العودة للرمز' : 'Back to Code'}
                </button>
              </form>
            )}
          </div>
        ) : (
          <div className="w-full flex flex-col items-center py-2">
            <div className="p-4 bg-white rounded-2xl shadow-xl relative group flex items-center justify-center min-w-[200px] min-h-[200px]">
              {qrLoading ? (
                <div className="flex flex-col items-center gap-2 text-slate-800">
                  <Loader2 className="w-8 h-8 text-[#3390ec] animate-spin" />
                  <span className="text-xs font-semibold">
                    {isAr ? 'جارٍ توليد الرمز...' : 'Generating QR...'}
                  </span>
                </div>
              ) : qrExpired ? (
                <div className="flex flex-col items-center gap-2 text-slate-800 text-center p-2">
                  <AlertCircle className="w-8 h-8 text-amber-500" />
                  <span className="text-xs font-bold">
                    {isAr ? 'انتهت صلاحية الرمز' : 'QR Expired'}
                  </span>
                  <button
                    type="button"
                    onClick={() => loadQrToken()}
                    className="px-3 py-1.5 bg-[#3390ec] text-white text-xs font-bold rounded-lg flex items-center gap-1 mt-1"
                  >
                    <RefreshCw className="w-3.5 h-3.5" />
                    <span>{isAr ? 'تحديث' : 'Refresh'}</span>
                  </button>
                </div>
              ) : qrUrl ? (
                <img
                  src={`https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(
                    qrUrl
                  )}`}
                  alt="Telegram Login QR Code"
                  className="w-44 h-44 rounded-lg"
                />
              ) : (
                <button
                  type="button"
                  onClick={() => loadQrToken()}
                  className="px-4 py-2 bg-[#3390ec] text-white text-xs font-bold rounded-xl flex items-center gap-1.5"
                >
                  <RefreshCw className="w-4 h-4" />
                  <span>{isAr ? 'إنشاء رمز QR' : 'Generate QR'}</span>
                </button>
              )}
            </div>

            <p className="text-xs text-gray-400 text-center mt-4 leading-relaxed max-w-xs">
              {isAr
                ? 'افتح تطبيق تليجرام في هاتفك > الإعدادات > الأجهزة > ربط جهاز، ووجّه الكاميرا لمسح هذا الرمز.'
                : 'Open Telegram on your phone > Settings > Devices > Link Desktop Device, then scan this QR code.'}
            </p>

            <div className="mt-3 flex items-center gap-1.5 text-xs text-emerald-400">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>{isAr ? 'جاهز للربط الفوري' : 'Ready for instant link'}</span>
            </div>
          </div>
        )}

        {/* Security Footer Note */}
        <div className="w-full mt-6 pt-4 border-t border-gray-800/80 flex items-center justify-center gap-1.5 text-[11px] text-gray-400">
          <ShieldCheck className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
          <span>{isAr ? 'جلسة حقيقية مشفرة ببروتوكول MTProto v2.0' : 'Authentic session secured with MTProto v2.0'}</span>
        </div>
      </div>
    </div>
  );
};
