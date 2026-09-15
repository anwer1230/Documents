import React, { useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Database,
  Trash2,
  Image,
  Video,
  Mic,
  FileText,
  Clock,
  CheckCircle2,
  HardDrive,
} from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';
import { cacheByChatsController } from '../../core/messenger/CacheByChatsController';
import { ChatCacheUsageInfo } from '../../types';

export const CacheByChatsView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';
  const BackIcon = isArabic ? ArrowRight : ArrowLeft;

  const [cacheList, setCacheList] = useState<ChatCacheUsageInfo[]>(
    cacheByChatsController.getCacheUsageList()
  );

  const formatSize = (bytes: number) => {
    if (bytes <= 0) return '0 B';
    const mb = bytes / (1024 * 1024);
    return `${mb.toFixed(1)} MB`;
  };

  const handleClearChatCache = (chatId: string, title: string) => {
    const cleared = cacheByChatsController.clearChatMediaType(chatId, 'all');
    setCacheList([...cacheByChatsController.getCacheUsageList()]);
    showToast(
      isArabic
        ? `تم مسح ${formatSize(cleared)} من التخزين المؤقت لمحادثة ${title}`
        : `Cleared ${formatSize(cleared)} cache for ${title}`,
      '🧹'
    );
  };

  const totalBytes = cacheByChatsController.getTotalCacheSize();

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
            <HardDrive className="w-5 h-5 text-emerald-300" />
            <span className="font-bold text-base">
              {isArabic ? 'إدارة التخزين المؤقت للمحادثات (Cache by Chats)' : 'Granular Cache by Chats'}
            </span>
          </div>
        </div>

        <span className="text-xs font-mono font-bold bg-black/20 px-2.5 py-1 rounded-lg border border-white/10">
          {formatSize(totalBytes)}
        </span>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {/* Total Storage Summary Card */}
        <div className="bg-[#17212b] rounded-2xl border border-white/10 p-4 space-y-3 shadow-lg">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-[#5288c1] uppercase tracking-wide">
              {isArabic ? 'إجمالي التخزين المؤقت المستهلك' : 'Total Cached Data in Chats'}
            </span>
            <span className="text-sm font-extrabold text-emerald-400">{formatSize(totalBytes)}</span>
          </div>

          <div className="text-xs text-gray-400">
            {isArabic
              ? 'يمكنك حذف وسائط ومستندات كل محادثة بشكل منفصل لتوفير مساحة الذاكرة دون حذف الرسائل.'
              : 'Clear media, voice notes, and documents per chat without deleting messages.'}
          </div>
        </div>

        {/* Per Chat List */}
        <div className="space-y-3">
          {cacheList.map((item) => (
            <div
              key={item.chatId}
              className="bg-[#17212b] rounded-2xl border border-white/10 p-4 space-y-3 shadow-md hover:border-[#5288c1]/40 transition-all"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <img
                    src={item.chatAvatar}
                    alt=""
                    className="w-10 h-10 rounded-full object-cover border border-white/10"
                  />
                  <div>
                    <span className="text-sm font-bold text-white block">{item.chatTitle}</span>
                    <span className="text-xs font-mono text-emerald-400 font-semibold">
                      {formatSize(item.totalBytes)}
                    </span>
                  </div>
                </div>

                <button
                  onClick={() => handleClearChatCache(item.chatId, item.chatTitle)}
                  disabled={item.totalBytes === 0}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-colors ${
                    item.totalBytes > 0
                      ? 'bg-rose-500/15 hover:bg-rose-500/25 active:bg-rose-500/35 text-rose-400 border border-rose-500/30'
                      : 'bg-white/5 text-gray-500 opacity-50 cursor-not-allowed'
                  }`}
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>{isArabic ? 'مسح التخزين' : 'Clear'}</span>
                </button>
              </div>

              {/* Media Breakdown Grid */}
              <div className="grid grid-cols-4 gap-2 pt-1">
                <div className="bg-[#0e1621] p-2 rounded-xl border border-white/5 text-center">
                  <Image className="w-3.5 h-3.5 text-blue-400 mx-auto mb-1" />
                  <span className="text-[10px] text-gray-400 block">{isArabic ? 'صور' : 'Photos'}</span>
                  <span className="text-[11px] font-mono text-white block font-semibold">
                    {formatSize(item.photosBytes)}
                  </span>
                </div>

                <div className="bg-[#0e1621] p-2 rounded-xl border border-white/5 text-center">
                  <Video className="w-3.5 h-3.5 text-purple-400 mx-auto mb-1" />
                  <span className="text-[10px] text-gray-400 block">{isArabic ? 'فيديو' : 'Videos'}</span>
                  <span className="text-[11px] font-mono text-white block font-semibold">
                    {formatSize(item.videosBytes)}
                  </span>
                </div>

                <div className="bg-[#0e1621] p-2 rounded-xl border border-white/5 text-center">
                  <Mic className="w-3.5 h-3.5 text-emerald-400 mx-auto mb-1" />
                  <span className="text-[10px] text-gray-400 block">{isArabic ? 'صوت' : 'Voice'}</span>
                  <span className="text-[11px] font-mono text-white block font-semibold">
                    {formatSize(item.audioBytes)}
                  </span>
                </div>

                <div className="bg-[#0e1621] p-2 rounded-xl border border-white/5 text-center">
                  <FileText className="w-3.5 h-3.5 text-amber-400 mx-auto mb-1" />
                  <span className="text-[10px] text-gray-400 block">{isArabic ? 'ملفات' : 'Files'}</span>
                  <span className="text-[11px] font-mono text-white block font-semibold">
                    {formatSize(item.documentsBytes)}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
