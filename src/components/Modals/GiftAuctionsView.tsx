import React, { useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Gift,
  Flame,
  Star,
  Gavel,
  Clock,
  Sparkles,
  Award,
  ChevronRight,
  CheckCircle2,
  TrendingUp,
} from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';
import { giftAuctionController } from '../../core/messenger/GiftAuctionController';
import { GiftAuctionItem, StarGiftItem } from '../../types';

export const GiftAuctionsView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings, currentUser, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';
  const BackIcon = isArabic ? ArrowRight : ArrowLeft;

  const [activeTab, setActiveTab] = useState<'auctions' | 'catalog'>('auctions');
  const [auctions, setAuctions] = useState<GiftAuctionItem[]>(giftAuctionController.getAuctions());
  const [starGifts] = useState<StarGiftItem[]>(giftAuctionController.getStarGifts());
  const [selectedAuction, setSelectedAuction] = useState<GiftAuctionItem | null>(auctions[0] || null);
  const [bidAmount, setBidAmount] = useState<number>(selectedAuction?.minNextBid || 1000);

  const handlePlaceBid = () => {
    if (!selectedAuction) return;
    const res = giftAuctionController.placeBid(
      selectedAuction.id,
      currentUser.id,
      currentUser.name,
      currentUser.avatar,
      bidAmount
    );

    if (res.success && res.updatedAuction) {
      setSelectedAuction(res.updatedAuction);
      setAuctions([...giftAuctionController.getAuctions()]);
      setBidAmount(res.updatedAuction.minNextBid);
      showToast(
        isArabic
          ? `تم تقديم العرض بقيمة ${bidAmount} نجمة بنجاح!`
          : `Bid of ${bidAmount} Stars placed successfully!`,
        '⭐'
      );
    } else {
      showToast(res.error || 'Failed to place bid', '⚠️');
    }
  };

  return (
    <div className="flex flex-col h-full bg-[#0e1621] text-white">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3.5 bg-[#2481cc] text-white shrink-0 shadow-md">
        <div className="flex items-center gap-3">
          <button
            onClick={onBack}
            className="p-1.5 rounded-full hover:bg-white/15 transition-colors"
          >
            <BackIcon className="w-5 h-5" />
          </button>
          <div className="flex items-center gap-2">
            <Gift className="w-5 h-5 text-amber-300 animate-bounce" />
            <span className="font-bold text-base">
              {isArabic ? 'مزادات هدايا النجوم الحصرية (Star Gifts)' : 'Telegram Star Gift Auctions'}
            </span>
          </div>
        </div>

        <div className="flex items-center gap-1.5 bg-black/20 px-3 py-1 rounded-full text-xs font-bold text-amber-300 border border-amber-300/30">
          <Star className="w-3.5 h-3.5 fill-amber-300 text-amber-300" />
          <span>2,500 {isArabic ? 'نجمة' : 'Stars'}</span>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-white/10 bg-[#17212b] px-4 shrink-0">
        <button
          onClick={() => setActiveTab('auctions')}
          className={`py-3 px-4 text-xs font-bold flex items-center gap-2 border-b-2 transition-all ${
            activeTab === 'auctions'
              ? 'border-[#5288c1] text-[#5288c1]'
              : 'border-transparent text-gray-400 hover:text-white'
          }`}
        >
          <Gavel className="w-4 h-4" />
          <span>{isArabic ? 'المزادات الحية الحصرية' : 'Live Unique Auctions'}</span>
        </button>
        <button
          onClick={() => setActiveTab('catalog')}
          className={`py-3 px-4 text-xs font-bold flex items-center gap-2 border-b-2 transition-all ${
            activeTab === 'catalog'
              ? 'border-[#5288c1] text-[#5288c1]'
              : 'border-transparent text-gray-400 hover:text-white'
          }`}
        >
          <Gift className="w-4 h-4" />
          <span>{isArabic ? 'كتالوج الهدايا المتاحة' : 'Star Gifts Catalog'}</span>
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {activeTab === 'auctions' ? (
          <div className="space-y-4">
            {/* Live Auction Card */}
            {selectedAuction && (
              <div className="bg-[#17212b] rounded-2xl border border-amber-500/30 p-4 space-y-4 shadow-xl relative overflow-hidden">
                <div className="absolute top-0 right-0 bg-gradient-to-l from-amber-500/20 to-transparent w-40 h-40 pointer-events-none" />

                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-amber-400 to-orange-600 flex items-center justify-center text-3xl shadow-lg">
                      {selectedAuction.symbol.split('#')[0] || '👑'}
                    </div>
                    <div>
                      <div className="text-base font-bold text-white flex items-center gap-2">
                        <span>{selectedAuction.title}</span>
                        <span className="px-2 py-0.5 bg-amber-500/20 text-amber-300 border border-amber-500/30 rounded-full text-[10px] font-bold">
                          {selectedAuction.symbol}
                        </span>
                      </div>
                      <div className="text-xs text-gray-400 mt-0.5 flex items-center gap-1.5">
                        <Clock className="w-3.5 h-3.5 text-amber-400" />
                        <span>
                          {isArabic ? 'ينتهي المزاد خلال:' : 'Auction ends in:'} 18 {isArabic ? 'ساعة' : 'hours'}
                        </span>
                      </div>
                    </div>
                  </div>

                  <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 animate-pulse">
                    <Flame className="w-3.5 h-3.5" />
                    {isArabic ? 'مزاد نشط' : 'Live'}
                  </span>
                </div>

                {/* Attributes Grid */}
                <div className="grid grid-cols-3 gap-2">
                  {selectedAuction.attributes.map((attr, idx) => (
                    <div key={idx} className="bg-[#0e1621] p-2.5 rounded-xl border border-white/5 space-y-0.5">
                      <span className="text-[10px] text-gray-400 block">{attr.key}</span>
                      <span className="text-xs font-bold text-white block truncate">{attr.value}</span>
                      {attr.rarityPercentage && (
                        <span className="text-[10px] text-amber-400 block">
                          Top {attr.rarityPercentage}% {isArabic ? 'ندرة' : 'Rarity'}
                        </span>
                      )}
                    </div>
                  ))}
                </div>

                {/* Current Bid & Leading Bidder */}
                <div className="bg-[#0e1621] p-3.5 rounded-xl border border-white/5 flex items-center justify-between">
                  <div>
                    <span className="text-[11px] text-gray-400 block">{isArabic ? 'أعلى عرض حالي' : 'Current Highest Bid'}</span>
                    <div className="text-lg font-extrabold text-amber-300 flex items-center gap-1.5 mt-0.5">
                      <Star className="w-5 h-5 fill-amber-300 text-amber-300" />
                      <span>{selectedAuction.currentBidStars.toLocaleString()} Stars</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 text-right">
                    <img
                      src={selectedAuction.highestBidderAvatar}
                      alt=""
                      className="w-8 h-8 rounded-full border border-white/20 object-cover"
                    />
                    <div>
                      <span className="text-[10px] text-gray-400 block">{isArabic ? 'المتصدر' : 'Top Bidder'}</span>
                      <span className="text-xs font-bold text-white block">{selectedAuction.highestBidderName}</span>
                    </div>
                  </div>
                </div>

                {/* Place Bid Form */}
                <div className="space-y-2 pt-1">
                  <div className="flex items-center justify-between text-xs text-gray-300">
                    <span>{isArabic ? 'عرضك التالي (بالنجوم):' : 'Your Next Bid (Stars):'}</span>
                    <span className="text-[11px] text-amber-400">
                      {isArabic ? 'الحد الأدنى:' : 'Minimum:'} {selectedAuction.minNextBid} Stars
                    </span>
                  </div>

                  <div className="flex gap-2">
                    <div className="relative flex-1">
                      <Star className="w-4 h-4 text-amber-400 absolute left-3 top-1/2 -translate-y-1/2 fill-amber-400" />
                      <input
                        type="number"
                        min={selectedAuction.minNextBid}
                        step={50}
                        value={bidAmount}
                        onChange={(e) => setBidAmount(Number(e.target.value))}
                        className="w-full bg-[#0e1621] border border-white/10 rounded-xl pl-9 pr-3 py-2.5 text-xs text-white font-bold focus:outline-none focus:border-[#5288c1]"
                      />
                    </div>
                    <button
                      onClick={handlePlaceBid}
                      className="px-6 py-2.5 bg-gradient-to-r from-amber-500 to-orange-600 hover:from-amber-600 hover:to-orange-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-lg transition-all"
                    >
                      <Gavel className="w-4 h-4" />
                      <span>{isArabic ? 'تقديم العرض' : 'Place Bid'}</span>
                    </button>
                  </div>
                </div>

                {/* Recent Bids Log */}
                <div className="space-y-1.5 pt-2">
                  <span className="text-xs font-bold text-gray-400 uppercase tracking-wide block">
                    {isArabic ? 'سجل المزايدات الحية' : 'Live Bid History'} ({selectedAuction.totalBidsCount})
                  </span>
                  <div className="space-y-1.5 max-h-36 overflow-y-auto">
                    {selectedAuction.recentBids.map((b) => (
                      <div
                        key={b.bidId}
                        className="p-2 bg-[#0e1621] rounded-lg border border-white/5 flex items-center justify-between text-xs"
                      >
                        <div className="flex items-center gap-2">
                          <img src={b.userAvatar} alt="" className="w-6 h-6 rounded-full object-cover" />
                          <span className="font-bold text-white">{b.userName}</span>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="font-bold text-amber-300 flex items-center gap-1">
                            <Star className="w-3.5 h-3.5 fill-amber-300" />
                            {b.amountStars.toLocaleString()}
                          </span>
                          <span className="text-[10px] text-gray-500 font-mono">{b.timestamp}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        ) : (
          /* Star Gifts Catalog */
          <div className="grid grid-cols-2 gap-3">
            {starGifts.map((gift) => (
              <div
                key={gift.id}
                className="bg-[#17212b] rounded-2xl border border-white/10 p-3.5 flex flex-col items-center text-center relative group hover:border-[#5288c1]/50 transition-all shadow-md"
              >
                {gift.badge && (
                  <span className="absolute top-2.5 right-2.5 px-2 py-0.5 bg-amber-500/20 border border-amber-500/30 text-amber-300 rounded-full text-[10px] font-bold">
                    {gift.badge}
                  </span>
                )}

                <div className="w-16 h-16 rounded-2xl bg-white/5 flex items-center justify-center text-3xl my-2 group-hover:scale-110 transition-transform">
                  {gift.emoji}
                </div>

                <span className="text-xs font-bold text-white block">{gift.title}</span>

                <div className="flex items-center gap-1 text-amber-300 text-xs font-bold mt-1">
                  <Star className="w-3.5 h-3.5 fill-amber-300" />
                  <span>{gift.starsPrice} Stars</span>
                </div>

                {gift.soldCount && (
                  <span className="text-[10px] text-gray-400 mt-1">
                    {gift.soldCount.toLocaleString()} {isArabic ? 'تم إرسالها' : 'sent'}
                  </span>
                )}

                <button
                  onClick={() =>
                    showToast(
                      isArabic
                        ? `تم إرسال هدية ${gift.title} بنجاح!`
                        : `Sent ${gift.title} gift successfully!`,
                      gift.emoji
                    )
                  }
                  className="w-full mt-3 py-2 bg-[#2481cc] hover:bg-[#1f6fa8] active:bg-[#195a88] text-white rounded-xl text-xs font-bold flex items-center justify-center gap-1 transition-colors"
                >
                  <Gift className="w-3.5 h-3.5" />
                  <span>{isArabic ? 'إرسال كهدية' : 'Send Gift'}</span>
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
