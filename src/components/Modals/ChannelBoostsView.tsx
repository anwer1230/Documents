import React, { useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Zap,
  CheckCircle2,
  Lock,
  Share2,
  Flame,
  Award,
  Sparkles,
  Link,
  Copy,
} from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';
import { channelBoostsController } from '../../core/messenger/ChannelBoostsController';
import { ChannelBoostData } from '../../types';

export const ChannelBoostsView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings, activeChatId, chats, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';
  const BackIcon = isArabic ? ArrowRight : ArrowLeft;

  const currentChannel = chats.find((c) => c.id === activeChatId) || chats.find((c) => c.isChannel) || chats[0];
  const [boostData, setBoostData] = useState<ChannelBoostData>(
    channelBoostsController.getChannelBoost(currentChannel?.id || 'chat_telegram_news')
  );

  const handleBoost = () => {
    const res = channelBoostsController.boostChannel(boostData.chatId);
    setBoostData({ ...res.boostData });
    showToast(
      isArabic
        ? 'تم تعزيز القناة بنجاح! شكراً لدعمك للقناة'
        : 'Channel boosted successfully! Thank you for supporting the channel',
      '⚡'
    );
  };

  const copyBoostLink = () => {
    navigator.clipboard.writeText(boostData.boostUrl);
    showToast(isArabic ? 'تم نسخ رابط التعزيز' : 'Boost link copied to clipboard', '📋');
  };

  const progressPercent = Math.min(
    100,
    Math.round(((boostData.currentBoosts % 10) / 10) * 100) || 40
  );

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
            <Zap className="w-5 h-5 text-amber-300 animate-pulse" />
            <span className="font-bold text-base">
              {isArabic ? 'تعزيز القناة والمميزات (Channel Boosts)' : 'Channel Boosts & Level Perks'}
            </span>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {/* Main Boost Level Card */}
        <div className="bg-[#17212b] rounded-2xl border border-[#5288c1]/30 p-5 text-center space-y-4 shadow-xl relative overflow-hidden">
          <div className="w-20 h-20 mx-auto rounded-3xl bg-gradient-to-br from-amber-400 via-orange-500 to-purple-600 flex items-center justify-center text-4xl shadow-2xl">
            ⚡
          </div>

          <div>
            <h2 className="text-xl font-extrabold text-white">{currentChannel?.title || 'Telegram Channel'}</h2>
            <div className="text-xs text-amber-300 font-bold mt-1 flex items-center justify-center gap-1.5">
              <span>Level {boostData.currentLevel}</span>
              <span>•</span>
              <span>{boostData.currentBoosts} Boosts</span>
            </div>
          </div>

          {/* Progress Bar */}
          <div className="space-y-1.5 max-w-sm mx-auto">
            <div className="flex items-center justify-between text-[11px] text-gray-400 font-semibold">
              <span>Level {boostData.currentLevel}</span>
              <span>
                {boostData.boostsToNextLevel} {isArabic ? 'تعزيزات للمستوى التالي' : 'boosts to Level'} {boostData.currentLevel + 1}
              </span>
            </div>
            <div className="w-full h-2.5 bg-black/40 rounded-full overflow-hidden p-0.5">
              <div
                className="h-full bg-gradient-to-r from-amber-400 to-orange-500 rounded-full transition-all duration-500"
                style={{ width: `${progressPercent}%` }}
              />
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-2 max-w-sm mx-auto pt-2">
            <button
              onClick={handleBoost}
              className="flex-1 py-3 bg-gradient-to-r from-amber-500 to-orange-600 hover:from-amber-600 hover:to-orange-700 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-2 shadow-lg transition-all"
            >
              <Zap className="w-4 h-4 fill-white" />
              <span>{isArabic ? 'تعزيز هذه القناة (+1)' : 'Boost Channel (+1)'}</span>
            </button>

            <button
              onClick={copyBoostLink}
              className="px-3.5 py-3 bg-[#0e1621] hover:bg-white/10 text-gray-300 rounded-xl border border-white/10 flex items-center justify-center transition-colors"
              title={isArabic ? 'نسخ رابط التعزيز' : 'Copy Boost Link'}
            >
              <Copy className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Perks List */}
        <div className="bg-[#17212b] rounded-2xl border border-white/10 p-4 space-y-3">
          <div className="flex items-center gap-2">
            <Award className="w-4 h-4 text-[#5288c1]" />
            <span className="text-xs font-bold text-[#5288c1] uppercase tracking-wide">
              {isArabic ? 'مميزات مستويات التعزيز المفتوحة' : 'Level Perks & Features'}
            </span>
          </div>

          <div className="space-y-2">
            {boostData.unlockedPerks.map((perk) => (
              <div
                key={perk.level}
                className={`p-3 rounded-xl border flex items-center justify-between gap-3 ${
                  perk.isUnlocked
                    ? 'bg-[#0e1621] border-emerald-500/30 text-white'
                    : 'bg-[#0e1621]/40 border-white/5 text-gray-400 opacity-60'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold ${
                      perk.isUnlocked
                        ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                        : 'bg-white/5 text-gray-400'
                    }`}
                  >
                    L{perk.level}
                  </div>
                  <div>
                    <span className="text-xs font-bold block text-white">
                      {isArabic ? perk.titleAr : perk.title}
                    </span>
                    <span className="text-[11px] text-gray-400 block">{perk.description}</span>
                  </div>
                </div>

                {perk.isUnlocked ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                ) : (
                  <Lock className="w-4 h-4 text-gray-500 shrink-0" />
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
