import React from 'react';
import { LogOut, AlertOctagon, X } from 'lucide-react';
import { TelegramChat } from '../../types';

interface LeaveGroupModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  chat: TelegramChat | null;
  lang: 'ar' | 'en';
  isDark: boolean;
}

export const LeaveGroupModal: React.FC<LeaveGroupModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  chat,
  lang,
  isDark,
}) => {
  const isAr = lang === 'ar';

  if (!isOpen || !chat) return null;

  const isChannel = chat.type === 'channel';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs select-none">
      <div
        className={`w-full max-w-sm rounded-2xl shadow-2xl border p-5 transition-all transform scale-100 ${
          isDark ? 'bg-[#17212b] border-[#232e3c] text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        {/* Header */}
        <div className="flex items-center gap-3 mb-3">
          <div className="w-10 h-10 rounded-full bg-red-500/15 text-red-500 flex items-center justify-center shrink-0">
            <LogOut className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-bold text-base">
              {isChannel
                ? isAr
                  ? 'مغادرة القناة'
                  : 'Leave Channel'
                : isAr
                ? 'مغادرة المجموعة'
                : 'Leave Group'}
            </h3>
            <p className="text-xs text-gray-400 truncate max-w-[220px]">{chat.title}</p>
          </div>
        </div>

        {/* Body Warning */}
        <p className="text-xs text-gray-300 dark:text-gray-300 leading-relaxed mb-6">
          {isChannel
            ? isAr
              ? 'هل أنت متأكد من رغبتك في مغادرة هذه القناة؟ لن تصلك تحديثات أو منشورات جديدة.'
              : 'Are you sure you want to leave this channel? You will no longer receive new posts.'
            : isAr
            ? 'هل أنت متأكد من رغبتك في مغادرة هذه المجموعة؟ لن تتمكن بعد ذلك من إرسال أو استلام الرسائل مع باقي الأعضاء.'
            : 'Are you sure you want to leave this group? You will no longer be able to send or receive messages.'}
        </p>

        {/* Actions */}
        <div className="flex items-center justify-end gap-2.5">
          <button
            onClick={onClose}
            className={`px-4 py-2 rounded-xl text-xs font-semibold transition ${
              isDark ? 'text-gray-300 hover:bg-[#232e3c]' : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            {isAr ? 'إلغاء' : 'Cancel'}
          </button>
          <button
            onClick={() => {
              onConfirm();
              onClose();
            }}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-red-500 hover:bg-red-600 text-white transition shadow-sm flex items-center gap-1.5"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span>{isAr ? 'مغادرة' : 'Leave'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
