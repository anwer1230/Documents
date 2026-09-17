import React, { useState } from 'react';
import { Flag, X, AlertCircle } from 'lucide-react';
import { TelegramChat } from '../../types';

interface ReportChatModalProps {
  isOpen: boolean;
  onClose: () => void;
  chat: TelegramChat | null;
  onReportSubmitted: (reason: string, details?: string) => void;
  lang?: 'ar' | 'en';
  isDark?: boolean;
}

export const ReportChatModal: React.FC<ReportChatModalProps> = ({
  isOpen,
  onClose,
  chat,
  onReportSubmitted,
  lang = 'ar',
  isDark = true,
}) => {
  const [reason, setReason] = useState('spam');
  const [details, setDetails] = useState('');
  const isAr = lang === 'ar';

  if (!isOpen || !chat) return null;

  const reasons = [
    { id: 'spam', label: isAr ? 'رسائل اقتحامية (Spam)' : 'Spam' },
    { id: 'violence', label: isAr ? 'عنف أو تهديد' : 'Violence' },
    { id: 'copyright', label: isAr ? 'انتهاك حقوق النشر' : 'Copyright Infringement' },
    { id: 'illegal_goods', label: isAr ? 'سلع غير قانونية' : 'Illegal Goods' },
    { id: 'other', label: isAr ? 'سبب آخر' : 'Other' },
  ];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onReportSubmitted(reason, details);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
      <div
        className={`w-full max-w-md rounded-2xl shadow-2xl p-6 border transition-colors ${
          isDark ? 'bg-[#17212b] border-[#242f3d] text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        <div className="flex items-center justify-between pb-3 border-b border-gray-700/40">
          <div className="flex items-center gap-2 font-bold text-base text-amber-500">
            <Flag className="w-5 h-5" />
            <span>{isAr ? 'الإبلاغ عن المحادثة' : 'Report Chat'}</span>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg hover:bg-white/10 text-gray-400 hover:text-white transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <p className="text-xs text-gray-400">
            {isAr
              ? `حدد سبب الإبلاغ عن "${chat.title}" لإرساله لفريق المراجعة:`
              : `Select a reason for reporting "${chat.title}" to our moderation team:`}
          </p>

          <div className="space-y-2">
            {reasons.map((r) => (
              <label
                key={r.id}
                className={`flex items-center gap-3 p-3 rounded-xl border cursor-pointer transition ${
                  reason === r.id
                    ? 'border-[#3390ec] bg-[#3390ec]/10'
                    : isDark
                    ? 'border-gray-700/50 bg-black/20 hover:bg-black/30'
                    : 'border-gray-200 bg-gray-50 hover:bg-gray-100'
                }`}
              >
                <input
                  type="radio"
                  name="reportReason"
                  value={r.id}
                  checked={reason === r.id}
                  onChange={(e) => setReason(e.target.value)}
                  className="w-4 h-4 text-[#3390ec] focus:ring-[#3390ec]"
                />
                <span className="text-xs font-semibold">{r.label}</span>
              </label>
            ))}
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-400 mb-1">
              {isAr ? 'تفاصيل إضافية (اختياري)' : 'Additional details (optional)'}
            </label>
            <textarea
              value={details}
              onChange={(e) => setDetails(e.target.value)}
              placeholder={isAr ? 'اكتب تفاصيل البلاغ هنا...' : 'Provide more context...'}
              rows={3}
              className={`w-full px-3 py-2 rounded-xl text-xs outline-none border transition ${
                isDark
                  ? 'bg-black/30 border-gray-700 text-white focus:border-[#3390ec]'
                  : 'bg-white border-gray-300 text-gray-900 focus:border-[#3390ec]'
              }`}
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className={`px-4 py-2 rounded-xl text-sm font-semibold transition ${
                isDark ? 'hover:bg-white/10 text-gray-300' : 'hover:bg-gray-100 text-gray-700'
              }`}
            >
              {isAr ? 'إلغاء' : 'Cancel'}
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl text-sm font-bold bg-amber-600 hover:bg-amber-700 text-white shadow-lg shadow-amber-600/25 transition active:scale-95"
            >
              {isAr ? 'إرسال البلاغ' : 'Submit Report'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
