import React, { useState } from 'react';
import { Link2, Copy, Check, Share2, QrCode, X } from 'lucide-react';
import { TelegramChat } from '../../types';

interface ShareLinkModalProps {
  isOpen: boolean;
  onClose: () => void;
  chat: TelegramChat | null;
  onToast: (message: string, type?: 'success' | 'info' | 'error') => void;
  lang: 'ar' | 'en';
  isDark: boolean;
}

export const ShareLinkModal: React.FC<ShareLinkModalProps> = ({
  isOpen,
  onClose,
  chat,
  onToast,
  lang,
  isDark,
}) => {
  const [isCopied, setIsCopied] = useState(false);
  const [showQR, setShowQR] = useState(false);
  const isAr = lang === 'ar';

  if (!isOpen || !chat) return null;

  const inviteLink =
    chat.inviteLink ||
    (chat.username ? `https://t.me/${chat.username}` : `https://t.me/+join_${chat.id}`);

  const handleCopy = () => {
    try {
      navigator.clipboard.writeText(inviteLink);
      setIsCopied(true);
      onToast(
        isAr ? 'تم نسخ الرابط إلى الحافظة!' : 'Link copied to clipboard!',
        'success'
      );
      setTimeout(() => setIsCopied(false), 2500);
    } catch {
      onToast(inviteLink, 'info');
    }
  };

  const handleShare = async () => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: chat.title,
          text: isAr ? `انضم إلى "${chat.title}" على تليجرام:` : `Join "${chat.title}" on Telegram:`,
          url: inviteLink,
        });
        onToast(isAr ? 'تمت المشاركة بنجاح!' : 'Shared successfully!', 'success');
      } catch {}
    } else {
      handleCopy();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs select-none">
      <div
        className={`w-full max-w-sm rounded-2xl shadow-2xl border p-5 transition-all transform scale-100 ${
          isDark ? 'bg-[#17212b] border-[#232e3c] text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-full bg-[#3390ec]/15 text-[#3390ec] flex items-center justify-center">
              <Link2 className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-sm">
                {isAr ? 'رابط الدعوة للمجموعة' : 'Invite Link'}
              </h3>
              <p className="text-[11px] text-gray-400 truncate max-w-[200px]">{chat.title}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-white p-1 rounded-full transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Explanatory text */}
        <p className="text-xs text-gray-300 dark:text-gray-400 leading-relaxed mb-4">
          {isAr
            ? 'أي شخص يمتلك هذا الرابط سيتمكن من الانضمام إلى هذه المجموعة ومراسلة أعضائها مباشرة.'
            : 'Anyone who opens this link can join this group and chat with its members.'}
        </p>

        {/* Link Box */}
        <div
          className={`flex items-center gap-2 p-2.5 rounded-xl border mb-3 font-mono text-xs ${
            isDark ? 'bg-[#242f3d] border-[#2f3f50] text-gray-200' : 'bg-gray-50 border-gray-200 text-gray-800'
          }`}
        >
          <span className="truncate flex-1 select-all text-ellipsis">{inviteLink}</span>
          <button
            onClick={handleCopy}
            className={`p-1.5 rounded-lg transition shrink-0 flex items-center gap-1 text-[11px] font-semibold ${
              isCopied
                ? 'bg-emerald-500 text-white'
                : 'bg-[#3390ec] hover:bg-[#2b7ec9] text-white'
            }`}
            title={isAr ? 'نسخ' : 'Copy'}
          >
            {isCopied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
            <span className="hidden sm:inline">{isCopied ? (isAr ? 'تم' : 'Copied') : (isAr ? 'نسخ' : 'Copy')}</span>
          </button>
        </div>

        {/* QR Code preview toggle */}
        {showQR && (
          <div className="flex flex-col items-center justify-center p-4 rounded-xl bg-white text-gray-900 mb-3 shadow-inner">
            <div className="w-32 h-32 border-4 border-gray-900 rounded-lg p-2 flex flex-col justify-between items-center bg-white">
              <div className="flex justify-between w-full">
                <div className="w-6 h-6 border-2 border-gray-900 bg-gray-900" />
                <div className="w-6 h-6 border-2 border-gray-900 bg-gray-900" />
              </div>
              <div className="text-[9px] font-mono text-center font-bold text-gray-700">
                TELEGRAM WEB K
              </div>
              <div className="flex justify-between w-full">
                <div className="w-6 h-6 border-2 border-gray-900 bg-gray-900" />
                <div className="w-4 h-4 bg-gray-900 rounded-full" />
              </div>
            </div>
            <span className="text-[10px] text-gray-600 mt-2 font-medium">
              {isAr ? 'امسح الرمز بكاميرا الهاتف للانضمام' : 'Scan with phone camera to join'}
            </span>
          </div>
        )}

        {/* Buttons: Share & QR */}
        <div className="grid grid-cols-2 gap-2 mt-2">
          <button
            onClick={() => setShowQR(!showQR)}
            className={`py-2 px-3 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 border transition ${
              isDark
                ? 'bg-[#242f3d] border-[#2f3f50] text-gray-300 hover:bg-[#2c3949]'
                : 'bg-gray-100 border-gray-200 text-gray-700 hover:bg-gray-200'
            }`}
          >
            <QrCode className="w-3.5 h-3.5" />
            <span>{showQR ? (isAr ? 'إخفاء الرمز' : 'Hide QR') : (isAr ? 'رمز QR' : 'QR Code')}</span>
          </button>

          <button
            onClick={handleShare}
            className="py-2 px-3 rounded-xl text-xs font-semibold bg-[#3390ec] hover:bg-[#2b7ec9] text-white flex items-center justify-center gap-1.5 transition shadow-sm"
          >
            <Share2 className="w-3.5 h-3.5" />
            <span>{isAr ? 'مشاركة الرابط' : 'Share Link'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
