import React, { useState } from "react";
import { X, Calendar, Clock, Plus, Trash2, Send, Check } from "lucide-react";
import { ScheduledMessage, TelegramDialog } from "../../types";
import { getScheduledMessages, saveScheduledMessages } from "../../lib/telegramProTools";

interface ScheduledMessagesModalProps {
  isOpen: boolean;
  onClose: () => void;
  dialogs: TelegramDialog[];
  onScheduleAdded: (msg: ScheduledMessage) => void;
}

export const ScheduledMessagesModal: React.FC<ScheduledMessagesModalProps> = ({
  isOpen,
  onClose,
  dialogs,
  onScheduleAdded,
}) => {
  const [messages, setMessages] = useState<ScheduledMessage[]>(() => getScheduledMessages());
  const [selectedChatId, setSelectedChatId] = useState(dialogs[0]?.id || "");
  const [messageText, setMessageText] = useState("");
  const [scheduledDate, setScheduledDate] = useState("");
  const [scheduledTime, setScheduledTime] = useState("");
  const [showAddForm, setShowAddForm] = useState(false);

  if (!isOpen) return null;

  const handleAddSchedule = (e: React.FormEvent) => {
    e.preventDefault();
    if (!messageText.trim() || !selectedChatId || !scheduledDate || !scheduledTime) return;

    const targetTimestamp = new Date(`${scheduledDate}T${scheduledTime}`).getTime();
    const chat = dialogs.find((d) => d.id === selectedChatId);

    const newSchedule: ScheduledMessage = {
      id: `sched_${Date.now()}`,
      chatId: selectedChatId,
      chatTitle: chat?.title || chat?.name || "Chat",
      text: messageText.trim(),
      scheduledTime: targetTimestamp,
      status: "pending",
      createdAt: Date.now(),
    };

    const updated = [...messages, newSchedule];
    setMessages(updated);
    saveScheduledMessages(updated);
    onScheduleAdded(newSchedule);

    setMessageText("");
    setShowAddForm(false);
  };

  const handleRemove = (id: string | number) => {
    const updated = messages.filter((m) => m.id !== id);
    setMessages(updated);
    saveScheduledMessages(updated);
  };

  return (
    <div
      id="scheduled-messages-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in"
      dir="rtl"
    >
      <div
        id="scheduled-messages-modal"
        className="w-full max-w-md bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden flex flex-col p-6 select-none max-h-[90vh]"
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2.5 text-amber-500 font-bold text-base">
            <Clock className="w-5 h-5" />
            <div>
              <h3 className="text-slate-800 dark:text-white">الرسائل المجدولة (Scheduled Messages)</h3>
              <p className="text-[11px] text-slate-400 font-normal">إرسال الرسائل تلقائياً في موعد محدد</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* List of scheduled messages */}
        <div className="mt-4 space-y-2.5 overflow-y-auto flex-1 max-h-56 p-1">
          {messages.length === 0 ? (
            <div className="text-center py-6 text-slate-400 text-xs">
              لا توجد رسائل مجدولة حالياً
            </div>
          ) : (
            messages.map((item) => (
              <div
                key={item.id}
                className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200/80 dark:border-slate-800 flex items-center justify-between"
              >
                <div className="min-w-0 flex-1 pr-1">
                  <div className="flex items-center gap-2 font-bold text-xs text-slate-800 dark:text-white">
                    <span>إلى: {item.chatTitle}</span>
                    <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-amber-100 dark:bg-amber-950 text-amber-600 dark:text-amber-400 font-semibold">
                      {new Date(item.scheduledTime).toLocaleString([], {
                        month: "short",
                        day: "numeric",
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-600 dark:text-slate-300 truncate mt-1">
                    "{item.text}"
                  </p>
                </div>
                <button
                  onClick={() => handleRemove(item.id)}
                  className="w-7 h-7 rounded-full text-slate-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/40 flex items-center justify-center transition-colors shrink-0"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))
          )}
        </div>

        {/* Add Form */}
        {showAddForm ? (
          <form onSubmit={handleAddSchedule} className="mt-4 p-3.5 bg-slate-50 dark:bg-slate-800/60 rounded-2xl border border-slate-200 dark:border-slate-700 space-y-2.5 text-xs">
            <div>
              <label className="font-semibold text-slate-600 dark:text-slate-400 block mb-1">المحادثة:</label>
              <select
                value={selectedChatId}
                onChange={(e) => setSelectedChatId(e.target.value)}
                className="w-full p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-700 text-xs focus:outline-none"
              >
                {dialogs.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.title || d.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="font-semibold text-slate-600 dark:text-slate-400 block mb-1">نص الرسالة:</label>
              <input
                type="text"
                value={messageText}
                onChange={(e) => setMessageText(e.target.value)}
                placeholder="اكتب نص الرسالة هنا..."
                className="w-full p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-700 text-xs focus:outline-none"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="font-semibold text-slate-600 dark:text-slate-400 block mb-1">التاريخ:</label>
                <input
                  type="date"
                  value={scheduledDate}
                  onChange={(e) => setScheduledDate(e.target.value)}
                  className="w-full p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-700 text-xs focus:outline-none"
                  required
                />
              </div>
              <div>
                <label className="font-semibold text-slate-600 dark:text-slate-400 block mb-1">الوقت:</label>
                <input
                  type="time"
                  value={scheduledTime}
                  onChange={(e) => setScheduledTime(e.target.value)}
                  className="w-full p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-700 text-xs focus:outline-none"
                  required
                />
              </div>
            </div>

            <div className="flex gap-2 pt-1">
              <button
                type="submit"
                className="flex-1 py-2 bg-amber-500 hover:bg-amber-600 text-white font-bold rounded-xl"
              >
                حفظ الجدولة
              </button>
              <button
                type="button"
                onClick={() => setShowAddForm(false)}
                className="px-3 py-2 bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-300 font-bold rounded-xl"
              >
                إلغاء
              </button>
            </div>
          </form>
        ) : (
          <button
            onClick={() => setShowAddForm(true)}
            className="mt-4 py-2.5 bg-amber-500 hover:bg-amber-600 active:scale-98 text-white font-bold text-xs rounded-2xl shadow-md transition-all flex items-center justify-center gap-2"
          >
            <Plus className="w-4 h-4" />
            <span>جدولة رسالة جديدة</span>
          </button>
        )}
      </div>
    </div>
  );
};
