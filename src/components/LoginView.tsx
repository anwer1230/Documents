import React, { useState } from 'react';
import {
  Send,
  ShieldCheck,
  Key,
  Bot,
  Sparkles,
  Smartphone,
  Lock,
  AlertCircle,
  Loader2,
  ArrowRight,
  QrCode,
  Search,
  Check,
  Edit2,
  Eye,
  EyeOff,
  Globe,
} from 'lucide-react';
import { TelegramUser } from '../types';

interface LoginViewProps {
  onLoginSuccess: (user: TelegramUser, isDemo?: boolean, sessionToken?: string) => void;
  lang: 'ar' | 'en';
}

const COUNTRY_CODES = [
  { code: '+966', name: 'المملكة العربية السعودية', nameEn: 'Saudi Arabia', flag: '🇸🇦' },
  { code: '+971', name: 'الإمارات العربية المتحدة', nameEn: 'United Arab Emirates', flag: '🇦🇪' },
  { code: '+20', name: 'جمهورية مصر العربية', nameEn: 'Egypt', flag: '🇪🇬' },
  { code: '+964', name: 'العراق', nameEn: 'Iraq', flag: '🇮🇶' },
  { code: '+965', name: 'الكويت', nameEn: 'Kuwait', flag: '🇰🇼' },
  { code: '+974', name: 'قطر', nameEn: 'Qatar', flag: '🇶🇦' },
  { code: '+968', name: 'عُمان', nameEn: 'Oman', flag: '🇴🇲' },
  { code: '+973', name: 'البحرين', nameEn: 'Bahrain', flag: '🇧🇭' },
  { code: '+962', name: 'الأردن', nameEn: 'Jordan', flag: '🇯🇴' },
  { code: '+961', name: 'لبنان', nameEn: 'Lebanon', flag: '🇱🇧' },
  { code: '+212', name: 'المغرب', nameEn: 'Morocco', flag: '🇲🇦' },
  { code: '+213', name: 'الجزائر', nameEn: 'Algeria', flag: '🇩🇿' },
  { code: '+216', name: 'تونس', nameEn: 'Tunisia', flag: '🇹🇳' },
  { code: '+967', name: 'اليمن', nameEn: 'Yemen', flag: '🇾🇪' },
  { code: '+963', name: 'سوريا', nameEn: 'Syria', flag: '🇸🇾' },
  { code: '+970', name: 'فلسطين', nameEn: 'Palestine', flag: '🇵🇸' },
  { code: '+249', name: 'السودان', nameEn: 'Sudan', flag: '🇸🇩' },
  { code: '+218', name: 'ليبيا', nameEn: 'Libya', flag: '🇱🇾' },
  { code: '+1', name: 'الولايات المتحدة / كندا', nameEn: 'USA / Canada', flag: '🇺🇸' },
  { code: '+44', name: 'المملكة المتحدة', nameEn: 'United Kingdom', flag: '🇬🇧' },
  { code: '+49', name: 'ألمانيا', nameEn: 'Germany', flag: '🇩🇪' },
  { code: '+33', name: 'فرنسا', nameEn: 'France', flag: '🇫🇷' },
  { code: '+90', name: 'تركيا', nameEn: 'Turkey', flag: '🇹🇷' },
  { code: '+7', name: 'روسيا', nameEn: 'Russia', flag: '🇷🇺' },
];

