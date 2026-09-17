import React, { useState } from 'react';
import {
  X,
  Star,
  Sparkles,
  Gift,
  CreditCard,
  CheckCircle2,
  TrendingUp,
  ShieldCheck,
  Zap,
} from 'lucide-react';
import { TelegramStarPackage } from '../types';

interface TelegramStarsModalProps {
  isOpen: boolean;
  onClose: () => void;
  starsBalance: number;
  onBuyStars: (amount: number) => Promise<void>;
  onSendStarsGift: (recipientId: string, stars: number) => Promise<void>;
  currentChatId?: string;
  currentChatTitle?: string;
  isDark: boolean;
  lang: 'ar' | 'en';
}

const STAR_PACKAGES: TelegramStarPackage[] = [
  { id: 'p50', stars: 50, price: '$0.99', badge: 'Starter' },
  { id: 'p100', stars: 100, price: '$1.99', badge: 'Popular' },
  { id: 'p250', stars: 250, price: '$4.99', bonus: 25 },
  { id: 'p500', stars: 500, price: '$9.99', bonus: 75, badge: 'Best Value' },
  { id: 'p1000', stars: 1000, price: '$18.99', bonus: 200 },
  { id: 'p2500', stars: 2500, price: '$44.99', bonus: 600, badge: 'VIP' },
];

