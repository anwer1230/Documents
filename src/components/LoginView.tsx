import React, { useState } from 'react';
import { Send, ShieldCheck, Key, Bot, Sparkles, Smartphone, Lock, AlertCircle, Loader2, ArrowRight } from 'lucide-react';
import { TelegramUser } from '../types';

interface LoginViewProps {
  onLoginSuccess: (user: TelegramUser, isDemo?: boolean) => void;
  lang: 'ar' | 'en';
}

const COUNTRY_CODES = [
  { code: '+966', name: 'المملكة العربية السعودية', flag: '🇸🇦' },
  { code: '+971', name: 'الإمارات العربية المتحدة', flag: '🇦🇪' },
  { code: '+20', name: 'جمهورية مصر العربية', flag: '🇪🇬' },
  { code: '+964', name: 'العراق', flag: '🇮🇶' },
  { code: '+965', name: 'الكويت', flag: '🇰🇼' },
  { code: '+974', name: 'قطر', flag: '🇶🇦' },
  { code: '+968', name: 'عُمان', flag: '🇴🇲' },
  { code: '+973', name: 'البحرين', flag: '🇧🇭' },
  { code: '+962', name: 'الأردن', flag: '🇯🇴' },
  { code: '+961', name: 'لبنان', flag: '🇱🇧' },
  { code: '+212', name: 'المغرب', flag: '🇲🇦' },
  { code: '+213', name: 'الجزائر', flag: '🇩🇿' },
  { code: '+216', name: 'تونس', flag: '🇹🇳' },
  { code: '+1', name: 'الولايات المتحدة / كندا', flag: '🇺🇸' },
  { code: '+44', name: 'المملكة المتحدة', flag: '🇬🇧' },
  { code: '+49', name: 'ألمانيا', flag: '🇩🇪' },
  { code: '+33', name: 'فرنسا', flag: '🇫🇷' },
  { code: '+90', name: 'تركيا', flag: '🇹🇷' },
];

