import React, { useState } from 'react';
import {
  Sparkles,
  Gift,
  Plus,
  Send,
  X,
  CheckCircle2,
  Trophy,
  ShieldCheck,
  Zap,
} from 'lucide-react';
import { TelegramChat } from '../types';
import { TgsPlayer } from './TgsPlayer';
import { STAR_LOTTIE } from '../utils/tgsAnimations';

interface TelegramStarsModalProps {
  isOpen: boolean;
  onClose: () => void;
  chat?: TelegramChat;
  userStarsBalance: number;
  onTopUpStars: (amount: number) => void;
  onSendStars: (gift: { amount: number; message?: string; targetChatId: string }) => void;
  isDark: boolean;
  lang: 'ar' | 'en';
}

const STAR_PURCHASE_PACKAGES = [
  { stars: 50, price: '$0.99', popular: false },
  { stars: 100, price: '$1.99', popular: false },
  { stars: 250, price: '$4.99', popular: true },
  { stars: 500, price: '$9.99', popular: false },
  { stars: 1000, price: '$19.99', popular: false },
  { stars: 2500, price: '$49.99', popular: false },
];

const GIFT_PRESETS = [15, 50, 100, 250, 500];

export const TelegramStarsModal: React.FC<TelegramStarsModalProps> = ({
  isOpen,
  onClose,
  chat,
  userStarsBalance,
  onTopUpStars,
  onSendStars,
  isDark,
  lang,
}) => {
  const isAr = lang === 'ar';
  const [activeTab, setActiveTab] = useState<'send' | 'buy'>('send');
  const [giftAmount, setGiftAmount] = useState<number>(50);
  const [giftMessage, setGiftMessage] = useState<string>('');
  const [isSuccess, setIsSuccess] = useState(false);

  if (!isOpen) return null;

  const handleSendGift = (e: React.FormEvent) => {
    e.preventDefault();
    if (!chat || giftAmount <= 0) return;

    if (userStarsBalance < giftAmount) {
      alert(
        isAr
          ? 'رصيدك من النجوم غير كافٍ. يرجى شحن رصيدك من النجوم أولاً!'
          : 'Insufficient Stars balance. Please top up your Stars first!'
      );
      setActiveTab('buy');
      return;
    }

    onSendStars({
      amount: giftAmount,
      message: giftMessage.trim() || undefined,
      targetChatId: chat.id,
    });

    setIsSuccess(true);
    setTimeout(() => {
      setIsSuccess(false);
      onClose();
    }, 1800);
  };

  const handleBuy = (amount: number) => {
    onTopUpStars(amount);
    alert(
      isAr
        ? `تم شحن ${amount} نجمة تليجرام بنجاح!`
        : `Successfully purchased ${amount} Telegram Stars!`
    );
    setActiveTab('send');
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/65 backdrop-blur-sm animate-in fade-in duration-150">
      <div
        className={`w-full max-w-md rounded-3xl shadow-2xl border flex flex-col overflow-hidden relative ${
          isDark ? 'bg-[#17212b] border-gray-700/80 text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        {/* Success Overlay Animation */}
        {isSuccess && (
          <div className="absolute inset-0 z-50 flex flex-col items-center justify-center bg-[#17212b]/95 text-white animate-in zoom-in-95 duration-200">
            <TgsPlayer animationData={STAR_LOTTIE} width={120} height={120} loop={false} />
            <h3 className="text-lg font-bold mt-2 text-amber-300">
              {isAr ? 'تم إرسال هدية النجوم بنجاح! ⭐️' : 'Stars Gift Sent Successfully! ⭐️'}
            </h3>
            <p className="text-xs text-gray-300 mt-1">
              {isAr ? `شكراً لدعمك ${chat?.title || 'القناة'}` : `Thank you for supporting ${chat?.title || 'the channel'}`}
            </p>
          </div>
        )}

        {/* Header with Star Hero */}
        <div className="relative p-5 pb-3 flex flex-col items-center justify-center text-center bg-gradient-to-b from-amber-500/15 via-transparent to-transparent">
          <button
            onClick={onClose}
            className="absolute top-4 right-4 p-1.5 rounded-full text-gray-400 hover:text-white transition"
          >
            <X className="w-4 h-4" />
          </button>

          {/* 3D Vector Telegram Star */}
          <div className="relative w-20 h-20 mb-1">
            <TgsPlayer animationData={STAR_LOTTIE} width={80} height={80} loop={true} />
          </div>

          <h2 className="text-lg font-bold flex items-center gap-1.5 text-amber-400">
            <span>{isAr ? 'نجوم تليجرام (Telegram Stars)' : 'Telegram Stars'}</span>
          </h2>

          {/* Current User Balance Badge */}
          <div className="mt-2 inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-400/20 text-amber-300 border border-amber-400/30 shadow-xs">
            <Sparkles className="w-3.5 h-3.5" />
            <span>{isAr ? 'رصيدك الحالي:' : 'Your Balance:'} {userStarsBalance.toLocaleString()} ⭐️</span>
          </div>

          {/* Tab Switcher */}
          <div className="mt-4 flex items-center p-1 rounded-2xl bg-black/10 dark:bg-white/5 border border-white/10 w-full max-w-xs">
            <button
              type="button"
              onClick={() => setActiveTab('send')}
              className={`flex-1 py-1.5 rounded-xl text-xs font-bold transition flex items-center justify-center gap-1.5 ${
                activeTab === 'send'
                  ? 'bg-amber-400 text-gray-950 shadow-sm'
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              <Gift className="w-3.5 h-3.5" />
              <span>{isAr ? 'إرسال هدية' : 'Send Gift'}</span>
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('buy')}
              className={`flex-1 py-1.5 rounded-xl text-xs font-bold transition flex items-center justify-center gap-1.5 ${
                activeTab === 'buy'
                  ? 'bg-amber-400 text-gray-950 shadow-sm'
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              <Plus className="w-3.5 h-3.5" />
              <span>{isAr ? 'شراء نجوم' : 'Buy Stars'}</span>
            </button>
          </div>
        </div>

        {/* Tab Content */}
        <div className="p-5 pt-2 flex-1 overflow-y-auto">
          {activeTab === 'send' ? (
            <form onSubmit={handleSendGift} className="space-y-4">
              <div className="text-center">
                <p className="text-xs text-gray-400">
                  {isAr
                    ? `إرسال نجوم لدعم منشئي المحتوى وقنوات ${chat?.title || 'المحادثة'}`
                    : `Send stars to support creators of ${chat?.title || 'chat'}`}
                </p>
              </div>

              {/* Gift Amount Selector */}
              <div>
                <label className="text-xs text-gray-400 block mb-2 font-medium">
                  {isAr ? 'اختر عدد النجوم المراد إهداؤها:' : 'Choose stars amount to gift:'}
                </label>
                <div className="grid grid-cols-5 gap-1.5">
                  {GIFT_PRESETS.map((amt) => (
                    <button
                      key={amt}
                      type="button"
                      onClick={() => setGiftAmount(amt)}
                      className={`py-2 rounded-xl text-xs font-bold border flex flex-col items-center justify-center transition-all ${
                        giftAmount === amt
                          ? 'bg-amber-400 text-gray-950 border-amber-300 shadow-md scale-105'
                          : isDark
                          ? 'bg-[#242f3d] border-gray-700 text-gray-200 hover:border-amber-400/50'
                          : 'bg-gray-100 border-gray-200 text-gray-800 hover:border-amber-400'
                      }`}
                    >
                      <span>⭐️</span>
                      <span>{amt}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Personal message */}
              <div>
                <label className="text-xs text-gray-400 block mb-1">
                  {isAr ? 'رسالة إهداء شخصية (اختياري):' : 'Personal dedication (optional):'}
                </label>
                <input
                  type="text"
                  value={giftMessage}
                  onChange={(e) => setGiftMessage(e.target.value)}
                  placeholder={isAr ? 'شكراً على المحتوى الرائع والمفيد! 🚀' : 'Thanks for awesome content! 🚀'}
                  className={`w-full px-3 py-2 rounded-xl text-xs outline-none border ${
                    isDark ? 'bg-[#242f3d] border-gray-700 text-white' : 'bg-gray-100 border-gray-200 text-gray-900'
                  }`}
                />
              </div>

              <button
                type="submit"
                className="w-full py-3 rounded-2xl bg-gradient-to-r from-amber-400 via-amber-300 to-yellow-400 text-gray-950 text-xs font-extrabold shadow-lg hover:opacity-95 transition flex items-center justify-center gap-2"
              >
                <Gift className="w-4 h-4 text-gray-950" />
                <span>
                  {isAr ? `إرسال ${giftAmount} ⭐️ إلى ${chat?.title || 'المستلم'}` : `Send ${giftAmount} ⭐️ to ${chat?.title || 'Chat'}`}
                </span>
              </button>
            </form>
          ) : (
            /* Buy Stars Tab */
            <div className="space-y-3">
              <p className="text-xs text-gray-400 text-center">
                {isAr
                  ? 'اختر حزمة النجوم المراد شراؤها واستخدامها في التفاعل والهدايا والتطبيقات المصغرة'
                  : 'Select stars package to purchase for reactions, gifts, and mini apps'}
              </p>

              <div className="grid grid-cols-2 gap-2">
                {STAR_PURCHASE_PACKAGES.map((pkg) => (
                  <button
                    key={pkg.stars}
                    type="button"
                    onClick={() => handleBuy(pkg.stars)}
                    className={`relative p-3 rounded-2xl border text-center transition hover:border-amber-400 hover:scale-[1.02] flex flex-col items-center justify-center ${
                      pkg.popular
                        ? 'border-amber-400/80 bg-amber-400/10'
                        : isDark
                        ? 'border-gray-700 bg-[#242f3d]'
                        : 'border-gray-200 bg-gray-50'
                    }`}
                  >
                    {pkg.popular && (
                      <span className="absolute -top-2 px-2 py-0.5 rounded-full text-[9px] font-bold bg-amber-400 text-gray-950">
                        {isAr ? 'الأكثر طلباً' : 'POPULAR'}
                      </span>
                    )}
                    <div className="text-base font-extrabold text-amber-400 flex items-center gap-1">
                      <span>⭐️</span>
                      <span>{pkg.stars.toLocaleString()}</span>
                    </div>
                    <span className="text-xs font-semibold text-gray-400 mt-1">{pkg.price}</span>
                  </button>
                ))}
              </div>

              <div className="pt-2 text-center">
                <p className="text-[11px] text-gray-500 flex items-center justify-center gap-1">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                  <span>{isAr ? 'دفع آمن معتمد من تيليجرام' : 'Secure official Telegram Stars purchase'}</span>
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
