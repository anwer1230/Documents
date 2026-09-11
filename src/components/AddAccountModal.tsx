import React, { useState } from 'react';
import {
  X,
  Smartphone,
  Bot,
  UserPlus,
  Lock,
  ArrowRight,
  Loader2,
  AlertCircle,
  Sparkles,
  ShieldCheck,
  CheckCircle2,
} from 'lucide-react';
import { TelegramAccount, TelegramUser } from '../types';

interface AddAccountModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAccountAdded: (account: TelegramAccount) => void;
  currentAccountsCount: number;
  maxAccounts: number;
  lang: 'ar' | 'en';
  isDark: boolean;
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

const DEMO_PRESETS = [
  {
    firstName: 'سارة',
    lastName: 'العتيبي',
    username: 'sara_otb',
    phone: '+966551234567',
    bio: 'حساب العمل والمشاريع التقنية 💼',
  },
  {
    firstName: 'فهد',
    lastName: 'الحربي',
    username: 'fahad_eng',
    phone: '+966567890123',
    bio: 'مهندس برمجيات | تواصل للمشاريع 🚀',
  },
  {
    firstName: 'نوف',
    lastName: 'الشمري',
    username: 'nouf_sh',
    phone: '+966509876543',
    bio: 'مصممة واجهات تجربة المستخدم 🎨',
  },
  {
    firstName: 'قناة التقنية',
    lastName: 'العربية',
    username: 'arab_tech_ch',
    phone: '+971501122334',
    bio: 'أحدث أخبار التقنية والتطبيقات الذكية ⚡',
  },
  {
    firstName: 'فريق الدعم',
    lastName: 'الفني',
    username: 'support_team',
    phone: '+201012345678',
    bio: 'خدمة العملاء والمساعدة الفنية على مدار الساعة 🛠️',
  },
];