export const LoginView: React.FC<LoginViewProps> = ({ onLoginSuccess, lang }) => {
  const isAr = lang === 'ar';
  const [activeTab, setActiveTab] = useState<'phone' | 'bot' | 'demo'>('phone');
  
  // Phone flow state
  const [countryCode, setCountryCode] = useState('+966');
  const [phoneNational, setPhoneNational] = useState('');
  const [step, setStep] = useState<'phone' | 'code' | 'password'>('phone');
  const [verificationCode, setVerificationCode] = useState('');
  const [phoneCodeHash, setPhoneCodeHash] = useState('');
  const [password2FA, setPassword2FA] = useState('');
  
  // Bot Token state
  const [botToken, setBotToken] = useState('');

  // UI state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [keepSignedIn, setKeepSignedIn] = useState(true);

  const fullPhoneNumber = `${countryCode}${phoneNational.replace(/^0+/, '')}`;

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
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phoneNumber: fullPhoneNumber }),
      });
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || data.details || 'فشل إرسال كود التحقق');
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
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          phoneCode: verificationCode.trim(),
          phoneCodeHash,
        }),
      });
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || data.details || 'فشل التحقق من الكود');
      }

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
        onLoginSuccess(u, false);
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
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password: password2FA }),
      });
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || 'كلمة المرور غير صحيحة');
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
        onLoginSuccess(u, false);
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
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ botToken: botToken.trim() }),
      });
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || 'فشل تسجيل الدخول برمز البوت');
      }

      const u: TelegramUser = {
        id: data.user?.id || 'bot_user',
        firstName: data.user?.firstName || 'Telegram Bot',
        username: data.user?.username || 'bot',
        isBot: true,
        status: 'online',
      };
      onLoginSuccess(u, false);
    } catch (err: any) {
      setError(err.message || 'رمز البوت غير صالح');
    } finally {
      setLoading(false);
    }
  };

  // Demo Login (One-click instant workspace)
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
    <div className="min-h-screen bg-[#0e1621] text-white flex flex-col items-center justify-center p-4 selection:bg-[#3390ec] selection:text-white" dir={isAr ? 'rtl' : 'ltr'}>
      <div className="w-full max-w-md bg-[#17212b] rounded-2xl p-8 border border-[#232e3c] shadow-2xl relative overflow-hidden">
        
        {/* Telegram Header Badge with Fixed API Credentials */}
        <div className="flex flex-col items-center text-center mb-6">
          <div className="w-20 h-20 bg-[#3390ec] rounded-full flex items-center justify-center shadow-lg shadow-[#3390ec]/30 mb-4 transition-transform hover:scale-105">
            <Send className="w-10 h-10 text-white fill-white translate-x-[-2px] translate-y-[-1px]" />
          </div>
          <h1 className="text-2xl font-bold text-white tracking-wide">Telegram Web</h1>
          <p className="text-sm text-gray-400 mt-1">
            {isAr ? 'تطبيق تليجرام ويب الرسمي المتصل بخوادم MTProto' : 'Official Telegram Web Client connected to MTProto'}
          </p>
          
          {/* Embedded API Credentials Badge */}
          <div className="mt-3 inline-flex items-center gap-1.5 px-3 py-1 bg-[#232e3c]/80 rounded-full text-xs text-blue-300 border border-[#3390ec]/20">
            <ShieldCheck className="w-3.5 h-3.5 text-[#3390ec]" />
            <span>API ID: <strong>22043994</strong> (مثبت في التطبيق)</span>
          </div>
        </div>

        {/* Tab Selection */}
        <div className="flex bg-[#0e1621] p-1 rounded-xl mb-6 text-sm">
          <button
            type="button"
            onClick={() => { setActiveTab('phone'); setStep('phone'); setError(null); }}
            className={`flex-1 py-2 rounded-lg font-medium transition-all flex items-center justify-center gap-1.5 ${
              activeTab === 'phone' ? 'bg-[#3390ec] text-white shadow' : 'text-gray-400 hover:text-white'
            }`}
          >
            <Smartphone className="w-4 h-4" />
            <span>{isAr ? 'رقم الهاتف' : 'Phone'}</span>
          </button>
          
          <button
            type="button"
            onClick={() => { setActiveTab('bot'); setError(null); }}
            className={`flex-1 py-2 rounded-lg font-medium transition-all flex items-center justify-center gap-1.5 ${
              activeTab === 'bot' ? 'bg-[#3390ec] text-white shadow' : 'text-gray-400 hover:text-white'
            }`}
          >
            <Bot className="w-4 h-4" />
            <span>{isAr ? 'رمز البوت' : 'Bot Token'}</span>
          </button>

          <button
            type="button"
            onClick={() => { setActiveTab('demo'); setError(null); }}
            className={`flex-1 py-2 rounded-lg font-medium transition-all flex items-center justify-center gap-1.5 ${
              activeTab === 'demo' ? 'bg-[#3390ec] text-white shadow' : 'text-gray-400 hover:text-white'
            }`}
          >
            <Sparkles className="w-4 h-4 text-amber-300" />
            <span>{isAr ? 'معاينة فورية' : 'Quick Demo'}</span>
          </button>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="mb-4 p-3 bg-red-900/40 border border-red-500/40 rounded-xl text-red-200 text-xs flex items-start gap-2 animate-shake">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-red-400" />
            <div className="flex-1">{error}</div>
          </div>
        )}

        {/* Phone Tab Flow */}
        {activeTab === 'phone' && (
          <>
            {step === 'phone' && (
              <form onSubmit={handleSendCode} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
                    {isAr ? 'الدولة / الرمز الدولي' : 'Country'}
                  </label>
                  <select
                    value={countryCode}
                    onChange={(e) => setCountryCode(e.target.value)}
                    className="w-full bg-[#242f3d] text-white text-sm rounded-xl px-4 py-3 border border-[#2f3f50] focus:outline-none focus:border-[#3390ec] transition"
                  >
                    {COUNTRY_CODES.map((c) => (
                      <option key={c.code + c.name} value={c.code}>
                        {c.flag} {c.name} ({c.code})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
                    {isAr ? 'رقم الهاتف' : 'Phone Number'}
                  </label>
                  <div className="flex items-center gap-2">
                    <span className="bg-[#242f3d] text-gray-300 px-3 py-3 rounded-xl text-sm border border-[#2f3f50] font-mono select-none" dir="ltr">
                      {countryCode}
                    </span>
                    <input
                      type="tel"
                      dir="ltr"
                      placeholder="50 123 4567"
                      value={phoneNational}
                      onChange={(e) => setPhoneNational(e.target.value)}
                      className="flex-1 bg-[#242f3d] text-white text-sm rounded-xl px-4 py-3 border border-[#2f3f50] focus:outline-none focus:border-[#3390ec] transition tracking-wider"
                      autoFocus
                    />
                  </div>
                  <p className="text-xs text-gray-500 mt-1.5">
                    {isAr ? 'سيصلك رمز تأكيد عبر تطبيق تليجرام أو رسالة نصية قصيرة.' : 'You will receive a confirmation code in your Telegram app or SMS.'}
                  </p>
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
                  className="w-full bg-[#3390ec] hover:bg-[#2b7ec9] text-white font-semibold py-3.5 px-4 rounded-xl transition duration-200 flex items-center justify-center gap-2 shadow-lg shadow-[#3390ec]/25 disabled:opacity-50"
                >
                  {loading ? (
                    <>
                      <Loader2 className="w-5 h-5 animate-spin" />
                      <span>{isAr ? 'جارٍ الاتصال بخوادم تيليجرام...' : 'Connecting to Telegram...'}</span>
                    </>
                  ) : (
                    <>
                      <span>{isAr ? 'التالي' : 'Next'}</span>
                      <ArrowRight className={`w-4 h-4 ${isAr ? 'rotate-180' : ''}`} />
                    </>
                  )}
                </button>
              </form>
            )}

            {step === 'code' && (
              <form onSubmit={handleVerifyCode} className="space-y-4">
                <div className="text-center mb-4">
                  <p className="text-sm text-gray-300 font-mono" dir="ltr">{fullPhoneNumber}</p>
                  <p className="text-xs text-gray-400 mt-1">
                    {isAr ? 'أدخل رمز التحقق المكون من 5 أرقام الذي وصلك في تطبيق تليجرام' : 'Enter the 5-digit code sent to your Telegram app'}
                  </p>
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
                  className="w-full bg-[#3390ec] hover:bg-[#2b7ec9] text-white font-semibold py-3.5 px-4 rounded-xl transition flex items-center justify-center gap-2 shadow-lg shadow-[#3390ec]/25 disabled:opacity-50"
                >
                  {loading ? (
                    <>
                      <Loader2 className="w-5 h-5 animate-spin" />
                      <span>{isAr ? 'جارٍ التحقق...' : 'Verifying...'}</span>
                    </>
                  ) : (
                    <span>{isAr ? 'تأكيد الرمز' : 'Verify Code'}</span>
                  )}
                </button>

                <button
                  type="button"
                  onClick={() => { setStep('phone'); setVerificationCode(''); }}
                  className="w-full text-xs text-gray-400 hover:text-white py-1 transition"
                >
                  {isAr ? 'تغيير رقم الهاتف' : 'Change phone number'}
                </button>
              </form>
            )}

            {step === 'password' && (
              <form onSubmit={handleVerifyPassword} className="space-y-4">
                <div className="text-center mb-3">
                  <div className="w-12 h-12 bg-amber-500/20 text-amber-400 rounded-full flex items-center justify-center mx-auto mb-2">
                    <Lock className="w-6 h-6" />
                  </div>
                  <h3 className="font-semibold text-white">{isAr ? 'التحقق بخطوتين (2FA)' : 'Two-Step Verification'}</h3>
                  <p className="text-xs text-gray-400 mt-1">
                    {isAr ? 'حسابك محمي بكلمة مرور سحابية، يرجى إدخالها لإتمام الدخول.' : 'Your account is protected by a cloud password.'}
                  </p>
                </div>

                <div>
                  <input
                    type="password"
                    placeholder={isAr ? 'أدخل كلمة مرور التحقق' : 'Enter 2FA Password'}
                    value={password2FA}
                    onChange={(e) => setPassword2FA(e.target.value)}
                    className="w-full bg-[#242f3d] text-white text-sm rounded-xl px-4 py-3 border border-[#2f3f50] focus:outline-none focus:border-[#3390ec] transition"
                    autoFocus
                  />
                </div>

                <button
                  type="submit"
                  disabled={loading || !password2FA.trim()}
                  className="w-full bg-[#3390ec] hover:bg-[#2b7ec9] text-white font-semibold py-3.5 px-4 rounded-xl transition flex items-center justify-center gap-2 shadow-lg shadow-[#3390ec]/25 disabled:opacity-50"
                >
                  {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : <span>{isAr ? 'دخول' : 'Sign In'}</span>}
                </button>
              </form>
            )}
          </>
        )}

        {/* Bot Token Tab */}
        {activeTab === 'bot' && (
          <form onSubmit={handleBotLogin} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
                {isAr ? 'رمز البوت (Bot Token)' : 'Bot Token'}
              </label>
              <div className="relative">
                <input
                  type="password"
                  dir="ltr"
                  placeholder="123456789:ABCdefGHIjklmn..."
                  value={botToken}
                  onChange={(e) => setBotToken(e.target.value)}
                  className="w-full bg-[#242f3d] text-white text-sm rounded-xl px-4 py-3 pr-10 border border-[#2f3f50] focus:outline-none focus:border-[#3390ec] font-mono transition"
                  autoFocus
                />
                <Key className="w-4 h-4 text-gray-400 absolute right-3 top-3.5" />
              </div>
              <p className="text-xs text-gray-500 mt-1.5">
                {isAr ? 'يمكنك الحصول على رمز البوت من @BotFather في تليجرام' : 'Get your bot token from @BotFather on Telegram'}
              </p>
            </div>

            <button
              type="submit"
              disabled={loading || !botToken.trim()}
              className="w-full bg-[#3390ec] hover:bg-[#2b7ec9] text-white font-semibold py-3.5 px-4 rounded-xl transition flex items-center justify-center gap-2 shadow-lg shadow-[#3390ec]/25 disabled:opacity-50"
            >
              {loading ? (
                <>
                  <Loader2 className="w-5 h-5 animate-spin" />
                  <span>{isAr ? 'جارٍ تسجيل الدخول...' : 'Logging in...'}</span>
                </>
              ) : (
                <span>{isAr ? 'تسجيل الدخول كبوت' : 'Log In with Bot'}</span>
              )}
            </button>
          </form>
        )}

        {/* Demo Tab */}
        {activeTab === 'demo' && (
          <div className="space-y-4 text-center">
            <div className="bg-[#242f3d]/60 border border-[#2f3f50] p-4 rounded-xl text-start space-y-2">
              <div className="flex items-center gap-2 text-amber-400 font-semibold text-sm">
                <Sparkles className="w-4 h-4" />
                <span>{isAr ? 'تجربة فورية بدون انتظار الرمز' : 'Instant Demo Experience'}</span>
              </div>
              <p className="text-xs text-gray-300 leading-relaxed">
                {isAr
                  ? 'يتيح لك هذا الوضع استكشاف وتجربة واجهة تليجرام ويب كاملة بمحادثات حية، قنوات إخبارية عربية، قنوات تقنية، الرسائل المحفوظة، الرسائل الصوتية، الملصقات، وتغيير الثيمات دون الحاجة لإدخال هاتفك الخاص فوراً.'
                  : 'Explore the full Telegram Web interface with channels, groups, Saved Messages, voice notes, stickers, and themes instantly.'}
              </p>
            </div>

            <button
              type="button"
              onClick={handleDemoAccess}
              className="w-full bg-gradient-to-r from-[#3390ec] to-[#00b4d8] hover:opacity-90 text-white font-semibold py-3.5 px-4 rounded-xl transition flex items-center justify-center gap-2 shadow-lg shadow-[#3390ec]/25"
            >
              <Sparkles className="w-4 h-4" />
              <span>{isAr ? 'دخول فوري إلى تليجرام ويب' : 'Launch Telegram Web Demo'}</span>
            </button>
          </div>
        )}

        {/* Footer info */}
        <div className="mt-8 pt-4 border-t border-[#232e3c] text-center text-xs text-gray-500 flex items-center justify-center gap-2">
          <span>MTProto v2.0 Client</span>
          <span>•</span>
          <span>Layer 198</span>
          <span>•</span>
          <span className="text-emerald-400 font-mono">22043994</span>
        </div>
      </div>
    </div>
  );
};
