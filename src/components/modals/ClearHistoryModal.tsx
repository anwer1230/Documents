import React, { useState } from 'react';
import { Trash2, AlertTriangle, X } from 'lucide-react';
import { TelegramChat } from '../../types';

interface ClearHistoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (alsoForEveryone: boolean) => void;
  chat: TelegramChat | null;
  lang: 'ar' | 'en';
  isDark: boolean;
}

export const ClearHistoryModal: React.FC<ClearHistoryModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  chat,
  lang,
  isDark,
}) => {
  const [alsoForEveryone, setAlsoForEveryone] = useState(true);
  const isAr = lang === 'ar';

  if (!isOpen || !chat) return null;

  const isGroup = chat.type === 'group' || chat.type === 'supergroup';

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
            <Trash2 className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-bold text-base">
              {isAr ? 'مسح سجل المحادثة' : 'Clear History'}
            </h3>
            <p className="text-xs text-gray-400 truncate max-w-[220px]">{chat.title}</p>
          </div>
        </div>

        {/* Content warning */}
        <p className="text-xs text-gray-300 dark:text-gray-300 leading-relaxed mb-4">
          {isAr
            ? 'هل تريد بالتأكيد مسح جميع الرسائل في هذه المحادثة؟ لا يمكن التراجع عن هذا الإجراء بعد تنفيذه.'
            : 'Are you sure you want to clear all messages in this chat? This action cannot be undone.'}
        </p>

        {/* Checkbox option */}
        {chat.type !== 'saved' && (
          <label className="flex items-center gap-2.5 p-2.5 rounded-xl cursor-pointer hover:bg-black/10 dark:hover:bg-white/5 transition mb-5 border border-transparent hover:border-gray-700/30">
            <input
              type="checkbox"
              checked={alsoForEveryone}
              onChange={(e) => setAlsoForEveryone(e.target.checked)}
              className="w-4 h-4 rounded text-[#3390ec] accent-[#3390ec] cursor-pointer"
            />
            <span className="text-xs font-medium text-gray-300">
              {isGroup
                ? isAr
                  ? 'حذف السجل أيضاً لجميع أعضاء المجموعة'
                  : 'Also clear history for all members'
                : isAr
                ? `حذف السجل أيضاً لدى ${chat.title}`
                : `Also delete for ${chat.title}`}
            </span>
          </label>
        )}

        {/* Action buttons */}
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
              onConfirm(alsoForEveryone);
              onClose();
            }}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-red-500 hover:bg-red-600 text-white transition shadow-sm"
          >
            {isAr ? 'مسح السجل' : 'Clear'}
          </button>
        </div>
      </div>
    </div>
  );
};
