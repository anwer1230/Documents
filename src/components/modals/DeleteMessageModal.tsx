import React, { useState } from 'react';
import { Trash2, X } from 'lucide-react';

interface DeleteMessageModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (alsoForEveryone: boolean) => void;
  lang: 'ar' | 'en';
  isDark: boolean;
  canDeleteForEveryone?: boolean;
}

export const DeleteMessageModal: React.FC<DeleteMessageModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  lang,
  isDark,
  canDeleteForEveryone = true,
}) => {
  const [alsoForEveryone, setAlsoForEveryone] = useState(true);
  const isAr = lang === 'ar';

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs select-none">
      <div
        className={`w-full max-w-xs rounded-2xl shadow-2xl border p-5 transition-all transform scale-100 ${
          isDark ? 'bg-[#17212b] border-[#232e3c] text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        <div className="flex items-center gap-3 mb-3">
          <div className="w-9 h-9 rounded-full bg-red-500/15 text-red-500 flex items-center justify-center shrink-0">
            <Trash2 className="w-4 h-4" />
          </div>
          <h3 className="font-bold text-sm">
            {isAr ? 'حذف الرسالة' : 'Delete Message'}
          </h3>
        </div>

        <p className="text-xs text-gray-300 dark:text-gray-300 leading-relaxed mb-4">
          {isAr
            ? 'هل تريد بالتأكيد حذف هذه الرسالة؟'
            : 'Are you sure you want to delete this message?'}
        </p>

        {canDeleteForEveryone && (
          <label className="flex items-center gap-2.5 p-2 rounded-xl cursor-pointer hover:bg-black/10 dark:hover:bg-white/5 transition mb-5">
            <input
              type="checkbox"
              checked={alsoForEveryone}
              onChange={(e) => setAlsoForEveryone(e.target.checked)}
              className="w-4 h-4 rounded text-[#3390ec] accent-[#3390ec]"
            />
            <span className="text-xs text-gray-300">
              {isAr ? 'حذف أيضاً لدى الجميع' : 'Also delete for everyone'}
            </span>
          </label>
        )}

        <div className="flex items-center justify-end gap-2">
          <button
            onClick={onClose}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition ${
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
            className="px-4 py-1.5 rounded-xl text-xs font-semibold bg-red-500 hover:bg-red-600 text-white transition shadow-sm"
          >
            {isAr ? 'حذف' : 'Delete'}
          </button>
        </div>
      </div>
    </div>
  );
};
