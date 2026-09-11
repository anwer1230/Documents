import React, { useState } from 'react';
import { Flag, X, AlertCircle } from 'lucide-react';
import { TelegramChat } from '../../types';

interface ReportChatModalProps {
  isOpen: boolean;
  onClose: () => void;
  chat: TelegramChat | null;
  onReportSubmitted: (reason: string, details?: string) => void;
  lang: 'ar' | 'en';
  isDark: boolean;
}

export const ReportChatModal: React.FC<ReportChatModalProps> = ({
  isOpen,
  onClose,
  chat,
  onReportSubmitted,
  lang,
  isDark,
}) => {
  const isAr = lang === 'ar';
  const [selectedReason, setSelectedReason] = useState<string>('spam');
  const [customDetails, setCustomDetails] = useState('');

  if (!isOpen || !chat) return null;

  const reasons = [
    { id: 'spam', labelAr: 'البريد العشوائي والرسائل المزعجة', labelEn: 'Spam & unsolicited ads' },
    { id: 'violence', labelAr: 'العنف والترويج للعدوان', labelEn: 'Violence & violent extremism' },
    { id: 'child_abuse', labelAr: 'إساءة معاملة القاصرين أو استغلال الأطفال', labelEn: 'Child sexual abuse material' },
    { id: 'pornography', labelAr: 'مواد إباحية غير مصرح بها', labelEn: 'Pornography' },
    { id: 'copyright', labelAr: 'انتهاك حقوق الملكية والنشر', labelEn: 'Copyright infringement' },
    { id: 'illegal_goods', labelAr: 'عقاقير أو بضائع غير قانونية', labelEn: 'Illegal drugs or weapons' },
    { id: 'personal_details', labelAr: 'نشر بيانات شخصية بدون إذن (Doxxing)', labelEn: 'Personal details leak' },
    { id: 'other', labelAr: 'سبب آخر', labelEn: 'Other reason' },
  ];

  const handleSubmit = () => {
    onReportSubmitted(selectedReason, customDetails);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs select-none">
      <div
        className={`w-full max-w-md rounded-2xl shadow-2xl border p-5 transition-all transform scale-100 ${
          isDark ? 'bg-[#17212b] border-[#232e3c] text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        {/* Header */}
        <div className="flex items-center justify-between mb-3 pb-2 border-b border-gray-700/20">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-full bg-red-500/15 text-red-500 flex items-center justify-center">
              <Flag className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-bold text-sm">
                {isAr ? 'الإبلاغ عن المحادثة' : 'Report Chat'}
              </h3>
              <p className="text-[11px] text-gray-400 truncate max-w-[240px]">{chat.title}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-white p-1 rounded-full transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Reasons list */}
        <div className="space-y-1 my-3 max-h-60 overflow-y-auto pe-1">
          {reasons.map((r) => (
            <label
              key={r.id}
              className={`flex items-center gap-3 p-2.5 rounded-xl cursor-pointer transition text-xs ${
                selectedReason === r.id
                  ? isDark
                    ? 'bg-[#2b5278]/40 text-white font-semibold'
                    : 'bg-blue-50 text-[#3390ec] font-semibold'
                  : isDark
                  ? 'hover:bg-[#232e3c] text-gray-300'
                  : 'hover:bg-gray-100 text-gray-700'
              }`}
            >
              <input
                type="radio"
                name="report_reason"
                value={r.id}
                checked={selectedReason === r.id}
                onChange={() => setSelectedReason(r.id)}
                className="w-4 h-4 text-[#3390ec] accent-[#3390ec]"
              />
              <span className="flex-1">{isAr ? r.labelAr : r.labelEn}</span>
            </label>
          ))}
        </div>

        {/* Optional text area */}
        {selectedReason === 'other' && (
          <div className="mb-4">
            <textarea
              value={customDetails}
              onChange={(e) => setCustomDetails(e.target.value)}
              placeholder={
                isAr
                  ? 'يرجى تقديم تفاصيل إضافية حول سبب الإبلاغ...'
                  : 'Please provide additional details...'
              }
              rows={2}
              className={`w-full p-2.5 rounded-xl text-xs focus:outline-none border ${
                isDark
                  ? 'bg-[#242f3d] border-[#2f3f50] text-white focus:border-[#3390ec]'
                  : 'bg-gray-50 border-gray-200 text-gray-800 focus:border-[#3390ec]'
              }`}
            />
          </div>
        )}

        {/* Bottom Actions */}
        <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-gray-700/20">
          <button
            onClick={onClose}
            className={`px-4 py-2 rounded-xl text-xs font-semibold transition ${
              isDark ? 'text-gray-300 hover:bg-[#232e3c]' : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            {isAr ? 'إلغاء' : 'Cancel'}
          </button>
          <button
            onClick={handleSubmit}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-red-500 hover:bg-red-600 text-white transition shadow-sm"
          >
            {isAr ? 'إرسال البلاغ' : 'Submit Report'}
          </button>
        </div>
      </div>
    </div>
  );
};