export const TelegramStarsModal: React.FC<TelegramStarsModalProps> = ({
  isOpen,
  onClose,
  starsBalance,
  onBuyStars,
  onSendStarsGift,
  currentChatId,
  currentChatTitle,
  isDark,
  lang,
}) => {
  const isAr = lang === 'ar';
  const [activeTab, setActiveTab] = useState<'buy' | 'gift'>('buy');
  const [customGiftAmount, setCustomGiftAmount] = useState('25');
  const [isProcessing, setIsProcessing] = useState(false);
  const [successToast, setSuccessToast] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleBuy = async (pkg: TelegramStarPackage) => {
    setIsProcessing(true);
    try {
      await onBuyStars(pkg.stars + (pkg.bonus || 0));
      setSuccessToast(
        isAr
          ? `تم شحن ${pkg.stars + (pkg.bonus || 0)} نجمة تيليجرام بنجاح!`
          : `Successfully purchased ${pkg.stars + (pkg.bonus || 0)} Telegram Stars!`
      );
      setTimeout(() => setSuccessToast(null), 3000);
    } catch (err) {
      console.error('Failed to buy stars:', err);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleSendGift = async (e: React.FormEvent) => {
    e.preventDefault();
    const amount = parseInt(customGiftAmount, 10);
    if (!amount || amount <= 0 || !currentChatId || isProcessing) return;
    if (amount > starsBalance) {
      alert(
        isAr
          ? 'رصيد النجوم غير كافٍ. يرجى شحن رصيد النجوم أولاً.'
          : 'Insufficient Stars balance. Please top up your stars first.'
      );
      setActiveTab('buy');
      return;
    }

    setIsProcessing(true);
    try {
      await onSendStarsGift(currentChatId, amount);
      setSuccessToast(
        isAr
          ? `تم إرسال ${amount} نجمة هدية إلى ${currentChatTitle || 'القناة'}!`
          : `Gift of ${amount} Stars sent to ${currentChatTitle || 'Chat'}!`
      );
      setTimeout(() => setSuccessToast(null), 3500);
    } catch (err) {
      console.error('Failed to send stars gift:', err);
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className={`w-full max-w-md rounded-2xl shadow-2xl flex flex-col max-h-[90vh] overflow-hidden border ${
          isDark ? 'bg-[#17212b] border-gray-700/80 text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        {/* Header with Telegram Stars Golden Branding */}
        <div className="px-5 py-4 bg-gradient-to-r from-amber-500 to-yellow-400 text-black flex items-center justify-between shrink-0 shadow-md">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-black/10 text-yellow-900">
              <Star className="w-6 h-6 fill-current animate-pulse" />
            </div>
            <div>
              <h3 className="font-extrabold text-base tracking-tight">
                {isAr ? 'نجوم تيليجرام (Telegram Stars)' : 'Telegram Stars'}
              </h3>
              <p className="text-xs text-yellow-950 font-medium">
                {isAr ? 'العملة الرقمية الرسمية لدعم المبدعين والتطبيقات' : 'Official currency for creators & digital goods'}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-full text-black/70 hover:text-black hover:bg-black/10 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Balance Card */}
        <div
          className={`p-4 border-b flex items-center justify-between shrink-0 ${
            isDark ? 'bg-[#242f3d] border-gray-700/60' : 'bg-amber-50/50 border-amber-100'
          }`}
        >
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-full bg-gradient-to-tr from-amber-400 to-yellow-300 flex items-center justify-center text-black font-black text-xl shadow-md">
              ⭐
            </div>
            <div>
              <span className="text-xs text-gray-400 block font-medium">
                {isAr ? 'رصيدك الحالي من النجوم' : 'Your Stars Balance'}
              </span>
              <span className="text-xl font-extrabold text-amber-400 flex items-center gap-1">
                {starsBalance.toLocaleString()} <span className="text-xs text-gray-400 font-normal">Stars</span>
              </span>
            </div>
          </div>

          <div className="flex items-center gap-1 p-1 rounded-xl bg-black/10 dark:bg-white/5 text-xs">
            <button
              type="button"
              onClick={() => setActiveTab('buy')}
              className={`px-3 py-1.5 rounded-lg font-semibold transition ${
                activeTab === 'buy' ? 'bg-[#3390ec] text-white shadow-sm' : 'text-gray-400 hover:text-white'
              }`}
            >
              {isAr ? 'شراء' : 'Buy'}
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('gift')}
              className={`px-3 py-1.5 rounded-lg font-semibold transition ${
                activeTab === 'gift' ? 'bg-[#3390ec] text-white shadow-sm' : 'text-gray-400 hover:text-white'
              }`}
            >
              {isAr ? 'إهداء' : 'Gift'}
            </button>
          </div>
        </div>

        {/* Success Alert Banner */}
        {successToast && (
          <div className="p-3 bg-emerald-500/20 border-b border-emerald-500/30 text-emerald-300 text-xs flex items-center gap-2 animate-in fade-in">
            <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-400" />
            <span>{successToast}</span>
          </div>
        )}

        {/* Tab Content */}
        <div className="flex-1 overflow-y-auto p-4">
          {activeTab === 'buy' ? (
            <div className="space-y-3">
              <div className="text-xs text-gray-400 mb-2">
                {isAr
                  ? 'اختر باقة النجوم التي تناسبك للشحن المباشر:'
                  : 'Select a Stars package to top up your balance:'}
              </div>

              <div className="grid grid-cols-2 gap-2.5">
                {STAR_PACKAGES.map((pkg) => (
                  <button
                    key={pkg.id}
                    type="button"
                    onClick={() => handleBuy(pkg)}
                    disabled={isProcessing}
                    className={`p-3.5 rounded-2xl border text-left transition transform active:scale-[0.98] flex flex-col justify-between relative overflow-hidden group ${
                      isDark
                        ? 'bg-[#242f3d] border-gray-700/80 hover:border-amber-400/50 hover:bg-[#2c3848]'
                        : 'bg-white border-gray-200 hover:border-amber-400/80 hover:shadow-md'
                    }`}
                  >
                    {pkg.badge && (
                      <span className="absolute top-2 right-2 px-1.5 py-0.5 rounded-full text-[9px] font-bold bg-amber-500/20 text-amber-400 border border-amber-500/30">
                        {pkg.badge}
                      </span>
                    )}

                    <div className="flex items-center gap-1.5 text-base font-black text-amber-400">
                      <span>⭐</span>
                      <span>{pkg.stars}</span>
                      {pkg.bonus && (
                        <span className="text-[10px] text-emerald-400 font-bold">
                          +{pkg.bonus}
                        </span>
                      )}
                    </div>

                    <div className="mt-3 flex items-center justify-between">
                      <span className="text-xs font-bold text-gray-300 group-hover:text-white">
                        {pkg.price}
                      </span>
                      <span className="p-1 rounded-lg bg-[#3390ec]/20 text-[#3390ec] text-[10px] font-bold">
                        {isAr ? 'شحن' : 'Top up'}
                      </span>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <form onSubmit={handleSendGift} className="space-y-4">
              <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center gap-3">
                <Gift className="w-8 h-8 text-amber-400 shrink-0" />
                <div className="text-xs">
                  <h4 className="font-bold text-amber-300">
                    {isAr ? 'إهداء نجوم لمنشئ المحتوى' : 'Gift Stars to Creator'}
                  </h4>
                  <p className="text-gray-400 mt-0.5">
                    {isAr
                      ? `إرسال نجوم لدعم ${currentChatTitle || 'هذه المحادثة'}`
                      : `Support ${currentChatTitle || 'this chat'} with Stars`}
                  </p>
                </div>
              </div>

              <div>
                <label className="text-xs text-gray-400 block mb-1">
                  {isAr ? 'عدد النجوم المراد إهداؤها' : 'Stars Amount'}
                </label>
                <div className="grid grid-cols-4 gap-2 mb-2">
                  {['10', '25', '50', '100'].map((val) => (
                    <button
                      key={val}
                      type="button"
                      onClick={() => setCustomGiftAmount(val)}
                      className={`py-2 rounded-xl text-xs font-bold border transition ${
                        customGiftAmount === val
                          ? 'bg-amber-400 text-black border-amber-400 shadow-sm'
                          : isDark
                          ? 'bg-[#242f3d] border-gray-700 text-gray-300'
                          : 'bg-gray-100 border-gray-200 text-gray-700'
                      }`}
                    >
                      ⭐ {val}
                    </button>
                  ))}
                </div>

                <input
                  type="number"
                  min={1}
                  value={customGiftAmount}
                  onChange={(e) => setCustomGiftAmount(e.target.value)}
                  placeholder="25"
                  className={`w-full px-3 py-2.5 rounded-xl text-xs outline-none border ${
                    isDark ? 'bg-[#242f3d] border-gray-700 text-white' : 'bg-gray-100 border-gray-300'
                  }`}
                />
              </div>

              <button
                type="submit"
                disabled={isProcessing || !customGiftAmount}
                className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-amber-500 to-yellow-400 text-black font-extrabold text-sm hover:from-amber-600 hover:to-yellow-500 transition shadow-lg flex items-center justify-center gap-2 disabled:opacity-50"
              >
                <Sparkles className="w-4 h-4" />
                <span>
                  {isProcessing
                    ? isAr
                      ? 'جارِ إرسال الهدية...'
                      : 'Sending Gift...'
                    : isAr
                    ? `إرسال ⭐ ${customGiftAmount} نجمة`
                    : `Send ⭐ ${customGiftAmount} Stars`}
                </span>
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};
