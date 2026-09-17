import React from 'react';
import { LogOut, X } from 'lucide-react';
import { TelegramChat } from '../../types';

interface LeaveGroupModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  chat: TelegramChat | null;
  lang?: 'ar' | 'en';
  isDark?: boolean;
}

export const LeaveGroupModal: React.FC<LeaveGroupModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  chat,
  lang = 'ar',
  isDark = true,
}) => {
  const isAr = lang === 'ar';
  if (!isOpen || !chat) return null;

  const isChannel = chat.type === 'channel';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
      <div
        className={`w-full max-w-md rounded-2xl shadow-2xl p-6 border transition-colors ${
          isDark ? 'bg-[#17212b] border-[#242f3d] text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        <div className="flex items-center justify-between pb-3 border-b border-gray-700/40">
          <div className="flex items-center gap-2 font-bold text-base text-rose-500">
            <LogOut className="w-5 h-5" />
            <span>
              {isAr
                ? isChannel
                  ? 'مغادرة القناة'
                  : 'مغادرة المجموعة'
                : isChannel
                ? 'Leave Channel'
                : 'Leave Group'}
            </span>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg hover:bg-white/10 text-gray-400 hover:text-white transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="mt-4 text-sm text-gray-300 leading-relaxed">
          {isAr
            ? `هل أنت متأكد من رغبتك في مغادرة "${chat.title}"؟ لن تتلقى أي إشعارات أو رسائل جديدة بعد المغادرة.`
            : `Are you sure you want to leave "${chat.title}"? You will not receive any further notifications or messages.`}
        </p>

        <div className="mt-6 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className={`px-4 py-2 rounded-xl text-sm font-semibold transition ${
              isDark ? 'hover:bg-white/10 text-gray-300' : 'hover:bg-gray-100 text-gray-700'
            }`}
          >
            {isAr ? 'إلغاء' : 'Cancel'}
          </button>
          <button
            onClick={() => {
              onConfirm();
              onClose();
            }}
            className="px-5 py-2 rounded-xl text-sm font-bold bg-rose-600 hover:bg-rose-700 text-white shadow-lg shadow-rose-600/25 transition active:scale-95"
          >
            {isAr ? 'مغادرة' : 'Leave'}
          </button>
        </div>
      </div>
    </div>
  );
};
