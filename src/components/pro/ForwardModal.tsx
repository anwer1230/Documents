import React, { useState } from "react";
import { X, CornerUpRight, Send, Search, Check, ShieldCheck, UserX } from "lucide-react";
import { TelegramDialog, TelegramMessage } from "../../types";
import { TelegramAvatar } from "../TelegramAvatar";

interface ForwardModalProps {
  isOpen: boolean;
  onClose: () => void;
  message: TelegramMessage | null;
  dialogs: TelegramDialog[];
  onConfirmForward: (targetChatId: string, customText: string, removeQuote: boolean) => void;
}

export const ForwardModal: React.FC<ForwardModalProps> = ({
  isOpen,
  onClose,
  message,
  dialogs,
  onConfirmForward,
}) => {
  const [search, setSearch] = useState("");
  const [selectedChatId, setSelectedChatId] = useState<string | null>(null);
  const [customText, setCustomText] = useState(message?.text || "");
  const [removeQuote, setRemoveQuote] = useState(true);

  // Sync text when message changes
  React.useEffect(() => {
    if (message) {
      setCustomText(message.text || "");
      setSelectedChatId(null);
    }
  }, [message]);

  if (!isOpen || !message) return null;

  const filteredDialogs = dialogs.filter((d) =>
    (d.title || d.name || "").toLowerCase().includes(search.toLowerCase())
  );

  const handleSend = () => {
    if (!selectedChatId) return;
    onConfirmForward(selectedChatId, customText, removeQuote);
    onClose();
  };

  return (
    <div
      id="forward-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in"
      dir="rtl"
    >
      <div
        id="forward-modal"
        className="w-full max-w-md bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden flex flex-col p-6 select-none max-h-[90vh]"
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2.5 text-sky-500 font-bold text-base">
            <CornerUpRight className="w-5 h-5 -scale-x-100" />
            <h3 className="text-slate-800 dark:text-white">إعادة توجيه متقدمة للرسالة (Direct Forward)</h3>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Message Preview */}
        <div className="mt-3 p-3 bg-slate-50 dark:bg-slate-800/60 rounded-2xl border border-slate-200/80 dark:border-slate-700/60 text-xs">
          <div className="text-[11px] font-bold text-sky-600 dark:text-sky-400 mb-1">
            المرسل الأصلي: {message.senderName}
          </div>
          <textarea
            value={customText}
            onChange={(e) => setCustomText(e.target.value)}
            className="w-full bg-white dark:bg-slate-900 p-2 rounded-xl border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-1 focus:ring-sky-500 resize-none h-16 leading-relaxed"
            placeholder="يمكنك تعديل نص الرسالة قبل التحويل..."
          />
        </div>

        {/* Quote Toggle */}
        <div className="mt-3 p-2.5 bg-sky-50 dark:bg-sky-950/40 rounded-xl flex items-center justify-between border border-sky-100 dark:border-sky-900/50">
          <div className="flex items-center gap-2">
            <UserX className="w-4 h-4 text-sky-600 dark:text-sky-400" />
            <div>
              <span className="text-xs font-semibold text-slate-800 dark:text-slate-200 block">
                تحويل مباشر بدون اقتباس (إخفاء اسم المرسل)
              </span>
              <span className="text-[10px] text-slate-400">
                إرسال الرسالة باسمك وكأنك كاتبها مباشرة
              </span>
            </div>
          </div>
          <button
            onClick={() => setRemoveQuote(!removeQuote)}
            className={`w-9 h-5 rounded-full transition-colors relative flex items-center p-0.5 shrink-0 ${
              removeQuote ? "bg-sky-500" : "bg-slate-300 dark:bg-slate-700"
            }`}
          >
            <span
              className={`w-4 h-4 rounded-full bg-white shadow-sm transform transition-transform ${
                removeQuote ? "-translate-x-4" : "translate-x-0"
              }`}
            />
          </button>
        </div>

        {/* Search Chat */}
        <div className="mt-3 relative">
          <Search className="w-4 h-4 absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="اختر المحادثة أو القناة للتحويل إليها..."
            className="w-full pr-9 pl-4 py-2 bg-slate-100 dark:bg-slate-800/80 rounded-xl text-xs text-slate-800 dark:text-slate-100 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>

        {/* Chat List */}
        <div className="mt-2 space-y-1.5 overflow-y-auto flex-1 max-h-48 p-1">
          {filteredDialogs.map((d) => {
            const isSelected = selectedChatId === String(d.id);
            return (
              <div
                key={String(d.id)}
                onClick={() => setSelectedChatId(String(d.id))}
                className={`p-2.5 rounded-xl flex items-center justify-between cursor-pointer transition-all ${
                  isSelected
                    ? "bg-sky-500 text-white font-bold"
                    : "hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200"
                }`}
              >
                <div className="flex items-center gap-2.5 min-w-0">
                  <TelegramAvatar
                    id={String(d.id)}
                    name={d.title || d.name}
                    photoUrl={d.photoUrl}
                    type={d.type}
                    size="sm"
                  />
                  <span className="truncate text-xs">{d.title || d.name}</span>
                </div>
                {isSelected && <Check className="w-4 h-4 shrink-0" />}
              </div>
            );
          })}
        </div>

        {/* Submit */}
        <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 flex justify-end gap-2">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 font-bold text-xs rounded-xl"
          >
            إلغاء
          </button>
          <button
            disabled={!selectedChatId}
            onClick={handleSend}
            className="px-5 py-2 bg-sky-500 hover:bg-sky-600 active:scale-98 disabled:opacity-50 text-white font-bold text-xs rounded-xl shadow-md transition-all flex items-center gap-1.5"
          >
            <Send className="w-3.5 h-3.5 -rotate-45 ml-1 fill-current" />
            <span>إرسال التحويل</span>
          </button>
        </div>
      </div>
    </div>
  );
};