export const LoginView: React.FC<LoginViewProps> = ({ onLoginSuccess, lang }) => {
  const isAr = lang === 'ar';
  // Web K default is 'qr', with toggle to 'phone', 'bot', and 'demo'
  const [loginMode, setLoginMode] = useState<'qr' | 'phone' | 'bot' | 'demo'>('qr');
  
  // Phone flow state
  const [countryCode, setCountryCode] = useState('+966');
  const [phoneNational, setPhoneNational] = useState('');
  const [step, setStep] = useState<'phone' | 'code' | 'password'>('phone');
  const [verificationCode, setVerificationCode] = useState('');
  const [phoneCodeHash, setPhoneCodeHash] = useState('');
  const [password2FA, setPassword2FA] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isCountryModalOpen, setIsCountryModalOpen] = useState(false);
  const [countrySearch, setCountrySearch] = useState('');
  
  // Bot Token state
  const [botToken, setBotToken] = useState('');

  // UI state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [keepSignedIn, setKeepSignedIn] = useState(true);
  const [sessionToken, setSessionToken] = useState<string>(() => {
    return (
      localStorage.getItem('tg_active_session_token') ||
      'user_session_' + Math.random().toString(36).substring(2, 12)
    );
  });

  const fullPhoneNumber = `${countryCode}${phoneNational.replace(/^0+/, '')}`;
  const currentCountry = COUNTRY_CODES.find((c) => c.code === countryCode) || COUNTRY_CODES[0];

  const filteredCountries = COUNTRY_CODES.filter((c) => {
    const q = countrySearch.toLowerCase();
    return (
      c.name.toLowerCase().includes(q) ||
      c.nameEn.toLowerCase().includes(q) ||
      c.code.includes(q)
    );
  });

  // Step 1: Send verification code via MTProto
  const handleSendCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!phoneNational.trim()) {
      setError(isAr ? 'يرجى إدخال رقم الهاتف' : 'Please enter your phone number');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const res = await fetch('/api/telegram/send-code', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-session-token': sessionToken,
        },
        credentials: 'include',
        body: JSON.stringify({
          phoneNumber: fullPhoneNumber,
          sessionToken,
        }),
      });
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || data.details || 'فشل إرسال كود التحقق');
      }

      if (data.sessionToken) {
        setSessionToken(data.sessionToken);
        localStorage.setItem('tg_active_session_token', data.sessionToken);
      }

      setPhoneCodeHash(data.phoneCodeHash);
      setStep('code');
    } catch (err: any) {
      console.error('Send code error:', err);
      setError(err.message || 'حدث خطأ أثناء الاتصال بخوادم تليجرام');
    } finally {
      setLoading(false);
    }
  };

  // Step 2: Verify Code
  const handleVerifyCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!verificationCode.trim()) {
      setError(isAr ? 'يرجى إدخال كود التحقق' : 'Please enter the verification code');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const res = await fetch('/api/telegram/sign-in', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-session-token': sessionToken,
        },
        credentials: 'include',
        body: JSON.stringify({
          phoneCode: verificationCode.trim(),
          phoneCodeHash,
          phoneNumber: fullPhoneNumber,
          sessionToken,
        }),
      });
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || data.details || 'فشل التحقق من الكود');
      }

      const effectiveToken = data.sessionToken || sessionToken;
      localStorage.setItem('tg_active_session_token', effectiveToken);

      if (data.needs2FA) {
        setStep('password');
        return;
      }

      if (data.user) {
        const u: TelegramUser = {
          id: data.user.id || 'me',
          firstName: data.user.firstName || 'مستخدم',
          lastName: data.user.lastName,
          username: data.user.username,
          phone: fullPhoneNumber,
          status: 'online',
        };
        onLoginSuccess(u, false, effectiveToken);
      }
    } catch (err: any) {
      console.error('Sign in error:', err);
      setError(err.message || 'كود التحقق غير صحيح أو انتهت صلاحيته');
    } finally {
      setLoading(false);
    }
  };

  // Step 3: 2FA Password
  const handleVerifyPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!password2FA.trim()) {
      setError(isAr ? 'يرجى إدخال كلمة المرور' : 'Please enter your password');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const res = await fetch('/api/telegram/sign-in-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-session-token': sessionToken,
        },
        credentials: 'include',
        body: JSON.stringify({
          password: password2FA,
          sessionToken,
        }),
      });
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || 'كلمة المرور غير صحيحة');
      }

      const effectiveToken = data.sessionToken || sessionToken;
      localStorage.setItem('tg_active_session_token', effectiveToken);

      if (data.user) {
        const u: TelegramUser = {
          id: data.user.id || 'me',
          firstName: data.user.firstName || 'مستخدم',
          lastName: data.user.lastName,
          username: data.user.username,
          phone: fullPhoneNumber,
          status: 'online',
        };
        onLoginSuccess(u, false, effectiveToken);
      }
    } catch (err: any) {
      setError(err.message || 'كلمة المرور غير صحيحة');
    } finally {
      setLoading(false);
    }
  };

  // Bot Login
  const handleBotLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!botToken.trim()) {
      setError(isAr ? 'يرجى إدخال رمز البوت' : 'Please enter the bot token');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const res = await fetch('/api/telegram/bot-login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-session-token': sessionToken,
        },
        credentials: 'include',
        body: JSON.stringify({
          botToken: botToken.trim(),
          sessionToken,
        }),
      });
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || 'فشل تسجيل الدخول برمز البوت');
      }

      const effectiveToken = data.sessionToken || sessionToken;
      localStorage.setItem('tg_active_session_token', effectiveToken);

      const u: TelegramUser = {
        id: data.user?.id || 'bot_user',
        firstName: data.user?.firstName || 'Telegram Bot',
        username: data.user?.username || 'bot',
        isBot: true,
        status: 'online',
      };
      onLoginSuccess(u, false, effectiveToken);
    } catch (err: any) {
      setError(err.message || 'رمز البوت غير صالح');
    } finally {
      setLoading(false);
    }
  };

  // Demo Login (Instant workspace access)
  const handleDemoAccess = () => {
    const demoUser: TelegramUser = {
      id: 'demo_user',
      firstName: 'المستخدم',
      lastName: 'التجريبي',
      username: 'telegram_user',
      phone: '+966 50 123 4567',
      photoUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80',
      bio: 'مرحباً! أستخدم تليجرام ويب للتواصل والعمل.',
      status: 'online',
    };
    onLoginSuccess(demoUser, true);
  };

  return (
    <div
      className="min-h-screen bg-[#0e1621] text-white flex flex-col items-center justify-center p-4 selection:bg-[#3390ec] selection:text-white"
      dir={isAr ? 'rtl' : 'ltr'}
    >
      <div className="w-full max-w-[420px] bg-[#17212b] rounded-3xl p-7 sm:p-8 border border-[#232e3c] shadow-2xl relative overflow-hidden transition-all">
        
        {/* Telegram Web K Signature Top Navigation Pills */}
        <div className="flex bg-[#0e1621] p-1 rounded-2xl mb-6 text-xs font-semibold select-none border border-[#232e3c]">
          <button
            type="button"
            onClick={() => { setLoginMode('qr'); setError(null); }}
            className={`flex-1 py-2 rounded-xl transition-all flex items-center justify-center gap-1.5 ${
              loginMode === 'qr' ? 'bg-[#3390ec] text-white shadow-md' : 'text-gray-400 hover:text-white'
            }`}
          >
            <QrCode className="w-3.5 h-3.5" />
            <span>{isAr ? 'رمز QR' : 'QR Code'}</span>
          </button>
          
          <button
            type="button"
            onClick={() => { setLoginMode('phone'); setStep('phone'); setError(null); }}
            className={`flex-1 py-2 rounded-xl transition-all flex items-center justify-center gap-1.5 ${
              loginMode === 'phone' ? 'bg-[#3390ec] text-white shadow-md' : 'text-gray-400 hover:text-white'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            <span>{isAr ? 'رقم الهاتف' : 'Phone'}</span>
          </button>

          <button
            type="button"
            onClick={() => { setLoginMode('bot'); setError(null); }}
            className={`flex-1 py-2 rounded-xl transition-all flex items-center justify-center gap-1.5 ${
              loginMode === 'bot' ? 'bg-[#3390ec] text-white shadow-md' : 'text-gray-400 hover:text-white'
            }`}
          >
            <Bot className="w-3.5 h-3.5" />
            <span>{isAr ? 'بوت' : 'Bot'}</span>
          </button>

          <button
            type="button"
            onClick={() => { setLoginMode('demo'); setError(null); }}
            className={`flex-1 py-2 rounded-xl transition-all flex items-center justify-center gap-1.5 ${
              loginMode === 'demo' ? 'bg-[#3390ec] text-white shadow-md' : 'text-gray-400 hover:text-white'
            }`}
          >
            <Sparkles className="w-3.5 h-3.5 text-amber-300" />
            <span>{isAr ? 'تجريبي' : 'Demo'}</span>
          </button>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="mb-4 p-3 bg-red-900/40 border border-red-500/40 rounded-xl text-red-200 text-xs flex items-start gap-2 animate-shake">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-red-400" />
            <div className="flex-1">{error}</div>
          </div>
        )}

        {/* MODE 1: TELEGRAM WEB K QR CODE LOGIN */}
        {loginMode === 'qr' && (
          <div className="flex flex-col items-center text-center space-y-5">
            <div className="relative group cursor-pointer" onClick={handleDemoAccess} title={isAr ? 'انقر لتسجيل الدخول السريع برمز QR' : 'Click to quickly confirm QR login'}>
              {/* QR Code Container */}
              <div className="w-56 h-56 bg-white p-3.5 rounded-2xl shadow-xl flex items-center justify-center relative overflow-hidden">
                {/* SVG QR Code Pattern */}
                <svg viewBox="0 0 100 100" className="w-full h-full text-gray-900 fill-current">
                  <path d="M0 0h30v30H0zm5 5h20v20H5zm5 5h10v10H10zM70 0h30v30H70zm5 5h20v20H75zm5 5h10v10H80zM0 70h30v30H0zm5 5h20v20H5zm5 5h10v10H10zM35 5h5v5h-5zM45 5h10v5H45zm15 0h5v5h-5zM35 15h5v5h-5zm10 0h5v5h-5zm10 0h5v10h-5zm0 15h5v5h-5zm-20-5h5v10h-5zm-5 5h5v5h-5zm15 5h5v5h-5zm5 5h5v5h-5zm5-10h5v5h-5zm10 5h5v5h-5zm-30 15h5v5h-5zm10 0h5v10h-5zm10 0h10v5h-10zm-20 10h5v5h-5zm20 5h5v5h-5zm-15 10h5v5h-5zm10 0h10v5h-10zm25-30h5v5h-5zm5-5h5v5h-5zm5 10h5v10h-5zm-5 15h5v5h-5zm5 5h5v5h-5zm-10 5h5v5h-5zm10 5h5v5h-5zm-5 10h5v5h-5zM40 75h5v5h-5zm-5 10h5v5h-5zm10 5h5v5h-5zm-5 5h10v5H40zm25-5h5v5h-5zm-5-10h5v5h-5z" />
                </svg>

                {/* Telegram Logo in Center of QR */}
                <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                  <div className="w-12 h-12 bg-[#3390ec] rounded-full flex items-center justify-center shadow-lg border-2 border-white">
                    <Send className="w-6 h-6 text-white fill-white translate-x-[-1px] translate-y-[-1px]" />
                  </div>
                </div>
              </div>

              {/* Hover overlay hint */}
              <div className="absolute inset-0 bg-[#3390ec]/85 rounded-2xl flex flex-col items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity p-4 text-white">
                <Sparkles className="w-8 h-8 text-amber-300 mb-1 animate-pulse" />
                <span className="font-bold text-xs">{isAr ? 'تأكيد تسجيل الدخول بالرمز فوراً' : 'Scan & Enter Instantly'}</span>
              </div>
            </div>

            <div>
              <h2 className="text-xl font-bold text-white mb-2">
                {isAr ? 'تسجيل الدخول بواسطة رمز QR' : 'Log in to Telegram by QR Code'}
              </h2>
              <ol className="text-xs text-gray-400 space-y-2 text-start max-w-xs mx-auto">
                <li className="flex items-center gap-2">
                  <span className="w-5 h-5 rounded-full bg-[#242f3d] text-[#3390ec] flex items-center justify-center font-bold text-[11px] shrink-0">1</span>
                  <span>{isAr ? 'افتح تطبيق تليجرام على هاتفك' : 'Open Telegram on your phone'}</span>
                </li>
                <li className="flex items-center gap-2">
                  <span className="w-5 h-5 rounded-full bg-[#242f3d] text-[#3390ec] flex items-center justify-center font-bold text-[11px] shrink-0">2</span>
                  <span>{isAr ? 'انتقل إلى الإعدادات > الأجهزة > ربط جهاز' : 'Go to Settings > Devices > Link Desktop'}</span>
                </li>
                <li className="flex items-center gap-2">
                  <span className="w-5 h-5 rounded-full bg-[#242f3d] text-[#3390ec] flex items-center justify-center font-bold text-[11px] shrink-0">3</span>
                  <span>{isAr ? 'وجّه كاميرا هاتفك نحو هذه الشاشة للتأكيد' : 'Point your phone at this screen to confirm'}</span>
                </li>
              </ol>
            </div>

            <button
              type="button"
              onClick={() => { setLoginMode('phone'); setStep('phone'); }}
              className="w-full py-3 px-4 rounded-xl text-[#3390ec] hover:bg-[#3390ec]/10 font-bold text-xs uppercase tracking-wider transition border border-[#3390ec]/30"
            >
              {isAr ? 'تسجيل الدخول برقم الهاتف' : 'LOG IN BY PHONE NUMBER'}
            </button>
          </div>
        )}

        {/* MODE 2: PHONE NUMBER LOGIN */}
        {loginMode === 'phone' && (
          <div>
            <div className="flex flex-col items-center text-center mb-6">
              <div className="w-16 h-16 bg-[#3390ec] rounded-full flex items-center justify-center shadow-lg shadow-[#3390ec]/30 mb-3">
                <Send className="w-8 h-8 text-white fill-white translate-x-[-1px] translate-y-[-1px]" />
              </div>
              <h2 className="text-xl font-bold text-white tracking-wide">
                {step === 'phone'
                  ? (isAr ? 'رقم هاتفك' : 'Your Phone Number')
                  : step === 'code'
                  ? (isAr ? 'رمز التأكيد' : 'Confirmation Code')
                  : (isAr ? 'كلمة المرور السحابية' : '2FA Password')}
              </h2>
              <p className="text-xs text-gray-400 mt-1 max-w-xs">
                {step === 'phone'
                  ? (isAr ? 'يرجى تأكيد رمز بلدك وإدخال رقم هاتفك للتسجيل عبر MTProto.' : 'Please confirm your country code and enter your phone number.')
                  : step === 'code'
                  ? (isAr ? `أرسلنا رمز التحقق المكون من 5 أرقام إلى جهازك.` : `We've sent the code to your Telegram app.`)
                  : (isAr ? 'حسابك محمي بكلمة مرور إضافية.' : 'Your account is protected with Two-Step Verification.')}
              </p>
            </div>

            {step === 'phone' && (
              <form onSubmit={handleSendCode} className="space-y-4">
                {/* Country selector button that opens searchable picker */}
                <div>
                  <label className="block text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                    {isAr ? 'الدولة' : 'Country'}
                  </label>
                  <button
                    type="button"
                    onClick={() => setIsCountryModalOpen(true)}
                    className="w-full bg-[#242f3d] hover:bg-[#2b394a] text-white text-sm rounded-xl px-4 py-3 border border-[#2f3f50] flex items-center justify-between transition text-start"
                  >
                    <span className="flex items-center gap-2 truncate">
                      <span className="text-base">{currentCountry.flag}</span>
                      <span className="font-semibold text-sm truncate">{isAr ? currentCountry.name : currentCountry.nameEn}</span>
                    </span>
                    <span className="text-gray-400 font-mono text-xs ps-2">{currentCountry.code}</span>
                  </button>
                </div>

                {/* Phone Input with Country Code */}
                <div>
                  <label className="block text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                    {isAr ? 'رقم الهاتف' : 'Phone Number'}
                  </label>
                  <div className="flex items-center gap-2">
                    <span
                      onClick={() => setIsCountryModalOpen(true)}
                      className="bg-[#242f3d] hover:bg-[#2b394a] cursor-pointer text-gray-300 px-3 py-3 rounded-xl text-sm border border-[#2f3f50] font-mono select-none"
                      dir="ltr"
                    >
                      {countryCode}
                    </span>
                    <input
                      type="tel"
                      dir="ltr"
                      placeholder="50 123 4567"
                      value={phoneNational}
                      onChange={(e) => setPhoneNational(e.target.value)}
                      className="flex-1 bg-[#242f3d] text-white text-sm rounded-xl px-4 py-3 border border-[#2f3f50] focus:outline-none focus:border-[#3390ec] transition tracking-wider font-mono"
                      autoFocus
                    />
                  </div>
                </div>

                <div className="flex items-center gap-2 pt-1">
                  <input
                    type="checkbox"
                    id="keepSignedIn"
                    checked={keepSignedIn}
                    onChange={(e) => setKeepSignedIn(e.target.checked)}
                    className="rounded bg-[#242f3d] border-[#2f3f50] text-[#3390ec] focus:ring-0 w-4 h-4 cursor-pointer"
                  />
                  <label htmlFor="keepSignedIn" className="text-xs text-gray-300 cursor-pointer select-none">
                    {isAr ? 'البقاء قيد تسجيل الدخول' : 'Keep me signed in'}
                  </label>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full bg-[#3390ec] hover:bg-[#2881da] text-white font-bold py-3 px-4 rounded-xl transition duration-200 flex items-center justify-center gap-2 shadow-lg shadow-[#3390ec]/25 disabled:opacity-50 text-xs uppercase tracking-wider"
                >
                  {loading ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      <span>{isAr ? 'جارٍ الاتصال...' : 'Connecting...'}</span>
                    </>
                  ) : (
                    <>
                      <span>{isAr ? 'التالي' : 'NEXT'}</span>
                      <ArrowRight className={`w-4 h-4 ${isAr ? 'rotate-180' : ''}`} />
                    </>
                  )}
                </button>

                <button
                  type="button"
                  onClick={() => setLoginMode('qr')}
                  className="w-full text-center text-xs text-[#3390ec] hover:underline font-semibold py-1.5 transition"
                >
                  {isAr ? 'تسجيل الدخول بواسطة رمز QR' : 'LOG IN BY QR CODE'}
                </button>
              </form>
            )}

            {step === 'code' && (
              <form onSubmit={handleVerifyCode} className="space-y-4">
                <div className="flex items-center justify-center gap-2 bg-[#242f3d]/60 py-2 px-3 rounded-xl mb-2 text-xs">
                  <span className="font-mono text-gray-200" dir="ltr">{fullPhoneNumber}</span>
                  <button
                    type="button"
                    onClick={() => { setStep('phone'); setVerificationCode(''); }}
                    className="text-[#3390ec] hover:text-white p-1"
                    title={isAr ? 'تعديل الرقم' : 'Edit number'}
                  >
                    <Edit2 className="w-3.5 h-3.5" />
                  </button>
                </div>

                <div>
                  <input
                    type="text"
                    inputMode="numeric"
                    placeholder="•••••"
                    value={verificationCode}
                    onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    className="w-full bg-[#242f3d] text-center text-2xl tracking-[0.5em] font-mono text-white rounded-xl px-4 py-3.5 border border-[#2f3f50] focus:outline-none focus:border-[#3390ec] transition"
                    autoFocus
                  />
                </div>

                <button
                  type="submit"
                  disabled={loading || verificationCode.length < 4}
                  className="w-full bg-[#3390ec] hover:bg-[#2881da] text-white font-bold py-3 px-4 rounded-xl transition flex items-center justify-center gap-2 shadow-lg shadow-[#3390ec]/25 disabled:opacity-50 text-xs uppercase tracking-wider"
                >
                  {loading ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      <span>{isAr ? 'جارٍ التحقق...' : 'Verifying...'}</span>
                    </>
                  ) : (
                    <span>{isAr ? 'تأكيد الرمز' : 'VERIFY CODE'}</span>
                  )}
                </button>

                <button
                  type="button"
                  onClick={() => { setStep('phone'); setVerificationCode(''); }}
                  className="w-full text-xs text-gray-400 hover:text-white py-1 transition text-center"
                >
                  {isAr ? 'تغيير رقم الهاتف' : 'Change phone number'}
                </button>
              </form>
            )}

            {step === 'password' && (
              <form onSubmit={handleVerifyPassword} className="space-y-4">
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    placeholder={isAr ? 'أدخل كلمة مرور التحقق' : 'Enter 2FA Password'}
                    value={password2FA}
                    onChange={(e) => setPassword2FA(e.target.value)}
                    className="w-full bg-[#242f3d] text-white text-sm rounded-xl px-4 py-3 border border-[#2f3f50] focus:outline-none focus:border-[#3390ec] transition pe-10"
                    autoFocus
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute end-3 top-3.5 text-gray-400 hover:text-white"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>

                <button
                  type="submit"
                  disabled={loading || !password2FA.trim()}
                  className="w-full bg-[#3390ec] hover:bg-[#2881da] text-white font-bold py-3 px-4 rounded-xl transition flex items-center justify-center gap-2 shadow-lg shadow-[#3390ec]/25 disabled:opacity-50 text-xs uppercase tracking-wider"
                >
                  {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <span>{isAr ? 'دخول' : 'SIGN IN'}</span>}
                </button>
              </form>
            )}
          </div>
        )}

        {/* MODE 3: BOT TOKEN LOGIN */}
        {loginMode === 'bot' && (
          <form onSubmit={handleBotLogin} className="space-y-4">
            <div className="text-center mb-4">
              <div className="w-16 h-16 bg-[#3390ec]/20 text-[#3390ec] rounded-full flex items-center justify-center mx-auto mb-2">
                <Bot className="w-8 h-8" />
              </div>
              <h3 className="font-bold text-white text-lg">{isAr ? 'تسجيل الدخول برمز البوت' : 'Log In with Bot Token'}</h3>
              <p className="text-xs text-gray-400 mt-1">
                {isAr ? 'أدخل رمز التوكن الخاص ببوتك من @BotFather' : 'Enter your bot token obtained from @BotFather'}
              </p>
            </div>

            <div>
              <div className="relative">
                <input
                  type="password"
                  dir="ltr"
                  placeholder="123456789:ABCdefGHIjklmn..."
                  value={botToken}
                  onChange={(e) => setBotToken(e.target.value)}
                  className="w-full bg-[#242f3d] text-white text-sm rounded-xl px-4 py-3 pe-10 border border-[#2f3f50] focus:outline-none focus:border-[#3390ec] font-mono transition"
                  autoFocus
                />
                <Key className="w-4 h-4 text-gray-400 absolute end-3 top-3.5 pointer-events-none" />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading || !botToken.trim()}
              className="w-full bg-[#3390ec] hover:bg-[#2881da] text-white font-bold py-3 px-4 rounded-xl transition flex items-center justify-center gap-2 shadow-lg shadow-[#3390ec]/25 disabled:opacity-50 text-xs uppercase tracking-wider"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>{isAr ? 'جارٍ الاتصال...' : 'Connecting...'}</span>
                </>
              ) : (
                <span>{isAr ? 'دخول كبوت' : 'LOG IN AS BOT'}</span>
              )}
            </button>
          </form>
        )}

        {/* MODE 4: QUICK DEMO */}
        {loginMode === 'demo' && (
          <div className="space-y-4 text-center">
            <div className="w-16 h-16 bg-gradient-to-tr from-[#3390ec] to-[#00b4d8] text-white rounded-full flex items-center justify-center mx-auto mb-2 shadow-lg">
              <Sparkles className="w-8 h-8" />
            </div>
            <h3 className="font-bold text-white text-lg">{isAr ? 'معاينة تليجرام ويب الكاملة' : 'Telegram Web Live Demo'}</h3>
            <p className="text-xs text-gray-300 leading-relaxed max-w-xs mx-auto">
              {isAr
                ? 'استكشف واجهة تليجرام ويب الرسمية K مع القنوات، البوتات التفاعلية، الرسائل الصوتية، تطبيقات الويب المصغرة، والرسائل المحفوظة فوراً.'
                : 'Explore full Telegram Web K interface with channels, interactive bots, voice notes, and mini apps instantly.'}
            </p>

            <button
              type="button"
              onClick={handleDemoAccess}
              className="w-full bg-[#3390ec] hover:bg-[#2881da] active:scale-98 text-white font-bold py-3 px-4 rounded-xl transition flex items-center justify-center gap-2 shadow-lg shadow-[#3390ec]/25 text-xs uppercase tracking-wider"
            >
              <Sparkles className="w-4 h-4 text-amber-300" />
              <span>{isAr ? 'دخول فوري لتليجرام ويب' : 'LAUNCH TELEGRAM WEB'}</span>
            </button>
          </div>
        )}

        {/* Footer info: fixed MTProto configuration */}
        <div className="mt-7 pt-4 border-t border-[#232e3c] text-center text-[11px] text-gray-500 flex items-center justify-center gap-2 select-none">
          <span className="text-gray-400">Telegram Web K</span>
          <span>•</span>
          <span>Layer 198</span>
          <span>•</span>
          <span className="text-[#3390ec] font-mono">API 22043994</span>
        </div>
      </div>

      {/* Country Selection Modal */}
      {isCountryModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs select-none">
          <div className="w-full max-w-md bg-[#17212b] rounded-2xl border border-[#232e3c] shadow-2xl flex flex-col max-h-[80vh] overflow-hidden">
            {/* Modal Header */}
            <div className="p-4 border-b border-[#232e3c] flex items-center justify-between">
              <h3 className="font-bold text-base text-white">{isAr ? 'اختر الدولة' : 'Select Country'}</h3>
              <button
                type="button"
                onClick={() => setIsCountryModalOpen(false)}
                className="text-gray-400 hover:text-white text-xs font-semibold px-2 py-1 rounded"
              >
                {isAr ? 'إغلاق' : 'Close'}
              </button>
            </div>

            {/* Country Search Bar */}
            <div className="p-3 border-b border-[#232e3c]">
              <div className="relative">
                <input
                  type="text"
                  placeholder={isAr ? 'ابحث عن اسم الدولة أو الرمز...' : 'Search country or code...'}
                  value={countrySearch}
                  onChange={(e) => setCountrySearch(e.target.value)}
                  className="w-full bg-[#242f3d] text-white text-xs rounded-xl px-3 py-2.5 ps-9 border border-[#2f3f50] focus:outline-none focus:border-[#3390ec]"
                  autoFocus
                />
                <Search className="w-4 h-4 text-gray-400 absolute start-3 top-2.5 pointer-events-none" />
              </div>
            </div>

            {/* Country List */}
            <div className="flex-1 overflow-y-auto p-2 divide-y divide-[#232e3c]/50">
              {filteredCountries.map((c) => {
                const isSelected = c.code === countryCode;
                return (
                  <button
                    key={c.code + c.name}
                    type="button"
                    onClick={() => {
                      setCountryCode(c.code);
                      setIsCountryModalOpen(false);
                      setCountrySearch('');
                    }}
                    className={`w-full flex items-center justify-between p-3 rounded-xl transition text-start ${
                      isSelected ? 'bg-[#3390ec] text-white' : 'hover:bg-[#242f3d] text-gray-200'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <span className="text-xl">{c.flag}</span>
                      <div>
                        <div className="font-semibold text-xs">{isAr ? c.name : c.nameEn}</div>
                        <div className={`text-[11px] ${isSelected ? 'text-blue-100' : 'text-gray-400'}`}>
                          {isAr ? c.nameEn : c.name}
                        </div>
                      </div>
                    </div>
                    <span className="font-mono font-bold text-xs" dir="ltr">{c.code}</span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

