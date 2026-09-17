import React, { useState } from 'react';
import { Share2, Copy, Check, X, QrCode } from 'lucide-react';
import { TelegramChat } from '../../types';

interface ShareLinkModalProps {
  isOpen: boolean;
  onClose: () => void;
  chat: TelegramChat | null;
  onToast?: (message: string, type?: 'success' | 'error' | 'info') => void;
  lang?: 'ar' | 'en';
  isDark?: boolean;
}

export const ShareLinkModal: React.FC<ShareLinkModalProps> = ({
  isOpen,
  onClose,
  chat,
  onToast,
  lang = 'ar',
  isDark = true,
}) => {
  const [copied, setCopied] = useState(false);
  const isAr = lang === 'ar';

  if (!isOpen || !chat) return null;

  const shareUrl = chat.username
    ? `https://t.me/${chat.username}`
    : `https://t.me/joinchat/${chat.id.replace(/[^a-zA-Z0-9]/g, '')}`;

  const handleCopy = () => {
    navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    if (onToast) {
      onToast(isAr ? 'تم نسخ الرابط إلى الحافظة' : 'Link copied to clipboard', 'success');
    }
    setTimeout(() => setCopied(false), 2500);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
      <div
        className={`w-full max-w-md rounded-2xl shadow-2xl p-6 border transition-colors ${
          isDark ? 'bg-[#17212b] border-[#242f3d] text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        <div className="flex items-center justify-between pb-3 border-b border-gray-700/40">
          <div className="flex items-center gap-2 font-bold text-base text-[#3390ec]">
            <Share2 className="w-5 h-5" />
            <span>{isAr ? 'مشاركة رابط المحادثة' : 'Share Chat Link'}</span>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg hover:bg-white/10 text-gray-400 hover:text-white transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="mt-4 flex flex-col items-center">
          <div className="w-16 h-16 rounded-full flex items-center justify-center font-bold text-2xl text-white shadow-lg mb-3 bg-[#3390ec]">
            {chat.title.slice(0, 2).toUpperCase()}
          </div>
          <h3 className="font-bold text-lg text-center">{chat.title}</h3>
          {chat.username && (
            <p className="text-xs text-gray-400 font-mono mt-0.5">@{chat.username}</p>
          )}

          <div className="w-full mt-5 p-3 rounded-xl bg-black/30 border border-gray-700/50 flex items-center justify-between gap-3">
            <span className="text-xs text-gray-300 font-mono truncate select-all">{shareUrl}</span>
            <button
              onClick={handleCopy}
              className={`p-2 rounded-lg font-bold text-xs flex items-center gap-1.5 transition ${
                copied
                  ? 'bg-emerald-600 text-white'
                  : 'bg-[#3390ec] hover:bg-[#2b7ec9] text-white'
              }`}
            >
              {copied ? (
                <>
                  <Check className="w-4 h-4" />
                  <span>{isAr ? 'تم النسخ' : 'Copied'}</span>
                </>
              ) : (
                <>
                  <Copy className="w-4 h-4" />
                  <span>{isAr ? 'نسخ' : 'Copy'}</span>
                </>
              )}
            </button>
          </div>
        </div>

        <div className="mt-6 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 rounded-xl text-sm font-semibold bg-[#3390ec]/20 hover:bg-[#3390ec]/30 text-[#3390ec] transition"
          >
            {isAr ? 'إغلاق' : 'Close'}
          </button>
        </div>
      </div>
    </div>
  );
};