export const AddAccountModal: React.FC<AddAccountModalProps> = ({
  isOpen,
  onClose,
  onAccountAdded,
  currentAccountsCount,
  maxAccounts,
  lang,
  isDark,
}) => {
  const isAr = lang === 'ar';
  const [method, setMethod] = useState<'phone' | 'bot' | 'demo'>('phone');

  // Phone flow
  const [countryCode, setCountryCode] = useState('+966');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [step, setStep] = useState<'phone' | 'code' | 'password'>('phone');
  const [verificationCode, setVerificationCode] = useState('');
  const [phoneCodeHash, setPhoneCodeHash] = useState('');
  const [password2FA, setPassword2FA] = useState('');

  // Bot flow
  const [botToken, setBotToken] = useState('');

  // Demo flow
  const [selectedDemoIdx, setSelectedDemoIdx] = useState(0);

  // Status
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [allocatedSessionToken, setAllocatedSessionToken] = useState<string | null>(null);

  if (!isOpen) return null;

  // Initialize a fresh isolated session token for this new account slot
  const ensureIsolatedSlot = async (): Promise<string> => {
    if (allocatedSessionToken) return allocatedSessionToken;
    try {
      const res = await fetch('/api/telegram/accounts/new', { method: 'POST' });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.error || 'تعذر حجز فتحة مستخدم جديدة');
      }
      setAllocatedSessionToken(data.sessionToken);
      return data.sessionToken;
    } catch (err: any) {
      // Fallback unique session token
      const fallbackToken = 'user_session_' + Math.random().toString(36).substring(2, 12);
      setAllocatedSessionToken(fallbackToken);
      return fallbackToken;
    }
  };

  const handleSendCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!phoneNumber.trim()) {
      setError(isAr ? 'يرجى إدخال رقم الهاتف' : 'Please enter your phone number');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const token = await ensureIsolatedSlot();
      const cleanNumber = `${countryCode}${phoneNumber.replace(/^0+/, '')}`;

      const res = await fetch('/api/telegram/send-code', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-session-token': token,
        },
        body: JSON.stringify({ phoneNumber: cleanNumber }),
      });
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || data.details || 'فشل إرسال كود التحقق');
      }

      setPhoneCodeHash(data.phoneCodeHash);
      setStep('code');
    } catch (err: any) {
      setError(err.message || 'حدث خطأ أثناء الاتصال بسحابة تيليجرام');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!verificationCode.trim()) {
      setError(isAr ? 'يرجى إدخال كود التحقق' : 'Please enter verification code');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const token = await ensureIsolatedSlot();
      const res = await fetch('/api/telegram/sign-in', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-session-token': token,
        },
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
        const newAcc: TelegramAccount = {
          id: 'acc_' + Date.now().toString(36),
          sessionToken: token,
          user: {
            id: data.user.id || 'me_' + Date.now(),
            firstName: data.user.firstName || 'مستخدم تيليجرام',
            lastName: data.user.lastName,
            username: data.user.username,
            phone: `${countryCode}${phoneNumber.replace(/^0+/, '')}`,
            photoUrl: data.user.photoUrl,
            status: 'online',
          },
          isLoggedIn: true,
          addedAt: Date.now(),
        };
        onAccountAdded(newAcc);
        onClose();
      }
    } catch (err: any) {
      setError(err.message || 'كود التحقق غير صحيح أو انتهت صلاحيته');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!password2FA.trim()) {
      setError(isAr ? 'يرجى إدخال كلمة المرور' : 'Please enter password');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const token = await ensureIsolatedSlot();
      const res = await fetch('/api/telegram/sign-in-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-session-token': token,
        },
        body: JSON.stringify({ password: password2FA }),
      });
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || data.details || 'كلمة المرور غير صحيحة');
      }

      if (data.user) {
        const newAcc: TelegramAccount = {
          id: 'acc_' + Date.now().toString(36),
          sessionToken: token,
          user: {
            id: data.user.id || 'me_' + Date.now(),
            firstName: data.user.firstName || 'مستخدم تيليجرام',
            lastName: data.user.lastName,
            username: data.user.username,
            phone: `${countryCode}${phoneNumber.replace(/^0+/, '')}`,
            status: 'online',
          },
          isLoggedIn: true,
          addedAt: Date.now(),
        };
        onAccountAdded(newAcc);
        onClose();
      }
    } catch (err: any) {
      setError(err.message || 'فشل التحقق من كلمة المرور');
    } finally {
      setLoading(false);
    }
  };

  const handleBotLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!botToken.trim()) {
      setError(isAr ? 'يرجى إدخال رمز البوت' : 'Please enter Bot Token');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const token = await ensureIsolatedSlot();
      const res = await fetch('/api/telegram/bot-login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-session-token': token,
        },
        body: JSON.stringify({ botToken: botToken.trim() }),
      });
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || 'فشل تسجيل الدخول برمز البوت');
      }

      if (data.user) {
        const newAcc: TelegramAccount = {
          id: 'acc_' + Date.now().toString(36),
          sessionToken: token,
          user: {
            id: data.user.id || 'bot_' + Date.now(),
            firstName: data.user.firstName || 'بوت تيليجرام',
            username: data.user.username,
            isBot: true,
            status: 'online',
          },
          isLoggedIn: true,
          addedAt: Date.now(),
        };
        onAccountAdded(newAcc);
        onClose();
      }
    } catch (err: any) {
      setError(err.message || 'رمز البوت غير صالح');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateDemoAccount = async () => {
    setLoading(true);
    setError(null);

    try {
      const token = await ensureIsolatedSlot();
      const preset = DEMO_PRESETS[selectedDemoIdx % DEMO_PRESETS.length];
      const newAcc: TelegramAccount = {
        id: 'acc_' + Date.now().toString(36),
        sessionToken: token,
        user: {
          id: 'user_' + Math.random().toString(36).substring(2, 9),
          firstName: preset.firstName,
          lastName: preset.lastName,
          username: preset.username,
          phone: preset.phone,
          bio: preset.bio,
          status: 'online',
        },
        isLoggedIn: true,
        isDemo: true,
        addedAt: Date.now(),
      };
      onAccountAdded(newAcc);
      onClose();
    } catch (err: any) {
      setError(err.message || 'فشل إنشاء الحساب');
    } finally {
      setLoading(false);
    }
  };

  const isFull = currentAccountsCount >= maxAccounts;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 select-none">
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/60 backdrop-blur-xs transition-opacity"
      />

      {/* Modal Container */}
      <div
        className={`relative w-full max-w-md rounded-2xl shadow-2xl overflow-hidden z-10 flex flex-col transition-all ${
          isDark ? 'bg-[#17212b] text-white border border-[#232e3c]' : 'bg-white text-gray-900 border border-gray-100'
        }`}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-gray-200/10 bg-gradient-to-r from-[#2b5278]/20 to-[#3390ec]/10">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-full bg-[#3390ec] flex items-center justify-center text-white shadow-md">
              <UserPlus className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-base flex items-center gap-2">
                {isAr ? 'إضافة حساب جديد' : 'Add New Account'}
                <span className="text-xs px-2 py-0.5 rounded-full bg-[#3390ec]/20 text-[#3390ec] font-semibold">
                  {currentAccountsCount} / {maxAccounts}
                </span>
              </h3>
              <p className="text-xs text-gray-400">
                {isAr ? 'عزل كامل للحسابات مثل تليجرام ويب الرسمي' : 'Strict account isolation matching Telegram Web'}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full hover:bg-gray-500/10 transition text-gray-400 hover:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 overflow-y-auto max-h-[80vh]">
          {isFull ? (
            <div className="text-center py-6">
              <div className="w-16 h-16 rounded-full bg-amber-500/20 text-amber-500 flex items-center justify-center mx-auto mb-3">
                <AlertCircle className="w-8 h-8" />
              </div>
              <h4 className="font-bold text-lg mb-2">
                {isAr ? 'تم الوصول للحد الأقصى للحسابات' : 'Maximum Accounts Reached'}
              </h4>
              <p className="text-sm text-gray-400 mb-4 leading-relaxed">
                {isAr
                  ? `يدعم تطبيق تليجرام ويب حتى ${maxAccounts} مستخدمين متصلين في نفس الوقت. يرجى تسجيل الخروج من أحد الحسابات الحالية لإضافة حساب جديد.`
                  : `Telegram Web supports up to ${maxAccounts} accounts simultaneously. Please remove an existing account first.`}
              </p>
              <button
                onClick={onClose}
                className="w-full py-2.5 bg-[#3390ec] hover:bg-[#2883df] text-white font-semibold rounded-xl transition"
              >
                {isAr ? 'فهمت' : 'Close'}
              </button>
            </div>
          ) : (
            <>
              {/* Method Switcher */}
              <div className="flex rounded-xl p-1 bg-gray-500/10 mb-5 text-sm font-semibold">
                <button
                  type="button"
                  onClick={() => {
                    setMethod('phone');
                    setError(null);
                  }}
                  className={`flex-1 py-2 rounded-lg flex items-center justify-center gap-1.5 transition ${
                    method === 'phone'
                      ? 'bg-[#3390ec] text-white shadow-sm'
                      : 'text-gray-400 hover:text-gray-200'
                  }`}
                >
                  <Smartphone className="w-4 h-4" />
                  {isAr ? 'رقم هاتف حقيقي' : 'Phone'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setMethod('bot');
                    setError(null);
                  }}
                  className={`flex-1 py-2 rounded-lg flex items-center justify-center gap-1.5 transition ${
                    method === 'bot'
                      ? 'bg-[#3390ec] text-white shadow-sm'
                      : 'text-gray-400 hover:text-gray-200'
                  }`}
                >
                  <Bot className="w-4 h-4" />
                  {isAr ? 'رمز البوت' : 'Bot'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setMethod('demo');
                    setError(null);
                  }}
                  className={`flex-1 py-2 rounded-lg flex items-center justify-center gap-1.5 transition ${
                    method === 'demo'
                      ? 'bg-[#3390ec] text-white shadow-sm'
                      : 'text-gray-400 hover:text-gray-200'
                  }`}
                >
                  <Sparkles className="w-4 h-4" />
                  {isAr ? 'حساب تجريبي' : 'Demo Profile'}
                </button>
              </div>

              {/* Error Message */}
              {error && (
                <div className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-500 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              {/* Phone Method */}
              {method === 'phone' && (
                <div>
                  {step === 'phone' && (
                    <form onSubmit={handleSendCode} className="space-y-4">
                      <div>
                        <label className="block text-xs font-semibold mb-1.5 text-gray-400">
                          {isAr ? 'الدولة والرمز' : 'Country'}
                        </label>
                        <select
                          value={countryCode}
                          onChange={(e) => setCountryCode(e.target.value)}
                          className={`w-full px-3 py-2.5 rounded-xl border text-sm focus:outline-hidden focus:border-[#3390ec] transition ${
                            isDark
                              ? 'bg-[#232e3c] border-gray-700 text-white'
                              : 'bg-gray-50 border-gray-200 text-gray-900'
                          }`}
                        >
                          {COUNTRY_CODES.map((c) => (
                            <option key={c.code} value={c.code}>
                              {c.flag} {c.name} ({c.code})
                            </option>
                          ))}
                        </select>
                      </div>

                      <div>
                        <label className="block text-xs font-semibold mb-1.5 text-gray-400">
                          {isAr ? 'رقم الهاتف' : 'Phone Number'}
                        </label>
                        <div className="flex gap-2" dir="ltr">
                          <span
                            className={`px-3 py-2.5 rounded-xl text-sm font-mono flex items-center ${
                              isDark ? 'bg-[#232e3c] text-gray-300' : 'bg-gray-100 text-gray-700'
                            }`}
                          >
                            {countryCode}
                          </span>
                          <input
                            type="tel"
                            placeholder="5XXXXXXXX"
                            value={phoneNumber}
                            onChange={(e) => setPhoneNumber(e.target.value)}
                            className={`flex-1 px-3 py-2.5 rounded-xl border text-sm focus:outline-hidden focus:border-[#3390ec] font-mono transition ${
                              isDark
                                ? 'bg-[#232e3c] border-gray-700 text-white'
                                : 'bg-gray-50 border-gray-200 text-gray-900'
                            }`}
                            required
                          />
                        </div>
                      </div>

                      <div className="p-3 rounded-xl bg-[#3390ec]/10 border border-[#3390ec]/20 text-xs text-[#3390ec] leading-relaxed flex items-center gap-2">
                        <ShieldCheck className="w-4 h-4 shrink-0" />
                        <span>
                          {isAr
                            ? 'سيتم إرسال كود التحقق الرسمي عبر تطبيق تليجرام على أجهزتك الأخرى أو عبر رسالة SMS.'
                            : 'Telegram will send an official login code to your other devices or via SMS.'}
                        </span>
                      </div>

                      <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-3 bg-[#3390ec] hover:bg-[#2883df] text-white font-semibold rounded-xl transition flex items-center justify-center gap-2 disabled:opacity-50 shadow-md"
                      >
                        {loading ? (
                          <Loader2 className="w-5 h-5 animate-spin" />
                        ) : (
                          <>
                            <span>{isAr ? 'التالي وإرسال الكود' : 'Next'}</span>
                            <ArrowRight className="w-4 h-4 rotate-180" />
                          </>
                        )}
                      </button>
                    </form>
                  )}

                  {step === 'code' && (
                    <form onSubmit={handleVerifyCode} className="space-y-4">
                      <div className="text-center mb-2">
                        <p className="text-xs text-gray-400 mb-1">
                          {isAr ? 'تم إرسال كود التحقق إلى:' : 'Verification code sent to:'}
                        </p>
                        <p className="text-sm font-bold font-mono text-[#3390ec]" dir="ltr">
                          {countryCode} {phoneNumber}
                        </p>
                      </div>

                      <div>
                        <label className="block text-xs font-semibold mb-1.5 text-gray-400 text-center">
                          {isAr ? 'أدخل كود التحقق (5 أرقام)' : 'Enter 5-digit code'}
                        </label>
                        <input
                          type="text"
                          maxLength={6}
                          placeholder="•••••"
                          value={verificationCode}
                          onChange={(e) => setVerificationCode(e.target.value)}
                          className={`w-full text-center tracking-widest text-2xl font-mono py-3 rounded-xl border focus:outline-hidden focus:border-[#3390ec] transition ${
                            isDark
                              ? 'bg-[#232e3c] border-gray-700 text-white'
                              : 'bg-gray-50 border-gray-200 text-gray-900'
                          }`}
                          autoFocus
                          required
                        />
                      </div>

                      <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-3 bg-[#3390ec] hover:bg-[#2883df] text-white font-semibold rounded-xl transition flex items-center justify-center gap-2 disabled:opacity-50 shadow-md"
                      >
                        {loading ? (
                          <Loader2 className="w-5 h-5 animate-spin" />
                        ) : (
                          <span>{isAr ? 'تأكيد الرمز والدخول' : 'Confirm & Login'}</span>
                        )}
                      </button>

                      <button
                        type="button"
                        onClick={() => setStep('phone')}
                        className="w-full text-xs text-gray-400 hover:text-white transition py-1 text-center"
                      >
                        {isAr ? 'العودة لتغيير رقم الهاتف' : 'Back to change phone'}
                      </button>
                    </form>
                  )}

                  {step === 'password' && (
                    <form onSubmit={handleVerifyPassword} className="space-y-4">
                      <div className="text-center mb-2">
                        <div className="w-12 h-12 rounded-full bg-[#3390ec]/20 text-[#3390ec] flex items-center justify-center mx-auto mb-2">
                          <Lock className="w-6 h-6" />
                        </div>
                        <h4 className="font-bold text-sm">
                          {isAr ? 'التحقق بخطوتين (2FA)' : 'Two-Step Verification'}
                        </h4>
                        <p className="text-xs text-gray-400 mt-1">
                          {isAr
                            ? 'هذا الحساب محمي بكلمة مرور إضافية.'
                            : 'This account is protected by an additional cloud password.'}
                        </p>
                      </div>

                      <div>
                        <input
                          type="password"
                          placeholder={isAr ? 'أدخل كلمة المرور السحابية' : 'Enter 2FA password'}
                          value={password2FA}
                          onChange={(e) => setPassword2FA(e.target.value)}
                          className={`w-full px-3 py-3 rounded-xl border text-sm focus:outline-hidden focus:border-[#3390ec] transition ${
                            isDark
                              ? 'bg-[#232e3c] border-gray-700 text-white'
                              : 'bg-gray-50 border-gray-200 text-gray-900'
                          }`}
                          autoFocus
                          required
                        />
                      </div>

                      <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-3 bg-[#3390ec] hover:bg-[#2883df] text-white font-semibold rounded-xl transition flex items-center justify-center gap-2 disabled:opacity-50 shadow-md"
                      >
                        {loading ? (
                          <Loader2 className="w-5 h-5 animate-spin" />
                        ) : (
                          <span>{isAr ? 'فتح الحساب وإضافته' : 'Unlock & Add Account'}</span>
                        )}
                      </button>
                    </form>
                  )}
                </div>
              )}

              {/* Bot Method */}
              {method === 'bot' && (
                <form onSubmit={handleBotLogin} className="space-y-4">
                  <div>
                    <label className="block text-xs font-semibold mb-1.5 text-gray-400">
                      {isAr ? 'رمز البوت (Bot Token)' : 'Bot Token'}
                    </label>
                    <input
                      type="text"
                      placeholder="123456789:ABCdefGHIjklMNOpqrSTUvwxYZ"
                      value={botToken}
                      onChange={(e) => setBotToken(e.target.value)}
                      className={`w-full px-3 py-2.5 rounded-xl border text-sm font-mono focus:outline-hidden focus:border-[#3390ec] transition ${
                        isDark
                          ? 'bg-[#232e3c] border-gray-700 text-white'
                          : 'bg-gray-50 border-gray-200 text-gray-900'
                      }`}
                      required
                    />
                    <p className="text-xs text-gray-400 mt-1.5">
                      {isAr
                        ? 'يمكنك الحصول على رمز البوت من @BotFather على تليجرام.'
                        : 'Obtain your bot token from @BotFather on Telegram.'}
                    </p>
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full py-3 bg-[#3390ec] hover:bg-[#2883df] text-white font-semibold rounded-xl transition flex items-center justify-center gap-2 disabled:opacity-50 shadow-md"
                  >
                    {loading ? (
                      <Loader2 className="w-5 h-5 animate-spin" />
                    ) : (
                      <span>{isAr ? 'تسجيل دخول البوت كحساب منفصل' : 'Add Bot Account'}</span>
                    )}
                  </button>
                </form>
              )}

              {/* Demo Profile Method */}
              {method === 'demo' && (
                <div className="space-y-4">
                  <p className="text-xs text-gray-400 leading-relaxed">
                    {isAr
                      ? 'اختر شخصية جاهزة لاختبار التبديل السلس والعزل الكامل بين المستخدمين حتى 6 حسابات:'
                      : 'Select a preset profile to test seamless multi-account switching up to 6 users:'}
                  </p>

                  <div className="space-y-2 max-h-56 overflow-y-auto">
                    {DEMO_PRESETS.map((preset, idx) => (
                      <div
                        key={preset.username}
                        onClick={() => setSelectedDemoIdx(idx)}
                        className={`p-3 rounded-xl border cursor-pointer transition flex items-center justify-between ${
                          selectedDemoIdx === idx
                            ? 'border-[#3390ec] bg-[#3390ec]/10 text-white'
                            : isDark
                            ? 'border-[#232e3c] bg-[#202b36] hover:bg-[#263442]'
                            : 'border-gray-200 bg-gray-50 hover:bg-gray-100'
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-full bg-[#3390ec] flex items-center justify-center text-white font-bold text-sm">
                            {preset.firstName.slice(0, 2)}
                          </div>
                          <div>
                            <div className="font-bold text-sm">
                              {preset.firstName} {preset.lastName}
                            </div>
                            <div className="text-xs text-gray-400 font-mono">@{preset.username}</div>
                          </div>
                        </div>
                        {selectedDemoIdx === idx && (
                          <CheckCircle2 className="w-5 h-5 text-[#3390ec]" />
                        )}
                      </div>
                    ))}
                  </div>

                  <button
                    type="button"
                    onClick={handleCreateDemoAccount}
                    disabled={loading}
                    className="w-full py-3 bg-[#3390ec] hover:bg-[#2883df] text-white font-semibold rounded-xl transition flex items-center justify-center gap-2 shadow-md"
                  >
                    {loading ? (
                      <Loader2 className="w-5 h-5 animate-spin" />
                    ) : (
                      <span>{isAr ? 'إنشاء وإضافة هذا المستخدم' : 'Add This User'}</span>
                    )}
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};
