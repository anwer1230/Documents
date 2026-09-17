import React, { useState } from 'react';
import { Trash2, AlertTriangle, X } from 'lucide-react';
import { TelegramChat } from '../../types';

interface ClearHistoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (alsoForEveryone?: boolean) => void;
  chat: TelegramChat | null;
  lang?: 'ar' | 'en';
  isDark?: boolean;
}

export const ClearHistoryModal: React.FC<ClearHistoryModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  chat,
  lang = 'ar',
  isDark = true,
}) => {
  const [alsoForEveryone, setAlsoForEveryone] = useState(false);
  const isAr = lang === 'ar';

  if (!isOpen || !chat) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
      <div
        className={`w-full max-w-md rounded-2xl shadow-2xl p-6 border transition-colors ${
          isDark ? 'bg-[#17212b] border-[#242f3d] text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        <div className="flex items-center justify-between pb-3 border-b border-gray-700/40">
          <div className="flex items-center gap-2 font-bold text-base text-rose-500">
            <Trash2 className="w-5 h-5" />
            <span>{isAr ? 'مسح سجل المحادثة' : 'Clear Chat History'}</span>
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
            ? `هل أنت متأكد من رغبتك في مسح كافة الرسائل في المحادثة مع "${chat.title}"؟ لا يمكن التراجع عن هذا الإجراء.`
            : `Are you sure you want to clear all messages in the chat with "${chat.title}"? This action cannot be undone.`}
        </p>

        {chat.type === 'private' && (
          <label className="mt-4 flex items-center gap-3 p-3 rounded-xl bg-black/20 hover:bg-black/30 cursor-pointer transition">
            <input
              type="checkbox"
              checked={alsoForEveryone}
              onChange={(e) => setAlsoForEveryone(e.target.checked)}
              className="w-4 h-4 rounded text-rose-500 focus:ring-rose-500 accent-rose-500"
            />
            <span className="text-xs text-gray-300 font-medium">
              {isAr ? `مسح أيضاً لدى "${chat.title}"` : `Also clear for "${chat.title}"`}
            </span>
          </label>
        )}

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
              onConfirm(alsoForEveryone);
              onClose();
            }}
            className="px-5 py-2 rounded-xl text-sm font-bold bg-rose-600 hover:bg-rose-700 text-white shadow-lg shadow-rose-600/25 transition active:scale-95"
          >
            {isAr ? 'مسح السجل' : 'Clear History'}
          </button>
        </div>
      </div>
    </div>
  );
};
