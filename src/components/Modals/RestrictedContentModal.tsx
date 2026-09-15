import React from 'react';
import { AlertOctagon, X, ShieldAlert, Lock, Info, ExternalLink } from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';

interface RestrictedContentModalProps {
  isOpen: boolean;
  onClose: () => void;
  reason?: string;
  chatTitle?: string;
}

export const RestrictedContentModal: React.FC<RestrictedContentModalProps> = ({
  isOpen,
  onClose,
  reason,
  chatTitle,
}) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';

  if (!isOpen) return null;

  const defaultReason = isArabic
    ? 'لا يمكن عرض هذه المجموعة بسبب استخدامها لنشر محتوى إباحي في السابق.'
    : 'This channel or group is blocked because it was used to spread sensitive or restricted content.';

  const displayReason = reason || defaultReason;

  return (
    <div
      id="tg-restricted-content-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in duration-200"
      onClick={onClose}
    >
      <div
        id="tg-restricted-content-dialog"
        className="w-full max-w-sm bg-[#17212b] border border-red-500/30 rounded-3xl p-6 shadow-2xl space-y-4 text-center select-none transform transition-all animate-in zoom-in-95 duration-150"
        onClick={(e) => e.stopPropagation()}
        style={{
          boxShadow: '0 25px 50px -12px rgba(220, 38, 38, 0.25)',
        }}
      >
        {/* Warning Icon Badge */}
        <div className="w-16 h-16 mx-auto rounded-full bg-red-500/15 border border-red-500/30 flex items-center justify-center text-red-400">
          <ShieldAlert className="w-9 h-9" />
        </div>

        {/* Title */}
        <div className="space-y-1">
          <h3 className="text-lg font-bold text-white tracking-tight">
            {isArabic ? 'غير مُتاحة' : 'Unavailable'}
          </h3>
          {chatTitle && (
            <p className="text-xs text-gray-400 font-mono truncate px-2">
              {chatTitle}
            </p>
          )}
        </div>

        {/* Official Restriction Notice Box */}
        <div className="p-4 bg-black/30 rounded-2xl border border-white/5 text-xs text-gray-200 leading-relaxed text-center space-y-2">
          <p>{displayReason}</p>
          <div className="flex items-center justify-center gap-1.5 text-[11px] text-red-400/90 font-semibold pt-1 border-t border-white/5">
            <Lock className="w-3.5 h-3.5" />
            <span>{isArabic ? 'حظر امتثال لقوانين وشروط الخدمة' : 'Restricted by Terms of Service'}</span>
          </div>
        </div>

        {/* Actions */}
        <div className="pt-2 flex flex-col gap-2">
          <button
            id="tg-restricted-close-btn"
            onClick={onClose}
            className="w-full py-3 bg-[#2481cc] hover:bg-[#1f6fa8] active:bg-[#195a88] text-white font-bold text-sm rounded-xl shadow-lg transition-all"
          >
            {isArabic ? 'إغلاق' : 'Close'}
          </button>

          <button
            onClick={() => {
              showToast(
                isArabic
                  ? 'تم إرسال طلب مراجعة لفريق الرقابة والأمان'
                  : 'Appeal sent to Telegram Moderation team',
                '🛡️'
              );
              onClose();
            }}
            className="w-full py-2 text-xs text-gray-400 hover:text-gray-200 transition-colors flex items-center justify-center gap-1"
          >
            <Info className="w-3.5 h-3.5" />
            <span>{isArabic ? 'تقديم طلب التماس / مراجعة' : 'Appeal this restriction'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
